package com.codeoj.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 判题状态机（论文表 4-8）。
 * 状态仅允许单向流转：排队中(0) → 判题中(1) → 终态(2~8)，由条件更新保证幂等。
 */
@Getter
@AllArgsConstructor
public enum SubmissionStatus {

    /** 提交入库，等待消费 */
    QUEUING(0, "排队中"),
    /** 消费者领取任务 */
    JUDGING(1, "判题中"),
    /** 全部用例输出与期望一致 */
    ACCEPTED(2, "通过"),
    /** 首个用例输出不匹配（Fail-Fast） */
    WRONG_ANSWER(3, "答案错误"),
    /** 运行时间超过时间限制 */
    TIME_LIMIT_EXCEEDED(4, "超时"),
    /** 内存使用超过内存限制 */
    MEMORY_LIMIT_EXCEEDED(5, "超内存"),
    /** 非零退出码 / 段错误 / 异常 */
    RUNTIME_ERROR(6, "运行错误"),
    /** 编译失败 */
    COMPILE_ERROR(7, "编译错误"),
    /** 判题服务自身故障 */
    SYSTEM_ERROR(8, "系统错误");

    private final int code;
    private final String desc;

    /** 终态判定：状态 >= 2 即不可再流转 */
    public boolean isFinal() {
        return code >= ACCEPTED.code;
    }

    public static SubmissionStatus of(int code) {
        for (SubmissionStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知判题状态: " + code);
    }
}
