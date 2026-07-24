package com.seeyou.search.client.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容搜索文档 DTO
 * 对应 content 服务 ContentInnerController 返回的 PostSearchDocVO 结构（字段名一致）。
 * OpenFeign 通过 Jackson 反序列化 JSON 响应，DTO 与 content 的 VO 仅在字段层面对齐，
 * 不依赖 content 模块代码，保持服务解耦。
 */
@Data
public class PostSearchDocDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer type;
    private String title;
    private String summary;
    private String content;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
