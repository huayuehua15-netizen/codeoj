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
 */
@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final ProblemMapper problemMapper;

    public FavoriteController(FavoriteMapper favoriteMapper, ProblemMapper problemMapper) {
        this.favoriteMapper = favoriteMapper;
        this.problemMapper = problemMapper;
    }

    /** 收藏/取消收藏（toggle），返回当前是否已收藏；题目不存在直接 40400 */
    @PostMapping("/problems/{id}/favorite")
    public R<Boolean> toggle(@PathVariable(name = "id") Long problemId,
                             @AuthenticationPrincipal LoginUser user) {
        if (problemMapper.selectById(problemId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.userId())
                .eq(Favorite::getProblemId, problemId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return R.ok(false);
        }
        Favorite f = new Favorite();
        f.setUserId(user.userId());
        f.setProblemId(problemId);
        f.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(f);
        return R.ok(true);
    }

    /** 当前题目的收藏状态（详情页按钮初始态） */
    @GetMapping("/problems/{id}/favorite")
    public R<Boolean> favorited(@PathVariable(name = "id") Long problemId,
                                @AuthenticationPrincipal LoginUser user) {
        if (problemMapper.selectById(problemId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.userId())
                .eq(Favorite::getProblemId, problemId));
        return R.ok(count != null && count > 0);
    }

    /** 我的收藏列表（个人中心） */
    @GetMapping("/favorites")
    public R<List<Map<String, Object>>> myFavorites(@AuthenticationPrincipal LoginUser user) {
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.userId())
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
}
