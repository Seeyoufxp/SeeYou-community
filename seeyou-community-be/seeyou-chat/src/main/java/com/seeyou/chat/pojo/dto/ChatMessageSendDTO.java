package com.seeyou.chat.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送聊天消息 DTO（HTTP 提交，落库后由 MQ 广播）
 */
@Data
@Schema(description = "发送聊天消息参数")
public class ChatMessageSendDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天室ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "聊天室ID不能为空")
    private Long roomId;

    @Schema(description = "消息内容（纯文本）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1024, message = "单条消息最长1024字符")
    private String content;
}
