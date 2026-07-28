package com.seeyou.ai.service.impl;

import com.seeyou.ai.config.KnowledgeProperties;
import com.seeyou.ai.pojo.vo.AssistantAnswerVO;
import com.seeyou.ai.pojo.vo.AssistantAnswerVO.Reference;
import com.seeyou.ai.service.AssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 开发助手实现
 * 流程：用户问题 → 向量检索 Top-K 知识 → 拼 prompt → LLM 生成回答 + 引用来源
 *
 * 向量检索或 embedding 失败时降级为纯 LLM 回答（references 为空），不阻断问答。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final KnowledgeProperties properties;

    private static final String SYSTEM_PROMPT =
            "你是之友(SeeYou)开发者社区的 AI 开发助手。回答要准确、简洁、有条理，专注技术问题。" +
            "优先基于「知识库内容」回答；如果知识库里有相关信息，直接把要点写进答案里，**不要在答案前后加「根据知识库内容」「基于以上资料」之类的套话**。" +
            "如果知识库内容与问题无关或为空，就基于你自己的知识回答；也不要加「知识库未覆盖此问题」「以下为通用回答」之类的免责声明，直接给答案即可。" +
            "禁止编造不存在的库、API、文章或用户。无法确认的事老实说不知道。";

    @Override
    public AssistantAnswerVO ask(String question) {
        // 1. 向量检索相关知识（失败降级为空，纯 LLM 回答）
        List<Document> docs = retrieve(question);

        // 2. 构造引用来源
        List<Reference> references = docs.stream()
                .map(this::toReference)
                .filter(r -> r.getId() != null)
                .collect(Collectors.toList());

        // 3. 拼 prompt 调 LLM
        String context = buildContext(docs);
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text(buildUserPrompt(question, context)))
                    .call()
                    .content();
            if (answer == null || answer.isBlank()) {
                answer = "抱歉，暂时无法生成回答，请稍后重试。";
            }
        } catch (Exception e) {
            log.warn("AI 问答失败，降级返回提示: question={}", question, e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        AssistantAnswerVO vo = new AssistantAnswerVO();
        vo.setAnswer(answer);
        vo.setReferences(references);
        return vo;
    }

    /** 向量检索 Top-K 相关文档，失败返回空列表 */
    private List<Document> retrieve(String question) {
        try {
            // M4 用链式 withXxx，无 builder
            SearchRequest request = SearchRequest.query(question)
                    .withTopK(properties.getTopK())
                    .withSimilarityThreshold(properties.getSimilarityThreshold());
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.warn("向量检索失败，降级为纯 LLM 回答: question={}", question, e);
            return List.of();
        }
    }

    private Reference toReference(Document doc) {
        Object id = doc.getMetadata().get("id");
        Object type = doc.getMetadata().get("type");
        Object title = doc.getMetadata().get("title");
        Long refId = parseLong(id);
        Integer refType = parseInt(type);
        String refTitle = title == null ? "" : title.toString();
        return new Reference(refId, refType, refTitle);
    }

    private String buildContext(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "（无）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            sb.append("【").append(i + 1).append("】")
                    .append(d.getMetadata().getOrDefault("title", ""))
                    .append("\n")
                    .append(d.getContent())
                    .append("\n\n");
        }
        return sb.toString();
    }

    private String buildUserPrompt(String question, String context) {
        // 提示词里不出现"知识库内容"等字样，避免模型在回答里也复读这种前缀
        return "参考资料：\n" + context + "\n\n问题：" + question;
    }

    private Long parseLong(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(Object o) {
        if (o == null) return null;
        try {
            return Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
