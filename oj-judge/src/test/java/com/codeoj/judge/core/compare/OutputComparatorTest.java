package com.codeoj.judge.core.compare;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 输出比对测试（论文 5.4：去行尾空白 + 忽略末尾空行）。
 */
class OutputComparatorTest {

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void exactMatch() {
        assertTrue(comparator.matches("1 2\n3\n", "1 2\n3\n"));
    }

    @Test
    void trailingWhitespacePerLineIgnored() {
        assertTrue(comparator.matches("1 2  \n3\t\n", "1 2\n3\n"));
    }

    @Test
    void trailingEmptyLinesIgnored() {
        assertTrue(comparator.matches("42\n\n\n", "42\n"));
    }

    @Test
    void crlfTreatedAsTrailingWhitespace() {
        assertTrue(comparator.matches("1\r\n2\r\n", "1\n2\n"));
    }

    @Test
    void differentContentMismatch() {
        assertFalse(comparator.matches("42\n", "43\n"));
    }

    @Test
    void missingLineMismatch() {
        assertFalse(comparator.matches("1\n2\n", "1\n2\n3\n"));
    }

    @Test
    void leadingSpacesAndLeadingEmptyLinesAreSignificant() {
        assertFalse(comparator.matches(" 42\n", "42\n"));
        assertFalse(comparator.matches("\n42\n", "42\n"));
        assertFalse(comparator.matches("1\n\n2", "1\n2"));
        assertTrue(comparator.matches("\n 42 \n\n", "\n 42\n"));
    }

    @Test
    void largeOrdinaryMultilineOutputIsComparedWithinLinearBudget() {
        String actual = "12345 \n".repeat(100000);
        String expected = "12345\n".repeat(100000);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                () -> assertTrue(comparator.matches(actual, expected)));
    }
}
