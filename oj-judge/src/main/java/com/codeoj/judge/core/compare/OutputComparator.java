package com.codeoj.judge.core.compare;

import org.springframework.stereotype.Component;

/** 去行尾空白、忽略末尾空行；保留首部空白。时间和空间复杂度均为 O(n)。 */
@Component
public class OutputComparator {

    public boolean matches(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    static String normalize(String text) {
        if (text == null) throw new IllegalArgumentException("output must not be null");
        StringBuilder result = new StringBuilder(text.length());
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) end = text.length();
            int trimmed = end;
            while (trimmed > start && trailingWhitespace(text.charAt(trimmed - 1))) trimmed--;
            result.append(text, start, trimmed);
            if (end < text.length()) result.append('\n');
            start = end + 1;
        }
        while (!result.isEmpty() && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    private static boolean trailingWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\f' || c == '\u000b';
    }
}
