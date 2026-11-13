package com.codeoj.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 课程创建/编辑请求（论文 3.3：课程实验题组管理）。
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
}
