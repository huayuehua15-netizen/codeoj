package com.codeoj.judge.core;

import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.enums.Language;
import com.codeoj.common.model.JudgeSnapshot;
import com.codeoj.judge.config.JudgeProperties;
import com.codeoj.judge.core.compare.OutputComparator;
import com.codeoj.judge.core.sandbox.DockerSandbox;
import com.codeoj.judge.core.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 编译一次、逐用例隔离运行。real 执行真实沙箱；mock 只用于显式选用的链路演示。 */
@Component
public class JudgeEngine {
    private static final Logger log = LoggerFactory.getLogger(JudgeEngine.class);
    private static final int COMPILE_MEMORY_MB = 512;
    private final DockerSandbox sandbox;
    private final OutputComparator comparator;
    private final JudgeProperties props;
    private final String engineMode;
    private final Map<String, LanguageStrategy> strategies;

    public JudgeEngine(DockerSandbox sandbox, OutputComparator comparator,
                       LocalJudgeService localJudgeService, JudgeProperties props,
                       @Value("${oj.judge.engine-mode:real}") String engineMode,
                       CStrategy c, CppStrategy cpp, JavaStrategy java, Python3Strategy python3,
                       JsStrategy js) {
        this.sandbox = sandbox;
        this.comparator = comparator;
        this.props = props;
        this.engineMode = engineMode;
        strategies = Map.of("c", c, "cpp", cpp, "java", java, "python3", python3, "js", js);
    }

    public JudgeResult run(JudgeTask task) {
        String validation = validate(task);
        if (validation != null) return systemErrorResult(task, validation);
        LanguageStrategy strategy = strategies.get(task.getLanguage());
        if (strategy == null) return systemErrorResult(task, "不支持的语言");
        return switch (engineMode) {
            case "mock" -> mockResult(task);
            case "local" -> systemErrorResult(task, "local 演示模式不执行代码，请使用 real 沙箱模式评测");
            case "real" -> realRun(task, strategy);
            default -> systemErrorResult(task, "未知判题模式，拒绝评测");
        };
    }

    static String validate(JudgeTask task) {
        if (task == null || task.getSubmissionId() == null) return "判题任务缺少提交编号";
        if (task.getCases() == null || task.getCases().isEmpty()) return "题目没有可用测试用例";
        if (task.getCases().size() > JudgeSnapshot.MAX_CASES) return "题目用例数超过 1000 上限";
        if (task.getTimeLimitMs() == null || task.getTimeLimitMs() <= 0 || task.getTimeLimitMs() > 60000)
            return "题目时间限制应在 1 到 60000 毫秒之间";
        if (task.getMemoryLimitMb() == null || task.getMemoryLimitMb() < 16 || task.getMemoryLimitMb() > 1024)
            return "题目内存限制应在 16 到 1024 MB 之间";
        Language language;
        try { language = Language.of(task.getLanguage()); }
        catch (IllegalArgumentException e) { return "不支持的语言"; }
        if (task.getMemoryLimitMb() < language.getMinimumMemoryMb())
            return language.getLabel() + " 的最低题目内存限制为 " + language.getMinimumMemoryMb() + " MB";
        if (task.getCode() == null) return "提交代码缺失";
        for (JudgeTask.CaseInput input : task.getCases()) {
            if (input == null || input.getInput() == null || input.getExpectedOutput() == null)
                return "测试用例输入或期望输出缺失";
        }
        return null;
    }

    private JudgeResult mockResult(JudgeTask task) {
        List<CaseResult> cases = new ArrayList<>();
        for (JudgeTask.CaseInput input : task.getCases()) {
            cases.add(CaseResult.builder().index(input.getIndex()).status(SubmissionStatus.ACCEPTED)
                    .timeUsedMs(1).memoryUsedKb(1024).build());
        }
        return JudgeResult.builder().status(SubmissionStatus.ACCEPTED).timeUsedMs(1).memoryUsedKb(1024)
                .passedCases(cases.size()).totalCases(cases.size()).cases(cases)
                .errorInfo("mock 演示结果，未执行代码").build();
    }

