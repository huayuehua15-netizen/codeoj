package com.codeoj.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱修改请求（论文表 3-1 个人信息维护）。
 */
@Data
public class UpdateEmailDTO {

    @Email
    @Size(max = 100)
    private String email;
}
