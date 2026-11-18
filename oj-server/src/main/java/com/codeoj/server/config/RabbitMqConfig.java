package com.codeoj.server.config;

import com.codeoj.common.constant.MqConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RabbitMQ 拓扑声明（论文 4.6/5.4）：
 * 判题任务经 direct 交换机进入持久化队列，消息持久化 + 手动 ACK 保证任务不丢失；
 * 持续失败的消息经 x-dead-letter-exchange 进入死信队列告警。
 * 另声明 oj-judge → oj-server 的判题结果事件队列，用于 WebSocket 推送。
 */
@Configuration
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);

    @Bean
    public ThreadPoolTaskExecutor judgeDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("judge-dispatch-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(128);
        return executor;
    }

    /**
     * 判题任务发送模板：消息体为纯文本 submissionId（CONTRACT.md §1.1）。
     * 显式指定 SimpleMessageConverter，避免容器内的事件 JSON 转换器被自动应用到发送端。
     *
     * <p>注意：自动配置里的 mandatory/publisher-confirm 不会作用于手工 new 出来的
     * RabbitTemplate——必须在这里重新设置，否则 yml 中的 publisher-confirm 配置形同虚设。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new SimpleMessageConverter());
        // 消息无路由时回调（配合 publisher-returns），否则会静默丢弃
        template.setMandatory(true);
        template.setReturnsCallback(returned ->
                log.error("judge message returned: exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        // 投递确认：失败时留痕，便于配合 StartupScanner 兜底重投
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("judge message nack by broker, correlationId={}, cause={}",
                        correlationData == null ? null : correlationData.getId(), cause);
            }
        });
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
