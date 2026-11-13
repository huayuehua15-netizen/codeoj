package com.codeoj.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 推送消息结构（论文 4.5）：{type, data}。
 * 消息类型：JUDGE_QUEUED（入队）、JUDGING（判题中）、JUDGE_FINISHED（判题完成），
 * 经 /user/queue/judge 用户级目标定向推送。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgePushMessage {

    public static final String TYPE_QUEUED = "JUDGE_QUEUED";
    public static final String TYPE_JUDGING = "JUDGING";
    public static final String TYPE_FINISHED = "JUDGE_FINISHED";

    private String type;
    private Object data;

    public static JudgePushMessage of(String type, Object data) {
        return new JudgePushMessage(type, data);
    }
}
