package com.codeoj.server.mq;

import com.codeoj.common.constant.MqConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 判题消息生产者（论文 4.6）：提交接口只负责入队即返回，判题由消费者异步完成。
 * 消息体只携带 submissionId；发布者确认保证可靠投递。
 */
@Component
public class JudgeDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JudgeDispatcher.class);

    private final RabbitTemplate rabbitTemplate;

    public JudgeDispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** 发布判题任务：仅提交 ID（论文 4.6） */
    public void publish(long submissionId) {
        log.info("judge task published, submissionId={}", submissionId);
        rabbitTemplate.convertAndSend(
                MqConstants.JUDGE_EXCHANGE,
                MqConstants.JUDGE_ROUTING_KEY,
                String.valueOf(submissionId),
                new CorrelationData(String.valueOf(submissionId)));
    }
}
