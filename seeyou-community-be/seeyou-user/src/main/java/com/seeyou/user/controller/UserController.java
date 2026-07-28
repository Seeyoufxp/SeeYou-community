package com.seeyou.user.controller;

import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.R;
import com.seeyou.common.result.ResultCode;
import com.seeyou.common.utils.IdWorker;
import com.seeyou.common.utils.OssUtils;
import com.seeyou.user.pojo.dto.LoginDTO;
import com.seeyou.user.pojo.dto.RegisterDTO;
import com.seeyou.user.pojo.dto.UserInfoDTO;
import com.seeyou.user.pojo.vo.LoginVO;
import com.seeyou.user.pojo.vo.RegisterInfoVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Tag(name = "用户服务")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserInfoService userInfoService;
    private final OssUtils ossUtils;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

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

    @Operation(summary = "上传图片（头像等），返回公网URL")
    @PostMapping("/upload/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 JPG/PNG/GIF/WEBP 格式");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片大小不能超过 5MB");
        }
        // 按用户隔离目录：avatar/{userId}/{雪花id}.{ext}
        Long userId = UserContext.getUserId();
        String ext = guessExt(file.getOriginalFilename(), contentType);
        String key = "avatar/" + userId + "/" + IdWorker.nextIdStr() + ext;
        String url = ossUtils.upload(file.getBytes(), key, contentType);
        return R.ok(url);
    }

    private String guessExt(String filename, String contentType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0 && dot < filename.length() - 1) {
                return filename.substring(dot).toLowerCase();
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> "";
        };
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

    @Operation(summary = "内部接口：查用户注册信息（供AI服务计算注册时长/取城市）")
    @GetMapping("/inner/{id}/register-info")
    public R<RegisterInfoVO> innerGetRegisterInfo(@PathVariable Long id) {
        return R.ok(userInfoService.getRegisterInfo(id));
    }
}
