package com.codeoj.server.mq;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.server.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

/** Best-effort fast dispatch. QUEUING database rows are retried until a worker claims them. */
@Component
public class JudgeDispatcher {
    private static final Logger log = LoggerFactory.getLogger(JudgeDispatcher.class);
    private final RabbitTemplate rabbitTemplate;
    private final SubmissionMapper submissions;
    public JudgeDispatcher(RabbitTemplate rabbitTemplate, SubmissionMapper submissions) {
        this.rabbitTemplate = rabbitTemplate;
        this.submissions = submissions;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(long submissionId) {
        try {
            // Reservation avoids immediate duplicate publishing by the recovery scanner.
            if (submissions.reserveDispatch(submissionId, 120) == 0) return;
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            rabbitTemplate.send(MqConstants.JUDGE_EXCHANGE, MqConstants.JUDGE_ROUTING_KEY,
                    new Message(String.valueOf(submissionId).getBytes(StandardCharsets.UTF_8), properties),
                    new CorrelationData(String.valueOf(submissionId)));
        } catch (Exception e) {
            // A committed submission still succeeds; the durable row is retried after its dispatch window.
            log.warn("judge dispatch deferred, submission={}", submissionId, e);
        }
    }
}
