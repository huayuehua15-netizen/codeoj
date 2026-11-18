package com.codeoj.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.Favorite;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.R;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.mapper.FavoriteMapper;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题目收藏接口：详情页收藏/取消（toggle），个人中心收藏列表。
 *
 * <p><b>为何显式判空而不是加 @PreAuthorize</b>：本控制器挂在
 * /api/problems/{id}/favorite 下，而 SecurityConfig 对 {@code /api/problems/**}
 * 是 permitAll（题目浏览对游客公开）。匿名请求会让 {@code @AuthenticationPrincipal}
 * 为 null，直接调用 {@code user.userId()} 会空指针并被兜底成 50000；
 * 而契约（API.md §0.3）要求未登录统一返回 <b>40100</b>,
 * 不是 40300（@PreAuthorize 拒绝走的是 AccessDenied）。故统一走 {@link #requireLogin}。
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final ProblemMapper problemMapper;

    public FavoriteController(FavoriteMapper favoriteMapper, ProblemMapper problemMapper) {
        this.favoriteMapper = favoriteMapper;
        this.problemMapper = problemMapper;
    }

    /** 收藏/取消收藏（toggle），返回当前是否已收藏；题目不存在/未上架见 {@link #requireEnabledProblem} */
    @PostMapping("/problems/{id}/favorite")
    public R<Boolean> toggle(@PathVariable(name = "id") Long problemId,
                             @AuthenticationPrincipal LoginUser user) {
        LoginUser me = requireLogin(user);
        requireEnabledProblem(problemId);
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, me.userId())
                .eq(Favorite::getProblemId, problemId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return R.ok(false);
        }
        Favorite f = new Favorite();
        f.setUserId(me.userId());
        f.setProblemId(problemId);
        f.setCreatedAt(LocalDateTime.now());
        try {
            favoriteMapper.insert(f);
        } catch (DuplicateKeyException e) {
            // 双击/并发请求下两个线程都读到"未收藏"，后插入的会撞 uk_user_problem。
            // toggle 的语义本来就是"切到已收藏"，此行已存在即目标状态已达成，
            // 按幂等成功返回——让用户因为多点了一次而看到 50000 才是真正的错误。
            log.debug("favorite already exists, treat as idempotent success, userId={}, problemId={}",
                    me.userId(), problemId);
        }
        return R.ok(true);
    }

    /** 当前题目的收藏状态（详情页按钮初始态） */
    @GetMapping("/problems/{id}/favorite")
    public R<Boolean> favorited(@PathVariable(name = "id") Long problemId,
                                @AuthenticationPrincipal LoginUser user) {
        LoginUser me = requireLogin(user);
        requireEnabledProblem(problemId);
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, me.userId())
                .eq(Favorite::getProblemId, problemId));
        return R.ok(count != null && count > 0);
    }

    /**
     * 收藏只允许针对"存在且已上架"的题目：不存在抛 40400，存在但已下架抛 40002。
     *
     * <p><b>为什么要校验上架状态</b>：收藏是"我以后要做这题"的候选题单，不是归档夹。
     * 允许收藏已下架或不存在的 ID，个人收藏页就会长期挂着点不进去的死链
     * （详情接口对下架题返回 40002）。这里与 {@code ProblemServiceImpl.getEnabled}
     * 保持同一口径，收藏入口与详情入口的可见性才不会互相矛盾。
     */
    private Problem requireEnabledProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(problem.getEnabled())) {
            throw new BizException(ResultCode.PROBLEM_UNAVAILABLE);
        }
        return problem;
    }

    /** 我的收藏列表（个人中心） */
    @GetMapping("/favorites")
    public R<List<Map<String, Object>>> myFavorites(@AuthenticationPrincipal LoginUser user) {
        LoginUser me = requireLogin(user);
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, me.userId())
                .orderByDesc(Favorite::getId));
        List<Long> pids = favs.stream().map(Favorite::getProblemId).distinct().toList();
        Map<Long, Problem> problems = pids.isEmpty() ? Map.of()
                : problemMapper.selectBatchIds(pids).stream()
                        .collect(Collectors.toMap(Problem::getId, p -> p));
        return R.ok(favs.stream()
                .filter(f -> problems.containsKey(f.getProblemId()))
                .map(f -> {
                    Problem p = problems.get(f.getProblemId());
                    return Map.<String, Object>of(
                            "problemId", (Object) p.getId(),
                            "title", p.getTitle(),
                            "difficulty", p.getDifficulty(),
                            "favoritedAt", String.valueOf(f.getCreatedAt()));
                })
                .collect(Collectors.toList()));
    }

    /**
     * 未登录统一抛 40100。_路径级 permitAll 让匿名请求能进到方法体，
     * 因此认证前提必须在这里显式表达。_
     */
    private static LoginUser requireLogin(LoginUser user) {
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}
