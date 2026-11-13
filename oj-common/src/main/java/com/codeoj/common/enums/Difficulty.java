package com.codeoj.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目难度（论文表 4-2）：1=简单，2=中等，3=困难。
 */
@Getter
@AllArgsConstructor
public enum Difficulty {

    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难");

    private final int code;
    private final String label;

    public static Difficulty of(int code) {
        for (Difficulty d : values()) {
            if (d.code == code) {
                return d;
            }
        }
        throw new IllegalArgumentException("未知难度: " + code);
    }
}
