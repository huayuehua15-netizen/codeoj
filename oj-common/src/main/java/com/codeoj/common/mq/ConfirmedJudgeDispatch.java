package com.codeoj.common.mq;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.common.persistence.SubmissionLeaseOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 提交后的首发与恢复扫描使用同一套投递确认逻辑。 */
public final class ConfirmedJudgeDispatch {
    private static final Logger log = LoggerFactory.getLogger(ConfirmedJudgeDispatch.class);
    private final SubmissionLeaseOperations submissions;
    private final RabbitTemplate rabbit;

    public ConfirmedJudgeDispatch(SubmissionLeaseOperations submissions, RabbitTemplate rabbit) {
        this.submissions = submissions;
        this.rabbit = rabbit;
    }

    /** @return false 时结束本批扫描，避免 broker 故障时逐个等待确认超时。 */
    public boolean publish(long sid) {
        String token = UUID.randomUUID().toString();
        if (submissions.reserveDispatch(sid, token, 120) == 0) return true;
        try {
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            CorrelationData correlation = new CorrelationData(token);
            rabbit.send(MqConstants.JUDGE_EXCHANGE, MqConstants.JUDGE_ROUTING_KEY,
                    new Message(Long.toString(sid).getBytes(StandardCharsets.UTF_8), props), correlation);
            var confirm = correlation.getFuture().get(2, TimeUnit.SECONDS);
            if (confirm.isAck() && correlation.getReturned() == null) {
                // Workers may already have claimed the message. A stale confirmation must leave their state alone.
                submissions.confirmDispatch(sid, token);
                return true;
            }
            log.warn("judge message rejected or unrouted, submission={}", sid);
            submissions.deferDispatch(sid, token, 15);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("judge dispatch confirmation unavailable, submission={}", sid, e);
        }
        // On an ambiguous failure keep the original window; a delivered message can still be claimed.
        return false;
    }
}
