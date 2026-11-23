package com.codeoj.server.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师学情面板中的单个学生。
 *
 * <p>{@code statuses} 与课程题目列表一一对应（未提交的题也显式返回 none），
 * 前端可以直接渲染成"学生 × 题目"矩阵，不用再猜缺失项。
 */
public record CourseMemberVO(
        Long userId,
        String username,
        String avatar,
        LocalDateTime joinedAt,
        long solvedCount,
        long onTimeSolvedCount,
        long submitCount,
        LocalDateTime lastSubmitAt,
        List<ProblemStatus> statuses) {

    public record ProblemStatus(
            Long problemId,
            String status,
            LocalDateTime firstAcAt,
            long attempts,
            LocalDateTime lastAt,
            Boolean onTime) {
    }
}
