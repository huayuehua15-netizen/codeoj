package com.codeoj.judge.core.strategy;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * C 语言策略（论文表 5-2：gcc -O2 -std=c17 main.c -o main）。
 */
@Component
public class CStrategy implements LanguageStrategy {

    @Override
    public String image() {
        return "codeoj/c:gcc13";
    }

    @Override
    public String sourceFileName() {
        return "main.c";
    }

    @Override
    public Optional<String[]> compileCommand() {
        return Optional.of(new String[]{"gcc", "-O2", "-std=c17", "main.c", "-o", "main"});
    }

    @Override
    public String[] runCommand(int memoryLimitMb) {
        return new String[]{"./main"};
    }
}
