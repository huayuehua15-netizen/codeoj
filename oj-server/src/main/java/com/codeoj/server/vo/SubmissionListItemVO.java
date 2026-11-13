package com.codeoj.server.vo;

import com.codeoj.common.entity.Submission;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交记录列表项。
 */
@Data
public class SubmissionListItemVO {

    private Long id;
    private Long problemId;
    private String problemTitle;
    private String language;
    private Integer status;
    private String statusDesc;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private LocalDateTime createdAt;

    public static SubmissionListItemVO of(Submission s, String problemTitle) {
        SubmissionListItemVO vo = new SubmissionListItemVO();
        vo.setId(s.getId());
        vo.setProblemId(s.getProblemId());
        vo.setProblemTitle(problemTitle);
        vo.setLanguage(s.getLanguage());
        vo.setStatus(s.getStatus());
        vo.setStatusDesc(com.codeoj.common.enums.SubmissionStatus.of(s.getStatus()).getDesc());
        vo.setTimeUsedMs(s.getTimeUsedMs());
        vo.setMemoryUsedKb(s.getMemoryUsedKb());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }
}
