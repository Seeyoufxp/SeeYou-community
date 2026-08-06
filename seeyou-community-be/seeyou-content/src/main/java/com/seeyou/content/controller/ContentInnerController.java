package com.seeyou.content.controller;

import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import com.seeyou.content.pojo.vo.KnowledgeDocVO;
import com.seeyou.content.pojo.vo.PostSearchDocVO;
import com.seeyou.content.service.IContentPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容服务内部接口
 * 仅供其他微服务（如 search / ai）通过 OpenFeign 调用，不走网关白名单。
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

    @Operation(summary = "内部接口：分页获取知识中心文档（博客+问答，供AI构建向量库）")
    @GetMapping("/knowledge/list")
    public R<PageResult<KnowledgeDocVO>> listKnowledge(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数，最大100") @RequestParam(defaultValue = "20") int size) {
        return R.ok(contentPostService.listKnowledge(current, size));
    }
}
