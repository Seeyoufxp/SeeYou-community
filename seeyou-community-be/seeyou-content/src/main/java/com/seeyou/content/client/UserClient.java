package com.seeyou.content.client;

import com.seeyou.common.result.R;
import com.seeyou.content.pojo.dto.UserBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.List;

// 用户服务远程调用客户端

@FeignClient(name = "seeyou-user", path = "/api/user/inner")
public interface UserClient {

    @GetMapping("/{id}")
    R<UserBriefDTO> getById(@PathVariable("id") Long id);

    @PostMapping("/listByIds")
    R<List<UserBriefDTO>> listByIds(@RequestBody Collection<Long> ids);
}