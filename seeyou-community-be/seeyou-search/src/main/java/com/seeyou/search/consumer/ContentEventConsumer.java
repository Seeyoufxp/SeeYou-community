package com.seeyou.search.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.seeyou.common.result.R;
import com.seeyou.search.client.ContentClient;
import com.seeyou.search.client.dto.PostSearchDocDTO;
import com.seeyou.search.pojo.doc.ContentDoc;
import com.seeyou.search.pojo.event.ContentEventMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 内容事件消费者
 * Spring Cloud Stream 通过 binding `contentEvent-in-0` 调用名为 contentEvent 的 Consumer Bean。
 * 收到 content 服务发来的 (id, action) 后：
 *   CREATE/UPDATE：远程拉内容写 ES
 *   DELETE：从 ES 删除文档
 * 任意异常均 try-catch 降级，不抛回 binder，避免无限重投到死信队列——后续可补本地消息表重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEventConsumer {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";

    /**
     * 时间字段写入 ES 的格式。
     * 选 yyyy-MM-dd'T'HH:mm:ss.SSS（带毫秒）是因为 ES 默认的 date_optional_time 接受完整 ISO 8601，
     * 且这种格式的字典序与时间先后一致，sort by createTime desc 能正确生效。
     */
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final ContentClient contentClient;
    private final ElasticsearchOperations operations;

    @Bean
    public Consumer<ContentEventMsg> contentEvent() {
        return msg -> {
            if (msg == null || msg.getId() == null) {
                log.warn("收到空内容事件，忽略");
                return;
            }
            log.info("收到内容事件: id={}, action={}", msg.getId(), msg.getAction());
            try {
                if (DELETE.equals(msg.getAction())) {
                    operations.delete(String.valueOf(msg.getId()), ContentDoc.class);
                    return;
                }
                if (CREATE.equals(msg.getAction()) || UPDATE.equals(msg.getAction())) {
                    R<PostSearchDocDTO> resp = contentClient.getSearchDoc(msg.getId());
                    if (resp == null || resp.getData() == null) {
                        // 内容已被逻辑删/草稿/隐藏/封禁 → 确保 ES 中没有该文档（兜底删除）
                        operations.delete(String.valueOf(msg.getId()), ContentDoc.class);
                        log.info("无可索引文档，已从 ES 删除（如有）: id={}", msg.getId());
                        return;
                    }
                    ContentDoc doc = new ContentDoc();
                    BeanUtil.copyProperties(resp.getData(), doc);
                    // BeanUtil.copyProperties 已把 LocalDateTime 原样 copy 进 String 字段，调用 toString 会输出纳秒精度
                    // 统一改用 ISO 8601 (毫秒精度) 格式化，确保 ES 端能正确解析
                    doc.setCreateTime(formatDateTime(resp.getData().getCreateTime()));
                    doc.setUpdateTime(formatDateTime(resp.getData().getUpdateTime()));
                    operations.save(doc);
                    log.info("ES 文档同步完成: id={}", msg.getId());
                    return;
                }
                log.warn("未知内容事件 action，忽略: id={}, action={}", msg.getId(), msg.getAction());
            } catch (Exception e) {
                // 不抛出避免 RocketMQ binder 无限重投到死信队列
                log.error("处理内容事件失败: id={}, action={}", msg.getId(), msg.getAction(), e);
            }
        };
    }

    private static String formatDateTime(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.format(ISO_FMT);
    }
}
