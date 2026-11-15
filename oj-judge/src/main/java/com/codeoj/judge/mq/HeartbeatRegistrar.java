package com.codeoj.judge.mq;

import com.codeoj.common.constant.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

/**
 * 判题实例心跳（论文表 3-3 运维监控）：启动注册 + 每 20 秒续期（TTL 30 秒），
 * oj-server 通过统计 oj:judge:instance:* 键数量得到在线实例数（--scale 扩容后自动可见）。
 */
@Component
public class HeartbeatRegistrar {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatRegistrar.class);

    private final StringRedisTemplate redis;
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    public HeartbeatRegistrar(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStart() {
        beat();
        log.info("judge instance registered: id={}, host={}", instanceId, host());
    }

    @Scheduled(fixedDelay = 20_000)
    public void beat() {
        redis.opsForValue().set(RedisKeys.JUDGE_INSTANCE.formatted(instanceId),
                host() + "|" + instanceId, Duration.ofSeconds(30));
    }

    private String host() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
