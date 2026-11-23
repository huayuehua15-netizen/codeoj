package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程 / 班级数据访问。
 *
 * <p>课程表在 {@link Course} 实体之外增加了 {@code join_code / deadline} 两个班级字段，
 * 实体保持不动（oj-common 属于双人共用区，避免为了展示字段打断另一侧）；
 * 需要这两个字段时走本 Mapper 的显式查询。所有自定义 SQL 都只读业务表，
 * 不触碰判题链路。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 管理端课程列表：课程基本信息 + 班级人数 + 题目数。
     */
    @Select("SELECT c.id AS id, c.name AS name, c.description AS description, " +
            "       c.teacher_id AS teacherId, u.username AS teacherName, " +
            "       c.join_code AS joinCode, c.deadline AS deadline, " +
            "       c.created_at AS createdAt, c.updated_at AS updatedAt, " +
            "       (SELECT COUNT(*) FROM course_member m WHERE m.course_id = c.id) AS memberCount, " +
            "       (SELECT COUNT(*) FROM course_problem cp WHERE cp.course_id = c.id) AS problemCount " +
            "  FROM course c " +
            "  LEFT JOIN `user` u ON u.id = c.teacher_id " +
            " ORDER BY c.id DESC")
    List<CourseRow> selectCourseRows();

    @Select("SELECT c.id AS id, c.name AS name, c.description AS description, " +
            "       c.teacher_id AS teacherId, u.username AS teacherName, " +
            "       c.join_code AS joinCode, c.deadline AS deadline, " +
            "       c.created_at AS createdAt, c.updated_at AS updatedAt, " +
            "       (SELECT COUNT(*) FROM course_member m WHERE m.course_id = c.id) AS memberCount, " +
            "       (SELECT COUNT(*) FROM course_problem cp WHERE cp.course_id = c.id) AS problemCount " +
            "  FROM course c " +
            "  LEFT JOIN `user` u ON u.id = c.teacher_id " +
            " WHERE c.id = #{id}")
    CourseRow selectCourseRow(@Param("id") Long id);

    @Select("SELECT c.id AS id, c.name AS name, c.description AS description, " +
            "       c.teacher_id AS teacherId, u.username AS teacherName, " +
            "       c.join_code AS joinCode, c.deadline AS deadline, " +
            "       c.created_at AS createdAt, c.updated_at AS updatedAt, " +
            "       (SELECT COUNT(*) FROM course_member m WHERE m.course_id = c.id) AS memberCount, " +
            "       (SELECT COUNT(*) FROM course_problem cp WHERE cp.course_id = c.id) AS problemCount " +
            "  FROM course c " +
            "  LEFT JOIN `user` u ON u.id = c.teacher_id " +
            " WHERE c.join_code = #{code}")
    CourseRow selectCourseByJoinCode(@Param("code") String code);

    /** 写入班级字段；与 BaseMapper 的 updateById 分开，避免实体字段缺失导致覆盖空值。 */
    @Update("UPDATE course SET join_code = #{joinCode}, deadline = #{deadline}, updated_at = NOW() " +
            " WHERE id = #{id}")
    int updateClassMeta(@Param("id") Long id,
                        @Param("joinCode") String joinCode,
                        @Param("deadline") LocalDateTime deadline);

    /**
     * 学生"我的课程"：一次查询拿到课程 + 我的完成度，避免按课程 N+1。
     *
     * <p>完成度口径：{@code solvedCount} 为历史 AC（含迟交）；
     * {@code onTimeSolvedCount} 为截止前首次 AC。课程无截止时间时两者相等。
     */
    @Select("SELECT c.id AS id, c.name AS name, c.description AS description, " +
            "       u.username AS teacherName, c.deadline AS deadline, " +
            "       m.joined_at AS joinedAt, " +
            "       COUNT(DISTINCT cp.problem_id) AS problemCount, " +
            "       (SELECT COUNT(*) FROM course_member cm WHERE cm.course_id = c.id) AS memberCount, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 THEN s.problem_id END) AS solvedCount, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 AND " +
            "            (c.deadline IS NULL OR s.created_at <= c.deadline) THEN s.problem_id END) AS onTimeSolvedCount, " +
            "       COUNT(s.id) AS submitCount, MAX(s.created_at) AS lastSubmitAt " +
            "  FROM course_member m " +
            "  JOIN course c ON c.id = m.course_id " +
            "  LEFT JOIN `user` u ON u.id = c.teacher_id " +
            "  LEFT JOIN course_problem cp ON cp.course_id = c.id " +
            "  LEFT JOIN submission s ON s.problem_id = cp.problem_id AND s.user_id = m.user_id " +
            "       AND s.created_at >= m.joined_at " +
            " WHERE m.user_id = #{userId} " +
            " GROUP BY c.id, c.name, c.description, u.username, c.deadline, m.joined_at " +
            " ORDER BY CASE WHEN c.deadline IS NULL THEN 2 " +
            "              WHEN c.deadline >= NOW() THEN 0 ELSE 1 END ASC, " +
            "          c.deadline ASC, c.id DESC")
    List<StudentCourseRow> selectStudentCourses(@Param("userId") Long userId);

    @Select("SELECT c.id AS id, c.name AS name, c.description AS description, " +
            "       u.username AS teacherName, c.deadline AS deadline, " +
            "       m.joined_at AS joinedAt, " +
            "       COUNT(DISTINCT cp.problem_id) AS problemCount, " +
            "       (SELECT COUNT(*) FROM course_member cm WHERE cm.course_id = c.id) AS memberCount, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 THEN s.problem_id END) AS solvedCount, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 AND " +
            "            (c.deadline IS NULL OR s.created_at <= c.deadline) THEN s.problem_id END) AS onTimeSolvedCount, " +
            "       COUNT(s.id) AS submitCount, MAX(s.created_at) AS lastSubmitAt " +
            "  FROM course_member m " +
            "  JOIN course c ON c.id = m.course_id " +
            "  LEFT JOIN `user` u ON u.id = c.teacher_id " +
            "  LEFT JOIN course_problem cp ON cp.course_id = c.id " +
            "  LEFT JOIN submission s ON s.problem_id = cp.problem_id AND s.user_id = m.user_id " +
            "       AND s.created_at >= m.joined_at " +
            " WHERE m.user_id = #{userId} AND c.id = #{courseId} " +
            " GROUP BY c.id, c.name, c.description, u.username, c.deadline, m.joined_at")
    StudentCourseRow selectStudentCourse(@Param("userId") Long userId,
                                         @Param("courseId") Long courseId);

    /**
     * 班级完成情况统计（论文 3.3 / 测试 F-15）：课程内每题的提交人数与通过人数。
     *
     * <p>口径已从"全站提交过该题的人"收紧为"课程成员"——review P2-21 的遗留项由
     * course_member 表解决。{@code onTimeAcceptedUsers} 额外统计截止前通过人数，
     * 让教师能区分"做完"与"按时做完"。
     */
    @Select("SELECT cp.problem_id AS problemId, p.title AS title, " +
            "       COUNT(DISTINCT m.user_id) AS memberCount, " +
            "       COUNT(DISTINCT CASE WHEN s.id IS NOT NULL THEN m.user_id END) AS submittedUsers, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 THEN m.user_id END) AS acceptedUsers, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 AND " +
            "            (c.deadline IS NULL OR s.created_at <= c.deadline) THEN m.user_id END) AS onTimeAcceptedUsers " +
            "  FROM course_problem cp " +
            "  JOIN problem p ON cp.problem_id = p.id " +
            "  JOIN course c ON c.id = cp.course_id " +
            "  LEFT JOIN course_member m ON m.course_id = cp.course_id " +
            "  LEFT JOIN submission s ON s.problem_id = cp.problem_id AND s.user_id = m.user_id " +
            "       AND s.created_at >= m.joined_at " +
            " WHERE cp.course_id = #{courseId} " +
            " GROUP BY cp.problem_id, p.title, cp.sort_order, c.deadline " +
            " ORDER BY cp.sort_order")
    List<CompletionRow> completionStats(@Param("courseId") Long courseId);

    /**
     * 管理端课程行。
     */
    class CourseRow {
        private Long id;
        private String name;
        private String description;
        private Long teacherId;
        private String teacherName;
        private String joinCode;
        private LocalDateTime deadline;
        private Long memberCount;
        private Long problemCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getTeacherId() { return teacherId; }
        public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
        public String getJoinCode() { return joinCode; }
        public void setJoinCode(String joinCode) { this.joinCode = joinCode; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
        public Long getMemberCount() { return memberCount; }
        public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
        public Long getProblemCount() { return problemCount; }
        public void setProblemCount(Long problemCount) { this.problemCount = problemCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    class StudentCourseRow {
        private Long id;
        private String name;
        private String description;
        private String teacherName;
        private LocalDateTime deadline;
        private LocalDateTime joinedAt;
        private Long problemCount;
        private Long memberCount;
        private Long solvedCount;
        private Long onTimeSolvedCount;
        private Long submitCount;
        private LocalDateTime lastSubmitAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
        public LocalDateTime getJoinedAt() { return joinedAt; }
        public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
        public Long getProblemCount() { return problemCount; }
        public void setProblemCount(Long problemCount) { this.problemCount = problemCount; }
        public Long getMemberCount() { return memberCount; }
        public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
        public Long getSolvedCount() { return solvedCount; }
        public void setSolvedCount(Long solvedCount) { this.solvedCount = solvedCount; }
        public Long getOnTimeSolvedCount() { return onTimeSolvedCount; }
        public void setOnTimeSolvedCount(Long onTimeSolvedCount) { this.onTimeSolvedCount = onTimeSolvedCount; }
        public Long getSubmitCount() { return submitCount; }
        public void setSubmitCount(Long submitCount) { this.submitCount = submitCount; }
        public LocalDateTime getLastSubmitAt() { return lastSubmitAt; }
        public void setLastSubmitAt(LocalDateTime lastSubmitAt) { this.lastSubmitAt = lastSubmitAt; }
    }

    class CompletionRow {
        private Long problemId;
        private String title;
        private Long memberCount;
        private Long submittedUsers;
        private Long acceptedUsers;
        private Long onTimeAcceptedUsers;

        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Long getMemberCount() { return memberCount; }
        public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
        public Long getSubmittedUsers() { return submittedUsers; }
        public void setSubmittedUsers(Long submittedUsers) { this.submittedUsers = submittedUsers; }
        public Long getAcceptedUsers() { return acceptedUsers; }
        public void setAcceptedUsers(Long acceptedUsers) { this.acceptedUsers = acceptedUsers; }
        public Long getOnTimeAcceptedUsers() { return onTimeAcceptedUsers; }
        public void setOnTimeAcceptedUsers(Long onTimeAcceptedUsers) { this.onTimeAcceptedUsers = onTimeAcceptedUsers; }
    }
}
