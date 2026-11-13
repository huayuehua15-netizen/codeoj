package com.codeoj.judge.mq;

import com.codeoj.common.constant.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 排行榜更新（论文 5.6）：排行榜以 Redis ZSet oj:leaderboard:ac 为唯一数据源，
 * AC 时对应用户分数自增（oj-judge 侧）。
 */
@Component
public class LeaderboardUpdater {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardUpdater.class);

    private final RedisTemplate<String, Object> redis;

    public LeaderboardUpdater(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /** AC 时更新排行：ZINCRBY oj:leaderboard:ac（member=userId，score=AC 题数） */
    public void incr(Long userId) {
        redis.opsForZSet().incrementScore(RedisKeys.LEADERBOARD_AC, String.valueOf(userId), 1);
        log.debug("leaderboard incr, userId={}", userId);
    }
}
