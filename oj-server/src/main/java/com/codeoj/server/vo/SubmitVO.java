package com.codeoj.server.vo;

import com.codeoj.common.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提交响应（论文 5.3：SubmitVO.of(sid, SubmissionStatus.QUEUING)）。
 */
@Data
@AllArgsConstructor
public class SubmitVO {

    private Long submissionId;
    private SubmissionStatus status;

    public static SubmitVO of(Long submissionId, SubmissionStatus status) {
        return new SubmitVO(submissionId, status);
    }
}
