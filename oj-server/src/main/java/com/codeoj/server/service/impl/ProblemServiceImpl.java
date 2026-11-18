package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.Problem;
import com.codeoj.common.entity.TestCase;
import com.codeoj.common.enums.Language;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.ProblemFormDTO;
import com.codeoj.server.mapper.ProblemMapper;
import com.codeoj.server.mapper.TestCaseMapper;
import com.codeoj.server.service.ProblemService;
import com.codeoj.server.util.PageUtils;
import com.codeoj.server.vo.ProblemListItemVO;
import com.codeoj.server.vo.ProblemOverviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目服务实现（论文表 3-3：题目增删改查与上下架）。
 */
@Service
public class ProblemServiceImpl implements ProblemService {

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;

    public ProblemServiceImpl(ProblemMapper problemMapper, TestCaseMapper testCaseMapper) {
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    public ProblemOverviewVO overview() {
        // 只读取轻量元信息；不返回题面、隐藏题目和判题用例。
        List<Problem> available = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                .select(Problem::getId, Problem::getTags).eq(Problem::getEnabled, true));
        List<String> tags = available.stream().filter(p -> p.getTags() != null)
                .flatMap(p -> p.getTags().stream()).filter(Objects::nonNull)
                .map(String::trim).filter(t -> !t.isEmpty()).distinct().sorted().toList();
        return new ProblemOverviewVO(available.size(), tags);
    }

    @Override
    public Page<ProblemListItemVO> page(String keyword, Integer difficulty, String tag, long page, long size, boolean isAdmin) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(Problem::getEnabled, true);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Problem::getTitle, keyword);
        }
        if (difficulty != null && difficulty > 0) {
            wrapper.eq(Problem::getDifficulty, difficulty);
        }
        // 标签筛选：tags 为 JSON 列，JSON_CONTAINS 参数化传值（无拼接，防注入）
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag);
        }
        // 列表只查轻量列：description 是 LONGTEXT、samples 是 JSON，
        // 一页 10 条全带会把整份题面塞进响应（API.md §2.1：列表不返回大字段）
        wrapper.select(Problem::getId, Problem::getTitle, Problem::getTags,
                Problem::getDifficulty, Problem::getTimeLimitMs, Problem::getMemoryLimitMb,
                Problem::getSubmitCount, Problem::getAcceptedCount, Problem::getEnabled,
                Problem::getCreatedAt, Problem::getUpdatedAt);
        wrapper.orderByAsc(Problem::getId);
        // page/size 下限由 PageUtils 统一夹取（review P2-25）：
        // MyBatis-Plus 的 maxLimit 只封上限，page=0 会算出负 offset
        Page<Problem> result = problemMapper.selectPage(PageUtils.of(page, size), wrapper);

        Page<ProblemListItemVO> voPage =
                new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(ProblemListItemVO::of).toList());
        return voPage;
    }

    @Override
    public Problem getById(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return problem;
    }

    @Override
    public Problem getEnabled(Long id) {
        Problem problem = problemMapper.selectById(id);
        // 不存在/未上架则抛 40002（论文 5.3）
        if (problem == null || !Boolean.TRUE.equals(problem.getEnabled())) {
            throw new BizException(ResultCode.PROBLEM_UNAVAILABLE);
        }
        return problem;
    }

    @Override
    public Long create(ProblemFormDTO form) {
        requireAllowedLanguages(form.getLanguages());
        Problem p = new Problem();
        p.setTitle(form.getTitle());
        p.setDescription(form.getDescription());
        p.setSamples(form.getSamples());
        p.setDifficulty(form.getDifficulty());
        p.setTimeLimitMs(form.getTimeLimitMs());
        p.setMemoryLimitMb(form.getMemoryLimitMb());
        p.setLanguages(form.getLanguages());
        p.setTags(form.getTags());
        p.setEnabled(form.getEnabled());
        p.setSubmitCount(0);
        p.setAcceptedCount(0);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        problemMapper.insert(p);
        return p.getId();
    }

    @Override
    public void update(Long id, ProblemFormDTO form) {
        requireAllowedLanguages(form.getLanguages());
        Problem p = getById(id);
        p.setTitle(form.getTitle());
        p.setDescription(form.getDescription());
        p.setSamples(form.getSamples());
        p.setDifficulty(form.getDifficulty());
        p.setTimeLimitMs(form.getTimeLimitMs());
        p.setMemoryLimitMb(form.getMemoryLimitMb());
        p.setLanguages(form.getLanguages());
        p.setTags(form.getTags());
        p.setEnabled(form.getEnabled());
        p.setUpdatedAt(LocalDateTime.now());
        problemMapper.updateById(p);
    }

    /**
     * 语言白名单校验（review P2-40）。
     *
     * <p><b>为什么 DTO 上的 {@code @Pattern} 还不够</b>：DTO 里的正则
     * {@code "c|cpp|java|python3|js"} 是把 {@link Language} 的 code 抄了一遍，
     * 两处各写一份就一定会漂移——新增语言时改了枚举忘了改正则（或反过来），
     * 数据库里就会出现 {@code languages=[\"go\"]} 这种"题目允许、判题镜像却不存在"的题，
     * 学生点提交只会拿到一个必然失败的判题任务。这里以枚举为唯一权威再做一次业务校验。
     *
     * <p>非空校验同样放在服务层：{@code @NotEmpty} 只在走 {@code @Valid} 的 HTTP 入口生效，
     * 其它调用方（测试、未来的批量导入）绕开校验器时会写出一个谁都不能提交的题。
     */
    private static void requireAllowedLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "语言不能为空");
        }
        Set<String> allowed = Arrays.stream(Language.values())
                .map(Language::getCode)
                .collect(Collectors.toSet());
        for (String code : languages) {
            if (code == null || !allowed.contains(code)) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                        "不支持的语言：" + code + "，可选值 " + allowed);
            }
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        if (problemMapper.hasReferences(id)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "题目已有提交或被课程/竞赛引用，请改为下架");
        }
        // 级联清理：题目删了用例还在，会留下永久孤儿数据（且旧用例可能被新题 ID 复用时误用）
        testCaseMapper.delete(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, id));
        problemMapper.deleteById(id);
    }
}
