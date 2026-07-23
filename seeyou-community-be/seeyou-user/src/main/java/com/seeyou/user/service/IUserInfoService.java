package com.seeyou.user.service;

import com.seeyou.user.pojo.dto.LoginDTO;
import com.seeyou.user.pojo.dto.RegisterDTO;
import com.seeyou.user.pojo.dto.UserInfoDTO;
import com.seeyou.user.pojo.vo.LoginVO;
import com.seeyou.user.pojo.vo.UserBriefVO;
import com.seeyou.user.pojo.vo.UserInfoVO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

public interface IUserInfoService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    void logout();

    UserInfoVO getCurrentUserInfo();

    void editUserInfo(UserInfoDTO userInfoDTO);

    /**
     * 按 ID 查询用户概要（供内部 Feign 调用，用户不存在返回 null）
     */
    UserBriefVO getBriefById(Long id);

    /**
     * 批量查询用户概要（供内部 Feign 调用，仅返回存在的用户）
     */
    List<UserBriefVO> listBriefByIds(Collection<Long> ids);

}
