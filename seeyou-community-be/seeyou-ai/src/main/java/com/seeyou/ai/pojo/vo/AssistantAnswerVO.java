package com.seeyou.ai.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 开发助手回答
 * RAG 检索知识中心内容后由 LLM 生成，附引用来源。
 */
@Data
@Schema(description = "AI助手回答")
public class AssistantAnswerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "AI 生成的回答")
    private String answer;

    @Schema(description = "引用的知识来源列表（可能为空）")
    private List<Reference> references;

    @Data
    @Schema(description = "知识来源引用")
    public static class Reference implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "内容ID")
        private Long id;

        @Schema(description = "内容类型 2:技术博客 3:问答")
        private Integer type;

        @Schema(description = "标题")
        private String title;

        public Reference() {
        }

        public Reference(Long id, Integer type, String title) {
            this.id = id;
            this.type = type;
            this.title = title;
        }
    }
}
