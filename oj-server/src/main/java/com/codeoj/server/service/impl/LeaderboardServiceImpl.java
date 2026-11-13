package com.codeoj.server.service.impl;

import com.codeoj.common.constant.RedisKeys;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.service.LeaderboardService;
import com.codeoj.server.vo.LeaderboardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜服务实现（骨架：ZSet 读取骨架已就位，用户信息补齐与对账任务在第二阶段补全）。
 */
@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);

    private final RedisTemplate<String, Object> redis;
    private final UserMapper userMapper;

    public LeaderboardServiceImpl(RedisTemplate<String, Object> redis, UserMapper userMapper) {
        this.redis = redis;
        this.userMapper = userMapper;
    }

    @Override
    public LeaderboardVO page(long page, long size) {
        Long zsetSize = redis.opsForZSet().size(RedisKeys.LEADERBOARD_AC);
        long total = zsetSize == null ? 0 : zsetSize;
        long offset = (page - 1) * size;
        // 分页查询前 N 名（论文 5.6）
        var top = redis.opsForZSet().reverseRangeWithScores(
                RedisKeys.LEADERBOARD_AC, offset, offset + size - 1);
        List<LeaderboardVO.Item> items = new ArrayList<>();
        if (top != null) {
            for (var tuple : top) {
                // TODO 第二阶段：以用户表补齐昵称等展示信息
                items.add(new LeaderboardVO.Item(Long.valueOf(String.valueOf(tuple.getValue())),
                        null, null,
                        tuple.getScore() == null ? 0 : tuple.getScore().longValue(),
                        offset + items.size() + 1));
            }
        }
        return new LeaderboardVO(total, items);
    }

    @Override
    public void incr(Long userId) {
        // AC 时更新排行（论文 5.6）
        redis.opsForZSet().incrementScore(RedisKeys.LEADERBOARD_AC, String.valueOf(userId), 1);
        log.debug("leaderboard incr, userId={}", userId);
    }
}
