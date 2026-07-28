package com.seeyou.chat.controller;

import com.seeyou.chat.pojo.dto.ChatMessageQueryDTO;
import com.seeyou.chat.pojo.dto.ChatMessageSendDTO;
import com.seeyou.chat.pojo.vo.ChatMessageVO;
import com.seeyou.chat.service.IChatMessageService;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天消息接口
 */
@Tag(name = "聊天消息")
@RestController
@RequestMapping("/api/chat/message")
@RequiredArgsConstructor
public class ChatMessageController {

    private final IChatMessageService chatMessageService;

    @Operation(summary = "发送聊天消息（HTTP 提交，落库后 MQ 广播）")
    @PostMapping("/send")
    public R<ChatMessageVO> send(@Valid @RequestBody ChatMessageSendDTO dto) {
        return R.ok(chatMessageService.send(dto));
    }

    @Operation(summary = "聊天历史消息（分页，倒序）")
    @GetMapping("/history")
    public R<PageResult<ChatMessageVO>> history(@Valid @ModelAttribute ChatMessageQueryDTO query) {
        return R.ok(chatMessageService.history(query));
    }
}
