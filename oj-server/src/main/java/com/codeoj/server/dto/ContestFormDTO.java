package com.codeoj.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛创建/编辑请求（论文 7.2 展望落地）。
 */
@Data
public class ContestFormDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    /** 比赛题目 ID 列表（按顺序对应字母题号） */
    private List<Long> problemIds;
}
