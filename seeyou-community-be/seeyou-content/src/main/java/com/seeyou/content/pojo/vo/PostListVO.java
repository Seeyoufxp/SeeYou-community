package com.seeyou.content.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容列表项
 * 列表只展示主表字段（标题/摘要/计数），不连详情表
 */
@Data
@Schema(description = "内容列表项")
public class PostListVO {

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容类型 1:社交帖子 2:技术博客 3:问答")
    private Integer type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

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

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;
}
