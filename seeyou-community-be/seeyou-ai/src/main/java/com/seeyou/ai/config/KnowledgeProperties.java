package com.seeyou.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 知识库 RAG 相关配置
 */
@Data
@ConfigurationProperties(prefix = "seeyou.ai.knowledge")
public class KnowledgeProperties {

    /** 向量检索 Top-K */
    private int topK = 4;

    /** 相似度阈值 0~1 */
    private double similarityThreshold = 0.5;

    /** 启动时是否异步全量加载知识库 */
    private boolean autoInit = true;

    /** 全量加载每页条数 */
    private int initPageSize = 50;
}
