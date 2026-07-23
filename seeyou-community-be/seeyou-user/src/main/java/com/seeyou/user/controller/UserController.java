package com.seeyou.user.controller;

import com.seeyou.common.result.R;
import com.seeyou.user.pojo.dto.LoginDTO;
import com.seeyou.user.pojo.dto.RegisterDTO;
import com.seeyou.user.pojo.dto.UserInfoDTO;
import com.seeyou.user.pojo.vo.LoginVO;
import com.seeyou.user.pojo.vo.UserBriefVO;
import com.seeyou.user.pojo.vo.UserInfoVO;
import com.seeyou.user.service.IUserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@Tag(name = "用户服务")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserInfoService userInfoService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userInfoService.register(dto);
        return R.ok();
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userInfoService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        userInfoService.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public R<UserInfoVO> info() {
        return R.ok(userInfoService.getCurrentUserInfo());
    }

    @Operation(summary = "编辑个人信息")
    @PostMapping("/edit")
    public R<Void> edit(@Valid @RequestBody UserInfoDTO userInfoDTO) {
        userInfoService.editUserInfo(userInfoDTO);
        return R.ok();
    }

    // OpenFeign调用接口

    @Operation(summary = "内部接口：按ID查用户概要")
    @GetMapping("/inner/{id}")
    public R<UserBriefVO> innerGetById(@PathVariable Long id) {
        return R.ok(userInfoService.getBriefById(id));
    }

    @Operation(summary = "内部接口：批量查用户概要")
    @PostMapping("/inner/listByIds")
    public R<List<UserBriefVO>> innerListByIds(@RequestBody Collection<Long> ids) {
        return R.ok(userInfoService.listBriefByIds(ids));
    }
}
