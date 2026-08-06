package com.seeyou.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seeyou.chat.pojo.entity.ChatRoom;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天室 Mapper
 */
@Mapper
public interface IChatRoomMapper extends BaseMapper<ChatRoom> {
}
