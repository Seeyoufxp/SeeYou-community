package com.seeyou.content.controller;

import com.seeyou.common.result.R;
import com.seeyou.content.pojo.vo.PostSearchDocVO;
import com.seeyou.content.service.IContentPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容服务内部接口
 * 仅供其他微服务（如 search）通过 OpenFeign 调用，不走网关白名单。
 * OpenFeign 通过 Nacos 服务发现直连，绕过网关，请求头不带 X-User-* 也无妨——
 * UserContextInterceptor 在缺失头时只跳过写上下文，不影响业务。
 */
@Tag(name = "内容服务内部接口")
@RestController
@RequestMapping("/api/post/inner")
@RequiredArgsConstructor
public class ContentInnerController {

    private final IContentPostService contentPostService;

    @Operation(summary = "内部接口：按ID获取搜索索引文档")
    @GetMapping("/{id}/search-doc")
    public R<PostSearchDocVO> getSearchDoc(@PathVariable Long id) {
        return R.ok(contentPostService.getSearchDoc(id));
    }
}
