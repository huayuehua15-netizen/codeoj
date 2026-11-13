package com.codeoj.judge.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.entity.TestCase;
import com.codeoj.judge.mapper.ProblemMapper;
import com.codeoj.judge.mapper.SubmissionMapper;
import com.codeoj.judge.mapper.TestCaseMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务加载（论文 4.6/5.4）：消费者按 submissionId 回查数据库，组装代码 + 用例 + 限制。
 * 消息体只携带 submissionId，天然保证数据一致。
 */
@Component
public class TaskLoader {

    private final SubmissionMapper submissionMapper;
    private final TestCaseMapper testCaseMapper;
    private final ProblemMapper problemMapper;

    public TaskLoader(SubmissionMapper submissionMapper,
                      TestCaseMapper testCaseMapper,
                      ProblemMapper problemMapper) {
        this.submissionMapper = submissionMapper;
        this.testCaseMapper = testCaseMapper;
        this.problemMapper = problemMapper;
    }

    public JudgeTask load(long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new IllegalStateException("submission not found: " + submissionId);
        }
        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem == null) {
            throw new IllegalStateException("problem not found: " + submission.getProblemId());
        }
        List<TestCase> testCases = testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, submission.getProblemId())
                .orderByAsc(TestCase::getId));

        List<JudgeTask.CaseInput> cases = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            cases.add(JudgeTask.CaseInput.builder()
                    .index(i)
                    .input(testCases.get(i).getInput())
                    .expectedOutput(testCases.get(i).getExpectedOutput())
                    .build());
        }

        return JudgeTask.builder()
                .submissionId(submission.getId())
                .userId(submission.getUserId())
                .problemId(submission.getProblemId())
                .language(submission.getLanguage())
                .code(submission.getCode())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .cases(cases)
                .build();
    }
}
