package com.codeoj.judge.mq;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StartupScannerTest {
    @Test void unavailableBrokerStopsOnlyTheCurrentScanAndNextScanStillPublishes() {
        SubmissionMapper mapper = mock(SubmissionMapper.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        when(mapper.findDispatchable(200)).thenReturn(List.of(1L, 2L), List.of(2L));
        when(mapper.reserveDispatch(anyLong(), anyString(), eq(120))).thenReturn(1);
        doThrow(new IllegalStateException("broker")).doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        StartupScanner scanner = new StartupScanner(mapper, rabbit);
        scanner.rescanStuckTasks();
        scanner.rescanStuckTasks();
        verify(rabbit, times(2)).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        verify(mapper, never()).confirmDispatch(eq(1L), anyString());
        verify(mapper).confirmDispatch(eq(2L), anyString());
        verify(mapper, times(2)).requeueExpiredLeases();
    }
    @Test void scannerThatLosesReservationDoesNotPublishDuplicate() {
        SubmissionMapper mapper = mock(SubmissionMapper.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        when(mapper.findDispatchable(200)).thenReturn(List.of(1L));
        when(mapper.reserveDispatch(eq(1L), anyString(), eq(120))).thenReturn(0);
        new StartupScanner(mapper, rabbit).rescanStuckTasks();
        verifyNoInteractions(rabbit);
    }
}
