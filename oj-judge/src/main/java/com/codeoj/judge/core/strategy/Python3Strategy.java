package com.codeoj.judge.core.strategy;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Python3 策略（论文表 5-2：无编译，python3 main.py）。
 */
@Component
public class Python3Strategy implements LanguageStrategy {

    @Override
    public String image() {
        return "codeoj/python:3.12";
    }

    @Override
    public String sourceFileName() {
        return "main.py";
    }

    @Override
    public Optional<String[]> compileCommand() {
        return Optional.empty();
    }

    @Override
    public String[] runCommand(int memoryLimitMb) {
        return new String[]{"python3", "main.py"};
    }
}
