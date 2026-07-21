package com.seeyou.user.service;

import com.seeyou.user.pojo.dto.LoginDTO;
import com.seeyou.user.pojo.dto.RegisterDTO;
import com.seeyou.user.pojo.dto.UserInfoDTO;
import com.seeyou.user.pojo.vo.LoginVO;
import com.seeyou.user.pojo.vo.UserInfoVO;
import jakarta.validation.Valid;

public interface IUserInfoService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    void logout();

    UserInfoVO getCurrentUserInfo();

    void editUserInfo(UserInfoDTO userInfoDTO);

}
