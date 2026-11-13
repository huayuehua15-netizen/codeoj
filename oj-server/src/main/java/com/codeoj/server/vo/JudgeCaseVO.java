package com.codeoj.server.vo;

import com.codeoj.common.entity.JudgeCase;
import lombok.Builder;
import lombok.Data;

/**
 * 用例级判题结果（论文 5.7：用例序号、状态、耗时、内存）。
 */
@Data
@Builder
public class JudgeCaseVO {

    private Integer caseIndex;
    private Integer status;
    private String statusDesc;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;

    public static JudgeCaseVO of(JudgeCase jc) {
        return JudgeCaseVO.builder()
                .caseIndex(jc.getCaseIndex())
                .status(jc.getStatus())
                .statusDesc(com.codeoj.common.enums.SubmissionStatus.of(jc.getStatus()).getDesc())
                .timeUsedMs(jc.getTimeUsedMs())
                .memoryUsedKb(jc.getMemoryUsedKb())
                .build();
    }
}
