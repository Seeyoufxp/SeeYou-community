package com.seeyou.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度需在6-50之间")
    private String password;

    private String nickname;

    /**
     * 选填：所在地区，格式"省份,城市"（如"北京市,北京""广东省,广州"）。
     * 和风天气 location 参数支持该组合格式。
     */
    private String city;
}
