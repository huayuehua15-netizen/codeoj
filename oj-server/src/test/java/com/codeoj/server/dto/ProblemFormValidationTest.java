package com.codeoj.server.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProblemFormValidationTest {
    @Test void historicalUnboundedResourceSettingsAreRejectedAtApiBoundary() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            ProblemFormDTO form = new ProblemFormDTO();
            form.setTitle("A+B"); form.setLanguages(List.of("c", "js"));
            assertTrue(validator.validate(form).isEmpty());
            form.setMemoryLimitMb(16);
            assertFalse(validator.validate(form).isEmpty());
            form.setLanguages(List.of("c"));
            assertTrue(validator.validate(form).isEmpty());
            form.setLanguages(List.of("java"));
            form.setMemoryLimitMb(64);
            assertTrue(validator.validate(form).isEmpty());
            form.setTimeLimitMs(20000);
            assertTrue(validator.validate(form).isEmpty());
            form.setTimeLimitMs(0); form.setMemoryLimitMb(100000);
            assertFalse(validator.validate(form).isEmpty());
            form.setTimeLimitMs(null); form.setMemoryLimitMb(null);
            assertFalse(validator.validate(form).isEmpty());
        }
    }
}
