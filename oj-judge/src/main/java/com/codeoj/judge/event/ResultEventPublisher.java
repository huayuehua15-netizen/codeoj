package com.codeoj.judge.event;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.mq.JudgePushEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** Live events are best effort; the persisted submission remains authoritative. */
@Component
public class ResultEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ResultEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ResultEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishFinished(JudgePushEvent event) { publish(event); }

    public void publishJudging(long submissionId, long userId, long problemId) {
        publishJudging(JudgePushEvent.builder().submissionId(submissionId).userId(userId)
                .problemId(problemId).status(SubmissionStatus.JUDGING).build());
    }

    public void publishJudging(JudgePushEvent event) { publish(event); }

    private void publish(JudgePushEvent event) {
        try {
            rabbitTemplate.convertAndSend(MqConstants.JUDGE_EVENT_EXCHANGE,
                    MqConstants.JUDGE_EVENT_ROUTING_KEY, event);
        } catch (Exception e) {
            log.warn("judge live event unavailable, submission={}, status={}; use result polling",
                    event.getSubmissionId(), event.getStatus(), e);
        }
    }

    @Configuration
    public static class EventMessageConverterConfig {
        @Bean
        public MessageConverter judgeEventMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    }
}
