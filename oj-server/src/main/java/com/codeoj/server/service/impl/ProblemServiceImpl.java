package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.ProblemFormDTO;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.service.ProblemService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 题目服务实现（骨架：列表/详情已可用；增删改查为第二阶段补全项）。
 */
@Service
public class ProblemServiceImpl implements ProblemService {

    private final ProblemMapper problemMapper;

    public ProblemServiceImpl(ProblemMapper problemMapper) {
        this.problemMapper = problemMapper;
    }

    @Override
    public Page<Problem> page(String keyword, Integer difficulty, String tag, long page, long size, boolean isAdmin) {
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
        wrapper.orderByAsc(Problem::getId);
        return problemMapper.selectPage(new Page<>(page, size), wrapper);
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
        // TODO 第二阶段：字段组装 + 样例/语言 JSON 落库
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
        // TODO 第二阶段：完整更新逻辑
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
    public void delete(Long id) {
        // TODO 第二阶段：删除题目并级联清理用例关联
        getById(id);
        problemMapper.deleteById(id);
    }
}
