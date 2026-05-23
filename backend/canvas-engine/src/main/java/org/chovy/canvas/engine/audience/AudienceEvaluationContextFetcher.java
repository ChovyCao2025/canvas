package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人群规则评估上下文拉取器。
 *
 * <p>根据规则 JSON 自动提取所需字段，再向标签系统批量查询，减少无关字段拉取。
 * 该组件只负责“取数”，不负责规则求值。
 */
@Component
@RequiredArgsConstructor
public class AudienceEvaluationContextFetcher {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;

    /**
     * 拉取某用户的人群评估上下文。
     *
     * @param client   标签服务客户端
     * @param userId   用户标识
     * @param ruleJson 人群规则 JSON
     * @return 字段名到字段值的映射
     */
    public Map<String, Object> fetch(WebClient client, String userId, String ruleJson) throws Exception {
        List<String> fields = extractFields(ruleJson);
        if (fields.isEmpty()) {
            // 规则不依赖任何字段时，返回空上下文即可
            return Map.of();
        }
        Map<String, Object> response = client.post()
                .uri("/offline/user-tags/query")
                .bodyValue(Map.of("userId", userId, "tagCodes", fields))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        Object tags = response == null ? null : response.get("tags");
        if (!(tags instanceof Map<?, ?> tagMap)) {
            // 标签服务返回结构异常时降级为空上下文
            return Map.of();
        }
        Map<String, Object> result = new HashMap<>();
        tagMap.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    /** 从规则 JSON 提取所有字段名（去重后返回）。 */
    private List<String> extractFields(String ruleJson) throws Exception {
        Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {});
        List<String> fields = new ArrayList<>();
        collectFields(rule, fields);
        return fields.stream().distinct().toList();
    }

    @SuppressWarnings("unchecked")
    private void collectFields(Map<String, Object> group, List<String> fields) {
        // 当前层条件字段
        Object conditionsObj = group.get("conditions");
        if (conditionsObj instanceof List<?> conditions) {
            for (Object item : conditions) {
                if (item instanceof Map<?, ?> condition && condition.get("field") != null) {
                    fields.add(String.valueOf(condition.get("field")));
                }
            }
        }
        // 递归子分组字段
        Object groupsObj = group.get("groups");
        if (groupsObj instanceof List<?> groups) {
            for (Object item : groups) {
                if (item instanceof Map<?, ?> nested) {
                    collectFields((Map<String, Object>) nested, fields);
                }
            }
        }
    }
}
