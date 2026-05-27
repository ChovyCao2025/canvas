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
    /** 客户画像访问器，用于读取和更新用户画像。 */
    private final CustomerProfileMapper profileMapper;

    /** JSON 序列化器，用于读写画像动态属性。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造 UpdateProfileHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param profileMapper profileMapper 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     */
    public UpdateProfileHandler(CustomerProfileMapper profileMapper, ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        CustomerProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CustomerProfileDO>()
                .eq(CustomerProfileDO::getUserId, ctx.getUserId())
                .last("LIMIT 1"));
        boolean created = false;
        if (profile == null) {
            // 用户画像不存在时先创建空 attributes，后续统一走属性变更逻辑。
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

    /**
     * 执行 apply 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param profile profile 方法执行所需的业务参数
     * @param attributes attributes 方法执行所需的业务参数
     * @param op op 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     */
    private void apply(CustomerProfileDO profile, Map<String, Object> attributes, Map<String, Object> op, ExecutionContext ctx) {
        String field = string(op, "field", null);
        if (field == null || field.isBlank()) return;
        Object value = resolve(op.get("value"), ctx);
        String operator = string(op, "operator", "SET");

        if ("timezone".equals(field)) {
            // 常用标准字段写入实体列，其他动态属性写入 attributes JSON。
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

        // 动态属性支持增减、清空和仅空值写入，便于运营配置画像累积逻辑。
        switch (operator) {
            case "SET_IF_NULL" -> attributes.putIfAbsent(field, value);
            case "INCREMENT" -> attributes.put(field, number(attributes.get(field)) + number(value));
            case "DECREMENT" -> attributes.put(field, number(attributes.get(field)) - number(value));
            case "CLEAR" -> attributes.remove(field);
            default -> attributes.put(field, value);
        }
    }

    /**
     * 构建、解析或转换 resolve 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private Object resolve(Object value, ExecutionContext ctx) {
        if (value instanceof String text && text.startsWith("$")) {
            Object resolved = ctx.getContextValue(text.substring(1));
            return resolved == null ? value : resolved;
        }
        return value;
    }

    /**
     * 执行 number 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 计算得到的数值结果
     */
    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * 查询或读取 read Attributes 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param json json 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> readAttributes(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            // 历史脏 JSON 不阻断画像更新，按空属性继续覆盖写回。
            return new LinkedHashMap<>();
        }
    }

    /**
     * 构建、解析或转换 to Json 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("用户属性序列化失败", e);
        }
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
