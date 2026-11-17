package com.codeoj.server.service;

import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.mapper.ProblemMapper;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;

/** Validate assignments before replacing existing course/contest links. */
@Component
public class ProblemSelectionValidator {
    private final ProblemMapper problems;

    public ProblemSelectionValidator(ProblemMapper problems) { this.problems = problems; }

    public void validate(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        if (ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "题目编号须为正整数且不能重复");
        }
        if (problems.selectBatchIds(ids).size() != ids.size()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "所选题目包含不存在的题目");
        }
    }
}
