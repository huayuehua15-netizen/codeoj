package com.codeoj.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * oj-server 业务服务启动类：提供全部业务 REST 接口，并作为判题消息生产者（论文 4.2）。
 */
@SpringBootApplication
public class OjServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjServerApplication.class, args);
    }
}
