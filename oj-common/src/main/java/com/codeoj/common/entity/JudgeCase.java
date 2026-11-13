package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 判题明细表（论文表 4-5）：每条提交 × 每个测试用例一行，用例级判题详情。
 */
@Data
@TableName("judge_case")
public class JudgeCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交 ID */
    private Long submissionId;

    /** 用例序号（从 0 开始） */
    private Integer caseIndex;

    /** 该用例判题状态（取值同 submission.status） */
    private Integer status;

    /** 该用例耗时（毫秒） */
    private Integer timeUsedMs;

    /** 该用例内存（KB） */
    private Integer memoryUsedKb;
}
