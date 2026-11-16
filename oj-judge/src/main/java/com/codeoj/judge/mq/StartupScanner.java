package com.codeoj.judge.mq;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

/** QUEUING rows are durable dispatch records; lost publishes are retried periodically. */
@Component
public class StartupScanner {
    private static final Logger log = LoggerFactory.getLogger(StartupScanner.class);
    private final SubmissionMapper submissionMapper;
    private final RabbitTemplate rabbitTemplate;
    public StartupScanner(SubmissionMapper submissionMapper, RabbitTemplate rabbitTemplate) {
        this.submissionMapper = submissionMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${oj.judge.recovery-interval-ms:15000}", initialDelay = 15000)
    public void rescanStuckTasks() {
        try {
            // A new instance never steals an unexpired lease from a healthy worker.
            int recovered = submissionMapper.requeueExpiredLeases();
            if (recovered > 0) log.warn("expired judge leases recovered: {}", recovered);
            for (Long sid : submissionMapper.findDispatchable(200)) {
                // Competing scanners reserve one publish window per row.
                if (submissionMapper.reserveDispatch(sid, 120) == 0) continue;
                try {
                    MessageProperties props = new MessageProperties();
                    props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
                    props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    rabbitTemplate.send(MqConstants.JUDGE_EXCHANGE, MqConstants.JUDGE_ROUTING_KEY,
                            new Message(String.valueOf(sid).getBytes(StandardCharsets.UTF_8), props));
                } catch (Exception e) {
                    // A crash or broker rejection is retried when the reservation expires.
                    log.warn("judge dispatch deferred, submission={}", sid, e);
                }
            }
        } catch (Exception e) {
            log.error("judge recovery scan failed; next scheduled scan will retry", e);
        }
    }
}
