package com.codeoj.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告表（教学场景：实验截止提醒、平台通知）。
 */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 正文（纯文本，展示层换行渲染） */
    private String content;

    private Long createdBy;

    private LocalDateTime createdAt;
}
