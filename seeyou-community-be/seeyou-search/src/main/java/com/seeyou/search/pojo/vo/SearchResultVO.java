package com.seeyou.search.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索结果项
 * title / summary / contentSnippet 含 <em></em> 高亮片段，前端按需渲染。
 */
@Data
@Schema(description = "搜索结果项")
public class SearchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容类型 1:社交帖子 2:技术博客 3:问答")
    private Integer type;

    @Schema(description = "标题（含高亮片段）")
    private String title;

    @Schema(description = "摘要（含高亮片段）")
    private String summary;

    @Schema(description = "内容片段（含高亮片段，已截断）")
    private String contentSnippet;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "作者昵称")
    private String nickname;

    @Schema(description = "作者头像URL")
    private String avatarUrl;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
