package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.MapFieldKeys;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private final ConcurrentMap<String, List<String>> fieldCache = new ConcurrentHashMap<>();

    /** 使用同步 HTTP 客户端拉取某用户的人群评估上下文，供批计算虚拟线程任务使用。 */
    public Map<String, Object> fetch(RestClient client, String userId, String ruleJson) throws Exception {
        List<String> fields = extractFields(ruleJson);
        if (fields.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> response = client.post()
                .uri("/offline/user-tags/query")
                .body(Map.of(MapFieldKeys.USER_ID, userId, MapFieldKeys.TAG_CODES, fields))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return extractTagContext(response);
    }

    /** 以非阻塞方式拉取某用户的人群评估上下文。 */
    public Mono<Map<String, Object>> fetchAsync(WebClient client, String userId, String ruleJson) {
        return Mono.fromCallable(() -> extractFields(ruleJson))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(fields -> {
                    if (fields.isEmpty()) {
                        // 规则不依赖任何字段时，返回空上下文即可
                        return Mono.just(Map.<String, Object>of());
                    }
                    return client.post()
                            .uri("/offline/user-tags/query")
                            // 只把规则里出现过的字段传给 Tagger，降低单用户上下文拉取成本。
                            .bodyValue(Map.of(MapFieldKeys.USER_ID, userId, MapFieldKeys.TAG_CODES, fields))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .map(AudienceEvaluationContextFetcher::extractTagContext)
                            .defaultIfEmpty(Map.of());
                });
    }

    /** 从标签服务响应中提取规则求值上下文。 */
    private static Map<String, Object> extractTagContext(Map<String, Object> response) {
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
        String cacheKey = ruleJson == null ? "" : ruleJson;
        List<String> cached = fieldCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> rule = objectMapper.readValue(cacheKey, new TypeReference<>() {});
        List<String> fields = new ArrayList<>();
        collectFields(rule, fields);
        // 同一字段可能出现在多个条件或分组里，发起查询前去重。
        List<String> extracted = fields.stream().distinct().toList();
        // 规则字段集合只由 ruleJson 决定，缓存后可减少实时判断时的重复解析开销。
        fieldCache.put(cacheKey, extracted);
        return extracted;
    }

    /**
     * 执行 collect Fields 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param group group 方法执行所需的业务参数
     * @param fields fields 方法执行所需的业务参数
     */
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
