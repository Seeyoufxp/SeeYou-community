package com.seeyou.chat.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改聊天室 DTO（name/description 均可选，仅传需要修改的字段）
 */
@Data
@Schema(description = "修改聊天室参数")
public class ChatRoomUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天室名称")
    @Size(max = 100, message = "聊天室名称最长100字符")
    private String name;

    @Schema(description = "聊天室公告")
    @Size(max = 255, message = "聊天室公告最长255字符")
    private String description;
}
