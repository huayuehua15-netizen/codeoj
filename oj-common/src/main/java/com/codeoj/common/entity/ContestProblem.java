package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 竞赛题目关联表：比赛题目集（按 sortOrder 对应字母题号 A/B/C…）。
 */
@Data
@TableName("contest_problem")
public class ContestProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contestId;

    private Long problemId;

    /** 题目顺序（0=A, 1=B, …） */
    private Integer sortOrder;
}
