package com.seeyou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seeyou.content.pojo.entity.ContentPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IContentPostMapper extends BaseMapper<ContentPost> {

    /**
     * 点赞数原子 +1
     */
    @Update("UPDATE content_post SET like_count = like_count + 1 WHERE id = #{id}")
    int incrLikeCount(@Param("id") Long id);

    /**
     * 点赞数原子 -1，避免减到负数
     */
    @Update("UPDATE content_post SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrLikeCount(@Param("id") Long id);
}
