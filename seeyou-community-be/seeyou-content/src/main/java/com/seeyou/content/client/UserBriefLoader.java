package com.seeyou.content.client;

import com.seeyou.common.result.R;
import com.seeyou.content.pojo.dto.UserBriefDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 用户概要批量加载器
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBriefLoader {

    private final UserClient userClient;

    /**
     * 按 userId 集合批量拉取用户概要并组装成 Map。
     * 任意异常（网络超时、user 服务不可用、反序列化失败）均降级为空 Map。
     */
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
}