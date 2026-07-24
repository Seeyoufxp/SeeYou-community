package com.seeyou.content.service;

import com.seeyou.common.result.PageResult;
import com.seeyou.content.pojo.dto.PostPublishDTO;
import com.seeyou.content.pojo.dto.PostQueryDTO;
import com.seeyou.content.pojo.dto.PostUpdateDTO;
import com.seeyou.content.pojo.enums.ContentType;
import com.seeyou.content.pojo.vo.LikeResultVO;
import com.seeyou.content.pojo.vo.PostDetailVO;
import com.seeyou.content.pojo.vo.PostListVO;
import com.seeyou.content.pojo.vo.PostSearchDocVO;

/**
 * 内容服务（帖子/博客/问答共用）
 * type 由调用方传入，不在 DTO 中携带，避免越权改类型
 */
public interface IContentPostService {

    Long publish(ContentType type, PostPublishDTO dto);

    void update(Long id, PostUpdateDTO dto);

    void delete(Long id);

    PostDetailVO getDetail(Long id);

    PageResult<PostListVO> list(ContentType type, PostQueryDTO query);

    /** 点赞 */
    LikeResultVO like(Long id);

    /** 取消点赞 */
    LikeResultVO unlike(Long id);

    /**
     * 获取搜索索引文档（供 search 服务通过 OpenFeign 拉取写入 ES）
     * 仅返回已发布(status=1)且未删除的内容；其余返回 null。
     */
    PostSearchDocVO getSearchDoc(Long id);
}
