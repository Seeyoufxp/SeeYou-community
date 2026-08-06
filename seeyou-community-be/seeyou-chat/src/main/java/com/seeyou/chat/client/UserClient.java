package com.seeyou.chat.client;

import com.seeyou.chat.pojo.dto.UserBriefDTO;
import com.seeyou.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.List;

/**
 * 用户微服务 Feign 客户端
 * 通过 Nacos 直连 seeyou-user，不经网关，调用内部接口 /api/user/inner/**
 */
@FeignClient(name = "seeyou-user", path = "/api/user/inner")
public interface UserClient {

    @GetMapping("/{id}")
    R<UserBriefDTO> getById(@PathVariable("id") Long id);

    @PostMapping("/listByIds")
    R<List<UserBriefDTO>> listByIds(@RequestBody Collection<Long> ids);
}
