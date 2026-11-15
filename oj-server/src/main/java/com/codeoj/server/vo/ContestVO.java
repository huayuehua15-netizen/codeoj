package com.codeoj.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛列表项/详情基础信息。
 */
@Data
@AllArgsConstructor
public class ContestVO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0=未开始 1=进行中 2=已结束 */
    private Integer status;
    private int problemCount;
}
