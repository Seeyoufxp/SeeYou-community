package com.seeyou.chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天室实体 (chat_room)
 * 注意：chat_room 表无 is_deleted 字段，解散/封禁通过 status=0 体现。
 */
@Data
@TableName("chat_room")
public class ChatRoom implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 聊天室ID（自增，覆盖全局 input 配置） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 聊天室名称 */
    private String name;

    /** 聊天室公告 */
    private String description;

    /** 创建者ID (对应 user_info.id) */
    private Long creatorId;

    /** 状态 1:正常 0:已解散/封禁 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    private String extend1;
    private String extend2;
    private String extend3;
    private String extend4;
    private String extend5;
}
