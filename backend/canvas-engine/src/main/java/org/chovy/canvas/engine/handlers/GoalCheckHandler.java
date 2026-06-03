package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 目标达成检查节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.GOAL_CHECK)
public class GoalCheckHandler implements NodeHandler {

    /** 事件日志访问器，用于同步检查用户目标事件是否已发生。 */
    private final EventLogMapper eventLogMapper;

    /** 等待订阅服务，用于异步目标达成监听和超时恢复。 */
    private final WaitSubscriptionService waitSubscriptionService;

    /** JSON 序列化器，用于持久化目标等待恢复载荷。 */
    private final ObjectMapper objectMapper;

    /** 时钟依赖，用于计算异步目标等待的过期时间。 */
    private final Clock clock;

    /**
     * 构造 GoalCheckHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param eventLogMapper eventLogMapper 方法执行所需的业务参数
     * @param waitSubscriptionService waitSubscriptionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     */
    @Autowired
    public GoalCheckHandler(
            EventLogMapper eventLogMapper,
            WaitSubscriptionService waitSubscriptionService,
            ObjectMapper objectMapper
    ) {
        this(eventLogMapper, waitSubscriptionService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 构造 GoalCheckHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param eventLogMapper eventLogMapper 方法执行所需的业务参数
     * @param waitSubscriptionService waitSubscriptionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param clock clock 方法执行所需的业务参数
     */
    GoalCheckHandler(
            EventLogMapper eventLogMapper,
            WaitSubscriptionService waitSubscriptionService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.eventLogMapper = eventLogMapper;
        this.waitSubscriptionService = waitSubscriptionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
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
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String resumeStatus = string(config, MapFieldKeys.GOAL_RESUME_STATUS, "");
        if ("TIMEOUT".equalsIgnoreCase(resumeStatus) || "EXPIRED".equalsIgnoreCase(resumeStatus)) {
            // 异步目标等待超时恢复时，走 TIMEOUT 结果并交给调度层处理超时分支。
            return Mono.just(NodeResult.timeout(
                    string(config, "timeoutNodeId", null),
                    "GOAL_TIMEOUT",
                    "目标检测等待超时"
            ));
        }
        if ("COMPLETED".equalsIgnoreCase(resumeStatus)) {
            // 事件监听已确认目标达成，恢复后直接走 goal_met 分支。
            return Mono.just(goalMet(config));
        }

        String mode = string(config, "mode", "SYNC").toUpperCase();
        if ("ASYNC".equals(mode)) {
            // ASYNC 模式登记目标事件监听，先挂起当前流程。
            return Mono.fromCallable(() -> asyncWait(config, ctx))
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(e -> Mono.just(NodeResult.fail("GOAL_CHECK: " + e.getMessage())));
        }
        return Mono.fromCallable(() -> syncCheck(config, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(NodeResult.fail("GOAL_CHECK: " + e.getMessage())));
    }

    /**
     * 执行 sync Check 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private NodeResult syncCheck(Map<String, Object> config, ExecutionContext ctx) {
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return NodeResult.fail("GOAL_CHECK 必须配置 eventCode");
        }

        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<EventLogDO>()
                .eq(EventLogDO::getEventCode, eventCode)
                .eq(EventLogDO::getUserId, ctx.getUserId()));
        // 同步模式只看历史事件是否已发生，不创建等待订阅。
        boolean met = count != null && count > 0;
        return met ? goalMet(config) : goalNotMet(config);
    }

    /**
     * 执行 async Wait 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private NodeResult asyncWait(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", null);
        if (nodeId == null || nodeId.isBlank()) {
            return NodeResult.fail("GOAL_CHECK 节点未注入 __nodeId");
        }
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return NodeResult.fail("GOAL_CHECK 必须配置 eventCode");
        }
        if (!config.containsKey("maxWait")) {
            return NodeResult.fail("GOAL_CHECK 异步监听必须配置 maxWait");
        }
        Duration maxWait = durationFrom(config.get("maxWait"));
        if (maxWait.isZero() || maxWait.isNegative()) {
            return NodeResult.fail("GOAL_CHECK 的 maxWait 必须大于 0");
        }

        LocalDateTime expiresAt = now().plus(maxWait);
        // 目标事件订阅写入等待表，事件到达后由恢复载荷带回目标分支配置。
        waitSubscriptionService.createGoalWait(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                eventCode,
                toJson(resumePayload(nodeId, config)),
                expiresAt
        );
        return NodeResult.pending(
                expiresAt.atZone(zone()).toInstant().toEpochMilli(),
                "GOAL_PENDING",
                "目标检测等待事件"
        );
    }

    /**
     * 执行 goal Met 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 方法执行后的业务结果
     */
    private NodeResult goalMet(Map<String, Object> config) {
        return NodeResult.routed("goal_met", string(config, "goalMetNodeId", null), Map.of(MapFieldKeys.GOAL_MET, true));
    }

    /**
     * 执行 goal Not Met 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 方法执行后的业务结果
     */
    private NodeResult goalNotMet(Map<String, Object> config) {
        return NodeResult.routed("goal_not_met", string(config, "goalNotMetNodeId", null), Map.of(MapFieldKeys.GOAL_MET, false));
    }

    /**
     * 执行 resume Payload 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> resumePayload(String nodeId, Map<String, Object> config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MapFieldKeys.SOURCE_NODE_ID, nodeId);
        // 恢复载荷保存三类出口，事件命中和超时恢复都依赖这些节点 ID。
        payload.put(MapFieldKeys.GOAL_MET_NODE_ID, string(config, "goalMetNodeId", null));
        payload.put(MapFieldKeys.GOAL_NOT_MET_NODE_ID, string(config, "goalNotMetNodeId", null));
        payload.put(MapFieldKeys.TIMEOUT_NODE_ID, string(config, "timeoutNodeId", null));
        return payload;
    }

    /**
     * 执行 duration From 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 方法执行后的业务结果
     */
    @SuppressWarnings("unchecked")
    private Duration durationFrom(Object value) {
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> duration = (Map<String, Object>) map;
            long amount = duration.get("value") instanceof Number number ? number.longValue() : 0L;
            String unit = string(duration, "unit", "SECONDS").toUpperCase();
            return switch (unit) {
                case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
                case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
                case "HOUR", "HOURS" -> Duration.ofHours(amount);
                case "DAY", "DAYS" -> Duration.ofDays(amount);
                default -> Duration.ofSeconds(amount);
            };
        }
        return Duration.ZERO;
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
            throw new IllegalArgumentException("GOAL_CHECK 配置序列化失败", e);
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

    /**
     * 执行 now 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 方法执行后的业务结果
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 执行 zone 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 方法执行后的业务结果
     */
    private ZoneId zone() {
        return clock.getZone();
    }
}
