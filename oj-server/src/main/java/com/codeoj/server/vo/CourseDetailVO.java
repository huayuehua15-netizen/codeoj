package com.codeoj.server.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生课程详情：课程进度 + 每题个人状态。
 *
 * <p>{@code status} 为 {@code ac} / {@code attempted} / {@code none}；
 * {@code onTime} 仅在已 AC 时有意义，截止后首次 AC 会标记为 false。
 */
public record CourseDetailVO(
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
        LocalDateTime lastSubmitAt,
        List<ProblemProgress> problems) {

    public record ProblemProgress(
            Long problemId,
            String title,
            Integer difficulty,
            List<String> tags,
            int sortOrder,
            String status,
            LocalDateTime firstAcAt,
            long attempts,
            LocalDateTime lastAt,
            Boolean onTime) {
    }
}
