package com.seeyou.chat.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息 VO（含发送者昵称/头像，由 Feign 拉取用户信息填充）
 */
@Data
@Schema(description = "聊天消息")
public class ChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "聊天室ID")
    private Long roomId;

    @Schema(description = "发送者ID")
    private Long userId;

    @Schema(description = "发送者昵称")
    private String nickname;

    @Schema(description = "发送者头像URL")
    private String avatarUrl;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
