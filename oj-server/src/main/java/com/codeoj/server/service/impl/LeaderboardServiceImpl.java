package com.codeoj.server.service.impl;

import com.codeoj.common.constant.RedisKeys;
import com.codeoj.common.entity.User;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.service.LeaderboardService;
import com.codeoj.server.vo.LeaderboardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排行榜服务实现（论文 5.6）：Redis ZSet oj:leaderboard:ac 为唯一数据源，
 * 按分数倒序取前 N 名，再以用户表补齐昵称等展示信息；
 * 通过异步对账任务与数据库汇总保持一致（对账任务见第二阶段 M3）。
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
        // 分页倒序取前 N 名（论文 5.6）
        Set<ZSetOperations.TypedTuple<Object>> top =
                redis.opsForZSet().reverseRangeWithScores(RedisKeys.LEADERBOARD_AC, offset, offset + size - 1);

        List<LeaderboardVO.Item> items = new ArrayList<>();
        if (top != null && !top.isEmpty()) {
            // 批量补齐用户展示信息（一次查询，避免 N+1）
            List<Long> userIds = top.stream()
                    .map(t -> Long.valueOf(String.valueOf(t.getValue())))
                    .toList();
            Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            for (var tuple : top) {
                Long userId = Long.valueOf(String.valueOf(tuple.getValue()));
                User u = users.get(userId);
                items.add(new LeaderboardVO.Item(
                        userId,
                        u != null ? u.getUsername() : null,
                        tuple.getScore() == null ? 0 : tuple.getScore().longValue(),
                        offset + items.size() + 1));
            }
        }
        return new LeaderboardVO(total, items);
    }

    @Override
    public void incr(Long userId) {
        // AC 时更新排行（论文 5.6）：ZINCRBY oj:leaderboard:ac
        redis.opsForZSet().incrementScore(RedisKeys.LEADERBOARD_AC, String.valueOf(userId), 1);
        log.debug("leaderboard incr, userId={}", userId);
    }
}
