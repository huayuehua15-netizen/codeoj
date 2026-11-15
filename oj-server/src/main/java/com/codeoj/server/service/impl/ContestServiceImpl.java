package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.Contest;
import com.codeoj.common.entity.ContestProblem;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.entity.User;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.ContestFormDTO;
import com.codeoj.server.mapper.ContestMapper;
import com.codeoj.server.mapper.ContestProblemMapper;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.mapper.SubmissionMapper;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.service.ContestService;
import com.codeoj.server.vo.ContestDetailVO;
import com.codeoj.server.vo.ContestRankVO;
import com.codeoj.server.vo.ContestVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 竞赛服务实现（论文 7.2 展望落地）。
 * 排名聚合在 Java 侧完成（演示级数据量）：比赛窗口内、比赛题目集上的提交，
 * 按用户聚合出"每题首次 AC 时间 / 错误次数"，计算 ACM 简化罚时。
 */
@Service
public class ContestServiceImpl implements ContestService {

    /** 每次错误提交的罚时（分钟，ACM 惯例 20） */
    private static final long WRONG_PENALTY_MINUTES = 20;

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final com.codeoj.server.mapper.UserMapper userMapper;

    public ContestServiceImpl(ContestMapper contestMapper,
                              ContestProblemMapper contestProblemMapper,
                              ProblemMapper problemMapper,
                              SubmissionMapper submissionMapper,
                              com.codeoj.server.mapper.UserMapper userMapper) {
        this.contestMapper = contestMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Long create(Long teacherId, ContestFormDTO dto) {
        validateTime(dto);
        Contest c = new Contest();
        c.setTitle(dto.getTitle());
        c.setDescription(dto.getDescription());
        c.setStartTime(dto.getStartTime());
        c.setEndTime(dto.getEndTime());
        c.setCreatedBy(teacherId);
        c.setCreatedAt(LocalDateTime.now());
        contestMapper.insert(c);
        bindProblems(c.getId(), dto.getProblemIds());
        return c.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ContestFormDTO dto) {
        Contest c = contestMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        validateTime(dto);
        c.setTitle(dto.getTitle());
        c.setDescription(dto.getDescription());
        c.setStartTime(dto.getStartTime());
        c.setEndTime(dto.getEndTime());
        contestMapper.updateById(c);
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, id));
        bindProblems(id, dto.getProblemIds());
    }

    private void validateTime(ContestFormDTO dto) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "开始时间必须早于结束时间");
        }
    }

    private void bindProblems(Long contestId, List<Long> problemIds) {
        if (problemIds == null) {
            return;
        }
        for (int i = 0; i < problemIds.size(); i++) {
            ContestProblem cp = new ContestProblem();
            cp.setContestId(contestId);
            cp.setProblemId(problemIds.get(i));
            cp.setSortOrder(i);
            contestProblemMapper.insert(cp);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Contest c = contestMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        contestMapper.deleteById(id);
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, id));
    }

    @Override
    public List<ContestVO> listAll() {
        List<Contest> contests = contestMapper.selectList(
                new LambdaQueryWrapper<Contest>().orderByDesc(Contest::getStartTime));
        LocalDateTime now = LocalDateTime.now();
        List<ContestVO> result = new ArrayList<>();
        for (Contest c : contests) {
            Long count = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                    .eq(ContestProblem::getContestId, c.getId()));
            result.add(new ContestVO(c.getId(), c.getTitle(), c.getDescription(),
                    c.getStartTime(), c.getEndTime(), c.statusOf(now),
                    count == null ? 0 : count.intValue()));
        }
        return result;
    }

    @Override
    public ContestDetailVO detail(Long id) {
        Contest c = contestMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        List<ContestProblem> cps = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, id)
                        .orderByAsc(ContestProblem::getSortOrder));

        List<ContestDetailVO.Item> items = new ArrayList<>();
        for (ContestProblem cp : cps) {
            Problem p = problemMapper.selectById(cp.getProblemId());
            if (p != null) {
                items.add(new ContestDetailVO.Item(
                        labelOf(cp.getSortOrder()), p.getId(), p.getTitle(), p.getDifficulty()));
            }
        }
        return new ContestDetailVO(c.getId(), c.getTitle(), c.getDescription(),
                c.getStartTime(), c.getEndTime(), c.statusOf(LocalDateTime.now()), items);
    }

    /** 比赛题目按顺序对应字母题号（0=A, 1=B…） */
    private String labelOf(int sortOrder) {
        return String.valueOf((char) ('A' + sortOrder));
    }

    @Override
    public List<ContestRankVO> ranking(Long id) {
        Contest c = contestMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        List<ContestProblem> cps = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, id));
        if (cps.isEmpty()) {
            return List.of();
        }
        // 题目集合与字母题号映射
        Map<Long, String> labels = new HashMap<>();
        for (ContestProblem cp : cps) {
            labels.put(cp.getProblemId(), labelOf(cp.getSortOrder()));
        }

        // 比赛窗口内、题目集上的全部提交，按用户聚合
        List<Long> problemIds = cps.stream().map(ContestProblem::getProblemId).toList();
        var submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .in(Submission::getProblemId, problemIds)
                .between(Submission::getCreatedAt, c.getStartTime(), c.getEndTime())
                .orderByAsc(Submission::getCreatedAt));

        // per-user 聚合：每题首次 AC 时间、每题错误次数（AC 前的）
        record PerUser(Map<Long, LocalDateTime> firstAc, Map<Long, Integer> wrongCount) { }
        Map<Long, PerUser> byUser = new HashMap<>();
        for (var s : submissions) {
            var pu = byUser.computeIfAbsent(s.getUserId(),
                    k -> new PerUser(new HashMap<>(), new TreeMap<>()));
            if (s.getStatus() == SubmissionStatus.ACCEPTED.getCode()) {
                pu.firstAc().putIfAbsent(s.getProblemId(), s.getCreatedAt());
            } else if (s.getStatus() != SubmissionStatus.COMPILE_ERROR.getCode()
                    && pu.firstAc().get(s.getProblemId()) == null) {
                // 该题尚未 AC 的错误提交计罚时（CE 不计，ACM 惯例）
                pu.wrongCount().merge(s.getProblemId(), 1, Integer::sum);
            }
        }

        // 组装排名：解题数降序 → 罚时升序
        List<ContestRankVO> ranks = new ArrayList<>();
        for (var entry : byUser.entrySet()) {
            Long uid = entry.getKey();
            var pu = entry.getValue();
            int solved = pu.firstAc().size();
            long penaltyMinutes = 0;
            StringBuilder detail = new StringBuilder();
            List<String> parts = new ArrayList<>();
            for (var acEntry : new TreeMap<>(pu.firstAc()).entrySet()) {
                String label = labels.getOrDefault(acEntry.getKey(), "?");
                long minutes = Duration.between(c.getStartTime(), acEntry.getValue()).toMinutes();
                long wrong = pu.wrongCount().getOrDefault(acEntry.getKey(), 0);
                penaltyMinutes += minutes + wrong * WRONG_PENALTY_MINUTES;
                parts.add(label + " ✅" + minutes + "min" + (wrong > 0 ? " (-" + wrong + ")" : ""));
            }
            int wrongNoAc = pu.wrongCount().values().stream().mapToInt(Integer::intValue).sum();
            if (wrongNoAc > 0 && solved < labels.size()) {
                parts.add("未解题目错误 ×" + wrongNoAc);
            }
            if (solved > 0) {
                detail.append(String.join(" ｜ ", parts));
            }
            ranks.add(new ContestRankVO(0, uid, null, solved, penaltyMinutes,
                    detail.toString()));
        }
        // 批量补齐用户名
        List<Long> rankUserIds = ranks.stream().map(ContestRankVO::getUserId).toList();
        Map<Long, String> names = new HashMap<>();
        if (!rankUserIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(rankUserIds)) {
                names.put(u.getId(), u.getUsername());
            }
        }
        for (ContestRankVO r : ranks) {
            r.setUsername(names.get(r.getUserId()));
        }
        ranks.sort(Comparator.comparingInt(ContestRankVO::getSolved).reversed()
                .thenComparingLong(ContestRankVO::getPenalty));
        for (int i = 0; i < ranks.size(); i++) {
            ranks.get(i).setRank(i + 1);
        }
        return ranks;
    }
}
