package com.codeoj.judge.core.sandbox;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunnerStatParserTest {
    @Test void acceptsCompleteTrustedProtocolAndSeparatesMetrics() {
        var parsed = RunnerStatParser.parse("diagnostic\n###CODEOJ_STAT### exit=0 time_ms=56 cpu_ms=20 mem_kb=18432 trunc=0\n");
        assertTrue(parsed.valid());
        assertEquals(0, parsed.exitCode());
        assertEquals(56, parsed.wallTimeMs());
        assertEquals(20, parsed.cpuTimeMs());
        assertEquals(18432, parsed.peakMemoryKb());
        assertEquals("diagnostic\n", parsed.programStderr());
    }
    @Test void missingMalformedPartialAndOverflowingMetricsAreInvalid() {
        for (String text : new String[]{"", "stderr", "###CODEOJ_STAT### exit=0 time_ms=1 mem_kb=1 trunc=0",
                "###CODEOJ_STAT### exit=0 time_ms=999999999999 cpu_ms=1 mem_kb=1 trunc=0",
                "###CODEOJ_STAT### exit=0 time_ms=-1 cpu_ms=1 mem_kb=1 trunc=0",
                "###CODEOJ_STAT### exit=0 time_ms=1 cpu_ms=1 mem_kb=1 trunc=0\ntrailing garbage"}) {
            assertFalse(RunnerStatParser.parse(text).valid(), text);
        }
        assertFalse(RunnerStatParser.parse(null).valid());
    }
    @Test void requiresProtocolToBeAnEntireFinalLine() {
        assertFalse(RunnerStatParser.parse("prefix###CODEOJ_STAT### exit=0 time_ms=1 cpu_ms=1 mem_kb=1 trunc=0").valid());
        assertTrue(RunnerStatParser.parse("###CODEOJ_STAT### exit=124 time_ms=1000 cpu_ms=900 mem_kb=12 trunc=1").valid());
    }
}
