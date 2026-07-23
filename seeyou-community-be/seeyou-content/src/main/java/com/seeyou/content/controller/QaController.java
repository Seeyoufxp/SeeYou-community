package com.seeyou.content.controller;

import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import com.seeyou.content.pojo.dto.PostPublishDTO;
import com.seeyou.content.pojo.dto.PostQueryDTO;
import com.seeyou.content.pojo.dto.PostUpdateDTO;
import com.seeyou.content.pojo.enums.ContentType;
import com.seeyou.content.pojo.vo.LikeResultVO;
import com.seeyou.content.pojo.vo.PostDetailVO;
import com.seeyou.content.pojo.vo.PostListVO;
import com.seeyou.content.service.IContentPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "问答")
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final IContentPostService contentPostService;

    @Operation(summary = "发布问答")
    @PostMapping
    public R<Long> publish(@Valid @RequestBody PostPublishDTO dto) {
        return R.ok(contentPostService.publish(ContentType.QA, dto));
    }

    @Operation(summary = "编辑问答")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PostUpdateDTO dto) {
        contentPostService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除问答")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentPostService.delete(id);
        return R.ok();
    }

    @Operation(summary = "问答列表")
    @GetMapping("/list")
    public R<PageResult<PostListVO>> list(@ModelAttribute PostQueryDTO query) {
        return R.ok(contentPostService.list(ContentType.QA, query));
    }

    @Operation(summary = "问答详情")
    @GetMapping("/{id}")
    public R<PostDetailVO> detail(@PathVariable Long id) {
        return R.ok(contentPostService.getDetail(id));
    }

    @Operation(summary = "点赞问答")
    @PostMapping("/{id}/like")
    public R<LikeResultVO> like(@PathVariable Long id) {
        return R.ok(contentPostService.like(id));
    }

    @Operation(summary = "取消点赞问答")
    @PostMapping("/{id}/unlike")
    public R<LikeResultVO> unlike(@PathVariable Long id) {
        return R.ok(contentPostService.unlike(id));
    }
}
