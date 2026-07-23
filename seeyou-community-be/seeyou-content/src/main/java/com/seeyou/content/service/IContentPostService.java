package com.seeyou.content.service;

import com.seeyou.common.result.PageResult;
import com.seeyou.content.pojo.dto.PostPublishDTO;
import com.seeyou.content.pojo.dto.PostQueryDTO;
import com.seeyou.content.pojo.dto.PostUpdateDTO;
import com.seeyou.content.pojo.enums.ContentType;
import com.seeyou.content.pojo.vo.LikeResultVO;
import com.seeyou.content.pojo.vo.PostDetailVO;
import com.seeyou.content.pojo.vo.PostListVO;

/**
 * 内容服务（帖子/博客/问答共用）
 * type 由调用方传入，不在 DTO 中携带，避免越权改类型
 */
public interface IContentPostService {

    /** 发布内容，返回新生成的内容ID */
    Long publish(ContentType type, PostPublishDTO dto);

    /** 编辑内容 */
    void update(Long id, PostUpdateDTO dto);

    /** 删除内容（逻辑删） */
    void delete(Long id);

    /** 详情 */
    PostDetailVO getDetail(Long id);

    /** 列表 */
    PageResult<PostListVO> list(ContentType type, PostQueryDTO query);

    /** 点赞 */
    LikeResultVO like(Long id);

    /** 取消点赞 */
    LikeResultVO unlike(Long id);
}
