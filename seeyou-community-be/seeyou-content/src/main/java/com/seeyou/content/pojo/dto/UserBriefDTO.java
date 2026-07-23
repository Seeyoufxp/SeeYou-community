package com.seeyou.content.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户概要 DTO（来自 seeyou-user 远程调用）
 * 仅包含内容列表/详情渲染作者所需的最少字段，避免耦合完整用户资料。
 */
@Data
@Schema(description = "用户概要")
public class UserBriefDTO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;
}