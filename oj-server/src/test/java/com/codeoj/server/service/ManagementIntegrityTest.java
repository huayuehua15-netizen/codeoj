package com.codeoj.server.service;

import com.codeoj.common.entity.*;
import com.codeoj.common.exception.BizException;
import com.codeoj.server.dto.*;
import com.codeoj.server.mapper.*;
import com.codeoj.server.service.impl.*;
import com.codeoj.server.util.JoinCodeGenerator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManagementIntegrityTest {
    ProblemMapper problems = mock(ProblemMapper.class);
    ProblemSelectionValidator selection = new ProblemSelectionValidator(problems);

    @Test void malformedAndDuplicateIdsAreRejectedBeforeQuerying() {
        for (List<Long> ids : List.of(List.of(1L, 1L), List.of(-1L), Arrays.asList(1L, null))) {
            assertThrows(BizException.class, () -> selection.validate(ids));
        }
        verifyNoInteractions(problems);
        assertDoesNotThrow(() -> selection.validate(null));
        assertDoesNotThrow(() -> selection.validate(List.of()));
    }

    @Test void missingCourseProblemDoesNotReplaceExistingAssignments() {
        CourseMapper courses = mock(CourseMapper.class);
        CourseProblemMapper links = mock(CourseProblemMapper.class);
        when(problems.selectBatchIds(List.of(404L))).thenReturn(List.of());
        CourseFormDTO form = new CourseFormDTO(); form.setName("Course"); form.setProblemIds(List.of(404L));
        CourseServiceImpl service = courseService(courses, links);
        assertThrows(BizException.class, () -> service.update(5L, form));
        verify(courses, never()).updateById(any(Course.class));
        verifyNoInteractions(links);
        assertThrows(BizException.class, () -> service.create(1L, form));
        verify(courses, never()).insert(any(Course.class));
    }

    @Test void validCourseSelectionKeepsOrder() {
        CourseMapper courses = mock(CourseMapper.class);
        CourseProblemMapper links = mock(CourseProblemMapper.class);
        when(problems.selectBatchIds(List.of(3L, 2L))).thenReturn(List.of(new Problem(), new Problem()));
        when(courses.insert(any(Course.class))).thenAnswer(c -> { c.<Course>getArgument(0).setId(5L); return 1; });
        CourseFormDTO form = new CourseFormDTO(); form.setName("Course"); form.setProblemIds(List.of(3L, 2L));
        assertEquals(5L, courseService(courses, links).create(1L, form));
        var captured = org.mockito.ArgumentCaptor.forClass(CourseProblem.class);
        verify(links, times(2)).insert(captured.capture());
        assertEquals(List.of(3L,2L), captured.getAllValues().stream().map(CourseProblem::getProblemId).toList());
    }

    private CourseServiceImpl courseService(CourseMapper courses, CourseProblemMapper links) {
        JoinCodeGenerator codes = mock(JoinCodeGenerator.class);
        when(codes.generate()).thenReturn("CODE2345");
        return new CourseServiceImpl(courses, links, mock(CourseMemberMapper.class),
                problems, selection, codes);
    }

    @Test void invalidContestSelectionCannotCreatePartialContest() {
        ContestMapper contests = mock(ContestMapper.class);
        ContestProblemMapper links = mock(ContestProblemMapper.class);
        ContestServiceImpl service = new ContestServiceImpl(contests, links, problems,
                mock(SubmissionMapper.class), mock(UserMapper.class), selection);
        ContestFormDTO dto = new ContestFormDTO(); dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(dto.getStartTime().plusHours(1)); dto.setProblemIds(List.of(2L,2L));
        assertThrows(BizException.class, () -> service.create(1L, dto));
        verifyNoInteractions(contests, links);
    }

    @Test void testCasesCannotBeImportedOrReadForMissingProblem() {
        TestCaseMapper cases = mock(TestCaseMapper.class);
        TestCaseServiceImpl service = new TestCaseServiceImpl(cases, problems);
        TestCaseImportDTO dto = new TestCaseImportDTO(); dto.setCases(List.of());
        assertThrows(BizException.class, () -> service.batchImport(404L, dto));
        assertThrows(BizException.class, () -> service.listByProblem(404L));
        verifyNoInteractions(cases);
    }

    @Test void testCaseRequestRequiresCasesAndValidElementsButAllowsEmptyText() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            TestCaseImportDTO dto = new TestCaseImportDTO();
            assertFalse(validator.validate(dto).isEmpty());
            dto.setCases(Arrays.asList((TestCaseImportDTO.Item)null));
            assertFalse(validator.validate(dto).isEmpty());
            TestCaseImportDTO.Item item = new TestCaseImportDTO.Item();
            item.setInput(""); item.setExpectedOutput(""); item.setScore(0); item.setIsSample(false);
            dto.setCases(List.of(item)); assertTrue(validator.validate(dto).isEmpty());
            item.setScore(-1); assertFalse(validator.validate(dto).isEmpty());
        }
    }

    @Test void referencedProblemMustBeTakenOfflineInsteadOfDeletingHistory() {
        TestCaseMapper cases = mock(TestCaseMapper.class);
        when(problems.selectById(3L)).thenReturn(new Problem());
        when(problems.hasReferences(3L)).thenReturn(true);
        assertThrows(BizException.class, () -> new ProblemServiceImpl(problems, cases).delete(3L));
        verify(problems, never()).deleteById(anyLong()); verifyNoInteractions(cases);
    }
}
