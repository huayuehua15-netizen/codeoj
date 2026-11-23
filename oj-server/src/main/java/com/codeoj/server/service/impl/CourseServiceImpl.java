package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.Course;
import com.codeoj.common.entity.CourseProblem;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.CourseFormDTO;
import com.codeoj.server.mapper.CourseMapper;
import com.codeoj.server.mapper.CourseMapper.CourseRow;
import com.codeoj.server.mapper.CourseMapper.StudentCourseRow;
import com.codeoj.server.mapper.CourseMemberMapper;
import com.codeoj.server.mapper.CourseProblemMapper;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.CourseService;
import com.codeoj.server.service.ProblemSelectionValidator;
import com.codeoj.server.util.JoinCodeGenerator;
import com.codeoj.server.vo.CourseDetailVO;
import com.codeoj.server.vo.CourseMemberVO;
import com.codeoj.server.vo.CourseRosterVO;
import com.codeoj.server.vo.CourseVO;
import com.codeoj.server.vo.StudentCourseVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 课程班级服务：邀请码、成员、截止时间、学情聚合。
 *
 * <p>设计取舍：
 * <ul>
 *   <li>课程表新增字段通过 {@link CourseMapper#updateClassMeta} 写入，不改 oj-common 的
 *       {@link Course} 实体——共用区保持稳定，另一侧判题/管理代码不受影响；</li>
 *   <li>学生进度只统计"加入班级之后"的提交。加入前的历史 AC 不计入课程完成度，
 *       避免学生加入瞬间就"已完成"而失去练习意义；</li>
 *   <li>截止时间只影响"按时完成"口径，不阻断提交：迟交仍可练习，但教师能区分。</li>
 * </ul>
 */
@Service
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseProblemMapper courseProblemMapper;
    private final CourseMemberMapper memberMapper;
    private final ProblemMapper problemMapper;
    private final ProblemSelectionValidator problemSelection;
    private final JoinCodeGenerator joinCodeGenerator;

    public CourseServiceImpl(CourseMapper courseMapper,
                             CourseProblemMapper courseProblemMapper,
                             CourseMemberMapper memberMapper,
                             ProblemMapper problemMapper,
                             ProblemSelectionValidator problemSelection,
                             JoinCodeGenerator joinCodeGenerator) {
        this.courseMapper = courseMapper;
        this.courseProblemMapper = courseProblemMapper;
        this.memberMapper = memberMapper;
        this.problemMapper = problemMapper;
        this.problemSelection = problemSelection;
        this.joinCodeGenerator = joinCodeGenerator;
    }

    @Override
    @Transactional
    public Long create(Long teacherId, CourseFormDTO dto) {
        problemSelection.validate(dto.getProblemIds());
        String joinCode = resolveJoinCode(null, null, dto.getJoinCode());
        Course course = new Course();
        course.setName(dto.getName().trim());
        course.setDescription(trimToNull(dto.getDescription()));
        course.setTeacherId(teacherId);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(course);
        courseMapper.updateClassMeta(course.getId(), joinCode, dto.getDeadline());
        bindProblems(course.getId(), dto.getProblemIds());
        return course.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CourseFormDTO dto) {
        CourseRow current = requireRow(id);
        problemSelection.validate(dto.getProblemIds());
        Course course = getById(id);
        course.setName(dto.getName().trim());
        course.setDescription(trimToNull(dto.getDescription()));
        course.setUpdatedAt(LocalDateTime.now());
        String joinCode = resolveJoinCode(id, current.getJoinCode(), dto.getJoinCode());
        courseMapper.updateById(course);
        courseMapper.updateClassMeta(id, joinCode, dto.getDeadline());
        courseProblemMapper.delete(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, id));
        bindProblems(id, dto.getProblemIds());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireRow(id);
        memberMapper.removeByCourse(id);
        courseProblemMapper.delete(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, id));
        courseMapper.deleteById(id);
    }

    @Override
    public List<CourseVO> listAll() {
        return courseMapper.selectCourseRows().stream().map(this::toCourseVO).toList();
    }

    @Override
    public Course getById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return course;
    }

    @Override
    public List<CourseProblem> problems(Long courseId) {
        getById(courseId);
        return courseProblemMapper.selectList(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, courseId)
                .orderByAsc(CourseProblem::getSortOrder));
    }

    @Override
    public List<CompletionItem> completion(Long courseId) {
        requireRow(courseId);
        List<CourseMapper.CompletionRow> rows = courseMapper.completionStats(courseId);
        return rows.stream()
                .map(row -> new CompletionItem(row.getProblemId(), row.getTitle(),
                        asLong(row.getMemberCount()), asLong(row.getSubmittedUsers()),
                        asLong(row.getAcceptedUsers()), asLong(row.getOnTimeAcceptedUsers())))
                .toList();
    }

    @Override
    public List<StudentCourseVO> myCourses(LoginUser user) {
        return courseMapper.selectStudentCourses(user.userId()).stream()
                .map(this::toStudentCourseVO)
                .toList();
    }

    @Override
    @Transactional
    public StudentCourseVO join(LoginUser user, String code) {
        String normalized = normalizeCode(code);
        if (normalized == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "请输入邀请码");
        }
        CourseMapper.CourseRow course = courseMapper.selectCourseByJoinCode(normalized);
        if (course == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "邀请码无效，请核对后重试");
        }
        if (memberMapper.exists(course.getId(), user.userId()) == 0) {
            memberMapper.join(course.getId(), user.userId());
        }
        StudentCourseRow row = courseMapper.selectStudentCourse(user.userId(), course.getId());
        return row == null ? emptyStudentCourse(course) : toStudentCourseVO(row);
    }

    @Override
    public CourseDetailVO detail(Long courseId, LoginUser user) {
        CourseRow course = requireRow(courseId);
        boolean member = memberMapper.exists(courseId, user.userId()) > 0;
        if (!member && !user.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        List<CourseProblem> links = problems(courseId);
        Map<Long, Problem> problems = loadProblems(links);
        StudentCourseRow summary = member ? courseMapper.selectStudentCourse(user.userId(), courseId) : null;
        Map<Long, CourseMemberMapper.ProgressRow> progress = member
                ? toProgressMap(memberMapper.progressForUser(courseId, user.userId()))
                : Map.of();

        List<CourseDetailVO.ProblemProgress> items = new ArrayList<>(links.size());
        for (CourseProblem link : links) {
            Problem problem = problems.get(link.getProblemId());
            CourseMemberMapper.ProgressRow p = progress.get(link.getProblemId());
            String status = statusOf(p);
            LocalDateTime firstAcAt = p == null ? null : p.getFirstAcAt();
            boolean ac = "ac".equals(status);
            items.add(new CourseDetailVO.ProblemProgress(
                    link.getProblemId(),
                    problem == null ? "题目已下线" : problem.getTitle(),
                    problem == null ? null : problem.getDifficulty(),
                    problem == null || problem.getTags() == null ? List.of() : problem.getTags(),
                    link.getSortOrder(),
                    status,
                    firstAcAt,
                    p == null ? 0 : asLong(p.getAttempts()),
                    p == null ? null : p.getLastAt(),
                    ac ? isOnTime(firstAcAt, course.getDeadline()) : null));
        }

        return new CourseDetailVO(course.getId(), course.getName(), course.getDescription(),
                course.getTeacherName(), course.getDeadline(),
                asLong(course.getProblemCount()), asLong(course.getMemberCount()),
                summary == null ? 0 : asLong(summary.getSolvedCount()),
                summary == null ? 0 : asLong(summary.getOnTimeSolvedCount()),
                summary == null ? 0 : asLong(summary.getSubmitCount()),
                summary == null ? null : summary.getJoinedAt(),
                summary == null ? null : summary.getLastSubmitAt(),
                items);
    }

    @Override
    @Transactional
    public void leave(Long courseId, LoginUser user) {
        requireRow(courseId);
        if (memberMapper.remove(courseId, user.userId()) == 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "你尚未加入该课程");
        }
    }

    @Override
    public CourseRosterVO roster(Long courseId) {
        CourseRow course = requireRow(courseId);
        List<CourseProblem> links = problems(courseId);
        Map<Long, Problem> problems = loadProblems(links);
        List<CourseRosterVO.ProblemBrief> briefs = links.stream()
                .map(link -> {
                    Problem problem = problems.get(link.getProblemId());
                    return new CourseRosterVO.ProblemBrief(link.getProblemId(),
                            problem == null ? "题目已下线" : problem.getTitle(),
                            problem == null ? null : problem.getDifficulty(),
                            link.getSortOrder());
                })
                .toList();

        List<CourseMemberMapper.ProgressRow> progressRows = memberMapper.progressForCourse(courseId);
        Map<Long, Map<Long, CourseMemberMapper.ProgressRow>> progressByUser = new HashMap<>();
        for (CourseMemberMapper.ProgressRow row : progressRows) {
            progressByUser.computeIfAbsent(row.getUserId(), k -> new HashMap<>())
                    .put(row.getProblemId(), row);
        }

        List<CourseMemberVO> members = memberMapper.roster(courseId).stream()
                .map(row -> {
                    Map<Long, CourseMemberMapper.ProgressRow> own =
                            progressByUser.getOrDefault(row.getUserId(), Map.of());
                    List<CourseMemberVO.ProblemStatus> statuses = briefs.stream()
                            .map(brief -> {
                                CourseMemberMapper.ProgressRow p = own.get(brief.problemId());
                                String status = statusOf(p);
                                LocalDateTime firstAcAt = p == null ? null : p.getFirstAcAt();
                                boolean ac = "ac".equals(status);
                                return new CourseMemberVO.ProblemStatus(
                                        brief.problemId(), status, firstAcAt,
                                        p == null ? 0 : asLong(p.getAttempts()),
                                        p == null ? null : p.getLastAt(),
                                        ac ? isOnTime(firstAcAt, course.getDeadline()) : null);
                            })
                            .toList();
                    return new CourseMemberVO(row.getUserId(), row.getUsername(), row.getAvatar(),
                            row.getJoinedAt(), asLong(row.getSolvedCount()),
                            asLong(row.getOnTimeSolvedCount()), asLong(row.getSubmitCount()),
                            row.getLastSubmitAt(), statuses);
                })
                // SQL 已排序；这里再兜一次，保证测试/缓存数据乱序时教师看到的顺序仍稳定
                .sorted(Comparator.comparingLong(CourseMemberVO::solvedCount).reversed()
                        .thenComparing(Comparator.comparingLong(CourseMemberVO::onTimeSolvedCount).reversed())
                        .thenComparing(CourseMemberVO::joinedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new CourseRosterVO(toCourseVO(course), briefs, members);
    }

    @Override
    @Transactional
    public void removeMember(Long courseId, Long userId) {
        requireRow(courseId);
        if (memberMapper.remove(courseId, userId) == 0) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该学生不在这个班级");
        }
    }

    @Override
    @Transactional
    public String rotateJoinCode(Long courseId) {
        CourseRow course = requireRow(courseId);
        String code = generateUniqueCode();
        courseMapper.updateClassMeta(courseId, code, course.getDeadline());
        return code;
    }

    // ---------------------------------------------------------------- helpers

    private CourseRow requireRow(Long id) {
        CourseMapper.CourseRow row = courseMapper.selectCourseRow(id);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return row;
    }

    private CourseVO toCourseVO(CourseMapper.CourseRow row) {
        return new CourseVO(row.getId(), row.getName(), row.getDescription(), row.getTeacherId(),
                row.getTeacherName(), row.getJoinCode(), row.getDeadline(),
                asLong(row.getMemberCount()), asLong(row.getProblemCount()),
                row.getCreatedAt(), row.getUpdatedAt());
    }

    private StudentCourseVO toStudentCourseVO(CourseMapper.StudentCourseRow row) {
        return new StudentCourseVO(row.getId(), row.getName(), row.getDescription(),
                row.getTeacherName(), row.getDeadline(),
                asLong(row.getProblemCount()), asLong(row.getMemberCount()),
                asLong(row.getSolvedCount()), asLong(row.getOnTimeSolvedCount()),
                asLong(row.getSubmitCount()), row.getJoinedAt(), row.getLastSubmitAt());
    }

    private StudentCourseVO emptyStudentCourse(CourseMapper.CourseRow row) {
        return new StudentCourseVO(row.getId(), row.getName(), row.getDescription(),
                row.getTeacherName(), row.getDeadline(),
                asLong(row.getProblemCount()), asLong(row.getMemberCount()),
                0, 0, 0, LocalDateTime.now(), null);
    }

    private void bindProblems(Long courseId, List<Long> problemIds) {
        if (problemIds == null) {
            return;
        }
        for (int i = 0; i < problemIds.size(); i++) {
            CourseProblem cp = new CourseProblem();
            cp.setCourseId(courseId);
            cp.setProblemId(problemIds.get(i));
            cp.setSortOrder(i + 1);
            courseProblemMapper.insert(cp);
        }
    }

    private Map<Long, Problem> loadProblems(List<CourseProblem> links) {
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = links.stream().map(CourseProblem::getProblemId).toList();
        Map<Long, Problem> result = new HashMap<>();
        // selectBatchIds 对空集合返回空列表；这里已经挡过空
        for (Problem p : problemMapper.selectBatchIds(ids)) {
            result.put(p.getId(), p);
        }
        return result;
    }

    private Map<Long, CourseMemberMapper.ProgressRow> toProgressMap(
            List<CourseMemberMapper.ProgressRow> rows) {
        Map<Long, CourseMemberMapper.ProgressRow> map = new HashMap<>();
        for (CourseMemberMapper.ProgressRow row : rows) {
            map.put(row.getProblemId(), row);
        }
        return map;
    }

    private static String statusOf(CourseMemberMapper.ProgressRow row) {
        if (row == null) {
            return "none";
        }
        return row.getAc() != null && row.getAc() > 0 ? "ac" : "attempted";
    }

    private static boolean isOnTime(LocalDateTime firstAcAt, LocalDateTime deadline) {
        return firstAcAt != null && (deadline == null || !firstAcAt.isAfter(deadline));
    }

    /**
     * 解析教师填写的邀请码：
     * <ul>
     *   <li>填写了：校验格式 + 全局唯一，直接采用；</li>
     *   <li>没填写但课程已有码：保留原码（编辑其他字段不会误换码）；</li>
     *   <li>没填写且没有码：自动生成。</li>
     * </ul>
     */
    private String resolveJoinCode(Long courseId, String existing, String requestedRaw) {
        String requested = normalizeCode(requestedRaw);
        if (requested != null) {
            if (!JoinCodeGenerator.isValid(requested)) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                        "邀请码需为 4–12 位字母或数字");
            }
            CourseMapper.CourseRow other = courseMapper.selectCourseByJoinCode(requested);
            if (other != null && (courseId == null || !courseId.equals(other.getId()))) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                        "邀请码已被其他课程使用，请换一个");
            }
            return requested;
        }
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return generateUniqueCode();
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 12; i++) {
            String code = joinCodeGenerator.generate();
            if (courseMapper.selectCourseByJoinCode(code) == null) {
                return code;
            }
        }
        throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "邀请码生成失败，请重试");
    }

    private static String normalizeCode(String raw) {
        if (raw == null) {
            return null;
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        return code.isEmpty() ? null : code;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static long asLong(Long value) {
        return value == null ? 0 : value;
    }
}
