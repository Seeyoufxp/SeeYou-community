package com.seeyou.chat.service;

import com.seeyou.chat.pojo.dto.ChatMessageSendDTO;
import com.seeyou.chat.pojo.vo.ChatMessageVO;
import com.seeyou.common.result.PageResult;
import com.seeyou.chat.pojo.dto.ChatMessageQueryDTO;

/**
 * 聊天消息服务接口
 */
public interface IChatMessageService {

    /**
     * 发送聊天消息：落库（雪花算法 ID）+ 发 MQ 广播。
     * 返回完整 VO（含发送者昵称/头像），客户端收到 HTTP 响应后立即渲染；
     * MQ 广播用于同步给其他客户端（发送者客户端按 messageId 去重忽略回推）。
     */
    ChatMessageVO send(ChatMessageSendDTO dto);

    /** 聊天历史消息分页（按 create_time DESC, id DESC） */
    PageResult<ChatMessageVO> history(ChatMessageQueryDTO query);
}
