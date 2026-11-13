package com.codeoj.judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * oj-judge 判题服务启动类：消费队列消息并通过 docker-java 调度沙箱容器（论文 4.2）。
 */
@SpringBootApplication
public class OjJudgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjJudgeApplication.class, args);
    }
}
