package com.codeoj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    /** 消费者领取任务：排队中 → 判题中（论文 5.4，幂等：WHERE status=0） */
    @Update("UPDATE submission SET status = 1 WHERE id = #{id} AND status = 0")
    int markJudging(@Param("id") Long id);

    /** 结果回写条件更新（论文 5.4）：仅判题中可流转到终态，保证幂等 */
    @Update("UPDATE submission " +
            "   SET status = #{status}, time_used_ms = #{timeUsedMs}, memory_used_kb = #{memoryUsedKb}, " +
            "       error_info = #{errorInfo}, judged_at = NOW() " +
            " WHERE id = #{id} AND status = 1")
    int updateFinalResult(@Param("id") Long id,
                          @Param("status") int status,
                          @Param("timeUsedMs") Integer timeUsedMs,
                          @Param("memoryUsedKb") Integer memoryUsedKb,
                          @Param("errorInfo") String errorInfo);
}
