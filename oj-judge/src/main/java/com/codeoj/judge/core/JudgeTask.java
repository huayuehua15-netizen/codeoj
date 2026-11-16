package com.codeoj.judge.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 判题任务：消费者按 submissionId 回查数据库组装（论文 4.6：代码与用例由消费者回查数据库获取）。
 */
@Data
@Builder
public class JudgeTask {

    private Long submissionId;
    /** Unique lease token; keeps retry workspaces and containers separate. */
    private String executionToken;
    private Long userId;
    private Long problemId;

    /** c/cpp/java/python3 */
    private String language;

    /** 提交代码 */
    private String code;

    /** 题目时间限制（毫秒） */
    private Integer timeLimitMs;

    /** 题目内存限制（MB） */
    private Integer memoryLimitMb;

    /** 测试用例列表（标准输入 + 期望输出） */
    private List<CaseInput> cases;

    @Data
    @Builder
    public static class CaseInput {
        private int index;
        private String input;
        private String expectedOutput;
    }
}
