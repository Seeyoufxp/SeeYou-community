package com.seeyou.ai.pojo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内容事件消息体
 * 与 content 服务的 ContentEventMsg 字段对齐（id, action）。
 * AI 模块独立定义，不依赖 content 模块代码，保持服务解耦。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentEventMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";

    /** 内容ID */
    private Long id;

    /** 操作类型：CREATE / UPDATE / DELETE */
    private String action;
}
