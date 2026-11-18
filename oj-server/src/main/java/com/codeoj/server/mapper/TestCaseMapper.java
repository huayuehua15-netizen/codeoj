package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {
    @Select("SELECT input, expected_output AS expectedOutput FROM test_case "
            + "WHERE problem_id=#{problemId} ORDER BY id LIMIT #{limit}")
    List<TestCase> listJudgeCases(@Param("problemId") Long problemId, @Param("limit") int limit);
}
