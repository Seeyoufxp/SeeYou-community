package com.seeyou.content.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 内容列表查询入参
 * 分页参数 + 类型筛选 + 关键词（标题摘要模糊）+ 作者筛选 + 排序
 */
@Data
@Schema(description = "内容列表查询入参")
public class PostQueryDTO {

    @Schema(description = "当前页", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "关键词（标题/摘要模糊匹配）")
    private String keyword;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "状态，默认1=已发布")
    private Integer status = 1;

    @Schema(description = "排序字段：create_time(默认) / like_count / comment_count")
    private String sortBy = "create_time";

    @Schema(description = "排序方向 asc / desc，默认 desc")
    private String order = "desc";
}
