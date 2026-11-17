package com.codeoj.judge.mq;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StartupScannerTest {
    @Test void publishingFailureIsRetriedAndDoesNotPreventOtherPendingTasks() {
        SubmissionMapper mapper = mock(SubmissionMapper.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        when(mapper.findDispatchable(200)).thenReturn(List.of(1L, 2L));
        when(mapper.reserveDispatch(anyLong(), eq(120))).thenReturn(1);
        doThrow(new IllegalStateException("broker")).doNothing().doNothing().doNothing()
                .when(rabbit).send(anyString(), anyString(), any(Message.class));
        StartupScanner scanner = new StartupScanner(mapper, rabbit);
        scanner.rescanStuckTasks();
        scanner.rescanStuckTasks(); // DB makes the row eligible again after the reserved window
        verify(rabbit, times(4)).send(anyString(), anyString(), any(Message.class));
        verify(mapper, times(2)).requeueExpiredLeases();
    }
    @Test void scannerThatLosesReservationDoesNotPublishDuplicate() {
        SubmissionMapper mapper = mock(SubmissionMapper.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        when(mapper.findDispatchable(200)).thenReturn(List.of(1L));
        when(mapper.reserveDispatch(1L, 120)).thenReturn(0);
        new StartupScanner(mapper, rabbit).rescanStuckTasks();
        verifyNoInteractions(rabbit);
    }
}
