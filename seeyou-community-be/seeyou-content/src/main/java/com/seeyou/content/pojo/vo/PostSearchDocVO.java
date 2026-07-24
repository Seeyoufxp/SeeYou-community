package com.seeyou.content.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容搜索文档
 * 供 search 服务通过 OpenFeign 拉取后写入 Elasticsearch。
 * 字段精简到搜索/索引所需，避免耦合完整详情/点赞状态等。
 */
@Data
@Schema(description = "内容搜索文档")
public class PostSearchDocVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容类型 1:社交帖子 2:技术博客 3:问答")
    private Integer type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文（已剥离 HTML 标签的纯文本，限制长度避免超长字段）")
    private String content;

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

    @Schema(description = "状态 1:已发布")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
