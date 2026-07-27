package com.seeyou.ai.service;

/**
 * 知识库管理服务
 * 维护 RAG 向量知识库的加载、增量同步、重建。
 * 知识来源：content 服务的博客(2)+问答(3)，已发布内容。
 */
public interface KnowledgeService {

    /**
     * 处理 content 事件（MQ 消费调用）
     * CREATE/UPDATE：远程拉内容写向量库
     * DELETE：从向量库删除
     *
     * @param id     内容ID
     * @param action CREATE/UPDATE/DELETE
     */
    void handleContentEvent(Long id, String action);

    /**
     * 全量重建知识库（手动触发）
     * 清空当前向量库，分页拉取所有博客+问答重新 embedding 入库。
     *
     * @return 加载的文档数量
     */
    int rebuild();

    /**
     * 启动时异步全量加载（不阻塞启动，content 不可用则跳过）
     */
    void initOnStartup();
}
