package com.codeoj.server.task;

import com.codeoj.common.constant.RedisKeys;
import com.codeoj.server.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 排行榜异步对账任务（论文 5.6）：ZSet oj:leaderboard:ac 为准，通过与数据库汇总
 * 对比纠偏，保证最终一致——覆盖 ZINCRBY 重复计分、脏 member（用户已删）、丢消息等场景。
 */
@Component
public class LeaderboardReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardReconcileTask.class);

    private final StringRedisTemplate redis;
    private final SubmissionMapper submissionMapper;

    public LeaderboardReconcileTask(StringRedisTemplate redis,
                                    SubmissionMapper submissionMapper) {
        this.redis = redis;
        this.submissionMapper = submissionMapper;
    }

    /** 每 5 分钟对账一次 */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        Map<String, Long> expected = new HashMap<>();
        for (SubmissionMapper.AcCountRow row : submissionMapper.acCountByUser()) {
            expected.put(String.valueOf(row.getUserId()), row.getAcCount());
        }

        // 一次 rangeWithScores(0,-1) 取回 member + 分数（review P2-19）：原先先 range 拿
        // member 集合，再对每个 member 单独 score() 比对，N 个用户就是 N+1 次往返；
        // 对账是全量扫描，往返次数应固定为 1 次。
        Set<ZSetOperations.TypedTuple<String>> entries =
                redis.opsForZSet().rangeWithScores(RedisKeys.LEADERBOARD_AC, 0, -1);
        Map<String, Double> actual = new HashMap<>();
        if (entries != null) {
            for (ZSetOperations.TypedTuple<String> t : entries) {
                if (t.getValue() != null) {
                    actual.put(t.getValue(), t.getScore());
                }
            }
        }
        int removed = 0;
        int corrected = 0;

        // 1. ZSet 中的脏 member（DB 无此用户/无 AC）删除
        for (String member : actual.keySet()) {
            if (!expected.containsKey(member)) {
                redis.opsForZSet().remove(RedisKeys.LEADERBOARD_AC, member);
                removed++;
            }
        }

        // 2. 期望值与 ZSet 不符的 member 覆盖校正（缺失或不一致都重写）
        for (Map.Entry<String, Long> e : expected.entrySet()) {
            Double score = actual.get(e.getKey());
            if (score == null || score.longValue() != e.getValue()) {
                redis.opsForZSet().add(RedisKeys.LEADERBOARD_AC, e.getKey(), e.getValue());
                corrected++;
            }
        }

        if (removed > 0 || corrected > 0) {
            log.info("leaderboard reconciled: removed={}, corrected={}, expected={}", removed, corrected, expected.size());
        }
    }
}
