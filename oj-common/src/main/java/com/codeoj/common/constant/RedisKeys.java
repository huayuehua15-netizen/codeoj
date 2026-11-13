package com.codeoj.common.constant;

/**
 * Redis 键设计（论文表 4-6）。限流、幂等锁与令牌黑名单利用字符串键的过期机制实现。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 排行榜（ZSET，member=userId，score=AC 题数），永久 */
    public static final String LEADERBOARD_AC = "oj:leaderboard:ac";

    /** 热点题目提交/通过数缓存（HASH，5 min），占位符 {id} 为题目 ID */
    public static final String STATS_PROBLEM = "oj:stats:problem:%d";

    /** 登出令牌黑名单（STRING，TTL=token 剩余时长） */
    public static final String TOKEN_BLACKLIST = "oj:token:blacklist:%s";

    /** 登录/提交限流计数（STRING，TTL=窗口） */
    public static final String RATE = "oj:rate:%s:%s";

    /** 重复提交幂等锁（STRING NX，2 s） */
    public static final String LOCK_SUBMIT = "oj:lock:submit:%d:%d";

    /** 队列积压量缓存（STRING，30 s，运维展示） */
    public static final String QUEUE_BACKLOG = "oj:queue:backlog";

    public static String statsProblem(long problemId) {
        return String.format(STATS_PROBLEM, problemId);
    }

    public static String tokenBlacklist(String jti) {
        return String.format(TOKEN_BLACKLIST, jti);
    }

    public static String rate(String type, String key) {
        return String.format(RATE, type, key);
    }

    public static String lockSubmit(long userId, long problemId) {
        return String.format(LOCK_SUBMIT, userId, problemId);
    }
}
