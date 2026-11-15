package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目收藏表：用户与题目多对多（唯一约束防重复收藏）。
 */
@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long problemId;

    private LocalDateTime createdAt;
}
