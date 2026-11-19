package com.codeoj.server.service;

import com.codeoj.common.entity.Problem;
import com.codeoj.common.exception.BizException;
import com.codeoj.server.dto.ProblemFormDTO;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.mapper.TestCaseMapper;
import com.codeoj.server.service.impl.ProblemServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 题目语言白名单校验（review P2-40）。
 *
 * <p>DTO 上的 {@code @Pattern} 是把 {@code Language} 枚举的 code 抄了一份，
 * 会随枚举演进漂移；服务层以枚举为准再校验一次，这里验证这道防线真的生效。
 */
class ProblemLanguageWhitelistTest {

    ProblemMapper problems = mock(ProblemMapper.class);
    TestCaseMapper cases = mock(TestCaseMapper.class);
    ProblemServiceImpl service = new ProblemServiceImpl(problems, cases);

    private static ProblemFormDTO form(List<String> languages) {
        ProblemFormDTO form = new ProblemFormDTO();
        form.setTitle("两数之和");
        form.setLanguages(languages);
        return form;
    }

    @Test void everyLanguageCodeOfTheEnumIsAccepted() {
        for (com.codeoj.common.enums.Language language : com.codeoj.common.enums.Language.values()) {
            assertDoesNotThrow(() -> service.create(form(List.of(language.getCode()))),
                    "枚举自带的 code 不应被拒绝：" + language.getCode());
        }
    }

    @Test void unknownLanguageIsRejectedWithParamError() {
        BizException e = assertThrows(BizException.class,
                () -> service.create(form(List.of("c", "golang"))));
        assertEquals(40000, e.getCode());
        assertTrue(e.getMessage().contains("golang"));
        // 校验必须发生在落库之前，否则脏数据已经在库里了
        verify(problems, never()).insert(any(Problem.class));
    }

    @Test void nullElementIsRejectedInsteadOfPoisoningTheRow() {
        assertThrows(BizException.class, () -> service.create(form(Arrays.asList("c", null))));
        verify(problems, never()).insert(any(Problem.class));
    }

    @Test void emptyOrNullLanguagesAreRejected() {
        assertThrows(BizException.class, () -> service.create(form(null)));
        assertThrows(BizException.class, () -> service.create(form(List.of())));
        verify(problems, never()).insert(any(Problem.class));
    }

    @Test void updateAlsoValidatesBeforeTouchingTheRow() {
        when(problems.selectById(1L)).thenReturn(new Problem());
        assertThrows(BizException.class, () -> service.update(1L, form(List.of("rust"))));
        verify(problems, never()).updateById(any(Problem.class));
    }
}
