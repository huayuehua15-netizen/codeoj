package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 班级完成情况统计（论文 3.3 / 测试 F-15）：课程内每题的提交人数与通过人数。
     * 提交人数/通过人数均按去重用户计。
     */
    @Select("SELECT cp.problem_id AS problemId, p.title AS title, " +
            "       COUNT(DISTINCT s.user_id) AS submittedUsers, " +
            "       COUNT(DISTINCT CASE WHEN s.status = 2 THEN s.user_id END) AS acceptedUsers " +
            "  FROM course_problem cp " +
            "  JOIN problem p ON cp.problem_id = p.id " +
            "  LEFT JOIN submission s ON s.problem_id = cp.problem_id " +
            " WHERE cp.course_id = #{courseId} " +
            " GROUP BY cp.problem_id, p.title, cp.sort_order " +
            " ORDER BY cp.sort_order")
    List<CompletionRow> completionStats(@Param("courseId") Long courseId);

    /**
     * 完成情况统计行。
     */
    class CompletionRow {
        private Long problemId;
        private String title;
        private Long submittedUsers;
        private Long acceptedUsers;

        public Long getProblemId() {
            return problemId;
        }

        public String getTitle() {
            return title;
        }

        public Long getSubmittedUsers() {
            return submittedUsers;
        }

        public Long getAcceptedUsers() {
            return acceptedUsers;
        }
    }
}
