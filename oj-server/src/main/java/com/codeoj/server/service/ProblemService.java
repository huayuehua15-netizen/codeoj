package com.codeoj.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.server.dto.ProblemFormDTO;

/**
 * 题目服务（论文表 3-3 题目管理：增删改查、上下架、限制配置）。
 */
public interface ProblemService {

    /** 分页列表（关键词/难度/标签筛选，仅上架题目对普通用户可见） */
    Page<Problem> page(String keyword, Integer difficulty, String tag, long page, long size, boolean isAdmin);

    /** 题面、样例与限制 */
    Problem getById(Long id);

    /** 题目不存在/未上架则抛 40002（论文 5.3） */
    Problem getEnabled(Long id);

    Long create(ProblemFormDTO form);

    void update(Long id, ProblemFormDTO form);

    void delete(Long id);
}
