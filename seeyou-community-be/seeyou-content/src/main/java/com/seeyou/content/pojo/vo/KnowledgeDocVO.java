package com.seeyou.content.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识中心文档 VO
 * 供 AI 微服务通过 OpenFeign 分页拉取博客+问答，构建 RAG 向量知识库。
 * 仅返回已发布(status=1)的内容，正文已剥离 HTML 标签并截断。
 */
@Data
@Schema(description = "知识中心文档")
public class KnowledgeDocVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容类型 2:技术博客 3:问答")
    private Integer type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文（纯文本，已截断）")
    private String content;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