    private JudgeResult realRun(JudgeTask task, LanguageStrategy strategy) {
        long budgetMs = sandbox.hardTimeoutMs(strategy.compileTimeoutMs())
                + task.getCases().size() * (sandbox.hardTimeoutMs(task.getTimeLimitMs()) + 5000L) + 10000L;
        budgetMs = Math.min(budgetMs, TimeUnit.MINUTES.toMillis(10));
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(budgetMs);
        try (AttemptWorkspace workspace = new AttemptWorkspace(Path.of(props.getWorkDirRoot()),
                task.getSubmissionId(), task.getExecutionToken())) {
            Path compileDir = workspace.createDirectory("compile", true);
            AttemptWorkspace.writeNew(compileDir.resolve(strategy.sourceFileName()), task.getCode());
            if (strategy.compileCommand().isPresent()) {
                DockerSandbox.ExecutionResult compiled = sandbox.exec(strategy.image(), task.getSubmissionId(),
                        -1, compileDir, runnerCommand(strategy.compileCommand().orElseThrow(),
                                strategy.compileTimeoutMs(), COMPILE_MEMORY_MB),
                        strategy.compileTimeoutMs(), COMPILE_MEMORY_MB, COMPILE_MEMORY_MB, true);
                if (compiled.getStatus() != SubmissionStatus.ACCEPTED) {
                    return JudgeResult.builder().status(compiled.getStatus()).timeUsedMs(compiled.getWallTimeMs())
                            .memoryUsedKb(compiled.getPeakMemoryKb()).passedCases(0)
                            .totalCases(task.getCases().size()).errorInfo(compiled.getErrorInfo())
                            .cases(List.of()).build();
                }
            }
            Path artifacts = workspace.createDirectory("artifacts", false);
            collectArtifacts(strategy, compileDir, artifacts);
            AttemptWorkspace.seal(artifacts);
            List<CaseResult> results = new ArrayList<>();
            int passed = 0, maxTime = 0, maxMemory = 0, ordinal = 0;
            for (JudgeTask.CaseInput input : task.getCases()) {
                if (System.nanoTime() > deadline) return systemErrorResult(task, "评测任务总时限已到");
                Path caseDir = workspace.createDirectory("case-" + (++ordinal), false);
                try (var entries = Files.list(artifacts)) {
                    for (Path artifact : entries.toList()) {
                        AttemptWorkspace.copyArtifact(artifact, caseDir.resolve(artifact.getFileName()),
                                artifact.getFileName().toString().equals("main"));
                    }
                }
                AttemptWorkspace.writeNew(caseDir.resolve("input.txt"), input.getInput());
                AttemptWorkspace.seal(caseDir);
                DockerSandbox.ExecutionResult execution = sandbox.exec(strategy.image(), task.getSubmissionId(),
                        input.getIndex(), caseDir, runnerCommand(strategy.runCommand(task.getMemoryLimitMb()),
                                task.getTimeLimitMs(), task.getMemoryLimitMb()), task.getTimeLimitMs(),
                        task.getMemoryLimitMb(), task.getMemoryLimitMb() + strategy.memoryOverheadMb(), false);
                int time = execution.getWallTimeMs() == null ? 0 : execution.getWallTimeMs();
                int memory = execution.getPeakMemoryKb() == null ? 0 : execution.getPeakMemoryKb();
                maxTime = Math.max(maxTime, time);
                maxMemory = Math.max(maxMemory, memory);
                SubmissionStatus status = execution.getStatus();
                if (status == SubmissionStatus.ACCEPTED && !comparator.matches(execution.getStdout(), input.getExpectedOutput()))
                    status = SubmissionStatus.WRONG_ANSWER;
                String error = execution.getErrorInfo();
                if (status == SubmissionStatus.RUNTIME_ERROR && error == null) error = truncate(execution.getStderr(), 2048);
                results.add(CaseResult.builder().index(input.getIndex()).status(status).timeUsedMs(time)
                        .memoryUsedKb(memory).errorInfo(error).build());
                workspace.deleteDirectory(caseDir);
                if (status != SubmissionStatus.ACCEPTED) {
                    return JudgeResult.builder().status(status).timeUsedMs(maxTime).memoryUsedKb(maxMemory)
                            .passedCases(passed).totalCases(task.getCases().size()).errorInfo(error).cases(results).build();
                }
                passed++;
            }
            return JudgeResult.builder().status(SubmissionStatus.ACCEPTED).timeUsedMs(maxTime).memoryUsedKb(maxMemory)
                    .passedCases(passed).totalCases(task.getCases().size()).cases(results).build();
        } catch (IOException | IllegalArgumentException exception) {
            log.error("workspace failed, submission={}", task.getSubmissionId(), exception);
            return systemErrorResult(task, "工作目录或编译产物校验失败");
        }
    }

    private void collectArtifacts(LanguageStrategy strategy, Path compileDir, Path artifacts) throws IOException {
        if (strategy instanceof JavaStrategy) {
            List<Path> classes;
            try (var entries = Files.list(compileDir)) {
                classes = entries.filter(path -> path.getFileName().toString().endsWith(".class")).toList();
            }
            if (classes.isEmpty() || classes.size() > 1000) throw new IOException("invalid Java artifacts");
            long total = 0;
            for (Path source : classes) {
                if (Files.isSymbolicLink(source) || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("invalid Java artifact");
                total += Files.size(source);
                if (total > 64L * 1024 * 1024) throw new IOException("Java artifacts too large");
                AttemptWorkspace.copyArtifact(source, artifacts.resolve(source.getFileName()), false);
            }
        } else if (strategy.compileCommand().isPresent()) {
            AttemptWorkspace.copyArtifact(compileDir.resolve("main"), artifacts.resolve("main"), true);
        } else {
            AttemptWorkspace.copyArtifact(compileDir.resolve(strategy.sourceFileName()),
                    artifacts.resolve(strategy.sourceFileName()), false);
        }
    }

    private String[] runnerCommand(String[] program, long timeoutMs, int memoryMb) {
        List<String> command = new ArrayList<>(List.of(props.getRunnerPath(), "run", "--timeout-ms",
                String.valueOf(timeoutMs), "--memory-mb", String.valueOf(memoryMb), "--input", "/work/input.txt",
                "--user", props.getContainerUser(), "--"));
        command.addAll(Arrays.asList(program));
        return command.toArray(String[]::new);
    }

    private String truncate(String text, int length) {
        return text == null || text.isBlank() ? null : text.substring(0, Math.min(length, text.length()));
    }

    private JudgeResult systemErrorResult(JudgeTask task, String error) {
        return JudgeResult.builder().status(SubmissionStatus.SYSTEM_ERROR).passedCases(0)
                .totalCases(task == null || task.getCases() == null ? 0 : task.getCases().size())
                .errorInfo(error).cases(List.of()).build();
    }
}
