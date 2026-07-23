package com.seeyou.content.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容主表
 * type 区分内容类型：1:社交帖子 2:技术博客 3:问答
 * 帖子/博客/问答共用此表，列表只取主表（标题摘要+计数），详情通过 content_detail 关联取正文。
 */
@Data
@TableName("content_post")
public class ContentPost implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 内容类型 1:社交帖子 2:技术博客 3:问答 */
    private Integer type;

    private String title;

    private String summary;

    /** 状态 0:草稿 1:已发布 2:隐藏 3:封禁 */
    private Integer status;

    private Integer likeCount;

    private Integer commentCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    private String extend1;
    private String extend2;
    private String extend3;
    private String extend4;
    private String extend5;
}
