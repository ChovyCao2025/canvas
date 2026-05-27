package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CustomerProfileDO;
import org.chovy.canvas.dal.mapper.CustomerProfileMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 画像更新节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.UPDATE_PROFILE)
public class UpdateProfileHandler implements NodeHandler {
    private final CustomerProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    public UpdateProfileHandler(CustomerProfileMapper profileMapper, ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        CustomerProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CustomerProfileDO>()
                .eq(CustomerProfileDO::getUserId, ctx.getUserId())
                .last("LIMIT 1"));
        boolean created = false;
        if (profile == null) {
            profile = new CustomerProfileDO();
            profile.setUserId(ctx.getUserId());
            profile.setAttributes("{}");
            profile.setCreatedAt(LocalDateTime.now());
            created = true;
        }

        Map<String, Object> attributes = readAttributes(profile.getAttributes());
        List<Map<String, Object>> operations = (List<Map<String, Object>>) config.getOrDefault("operations", List.of());
        for (Map<String, Object> op : operations) {
            apply(profile, attributes, op, ctx);
        }
        profile.setAttributes(toJson(attributes));
        profile.setUpdatedAt(LocalDateTime.now());
        if (created) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.PROFILE_UPDATED, true)));
    }

    private void apply(CustomerProfileDO profile, Map<String, Object> attributes, Map<String, Object> op, ExecutionContext ctx) {
        String field = string(op, "field", null);
        if (field == null || field.isBlank()) return;
        Object value = resolve(op.get("value"), ctx);
        String operator = string(op, "operator", "SET");

        if ("timezone".equals(field)) {
            profile.setTimezone(String.valueOf(value));
            return;
        }
        if ("region".equals(field)) {
            profile.setRegion(String.valueOf(value));
            return;
        }
        if ("lifecycleStage".equals(field) || "lifecycle_stage".equals(field)) {
            profile.setLifecycleStage(String.valueOf(value));
            return;
        }

        switch (operator) {
            case "SET_IF_NULL" -> attributes.putIfAbsent(field, value);
            case "INCREMENT" -> attributes.put(field, number(attributes.get(field)) + number(value));
            case "DECREMENT" -> attributes.put(field, number(attributes.get(field)) - number(value));
            case "CLEAR" -> attributes.remove(field);
            default -> attributes.put(field, value);
        }
    }

    private Object resolve(Object value, ExecutionContext ctx) {
        if (value instanceof String text && text.startsWith("$")) {
            Object resolved = ctx.getContextValue(text.substring(1));
            return resolved == null ? value : resolved;
        }
        return value;
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private Map<String, Object> readAttributes(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("用户属性序列化失败", e);
        }
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
