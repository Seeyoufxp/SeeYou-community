package com.seeyou.ai.controller;

import com.seeyou.ai.pojo.dto.AssistantQueryDTO;
import com.seeyou.ai.pojo.vo.AssistantAnswerVO;
import com.seeyou.ai.pojo.vo.WelcomeVO;
import com.seeyou.ai.service.AssistantService;
import com.seeyou.ai.service.KnowledgeService;
import com.seeyou.ai.service.WelcomeService;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.R;
import com.seeyou.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 微服务对外接口
 * - GET  /api/ai/welcome           欢迎语（网关白名单已放开，未登录可访问）
 * - POST /api/ai/assistant         AI开发助手 RAG 问答（需登录）
 * - POST /api/ai/knowledge/rebuild 重建向量知识库（需管理员）
 */
@Tag(name = "AI服务")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final WelcomeService welcomeService;
    private final AssistantService assistantService;
    private final KnowledgeService knowledgeService;

    @Operation(summary = "获取当日欢迎语")
    @GetMapping("/welcome")
    public R<WelcomeVO> welcome() {
        return R.ok(welcomeService.getWelcome());
    }

    @Operation(summary = "AI开发助手问答（基于知识中心RAG）")
    @PostMapping("/assistant")
    public R<AssistantAnswerVO> assistant(@Valid @RequestBody AssistantQueryDTO dto) {
        return R.ok(assistantService.ask(dto.getQuestion()));
    }

    @Operation(summary = "重建向量知识库（管理员）")
    @PostMapping("/knowledge/rebuild")
    public R<Integer> rebuild() {
        Integer role = UserContext.getRole();
        if (role == null || role < 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "仅管理员可重建知识库");
        }
        int count = knowledgeService.rebuild();
        return R.ok(count);
    }
}
