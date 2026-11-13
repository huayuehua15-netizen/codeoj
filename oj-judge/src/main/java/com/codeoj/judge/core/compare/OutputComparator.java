package com.codeoj.judge.core.compare;

import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 输出比对（论文 5.4）：比对前先标准化——去除行尾空白并忽略末尾空行。
 */
@Component
public class OutputComparator {

    public boolean matches(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    /** 标准化（论文 5.4）：去行尾空白 + 忽略末尾空行 */
    static String normalize(String s) {
        return Arrays.stream(s.split("\n", -1))
                .map(line -> line.replaceAll("[ \t]+$", ""))
                .reduce("", (a, b) -> a + "\n" + b)
                .strip();
    }
}
