package com.seeyou.search.service;

import com.seeyou.common.result.PageResult;
import com.seeyou.search.pojo.dto.SearchQueryDTO;
import com.seeyou.search.pojo.vo.SearchResultVO;

public interface ISearchService {

    /**
     * 全站搜索
     * 关键词非空：multi_match(title/summary/content) + type 过滤 + status=1 过滤，按相关度倒序。
     * 关键词为空：match_all 按 createTime 倒序，相当于最近内容列表。
     */
    PageResult<SearchResultVO> search(SearchQueryDTO query);
}
