package com.codeoj.server.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 课程班级邀请码生成器。
 *
 * <p>8 位、32 进制字母表：去掉 I/O/0/1 这类口述时容易混淆的字符，
 * 老师念给学生、学生手输都不容易出错。使用 {@link SecureRandom}，
 * 避免按时间戳/自增可预测；真正的唯一性仍由 {@code uk_course_join_code} 兜底。
 */
@Component
public class JoinCodeGenerator {

    /** 去掉 I、O、0、1 的 32 字符表 */
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int LENGTH = 8;

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * 教师手填邀请码的合法格式：4–12 位大写字母或数字（服务层会先转大写）。
     * 保留较短长度是为了兼容已经线下分发的旧码。
     */
    public static boolean isValid(String code) {
        return code != null && code.matches("[A-Z0-9]{4,12}");
    }
}
