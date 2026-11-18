package com.codeoj.judge.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.persistence.SubmissionLeaseOperations;
import org.apache.ibatis.annotations.*;
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission>, SubmissionLeaseOperations {
    @Select("SELECT CONVERT(payload USING utf8mb4) FROM judge_problem_snapshot WHERE snapshot_key=#{key}")
    String findJudgeSnapshot(@Param("key") String key);

    @Select("SELECT COUNT(DISTINCT problem_id) FROM submission WHERE user_id=#{userId} AND status=2")
    long countAcceptedProblems(@Param("userId") Long userId);
}
