package com.seeyou.ai.function;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户注册信息 Function 的返回值
 * LLM 拿到后用于生成个性化欢迎语（如"你已加入社区 X 天"）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;
    /** 昵称 */
    private String nickname;
    /** 注册天数 */
    private Integer registerDays;
    /** 所在地区（"省份,城市" 组合），可能为空。空时 LLM 不要提及天气 */
    private String city;
}
