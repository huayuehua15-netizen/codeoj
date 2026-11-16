package com.codeoj.server.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 时间格式（docs/API.md §0.1）。
 *
 * <p>契约规定时间统一 {@code yyyy-MM-dd HH:mm:ss}。但 application.yml 里的
 * {@code spring.jackson.date-format} 只作用于 {@link java.util.Date}，
 * 对 java.time.* 完全无效——所以 LocalDateTime 默认会输出 ISO 的
 * {@code 2026-11-15T20:00:00}（带 T）。前端曾为此在三处写
 * {@code .slice(0,16).replace('T',' ')} 替后端补格式，属治标。
 *
 * <p>此处统一注册全局序列化/反序列化器：输出即契约格式，
 * 前端 JSON 与后端字段可直接互认，无需再清洗字符串。
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        return builder -> builder.modulesToInstall(module);
    }
}
