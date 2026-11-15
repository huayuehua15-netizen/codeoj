package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.constant.RedisKeys;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.server.mapper.SubmissionMapper;
import com.codeoj.server.service.QueueMonitorService;
import com.codeoj.server.vo.QueueStatusVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * 运维监控实现（骨架：队列积压量经 Redis 缓存 30s，论文表 4-6 oj:queue:backlog）。
 */
@Service
public class QueueMonitorServiceImpl implements QueueMonitorService {

    private final StringRedisTemplate redis;
    private final SubmissionMapper submissionMapper;
    private final RabbitTemplate rabbitTemplate;

    public QueueMonitorServiceImpl(StringRedisTemplate redis,
                                   SubmissionMapper submissionMapper,
                                   RabbitTemplate rabbitTemplate) {
        this.redis = redis;
        this.submissionMapper = submissionMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public QueueStatusVO status() {
        long backlog = backlog();
        Long judgedTodayCount = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .ge(Submission::getJudgedAt, LocalDate.now().atStartOfDay())
                .ne(Submission::getStatus, SubmissionStatus.QUEUING.getCode())
                .ne(Submission::getStatus, SubmissionStatus.JUDGING.getCode()));
        return new QueueStatusVO(backlog, judgedTodayCount == null ? 0 : judgedTodayCount,
                onlineJudgeInstances(), java.time.format.DateTimeFormatter.ISO_INSTANT
                        .format(java.time.Instant.now()));
    }

    /** 在线判题实例数：SCAN oj:judge:instance:* 心跳键（TTL 30s，掉线自动消失） */
    private int onlineJudgeInstances() {
        Set<String> keys = redis.execute((org.springframework.data.redis.core.RedisCallback<Set<String>>) conn -> {
            Set<String> result = new java.util.HashSet<>();
            try (var cursor = conn.scan(org.springframework.data.redis.core.ScanOptions
                    .scanOptions().match("oj:judge:instance:*").count(100).build())) {
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            return result;
        });
        return keys == null ? 0 : keys.size();
    }

    /** 队列积压量：优先 Redis 缓存（TTL 30s），未命中时查询 RabbitMQ 并回填 */
    private long backlog() {
        String cached = redis.opsForValue().get(RedisKeys.QUEUE_BACKLOG);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        Integer count = rabbitTemplate.execute(ch ->
                ch.queueDeclarePassive(com.codeoj.common.constant.MqConstants.JUDGE_QUEUE).getMessageCount());
        long value = count == null ? 0 : count;
        redis.opsForValue().set(RedisKeys.QUEUE_BACKLOG, String.valueOf(value),
                java.time.Duration.ofSeconds(30));
        return value;
    }
}
