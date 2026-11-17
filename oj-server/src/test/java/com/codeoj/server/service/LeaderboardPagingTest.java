package com.codeoj.server.service;

import com.codeoj.common.constant.RedisKeys;
import com.codeoj.server.mapper.*;
import com.codeoj.server.service.impl.LeaderboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaderboardPagingTest {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") ZSetOperations<String,String> zset = mock(ZSetOperations.class);
    LeaderboardStatsMapper stats = mock(LeaderboardStatsMapper.class);
    UserMapper users = mock(UserMapper.class);

    LeaderboardServiceImpl service() {
        var all = new LinkedHashSet<ZSetOperations.TypedTuple<String>>();
        all.add(new DefaultTypedTuple<>("1",10.0)); all.add(new DefaultTypedTuple<>("2",4.0));
        when(redis.opsForZSet()).thenReturn(zset);
        when(zset.size(RedisKeys.LEADERBOARD_AC)).thenReturn(2L);
        when(zset.reverseRangeWithScores(RedisKeys.LEADERBOARD_AC,0,-1)).thenReturn(all);
        when(zset.reverseRangeWithScores(RedisKeys.LEADERBOARD_AC,0,0)).thenReturn(Set.of(new DefaultTypedTuple<>("1",10.0)));
        when(users.selectBatchIds(List.of(1L,2L))).thenReturn(List.of());
        when(stats.submitStatsByUsers(List.of(1L,2L))).thenReturn(List.of());
        when(stats.firstAcTimes(List.of(1L,2L))).thenReturn(List.of());
        return new LeaderboardServiceImpl(redis,users,stats);
    }

    @Test void secondPageKeepsGlobalMaximumAndAbsoluteRank() {
        var page = service().page(2,1);
        assertEquals(4,page.getItems().get(0).getAcCount());
        assertEquals(2,page.getItems().get(0).getRank());
        assertEquals(10,page.getSummary().getMaxAcCount());
    }
    @Test void invalidPageBoundsAreClampedAndHugePageCannotOverflow() {
        var service = service();
        assertEquals(1L, service.page(-1,-1).getItems().get(0).getUserId());
        assertTrue(service.page(Long.MAX_VALUE,Long.MAX_VALUE).getItems().isEmpty());
        assertEquals(10,service.page(Long.MAX_VALUE,1).getSummary().getMaxAcCount());
    }
}
