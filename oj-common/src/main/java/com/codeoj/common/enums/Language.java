package com.codeoj.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提交语言（论文表 4-4 / 表 5-2）：c / cpp / java / python3 / js 五种。
 */
@Getter
@AllArgsConstructor
public enum Language {

    C("c", "C", "codeoj/c:gcc13", 16),
    CPP("cpp", "C++", "codeoj/cpp:gcc13", 16),
    JAVA("java", "Java", "codeoj/java:openjdk17", 64),
    PYTHON3("python3", "Python3", "codeoj/python:3.12", 16),
    JS("js", "JavaScript", "codeoj/node:20", 64);

    /** 提交记录与题目 languages 字段中的存储值 */
    private final String code;
    private final String label;
    /** 判题沙箱镜像（论文表 5-2） */
    private final String image;
    /** 支持该语言的题目内存下限，包含运行时基础开销。 */
    private final int minimumMemoryMb;

    public static Language of(String code) {
        for (Language l : values()) {
            if (l.code.equals(code)) {
                return l;
            }
        }
        throw new IllegalArgumentException("不支持的语言: " + code);
    }
}
