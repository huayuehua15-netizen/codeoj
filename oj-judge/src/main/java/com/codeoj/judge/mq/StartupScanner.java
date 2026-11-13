package com.codeoj.judge.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.common.constant.MqConstants;
import com.codeoj.judge.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动扫描（论文 4.6）：服务重启时扫描滞留的"判题中"任务并重置为排队中重判，
 * 保证服务重启后未完成任务可自动恢复。
 */
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
    public void rescanStuckTasks() {
        List<Submission> stuck = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, SubmissionStatus.JUDGING.getCode()));
        for (Submission s : stuck) {
            int updated = submissionMapper.update(null, new LambdaUpdateWrapper<Submission>()
                    .eq(Submission::getId, s.getId())
                    .eq(Submission::getStatus, SubmissionStatus.JUDGING.getCode())
                    .set(Submission::getStatus, SubmissionStatus.QUEUING.getCode()));
            if (updated > 0) {
                log.info("stuck task requeued, submission={}", s.getId());
                rabbitTemplate.convertAndSend(MqConstants.JUDGE_EXCHANGE,
                        MqConstants.JUDGE_ROUTING_KEY, String.valueOf(s.getId()));
            }
        }
        log.info("startup scan done, stuck requeued={}", stuck.size());
    }
}
