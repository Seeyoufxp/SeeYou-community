package com.seeyou.ai.client.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识中心文档 DTO
 * 对应 content 服务 ContentInnerController#listKnowledge 返回的 KnowledgeDocVO 结构。
 * 用于 AI 服务全量拉取博客+问答构建 RAG 向量知识库。
 */
@Data
public class KnowledgeDocDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 2:技术博客 3:问答 */
    private Integer type;
    private String title;
    private String summary;
    /** 正文（纯文本，已截断） */
    private String content;
    private LocalDateTime createTime;
}
