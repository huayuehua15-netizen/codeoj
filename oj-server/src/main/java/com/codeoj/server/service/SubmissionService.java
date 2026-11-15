package com.codeoj.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.Submission;
import com.codeoj.server.dto.SubmitDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.vo.StatisticsVO;
import com.codeoj.server.vo.SubmissionDetailVO;
import com.codeoj.server.vo.SubmissionListItemVO;
import com.codeoj.server.vo.SubmitVO;

import java.time.LocalDateTime;

/**
 * 提交服务（论文表 3-3：提交校验、限流、幂等、异步入队；提交记录查询；用例级判题详情）。
 */
public interface SubmissionService {

    /**
     * 提交链路入口（论文 5.3）：限流检查 → 幂等锁 → 题目与语言校验 → 事务写库 → 消息发布。
     */
    SubmitVO submit(LoginUser user, SubmitDTO dto);

    /** 事务写库 status=0（论文 5.3） */
    Long createQueued(Long userId, SubmitDTO dto);

    /** 语言允许、代码 ≤64KB 校验（论文 5.3 validate） */
    void validate(Problem problem, SubmitDTO dto);

    /** 判题详情（用例级），他人代码不可见（论文表 3-3 / F-13） */
    SubmissionDetailVO detail(Long id, LoginUser user);

    /** 提交记录：按题目/状态/时间筛选（论文 3.3） */
    Page<SubmissionListItemVO> pageMy(LoginUser user, Long problemId, Integer status,
                                      LocalDateTime begin, LocalDateTime end, long page, long size);

    /** 个人通过率统计（论文 5.6：按难度分组聚合） */
    StatisticsVO statistics(Long userId);
}
