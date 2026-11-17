package com.codeoj.judge.mq;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LeaderboardUpdaterTest {
    @Test void repeatedAcProjectsDistinctDatabaseCountInsteadOfIncrementing() {
        SubmissionMapper mapper = mock(SubmissionMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(mapper.countAcceptedProblems(12L)).thenReturn(1L);
        LeaderboardUpdater updater = new LeaderboardUpdater(redis, mapper);
        updater.refresh(12L);
        updater.refresh(12L);
        verify(redis, times(2)).execute(any(RedisScript.class), anyList(), eq("12"), eq("1"));
        verify(redis, never()).opsForZSet();
    }
}
