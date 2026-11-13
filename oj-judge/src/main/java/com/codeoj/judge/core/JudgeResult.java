package com.codeoj.judge.core;

import com.codeoj.common.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 一次判题的整体结果。
 */
@Data
@Builder
public class JudgeResult {

    private SubmissionStatus status;
    /** 最大用例耗时（毫秒） */
    private Integer timeUsedMs;
    /** 最大用例内存（KB） */
    private Integer memoryUsedKb;
    /** 通过用例数 / 总用例数 */
    private int passedCases;
    private int totalCases;
    /** 编译/运行错误摘要 */
    private String errorInfo;
    /** 用例级明细 */
    private List<CaseResult> cases;

    public boolean isAccepted() {
        return status == SubmissionStatus.ACCEPTED;
    }
}
