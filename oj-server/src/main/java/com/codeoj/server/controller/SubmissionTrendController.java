package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.mapper.SubmissionMapper;
import com.codeoj.server.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 提交趋势接口（个人统计页图表数据）：近 N 天按日聚合提交数与通过数。
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionTrendController {

    private final SubmissionMapper submissionMapper;

    public SubmissionTrendController(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    /** 近 days 天提交趋势（个人维度，默认 7 天） */
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> trend(@RequestParam(name = "days", defaultValue = "7") int days,
                                              @AuthenticationPrincipal LoginUser user) {
        List<Map<String, Object>> rows = submissionMapper.trendByUser(user.userId(), Math.max(1, Math.min(days, 30)));
        return R.ok(rows);
    }
}
