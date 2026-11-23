package com.codeoj.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生凭邀请码加入课程班级。code 统一由服务端转大写并 trim。
 */
@Data
public class JoinCourseDTO {

    @NotBlank
    @Size(max = 12)
    private String code;
}
