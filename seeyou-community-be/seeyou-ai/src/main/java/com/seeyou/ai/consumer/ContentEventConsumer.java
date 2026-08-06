package com.seeyou.ai.consumer;

import com.seeyou.ai.pojo.event.ContentEventMsg;
import com.seeyou.ai.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 内容事件消费者（AI 知识库同步）
 * Spring Cloud Stream 通过 binding `contentEvent-in-0` 调用名为 contentEvent 的 Consumer Bean。
 * 消费组 ai-knowledge-consumer-group 与 search-consumer-group 隔离，各自独立消费全量消息。
 *
 * 收到 content 服务发来的 (id, action) 后委托 KnowledgeService 处理：
 *   CREATE/UPDATE：远程拉内容 embedding 后写入向量库
 *   DELETE：从向量库删除
 * 任意异常 try-catch 降级，不抛回 binder，避免无限重投。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEventConsumer {

    private final KnowledgeService knowledgeService;

    @Bean
    public Consumer<ContentEventMsg> contentEvent() {
        return msg -> {
            if (msg == null || msg.getId() == null) {
                log.warn("[AI知识库] 收到空内容事件，忽略");
                return;
            }
            log.info("[AI知识库] 收到内容事件: id={}, action={}", msg.getId(), msg.getAction());
            try {
                knowledgeService.handleContentEvent(msg.getId(), msg.getAction());
            } catch (Exception e) {
                log.error("[AI知识库] 处理内容事件失败: id={}, action={}", msg.getId(), msg.getAction(), e);
            }
        };
    }
}
