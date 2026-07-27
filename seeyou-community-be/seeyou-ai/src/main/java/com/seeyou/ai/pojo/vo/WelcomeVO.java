package com.seeyou.ai.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 欢迎信息返回体
 */
@Data
@Schema(description = "欢迎信息")
public class WelcomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "欢迎语")
    private String message;

    @Schema(description = "日期，格式 yyyy-MM-dd")
    private String greetingDate;

    public WelcomeVO() {
    }

    public WelcomeVO(String message, String greetingDate) {
        this.message = message;
        this.greetingDate = greetingDate;
    }
}
