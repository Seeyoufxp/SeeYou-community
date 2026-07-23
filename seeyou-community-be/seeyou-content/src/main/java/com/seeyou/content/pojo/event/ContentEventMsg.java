package com.seeyou.content.pojo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内容事件消息体
 * content 通过 RocketMQ(contentEvent-out-0 → content-event-topic) 通知 search 服务同步 ES。
 * 开发文档约定"仅传id"，这里额外带 action 让 search 区分增/改/删。
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

    public static ContentEventMsg of(Long id, String action) {
        return new ContentEventMsg(id, action);
    }
}
