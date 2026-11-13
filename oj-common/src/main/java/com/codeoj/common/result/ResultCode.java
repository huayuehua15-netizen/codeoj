package com.codeoj.common.result;

import lombok.Getter;

/**
 * 业务状态码（论文 4.5）：code 为 0 表示成功，4xxxx/5xxxx 分别表示客户端与服务端错误。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),

    UNAUTHORIZED(40100, "未登录或登录已过期"),
    /** 连续 5 次密码错误，锁定 15 分钟（论文 4.7） */
    ACCOUNT_LOCKED(40101, "失败次数过多，账号已锁定 15 分钟"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),

    /** 提交过于频繁（幂等锁拦截，论文 5.3） */
    SUBMIT_TOO_FREQUENT(42901, "提交过于频繁，请稍后再试"),
    /** 请求过于频繁（限流） */
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试"),
    /** 题目不存在或未上架（论文 5.3：40002） */
    PROBLEM_UNAVAILABLE(40002, "题目不存在或未上架"),
    /** 语言不被该题目允许 / 代码超限 */
    SUBMIT_INVALID(40003, "提交参数不合法"),

    BAD_REQUEST(40000, "请求参数错误"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
