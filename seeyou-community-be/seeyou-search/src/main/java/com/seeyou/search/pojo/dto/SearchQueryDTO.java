package com.seeyou.search.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 全站搜索入参
 */
@Data
@Schema(description = "全站搜索入参")
public class SearchQueryDTO {

    @Schema(description = "关键词（标题/摘要/正文 multi_match）；为空则按时间倒序返回最近内容", example = "Spring")
    private String keyword;

    @Schema(description = "内容类型 1:社交帖子 2:技术博客 3:问答，不传则全部")
    private Integer type;

    @Schema(description = "当前页", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}
