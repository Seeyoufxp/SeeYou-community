package com.seeyou.user.pojo.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private String nickname;
    private String avatar_url;
    private String email;
    private String phone;
    /** "省份,城市" 组合格式，与 user_info.city 对齐 */
    private String city;
    private String bio;
    private String blog_url;
    private String company_or_school;
}
