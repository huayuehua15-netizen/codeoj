package com.codeoj.judge.mq;

import com.codeoj.common.constant.MqConstants;
import com.codeoj.common.mq.JudgePushEvent;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.core.JudgeResult;
import com.codeoj.judge.core.JudgeTask;
import com.codeoj.judge.core.TaskLoader;
import com.codeoj.judge.core.guard.SemaphoreGuard;
import com.codeoj.judge.core.result.ResultWriter;
import com.codeoj.judge.event.ResultEventPublisher;
import com.codeoj.judge.mapper.SubmissionMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 判题任务消费者（论文 5.4）：手动确认模式领取任务，先以条件更新将提交置为判题中
 * （影响行数为零说明重复投递，直接确认跳过），随后拉取代码与用例，按语言选择编译与运行方案，
 * 逐用例创建沙箱容器执行，比对输出，最后回写结果、更新排行榜并发送推送事件。
 * prefetch=1 与并发度 4 构成实例内第一层并发控制。
 */
@Component
public class JudgeConsumer {

    private static final Logger log = LoggerFactory.getLogger(JudgeConsumer.class);

    private final SubmissionMapper submissionMapper;
    private final TaskLoader taskLoader;
    private final com.codeoj.judge.core.JudgeEngine engine;
    private final SemaphoreGuard semaphoreGuard;
    private final ResultWriter resultWriter;
    private final LeaderboardUpdater leaderboardUpdater;
    private final ResultEventPublisher pushEventPublisher;

    public JudgeConsumer(SubmissionMapper submissionMapper,
                         TaskLoader taskLoader,
                         com.codeoj.judge.core.JudgeEngine engine,
                         SemaphoreGuard semaphoreGuard,
                         ResultWriter resultWriter,
                         LeaderboardUpdater leaderboardUpdater,
                         ResultEventPublisher pushEventPublisher) {
        this.submissionMapper = submissionMapper;
        this.taskLoader = taskLoader;
        this.engine = engine;
        this.semaphoreGuard = semaphoreGuard;
        this.resultWriter = resultWriter;
        this.leaderboardUpdater = leaderboardUpdater;
        this.pushEventPublisher = pushEventPublisher;
    }

    @RabbitListener(queues = MqConstants.JUDGE_QUEUE, ackMode = "MANUAL", concurrency = "4")
    public void onMessage(Message msg, Channel channel) throws IOException {
        long sid = Long.parseLong(new String(msg.getBody()));
        try {
            // 幂等：WHERE status=0，影响行数为零说明重复投递，直接确认跳过（论文 5.4）
            if (submissionMapper.markJudging(sid) == 0) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            JudgeTask task = taskLoader.load(sid);                   // 代码 + 用例 + 限制
            JudgeResult result = semaphoreGuard.run(() -> {
                try {
                    return engine.run(task);                          // 信号量内执行
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            boolean written = resultWriter.save(sid, result);         // 条件更新 + 明细 + 统计
            if (written && result.isAccepted()) {
                leaderboardUpdater.incr(task.getUserId());            // AC 时更新排行（论文 5.6）
            }
            pushEventPublisher.publishFinished(JudgePushEvent.builder()
                    .submissionId(sid)
                    .userId(task.getUserId())
                    .problemId(task.getProblemId())
                    .status(result.getStatus())
                    .timeUsedMs(result.getTimeUsedMs())
                    .memoryUsedKb(result.getMemoryUsedKb())
                    .passedCases(result.getPassedCases())
                    .totalCases(result.getTotalCases())
                    .errorInfo(result.getErrorInfo())
                    .build());
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("judge failed, submission={}", sid, e);
            // 重投或进死信（论文 4.6：持续失败的消息进入死信队列告警）
            channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false,
                    !msg.getMessageProperties().isRedelivered());
        }
    }
}
