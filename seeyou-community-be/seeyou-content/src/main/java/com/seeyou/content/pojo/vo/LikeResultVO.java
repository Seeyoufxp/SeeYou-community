package com.seeyou.content.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "点赞结果")
public class LikeResultVO {

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "最新点赞数")
    private Integer likeCount;
}
