package com.codeoj.server.ws;

import com.codeoj.common.mq.JudgePushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 判题状态推送（论文 4.5/5.5）：判题状态经 /user/queue/judge 用户级目标定向推送。
 * 消息类型：JUDGE_QUEUED（入队）、JUDGING（判题中）、JUDGE_FINISHED（判题完成）。
 */
@Component
public class PushEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PushEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public PushEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishQueued(long userId, long submissionId) {
        send(userId, JudgePushMessage.TYPE_QUEUED, new QueuedPayload(submissionId));
    }

    public void publishJudging(long userId, long submissionId) {
        send(userId, JudgePushMessage.TYPE_JUDGING, new QueuedPayload(submissionId));
    }

    public void publishFinished(long userId, Object resultVO) {
        send(userId, JudgePushMessage.TYPE_FINISHED, resultVO);
    }

    /** 定向推送至提交者（论文 5.5：convertAndSendToUser） */
    private void send(long userId, String type, Object data) {
        log.debug("push {} to user {}", type, userId);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId),
                "/queue/judge", JudgePushMessage.of(type, data));
    }

    record QueuedPayload(long submissionId) {
    }
}
