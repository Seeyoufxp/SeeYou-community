package com.seeyou.search.config;

import com.seeyou.search.pojo.doc.ContentDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * ES 索引初始化
 * 启动时检查索引 seeyou_content 是否存在，不存在则按 ContentDoc 的 @Field 注解创建 mapping。
 * 失败（如 ES 不可用、ik 分词器未安装）只记错误日志，不阻断服务启动——
 * 搜索时会因索引不存在而 catch 返回空结果，写入则会让消息消费失败记日志，均不影响服务可用性。
 */
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations operations;

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations indexOps = operations.indexOps(ContentDoc.class);
            if (Boolean.FALSE.equals(indexOps.exists())) {
                boolean ok = indexOps.createWithMapping();
                log.info("ES 索引 seeyou_content 创建结果: {}", ok);
            } else {
                log.info("ES 索引 seeyou_content 已存在，跳过创建");
            }
        } catch (Exception e) {
            log.error("初始化 ES 索引 seeyou_content 失败（可能是 ES 不可用或未安装 ik 分词器）: {}", e.getMessage(), e);
        }
    }
}
