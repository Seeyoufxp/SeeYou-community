package com.seeyou.content.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论/回答 VO
 * children 用于楼中楼：仅一级评论(parentId=0)会填充 children，
 * 楼中楼回复自身 children 为空集合，避免无限嵌套（只支持一层楼中楼）。
 */
@Data
@Schema(description = "评论/回答")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "所属内容ID")
    private Long postId;

    @Schema(description = "评论人ID")
    private Long userId;

    @Schema(description = "父评论ID，0代表一级评论")
    private Long parentId;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "楼中楼回复（仅一级评论有）")
    private List<CommentVO> children;
}
