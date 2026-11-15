package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛表（论文 7.2 展望落地：竞赛模式）。
 */
@Data
@TableName("contest")
public class Contest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 比赛名称 */
    private String title;

    /** 比赛说明 */
    private String description;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建教师 */
    private Long createdBy;

    private LocalDateTime createdAt;

    /** 状态推导：0=未开始 1=进行中 2=已结束（非数据库字段） */
    public int statusOf(LocalDateTime now) {
        if (now.isBefore(startTime)) return 0;
        if (now.isAfter(endTime)) return 2;
        return 1;
    }
}
