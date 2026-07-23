package com.seeyou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seeyou.content.pojo.entity.ContentComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IContentCommentMapper extends BaseMapper<ContentComment> {

    /**
     * 点赞数原子 +1
     */
    @Update("UPDATE content_comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrLikeCount(@Param("id") Long id);

    /**
     * 点赞数原子 -1，避免减到负数
     */
    @Update("UPDATE content_comment SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrLikeCount(@Param("id") Long id);

    /**
     * 评论数原子 +1（写入 content_post.comment_count）
     */
    @Update("UPDATE content_post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    int incrPostCommentCount(@Param("postId") Long postId);

    /**
     * 评论数原子 -1，避免减到负数
     */
    @Update("UPDATE content_post SET comment_count = comment_count - 1 WHERE id = #{postId} AND comment_count > 0")
    int decrPostCommentCount(@Param("postId") Long postId);

    /**
     * 评论数原子 -delta（删除一级评论及其楼中楼时用），避免减到负数
     */
    @Update("UPDATE content_post SET comment_count = comment_count - #{delta} WHERE id = #{postId} AND comment_count >= #{delta}")
    int decrPostCommentCountBy(@Param("postId") Long postId, @Param("delta") int delta);
}
