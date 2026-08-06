package com.seeyou.chat.controller;

import com.seeyou.chat.pojo.dto.ChatRoomCreateDTO;
import com.seeyou.chat.pojo.dto.ChatRoomUpdateDTO;
import com.seeyou.chat.pojo.vo.ChatRoomVO;
import com.seeyou.chat.pojo.vo.OnlineMemberVO;
import com.seeyou.chat.service.IChatRoomService;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天室管理接口
 */
@Tag(name = "聊天室")
@RestController
@RequestMapping("/api/chat/room")
@RequiredArgsConstructor
public class ChatRoomController {

    private final IChatRoomService chatRoomService;

    @Operation(summary = "创建聊天室（管理员）")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ChatRoomCreateDTO dto) {
        return R.ok(chatRoomService.create(dto));
    }

    @Operation(summary = "修改聊天室（创建者或管理员）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ChatRoomUpdateDTO dto) {
        chatRoomService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "解散聊天室（创建者或管理员）")
    @DeleteMapping("/{id}")
    public R<Void> dissolve(@PathVariable Long id) {
        chatRoomService.dissolve(id);
        return R.ok();
    }

    @Operation(summary = "聊天室列表（分页）")
    @GetMapping("/list")
    public R<PageResult<ChatRoomVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return R.ok(chatRoomService.list(current, size));
    }

    @Operation(summary = "聊天室详情")
    @GetMapping("/{id}")
    public R<ChatRoomVO> detail(@PathVariable Long id) {
        return R.ok(chatRoomService.getDetail(id));
    }

    @Operation(summary = "聊天室在线人数")
    @GetMapping("/{id}/online/count")
    public R<Long> onlineCount(@PathVariable Long id) {
        return R.ok(chatRoomService.getOnlineCount(id));
    }

    @Operation(summary = "聊天室在线成员列表")
    @GetMapping("/{id}/online/members")
    public R<List<OnlineMemberVO>> onlineMembers(@PathVariable Long id) {
        return R.ok(chatRoomService.getOnlineMembers(id));
    }
}
