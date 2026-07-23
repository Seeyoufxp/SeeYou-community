package com.seeyou.content.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "编辑内容入参")
public class PostUpdateDTO {

    @Schema(description = "标题")
    @Size(max = 128, message = "标题最长128字符")
    private String title;

    @Schema(description = "摘要")
    @Size(max = 255, message = "摘要最长255字符")
    private String summary;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "状态 0:草稿 1:已发布 2:隐藏")
    private Integer status;
}
