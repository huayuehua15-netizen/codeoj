package com.codeoj.judge.core.result;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codeoj.common.entity.JudgeCase;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.Submission;
import com.codeoj.judge.core.CaseResult;
import com.codeoj.judge.core.JudgeResult;
import com.codeoj.judge.mapper.JudgeCaseMapper;
import com.codeoj.judge.mapper.ProblemMapper;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结果回写（论文 4.6/5.4）：条件更新 + 用例明细 + 题目统计冗余字段更新。
 */
@Component
public class ResultWriter {

    private static final Logger log = LoggerFactory.getLogger(ResultWriter.class);

    private final SubmissionMapper submissionMapper;
    private final JudgeCaseMapper judgeCaseMapper;
    private final ProblemMapper problemMapper;

    public ResultWriter(SubmissionMapper submissionMapper,
                        JudgeCaseMapper judgeCaseMapper,
                        ProblemMapper problemMapper) {
        this.submissionMapper = submissionMapper;
        this.judgeCaseMapper = judgeCaseMapper;
        this.problemMapper = problemMapper;
    }

    /**
     * 回写判题结果：当前未过期租约条件更新 + judge_case 明细 + 冗余统计。
     *
     * @return false = 已终结或租约失效，禁止明细、统计和结果推送等副作用
     */
    @Transactional
    public boolean save(long submissionId, String token, JudgeResult result) {
        // The current lease owner alone may commit; expired/replaced attempts have no side effects.
        int updated = submissionMapper.updateFinalResult(
                submissionId,
                token,
                result.getStatus().getCode(),
                result.getTimeUsedMs(),
                result.getMemoryUsedKb(),
                result.getErrorInfo(),
                result.getTotalCases());

        if (updated == 0) {
            log.info("terminal or stale judge lease, skip result: submission={}", submissionId);
            return false;
        }

        // 用例级明细（论文表 4-5）
        if (result.getCases() != null) {
            for (CaseResult cr : result.getCases()) {
                JudgeCase jc = new JudgeCase();
                jc.setSubmissionId(submissionId);
                jc.setCaseIndex(cr.getIndex());
                jc.setStatus(cr.getStatus().getCode());
                jc.setTimeUsedMs(cr.getTimeUsedMs());
                jc.setMemoryUsedKb(cr.getMemoryUsedKb());
                judgeCaseMapper.insert(jc);
            }
        }

        // 冗余统计：submit_count +1；AC 时 accepted_count +1（论文 4.4）
        Submission s = submissionMapper.selectById(submissionId);
        if (s != null) {
            String setSql = result.isAccepted()
                    ? "submit_count = submit_count + 1, accepted_count = accepted_count + 1"
                    : "submit_count = submit_count + 1";
            problemMapper.update(null, new LambdaUpdateWrapper<Problem>()
                    .eq(Problem::getId, s.getProblemId())
                    .setSql(setSql));
        }
        return true;
    }
}
