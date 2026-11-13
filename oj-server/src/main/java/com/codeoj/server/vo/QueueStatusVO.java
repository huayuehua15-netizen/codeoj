package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 判题队列运行状态（论文表 3-3 运维监控：判题队列积压、今日判题量等指标）。
 */
@Data
@AllArgsConstructor
public class QueueStatusVO {

    /** 队列积压量（待判题任务数） */
    private long backlog;
    /** 今日判题量 */
    private long judgedToday;
    /** 消费者在线实例数（Redis 心跳统计） */
    private int judgeInstances;
    /** 队列积压缓存更新时间 */
    private String cachedAt;
}
