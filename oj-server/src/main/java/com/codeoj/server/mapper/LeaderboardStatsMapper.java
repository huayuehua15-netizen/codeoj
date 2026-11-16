package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜读侧聚合（论文 5.6）。
 *
 * <p>排行榜的<b>解题数</b>以 Redis ZSet 为准（写侧由 oj-judge AC 时 ZINCRBY），
 * 但 ZSet 只有 userId → 分数，无法表达"同分时谁先做到"。
 * 因此读侧按需从 submission 聚合出：总提交数、AC 提交数、最后活跃时间、
 * 以及<b>首次达到当前解题数的时间</b>（同分排序的依据）。
 */
@Mapper
public interface LeaderboardStatsMapper extends BaseMapper<Submission> {

    /**
     * 批量取用户的提交统计（一次查询，避免 N+1）。
     *
     * @param userIds 本页展示的用户 ID
     */
    @Select("<script>"
            + "SELECT s.user_id AS userId, "
            + "       COUNT(*) AS totalSubmit, "
            + "       SUM(s.status = 2) AS acceptedSubmit, "
            + "       MAX(s.created_at) AS lastSubmitAt "
            + "  FROM submission s "
            + " WHERE s.user_id IN "
            + "   <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + " GROUP BY s.user_id"
            + "</script>")
    List<UserSubmitStat> submitStatsByUsers(@Param("userIds") List<Long> userIds);

    /**
     * 每个用户"首次 AC 每题"的时间，用于计算同分排名。
     *
     * <p>思路：先按 (user_id, problem_id) 取该题最早一次 AC 时间，
     * 再按 user_id 聚合出全部首次 AC 时间，交由 Java 侧排序取第 N 个。
     * 只查有 AC 记录的用户，配合 userIds 限定范围。
     */
    @Select("<script>"
            + "SELECT first_ac.user_id AS userId, first_ac.ac_at AS acAt "
            + "  FROM (SELECT s.user_id AS user_id, s.problem_id AS problem_id, "
            + "               MIN(s.created_at) AS ac_at "
            + "          FROM submission s "
            + "         WHERE s.status = 2 "
            + "           AND s.user_id IN "
            + "          <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "         GROUP BY s.user_id, s.problem_id) first_ac "
            + " ORDER BY first_ac.user_id, first_ac.ac_at"
            + "</script>")
    List<FirstAcRow> firstAcTimes(@Param("userIds") List<Long> userIds);

    /**
     * 全站概览：上榜人数、总提交、总 AC 提交。
     *
     * <p><b>不能加 {@code WHERE status = 2}</b>——那会把总提交数也过滤成只剩 AC，
     * 通过率恒等于 100%，失去意义。上榜人数用 {@code COUNT(DISTINCT user_id)}
     * 配合 {@code MAX(...)} 判断"有 AC 记录"。
     */
    @Select("SELECT COUNT(DISTINCT CASE WHEN s.status = 2 THEN s.user_id END) AS solverCount, "
            + "       COUNT(*) AS totalSubmit, "
            + "       SUM(s.status = 2) AS acceptedSubmit "
            + "  FROM submission s")
    OverviewRow overview();

    /**
     * 用户提交统计行。
     */
    class UserSubmitStat {
        private Long userId;
        private Long totalSubmit;
        private Long acceptedSubmit;
        private LocalDateTime lastSubmitAt;

        public Long getUserId() {
            return userId;
        }

        public Long getTotalSubmit() {
            return totalSubmit;
        }

        public Long getAcceptedSubmit() {
            return acceptedSubmit;
        }

        public LocalDateTime getLastSubmitAt() {
            return lastSubmitAt;
        }
    }

    /**
     * 首次 AC 时间行。
     */
    class FirstAcRow {
        private Long userId;
        private LocalDateTime acAt;

        public Long getUserId() {
            return userId;
        }

        public LocalDateTime getAcAt() {
            return acAt;
        }
    }

    /**
     * 全站概览行。
     */
    class OverviewRow {
        private Long solverCount;
        private Long totalSubmit;
        private Long acceptedSubmit;

        public Long getSolverCount() {
            return solverCount;
        }

        public Long getTotalSubmit() {
            return totalSubmit;
        }

        public Long getAcceptedSubmit() {
            return acceptedSubmit;
        }
    }
}
