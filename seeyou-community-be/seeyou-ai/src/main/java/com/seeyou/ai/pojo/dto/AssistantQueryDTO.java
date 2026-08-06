package com.seeyou.ai.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 开发助手提问请求
 */
@Data
@Schema(description = "AI助手提问")
public class AssistantQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户问题", example = "Spring Boot 如何配置多数据源？")
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字")
    private String question;
}
