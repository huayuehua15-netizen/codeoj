package com.codeoj.judge.mq;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.mq.JudgePushEvent;
import com.codeoj.judge.core.*;
import com.codeoj.judge.core.guard.SemaphoreGuard;
import com.codeoj.judge.core.result.ResultWriter;
import com.codeoj.judge.event.ResultEventPublisher;
import com.codeoj.judge.mapper.SubmissionMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class JudgeConsumer {
    private static final Logger log = LoggerFactory.getLogger(JudgeConsumer.class);
    private final SubmissionMapper submissionMapper;
    private final TaskLoader taskLoader;
    private final JudgeEngine engine;
    private final SemaphoreGuard semaphoreGuard;
    private final ResultWriter resultWriter;
    private final LeaderboardUpdater leaderboardUpdater;
    private final ResultEventPublisher pushEventPublisher;
    private final JudgeLeaseService leases;
    private final int maxAttempts;

    public JudgeConsumer(SubmissionMapper submissionMapper, TaskLoader taskLoader, JudgeEngine engine,
                         SemaphoreGuard semaphoreGuard, ResultWriter resultWriter,
                         LeaderboardUpdater leaderboardUpdater, ResultEventPublisher pushEventPublisher,
                         JudgeLeaseService leases, @Value("${oj.judge.max-attempts:3}") int maxAttempts) {
        this.submissionMapper = submissionMapper;
        this.taskLoader = taskLoader;
        this.engine = engine;
        this.semaphoreGuard = semaphoreGuard;
        this.resultWriter = resultWriter;
        this.leaderboardUpdater = leaderboardUpdater;
        this.pushEventPublisher = pushEventPublisher;
        this.leases = leases;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    // Concurrency comes from spring.rabbitmq.listener.simple.concurrency / JUDGE_CONCURRENCY.
    @RabbitListener(queues = MqConstants.JUDGE_QUEUE, ackMode = "MANUAL")
    public void onMessage(Message msg, Channel channel) throws IOException {
        long tag = msg.getMessageProperties().getDeliveryTag();
        long sid;
        try { sid = parseSubmissionId(msg.getBody()); }
        catch (NumberFormatException e) {
            log.warn("invalid judge message, dead-lettering");
            channel.basicNack(tag, false, false);
            return;
        }
        String token = UUID.randomUUID().toString();
        boolean claimed = false;
        Submission submission = null;
        try {
            claimed = submissionMapper.markJudging(sid, token, leases.leaseSeconds()) > 0;
            if (!claimed) {
                // Live owner or terminal result exists; expired owners are recovered by the DB scan.
                channel.basicAck(tag, false);
                return;
            }
            try (AutoCloseable heartbeat = leases.keepAlive(sid, token)) {
                submission = submissionMapper.selectById(sid);
                JudgeResult result;
                if (submission != null && submission.getJudgeAttempts() != null
                        && submission.getJudgeAttempts() > maxAttempts) {
                    result = systemError("评测服务多次中断，重试次数已用尽");
                } else {
                    JudgeTask task = taskLoader.load(sid);
                    task.setExecutionToken(token);
                    bestEffort(() -> pushEventPublisher.publishJudging(sid, task.getUserId(), task.getProblemId()));
                    result = semaphoreGuard.run(() -> {
                        try { return engine.run(task); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    });
                }
                if (resultWriter.save(sid, token, result)) publishCommitted(submission, sid, result);
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("judge attempt failed, submission={}", sid, e);
            try {
                if (claimed) {
                    if (msg.getMessageProperties().isRedelivered()) {
                        JudgeResult failed = systemError("评测服务异常，重试后仍未完成");
                        if (resultWriter.save(sid, token, failed)) publishCommitted(submission, sid, failed);
                    } else {
                        // Release this token before redelivery; do not ACK a failed attempt as a duplicate.
                        submissionMapper.releaseClaim(sid, token);
                    }
                }
            } catch (Exception recoveryFailure) {
                log.error("judge recovery deferred to lease expiry, submission={}", sid, recoveryFailure);
            }
            channel.basicNack(tag, false, !msg.getMessageProperties().isRedelivered());
        }
    }

    private void publishCommitted(Submission submission, long sid, JudgeResult result) {
        if (submission == null) return;
        if (result.isAccepted()) bestEffort(() -> leaderboardUpdater.refresh(submission.getUserId()));
        bestEffort(() -> pushEventPublisher.publishFinished(JudgePushEvent.builder()
                .submissionId(sid).userId(submission.getUserId()).problemId(submission.getProblemId())
                .status(result.getStatus()).timeUsedMs(result.getTimeUsedMs()).memoryUsedKb(result.getMemoryUsedKb())
                .passedCases(result.getPassedCases()).totalCases(result.getTotalCases()).errorInfo(result.getErrorInfo())
                .build()));
    }
    private void bestEffort(Runnable action) {
        try { action.run(); }
        catch (Exception e) { log.warn("judge side effect deferred; DB remains authoritative", e); }
    }
    private JudgeResult systemError(String message) {
        return JudgeResult.builder().status(SubmissionStatus.SYSTEM_ERROR).passedCases(0).totalCases(0)
                .errorInfo(message).cases(List.of()).build();
    }
    static long parseSubmissionId(byte[] body) {
        String value = new String(body, StandardCharsets.UTF_8).trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
            value = value.substring(1, value.length() - 1);
        long id = Long.parseLong(value);
        if (id <= 0) throw new NumberFormatException("submission id must be positive");
        return id;
    }
}
