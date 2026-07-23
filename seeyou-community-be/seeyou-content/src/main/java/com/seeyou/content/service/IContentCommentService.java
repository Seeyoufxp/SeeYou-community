package com.seeyou.content.service;

import com.seeyou.content.pojo.dto.CommentCreateDTO;
import com.seeyou.content.pojo.vo.CommentVO;
import com.seeyou.content.pojo.vo.LikeResultVO;

import java.util.List;

/**
 * 评论/回答服务
 * 评论和问答的回答共用 content_comment 表：
 *   - 帖子/博客(type=1/2)的评论支持一层楼中楼（parent_id 指向一级评论）
 *   - 问答(type=3)的回答不支持楼中楼（parent_id 强制为 0）
 */
public interface IContentCommentService {

    /** 创建评论/回答，返回新评论ID */
    Long createComment(CommentCreateDTO dto);

    /** 评论/回答列表（树形，一级评论带 children） */
    List<CommentVO> listComments(Long postId);

    /** 删除评论（物理删；删一级评论时连带删除其楼中楼） */
    void delete(Long id);

    /** 点赞评论 */
    LikeResultVO like(Long id);

    /** 取消点赞评论 */
    LikeResultVO unlike(Long id);
}
