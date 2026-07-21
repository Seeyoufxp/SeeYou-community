package com.seeyou.user.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
    private Integer role;
    private String city;
    private String bio;
    private String blogUrl;
    private String companyOrSchool;
    private Integer status;
    private LocalDateTime createTime;
}
