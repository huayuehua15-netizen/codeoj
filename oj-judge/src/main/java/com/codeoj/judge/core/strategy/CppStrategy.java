package com.codeoj.judge.core.strategy;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * C++ 策略（论文表 5-2：g++ -O2 -std=c++17 main.cpp -o main）。
 */
@Component
public class CppStrategy implements LanguageStrategy {

    @Override
    public String image() {
        return "codeoj/cpp:gcc13";
    }

    @Override
    public String sourceFileName() {
        return "main.cpp";
    }

    @Override
    public Optional<String[]> compileCommand() {
        return Optional.of(new String[]{"g++", "-O2", "-std=c++17", "main.cpp", "-o", "main"});
    }

    @Override
    public String[] runCommand(int memoryLimitMb) {
        return new String[]{"./main"};
    }
}
