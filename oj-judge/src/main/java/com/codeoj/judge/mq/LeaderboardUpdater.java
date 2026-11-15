package com.codeoj.judge.mq;

import com.codeoj.common.constant.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 排行榜更新（论文 5.6）：排行榜以 Redis ZSet oj:leaderboard:ac 为唯一数据源，
 * AC 时对应用户分数自增（oj-judge 侧）。
 * member 一律为裸字符串 userId（StringRedisTemplate），与 oj-server 读侧/对账任务
 * 序列化格式一致——两端格式不一致会导致同一用户出现多个 member（已踩坑，见 CONTRACT §5）。
 */
@Component
public class LeaderboardUpdater {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardUpdater.class);

    private final StringRedisTemplate redis;

    public LeaderboardUpdater(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** AC 时更新排行：ZINCRBY oj:leaderboard:ac（member=userId 裸串，score=AC 题数） */
    public void incr(Long userId) {
        redis.opsForZSet().incrementScore(RedisKeys.LEADERBOARD_AC, String.valueOf(userId), 1);
        log.debug("leaderboard incr, userId={}", userId);
    }
}
