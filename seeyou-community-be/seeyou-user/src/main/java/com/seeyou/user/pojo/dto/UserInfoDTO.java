package com.seeyou.user.pojo.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private String nickname;
    private String avatar_url;
    private String email;
    private String phone;
    private String city;
    private String bio;
    private String blog_url;
    private String company_or_school;
}
