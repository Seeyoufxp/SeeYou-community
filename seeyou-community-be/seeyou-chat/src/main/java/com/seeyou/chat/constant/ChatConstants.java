package com.seeyou.chat.constant;

/**
 * 聊天模块常量
 */
public final class ChatConstants {

    private ChatConstants() {}

    /** WebSocket 路径前缀（网关路由 /api/chat/** 转发到本服务） */
    public static final String WS_PATH_PREFIX = "/api/chat/ws";

    /** RocketMQ 聊天消息广播 topic（与 application.yml 的 destination 一致） */
    public static final String CHAT_MESSAGE_TOPIC = "chat-message-topic";

    /** StreamBridge 发送 binding 名 */
    public static final String CHAT_MESSAGE_OUT_BINDING = "chatMessage-out-0";

    /** Redis 在线用户集合 key 前缀：chat:online:{roomId} -> Set<userId> */
    public static final String REDIS_ONLINE_KEY_PREFIX = "chat:online:";

    /** 聊天室创建/改名分布式锁 key 前缀：lock:chat:room:{roomId} */
    public static final String LOCK_ROOM_KEY_PREFIX = "lock:chat:room:";

    /** 聊天室状态：正常 */
    public static final int ROOM_STATUS_NORMAL = 1;
    /** 聊天室状态：已解散/封禁 */
    public static final int ROOM_STATUS_DISSOLVED = 0;

    /** 单条消息内容最大长度 */
    public static final int MESSAGE_MAX_LENGTH = 1024;
    /** 聊天室名称最大长度 */
    public static final int ROOM_NAME_MAX_LENGTH = 100;
    /** 聊天室公告最大长度 */
    public static final int ROOM_DESC_MAX_LENGTH = 255;

    /** 历史消息默认每页条数 */
    public static final int HISTORY_DEFAULT_SIZE = 20;
    /** 历史消息每页上限 */
    public static final int HISTORY_MAX_SIZE = 100;
}
