package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜分页响应（论文 5.6）。
 *
 * <p>数据源为 Redis ZSet {@code oj:leaderboard:ac}（score = AC 题数），
 * 展示用字段（总提交、通过率、最后活跃）由数据库聚合补齐。
 *
 * <p><b>排名规则（专业 OJ 惯例）</b>：解题数降序 → **首次达成该解题数的时间升序**
 * → userId 升序（最终兜底，保证结果稳定可复现）。
 * 早期实现直接取 ZSet 倒序，同分时顺序取决于 member 字典序（即 userId 大小），
 * 没有任何业务含义——两个人都做了 4 题，谁排在前面完全随机。
 */
@Data
@AllArgsConstructor
public class LeaderboardVO {

    private long total;
    private List<Item> items;
    /** 全站概览（顶部统计条展示） */
    private Summary summary;

    /**
     * 排行项。
     */
    @Data
    @AllArgsConstructor
    public static class Item {
        private Long userId;
        private String username;
        /** 头像相对路径（API.md v1.8），null=未设置，前端回退首字母占位 */
        private String avatar;
        /** AC 题数（ZSet score） */
        private long acCount;
        /** 名次 */
        private long rank;
        /** 总提交次数 */
        private long totalSubmit;
        /** 通过率的分子（AC 提交次数） */
        private long acceptedSubmit;
        /** 通过率（百分比，0-100，保留一位小数） */
        private double acceptRate;
        /** 最后一次提交时间（活跃度展示） */
        private LocalDateTime lastSubmitAt;
    }

    /**
     * 全站概览。
     */
    @Data
    @AllArgsConstructor
    public static class Summary {
        /** 榜上人数（有 AC 记录的用户数） */
        private long solverCount;
        /** 最高解题数 */
        private long maxAcCount;
        /** 全站总提交数 */
        private long totalSubmit;
        /** 全站整体通过率（百分比） */
        private double overallAcceptRate;
    }
}
