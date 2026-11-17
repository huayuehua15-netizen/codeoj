package com.codeoj.judge.mq;
import com.codeoj.common.constant.RedisKeys;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;

/** Projects committed distinct solves. Repeated AC and reordered updates cannot inflate scores. */
@Component
public class LeaderboardUpdater {
    private static final DefaultRedisScript<Long> UPDATE = new DefaultRedisScript<>(
            "local old=redis.call('ZSCORE',KEYS[1],ARGV[1]); " +
            "if not old or tonumber(old)<tonumber(ARGV[2]) then " +
            "redis.call('ZADD',KEYS[1],ARGV[2],ARGV[1]); return 1; end; return 0;", Long.class);
    private final StringRedisTemplate redis;
    private final SubmissionMapper submissions;
    public LeaderboardUpdater(StringRedisTemplate redis, SubmissionMapper submissions) {
        this.redis = redis;
        this.submissions = submissions;
    }
    public void refresh(Long userId) {
        long solved = submissions.countAcceptedProblems(userId);
        redis.execute(UPDATE, List.of(RedisKeys.LEADERBOARD_AC), String.valueOf(userId), String.valueOf(solved));
    }
}
