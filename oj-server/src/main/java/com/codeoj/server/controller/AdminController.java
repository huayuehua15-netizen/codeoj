package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.service.QueueMonitorService;
import com.codeoj.server.vo.QueueStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维监控接口（论文表 4-7：GET /api/admin/queue 判题队列状态，管理员）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final QueueMonitorService queueMonitorService;

    public AdminController(QueueMonitorService queueMonitorService) {
        this.queueMonitorService = queueMonitorService;
    }

    @GetMapping("/queue")
    public R<QueueStatusVO> queue() {
        return R.ok(queueMonitorService.status());
    }
}
