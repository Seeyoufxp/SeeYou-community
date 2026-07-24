package com.seeyou.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.seeyou.common.result.PageResult;
import com.seeyou.search.pojo.doc.ContentDoc;
import com.seeyou.search.pojo.dto.SearchQueryDTO;
import com.seeyou.search.pojo.vo.SearchResultVO;
import com.seeyou.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索服务实现
 * 基于 ElasticsearchOperations + NativeQuery（elc 包，5.x 推荐用法）。
 * 索引不存在或 ES 故障时 catch 返回空结果，避免接口 500 影响首页搜索可用性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {

    private final ElasticsearchOperations operations;

    @Override
    public PageResult<SearchResultVO> search(SearchQueryDTO query) {
        int current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 50);
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();

        Query queryDsl;
        boolean hasKeyword = StrUtil.isNotBlank(keyword);
        if (hasKeyword) {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder()
                    .should(Query.of(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("title^3", "summary^2", "content")
                            .type(TextQueryType.BestFields)
                    )))
                    .minimumShouldMatch("1");
            if (query.getType() != null) {
                boolBuilder.filter(f -> f.term(t -> t.field("type").value(query.getType())));
            }
            // 数据同步时已过滤 status，但兜底再过滤一遍，确保不命中草稿/隐藏/封禁
            boolBuilder.filter(f -> f.term(t -> t.field("status").value(1)));
            queryDsl = Query.of(q -> q.bool(boolBuilder.build()));
        } else {
            queryDsl = Query.of(q -> q.matchAll(m -> m));
        }

        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();
        HighlightField titleField = new HighlightField("title",
                HighlightFieldParameters.builder().withNumberOfFragments(0).build());
        HighlightField summaryField = new HighlightField("summary",
                HighlightFieldParameters.builder().withNumberOfFragments(0).withNoMatchSize(200).build());
        HighlightField contentField = new HighlightField("content",
                HighlightFieldParameters.builder().withNumberOfFragments(2).withFragmentSize(150).withNoMatchSize(200).build());
        Highlight highlight = new Highlight(params, List.of(titleField, summaryField, contentField));

        NativeQueryBuilder qb = NativeQuery.builder()
                .withQuery(queryDsl)
                .withHighlightQuery(new HighlightQuery(highlight, null))
                .withPageable(PageRequest.of(current - 1, size));

        if (hasKeyword) {
            qb.withSort(Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createTime")));
        } else {
            qb.withSort(Sort.by(Sort.Order.desc("createTime")));
        }

        NativeQuery nativeQuery = qb.build();

        try {
            SearchHits<ContentDoc> hits = operations.search(nativeQuery, ContentDoc.class);
            List<SearchResultVO> records = new ArrayList<>();
            for (SearchHit<ContentDoc> hit : hits.getSearchHits()) {
                ContentDoc doc = hit.getContent();
                if (doc == null) {
                    continue;
                }
                Map<String, List<String>> hls = hit.getHighlightFields();
                SearchResultVO vo = new SearchResultVO();
                vo.setId(doc.getId());
                vo.setType(doc.getType());
                vo.setTitle(pickFirst(hls.get("title"), doc.getTitle()));
                vo.setSummary(pickFirst(hls.get("summary"), doc.getSummary()));
                vo.setContentSnippet(pickFirst(hls.get("content"), truncate(doc.getContent(), 150)));
                vo.setUserId(doc.getUserId());
                vo.setNickname(doc.getNickname());
                vo.setAvatarUrl(doc.getAvatarUrl());
                vo.setLikeCount(doc.getLikeCount());
                vo.setCommentCount(doc.getCommentCount());
                vo.setCreateTime(parseDateTime(doc.getCreateTime()));
                records.add(vo);
            }
            long total = hits.getTotalHits();
            return new PageResult<>(records, total, current, size);
        } catch (Exception e) {
            log.error("ES 搜索失败，返回空结果: keyword={}", keyword, e);
            return PageResult.empty(current, size);
        }
    }

    private String pickFirst(List<String> fragments, String fallback) {
        if (fragments == null || fragments.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return fragments.get(0);
    }

    private String truncate(String text, int maxLen) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    /**
     * ES 里存的是 ISO 8601 字符串（yyyy-MM-dd'T'HH:mm:ss.SSS），但 ES 的 date_optional_time 格式
     * 也允许纯日期（"2026-07-24"）被原样保留在 _source 里。这里按"完整 ISO 优先，纯日期兜底"解析。
     */
    private LocalDateTime parseDateTime(String s) {
        if (StrUtil.isBlank(s)) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        } catch (Exception ignored) {
            // 兜底：纯日期 → 当天 00:00
        }
        try {
            return LocalDateTime.parse(s);  // 标准 ISO_LOCAL_DATE_TIME
        } catch (Exception ignored) {
            // 继续兜底
        }
        try {
            return java.time.LocalDate.parse(s).atStartOfDay();
        } catch (Exception e) {
            log.warn("createTime 解析失败，返回 null: {}", s);
            return null;
        }
    }
}
