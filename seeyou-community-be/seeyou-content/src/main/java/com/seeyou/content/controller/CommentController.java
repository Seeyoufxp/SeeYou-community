package com.seeyou.content.controller;

import com.seeyou.common.result.R;
import com.seeyou.content.pojo.dto.CommentCreateDTO;
import com.seeyou.content.pojo.vo.CommentVO;
import com.seeyou.content.pojo.vo.LikeResultVO;
import com.seeyou.content.service.IContentCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论/回答统一入口
 * 评论和问答的回答共用 content_comment 表，由 postId 关联的 content_post.type 决定语义。
 * 楼中楼规则在 service 层校验：问答(type=3) 不允许 parent_id!=0；帖子/博客(type=1/2) 允许一层楼中楼。
 */
@Tag(name = "评论/回答")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final IContentCommentService contentCommentService;

    @Operation(summary = "创建评论/回答")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CommentCreateDTO dto) {
        return R.ok(contentCommentService.createComment(dto));
    }

    @Operation(summary = "评论/回答列表（树形，一级评论带楼中楼）")
    @GetMapping("/list")
    public R<List<CommentVO>> list(@Parameter(description = "内容ID") @RequestParam Long postId) {
        return R.ok(contentCommentService.listComments(postId));
    }

    @Operation(summary = "删除评论（删一级评论会连带删除其楼中楼）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentCommentService.delete(id);
        return R.ok();
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    public R<LikeResultVO> like(@PathVariable Long id) {
        return R.ok(contentCommentService.like(id));
    }

    @Operation(summary = "取消点赞评论")
    @PostMapping("/{id}/unlike")
    public R<LikeResultVO> unlike(@PathVariable Long id) {
        return R.ok(contentCommentService.unlike(id));
    }
}
