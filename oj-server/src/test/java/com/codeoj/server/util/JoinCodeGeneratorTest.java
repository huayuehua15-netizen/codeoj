package com.codeoj.server.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JoinCodeGeneratorTest {

    @Test
    void codesAreEightCharsAndAvoidAmbiguousLetters() {
        JoinCodeGenerator generator = new JoinCodeGenerator();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String code = generator.generate();
            assertEquals(8, code.length());
            assertTrue(code.matches("[A-HJ-NP-Z2-9]{8}"), code);
            assertTrue(seen.add(code), "重复邀请码: " + code);
        }
    }

    @Test
    void teacherProvidedCodeValidation() {
        assertTrue(JoinCodeGenerator.isValid("ABCD2345"));
        assertTrue(JoinCodeGenerator.isValid("A1B2"));
        assertFalse(JoinCodeGenerator.isValid("ABC"));
        assertFalse(JoinCodeGenerator.isValid("ABCD-234"));
        assertFalse(JoinCodeGenerator.isValid("abcdefgh"));
        assertFalse(JoinCodeGenerator.isValid(null));
    }
}
