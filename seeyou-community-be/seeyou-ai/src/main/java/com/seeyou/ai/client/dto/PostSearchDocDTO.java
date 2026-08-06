package com.seeyou.ai.client.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容搜索文档 DTO
 * 对应 content 服务 ContentInnerController#getSearchDoc 返回的 PostSearchDocVO 结构。
 * MQ 消费内容事件时，AI 服务通过 Feign 拉取单条内容写入向量库。
 * 仅取 RAG 所需字段（id/title/summary/content），其余字段忽略。
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
