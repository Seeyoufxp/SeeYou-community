package com.seeyou.search.controller;

import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import com.seeyou.search.pojo.dto.SearchQueryDTO;
import com.seeyou.search.pojo.vo.SearchResultVO;
import com.seeyou.search.service.ISearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全站搜索
 * 网关白名单已放开 /api/search/**，未登录用户也能搜索。
 */
@Tag(name = "全站搜索")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ISearchService searchService;

    @Operation(summary = "全站搜索内容（标题/摘要/正文）")
    @GetMapping
    public R<PageResult<SearchResultVO>> search(@ModelAttribute SearchQueryDTO query) {
        return R.ok(searchService.search(query));
    }
}
