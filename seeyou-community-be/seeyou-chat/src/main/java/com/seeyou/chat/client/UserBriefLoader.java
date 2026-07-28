package com.seeyou.chat.client;

import com.seeyou.chat.pojo.dto.UserBriefDTO;
import com.seeyou.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户概要加载器：Feign 调用 user 模块的降级包装。
 * 任意异常均降级为空 Map，避免 user 服务故障导致 chat 接口整体失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBriefLoader {

    private final UserClient userClient;

    /** 批量拉取用户概要并组装成 Map<userId, UserBriefDTO>，失败降级返回空 Map */
    public Map<Long, UserBriefDTO> loadByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            R<List<UserBriefDTO>> resp = userClient.listByIds(userIds);
            if (resp == null || resp.getData() == null) {
                return Collections.emptyMap();
            }
            return resp.getData().stream()
                    .collect(Collectors.toMap(UserBriefDTO::getId, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量拉取用户概要失败，降级返回空 Map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 单个用户概要，失败降级返回 null */
    public UserBriefDTO loadById(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            R<UserBriefDTO> resp = userClient.getById(userId);
            return resp == null ? null : resp.getData();
        } catch (Exception e) {
            log.warn("拉取用户概要失败，降级返回 null: userId={}, msg={}", userId, e.getMessage());
            return null;
        }
    }
}
