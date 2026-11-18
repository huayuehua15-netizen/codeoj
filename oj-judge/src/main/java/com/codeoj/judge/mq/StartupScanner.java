package com.codeoj.judge.mq;

import com.codeoj.common.mq.ConfirmedJudgeDispatch;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** QUEUING rows are durable dispatch records; lost publishes are retried periodically. */
@Component
public class StartupScanner {
    private static final Logger log = LoggerFactory.getLogger(StartupScanner.class);
    private final SubmissionMapper submissionMapper;
    private final ConfirmedJudgeDispatch dispatcher;
    public StartupScanner(SubmissionMapper submissionMapper, RabbitTemplate rabbitTemplate) {
        this.submissionMapper = submissionMapper;
        this.dispatcher = new ConfirmedJudgeDispatch(submissionMapper, rabbitTemplate);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${oj.judge.recovery-interval-ms:15000}", initialDelay = 15000)
    public void rescanStuckTasks() {
        try {
            // A new instance never steals an unexpired lease from a healthy worker.
            int recovered = submissionMapper.requeueExpiredLeases();
            if (recovered > 0) log.warn("expired judge leases recovered: {}", recovered);
            for (Long sid : submissionMapper.findDispatchable(200)) {
                if (!dispatcher.publish(sid)) break;
            }
        } catch (Exception e) {
            log.error("judge recovery scan failed; next scheduled scan will retry", e);
        }
    }
}
