package com.seeyou.content.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容评论表
 * 评论和问答的回答共用此表：
 *   - 关联的 content_post.type=1/2 时，本行为"评论"
 *   - 关联的 content_post.type=3 时，本行为"回答"
 * parent_id=0 表示一级评论/回答；非 0 表示楼中楼回复，指向某条一级评论 id。
 * 问答(type=3)的 parent_id 强制要求为 0，不允许楼中楼。
 *
 * 注意：本表无 is_deleted 字段，删除为物理删除。
 */
@Data
@TableName("content_comment")
public class ContentComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    /** 父评论ID，0 代表一级评论 */
    private Long parentId;

    private String content;

    private Integer likeCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
