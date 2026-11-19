package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 竞赛排名项（ACM 赛制简化）：解题数降序 → 总罚时升序。
 */
@Data
@AllArgsConstructor
public class ContestRankVO {

    private int rank;
    private Long userId;
    private String username;
    /** 解题数 */
    private int solved;
    /** 总罚时（分钟） */
    private long penalty;
    /** 各题状态摘要：A AC 85min (-1) / 未解题目错误 ×2 …（纯文本，前端 mono 原样渲染） */
    private String detail;
}
