package com.seeyou.search.client;

import com.seeyou.common.result.R;
import com.seeyou.search.client.dto.PostSearchDocDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * content 服务远程调用客户端
 * OpenFeign 通过 Nacos 服务发现直连 seeyou-content，不经网关，无需在网关白名单中放行 inner 路径。
 */
@FeignClient(name = "seeyou-content", path = "/api/post/inner")
public interface ContentClient {

    @GetMapping("/{id}/search-doc")
    R<PostSearchDocDTO> getSearchDoc(@PathVariable("id") Long id);
}
