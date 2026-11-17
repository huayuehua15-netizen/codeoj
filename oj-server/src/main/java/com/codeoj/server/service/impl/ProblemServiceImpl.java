package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.TestCase;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.ProblemFormDTO;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.mapper.TestCaseMapper;
import com.codeoj.server.service.ProblemService;
import com.codeoj.server.vo.ProblemListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 题目服务实现（论文表 3-3：题目增删改查与上下架）。
 */
@Service
public class ProblemServiceImpl implements ProblemService {

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;

    public ProblemServiceImpl(ProblemMapper problemMapper, TestCaseMapper testCaseMapper) {
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    public Page<ProblemListItemVO> page(String keyword, Integer difficulty, String tag, long page, long size, boolean isAdmin) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(Problem::getEnabled, true);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Problem::getTitle, keyword);
        }
        if (difficulty != null && difficulty > 0) {
            wrapper.eq(Problem::getDifficulty, difficulty);
        }
        // 标签筛选：tags 为 JSON 列，JSON_CONTAINS 参数化传值（无拼接，防注入）
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag);
        }
        // 列表只查轻量列：description 是 LONGTEXT、samples 是 JSON，
        // 一页 10 条全带会把整份题面塞进响应（API.md §2.1：列表不返回大字段）
        wrapper.select(Problem::getId, Problem::getTitle, Problem::getTags,
                Problem::getDifficulty, Problem::getTimeLimitMs, Problem::getMemoryLimitMb,
                Problem::getSubmitCount, Problem::getAcceptedCount, Problem::getEnabled,
                Problem::getCreatedAt, Problem::getUpdatedAt);
        wrapper.orderByAsc(Problem::getId);
        Page<Problem> result = problemMapper.selectPage(new Page<>(page, size), wrapper);

        Page<ProblemListItemVO> voPage =
                new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(ProblemListItemVO::of).toList());
        return voPage;
    }

    @Override
    public Problem getById(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return problem;
    }

    @Override
    public Problem getEnabled(Long id) {
        Problem problem = problemMapper.selectById(id);
        // 不存在/未上架则抛 40002（论文 5.3）
        if (problem == null || !Boolean.TRUE.equals(problem.getEnabled())) {
            throw new BizException(ResultCode.PROBLEM_UNAVAILABLE);
        }
        return problem;
    }

    @Override
    public Long create(ProblemFormDTO form) {
        Problem p = new Problem();
        p.setTitle(form.getTitle());
        p.setDescription(form.getDescription());
        p.setSamples(form.getSamples());
        p.setDifficulty(form.getDifficulty());
        p.setTimeLimitMs(form.getTimeLimitMs());
        p.setMemoryLimitMb(form.getMemoryLimitMb());
        p.setLanguages(form.getLanguages());
        p.setTags(form.getTags());
        p.setEnabled(form.getEnabled());
        p.setSubmitCount(0);
        p.setAcceptedCount(0);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        problemMapper.insert(p);
        return p.getId();
    }

    @Override
    public void update(Long id, ProblemFormDTO form) {
        Problem p = getById(id);
        p.setTitle(form.getTitle());
        p.setDescription(form.getDescription());
        p.setSamples(form.getSamples());
        p.setDifficulty(form.getDifficulty());
        p.setTimeLimitMs(form.getTimeLimitMs());
        p.setMemoryLimitMb(form.getMemoryLimitMb());
        p.setLanguages(form.getLanguages());
        p.setTags(form.getTags());
        p.setEnabled(form.getEnabled());
        p.setUpdatedAt(LocalDateTime.now());
        problemMapper.updateById(p);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        if (problemMapper.hasReferences(id)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "题目已有提交或被课程/竞赛引用，请改为下架");
        }
        // 级联清理：题目删了用例还在，会留下永久孤儿数据（且旧用例可能被新题 ID 复用时误用）
        testCaseMapper.delete(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, id));
        problemMapper.deleteById(id);
    }
}
