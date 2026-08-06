package com.seeyou.ai.client.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户注册信息 DTO
 * 对应 user 服务 UserController#innerGetRegisterInfo 返回的 RegisterInfoVO 结构（字段名一致）。
 * OpenFeign 通过 Jackson 反序列化，DTO 与 user 的 VO 仅字段对齐，不依赖 user 模块代码。
 */
@Data
public class RegisterInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nickname;
    /**
     * 所在地区，格式"省份,城市"（如"北京市,北京""广东省,广州"），可能为空。
     * 和风天气 location 参数支持该组合格式。空时 AI 侧不调用天气，按"无地区"生成欢迎语。
     */
    private String city;
    private LocalDateTime createTime;
}
