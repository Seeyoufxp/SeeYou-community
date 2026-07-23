package com.seeyou.content.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布内容（帖子/博客/问答）入参
 * type 由具体接口决定，DTO 不接收 type，避免前端越权改类型
 */
@Data
@Schema(description = "发布内容入参")
public class PostPublishDTO {

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最长128字符")
    private String title;

    @Schema(description = "摘要")
    @Size(max = 255, message = "摘要最长255字符")
    private String summary;

    @Schema(description = "正文")
    @NotBlank(message = "正文不能为空")
    private String content;

    @Schema(description = "状态 0:草稿 1:已发布，默认1")
    private Integer status = 1;
}
