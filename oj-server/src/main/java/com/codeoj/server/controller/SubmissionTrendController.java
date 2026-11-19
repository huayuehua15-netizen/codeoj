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
 * 个人聚合数据接口（API.md v1.9）：趋势（统计页图表/主页热力图）与语言分布。
 * 与 SubmissionController 的区别：这里只做本人维度的小聚合，直读 Mapper、不过 Service 事务层。
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionTrendController {

    /**
     * days 上限：366 天（API.md v1.9 由 30 放宽）。个人主页热力图要展示近 26 周（182 天），
     * 30 天的上限正是"个人页只有统计、没有活动历史"的原因；366 覆盖整年，
     * 且是本人数据 + 登录态限定，不存在越权或刷量面。
     */
    static final int MAX_DAYS = 366;

    private final SubmissionMapper submissionMapper;

    public SubmissionTrendController(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    /** 近 days 天提交趋势（个人维度，默认 7 天，上限 366） */
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> trend(@RequestParam(name = "days", defaultValue = "7") int days,
                                              @AuthenticationPrincipal LoginUser user) {
        List<Map<String, Object>> rows = submissionMapper.trendByUser(user.userId(), Math.max(1, Math.min(days, MAX_DAYS)));
        return R.ok(rows);
    }

    /** 语言分布（个人主页，API.md v1.9 §4.4b）：按提交数降序 */
    @GetMapping("/languages")
    public R<List<Map<String, Object>>> languages(@AuthenticationPrincipal LoginUser user) {
        return R.ok(submissionMapper.languageDistByUser(user.userId()));
    }

    /** 错题本（API.md v1.10 §4.4c）：尝试过且未 AC 的题目，按最近提交倒序 */
    @GetMapping("/wrongbook")
    public R<List<Map<String, Object>>> wrongBook(@AuthenticationPrincipal LoginUser user) {
        return R.ok(submissionMapper.wrongBookByUser(user.userId()));
    }
}
