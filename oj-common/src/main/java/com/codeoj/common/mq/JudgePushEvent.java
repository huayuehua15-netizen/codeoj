package com.codeoj.common.mq;

import com.codeoj.common.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 判题结果事件（论文 4.2/5.5）：oj-judge 回写数据库后发布，
 * 由 oj-server 消费并经 WebSocket convertAndSendToUser 定向推送。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgePushEvent {

    private Long submissionId;
    private Long userId;
    private Long problemId;
    private SubmissionStatus status;
    /** 最大用例耗时（毫秒） */
    private Integer timeUsedMs;
    /** 最大用例内存（KB） */
    private Integer memoryUsedKb;
    /** 通过用例数 / 总用例数 */
    private Integer passedCases;
    private Integer totalCases;
    /** 编译/运行错误摘要 */
    private String errorInfo;
}
