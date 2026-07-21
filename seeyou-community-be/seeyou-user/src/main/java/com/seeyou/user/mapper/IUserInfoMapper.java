package com.seeyou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seeyou.user.pojo.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserInfoMapper extends BaseMapper<UserInfo> {
}
