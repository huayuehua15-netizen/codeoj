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
     *
     * <p><b>口径限制（review P2-21，已知并接受）</b>：LEFT JOIN 只按题目关联，
     * 没有"用户 ∈ 该课程班级"的约束——本项目没有选课关系表，无法界定"班级成员"，
     * 因此统计的是<b>全站</b>提交过该题的用户数，而非严格意义的班级内部人数。
     * 课程题目一般来自教学题库、站内用户即学生，演示口径下两者近似；
     * 若未来引入选课表，在 JOIN 上补 {@code AND s.user_id IN (SELECT ...)} 即可收紧。
     *
     * <p>{@code GROUP BY} 带 {@code p.title}：它函数依赖于 cp.problem_id
     * （经 p.id 主键 JOIN），MySQL 的 ONLY_FULL_GROUP_BY 函数依赖推断可以放行。
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
