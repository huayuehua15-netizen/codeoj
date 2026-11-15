package com.codeoj.server.config;

import com.codeoj.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑声明（论文 4.6/5.4）：
 * 判题任务经 direct 交换机进入持久化队列，消息持久化 + 手动 ACK 保证任务不丢失；
 * 持续失败的消息经 x-dead-letter-exchange 进入死信队列告警。
 * 另声明 oj-judge → oj-server 的判题结果事件队列，用于 WebSocket 推送。
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 判题任务发送模板：消息体为纯文本 submissionId（CONTRACT.md §1.1）。
     * 显式指定 SimpleMessageConverter，避免容器内的事件 JSON 转换器被自动应用到发送端。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new SimpleMessageConverter());
        return template;
    }

    /** 判题任务交换机（direct） */
    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(MqConstants.JUDGE_EXCHANGE, true, false);
    }

    /** 判题任务持久化队列，绑定死信交换机 */
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

    /** 死信交换机：兜底处理持续失败的任务 */
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

    /** 判题结果事件交换机（oj-judge 发布，oj-server 消费） */
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
