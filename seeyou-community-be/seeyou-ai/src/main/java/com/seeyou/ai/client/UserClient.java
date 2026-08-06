package com.seeyou.ai.client;

import com.seeyou.ai.client.dto.RegisterInfoDTO;
import com.seeyou.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user 服务远程调用客户端
 * OpenFeign 通过 Nacos 服务发现直连 seeyou-user，不经网关。
 */
@FeignClient(name = "seeyou-user", path = "/api/user/inner")
public interface UserClient {

    /**
     * 获取用户注册信息（昵称、城市、注册时间）
     * 用于 AI 欢迎语 Function Calling 计算注册时长、取城市查天气。
     */
    @GetMapping("/{id}/register-info")
    R<RegisterInfoDTO> getRegisterInfo(@PathVariable("id") Long id);
}
