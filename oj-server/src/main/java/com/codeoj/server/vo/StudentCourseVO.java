package com.codeoj.server.vo;

import java.time.LocalDateTime;

/**
 * 学生视角的课程摘要：课程基本信息 + 我的完成情况。
 *
 * <p>{@code solvedCount} 统计历史 AC（含截止后迟交）；{@code onTimeSolvedCount}
 * 只统计截止前首次 AC。没有截止时间的课程两者相等。
 */
public record StudentCourseVO(
        Long id,
        String name,
        String description,
        String teacherName,
        LocalDateTime deadline,
        long problemCount,
        long memberCount,
        long solvedCount,
        long onTimeSolvedCount,
        long submitCount,
        LocalDateTime joinedAt,
        LocalDateTime lastSubmitAt) {
}
