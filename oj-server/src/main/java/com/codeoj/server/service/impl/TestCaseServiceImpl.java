package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.TestCase;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.TestCaseImportDTO;
import com.codeoj.server.mapper.TestCaseMapper;
import com.codeoj.server.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用例服务实现：批量导入、按题目查询、删除。
 */
@Service
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseMapper testCaseMapper;

    public TestCaseServiceImpl(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    @Transactional
    public int batchImport(Long problemId, TestCaseImportDTO dto) {
        int imported = 0;
        for (TestCaseImportDTO.Item item : dto.getCases()) {
            TestCase tc = new TestCase();
            tc.setProblemId(problemId);
            tc.setInput(item.getInput());
            tc.setExpectedOutput(item.getExpectedOutput());
            tc.setScore(item.getScore());
            tc.setIsSample(item.getIsSample());
            testCaseMapper.insert(tc);
            imported++;
        }
        return imported;
    }

    @Override
    public List<TestCase> listByProblem(Long problemId) {
        return testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .orderByAsc(TestCase::getId));
    }

    /**
     * 删除用例。<b>必须校验用例归属</b>：
     * 路径是 /api/problems/{problemId}/testcases/{caseId}，若只用 caseId 删除，
     * 管理员（或未来任何能调此接口的角色）可以拿 A 题路径删掉 B 题的用例——
     * 典型的越权 IDOR。删错用例会直接改变判题结果，属高危。
     */
    @Override
    public void delete(Long problemId, Long caseId) {
        TestCase tc = testCaseMapper.selectById(caseId);
        if (tc == null || !tc.getProblemId().equals(problemId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        testCaseMapper.deleteById(caseId);
    }
}
