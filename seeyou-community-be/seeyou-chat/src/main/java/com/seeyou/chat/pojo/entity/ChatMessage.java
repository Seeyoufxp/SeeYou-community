package com.seeyou.chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seeyou.common.utils.IdWorker;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息实体 (chat_message)
 * id 用雪花算法（IdWorker.nextId）手动赋值，@TableId(type=INPUT)。
 * create_time 精确到毫秒（DDL: datetime(3)）。
 */
@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息ID（雪花算法，手动赋值 IdWorker.nextId()） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 所属聊天室ID */
    private Long roomId;

    /** 发送者ID */
    private Long userId;

    /** 聊天内容（纯文本） */
    private String content;

    /** 发送时间（精确到毫秒） */
    private LocalDateTime createTime;

    private String extend1;
    private String extend2;
    private String extend3;
    private String extend4;
    private String extend5;
}
