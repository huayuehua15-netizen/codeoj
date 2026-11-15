package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
