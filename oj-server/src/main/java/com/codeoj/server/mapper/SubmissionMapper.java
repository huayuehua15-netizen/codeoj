package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    /**
     * 消费者领取任务：排队中 → 判题中（论文 5.4，幂等：WHERE status=0）。
     * 影响行数为零说明重复投递，直接确认跳过。
     */
    @Update("UPDATE submission SET status = 1 WHERE id = #{id} AND status = 0")
    int markJudging(@Param("id") Long id);

    /**
     * 结果回写条件更新（论文 5.4）：仅判题中可流转到终态，
     * 从机制上保证重复投递不会重复计分或覆盖已定终态。
     */
    @Update("UPDATE submission " +
            "   SET status = #{status}, time_used_ms = #{timeUsedMs}, memory_used_kb = #{memoryUsedKb}, " +
            "       error_info = #{errorInfo}, judged_at = NOW() " +
            " WHERE id = #{id} AND status = 1")
    int updateFinalResult(@Param("id") Long id,
                          @Param("status") int status,
                          @Param("timeUsedMs") Integer timeUsedMs,
                          @Param("memoryUsedKb") Integer memoryUsedKb,
                          @Param("errorInfo") String errorInfo);

    /**
     * 个人通过率统计（论文 5.6）：按难度分组聚合提交记录。
     * submission 无难度列，JOIN problem 取值。
     */
    @Select("SELECT p.difficulty AS difficulty, COUNT(*) AS submitCount, " +
            "       SUM(s.status = 2) AS acceptedCount " +
            "  FROM submission s JOIN problem p ON s.problem_id = p.id " +
            " WHERE s.user_id = #{userId} " +
            " GROUP BY p.difficulty")
    List<DifficultyStatRow> statisticsByUser(@Param("userId") Long userId);

    /**
     * 难度维度聚合行。
     */
    class DifficultyStatRow {
        private Integer difficulty;
        private Long submitCount;
        private Long acceptedCount;

        public Integer getDifficulty() {
            return difficulty;
        }

        public Long getSubmitCount() {
            return submitCount;
        }

        public Long getAcceptedCount() {
            return acceptedCount;
        }
    }
}
