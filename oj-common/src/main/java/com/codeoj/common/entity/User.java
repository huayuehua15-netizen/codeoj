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

    /** 0=正常，1=封禁（封禁后登录与既有令牌均失效） */
    @TableField("status")
    private Integer status;

    /** 头像相对路径（/uploads/avatars/xxx），null=未设置，前端回退首字母占位（API.md v1.8） */
    @TableField("avatar")
    private String avatar;

    /** 个性签名（≤200 字），null=未设置（API.md v1.8） */
    @TableField("signature")
    private String signature;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
