package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Contest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContestMapper extends BaseMapper<Contest> {

    /**
     * 一次查出每场比赛的题目数（review P2-17）。
     *
     * <p>原先 {@code listAll()} 在循环里对每场比赛 {@code selectCount}，N 场比赛就是 N 次往返；
     * 比赛列表是首页/公开接口，N 会随运营时间线性增长。这里用一条 {@code GROUP BY}
     * 取全量计数，再在内存里按 contestId 回填，往返次数固定为 1 + 1。
     *
     * <p>没有任何题目的比赛不会出现在 GROUP BY 结果里，调用方按"查不到即 0"处理。
     */
    @Select("SELECT cp.contest_id AS contestId, COUNT(*) AS problemCount "
            + "  FROM contest_problem cp "
            + " GROUP BY cp.contest_id")
    List<ContestProblemCountRow> countProblemsGrouped();

    /** GROUP BY 聚合行：contestId → 题目数 */
    class ContestProblemCountRow {
        private Long contestId;
        private Integer problemCount;

        public Long getContestId() {
            return contestId;
        }

        public Integer getProblemCount() {
            return problemCount;
        }
    }
}
