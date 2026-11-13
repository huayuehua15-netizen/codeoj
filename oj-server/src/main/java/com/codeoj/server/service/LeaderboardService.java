package com.codeoj.server.service;

import com.codeoj.server.vo.LeaderboardVO;

/**
 * 排行榜服务（论文 5.6：Redis ZSet oj:leaderboard:ac 为唯一数据源，分页倒序取前 N 名）。
 */
public interface LeaderboardService {

    /** 排行榜分页 */
    LeaderboardVO page(long page, long size);

    /** AC 时更新排行（oj-judge 调用同名逻辑，此处供对账任务使用） */
    void incr(Long userId);
}
