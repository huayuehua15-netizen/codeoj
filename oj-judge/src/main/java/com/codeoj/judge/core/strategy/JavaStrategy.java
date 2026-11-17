package com.codeoj.judge.core.strategy;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Java 策略（论文表 5-2：javac -encoding UTF-8 Main.java；java -Xss8m -Xmx{mem}m Main）。
 */
@Component
public class JavaStrategy implements LanguageStrategy {

    @Override
    public String image() {
        return "codeoj/java:openjdk17";
    }

    @Override
    public String sourceFileName() {
        return "Main.java";
    }

    @Override
    public Optional<String[]> compileCommand() {
        return Optional.of(new String[]{"javac", "-encoding", "UTF-8", "Main.java"});
    }

    @Override
    public String[] runCommand(int memoryLimitMb) {
        return new String[]{"java", "-Xss8m", "-Xmx" + memoryLimitMb + "m", "Main"};
    }

    /**
     * JVM 堆外内存（元空间/线程栈/类加载器）超出 -Xmx 时会被 cgroup 误杀，
     * 容器限制需额外预留 128MB（论文 5.4 内存测量口径）。
     */
    @Override
    public int memoryOverheadMb() {
        return 128;
    }
}
