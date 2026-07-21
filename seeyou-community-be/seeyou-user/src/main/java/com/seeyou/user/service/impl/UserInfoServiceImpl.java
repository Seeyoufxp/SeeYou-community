package com.seeyou.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seeyou.common.constant.CommonConstants;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.ResultCode;
import com.seeyou.common.utils.IdWorker;
import com.seeyou.common.utils.JwtUtils;
import com.seeyou.common.utils.RedisUtils;
import com.seeyou.user.pojo.dto.LoginDTO;
import com.seeyou.user.pojo.dto.RegisterDTO;
import com.seeyou.user.pojo.dto.UserInfoDTO;
import com.seeyou.user.pojo.entity.UserInfo;
import com.seeyou.user.pojo.entity.UserPoints;
import com.seeyou.user.mapper.IUserInfoMapper;
import com.seeyou.user.mapper.IUserPointsMapper;
import com.seeyou.user.service.IUserInfoService;
import com.seeyou.user.pojo.vo.LoginVO;
import com.seeyou.user.pojo.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements IUserInfoService {

    private final IUserInfoMapper userInfoMapper;
    private final IUserPointsMapper userPointsMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userInfoMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        // 构建用户记录
        UserInfo user = new UserInfo();
        user.setId(IdWorker.nextId());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StrUtil.isBlank(dto.getNickname()) ? dto.getUsername() : dto.getNickname());
        user.setAvatarUrl("");
        user.setEmail("");
        user.setPhone("");
        user.setRole(0);
        user.setCity("");
        user.setBio("");
        user.setBlogUrl("");
        user.setCompanyOrSchool("");
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userInfoMapper.insert(user);

        // 初始化积分经验记录
        UserPoints points = new UserPoints();
        points.setUserId(user.getId());
        points.setPoints(0);
        points.setExperience(0);
        points.setLevel(1);
        points.setVersion(0);
        points.setCreateTime(LocalDateTime.now());
        points.setUpdateTime(LocalDateTime.now());
        userPointsMapper.insert(points);

        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        UserInfo user = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());

        // 写入 Redis，key="token:{userId}"
        String redisKey = CommonConstants.TOKEN_REDIS_PREFIX + user.getId();
        redisUtils.set(redisKey, token, CommonConstants.TOKEN_EXPIRE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());

        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl());
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            redisUtils.delete(CommonConstants.TOKEN_REDIS_PREFIX + userId);
            log.info("用户登出: userId={}", userId);
        }
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }

        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void editUserInfo(UserInfoDTO userInfoDTO) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }

        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        BeanUtil.copyProperties(userInfoDTO, user);
        user.setUpdateTime(LocalDateTime.now());
        userInfoMapper.updateById(user);
    }
}
