package com.codeoj.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代码提交请求（论文 5.3）。语言合法性与代码长度（≤64KB，按 UTF-8 字节）
 * 统一在 SubmissionService.validate 校验并返回 40003——避免 @Valid 层（40000）
 * 与业务层（40003）两套标准不一致。
 */
@Data
public class SubmitDTO {

    @NotNull
    private Long problemId;

    /** c/cpp/java/python3/js */
    @NotBlank
    private String language;

    @NotBlank
    private String code;
}
