package com.codeoj.judge.core.sandbox;

import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.config.JudgeProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** root supervisor 只负责可信统计；编译器/用户程序和内层 timeout 降权到非 root。 */
@Component
public class DockerSandbox {
    private static final Logger log = LoggerFactory.getLogger(DockerSandbox.class);
    private final DockerClient dockerClient;
    private final JudgeProperties props;
    private final ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "judge-container-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    public DockerSandbox(DockerClient dockerClient, JudgeProperties props) {
        this.dockerClient = dockerClient;
        this.props = props;
    }

    @PreDestroy
    public void shutdown() { watchdog.shutdownNow(); }

    /** 配置值是容器总时限下限；必须覆盖题目时间和容器启动/统计收尾余量。 */
    public long hardTimeoutMs(long programTimeoutMs) {
        return Math.max(Math.max(1000, props.getContainerHardTimeoutMs()), programTimeoutMs + 10000L);
    }

    public ExecutionResult exec(String image, long submissionId, int caseNo, Path hostWorkDir,
                                String[] command, long timeoutMs, int judgeMemoryLimitMb,
                                int containerMemoryMb, boolean compile) {
        String containerId = null;
        ScheduledFuture<?> killTask = null;
        AtomicBoolean hardKilled = new AtomicBoolean();
        long hardTimeout = hardTimeoutMs(timeoutMs);
        try {
            if (!props.getContainerUser().matches("[1-9][0-9]*:[1-9][0-9]*"))
                return systemError("沙箱程序用户必须为非 root UID:GID");
            containerId = dockerClient.createContainerCmd(image)
                    .withName("judge-" + submissionId + "-" + caseNo + "-" + UUID.randomUUID())
                    .withUser("0:0")
                    .withWorkingDir("/work")
                    .withCmd(command)
                    .withAttachStdout(true).withAttachStderr(true).withTty(false)
                    .withHostConfig(buildHostConfig(hostWorkDir, containerMemoryMb, compile)).exec().getId();
            final String id = containerId;
            killTask = watchdog.schedule(() -> {
                hardKilled.set(true);
                try { dockerClient.killContainerCmd(id).exec(); }
                catch (Exception exception) { log.debug("watchdog kill failed: {}", exception.getMessage()); }
            }, hardTimeout, TimeUnit.MILLISECONDS);
            dockerClient.startContainerCmd(containerId).exec();
            FrameCollector collector = new FrameCollector(props.getOutputLimitBytes());
            try (collector; WaitCallback waiter = new WaitCallback()) {
                dockerClient.waitContainerCmd(containerId).exec(waiter);
                if (!waiter.await(hardTimeout + 5000)) throw new IllegalStateException("wait container timeout");
                if (waiter.failure != null) throw new IllegalStateException("wait container failed", waiter.failure);
                // A fast process may exit before attach is established, leaving its upgraded
                // HTTP stream open. Read the finite, complete log after the container exits.
                dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true)
                        .withTailAll().withFollowStream(false).exec(collector);
                if (!collector.awaitCompletion(15000, TimeUnit.MILLISECONDS))
                    throw new IllegalStateException("incomplete sandbox output stream");
            }
            if (killTask != null) killTask.cancel(false);
            InspectContainerResponse.ContainerState state = dockerClient.inspectContainerCmd(containerId).exec().getState();
            if (state == null) return systemError("无法读取容器终态");
            RunnerStatParser.Parsed stat = RunnerStatParser.parse(collector.stderr.toString(StandardCharsets.UTF_8));
            boolean oomKilled = Boolean.TRUE.equals(state.getOOMKilled());
            SubmissionStatus status = determineStatus(stat, compile, timeoutMs, judgeMemoryLimitMb,
                    hardKilled.get(), oomKilled, state.getExitCodeLong(), collector.stdoutOverflow || collector.stderrOverflow);
            String error = null;
            if (status == SubmissionStatus.SYSTEM_ERROR) error = "沙箱统计协议缺失或容器异常退出";
            else if (hardKilled.get() || (stat.valid() && (stat.exitCode() == 124 || stat.wallTimeMs() > timeoutMs)))
                error = compile ? "编译超时" : "运行超过墙钟时间限制";
            else if (oomKilled || (stat.valid() && stat.peakMemoryKb() > judgeMemoryLimitMb * 1024L))
                error = compile ? "编译内存超限" : "内存超限";
            else if (stat.truncated() == 1 || collector.stdoutOverflow || collector.stderrOverflow)
                error = "输出超过上限";
            else if (status == SubmissionStatus.COMPILE_ERROR || status == SubmissionStatus.RUNTIME_ERROR)
                error = sanitize(stat.programStderr());
            return ExecutionResult.builder().status(status).exitCode(stat.exitCode())
                    .stdout(collector.stdout.toString(StandardCharsets.UTF_8)).stderr(stat.programStderr())
                    .wallTimeMs(hardKilled.get() ? (int) Math.min(Integer.MAX_VALUE, timeoutMs) : stat.wallTimeMs())
                    .cpuTimeMs(stat.cpuTimeMs()).peakMemoryKb(stat.peakMemoryKb()).errorInfo(error).build();
        } catch (Exception exception) {
            log.error("sandbox failed, submission={}", submissionId, exception);
            return systemError("沙箱调度或执行失败，请检查镜像和 Docker 服务");
        } finally {
            if (killTask != null) killTask.cancel(false);
            if (containerId != null) {
                try { dockerClient.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec(); }
                catch (Exception exception) { log.warn("remove container failed: {}", exception.getMessage()); }
            }
        }
    }

    HostConfig buildHostConfig(Path directory, int memoryMb, boolean compile) {
        long memory = memoryMb * 1024L * 1024L;
        return HostConfig.newHostConfig().withNetworkMode("none").withMemory(memory).withMemorySwap(memory)
                .withCpuPeriod(100000L).withCpuQuota(100000L).withPidsLimit(64L).withReadonlyRootfs(true)
                .withTmpFs(Map.of("/tmp", "rw,size=" + props.getTmpfsSize() + ",noexec,nosuid,nodev,mode=1777",
                        "/run/codeoj", "rw,size=40m,noexec,nosuid,nodev,mode=0700"))
                .withBinds(new Bind(directory.toAbsolutePath().toString(), new Volume("/work"),
                        compile ? AccessMode.rw : AccessMode.ro))
                .withCapDrop(Capability.ALL).withCapAdd(Capability.SETUID, Capability.SETGID)
                .withSecurityOpts(List.of("no-new-privileges"))
                .withUlimits(List.of(new Ulimit("nofile", 64L, 64L), new Ulimit("nproc", 64L, 64L),
                        new Ulimit("fsize", 16777216L, 16777216L)));
    }

    static SubmissionStatus determineStatus(RunnerStatParser.Parsed stat, boolean compile, long timeoutMs,
            int memoryMb, boolean hardKilled, boolean oomKilled, Long containerExit, boolean overflow) {
        if (oomKilled) return compile ? SubmissionStatus.COMPILE_ERROR : SubmissionStatus.MEMORY_LIMIT_EXCEEDED;
        if (hardKilled) return compile ? SubmissionStatus.COMPILE_ERROR : SubmissionStatus.TIME_LIMIT_EXCEEDED;
        if (!stat.valid() || containerExit == null || containerExit != 0L) return SubmissionStatus.SYSTEM_ERROR;
        if (compile) return stat.exitCode() != 0 || stat.truncated() != 0 || overflow
                || stat.wallTimeMs() > timeoutMs ? SubmissionStatus.COMPILE_ERROR : SubmissionStatus.ACCEPTED;
        if (stat.truncated() != 0 || overflow) return SubmissionStatus.RUNTIME_ERROR;
        if (stat.exitCode() == 124 || stat.wallTimeMs() > timeoutMs) return SubmissionStatus.TIME_LIMIT_EXCEEDED;
        if (stat.peakMemoryKb() > memoryMb * 1024L) return SubmissionStatus.MEMORY_LIMIT_EXCEEDED;
        if (stat.exitCode() != 0) return SubmissionStatus.RUNTIME_ERROR;
        return SubmissionStatus.ACCEPTED;
    }

    private String sanitize(String text) {
        if (text == null || text.isBlank()) return null;
        String value = text.replace(props.getWorkDirRoot(), "<workdir>").replace("/work", "<workdir>");
        return value.substring(0, Math.min(2048, value.length()));
    }

    private ExecutionResult systemError(String message) {
        return ExecutionResult.builder().status(SubmissionStatus.SYSTEM_ERROR).errorInfo(message).build();
    }

    private static final class FrameCollector extends ResultCallback.Adapter<Frame> {
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        private final long outputLimit;
        private volatile boolean stdoutOverflow;
        private volatile boolean stderrOverflow;
        FrameCollector(long outputLimit) { this.outputLimit = Math.max(1, outputLimit); }
        @Override public void onNext(Frame frame) {
            boolean isOut = frame.getStreamType() == StreamType.STDOUT;
            ByteArrayOutputStream target = isOut ? stdout : stderr;
            long limit = isOut ? outputLimit : 8192L;
            byte[] bytes = frame.getPayload();
            int count = (int) Math.min(bytes.length, Math.max(0, limit - target.size()));
            target.write(bytes, 0, count);
            if (count < bytes.length) {
                if (isOut) stdoutOverflow = true;
                else stderrOverflow = true;
            }
        }
    }

    private static final class WaitCallback extends ResultCallback.Adapter<WaitResponse> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Throwable failure;
        @Override public void onNext(WaitResponse response) { latch.countDown(); }
        @Override public void onError(Throwable exception) { failure = exception; latch.countDown(); super.onError(exception); }
        @Override public void onComplete() { latch.countDown(); }
        boolean await(long timeoutMs) throws InterruptedException { return latch.await(timeoutMs, TimeUnit.MILLISECONDS); }
    }

    @lombok.Data
    @lombok.Builder
    public static class ExecutionResult {
        private int exitCode;
        private String stdout;
        private String stderr;
        /** 用户进程墙钟毫秒，不含容器创建/启动耗时。 */
        private Integer wallTimeMs;
        /** CPU 毫秒仅作诊断，不参与题目时间判定。 */
        private Integer cpuTimeMs;
        private Integer peakMemoryKb;
        private SubmissionStatus status;
        private String errorInfo;
    }
}
