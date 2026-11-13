package com.codeoj.judge.core.strategy;

import java.util.Optional;

/**
 * 语言策略（论文表 5-2）：每种语言对应一个最小化镜像与编译/运行方案。
 */
public interface LanguageStrategy {

    /** 判题沙箱镜像 */
    String image();

    /** 源文件名（工作目录内） */
    String sourceFileName();

    /** 编译命令；解释型语言返回 empty（论文表 5-2 python3 无编译） */
    Optional<String[]> compileCommand();

    /** 运行命令（java 需 -Xss8m -Xmx{mem}m，论文表 5-2） */
    String[] runCommand(int memoryLimitMb);

    /** 编译阶段时间上限（毫秒） */
    default long compileTimeoutMs() {
        return 10000;
    }
}
