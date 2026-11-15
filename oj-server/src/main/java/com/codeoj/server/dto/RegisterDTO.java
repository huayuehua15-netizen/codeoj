package com.codeoj.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求：用户名 + 密码 + 邮箱（昵称字段已去除，业务上昵称默认与用户名一致）。
 * 用户名支持中文、字母、数字与下划线。
 */
@Data
public class RegisterDTO {

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z0-9_]+$",
            message = "用户名仅允许中文、字母、数字与下划线")
    private String username;

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @Email
    @Size(max = 100)
    private String email;
}
