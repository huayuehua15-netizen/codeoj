package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.persistence.SubmissionLeaseOperations;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission>, SubmissionLeaseOperations {

    /**
     * 个人通过率统计（论文 5.6）：按难度分组聚合提交记录。
     * submission 无难度列，JOIN problem 取值。
     *
     * <p>同时给出该难度的题目总数与已 AC 题目数，用于计算"难度覆盖度"
     * （做了 5 道简单题里的 3 道 vs 做了 1 道困难题里的 1 道）。
     */
    @Select("SELECT d.difficulty, COALESCE(s.submitCount, 0) AS submitCount, "
            + "COALESCE(s.acceptedCount, 0) AS acceptedCount, "
            + "COALESCE(s.solvedCount, 0) AS solvedCount, "
            + "COALESCE(s.availableSolvedCount, 0) AS availableSolvedCount, "
            + "COALESCE(p.totalCount, 0) AS totalCount "
            + "FROM (SELECT 1 AS difficulty UNION ALL SELECT 2 UNION ALL SELECT 3) d "
            + "LEFT JOIN (SELECT p.difficulty, COUNT(*) AS submitCount, "
            + "SUM(s.status = 2) AS acceptedCount, "
            + "COUNT(DISTINCT CASE WHEN s.status = 2 THEN s.problem_id END) AS solvedCount, "
            + "COUNT(DISTINCT CASE WHEN s.status = 2 AND p.enabled = 1 THEN s.problem_id END) AS availableSolvedCount "
            + "FROM submission s JOIN problem p ON s.problem_id = p.id "
            + "WHERE s.user_id = #{userId} GROUP BY p.difficulty) s ON s.difficulty = d.difficulty "
            + "LEFT JOIN (SELECT difficulty, COUNT(*) AS totalCount FROM problem "
            + "WHERE enabled = 1 GROUP BY difficulty) p ON p.difficulty = d.difficulty "
            + "ORDER BY d.difficulty")
    List<DifficultyStatRow> statisticsByUser(@Param("userId") Long userId);

    /**
     * 按判题状态分组计数（个人）：哪些类型的错误最多。
     */
    @Select("SELECT s.status AS status, COUNT(*) AS cnt "
            + "  FROM submission s "
            + " WHERE s.user_id = #{userId} "
            + " GROUP BY s.status ORDER BY s.status")
    List<StatusCountRow> statusCountByUser(@Param("userId") Long userId);

    /**
     * 个人题目覆盖：提交过的不同题目数。
     */
    @Select("SELECT COUNT(DISTINCT s.problem_id) FROM submission s WHERE s.user_id = #{userId}")
    long attemptedCountByUser(@Param("userId") Long userId);

    /**
     * 难度维度聚合行。
     */
    class DifficultyStatRow {
        private Integer difficulty;
        private Long submitCount;
        private Long acceptedCount;
        private Long solvedCount;
        private Long availableSolvedCount;
        private Long totalCount;

        public Integer getDifficulty() {
            return difficulty;
        }

        public Long getSubmitCount() {
            return submitCount;
        }

        public Long getAcceptedCount() {
            return acceptedCount;
        }

        public Long getSolvedCount() {
            return solvedCount;
        }

        public Long getAvailableSolvedCount() {
            return availableSolvedCount;
        }

        public Long getTotalCount() {
            return totalCount;
        }
    }

    /**
     * 状态维度聚合行。
     */
    class StatusCountRow {
        private Integer status;
        private Long cnt;

        public Integer getStatus() {
            return status;
        }

        public Long getCnt() {
            return cnt;
        }
    }

    /**
     * 排行榜对账数据源（论文 5.6）：每个用户的 AC 题目数（去重题目）。
     */
    @Select("SELECT s.user_id AS userId, COUNT(DISTINCT s.problem_id) AS acCount " +
            "  FROM submission s WHERE s.status = 2 GROUP BY s.user_id")
    List<AcCountRow> acCountByUser();

    /**
     * 排行榜对账聚合行。
     */
    class AcCountRow {
        private Long userId;
        private Long acCount;

        public Long getUserId() {
            return userId;
        }

        public Long getAcCount() {
            return acCount;
        }
    }

    /**
     * 提交趋势（个人统计页图表 / 个人主页热力图）：近 N 天按日聚合提交数与通过数。
     */
    @Select("SELECT DATE(s.created_at) AS date, COUNT(*) AS total, " +
            "       SUM(s.status = 2) AS accepted " +
            "  FROM submission s " +
            " WHERE s.user_id = #{userId} " +
            "   AND s.created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            " GROUP BY DATE(s.created_at) ORDER BY DATE(s.created_at)")
    List<Map<String, Object>> trendByUser(@Param("userId") Long userId, @Param("days") int days);

    /**
     * 语言分布（个人主页，API.md v1.9 §4.4b）：各语言提交数与通过数，按提交数降序。
     * language 是 Language 枚举 code，由前端映射为显示名。
     */
    @Select("SELECT s.language AS language, COUNT(*) AS total, " +
            "       SUM(s.status = 2) AS accepted " +
            "  FROM submission s " +
            " WHERE s.user_id = #{userId} " +
            " GROUP BY s.language ORDER BY total DESC")
    List<Map<String, Object>> languageDistByUser(@Param("userId") Long userId);

    /**
     * 错题本（API.md v1.10 §4.4c）：尝试过且从未 AC 的题目，按最近一次提交倒序。
     *
     * <p>窗口函数一步取每题的聚合与"最近一次提交"：ROW_NUMBER 取最新行拿到
     * lastStatus/lastLanguage/lastAt，COUNT(*) OVER 得尝试次数，
     * MAX(status=2) OVER 判断是否 AC 过（=0 即错题）。已 AC 的题不进错题本。
     */
    @Select("SELECT t.problem_id AS problemId, p.title AS title, p.difficulty AS difficulty, " +
            "       t.attempts AS attempts, t.status AS lastStatus, " +
            "       t.language AS lastLanguage, t.created_at AS lastAt " +
            "  FROM (SELECT s.problem_id, s.status, s.language, s.created_at, " +
            "               COUNT(*) OVER (PARTITION BY s.problem_id) AS attempts, " +
            "               ROW_NUMBER() OVER (PARTITION BY s.problem_id ORDER BY s.id DESC) AS rn, " +
            "               MAX(s.status = 2) OVER (PARTITION BY s.problem_id) AS solvedFlag " +
            "          FROM submission s WHERE s.user_id = #{userId}) t " +
            "  JOIN problem p ON p.id = t.problem_id " +
            " WHERE t.rn = 1 AND t.solvedFlag = 0 " +
            " ORDER BY t.created_at DESC")
    List<Map<String, Object>> wrongBookByUser(@Param("userId") Long userId);
}
