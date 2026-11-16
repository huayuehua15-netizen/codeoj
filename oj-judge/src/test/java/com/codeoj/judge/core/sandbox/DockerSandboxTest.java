package com.codeoj.judge.core.sandbox;

import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.config.JudgeProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Capability;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DockerSandboxTest {
    private final RunnerStatParser.Parsed missing = RunnerStatParser.parse("");
    private RunnerStatParser.Parsed stat(int exit, int wall, int cpu, int memory) {
        return RunnerStatParser.parse("###CODEOJ_STAT### exit=" + exit + " time_ms=" + wall
                + " cpu_ms=" + cpu + " mem_kb=" + memory + " trunc=0");
    }
    private SubmissionStatus status(RunnerStatParser.Parsed stat, boolean compile, boolean hard, boolean oom) {
        return DockerSandbox.determineStatus(stat, compile, 1000, 256, hard, oom, 0L, false);
    }
    @Test void missingProtocolIsSystemErrorForBothStages() {
        assertEquals(SubmissionStatus.SYSTEM_ERROR, status(missing, false, false, false));
        assertEquals(SubmissionStatus.SYSTEM_ERROR, status(missing, true, false, false));
    }
    @Test void trustedContainerReasonsSurviveMissingProtocol() {
        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, status(missing, false, true, false));
        assertEquals(SubmissionStatus.MEMORY_LIMIT_EXCEEDED, status(missing, false, false, true));
        assertEquals(SubmissionStatus.COMPILE_ERROR, status(missing, true, true, false));
        assertEquals(SubmissionStatus.COMPILE_ERROR, status(missing, true, false, true));
    }
    @Test void exit137AloneDoesNotProveOom() {
        assertEquals(SubmissionStatus.RUNTIME_ERROR, status(stat(137, 5, 1, 100), false, false, false));
        assertEquals(SubmissionStatus.MEMORY_LIMIT_EXCEEDED, status(stat(137, 5, 1, 100), false, false, true));
    }
    @Test void wallTimeIsTheSingleJudgeTimeMetric() {
        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, status(stat(0, 1001, 10, 100), false, false, false));
        assertEquals(SubmissionStatus.ACCEPTED, status(stat(0, 1000, 5000, 100), false, false, false));
        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, status(stat(124, 1000, 1, 100), false, false, false));
    }
    @Test void infrastructureExitIsNotAUserCompileError() {
        assertEquals(SubmissionStatus.SYSTEM_ERROR, DockerSandbox.determineStatus(stat(0, 1, 1, 1),
                true, 1000, 256, false, false, 70L, false));
    }
    @Test void resourceBoundariesAndCompileErrorsAreMapped() {
        assertEquals(SubmissionStatus.ACCEPTED, status(stat(0, 10, 5, 256 * 1024), false, false, false));
        assertEquals(SubmissionStatus.MEMORY_LIMIT_EXCEEDED, status(stat(0, 10, 5, 256 * 1024 + 1), false, false, false));
        assertEquals(SubmissionStatus.COMPILE_ERROR, status(stat(1, 10, 5, 100), true, false, false));
        assertEquals(SubmissionStatus.RUNTIME_ERROR, DockerSandbox.determineStatus(stat(0, 10, 5, 100),
                false, 1000, 256, false, false, 0L, true));
    }
    @Test void hardWatchdogAllowsLongerValidProblemLimits() {
        JudgeProperties properties = new JudgeProperties();
        DockerSandbox sandbox = new DockerSandbox(mock(DockerClient.class), properties);
        assertTrue(sandbox.hardTimeoutMs(60000) >= 70000);
        sandbox.shutdown();
    }
    @Test void runtimeMountIsReadOnlyAndSupervisorHasOnlyDropUserCapabilities() {
        DockerSandbox sandbox = new DockerSandbox(mock(DockerClient.class), new JudgeProperties());
        var runtime = sandbox.buildHostConfig(Path.of("work"), 256, false);
        assertEquals(AccessMode.ro, runtime.getBinds()[0].getAccessMode());
        assertEquals(AccessMode.rw, sandbox.buildHostConfig(Path.of("work"), 512, true).getBinds()[0].getAccessMode());
        assertEquals(Arrays.asList(Capability.SETUID, Capability.SETGID), Arrays.asList(runtime.getCapAdd()));
        assertEquals(Capability.ALL, runtime.getCapDrop()[0]);
        assertTrue(runtime.getTmpFs().get("/run/codeoj").contains("mode=0700"));
        assertEquals("none", runtime.getNetworkMode());
        sandbox.shutdown();
    }
}
