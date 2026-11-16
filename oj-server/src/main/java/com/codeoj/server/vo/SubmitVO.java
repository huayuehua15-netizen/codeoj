package com.codeoj.server.vo;

import com.codeoj.common.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提交响应（论文 5.3）。
 *
 * <p>status 为<b>数字状态码</b>（0=排队中），与 docs/API.md §4.1 的
 * {@code { "submissionId": 100, "status": 0 }} 一致。
 * 早期版本直接把枚举放进来，JSON 会序列化成字符串 "QUEUING"，
 * 与 WS 推送（JudgeEventConsumer 已转数字）口径不一致，前端拿到的类型也对不上。
 */
@Data
@AllArgsConstructor
public class SubmitVO {

    private Long submissionId;
    private Integer status;

    public static SubmitVO of(Long submissionId, SubmissionStatus status) {
        return new SubmitVO(submissionId, status == null ? null : status.getCode());
    }
}
