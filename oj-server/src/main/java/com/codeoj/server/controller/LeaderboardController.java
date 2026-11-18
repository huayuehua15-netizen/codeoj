package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.service.LeaderboardService;
import com.codeoj.server.vo.LeaderboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排行榜接口（论文表 4-7：GET /api/leaderboard 公开，分页）。
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * 排行榜分页。
     *
     * <p><b>参数在服务层夹取而非这里</b>：排行榜的数据源是 Redis ZSet 而不是 MyBatis-Plus，
     * {@code page}/{@code size} 直接换算成 {@code reverseRangeWithScores} 的起止下标，
     * {@code PaginationInnerInterceptor.maxLimit} 对此完全不生效。夹取逻辑放在
     * {@code LeaderboardServiceImpl} 里，保证直接调用服务（定时任务、测试）时同样安全。
     */
    @GetMapping
    public R<LeaderboardVO> page(@RequestParam(name = "page", defaultValue = "1") long page,
                                 @RequestParam(name = "size", defaultValue = "20") long size) {
        return R.ok(leaderboardService.page(page, size));
    }
}
