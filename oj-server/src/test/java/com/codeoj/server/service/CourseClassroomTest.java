package com.codeoj.server.service;

import com.codeoj.common.entity.Course;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.exception.BizException;
import com.codeoj.server.dto.CourseFormDTO;
import com.codeoj.server.mapper.CourseMapper;
import com.codeoj.server.mapper.CourseMemberMapper;
import com.codeoj.server.mapper.CourseProblemMapper;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.impl.CourseServiceImpl;
import com.codeoj.server.util.JoinCodeGenerator;
import com.codeoj.server.vo.CourseDetailVO;
import com.codeoj.server.vo.CourseRosterVO;
import com.codeoj.server.vo.StudentCourseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 课程班级核心行为：邀请码、唯一性、加入幂等、权限、学情矩阵。
 * 纯 Mockito 单测，不依赖数据库；SQL 口径由 docs/API.md 与迁移脚本约束。
 */
class CourseClassroomTest {

    private CourseMapper courses;
    private CourseProblemMapper links;
    private CourseMemberMapper members;
    private ProblemMapper problems;
    private JoinCodeGenerator codes;
    private CourseServiceImpl service;

    @BeforeEach
    void setUp() {
        courses = mock(CourseMapper.class);
        links = mock(CourseProblemMapper.class);
        members = mock(CourseMemberMapper.class);
        problems = mock(ProblemMapper.class);
        codes = mock(JoinCodeGenerator.class);
        when(codes.generate()).thenReturn("CODE2345");
        service = new CourseServiceImpl(courses, links, members, problems,
                new ProblemSelectionValidator(problems), codes);
    }

    @Test
    void createGeneratesJoinCodeAndWritesDeadline() {
        CourseFormDTO form = new CourseFormDTO();
        form.setName(" 数据结构第 3 周 ");
        form.setProblemIds(List.of());
        LocalDateTime deadline = LocalDateTime.of(2026, 12, 1, 23, 59, 59);
        form.setDeadline(deadline);
        when(courses.insert(any(Course.class))).thenAnswer(inv -> {
            inv.<Course>getArgument(0).setId(9L);
            return 1;
        });

        assertEquals(9L, service.create(1L, form));
        verify(courses).updateClassMeta(9L, "CODE2345", deadline);
        verify(courses).insert(org.mockito.ArgumentMatchers.<Course>argThat(
                c -> "数据结构第 3 周".equals(c.getName())));
    }

    @Test
    void editingWithoutCodeKeepsExistingCode() {
        CourseMapper.CourseRow row = row(5L, "KEEP1234");
        when(courses.selectCourseRow(5L)).thenReturn(row);
        when(courses.selectById(5L)).thenReturn(new Course());
        when(courses.selectCourseByJoinCode("KEEP1234")).thenReturn(row);
        CourseFormDTO form = new CourseFormDTO();
        form.setName("课程");
        form.setJoinCode("");
        form.setProblemIds(List.of());

        service.update(5L, form);

        verify(courses).updateClassMeta(5L, "KEEP1234", null);
        verify(codes, never()).generate();
    }

    @Test
    void reusingOwnCodeIsAllowedButAnotherCoursesCodeIsRejected() {
        CourseMapper.CourseRow own = row(5L, "OWN12345");
        when(courses.selectCourseRow(5L)).thenReturn(own);
        when(courses.selectById(5L)).thenReturn(new Course());
        when(courses.selectCourseByJoinCode("OWN12345")).thenReturn(own);

        CourseFormDTO same = new CourseFormDTO();
        same.setName("课程");
        same.setJoinCode("own12345"); // 大小写归一后与自身相同
        same.setProblemIds(List.of());
        assertDoesNotThrow(() -> service.update(5L, same));
        verify(courses).updateClassMeta(5L, "OWN12345", null);

        CourseMapper.CourseRow other = row(6L, "OTHER123");
        when(courses.selectCourseByJoinCode("OTHER123")).thenReturn(other);
        CourseFormDTO conflict = new CourseFormDTO();
        conflict.setName("课程");
        conflict.setJoinCode("OTHER123");
        conflict.setProblemIds(List.of());
        assertThrows(BizException.class, () -> service.update(5L, conflict));
    }

    @Test
    void joinIsIdempotentAndRejectsUnknownCode() {
        CourseMapper.CourseRow row = row(8L, "JOIN2345");
        when(courses.selectCourseByJoinCode("JOIN2345")).thenReturn(row);
        when(members.exists(8L, 2L)).thenReturn(1);
        StudentCourseVO vo = service.join(new LoginUser(2L, 0, "jti"), " join2345 ");
        assertNotNull(vo);
        assertEquals("课程 8", vo.name());
        verify(members, never()).join(anyLong(), anyLong());

        when(courses.selectCourseByJoinCode("NOPE2345")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.join(new LoginUser(2L, 0, "jti"), "NOPE2345"));
        assertEquals("邀请码无效，请核对后重试", ex.getMessage());
    }

