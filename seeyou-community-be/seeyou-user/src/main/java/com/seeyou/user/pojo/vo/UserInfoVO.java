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
    /** "省份,城市" 组合格式，如"北京市,北京""广东省,广州" */
    private String city;
    private String bio;
    private String blogUrl;
    private String companyOrSchool;
    private Integer status;
    private LocalDateTime createTime;
}
