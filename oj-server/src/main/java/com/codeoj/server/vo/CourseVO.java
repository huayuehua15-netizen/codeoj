package com.codeoj.server.vo;

import java.time.LocalDateTime;

/**
 * 管理端课程行：在原有课程字段上补充班级信息（邀请码 / 截止时间 / 人数 / 题数）。
 */
public record CourseVO(
        Long id,
        String name,
        String description,
        Long teacherId,
        String teacherName,
        String joinCode,
        LocalDateTime deadline,
        long memberCount,
        long problemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
