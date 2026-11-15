package com.codeoj.server.task;

import com.codeoj.common.constant.RedisKeys;
import com.codeoj.server.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 排行榜异步对账任务（论文 5.6）：ZSet oj:leaderboard:ac 为准，通过与数据库汇总
 * 对比纠偏，保证最终一致——覆盖 ZINCRBY 重复计分、脏 member（用户已删）、丢消息等场景。
 */
@Component
public class LeaderboardReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardReconcileTask.class);

    private final RedisTemplate<String, Object> redis;
    private final SubmissionMapper submissionMapper;

    public LeaderboardReconcileTask(RedisTemplate<String, Object> redis,
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

        Set<Object> members = redis.opsForZSet().range(RedisKeys.LEADERBOARD_AC, 0, -1);
        int removed = 0;
        int corrected = 0;

        // 1. ZSet 中的脏 member（DB 无此用户/无 AC）删除
        if (members != null) {
            for (Object m : members) {
                if (!expected.containsKey(String.valueOf(m))) {
                    redis.opsForZSet().remove(RedisKeys.LEADERBOARD_AC, m);
                    removed++;
                }
            }
        }

        // 2. 期望值与 ZSet 不符的 member 覆盖校正
        Set<String> checked = new HashSet<>();
        if (members != null) {
            for (Object m : members) {
                checked.add(String.valueOf(m));
            }
        }
        for (Map.Entry<String, Long> e : expected.entrySet()) {
            if (!checked.contains(e.getKey())) {
                redis.opsForZSet().add(RedisKeys.LEADERBOARD_AC, e.getKey(), e.getValue());
                corrected++;
            } else {
                Double score = redis.opsForZSet().score(RedisKeys.LEADERBOARD_AC, e.getKey());
                if (score == null || score.longValue() != e.getValue()) {
                    redis.opsForZSet().add(RedisKeys.LEADERBOARD_AC, e.getKey(), e.getValue());
                    corrected++;
                }
            }
        }

        if (removed > 0 || corrected > 0) {
            log.info("leaderboard reconciled: removed={}, corrected={}, expected={}", removed, corrected, expected.size());
        }
    }
}
