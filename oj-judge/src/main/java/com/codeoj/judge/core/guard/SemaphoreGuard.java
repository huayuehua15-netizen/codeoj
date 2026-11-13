package com.codeoj.judge.core.guard;

import com.codeoj.judge.config.JudgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * 实例内信号量限流（论文 4.6/5.4）：以信号量限制并行运行的容器数（默认 4），
 * 防止 Docker 引擎过载，构成实例层并发控制。
 */
@Component
public class SemaphoreGuard {

    private static final Logger log = LoggerFactory.getLogger(SemaphoreGuard.class);

    private final Semaphore semaphore;

    public SemaphoreGuard(JudgeProperties props) {
        this.semaphore = new Semaphore(props.getMaxParallelContainers());
    }

    /** 在信号量保护内执行判题：并发满时阻塞排队，由 MQ prefetch 背压兜底 */
    public <T> T run(Supplier<T> task) throws Exception {
        semaphore.acquire();
        try {
            return task.get();
        } finally {
            semaphore.release();
        }
    }
}
