package com.codeoj.server.vo;

import java.util.List;

/**
 * 教师班级学情：课程信息 + 题目列表 + 学生列表。
 */
public record CourseRosterVO(
        CourseVO course,
        List<ProblemBrief> problems,
        List<CourseMemberVO> members) {

    public record ProblemBrief(
            Long problemId,
            String title,
            Integer difficulty,
            int sortOrder) {
    }
}
