package com.codeoj.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提交语言（论文表 4-4 / 表 5-2）：c / cpp / java / python3 四种。
 */
@Getter
@AllArgsConstructor
public enum Language {

    C("c", "C", "codeoj/c:gcc13"),
    CPP("cpp", "C++", "codeoj/cpp:gcc13"),
    JAVA("java", "Java", "codeoj/java:openjdk17"),
    PYTHON3("python3", "Python3", "codeoj/python:3.12"),
    JS("js", "JavaScript", "codeoj/node:20");

    /** 提交记录与题目 languages 字段中的存储值 */
    private final String code;
    private final String label;
    /** 判题沙箱镜像（论文表 5-2） */
    private final String image;

    public static Language of(String code) {
        for (Language l : values()) {
            if (l.code.equals(code)) {
                return l;
            }
        }
        throw new IllegalArgumentException("不支持的语言: " + code);
    }
}
