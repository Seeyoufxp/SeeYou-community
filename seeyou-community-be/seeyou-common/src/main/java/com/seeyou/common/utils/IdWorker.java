package com.seeyou.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 雪花算法 ID 生成器
 * chat_message 与 user_info 的 ID 使用
 */
public class IdWorker {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private IdWorker() {
    }

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }
}
