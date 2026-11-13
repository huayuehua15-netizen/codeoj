package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测试用例表（论文表 4-3）。标准输入/期望输出为 LONGTEXT；样例用例可在题面展示。
 */
@Data
@TableName("test_case")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题目 */
    private Long problemId;

    /** 标准输入 */
    private String input;

    /** 期望输出 */
    private String expectedOutput;

    /** 分值（计分制） */
    private Integer score;

    /** 是否样例（样例可在题面展示） */
    private Boolean isSample;
}
