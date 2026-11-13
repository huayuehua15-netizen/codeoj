package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程题目关联表（论文 4.4）：课程与题目多对多。
 */
@Data
@TableName("course_problem")
public class CourseProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程 ID */
    private Long courseId;

    /** 题目 ID */
    private Long problemId;

    /** 题目在课程内的顺序 */
    private Integer sortOrder;
}
