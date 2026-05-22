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

@Component
@RequiredArgsConstructor
public class AudienceEvaluationContextFetcher {

    private final ObjectMapper objectMapper;

    public Map<String, Object> fetch(WebClient client, String userId, String ruleJson) throws Exception {
        List<String> fields = extractFields(ruleJson);
        if (fields.isEmpty()) {
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
            return Map.of();
        }
        Map<String, Object> result = new HashMap<>();
        tagMap.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private List<String> extractFields(String ruleJson) throws Exception {
        Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {});
        List<String> fields = new ArrayList<>();
        collectFields(rule, fields);
        return fields.stream().distinct().toList();
    }

    @SuppressWarnings("unchecked")
    private void collectFields(Map<String, Object> group, List<String> fields) {
        Object conditionsObj = group.get("conditions");
        if (conditionsObj instanceof List<?> conditions) {
            for (Object item : conditions) {
                if (item instanceof Map<?, ?> condition && condition.get("field") != null) {
                    fields.add(String.valueOf(condition.get("field")));
                }
            }
        }
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
