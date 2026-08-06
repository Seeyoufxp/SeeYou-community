package com.seeyou.chat.service;

import com.seeyou.chat.pojo.dto.ChatRoomCreateDTO;
import com.seeyou.chat.pojo.dto.ChatRoomUpdateDTO;
import com.seeyou.chat.pojo.entity.ChatRoom;
import com.seeyou.chat.pojo.vo.ChatRoomVO;
import com.seeyou.chat.pojo.vo.OnlineMemberVO;
import com.seeyou.common.result.PageResult;

import java.util.List;

/**
 * 聊天室服务接口
 */
public interface IChatRoomService {

    /** 创建聊天室（管理员） */
    Long create(ChatRoomCreateDTO dto);

    /** 修改聊天室名称/公告（创建者或管理员） */
    void update(Long id, ChatRoomUpdateDTO dto);

    /** 解散聊天室（创建者或管理员，置 status=0） */
    void dissolve(Long id);

    /** 聊天室列表（仅 status=1，分页） */
    PageResult<ChatRoomVO> list(Long current, Long size);

    /** 聊天室详情 */
    ChatRoomVO getDetail(Long id);

    /** 聊天室当前在线人数 */
    Long getOnlineCount(Long roomId);

    /** 聊天室在线成员列表 */
    List<OnlineMemberVO> getOnlineMembers(Long roomId);

    /**
     * 校验聊天室存在且状态正常，返回实体。不存在或已解散抛 BusinessException。
     * 供消息服务发送前校验使用。
     */
    ChatRoom checkRoomAvailable(Long roomId);
}
