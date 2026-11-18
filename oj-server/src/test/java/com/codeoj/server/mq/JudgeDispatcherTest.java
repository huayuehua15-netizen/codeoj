package com.codeoj.server.mq;
import com.codeoj.server.mapper.SubmissionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
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
        when(submissions.reserveDispatch(eq(7L),anyString(),eq(120))).thenReturn(1);
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
        new JudgeDispatcher(rabbit,submissions).publish(7);
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(anyString(),anyString(),message.capture(),any(CorrelationData.class));
        assertEquals("7",new String(message.getValue().getBody(), StandardCharsets.UTF_8));
        assertEquals(MessageDeliveryMode.PERSISTENT,message.getValue().getMessageProperties().getDeliveryMode());
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(submissions).reserveDispatch(eq(7L), token.capture(), eq(120));
        verify(submissions).confirmDispatch(7L, token.getValue());
    }
    @Test void brokerFailureLeavesDurableRecordForScheduledRecovery() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        SubmissionMapper submissions = mock(SubmissionMapper.class);
        when(submissions.reserveDispatch(eq(7L),anyString(),eq(120))).thenReturn(1);
        doThrow(new IllegalStateException("broker")).when(rabbit).send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
        assertDoesNotThrow(() -> new JudgeDispatcher(rabbit,submissions).publish(7));
        verify(submissions).reserveDispatch(eq(7L),anyString(),eq(120));
        verifyNoMoreInteractions(submissions); // never delete/cancel the committed QUEUING row
    }
    @Test void unroutedAckDoesNotMarkTheTaskConfirmed() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        SubmissionMapper submissions = mock(SubmissionMapper.class);
        when(submissions.reserveDispatch(eq(7L),anyString(),eq(120))).thenReturn(1);
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            correlation.setReturned(new ReturnedMessage(call.getArgument(2), 312, "NO_ROUTE", "exchange", "key"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
        new JudgeDispatcher(rabbit,submissions).publish(7);
        verify(submissions, never()).confirmDispatch(anyLong(), anyString());
        verify(submissions).deferDispatch(eq(7L), anyString(), eq(15));
    }
    @Test void dispatchUsesAnIndependentTransactionAfterSubmissionCommit() throws Exception {
        assertEquals(Propagation.REQUIRES_NEW, JudgeDispatcher.class.getMethod("publish",long.class)
                .getAnnotation(Transactional.class).propagation());
    }
}
