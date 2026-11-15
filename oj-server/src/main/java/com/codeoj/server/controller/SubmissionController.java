package com.codeoj.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import com.codeoj.common.result.R;
import com.codeoj.server.dto.SubmitDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.SubmissionService;
import com.codeoj.server.vo.StatisticsVO;
import com.codeoj.server.vo.SubmissionDetailVO;
import com.codeoj.server.vo.SubmissionListItemVO;
import com.codeoj.server.vo.SubmitVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 提交接口（论文表 4-7）：提交代码异步判题；判题详情（用例级）仅本人/管理员。
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** 提交代码，异步判题（论文 5.3） */
    @PostMapping
    public R<SubmitVO> submit(@Valid @RequestBody SubmitDTO dto,
                              @AuthenticationPrincipal LoginUser user) {
        return R.ok(submissionService.submit(user, dto));
    }

    /** 判题详情（用例级） */
    @GetMapping("/{id}")
    public R<SubmissionDetailVO> detail(@PathVariable("id") Long id,
                                        @AuthenticationPrincipal LoginUser user) {
        return R.ok(submissionService.detail(id, user));
    }

    /** 提交记录：按题目/状态/时间筛选（论文 3.3） */
    @GetMapping
    public R<Page<SubmissionListItemVO>> page(@RequestParam(name = "problemId", required = false) Long problemId,
                                              @RequestParam(name = "status", required = false) Integer status,
                                              @RequestParam(name = "begin", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime begin,
                                              @RequestParam(name = "end", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                              @RequestParam(name = "page", defaultValue = "1") long page,
                                              @RequestParam(name = "size", defaultValue = "10") long size,
                                              @AuthenticationPrincipal LoginUser user) {
        return R.ok(submissionService.pageMy(user, problemId, status, begin, end, page, size));
    }

    /** 个人通过率统计（论文 5.6） */
    @GetMapping("/statistics")
    public R<StatisticsVO> statistics(@AuthenticationPrincipal LoginUser user) {
        return R.ok(submissionService.statistics(user.userId()));
    }
}
