package com.codeoj.server.service.impl;

import com.codeoj.common.constant.RedisKeys;
import com.codeoj.common.entity.User;
import com.codeoj.server.mapper.LeaderboardStatsMapper;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.service.LeaderboardService;
import com.codeoj.server.util.PageUtils;
import com.codeoj.server.vo.LeaderboardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排行榜服务实现（论文 5.6）。
 *
 * <p><b>数据源</b>：Redis ZSet {@code oj:leaderboard:ac}（score = AC 题数）为解题数权威来源，
 * 写侧由 oj-judge 投影数据库去重通过题数；展示用统计（总提交/通过率/最后活跃）与
 * <b>同分排序依据</b>从数据库聚合。
 *
 * <p><b>排名规则</b>：解题数降序 → 首次达成该解题数的时间升序 → userId 升序。
 * 早期实现只取 ZSet 倒序，同分时顺序由 member 字典序决定（等价于 userId 大小），
 * 两个人都做了 4 题时谁排前面纯属偶然，无法解释也无法复现。
 */
@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final LeaderboardStatsMapper statsMapper;

    public LeaderboardServiceImpl(StringRedisTemplate redis,
                                  UserMapper userMapper,
                                  LeaderboardStatsMapper statsMapper) {
        this.redis = redis;
        this.userMapper = userMapper;
        this.statsMapper = statsMapper;
    }

    @Override
    public LeaderboardVO page(long page, long size) {
        // page/size 归一（review P2-22）：page=0 会让下面的分页换算得到负下标，
        // 而 Redis 的 reverseRangeWithScores(key,-20,-1) 是「从末尾数第 20 个」，
        // 不会报错只会返回一段完全无关的数据；size=0 则静默返回空页。
        page = PageUtils.page(page);
        size = PageUtils.size(size);
        Long zsetSize = redis.opsForZSet().size(RedisKeys.LEADERBOARD_AC);
        long total = zsetSize == null ? 0 : zsetSize;

        List<LeaderboardVO.Item> items = new ArrayList<>();
        if (total > 0) {
            items = buildPage(page, size, total);
        }
        return new LeaderboardVO(total, items, buildSummary());
    }

    /**
     * 构建一页排行。
     *
     * <p>同分排序要求全局有序，无法只在"当前页"内排序（否则分页结果会错乱）。
     * 演示级数据量（数十~数百人）下直接全量取出排序再切片：
     * 一次 ZSet 读取 + 两次数据库批量查询，不产生 N+1。
     * 若日后用户量上千，应改为离线计算排名并落库/落缓存。
     */
    private List<LeaderboardVO.Item> buildPage(long page, long size, long total) {
        Set<ZSetOperations.TypedTuple<String>> all =
                redis.opsForZSet().reverseRangeWithScores(RedisKeys.LEADERBOARD_AC, 0, -1);
        if (all == null || all.isEmpty()) {
            return new ArrayList<>();
        }

        // 先解析出干净的 userId 列表：脏 member 必须在使用前剔除，
        // 否则它会一路传进 selectBatchIds 的 IN 列表（review P2-23）
        List<ZSetOperations.TypedTuple<String>> rows = new ArrayList<>(all.size());
        List<Long> userIds = new ArrayList<>(all.size());
        Set<Long> seen = new HashSet<>();
        for (ZSetOperations.TypedTuple<String> tuple : all) {
            Long userId = parseMember(tuple.getValue());
            if (userId == null) {
                continue;
            }
            rows.add(tuple);
            if (seen.add(userId)) {
                userIds.add(userId);
            }
        }
        if (rows.isEmpty() || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 一次查出：用户信息、提交统计、首次 AC 时间
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, LeaderboardStatsMapper.UserSubmitStat> stats = statsMapper
                .submitStatsByUsers(userIds).stream()
                .collect(Collectors.toMap(LeaderboardStatsMapper.UserSubmitStat::getUserId,
                        Function.identity()));
        Map<Long, List<LocalDateTime>> firstAcs = new HashMap<>();
        for (LeaderboardStatsMapper.FirstAcRow row : statsMapper.firstAcTimes(userIds)) {
            firstAcs.computeIfAbsent(row.getUserId(), k -> new ArrayList<>()).add(row.getAcAt());
        }

        // 组装 + 计算同分排序键
        record Ranked(long acCount, LocalDateTime achievedAt, Long userId, LeaderboardVO.Item item) { }

        List<Ranked> ranked = new ArrayList<>(rows.size());
        for (var tuple : rows) {
            Long userId = parseMember(tuple.getValue());
            long acCount = tuple.getScore() == null ? 0 : tuple.getScore().longValue();
            // 首次达成"当前解题数"的时刻：把该用户的首次 AC 时间升序排，取第 acCount 个
            LocalDateTime achievedAt = null;
            List<LocalDateTime> times = firstAcs.get(userId);
            if (times != null && acCount > 0) {
                List<LocalDateTime> sorted = times.stream().sorted().toList();
                int idx = (int) Math.min(acCount, sorted.size()) - 1;
                if (idx >= 0) {
                    achievedAt = sorted.get(idx);
                }
            }

            LeaderboardStatsMapper.UserSubmitStat st = stats.get(userId);
            long totalSubmit = st != null && st.getTotalSubmit() != null ? st.getTotalSubmit() : 0;
            long acceptedSubmit = st != null && st.getAcceptedSubmit() != null ? st.getAcceptedSubmit() : 0;
            User u = users.get(userId);

            LeaderboardVO.Item item = new LeaderboardVO.Item(
                    userId,
                    u != null ? u.getUsername() : null,
                    acCount,
                    0,
                    totalSubmit,
                    acceptedSubmit,
                    totalSubmit == 0 ? 0 : Math.round(acceptedSubmit * 1000.0 / totalSubmit) / 10.0,
                    st != null ? st.getLastSubmitAt() : null);
            ranked.add(new Ranked(acCount, achievedAt, userId, item));
        }

        // 排序：解题数降序 → 达成时间升序（null 视为最晚）→ userId 升序兜底
        ranked.sort(Comparator
                .comparingLong(Ranked::acCount).reversed()
                .thenComparing(r -> r.achievedAt() == null ? LocalDateTime.MAX : r.achievedAt())
                .thenComparing(Ranked::userId));

        // 切片 + 回填名次
        int from = page - 1 > ranked.size() / size ? ranked.size() : (int) ((page - 1) * size);
        int to = (int) Math.min(from + size, ranked.size());
        List<LeaderboardVO.Item> pageItems = new ArrayList<>();
        for (int i = from; i < to; i++) {
            LeaderboardVO.Item it = ranked.get(i).item();
            it.setRank(i + 1L);
            pageItems.add(it);
        }
        return pageItems;
    }

    /**
     * 解析 ZSet member 为 userId，脏数据返回 null（review P2-23）。
     *
     * <p><b>为什么要兜这个</b>：ZSet 里的 member 在历史版本中曾被写成带引号的 JSON 串
     * （{@code "\"40\""}），这类残留值会让 {@code Long.valueOf} 抛
     * {@link NumberFormatException}，进而把整个排行榜变成 50000。
     * 一条脏 member 不该让所有人看不到榜——跳过并记日志，交给
     * {@code LeaderboardReconcileTask} 在下一轮对账时清掉。
     */
    private static Long parseMember(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            log.warn("skip dirty leaderboard member: {}", raw);
            return null;
        }
    }

    /**
     * 顶部统计条：人数 / 最高解题数 / 全站提交 / 全站通过率
     */
    private LeaderboardVO.Summary buildSummary() {
        LeaderboardStatsMapper.OverviewRow ov = statsMapper.overview();
        long solverCount = ov != null && ov.getSolverCount() != null ? ov.getSolverCount() : 0;
        long totalSubmit = ov != null && ov.getTotalSubmit() != null ? ov.getTotalSubmit() : 0;
        long accepted = ov != null && ov.getAcceptedSubmit() != null ? ov.getAcceptedSubmit() : 0;
        double rate = totalSubmit == 0 ? 0
                : Math.round(accepted * 1000.0 / totalSubmit) / 10.0;

        // 全站概览与页码无关；第二页首项不是全站最高分。
        long maxAc = 0;
        Set<ZSetOperations.TypedTuple<String>> top =
                redis.opsForZSet().reverseRangeWithScores(RedisKeys.LEADERBOARD_AC, 0, 0);
        if (top != null && !top.isEmpty()) {
            Double s = top.iterator().next().getScore();
            maxAc = s == null ? 0 : s.longValue();
        }
        return new LeaderboardVO.Summary(solverCount, maxAc, totalSubmit, rate);
    }

    /**
     * AC 时更新排行（论文 5.6）：ZINCRBY oj:leaderboard:ac。
     *
     * <p><b>保留但不由 oj-server 调用</b>：ZSet 的写入是 oj-judge 的职责
     * （CONTRACT §7：判题结果落库后由判题侧投影排行榜）。oj-server 只读该 ZSet，
     * 对账任务也只会覆盖校正而不是累加，所以这里的方法在本服务内没有调用点。
     * 保留它是为了维持接口语义的完整性（排行榜的"写"能力属于同一领域），
     * 同时避免日后有人以为"服务端没有写入口"而另外造一个重复实现。
     */
    @Override
    public void incr(Long userId) {
        // AC 时更新排行（论文 5.6）：ZINCRBY oj:leaderboard:ac
        redis.opsForZSet().incrementScore(RedisKeys.LEADERBOARD_AC, String.valueOf(userId), 1);
        log.debug("leaderboard incr, userId={}", userId);
    }
}
