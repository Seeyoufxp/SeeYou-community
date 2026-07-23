package com.seeyou.content.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 内容详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "内容详情")
public class PostDetailVO extends PostListVO {

    @Schema(description = "正文")
    private String content;

    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;
}
