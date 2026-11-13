package com.codeoj.server.service;

import com.codeoj.common.entity.TestCase;
import com.codeoj.server.dto.TestCaseImportDTO;

import java.util.List;

/**
 * 测试用例服务（论文表 3-3 用例管理：批量导入、增删改查）。
 */
public interface TestCaseService {

    /** 批量导入用例（论文表 4-7：POST /api/problems/{id}/testcases） */
    int batchImport(Long problemId, TestCaseImportDTO dto);

    /** 用例仅管理员可见（论文 4.7：测试用例等敏感数据仅对管理员可见） */
    List<TestCase> listByProblem(Long problemId);

    void delete(Long caseId);
}
