package com.seeyou.ai.service.impl;

import com.seeyou.ai.client.ContentClient;
import com.seeyou.ai.client.dto.KnowledgeDocDTO;
import com.seeyou.ai.client.dto.PostSearchDocDTO;
import com.seeyou.ai.config.KnowledgeProperties;
import com.seeyou.ai.pojo.event.ContentEventMsg;
import com.seeyou.ai.service.KnowledgeService;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库管理实现
 * 基于 RedisVectorStore（RediSearch / Redis Stack），向量数据持久化在 Redis，重启不丢失。
 * 维护已加载文档 id 集合以支持 rebuild 时清空旧数据（注意：重启后 loadedIds 为空，
 * rebuild 无法清掉重启前 Redis 里的旧数据，此时建议保持 autoInit 全量覆盖写入，同 id 会覆盖）。
 *
 * 数据流：
 * - 启动：ApplicationReadyEvent 触发异步全量加载（content 不可用则跳过，不阻断启动）
 *   首次切换到 Redis 时必须灌一次数据；之后 Redis 已有数据，可将 autoInit 关闭以节省 embedding API 成本
 * - 运行时：MQ 消费 content-event-topic 增量同步（CREATE/UPDATE 覆盖写，DELETE 删除）
 * - 手动：POST /api/ai/knowledge/rebuild 先清空再全量加载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final VectorStore vectorStore;
    private final ContentClient contentClient;
    private final KnowledgeProperties properties;

    /** 已加载文档 id 集合，rebuild 时用于先清空旧数据 */
    private final Set<String> loadedIds = ConcurrentHashMap.newKeySet();

    @Override
    public void handleContentEvent(Long id, String action) {
        String docId = String.valueOf(id);
        try {
            if (ContentEventMsg.DELETE.equals(action)) {
                vectorStore.delete(List.of(docId));
                loadedIds.remove(docId);
                log.info("[AI知识库] 删除文档: id={}", id);
                return;
            }
            if (ContentEventMsg.CREATE.equals(action) || ContentEventMsg.UPDATE.equals(action)) {
                R<PostSearchDocDTO> resp = contentClient.getSearchDoc(id);
                if (resp == null || resp.getData() == null) {
                    // 内容已被逻辑删/草稿/隐藏 → 兜底删除向量库中的文档
                    vectorStore.delete(List.of(docId));
                    loadedIds.remove(docId);
                    log.info("[AI知识库] 内容不可索引，已删除(如有): id={}", id);
                    return;
                }
                addPostDocument(resp.getData());
                log.info("[AI知识库] 同步文档完成: id={}, action={}", id, action);
                return;
            }
            log.warn("[AI知识库] 未知 action，忽略: id={}, action={}", id, action);
        } catch (Exception e) {
            log.error("[AI知识库] 处理内容事件失败: id={}, action={}", id, action, e);
        }
    }

    @Override
    public int rebuild() {
        // 先清空已加载的文档
        if (!loadedIds.isEmpty()) {
            try {
                vectorStore.delete(new ArrayList<>(loadedIds));
            } catch (Exception e) {
                log.warn("[AI知识库] 清空旧文档时部分失败，继续全量加载", e);
            }
            loadedIds.clear();
        }
        return loadAll();
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        if (!properties.isAutoInit()) {
            log.info("[AI知识库] autoInit=false，跳过启动加载");
            return;
        }
        // 异步加载，不阻塞启动
        new Thread(() -> {
            try {
                int count = loadAll();
                log.info("[AI知识库] 启动加载完成，共 {} 篇文档", count);
            } catch (Exception e) {
                log.warn("[AI知识库] 启动加载失败(content服务可能不可用)，可稍后手动 rebuild", e);
            }
        }, "ai-knowledge-init").start();
    }

    /**
     * 全量分页拉取博客+问答，embedding 后写入向量库
     * @return 加载的文档数
     */
    private int loadAll() {
        int current = 1;
        int size = properties.getInitPageSize();
        int total = 0;
        while (true) {
            R<PageResult<KnowledgeDocDTO>> resp;
            try {
                resp = contentClient.listKnowledge(current, size);
            } catch (Exception e) {
                log.warn("[AI知识库] 拉取知识文档失败: current={}", current, e);
                break;
            }
            if (resp == null || resp.getData() == null || resp.getData().getRecords() == null
                    || resp.getData().getRecords().isEmpty()) {
                break;
            }
            List<KnowledgeDocDTO> records = resp.getData().getRecords();
            for (KnowledgeDocDTO doc : records) {
                try {
                    addKnowledgeDocument(doc);
                    total++;
                } catch (Exception e) {
                    log.warn("[AI知识库] 单文档写入失败: id={}", doc.getId(), e);
                }
            }
            // 不足一页说明已到末尾
            if (records.size() < size) {
                break;
            }
            current++;
        }
        log.info("[AI知识库] 全量加载完成: {} 篇", total);
        return total;
    }

    /** 写入单条内容（MQ 增量同步用，来自 search-doc 接口） */
    private void addPostDocument(PostSearchDocDTO doc) {
        String docId = String.valueOf(doc.getId());
        String text = buildText(doc.getTitle(), doc.getSummary(), doc.getContent());
        Document document = new Document(docId, text, buildMetadata(doc.getId(), doc.getType(), doc.getTitle()));
        vectorStore.add(List.of(document));
        loadedIds.add(docId);
    }

    /** 写入单条知识文档（全量加载用，来自 knowledge/list 接口） */
    private void addKnowledgeDocument(KnowledgeDocDTO doc) {
        String docId = String.valueOf(doc.getId());
        String text = buildText(doc.getTitle(), doc.getSummary(), doc.getContent());
        Document document = new Document(docId, text, buildMetadata(doc.getId(), doc.getType(), doc.getTitle()));
        vectorStore.add(List.of(document));
        loadedIds.add(docId);
    }

    private String buildText(String title, String summary, String content) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title);
        }
        if (summary != null && !summary.isBlank()) {
            sb.append("\n").append(summary);
        }
        if (content != null && !content.isBlank()) {
            sb.append("\n").append(content);
        }
        return sb.toString();
    }

    private Map<String, Object> buildMetadata(Long id, Integer type, String title) {
        return Map.of(
                "id", id,
                "type", type == null ? 0 : type,
                "title", title == null ? "" : title
        );
    }
}
