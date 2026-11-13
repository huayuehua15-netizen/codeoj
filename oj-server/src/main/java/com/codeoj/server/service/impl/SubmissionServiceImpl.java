package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.SubmitDTO;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.mapper.SubmissionMapper;
import com.codeoj.server.mq.JudgeDispatcher;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.IdempotentLockService;
import com.codeoj.server.service.RateLimiter;
import com.codeoj.server.service.SubmissionService;
import com.codeoj.server.vo.StatisticsVO;
import com.codeoj.server.vo.SubmissionDetailVO;
import com.codeoj.server.vo.SubmissionListItemVO;
import com.codeoj.server.vo.SubmitVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交服务实现（论文 5.3）：提交接口是判题链路的入口，依次完成
 * 限流检查、幂等锁、题目与语言校验、事务写库与消息发布，任何一步失败均不产生脏数据。
 * 消息在事务提交之后发送，避免"库未落盘、消息先到"导致消费者查无此单。
 */
@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final RateLimiter rateLimiter;
    private final IdempotentLockService lockService;
    private final JudgeDispatcher judgeDispatcher;

    @Value("${oj.submit.max-code-bytes:65536}")
    private int maxCodeBytes;

    @Value("${oj.rate-limit.submit-per-minute:6}")
    private int submitPerMinute;

    public SubmissionServiceImpl(SubmissionMapper submissionMapper,
                                 ProblemMapper problemMapper,
                                 RateLimiter rateLimiter,
                                 IdempotentLockService lockService,
                                 JudgeDispatcher judgeDispatcher) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.rateLimiter = rateLimiter;
        this.lockService = lockService;
        this.judgeDispatcher = judgeDispatcher;
    }

    @Override
    public SubmitVO submit(LoginUser user, SubmitDTO dto) {
        rateLimiter.check("submit:" + user.userId(), String.valueOf(user.userId()), submitPerMinute, 60);
        if (!lockService.tryLock("oj:lock:submit:" + user.userId() + ":" + dto.getProblemId(), 2)) {
            throw new BizException(42901, "提交过于频繁，请稍后再试");
        }
        // 不存在/未上架则抛 40002（论文 5.3）
        Problem p = problemMapper.selectById(dto.getProblemId());
        if (p == null || !Boolean.TRUE.equals(p.getEnabled())) {
            throw new BizException(ResultCode.PROBLEM_UNAVAILABLE);
        }
        validate(p, dto);
        long sid = createQueued(user.userId(), dto);
        // 发布消息（仅提交 ID）；应在事务提交后发布，此处 createQueued 已独立提交
        judgeDispatcher.publish(sid);
        return SubmitVO.of(sid, SubmissionStatus.QUEUING);
    }

    @Transactional
    @Override
    public Long createQueued(Long userId, SubmitDTO dto) {
        Submission s = new Submission();
        s.setUserId(userId);
        s.setProblemId(dto.getProblemId());
        s.setLanguage(dto.getLanguage());
        s.setCode(dto.getCode());
        s.setStatus(SubmissionStatus.QUEUING.getCode());
        s.setCreatedAt(LocalDateTime.now());
        submissionMapper.insert(s);
        log.info("submission queued, id={}, user={}, problem={}", s.getId(), userId, dto.getProblemId());
        return s.getId();
    }

    @Override
    public void validate(Problem problem, SubmitDTO dto) {
        if (problem.getLanguages() == null || !problem.getLanguages().contains(dto.getLanguage())) {
            throw new BizException(ResultCode.SUBMIT_INVALID.getCode(), "该题目不允许使用此语言提交");
        }
        if (dto.getCode().getBytes(StandardCharsets.UTF_8).length > maxCodeBytes) {
            throw new BizException(ResultCode.SUBMIT_INVALID.getCode(), "代码不得超过 64KB");
        }
    }

    // ---------- 骨架阶段 TODO：以下方法在第二阶段补全 ----------

    @Override
    public SubmissionDetailVO detail(Long id, LoginUser user) {
        // TODO 第二阶段：查提交 + 题目标题 + judge_case 明细；权限：本人/管理员
        throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "接口尚未实现（骨架阶段）");
    }

    @Override
    public Page<SubmissionListItemVO> pageMy(LoginUser user, Long problemId, Integer status,
                                             long page, long size) {
        // TODO 第二阶段：按题目/状态/时间筛选 + 关联题目标题
        return new Page<>(page, size);
    }

    @Override
    public StatisticsVO statistics(Long userId) {
        // TODO 第二阶段：按难度分组聚合（论文 5.6 SQL）
        return new StatisticsVO(List.of(), 0, 0);
    }
}
