package com.codeoj.judge.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AttemptWorkspaceTest {
    @TempDir Path temporary;
    @Test void concurrentAttemptsNeverShareOrDeleteEachOthersDirectories() throws Exception {
        String token = UUID.randomUUID().toString();
        Path first;
        Path second;
        try (AttemptWorkspace one = new AttemptWorkspace(temporary, 7, token);
             AttemptWorkspace two = new AttemptWorkspace(temporary, 7, token)) {
            first = one.createDirectory("case-1", false);
            second = two.createDirectory("case-1", false);
            assertNotEquals(first.getParent(), second.getParent());
            AttemptWorkspace.writeNew(second.resolve("input.txt"), "keep");
        }
        assertFalse(Files.exists(first));
        assertFalse(Files.exists(second));
    }
    @Test void existingInputCanNeverBeTruncatedOrOverwritten() throws Exception {
        Path input = temporary.resolve("input.txt");
        AttemptWorkspace.writeNew(input, "original");
        assertThrows(Exception.class, () -> AttemptWorkspace.writeNew(input, "changed"));
        assertEquals("original", Files.readString(input));
    }
    @Test void acceptedArtifactsAreCopiedToIndependentFiles() throws Exception {
        Path compiled = temporary.resolve("compiled");
        Files.writeString(compiled, "artifact");
        Path copied = temporary.resolve("copied");
        AttemptWorkspace.copyArtifact(compiled, copied, true);
        Files.writeString(compiled, "changed");
        assertEquals("artifact", Files.readString(copied));
    }
    @Test void directoryArtifactsAreRejected() throws Exception {
        Path directory = Files.createDirectory(temporary.resolve("directory"));
        assertThrows(Exception.class, () -> AttemptWorkspace.copyArtifact(directory, temporary.resolve("artifact"), false));
    }
}
