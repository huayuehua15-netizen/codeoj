package com.codeoj.judge.core;

import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.core.compare.OutputComparator;
import com.codeoj.judge.core.sandbox.DockerSandbox;
import com.codeoj.judge.core.strategy.CStrategy;
import com.codeoj.judge.core.strategy.CppStrategy;
import com.codeoj.judge.core.strategy.JavaStrategy;
import com.codeoj.judge.core.strategy.LanguageStrategy;
import com.codeoj.judge.core.strategy.Python3Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 判题引擎（论文 5.4）：按语言选择编译与运行方案，逐用例创建沙箱容器执行，
 * 比对输出（Fail-Fast：首个不匹配即停），汇总结果。
 * <p>
 * engine-mode 三模式：
 * mock  —— 全部判 AC，仅打通链路（无执行）；
 * local —— 本机真实判题：宿主机真实编译运行（Java/Python），无 Docker 环境的演示选择；
 * real  —— Docker 沙箱判题（M2，A 侧实现），生产环境使用。
 */
@Component
public class JudgeEngine {

    private static final Logger log = LoggerFactory.getLogger(JudgeEngine.class);

    private final DockerSandbox sandbox;
    private final OutputComparator comparator;
    private final com.codeoj.judge.core.LocalJudgeService localJudgeService;
    private final String engineMode;
    private final Map<String, LanguageStrategy> strategies = new HashMap<>();

    public JudgeEngine(DockerSandbox sandbox,
                       OutputComparator comparator,
                       com.codeoj.judge.core.LocalJudgeService localJudgeService,
                       @Value("${oj.judge.engine-mode:local}") String engineMode,
                       CStrategy c, CppStrategy cpp, JavaStrategy java, Python3Strategy python3) {
        this.sandbox = sandbox;
        this.comparator = comparator;
        this.localJudgeService = localJudgeService;
        this.engineMode = engineMode;
        strategies.put("c", c);
        strategies.put("cpp", cpp);
        strategies.put("java", java);
        strategies.put("python3", python3);
    }

    /**
     * 执行判题：编译 → 逐用例运行 → 比对 → 汇总。
     */
    public JudgeResult run(JudgeTask task) throws Exception {
        LanguageStrategy strategy = strategies.get(task.getLanguage());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的语言: " + task.getLanguage());
        }
        log.info("judging submission {} ({}), mode={}, {} cases", task.getSubmissionId(),
                task.getLanguage(), engineMode, task.getCases().size());

        return switch (engineMode) {
            case "mock" -> mockResult(task);
            case "local" -> localJudgeService.localJudge(task);
            default -> realRun(task, strategy);
        };
    }

    /**
     * mock 模式（M1）：全部用例判 AC，固定小耗时与内存，仅打通链路，不做真实评测。
     */
    private JudgeResult mockResult(JudgeTask task) {
        List<CaseResult> cases = new ArrayList<>();
        for (JudgeTask.CaseInput ci : task.getCases()) {
            cases.add(CaseResult.builder()
                    .index(ci.getIndex())
                    .status(SubmissionStatus.ACCEPTED)
                    .timeUsedMs(1)
                    .memoryUsedKb(1024)
                    .build());
        }
        return JudgeResult.builder()
                .status(SubmissionStatus.ACCEPTED)
                .timeUsedMs(1)
                .memoryUsedKb(1024)
                .passedCases(cases.size())
                .totalCases(task.getCases().size())
                .cases(cases)
                .build();
    }

    /**
     * real 模式（M2 实现，论文 5.4 流水线）：编译 → 逐用例沙箱执行 → 比对 → 汇总。
     */
    private JudgeResult realRun(JudgeTask task, LanguageStrategy strategy) throws Exception {
        // 1. 编译（解释型语言跳过，论文表 5-2 python3 无编译）
        if (strategy.compileCommand().isPresent()) {
            // TODO M2：写源文件 + 编译容器执行 + CE 判定（编译失败判 CE，展示编译器报错）
        }

        // 2. 逐用例执行（Fail-Fast：首个用例输出不匹配即停，论文表 4-8）
        List<CaseResult> caseResults = new ArrayList<>();
        for (JudgeTask.CaseInput caseInput : task.getCases()) {
            // TODO M2：input.txt 写入工作目录 → 沙箱运行 → 记录耗时/内存 → 比对
        }

        // 3. 汇总（论文表 4-8 判定依据）
        // TODO M2：全部通过 AC / 首个失败用例 / TLE/MLE/RE 优先级汇总
        return JudgeResult.builder()
                .status(SubmissionStatus.SYSTEM_ERROR)
                .passedCases(0)
                .totalCases(task.getCases().size())
                .errorInfo("real 判题引擎将在 M2 实现（当前请使用 mock 模式联调）")
                .build();
    }
}
