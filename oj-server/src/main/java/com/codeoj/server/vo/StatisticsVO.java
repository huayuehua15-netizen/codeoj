package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 个人通过率统计（论文 5.6：按难度分组聚合提交记录）。
 */
@Data
@AllArgsConstructor
public class StatisticsVO {

    /** 按难度分组的统计项 */
    private List<Item> byDifficulty;

    /** 汇总 */
    private long totalSubmit;
    private long totalAccepted;

    /**
     * 难度维度统计项。
     */
    @Data
    @AllArgsConstructor
    public static class Item {
        /** 1=简单，2=中等，3=困难 */
        private Integer difficulty;
        private String difficultyLabel;
        private long submit;
        private long accepted;
    }
}
