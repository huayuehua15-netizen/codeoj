package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.dto.ContestFormDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.ContestService;
import com.codeoj.server.vo.ContestDetailVO;
import com.codeoj.server.vo.ContestRankVO;
import com.codeoj.server.vo.ContestVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 竞赛接口（论文 7.2 展望落地）：列表/详情/排名公开，管理需教师。
 */
@RestController
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    /** 竞赛列表（公开：含未开始/进行中/已结束） */
    @GetMapping("/api/contests")
    public R<List<ContestVO>> list() {
        return R.ok(contestService.listAll());
    }

    /** 竞赛详情（含字母题号题目集） */
    @GetMapping("/api/contests/{id}")
    public R<ContestDetailVO> detail(@PathVariable(name = "id") Long id) {
        return R.ok(contestService.detail(id));
    }

    /** 竞赛排名（ACM 简化赛制） */
    @GetMapping("/api/contests/{id}/ranking")
    public R<List<ContestRankVO>> ranking(@PathVariable(name = "id") Long id) {
        return R.ok(contestService.ranking(id));
    }

    @PostMapping("/api/admin/contests")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@Valid @RequestBody ContestFormDTO dto,
                          @AuthenticationPrincipal LoginUser user) {
        return R.ok(contestService.create(user.userId(), dto));
    }

    @PutMapping("/api/admin/contests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable(name = "id") Long id,
                          @Valid @RequestBody ContestFormDTO dto) {
        contestService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/api/admin/contests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable(name = "id") Long id) {
        contestService.delete(id);
        return R.ok();
    }
}
