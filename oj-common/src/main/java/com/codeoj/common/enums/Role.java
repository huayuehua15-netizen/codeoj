package com.codeoj.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色（论文表 4-1）：0=普通用户（学生），1=管理员（教师）。
 */
@Getter
@AllArgsConstructor
public enum Role {

    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    private final int code;
    private final String label;

    public static Role of(int code) {
        for (Role r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        throw new IllegalArgumentException("未知角色: " + code);
    }
}
