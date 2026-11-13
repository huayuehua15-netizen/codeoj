package com.codeoj.server.mq;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.common.mq.JudgePushEvent;
import com.codeoj.server.ws.PushEventPublisher;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 判题结果事件消费者（论文 4.2/5.5）：判题服务完成判题后发送完成事件，
 * oj-server 消费事件并通过 convertAndSendToUser 定向推送至提交者。
 */
@Component
public class JudgeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(JudgeEventConsumer.class);

    private final PushEventPublisher pushEventPublisher;

    public JudgeEventConsumer(PushEventPublisher pushEventPublisher) {
        this.pushEventPublisher = pushEventPublisher;
    }

    /** 事件消息使用 JSON 转换器解析 */
    @Configuration
    public static class EventMessageConverterConfig {
        @Bean
        public MessageConverter judgeEventMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    }

    @RabbitListener(queues = MqConstants.JUDGE_EVENT_QUEUE,
            messageConverter = "judgeEventMessageConverter")
    public void onJudgeFinished(JudgePushEvent event, Message msg, Channel channel) throws IOException {
        try {
            log.info("judge event received: submission={}, status={}",
                    event.getSubmissionId(), event.getStatus());
            if (event.getStatus() != null
                    && event.getStatus() == com.codeoj.common.enums.SubmissionStatus.JUDGING) {
                pushEventPublisher.publishJudging(event.getUserId(), event.getSubmissionId());
            } else {
                pushEventPublisher.publishFinished(event.getUserId(), event);
            }
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("judge event push failed, submission={}", event.getSubmissionId(), e);
            channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false,
                    !msg.getMessageProperties().isRedelivered());
        }
    }
}
