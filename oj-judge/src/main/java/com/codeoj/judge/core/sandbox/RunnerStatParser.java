package com.codeoj.judge.core.sandbox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 只接受可信 supervisor 输出的完整末行；缺失/损坏协议不得降级为用户错误。 */
public final class RunnerStatParser {
    public static final String MARKER = "###CODEOJ_STAT###";
    private static final Pattern STAT = Pattern.compile(
            "###CODEOJ_STAT### exit=(\\d{1,3}) time_ms=(\\d+) cpu_ms=(\\d+) mem_kb=(\\d+) trunc=([01])");

    private RunnerStatParser() { }

    public static Parsed parse(String stderr) {
        String text = stderr == null ? "" : stderr;
        int index = text.lastIndexOf(MARKER);
        if (index < 0 || (index > 0 && text.charAt(index - 1) != '\n')) return invalid(text);
        Matcher matcher = STAT.matcher(text.substring(index).stripTrailing());
        if (!matcher.matches()) return invalid(text);
        try {
            int exit = Integer.parseInt(matcher.group(1));
            if (exit > 255) return invalid(text);
            return new Parsed(text.substring(0, index), exit,
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)), true);
        } catch (NumberFormatException exception) {
            return invalid(text);
        }
    }

    private static Parsed invalid(String text) {
        return new Parsed(text, -1, 0, 0, 0, 0, false);
    }

    /** wallTimeMs 是题目时间口径；cpuTimeMs 仅供诊断。 */
    public record Parsed(String programStderr, int exitCode, int wallTimeMs, int cpuTimeMs,
                         int peakMemoryKb, int truncated, boolean valid) { }
}
