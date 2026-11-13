package com.codeoj.judge.config;

import com.codeoj.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑声明（与 oj-server 幂等一致，谁先启动谁生效）。
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(MqConstants.JUDGE_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeQueue() {
        return QueueBuilder.durable(MqConstants.JUDGE_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.JUDGE_DLX)
                .withArgument("x-dead-letter-routing-key", MqConstants.JUDGE_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding judgeBinding() {
        return BindingBuilder.bind(judgeQueue()).to(judgeExchange())
                .with(MqConstants.JUDGE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange judgeDlx() {
        return new DirectExchange(MqConstants.JUDGE_DLX, true, false);
    }

    @Bean
    public Queue judgeDlq() {
        return QueueBuilder.durable(MqConstants.JUDGE_DLQ).build();
    }

    @Bean
    public Binding judgeDlqBinding() {
        return BindingBuilder.bind(judgeDlq()).to(judgeDlx())
                .with(MqConstants.JUDGE_DLX_ROUTING_KEY);
    }

    @Bean
    public DirectExchange judgeEventExchange() {
        return new DirectExchange(MqConstants.JUDGE_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeEventQueue() {
        return QueueBuilder.durable(MqConstants.JUDGE_EVENT_QUEUE).build();
    }

    @Bean
    public Binding judgeEventBinding() {
        return BindingBuilder.bind(judgeEventQueue()).to(judgeEventExchange())
                .with(MqConstants.JUDGE_EVENT_ROUTING_KEY);
    }
}
