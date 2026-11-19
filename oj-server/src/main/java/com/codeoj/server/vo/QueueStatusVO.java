package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 判题队列运行状态（论文表 3-3 运维监控：判题队列积压、今日判题量等指标）。
 */
@Data
@AllArgsConstructor
public class QueueStatusVO {

    /** 队列积压量（待判题任务数，Redis 缓存 30s，可能是上一次探询值） */
    private long backlog;
    /** 今日判题量 */
    private long judgedToday;
    /** 消费者在线实例数（Redis 心跳统计） */
    private int judgeInstances;
    /**
     * 本次查询时刻（API.md §0.1 格式）。
     *
     * <p>原名 {@code cachedAt}（review P2-35）：字段填的从来都是"当下查询"的时间，
     * 而 {@code backlog} 才是可能来自 30s 缓存的旧值——叫"缓存时间"会让人误以为
     * 积压量是这一刻采集的。更名为 {@code queriedAt} 并与缓存语义解耦。
     */
    private LocalDateTime queriedAt;
}
