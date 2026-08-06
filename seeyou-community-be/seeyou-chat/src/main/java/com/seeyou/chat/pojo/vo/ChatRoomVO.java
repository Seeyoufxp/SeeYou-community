package com.seeyou.chat.pojo.vo;

import com.seeyou.chat.pojo.dto.UserBriefDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天室列表/详情 VO
 */
@Data
@Schema(description = "聊天室信息")
public class ChatRoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天室ID")
    private Long id;

    @Schema(description = "聊天室名称")
    private String name;

    @Schema(description = "聊天室公告")
    private String description;

    @Schema(description = "创建者ID")
    private Long creatorId;

    @Schema(description = "创建者信息")
    private UserBriefDTO creator;

    @Schema(description = "状态 1:正常 0:已解散/封禁")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "当前在线人数")
    private Long onlineCount;
}
