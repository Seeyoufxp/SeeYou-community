package com.seeyou.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seeyou.chat.pojo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface IChatMessageMapper extends BaseMapper<ChatMessage> {
}
