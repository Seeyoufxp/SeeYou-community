package com.seeyou.search.pojo.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 内容搜索 ES 文档
 * 索引名 seeyou_content；标题/摘要/正文用 ik_max_word 分词索引、ik_smart 检索（需 ES 装 ik 分词器）。
 * createIndex=false：不在启动时自动建索引（自动建会因 ik 未装直接抛错让服务起不来），
 * 由 ElasticsearchIndexInitializer 显式建索引，失败仅记日志，服务仍可启动。
 *
 * <p>注意：createTime/updateTime 用 String 而非 LocalDateTime——
 * Spring Data ES 5.2 的 TemporalPropertyValueConverter 写 LocalDateTime 时会输出纯日期字符串
 * （如 "2026-07-24"），但读回的 LocalDateTime 反序列化器只接受完整 ISO 8601，导致整个查询报
 * ConversionException 被 catch 吞掉、搜索结果为空。改为 String 后由我们自己用 ISO 8601 格式
 * 序列化，彻底绕开这个 5.2 的兼容性问题。
 */
@Data
@Document(indexName = "seeyou_content", createIndex = false)
public class ContentDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    /** 1:社交帖子 2:技术博客 3:问答 */
    @Field(type = FieldType.Integer)
    private Integer type;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String summary;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String nickname;

    /** 头像 URL 仅存储不索引，按关键词过滤不到也无意义 */
    @Field(type = FieldType.Keyword, index = false)
    private String avatarUrl;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    private Integer commentCount;

    @Field(type = FieldType.Integer)
    private Integer status;

    /** 写入格式 yyyy-MM-dd'T'HH:mm:ss.SSS（无时区，LocalDateTime）；ES 存为 date 类型，sort 时按时间序生效 */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private String createTime;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private String updateTime;
}
