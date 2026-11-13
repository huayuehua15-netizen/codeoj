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
import java.util.LinkedHashMap;
import java.util.Map;

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
                // 推送数据 status 用数字码（与 docs/API.md §7.3 前端协议一致）
                pushEventPublisher.publishFinished(event.getUserId(), toPushData(event));
            }
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("judge event push failed, submission={}", event.getSubmissionId(), e);
            channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false,
                    !msg.getMessageProperties().isRedelivered());
        }
    }

    /** JudgePushEvent → 前端推送负载：枚举转数字状态码 */
    private Map<String, Object> toPushData(JudgePushEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("submissionId", e.getSubmissionId());
        m.put("userId", e.getUserId());
        m.put("problemId", e.getProblemId());
        m.put("status", e.getStatus() == null ? null : e.getStatus().getCode());
        m.put("timeUsedMs", e.getTimeUsedMs());
        m.put("memoryUsedKb", e.getMemoryUsedKb());
        m.put("passedCases", e.getPassedCases());
        m.put("totalCases", e.getTotalCases());
        m.put("errorInfo", e.getErrorInfo());
        return m;
    }
}
