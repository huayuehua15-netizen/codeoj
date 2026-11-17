package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Problem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
    @Select("SELECT EXISTS(SELECT 1 FROM submission WHERE problem_id=#{id}) "
            + "OR EXISTS(SELECT 1 FROM course_problem WHERE problem_id=#{id}) "
            + "OR EXISTS(SELECT 1 FROM contest_problem WHERE problem_id=#{id})")
    boolean hasReferences(@Param("id") Long id);
}
