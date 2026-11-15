package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 排行榜分页响应（论文 5.6：ZSet 倒序取前 N 名，以用户表补齐昵称等展示信息）。
 */
@Data
@AllArgsConstructor
public class LeaderboardVO {

    private long total;
    private List<Item> items;

    /**
     * 排行项。
     */
    @Data
    @AllArgsConstructor
    public static class Item {
        private Long userId;
        private String username;
        /** AC 题数（ZSet score） */
        private long acCount;
        private long rank;
    }
}
