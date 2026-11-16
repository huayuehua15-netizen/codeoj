package com.codeoj.server.vo;

import com.codeoj.common.entity.JudgeCase;
import com.codeoj.common.enums.SubmissionStatus;
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
        // judge_case.status 在库中允许 NULL（历史数据/判题中断），
        // 直接交给 SubmissionStatus.of(int) 会拆箱 NPE → 整个详情接口 50000。
        Integer status = jc.getStatus();
        return JudgeCaseVO.builder()
                .caseIndex(jc.getCaseIndex())
                .status(status)
                .statusDesc(status == null ? "未知" : SubmissionStatus.of(status).getDesc())
                .timeUsedMs(jc.getTimeUsedMs())
                .memoryUsedKb(jc.getMemoryUsedKb())
                .build();
    }
}
