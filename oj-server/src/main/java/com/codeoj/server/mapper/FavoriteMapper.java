package com.codeoj.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeoj.common.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
