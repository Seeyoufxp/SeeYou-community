package com.seeyou.search.pojo.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内容事件消息体
 * 与 content 服务的 com.seeyou.content.pojo.event.ContentEventMsg 结构一致。
 * search 服务通过 Spring Cloud Stream 消费 content-event-topic，反序列化为本类型。
 * 字段名必须保持一致（id / action），跨服务通过 JSON 传输，不强依赖 content 模块。
 */
@Data
@NoArgsConstructor
public class ContentEventMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内容ID */
    private Long id;

    /** 操作类型：CREATE / UPDATE / DELETE */
    private String action;
}
