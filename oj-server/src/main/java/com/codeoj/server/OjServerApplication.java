package com.codeoj.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * oj-server 业务服务启动类：提供全部业务 REST 接口，并作为判题消息生产者（论文 4.2）。
 * 定时任务：排行榜对账（论文 5.6）。
 */
@SpringBootApplication
@EnableScheduling
public class OjServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjServerApplication.class, args);
    }
}
