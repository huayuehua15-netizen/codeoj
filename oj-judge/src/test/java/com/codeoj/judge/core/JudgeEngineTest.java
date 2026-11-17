package com.codeoj.judge.core;

import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.config.JudgeProperties;
import com.codeoj.judge.core.compare.OutputComparator;
import com.codeoj.judge.core.sandbox.DockerSandbox;
import com.codeoj.judge.core.strategy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JudgeEngineTest {
    @TempDir Path temporary;
    private final DockerSandbox sandbox = mock(DockerSandbox.class);
    private JudgeEngine engine(String mode) {
        JudgeProperties properties = new JudgeProperties();
        properties.setWorkDirRoot(temporary.toString());
        return new JudgeEngine(sandbox, new OutputComparator(), mock(LocalJudgeService.class), properties,
                mode, new CStrategy(), new CppStrategy(), new JavaStrategy(), new Python3Strategy(), new JsStrategy());
    }
    private JudgeTask task(String language) {
        return JudgeTask.builder().submissionId(7L).executionToken(UUID.randomUUID().toString())
                .language(language).code("normal submitted source").timeLimitMs(1000).memoryLimitMb(256)
                .cases(List.of(JudgeTask.CaseInput.builder().index(1).input("1 2\n").expectedOutput("3\n").build(),
                        JudgeTask.CaseInput.builder().index(2).input("2 1\n").expectedOutput("3\n").build())).build();
    }
    @Test void emptyCasesAndInvalidLimitsNeverProduceAcEvenInMockMode() {
        JudgeTask task = task("c");
        task.setCases(List.of());
        assertEquals(SubmissionStatus.SYSTEM_ERROR, engine("mock").run(task).getStatus());
        task = task("c");
        task.setTimeLimitMs(0);
        assertEquals(SubmissionStatus.SYSTEM_ERROR, engine("real").run(task).getStatus());
        task = task("c");
        task.setMemoryLimitMb(1025);
        assertEquals(SubmissionStatus.SYSTEM_ERROR, engine("real").run(task).getStatus());
        verifyNoInteractions(sandbox);
    }
    @Test void localAndUnknownModesFailClosed() {
        assertEquals(SubmissionStatus.SYSTEM_ERROR, engine("local").run(task("java")).getStatus());
        assertEquals(SubmissionStatus.SYSTEM_ERROR, engine("reel").run(task("java")).getStatus());
        verifyNoInteractions(sandbox);
    }
    @Test void normalCompiledProgramUsesIndependentCasesAndWallTime() throws Exception {
        List<Path> cases = new ArrayList<>();
        when(sandbox.exec(anyString(), anyLong(), anyInt(), any(Path.class), any(String[].class),
                anyLong(), anyInt(), anyInt(), anyBoolean())).thenAnswer(invocation -> {
            Path directory = invocation.getArgument(3);
            boolean compile = invocation.getArgument(8);
            if (compile) Files.writeString(directory.resolve("main"), "compiled artifact");
            else {
                cases.add(directory);
                assertTrue(Files.exists(directory.resolve("main")));
                assertTrue(Files.readString(directory.resolve("input.txt")).contains("1"));
            }
            return DockerSandbox.ExecutionResult.builder().status(SubmissionStatus.ACCEPTED)
                    .stdout("3\n").wallTimeMs(17).cpuTimeMs(2).peakMemoryKb(100).build();
        });
        JudgeResult result = engine("real").run(task("c"));
        assertEquals(SubmissionStatus.ACCEPTED, result.getStatus());
        assertEquals(17, result.getTimeUsedMs());
        assertEquals(2, result.getPassedCases());
        assertEquals(2, cases.size());
        assertNotEquals(cases.get(0), cases.get(1));
        assertFalse(Files.exists(cases.get(0)));
        assertFalse(Files.exists(cases.get(1)));
    }
    @Test void jsIsIncludedInRealPipeline() throws Exception {
        when(sandbox.exec(anyString(), anyLong(), anyInt(), any(Path.class), any(String[].class),
                anyLong(), anyInt(), anyInt(), anyBoolean())).thenAnswer(invocation -> {
            assertEquals("codeoj/node:20", invocation.getArgument(0));
            assertTrue(Files.exists(((Path) invocation.getArgument(3)).resolve("main.js")));
            assertFalse((boolean) invocation.getArgument(8));
            return DockerSandbox.ExecutionResult.builder().status(SubmissionStatus.ACCEPTED)
                    .stdout("3\n").wallTimeMs(5).cpuTimeMs(1).peakMemoryKb(20000).build();
        });
        assertEquals(SubmissionStatus.ACCEPTED, engine("real").run(task("js")).getStatus());
    }
}
