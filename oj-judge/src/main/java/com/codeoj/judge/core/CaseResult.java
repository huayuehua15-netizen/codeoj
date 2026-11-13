package com.codeoj.judge.core;

import com.codeoj.common.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 单用例判题结果。
 */
@Data
@Builder
public class CaseResult {

    private int index;
    private SubmissionStatus status;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String errorInfo;
}
