package com.codeoj.server.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程班级成员与学情数据访问。
 *
 * <p>所有聚合都以 {@code course_member} 为边界：教师看到的完成度只包含
 * 真正加入班级的学生，不再混入全站路过的提交。
 */
@Mapper
public interface CourseMemberMapper {

    /** 幂等加入：重复点击/重复提交不会产生第二行。 */
    @Insert("INSERT IGNORE INTO course_member (course_id, user_id, joined_at) " +
            "VALUES (#{courseId}, #{userId}, NOW())")
    int join(@Param("courseId") Long courseId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM course_member WHERE course_id = #{courseId} AND user_id = #{userId}")
    int exists(@Param("courseId") Long courseId, @Param("userId") Long userId);

    @Delete("DELETE FROM course_member WHERE course_id = #{courseId} AND user_id = #{userId}")
    int remove(@Param("courseId") Long courseId, @Param("userId") Long userId);

    @Delete("DELETE FROM course_member WHERE course_id = #{courseId}")
    int removeByCourse(@Param("courseId") Long courseId);

    /**
     * 班级成员概览：完成数、按时完成数、提交次数、最后活跃时间。
     *
     * <p>{@code LEFT JOIN course_problem} 保证"零提交/零题目"的成员不会从名单里消失；
     * 排序按完成数降序，教师一眼看到谁掉队。
     */
    @Select("SELECT m.user_id AS userId, u.username AS username, u.avatar AS avatar, " +
            "       m.joined_at AS joinedAt, " +
            "       COUNT(DISTINCT s.problem_id) AS attemptedCount, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 THEN s.problem_id END) AS solvedCount, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 AND " +
            "            (c.deadline IS NULL OR s.created_at <= c.deadline) THEN s.problem_id END) AS onTimeSolvedCount, " +
            "       COUNT(s.id) AS submitCount, MAX(s.created_at) AS lastSubmitAt " +
            "  FROM course_member m " +
            "  JOIN course c ON c.id = m.course_id " +
            "  JOIN `user` u ON u.id = m.user_id " +
            "  LEFT JOIN course_problem cp ON cp.course_id = m.course_id " +
            "  LEFT JOIN submission s ON s.problem_id = cp.problem_id AND s.user_id = m.user_id " +
            "       AND s.created_at >= m.joined_at " +
            " WHERE m.course_id = #{courseId} " +
            " GROUP BY m.user_id, u.username, u.avatar, m.joined_at, c.deadline " +
            " ORDER BY solvedCount DESC, onTimeSolvedCount DESC, m.joined_at ASC")
    List<RosterRow> roster(@Param("courseId") Long courseId);

    /**
     * 某学生在课程内每题的首次 AC / 尝试次数 / 最后提交。
     * 只返回有提交记录的题目，缺失项在服务层补成 none。
     */
    @Select("SELECT s.problem_id AS problemId, " +
            "       MAX(CASE WHEN s.status = 2 THEN 1 ELSE 0 END) AS ac, " +
            "       MIN(CASE WHEN s.status = 2 THEN s.created_at END) AS firstAcAt, " +
            "       COUNT(*) AS attempts, MAX(s.created_at) AS lastAt " +
            "  FROM submission s " +
            "  JOIN course_member m ON m.course_id = #{courseId} AND m.user_id = #{userId} " +
            "  JOIN course_problem cp ON cp.problem_id = s.problem_id AND cp.course_id = #{courseId} " +
            " WHERE s.user_id = #{userId} AND s.created_at >= m.joined_at " +
            " GROUP BY s.problem_id")
    List<ProgressRow> progressForUser(@Param("courseId") Long courseId,
                                      @Param("userId") Long userId);

    /**
     * 班级所有成员在课程题目上的进度，一次查询返回（教师矩阵）。
     */
    @Select("SELECT s.user_id AS userId, s.problem_id AS problemId, " +
            "       MAX(CASE WHEN s.status = 2 THEN 1 ELSE 0 END) AS ac, " +
            "       MIN(CASE WHEN s.status = 2 THEN s.created_at END) AS firstAcAt, " +
            "       COUNT(*) AS attempts, MAX(s.created_at) AS lastAt " +
            "  FROM submission s " +
            "  JOIN course_member m ON m.user_id = s.user_id AND m.course_id = #{courseId} " +
            "  JOIN course_problem cp ON cp.problem_id = s.problem_id AND cp.course_id = #{courseId} " +
            " WHERE s.created_at >= m.joined_at " +
            " GROUP BY s.user_id, s.problem_id")
    List<ProgressRow> progressForCourse(@Param("courseId") Long courseId);

    class RosterRow {
        private Long userId;
        private String username;
        private String avatar;
        private LocalDateTime joinedAt;
        private Long attemptedCount;
        private Long solvedCount;
        private Long onTimeSolvedCount;
        private Long submitCount;
        private LocalDateTime lastSubmitAt;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public LocalDateTime getJoinedAt() { return joinedAt; }
        public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
        public Long getAttemptedCount() { return attemptedCount; }
        public void setAttemptedCount(Long attemptedCount) { this.attemptedCount = attemptedCount; }
        public Long getSolvedCount() { return solvedCount; }
        public void setSolvedCount(Long solvedCount) { this.solvedCount = solvedCount; }
        public Long getOnTimeSolvedCount() { return onTimeSolvedCount; }
        public void setOnTimeSolvedCount(Long onTimeSolvedCount) { this.onTimeSolvedCount = onTimeSolvedCount; }
        public Long getSubmitCount() { return submitCount; }
        public void setSubmitCount(Long submitCount) { this.submitCount = submitCount; }
        public LocalDateTime getLastSubmitAt() { return lastSubmitAt; }
        public void setLastSubmitAt(LocalDateTime lastSubmitAt) { this.lastSubmitAt = lastSubmitAt; }
    }

    class ProgressRow {
        private Long userId;
        private Long problemId;
        private Integer ac;
        private LocalDateTime firstAcAt;
        private Long attempts;
        private LocalDateTime lastAt;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public Integer getAc() { return ac; }
        public void setAc(Integer ac) { this.ac = ac; }
        public LocalDateTime getFirstAcAt() { return firstAcAt; }
        public void setFirstAcAt(LocalDateTime firstAcAt) { this.firstAcAt = firstAcAt; }
        public Long getAttempts() { return attempts; }
        public void setAttempts(Long attempts) { this.attempts = attempts; }
        public LocalDateTime getLastAt() { return lastAt; }
        public void setLastAt(LocalDateTime lastAt) { this.lastAt = lastAt; }
    }
}
