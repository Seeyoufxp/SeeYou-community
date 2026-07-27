package com.seeyou.ai.service;

import com.seeyou.ai.pojo.vo.AssistantAnswerVO;

/**
 * AI 开发助手服务（RAG）
 * 用户提问 → 向量检索知识中心相关内容 → 拼 prompt → LLM 生成回答 + 引用来源。
 */
public interface AssistantService {

    /**
     * RAG 问答
     * 知识库无匹配时也由 LLM 基于自身知识回答（references 为空）。
     *
     * @param question 用户问题
     * @return 回答 + 引用来源
     */
    AssistantAnswerVO ask(String question);
}
