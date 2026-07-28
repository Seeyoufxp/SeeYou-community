package com.seeyou.chat.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建聊天室 DTO
 */
@Data
@Schema(description = "创建聊天室参数")
public class ChatRoomCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天室名称", example = "技术交流群")
    @NotBlank(message = "聊天室名称不能为空")
    @Size(max = 100, message = "聊天室名称最长100字符")
    private String name;

    @Schema(description = "聊天室公告", example = "请文明发言")
    @Size(max = 255, message = "聊天室公告最长255字符")
    private String description;
}
