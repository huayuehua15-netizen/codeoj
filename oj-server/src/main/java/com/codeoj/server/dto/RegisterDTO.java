package com.codeoj.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 */
@Data
public class RegisterDTO {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅允许字母、数字与下划线")
    private String username;

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @Size(max = 50)
    private String nickname;

    @Email
    @Size(max = 100)
    private String email;
}
