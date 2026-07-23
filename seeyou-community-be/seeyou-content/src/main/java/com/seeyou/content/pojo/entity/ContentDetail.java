package com.seeyou.content.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 内容详情表
 * 主键 post_id 关联 content_post.id，存正文（longtext）。
 * 由于主键非自增，使用 IdType.INPUT 由 service 显式赋值。
 */
@Data
@TableName("content_detail")
public class ContentDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long postId;

    private String content;
}
