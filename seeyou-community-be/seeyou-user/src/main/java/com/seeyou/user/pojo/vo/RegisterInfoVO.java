package com.seeyou.user.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户注册信息 VO
 * 供 AI 微服务通过 OpenFeign 拉取，用于计算注册时长（生成个性化欢迎语）。
 * 仅暴露欢迎语生成所需字段，不返回密码/邮箱等敏感信息。
 */
@Data
@Schema(description = "用户注册信息")
public class RegisterInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "所在地区，格式\"省份,城市\"（如\"北京市,北京\"），可能为空")
    private String city;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
