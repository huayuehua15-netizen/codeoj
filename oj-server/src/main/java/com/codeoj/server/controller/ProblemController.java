package com.codeoj.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.R;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.ProblemFormDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.ProblemService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目接口（论文表 4-7）：列表/详情公开；增删改需管理员。
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /** 分页列表（关键词/难度筛选） */
    @GetMapping
    public R<Page<Problem>> page(@RequestParam(name = "keyword", required = false) String keyword,
                                 @RequestParam(name = "difficulty", required = false) Integer difficulty,
                                 @RequestParam(name = "page", defaultValue = "1") long page,
                                 @RequestParam(name = "size", defaultValue = "10") long size,
                                 @AuthenticationPrincipal LoginUser user) {
        boolean admin = user != null && user.isAdmin();
        return R.ok(problemService.page(keyword, difficulty, page, size, admin));
    }

    /** 题面、样例与限制 */
    @GetMapping("/{id}")
    public R<Problem> detail(@PathVariable("id") Long id) {
        return R.ok(problemService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@RequestBody ProblemFormDTO form) {
        return R.ok(problemService.create(form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable("id") Long id, @RequestBody ProblemFormDTO form) {
        problemService.update(id, form);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable("id") Long id) {
        problemService.delete(id);
        return R.ok();
    }
}
