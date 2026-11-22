package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目表（论文表 4-2）。samples/languages 为 JSON 列，submit_count/accepted_count 为冗余统计字段。
 */
@Data
@TableName(value = "problem", autoResultMap = true)
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题 */
    private String title;

    /** 题目描述（Markdown） */
    private String description;

    /** 标签数组（动态规划/贪心/模拟…，JSON） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 样例输入输出数组 [{"input": "...", "output": "..."}] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Sample> samples;

    /** 1=简单，2=中等，3=困难 */
    private Integer difficulty;

    /** 时间限制（毫秒） */
    private Integer timeLimitMs;

    /** 内存限制（MB） */
    private Integer memoryLimitMb;

    /** 允许提交的语言数组 ["c","cpp","java","python3"] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> languages;

    /**
     * JS 场景题（函数实现题）入口函数名，如 "solve"。
     * NULL = 标准 stdin/stdout 题（全部既有题目不受影响）；
     * 非空 = oj-server 在入库判题代码前包装"用户代码 + 调用器"，
     * judge 收到的仍是可直接执行的 stdin/stdout 代码，判题端协议零变化。
     * 2026-11-22 增量字段（B 侧提出，纯新增可空列）——知会 A 侧备案。
     */
    private String entryFunction;

    /** 提交总数（冗余统计） */
    private Integer submitCount;

    /** 通过总数（冗余统计） */
    private Integer acceptedCount;

    /** 是否上架 */
    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 样例输入输出。
     */
    @Data
    public static class Sample {
        private String input;
        private String output;
    }
}
