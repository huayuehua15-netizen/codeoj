package com.codeoj.server.mq;

import com.codeoj.common.mq.ConfirmedJudgeDispatch;
import com.codeoj.server.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The independent fast path leaves queued rows recoverable if its executor is saturated. */
@Component
public class JudgeDispatcher {
    private static final Logger log = LoggerFactory.getLogger(JudgeDispatcher.class);
    private final ConfirmedJudgeDispatch dispatcher;
    public JudgeDispatcher(RabbitTemplate rabbitTemplate, SubmissionMapper submissions) {
        this.dispatcher = new ConfirmedJudgeDispatch(submissions, rabbitTemplate);
    }
    @Async("judgeDispatchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(long submissionId) {
        try {
            dispatcher.publish(submissionId);
        } catch (Exception e) {
            // A committed submission still succeeds; the durable row is retried after its dispatch window.
            log.warn("judge dispatch deferred, submission={}", submissionId, e);
        }
    }
}
