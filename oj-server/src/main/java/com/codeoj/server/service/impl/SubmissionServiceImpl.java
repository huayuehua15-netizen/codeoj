package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.JudgeCase;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.Difficulty;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.SubmitDTO;
import com.codeoj.server.mapper.JudgeCaseMapper;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.mapper.SubmissionMapper;
import com.codeoj.server.mq.JudgeDispatcher;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.IdempotentLockService;
import com.codeoj.server.service.RateLimiter;
import com.codeoj.server.service.SubmissionService;
import com.codeoj.server.vo.JudgeCaseVO;
import com.codeoj.server.vo.StatisticsVO;
import com.codeoj.server.vo.SubmissionDetailVO;
import com.codeoj.server.vo.SubmissionListItemVO;
import com.codeoj.server.vo.SubmitVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final JudgeCaseMapper judgeCaseMapper;
    private final RateLimiter rateLimiter;
    private final IdempotentLockService lockService;
    private final JudgeDispatcher judgeDispatcher;
    private final com.codeoj.server.ws.PushEventPublisher pushEventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Value("${oj.submit.max-code-bytes:65536}")
    private int maxCodeBytes;

    @Value("${oj.rate-limit.submit-per-minute:6}")
    private int submitPerMinute;

    public SubmissionServiceImpl(SubmissionMapper submissionMapper,
                                 ProblemMapper problemMapper,
                                 JudgeCaseMapper judgeCaseMapper,
                                 RateLimiter rateLimiter,
                                 IdempotentLockService lockService,
                                 JudgeDispatcher judgeDispatcher,
                                 com.codeoj.server.ws.PushEventPublisher pushEventPublisher,
                                 TransactionTemplate transactionTemplate) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.judgeCaseMapper = judgeCaseMapper;
        this.rateLimiter = rateLimiter;
        this.lockService = lockService;
        this.judgeDispatcher = judgeDispatcher;
        this.pushEventPublisher = pushEventPublisher;
        this.transactionTemplate = transactionTemplate;
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
        // 用 TransactionTemplate 显式开启事务：原先 this.createQueued(...) 是自调用，
        // Spring AOP 代理不生效，@Transactional 只是装饰。现在落库与提交在同一事务里，
        // 消息发布严格在 commit 之后，避免"库未落盘、消息先到"消费者查无此单。
        Long sid = transactionTemplate.execute(status -> createQueued(user.userId(), dto));
        // 事务已提交，发布判题消息（仅提交 ID）
        judgeDispatcher.publish(sid);
        // 推送阶段一：JUDGE_QUEUED（论文 4.5 三阶段推送）
        pushEventPublisher.publishQueued(user.userId(), sid);
        return SubmitVO.of(sid, SubmissionStatus.QUEUING);
    }

    /**
     * 写库（由 {@link #submit} 在 TransactionTemplate 内调用）。
     * 保留 @Transactional 是为了被外部直接调用时仍有事务语义，但主链路不依赖它。
     */
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

    // ---------- M1 实现：详情 / 记录 / 统计 ----------

    @Override
    public SubmissionDetailVO detail(Long id, LoginUser user) {
        Submission s = submissionMapper.selectById(id);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 他人代码不可见：仅本人与管理员（论文 4.7 / 测试 F-13）
        if (!s.getUserId().equals(user.userId()) && !user.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        SubmissionDetailVO vo = new SubmissionDetailVO();
        vo.setId(s.getId());
        vo.setUserId(s.getUserId());
        vo.setProblemId(s.getProblemId());
        Problem p = problemMapper.selectById(s.getProblemId());
        vo.setProblemTitle(p != null ? p.getTitle() : null);
        vo.setLanguage(s.getLanguage());
        vo.setCode(s.getCode());
        vo.setStatus(s.getStatus());
        vo.setStatusDesc(SubmissionStatus.of(s.getStatus()).getDesc());
        vo.setTimeUsedMs(s.getTimeUsedMs());
        vo.setMemoryUsedKb(s.getMemoryUsedKb());
        vo.setErrorInfo(s.getErrorInfo());
        vo.setJudgedAt(s.getJudgedAt());
        vo.setCreatedAt(s.getCreatedAt());
        // 用例级判题明细（论文表 4-5）
        List<JudgeCase> judgeCases = judgeCaseMapper.selectList(new LambdaQueryWrapper<JudgeCase>()
                .eq(JudgeCase::getSubmissionId, id)
                .orderByAsc(JudgeCase::getCaseIndex));
        vo.setCases(judgeCases.stream().map(JudgeCaseVO::of).toList());
        return vo;
    }

    @Override
    public Page<SubmissionListItemVO> pageMy(LoginUser user, Long problemId, Integer status,
                                             LocalDateTime begin, LocalDateTime end, long page, long size) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, user.userId());
        if (problemId != null) {
            wrapper.eq(Submission::getProblemId, problemId);
        }
        if (status != null) {
            wrapper.eq(Submission::getStatus, status);
        }
        // 时间筛选（论文 3.3）：提交时间落在 [begin, end]
        if (begin != null) {
            wrapper.ge(Submission::getCreatedAt, begin);
        }
        if (end != null) {
            wrapper.le(Submission::getCreatedAt, end);
        }
        wrapper.orderByDesc(Submission::getId);
        Page<Submission> result = submissionMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量补齐题目标题，避免 N+1 查询
        Map<Long, String> titles = result.getRecords().isEmpty() ? Map.of()
                : problemMapper.selectBatchIds(result.getRecords().stream()
                        .map(Submission::getProblemId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Problem::getId, Problem::getTitle));

        Page<SubmissionListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(s -> SubmissionListItemVO.of(s, titles.get(s.getProblemId())))
                .toList());
        return voPage;
    }

    @Override
    public StatisticsVO statistics(Long userId) {
        // 按难度分组聚合（论文 5.6 SQL）
        List<SubmissionMapper.DifficultyStatRow> rows = submissionMapper.statisticsByUser(userId);
        List<StatisticsVO.Item> items = rows.stream()
                .sorted(Comparator.comparing(SubmissionMapper.DifficultyStatRow::getDifficulty))
                .map(row -> new StatisticsVO.Item(
                        row.getDifficulty(),
                        Difficulty.of(row.getDifficulty()).getLabel(),
                        row.getSubmitCount() == null ? 0 : row.getSubmitCount(),
                        row.getAcceptedCount() == null ? 0 : row.getAcceptedCount()))
                .toList();
        long totalSubmit = items.stream().mapToLong(StatisticsVO.Item::getSubmit).sum();
        long totalAccepted = items.stream().mapToLong(StatisticsVO.Item::getAccepted).sum();
        return new StatisticsVO(items, totalSubmit, totalAccepted);
    }
}
