package com.seeyou.content.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论/回答入参
 * 评论和问答的回答共用此 DTO，落同一张 content_comment 表。
 * - 帖子/博客(type=1/2)：parentId=0 一级评论，parentId=某条一级评论id 楼中楼
 * - 问答(type=3)：parentId 必须为 0，不允许楼中楼
 */
@Data
@Schema(description = "创建评论/回答入参")
public class CommentCreateDTO {

    @Schema(description = "内容ID")
    @NotNull(message = "内容ID不能为空")
    private Long postId;

    @Schema(description = "父评论ID，0或空代表一级评论")
    private Long parentId;

    @Schema(description = "正文")
    @NotBlank(message = "内容不能为空")
    @Size(max = 1024, message = "评论最长1024字符")
    private String content;
}
