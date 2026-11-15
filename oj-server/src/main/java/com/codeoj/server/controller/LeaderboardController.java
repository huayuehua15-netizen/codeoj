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

    @GetMapping
    public R<LeaderboardVO> page(@RequestParam(name = "page", defaultValue = "1") long page,
                                 @RequestParam(name = "size", defaultValue = "20") long size) {
        return R.ok(leaderboardService.page(page, size));
    }
}
