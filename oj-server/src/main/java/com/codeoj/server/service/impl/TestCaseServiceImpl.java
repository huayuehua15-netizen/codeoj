package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.TestCase;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.TestCaseImportDTO;
import com.codeoj.server.mapper.TestCaseMapper;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.service.TestCaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用例服务实现：批量导入、按题目查询、删除。
 */
@Service
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseMapper testCaseMapper;
    private final ProblemMapper problemMapper;

    public TestCaseServiceImpl(TestCaseMapper testCaseMapper, ProblemMapper problemMapper) {
        this.testCaseMapper = testCaseMapper;
        this.problemMapper = problemMapper;
    }

    @Override
    @Transactional
    public int batchImport(Long problemId, TestCaseImportDTO dto) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) throw new BizException(ResultCode.NOT_FOUND);
        boolean functionMode = problem.getEntryFunction() != null;
        int imported = 0;
        for (TestCaseImportDTO.Item item : dto.getCases()) {
            if (functionMode) {
                // 函数题（JS 场景题）约定：input 是 JSON 参数数组（如 [1,2]），
                // 判题调用器按 JSON.parse 解析——导入非数组的输入必然判错，导入时就拦下。
                requireJsonArgArray(item.getInput(), imported);
            }
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

    /** 函数题用例输入必须是 JSON 数组（每组用例的参数列表，如 [1,2]） */
    private void requireJsonArgArray(String input, int caseIndex) {
        try {
            if (!new ObjectMapper().readTree(input == null ? "" : input).isArray()) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                        "函数题的用例输入须为 JSON 参数数组（如 [1,2]），第 " + (caseIndex + 1) + " 组不是数组");
            }
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                    "函数题的用例输入须为合法 JSON 参数数组（如 [1,2]），第 " + (caseIndex + 1) + " 组解析失败：\"" + input + "\"");
        }
    }

    @Override
    public List<TestCase> listByProblem(Long problemId) {
        requireProblem(problemId);
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

    private void requireProblem(Long problemId) {
        if (problemMapper.selectById(problemId) == null) throw new BizException(ResultCode.NOT_FOUND);
    }
}
