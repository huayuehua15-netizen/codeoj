package com.codeoj.common.constant;

/**
 * 判题消息拓扑（论文 4.6 / 5.4）。
 * direct 交换机 + 路由键 judge.submit + 持久化队列，消息体仅携带 submissionId。
 */
public final class MqConstants {

    private MqConstants() {
    }

    /** 判题任务 direct 交换机 */
    public static final String JUDGE_EXCHANGE = "oj.judge.exchange";
    /** 判题任务路由键 */
    public static final String JUDGE_ROUTING_KEY = "judge.submit";
    /** 判题任务持久化队列 */
    public static final String JUDGE_QUEUE = "oj.judge.queue";

    /** 死信交换机：兜底处理持续失败的任务 */
    public static final String JUDGE_DLX = "oj.judge.dlx";
    /** 死信队列 */
    public static final String JUDGE_DLQ = "oj.judge.dlq";
    /** 死信路由键 */
    public static final String JUDGE_DLX_ROUTING_KEY = "judge.dead";

    /** 判题结果事件：oj-judge → oj-server，经 WebSocket 推送给提交者 */
    public static final String JUDGE_EVENT_EXCHANGE = "oj.event.exchange";
    public static final String JUDGE_EVENT_ROUTING_KEY = "event.judge.finished";
    /** oj-server 接收判题结果事件的队列 */
    public static final String JUDGE_EVENT_QUEUE = "oj.event.judge.queue";
}
