package com.seeyou.ai.client;

import com.seeyou.ai.client.dto.KnowledgeDocDTO;
import com.seeyou.ai.client.dto.PostSearchDocDTO;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * content 服务远程调用客户端
 * OpenFeign 通过 Nacos 服务发现直连 seeyou-content，不经网关。
 */
@FeignClient(name = "seeyou-content", path = "/api/post/inner")
public interface ContentClient {

    /**
     * 按 ID 获取内容文档（MQ 消费内容事件时拉取单条，用于向量库增量同步）
     */
    @GetMapping("/{id}/search-doc")
    R<PostSearchDocDTO> getSearchDoc(@PathVariable("id") Long id);

    /**
     * 分页获取知识中心文档（博客+问答），用于向量库全量初始化/重建
     */
    @GetMapping("/knowledge/list")
    R<PageResult<KnowledgeDocDTO>> listKnowledge(
            @RequestParam("current") int current,
            @RequestParam("size") int size);
}
