package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交记录表（论文表 4-4）。status 为判题状态机（论文表 4-8）。
 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交用户 */
    private Long userId;

    /** 题目 */
    private Long problemId;

    /** c/cpp/java/python3/js */
    private String language;

    /** 提交代码（≤64KB） */
    private String code;

    /** 0排队 1判题中 2AC 3WA 4TLE 5MLE 6RE 7CE 8SE */
    private Integer status;

    /** 最大用例耗时（毫秒） */
    private Integer timeUsedMs;

    /** 最大用例内存（KB） */
    private Integer memoryUsedKb;

    /** 编译/运行错误摘要 */
    private String errorInfo;

    /** 判题完成时间 */
    private LocalDateTime judgedAt;

    /** The current execution owns this token only until its database lease expires. */
    private String judgeToken;
    private LocalDateTime judgeLeaseUntil;
    private Integer judgeAttempts;

    /** QUEUING rows are the durable dispatch record, retried after this time. */
    private LocalDateTime nextDispatchAt;

    /** 旧提交没有快照，仍可按原方式读取。 */
    private String judgeSnapshotKey;
    private Integer totalCases;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Integer difficulty;
}
