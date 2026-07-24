package com.seeyou.user.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户概要 VO
 * 供内部服务（如 seeyou-content）远程调用批量拉取作者信息使用。
 * 仅暴露展示必需的 3 个字段，避免完整资料外泄。
 */
@Data
@Schema(description = "用户概要")
public class UserBriefVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;

    public UserBriefVO() {
    }

    public UserBriefVO(Long id, String nickname, String avatarUrl) {
        this.id = id;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}