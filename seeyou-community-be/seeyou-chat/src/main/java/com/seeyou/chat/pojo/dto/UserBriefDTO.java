package com.seeyou.chat.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户概要 DTO（与 user 模块 UserBriefVO 字段对齐，避免依赖 user 模块代码）
 */
@Data
@Schema(description = "用户概要")
public class UserBriefDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;
}
