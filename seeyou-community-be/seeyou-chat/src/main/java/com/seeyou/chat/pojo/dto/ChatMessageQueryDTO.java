package com.seeyou.chat.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天历史消息分页查询 DTO
 * 按 create_time DESC, id DESC 倒序分页（current=1 返回最新一页）。
 */
@Data
@Schema(description = "聊天历史消息分页查询参数")
public class ChatMessageQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天室ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "聊天室ID不能为空")
    private Long roomId;

    @Schema(description = "页码，从1开始", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "20")
    private Long size = 20L;
}
