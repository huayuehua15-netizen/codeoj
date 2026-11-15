package com.codeoj.judge.event;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.common.mq.JudgePushEvent;
import com.codeoj.common.enums.SubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 判题结果事件发布（论文 4.2/5.5）：判题服务完成判题后发送完成事件，
 * 由 oj-server 消费并经 WebSocket 推送给提交者。
 */
@Component
public class ResultEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResultEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ResultEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** 发布判题结果事件（JSON 序列化） */
    public void publishFinished(JudgePushEvent event) {
        log.info("judge result event published: submission={}, status={}",
                event.getSubmissionId(), event.getStatus());
        rabbitTemplate.convertAndSend(
                MqConstants.JUDGE_EVENT_EXCHANGE,
                MqConstants.JUDGE_EVENT_ROUTING_KEY,
                event);
    }

    /** 发布"判题中"阶段事件（论文 4.5 三阶段推送：JUDGING） */
    public void publishJudging(long submissionId, long userId, long problemId) {
        rabbitTemplate.convertAndSend(
                MqConstants.JUDGE_EVENT_EXCHANGE,
                MqConstants.JUDGE_EVENT_ROUTING_KEY,
                JudgePushEvent.builder()
                        .submissionId(submissionId)
                        .userId(userId)
                        .problemId(problemId)
                        .status(SubmissionStatus.JUDGING)
                        .build());
    }

    /** 事件消息 JSON 转换器（oj-server 侧同名 bean 幂等声明） */
    @Configuration
    public static class EventMessageConverterConfig {
        @Bean
        public MessageConverter judgeEventMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    }
}