    @Test
    void nonMemberCannotReadCourseButAdminCanPreview() {
        CourseMapper.CourseRow row = row(3L, "CLASS234");
        when(courses.selectCourseRow(3L)).thenReturn(row);
        when(courses.selectById(3L)).thenReturn(new Course());
        when(members.exists(3L, 2L)).thenReturn(0);
        when(links.selectList(any())).thenReturn(List.of());
        when(problems.selectBatchIds(anyList())).thenReturn(List.of());

        assertThrows(BizException.class,
                () -> service.detail(3L, new LoginUser(2L, 0, "jti")));
        CourseDetailVO preview = service.detail(3L, new LoginUser(1L, 1, "jti"));
        assertEquals(3L, preview.id());
        assertEquals(0, preview.solvedCount());
    }

    @Test
    void detailMarksLateAcceptedProblem() {
        CourseMapper.CourseRow row = row(4L, "DEAD2345");
        row.setDeadline(LocalDateTime.of(2026, 11, 1, 0, 0));
        when(courses.selectCourseRow(4L)).thenReturn(row);
        when(courses.selectById(4L)).thenReturn(new Course());
        when(members.exists(4L, 2L)).thenReturn(1);
        var cp = new com.codeoj.common.entity.CourseProblem();
        cp.setProblemId(11L);
        cp.setSortOrder(1);
        when(links.selectList(any())).thenReturn(List.of(cp));
        Problem p = new Problem();
        p.setId(11L);
        p.setTitle("迟交题");
        p.setDifficulty(2);
        when(problems.selectBatchIds(List.of(11L))).thenReturn(List.of(p));
        var progress = new CourseMemberMapper.ProgressRow();
        progress.setProblemId(11L);
        progress.setAc(1);
        progress.setFirstAcAt(LocalDateTime.of(2026, 11, 2, 0, 0));
        progress.setAttempts(2L);
        when(members.progressForUser(4L, 2L)).thenReturn(List.of(progress));
        var summary = new CourseMapper.StudentCourseRow();
        summary.setId(4L);
        summary.setSolvedCount(1L);
        summary.setOnTimeSolvedCount(0L);
        when(courses.selectStudentCourse(2L, 4L)).thenReturn(summary);

        CourseDetailVO detail = service.detail(4L, new LoginUser(2L, 0, "jti"));

        assertEquals("ac", detail.problems().get(0).status());
        assertFalse(detail.problems().get(0).onTime());
        assertEquals(1, detail.solvedCount());
    }

    @Test
    void rosterFillsEveryProblemForEveryMemberEvenWhenNoSubmission() {
        CourseMapper.CourseRow row = row(6L, "ROST2345");
        when(courses.selectCourseRow(6L)).thenReturn(row);
        when(courses.selectById(6L)).thenReturn(new Course());
        var cp = new com.codeoj.common.entity.CourseProblem();
        cp.setProblemId(21L);
        cp.setSortOrder(1);
        when(links.selectList(any())).thenReturn(List.of(cp));
        Problem p = new Problem();
        p.setId(21L);
        p.setTitle("第一题");
        when(problems.selectBatchIds(List.of(21L))).thenReturn(List.of(p));
        var member = new CourseMemberMapper.RosterRow();
        member.setUserId(2L);
        member.setUsername("student");
        member.setSolvedCount(0L);
        member.setOnTimeSolvedCount(0L);
        member.setSubmitCount(0L);
        when(members.roster(6L)).thenReturn(List.of(member));
        when(members.progressForCourse(6L)).thenReturn(List.of());

        CourseRosterVO roster = service.roster(6L);

        assertEquals(1, roster.members().size());
        assertEquals(1, roster.members().get(0).statuses().size());
        assertEquals("none", roster.members().get(0).statuses().get(0).status());
        assertEquals("ROST2345", roster.course().joinCode());
    }

    @Test
    void rotateCodeRejectsCollisionAndPersistsNewCode() {
        CourseMapper.CourseRow row = row(7L, "OLD12345");
        row.setDeadline(LocalDateTime.of(2027, 1, 1, 0, 0));
        when(courses.selectCourseRow(7L)).thenReturn(row);
        when(codes.generate()).thenReturn("NEW23456");
        when(courses.selectCourseByJoinCode("NEW23456")).thenReturn(null);

        assertEquals("NEW23456", service.rotateJoinCode(7L));
        verify(courses).updateClassMeta(7L, "NEW23456", row.getDeadline());
    }

    @Test
    void removingUnknownMemberReportsNotFound() {
        when(courses.selectCourseRow(10L)).thenReturn(row(10L, "MEM12345"));
        when(members.remove(10L, 99L)).thenReturn(0);
        assertThrows(BizException.class, () -> service.removeMember(10L, 99L));
    }

    private static CourseMapper.CourseRow row(Long id, String code) {
        CourseMapper.CourseRow row = new CourseMapper.CourseRow();
        row.setId(id);
        row.setName("课程 " + id);
        row.setJoinCode(code);
        row.setMemberCount(0L);
        row.setProblemCount(0L);
        return row;
    }
}
