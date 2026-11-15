package com.codeoj.server.service;

import com.codeoj.server.dto.ContestFormDTO;
import com.codeoj.server.vo.ContestRankVO;

import java.util.List;

/**
 * 竞赛服务（论文 7.2 展望落地：竞赛模式）。
 * 排名规则（ACM 赛制简化）：解题数降序，同解题数按总罚时升序；
 * 罚时 = Σ(每题首次 AC 距开赛的分钟数 + 该题错误提交次数 × 20)。
 */
public interface ContestService {

    Long create(Long teacherId, ContestFormDTO dto);

    void update(Long id, ContestFormDTO dto);

    void delete(Long id);

    /** 全部竞赛（按开始时间倒序），附状态与题目数 */
    List<com.codeoj.server.vo.ContestVO> listAll();

    /** 竞赛详情（含按顺序的题目列表） */
    com.codeoj.server.vo.ContestDetailVO detail(Long id);

    /** ACM 排名（比赛时间窗内的提交按赛制聚合） */
    List<ContestRankVO> ranking(Long id);
}
