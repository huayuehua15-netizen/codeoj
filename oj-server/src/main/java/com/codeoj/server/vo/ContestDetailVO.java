package com.codeoj.server.vo;

import com.codeoj.common.entity.Problem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛详情：基础信息 + 按顺序的题目列表（字母题号 A/B/C…）。
 */
@Data
@AllArgsConstructor
public class ContestDetailVO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0=未开始 1=进行中 2=已结束 */
    private Integer status;

    private List<Item> problems;

    @Data
    @AllArgsConstructor
    public static class Item {
        /** 字母题号：A/B/C… */
        private String label;
        private Long problemId;
        private String title;
        private Integer difficulty;
    }
}
