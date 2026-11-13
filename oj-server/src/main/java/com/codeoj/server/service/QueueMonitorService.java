package com.codeoj.server.service;

import com.codeoj.server.vo.QueueStatusVO;

/**
 * 运维监控（论文表 3-3：判题队列积压、今日判题量等指标）。
 */
public interface QueueMonitorService {

    /** GET /api/admin/queue：队列积压量（Redis 缓存 30s）+ 今日判题量 + 判题实例数 */
    QueueStatusVO status();
}
