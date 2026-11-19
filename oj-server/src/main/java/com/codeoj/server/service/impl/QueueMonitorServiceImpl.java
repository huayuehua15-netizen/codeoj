package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.constant.RedisKeys;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.server.mapper.SubmissionMapper;
import com.codeoj.server.service.QueueMonitorService;
import com.codeoj.server.vo.QueueStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

/**
 * 运维监控实现（骨架：队列积压量经 Redis 缓存 30s，论文表 4-6 oj:queue:backlog）。
 */
@Service
public class QueueMonitorServiceImpl implements QueueMonitorService {

    private static final Logger log = LoggerFactory.getLogger(QueueMonitorServiceImpl.class);

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
        // queriedAt 用 LocalDateTime：全局 JacksonConfig 统一序列化为
        // yyyy-MM-dd HH:mm:ss（API.md §0.1）；原先手写 ISO_INSTANT 输出 UTC「Z」格式，
        // 与全站时间口径差 8 小时（review P2-35 顺带修正）
        return new QueueStatusVO(backlog, judgedTodayCount == null ? 0 : judgedTodayCount,
                onlineJudgeInstances(), java.time.LocalDateTime.now());
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

    /**
     * 队列积压量：优先 Redis 缓存（TTL 30s），未命中时查询 RabbitMQ 并回填。
     *
     * <p><b>为什么必须 catch AmqpException</b>（review P2-34）：{@code queueDeclarePassive}
     * 是"被动声明"——队列不存在时 broker 会直接关闭 channel 并回 404，
     * 客户端抛 {@link AmqpException}。而队列不存在正是新部署、判题侧还没启动的常态，
     * 监控页不该因此整体 50000：积压量降级为 0 并记 warn，
     * 运维仍能从"在线实例数=0"看出问题。
     *
     * <p>口径提醒：{@code getMessageCount()} 返回的是 <b>ready</b>（已入队未被取走）条数，
     * 不含被 consumer 预取但尚未 ack 的 unacked 消息，因此消费中途的瞬时值会略小于真实在途量。
     */
    private long backlog() {
        String cached = redis.opsForValue().get(RedisKeys.QUEUE_BACKLOG);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        long value = 0;
        try {
            Integer count = rabbitTemplate.execute(ch ->
                    ch.queueDeclarePassive(com.codeoj.common.constant.MqConstants.JUDGE_QUEUE)
                            .getMessageCount());
            value = count == null ? 0 : count;
        } catch (AmqpException e) {
            log.warn("queue {} not available, backlog degraded to 0: {}",
                    com.codeoj.common.constant.MqConstants.JUDGE_QUEUE, e.getMessage());
        }
        redis.opsForValue().set(RedisKeys.QUEUE_BACKLOG, String.valueOf(value),
                java.time.Duration.ofSeconds(30));
        return value;
    }
}
