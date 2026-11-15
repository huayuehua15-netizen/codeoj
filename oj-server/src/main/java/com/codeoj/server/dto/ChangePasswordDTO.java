package com.codeoj.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密码修改请求（论文表 3-1 个人信息维护）：验证旧密码后 BCrypt 存储新密码。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 32, message = "新密码长度 6-32 位")
    private String newPassword;
}
