package com.codeoj.judge.mq;

import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.config.JudgeProperties;
import com.codeoj.judge.core.*;
import com.codeoj.judge.core.guard.SemaphoreGuard;
import com.codeoj.judge.core.result.ResultWriter;
import com.codeoj.judge.event.ResultEventPublisher;
import com.codeoj.judge.mapper.SubmissionMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

class JudgeConsumerReliabilityTest {
    SubmissionMapper mapper = mock(SubmissionMapper.class);
    TaskLoader loader = mock(TaskLoader.class);
    JudgeEngine engine = mock(JudgeEngine.class);
    ResultWriter writer = mock(ResultWriter.class);
    LeaderboardUpdater leaderboard = mock(LeaderboardUpdater.class);
    ResultEventPublisher events = mock(ResultEventPublisher.class);
    JudgeLeaseService lease = mock(JudgeLeaseService.class);
    Channel channel = mock(Channel.class);
    JudgeConsumer consumer;
    JudgeTask task;
    JudgeResult ac = JudgeResult.builder().status(SubmissionStatus.ACCEPTED).cases(List.of()).build();

    @BeforeEach void setUp() throws Exception {
        when(lease.leaseSeconds()).thenReturn(120);
        when(lease.keepAlive(anyLong(), anyString())).thenReturn(() -> {});
        when(mapper.markJudging(eq(7L), anyString(), eq(120))).thenReturn(1);
        Submission submission = new Submission();
        submission.setId(7L); submission.setUserId(2L); submission.setProblemId(3L); submission.setJudgeAttempts(1);
        when(mapper.selectById(7L)).thenReturn(submission);
        task = JudgeTask.builder().submissionId(7L).userId(2L).problemId(3L).cases(List.of()).build();
        when(loader.load(7L)).thenReturn(task);
        when(engine.run(any())).thenReturn(ac);
        when(writer.save(eq(7L), anyString(), any())).thenReturn(true);
        consumer = new JudgeConsumer(mapper, loader, engine, new SemaphoreGuard(new JudgeProperties()),
                writer, leaderboard, events, lease, 3);
    }
    Message message(boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(redelivered ? 2 : 1); props.setRedelivered(redelivered);
        return new Message("\"7\"".getBytes(StandardCharsets.UTF_8), props);
    }
    @Test void transientLoadFailureReleasesOwnerAndActuallyRunsRedelivery() throws Exception {
        when(loader.load(7L)).thenThrow(new IllegalStateException("temporary DB failure")).thenReturn(task);
        consumer.onMessage(message(false), channel);
        var order = inOrder(mapper, channel);
        order.verify(mapper).releaseClaim(eq(7L), anyString());
        order.verify(channel).basicNack(1, false, true);
        consumer.onMessage(message(true), channel);
        verify(loader, times(2)).load(7L);
        verify(engine).run(task);
        verify(channel).basicAck(2, false);
        assertNotNull(task.getExecutionToken());
    }
    @Test void secondFailureWritesTerminalSystemErrorAndDeadLetters() throws Exception {
        when(loader.load(7L)).thenThrow(new IllegalStateException("DB"));
        consumer.onMessage(message(true), channel);
        verify(writer).save(eq(7L), anyString(), argThat(r -> r.getStatus() == SubmissionStatus.SYSTEM_ERROR));
        verify(channel).basicNack(2, false, false);
    }
    @Test void staleOwnerNeverPublishesUncommittedResultOrChangesLeaderboard() throws Exception {
        when(writer.save(eq(7L), anyString(), any())).thenReturn(false);
        consumer.onMessage(message(false), channel);
        verify(leaderboard, never()).refresh(anyLong());
        verify(events, never()).publishFinished(any());
        verify(channel).basicAck(1, false);
    }
    @Test void realtimePushFailureDoesNotPreventJudging() throws Exception {
        doThrow(new IllegalStateException("broker")).when(events).publishJudging(anyLong(), anyLong(), anyLong());
        consumer.onMessage(message(false), channel);
        verify(engine).run(task);
        verify(channel).basicAck(1, false);
        verify(mapper, never()).releaseClaim(anyLong(), anyString());
    }
    @Test void redisFailureAfterDatabaseCommitDoesNotRetryOrDiscardTask() throws Exception {
        doThrow(new IllegalStateException("redis")).when(leaderboard).refresh(2L);
        consumer.onMessage(message(false), channel);
        verify(channel).basicAck(1, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
    @Test void repeatedCrashesHaveBoundedAttempts() throws Exception {
        mapper.selectById(7L).setJudgeAttempts(4);
        consumer.onMessage(message(false), channel);
        verify(engine, never()).run(any());
        verify(writer).save(eq(7L), anyString(), argThat(r -> r.getStatus() == SubmissionStatus.SYSTEM_ERROR));
    }
    @Test void malformedMessageIsDeadLetteredWithoutClaim() throws Exception {
        consumer.onMessage(new Message("bad".getBytes(StandardCharsets.UTF_8), message(false).getMessageProperties()), channel);
        verify(mapper, never()).markJudging(anyLong(), anyString(), anyInt());
        verify(channel).basicNack(1, false, false);
    }
    @Test void listenerDoesNotOverrideConfiguredConcurrency() throws Exception {
        RabbitListener annotation = JudgeConsumer.class.getMethod("onMessage", Message.class, Channel.class)
                .getAnnotation(RabbitListener.class);
        assertEquals("", annotation.concurrency());
    }
}
