package com.codeoj.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程创建/编辑请求（论文 3.3：课程班级题组管理）。
 */
@Data
public class CourseFormDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    /** 课程内题目 ID 列表（按顺序） */
    private List<Long> problemIds;

    /**
     * 学生加入班级的邀请码；留空时新建课程自动生成、编辑课程保留原码。
     * 4–12 位字母数字，服务端统一转大写并校验唯一性。
     */
    @Size(max = 12)
    private String joinCode;

    /** 作业截止时间；null 表示不限时 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
}
