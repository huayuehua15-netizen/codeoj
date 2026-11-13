package com.codeoj.server.vo;

import com.codeoj.common.entity.Submission;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交详情（论文 4.5：判题详情（用例级）；他人代码仅本人与管理员可查）。
 */
@Data
public class SubmissionDetailVO {

    private Long id;
    private Long userId;
    private Long problemId;
    private String problemTitle;
    private String language;
    /** 代码仅本人与管理员可见 */
    private String code;
    private Integer status;
    private String statusDesc;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String errorInfo;
    private LocalDateTime judgedAt;
    private LocalDateTime createdAt;

    /** 用例级判题明细 */
    private List<JudgeCaseVO> cases;
}
