package com.codeoj.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代码提交请求（论文 5.3）：语言必须为题目允许的语言，代码 ≤64KB（服务端校验）。
 */
@Data
public class SubmitDTO {

    @NotNull
    private Long problemId;

    /** c/cpp/java/python3 */
    @NotBlank
    @Pattern(regexp = "^(c|cpp|java|python3)$", message = "不支持的语言")
    private String language;

    @NotBlank
    @Size(max = 65536, message = "代码不得超过 64KB")
    private String code;
}
