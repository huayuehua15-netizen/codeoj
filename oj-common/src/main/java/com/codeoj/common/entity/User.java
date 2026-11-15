package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（论文表 4-1）。密码为 BCrypt 哈希，用户名支持中文。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（支持中文，全局唯一） */
    @TableField("username")
    private String username;

    /** BCrypt 哈希 */
    @TableField("password")
    private String password;

    private String email;

    /** 0=用户，1=管理员 */
    @TableField("role")
    private Integer role;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
