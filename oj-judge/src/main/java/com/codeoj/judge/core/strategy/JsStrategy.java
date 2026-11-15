package com.codeoj.judge.core.strategy;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JavaScript（Node.js）策略（论文表 5-2 扩展：前端方向题目）。
 * real 模式运行于 codeoj/node:20 镜像；local 模式由静态判定覆盖。
 */
@Component
public class JsStrategy implements LanguageStrategy {

    @Override
    public String image() {
        return "codeoj/node:20";
    }

    @Override
    public String sourceFileName() {
        return "main.js";
    }

    @Override
    public Optional<String[]> compileCommand() {
        // 解释型语言：无编译
        return Optional.empty();
    }

    @Override
    public String[] runCommand(int memoryLimitMb) {
        return new String[]{"node", "main.js"};
    }
}
