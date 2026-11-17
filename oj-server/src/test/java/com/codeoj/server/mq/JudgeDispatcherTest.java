package com.codeoj.server.mq;
import com.codeoj.server.mapper.SubmissionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.nio.charset.StandardCharsets;

class JudgeDispatcherTest {
    @Test void publishesRawPersistentTextWithoutGlobalJsonConverter() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        SubmissionMapper submissions = mock(SubmissionMapper.class);
        when(submissions.reserveDispatch(7L,120)).thenReturn(1);
        new JudgeDispatcher(rabbit,submissions).publish(7);
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(anyString(),anyString(),message.capture(),any(CorrelationData.class));
        assertEquals("7",new String(message.getValue().getBody(), StandardCharsets.UTF_8));
        assertEquals(MessageDeliveryMode.PERSISTENT,message.getValue().getMessageProperties().getDeliveryMode());
    }
    @Test void brokerFailureLeavesDurableRecordForScheduledRecovery() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        SubmissionMapper submissions = mock(SubmissionMapper.class);
        when(submissions.reserveDispatch(7L,120)).thenReturn(1);
        doThrow(new IllegalStateException("broker")).when(rabbit).send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
        assertDoesNotThrow(() -> new JudgeDispatcher(rabbit,submissions).publish(7));
        verify(submissions).reserveDispatch(7L,120);
        verifyNoMoreInteractions(submissions); // never delete/cancel the committed QUEUING row
    }
    @Test void dispatchUsesAnIndependentTransactionAfterSubmissionCommit() throws Exception {
        assertEquals(Propagation.REQUIRES_NEW, JudgeDispatcher.class.getMethod("publish",long.class)
                .getAnnotation(Transactional.class).propagation());
    }
}
