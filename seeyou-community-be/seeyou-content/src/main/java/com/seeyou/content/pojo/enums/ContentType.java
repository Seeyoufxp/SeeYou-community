package com.seeyou.content.pojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容类型枚举
 * 对应 content_post.type 字段
 */
@Getter
@AllArgsConstructor
public enum ContentType {

    POST(1, "社交帖子"),
    BLOG(2, "技术博客"),
    QA(3, "问答");

    private final int code;
    private final String desc;

    public static boolean isValid(Integer type) {
        if (type == null) {
            return false;
        }
        for (ContentType t : values()) {
            if (t.code == type) {
                return true;
            }
        }
        return false;
    }

    public static ContentType of(Integer type) {
        if (type == null) {
            return null;
        }
        for (ContentType t : values()) {
            if (t.code == type) {
                return t;
            }
        }
        return null;
    }
}
