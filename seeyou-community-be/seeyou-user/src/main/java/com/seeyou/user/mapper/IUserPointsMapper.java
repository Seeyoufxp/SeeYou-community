package com.seeyou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seeyou.user.pojo.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserPointsMapper extends BaseMapper<UserPoints> {
}
