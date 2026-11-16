package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 个人提交统计分析（论文 5.6）。
 *
 * <p>从三个维度刻画一个用户的练习情况：
 * <ul>
 *   <li><b>难度维度</b>：简单/中等/困难各自的提交、通过、通过率、题目覆盖</li>
 *   <li><b>状态维度</b>：AC / WA / TLE / MLE / RE / CE 的分布（快速看出短板类型）</li>
 *   <li><b>汇总</b>：总提交、总通过、通过率、已解决题数、总尝试题数</li>
 * </ul>
 */
@Data
@AllArgsConstructor
public class StatisticsVO {

    /** 按难度分组的统计项 */
    private List<Item> byDifficulty;

    /** 按判题状态分组的统计项 */
    private List<StatusItem> byStatus;

    /** 汇总 */
    private long totalSubmit;
    private long totalAccepted;
    /** 总通过率（百分比，一位小数） */
    private double acceptRate;
    /** 已解决题目数（AC 过的不同题目） */
    private long solvedCount;
    /** 尝试过的题目数（提交过的不同题目） */
    private long attemptedCount;

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
        /** 该难度通过率（百分比，一位小数） */
        private double acceptRate;
        /** 该难度下已 AC 的题目数 */
        private long solvedCount;
        /** 该难度下的题目总数（题库口径） */
        private long totalCount;
    }

    /**
     * 判题状态维度统计项。
     */
    @Data
    @AllArgsConstructor
    public static class StatusItem {
        /** 状态码（见 CONTRACT §2） */
        private Integer status;
        /** 状态中文名，如"通过""答案错误" */
        private String statusDesc;
        private long count;
        /** 占该用户全部提交的百分比 */
        private double percent;
    }
}
