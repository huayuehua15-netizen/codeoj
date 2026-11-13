package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.TestCase;
import com.codeoj.server.dto.TestCaseImportDTO;
import com.codeoj.server.mapper.TestCaseMapper;
import com.codeoj.server.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用例服务实现（骨架：批量导入已实现；列表与删除为第二阶段补全项）。
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

    @Override
    public void delete(Long caseId) {
        testCaseMapper.deleteById(caseId);
    }
}
