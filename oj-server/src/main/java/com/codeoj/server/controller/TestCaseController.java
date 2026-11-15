package com.codeoj.server.controller;

import com.codeoj.common.entity.TestCase;
import jakarta.validation.Valid;
import com.codeoj.common.result.R;
import com.codeoj.server.dto.TestCaseImportDTO;
import com.codeoj.server.service.TestCaseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 测试用例接口（论文表 4-7）：批量导入与维护仅管理员可见。
 */
@RestController
@RequestMapping("/api/problems/{problemId}/testcases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    /** 批量导入用例（论文表 4-7 POST /api/problems/{id}/testcases） */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Integer> batchImport(@PathVariable("problemId") Long problemId,
                                  @Valid @RequestBody TestCaseImportDTO dto) {
        return R.ok(testCaseService.batchImport(problemId, dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<TestCase>> list(@PathVariable("problemId") Long problemId) {
        return R.ok(testCaseService.listByProblem(problemId));
    }

    @DeleteMapping("/{caseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable("problemId") Long problemId, @PathVariable("caseId") Long caseId) {
        testCaseService.delete(caseId);
        return R.ok();
    }
}
