package com.codeoj.judge.event;

import com.codeoj.common.mq.JudgePushEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResultEventPublisherTest {
    @Test void eventBrokerFailureDoesNotBreakJudging() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new AmqpException("temporary event outage"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        ResultEventPublisher publisher = new ResultEventPublisher(rabbit);
        assertDoesNotThrow(() -> publisher.publishFinished(JudgePushEvent.builder().submissionId(1L).build()));
        assertDoesNotThrow(() -> publisher.publishJudging(1L, 2L, 3L));
    }
}
