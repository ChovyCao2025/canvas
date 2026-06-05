package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 等待节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.WAIT)
public class WaitHandler implements NodeHandler {

    /** 未显式配置 waitType 时使用的默认等待类型。 */
    private static final String DEFAULT_WAIT_TYPE = "DURATION";

    /** 等待订阅服务，用于登记事件等待和定时恢复。 */
    private final WaitSubscriptionService waitSubscriptionService;

    /** JSON 序列化器，用于保存等待恢复载荷。 */
    private final ObjectMapper objectMapper;

    /** 时钟依赖，用于计算等待节点的恢复时间。 */
    private final Clock clock;

    /**
     * 构造 WaitHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param waitSubscriptionService waitSubscriptionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     */
    @Autowired
    public WaitHandler(WaitSubscriptionService waitSubscriptionService, ObjectMapper objectMapper) {
        this(waitSubscriptionService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 构造 WaitHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param waitSubscriptionService waitSubscriptionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param clock clock 方法执行所需的业务参数
     */
    WaitHandler(WaitSubscriptionService waitSubscriptionService, ObjectMapper objectMapper, Clock clock) {
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
        String resumeStatus = string(config, MapFieldKeys.WAIT_RESUME_STATUS, "");
        if (MapFieldKeys.TIMEOUT.equalsIgnoreCase(resumeStatus) || MapFieldKeys.EXPIRED.equalsIgnoreCase(resumeStatus)) {
            // 恢复执行时由订阅/看门狗注入状态，超时统一走 timeout 结果。
            return Mono.just(NodeResult.timeout(
                    string(config, MapFieldKeys.TIMEOUT_NODE_ID, null),
                    "WAIT_TIMEOUT",
                    "等待节点超时"
            ));
        }
        if (MapFieldKeys.COMPLETED.equalsIgnoreCase(resumeStatus)) {
            // 外部事件或定时器已完成等待，继续原 nextNodeId。
            return Mono.just(success(config));
        }

        String waitType = waitType(config);
        // 首次进入等待节点时，按 waitType 分派到定时等待或事件订阅。
        return switch (waitType) {
            case "UNTIL_EVENT" -> Mono.just(waitUntilEvent(config, ctx));
            case "UNTIL_DATE" -> Mono.just(waitUntilDate(config, ctx));
            case "RELATIVE_TIME" -> Mono.just(waitUntilRelativeTime(config, ctx));
            case "TIME_WINDOW" -> Mono.just(waitForTimeWindow(config, ctx));
            case "DURATION" -> Mono.just(waitForDuration(config, ctx));
            default -> Mono.just(NodeResult.fail("未知 WAIT 类型: " + waitType));
        };
    }

    /**
     * 执行 wait For Duration 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private NodeResult waitForDuration(Map<String, Object> config, ExecutionContext ctx) {
        Duration duration = durationFrom(config.get(MapFieldKeys.DURATION), config);
        if (duration.isZero() || duration.isNegative()) {
            return NodeResult.fail("WAIT DURATION 的等待时长必须大于 0");
        }
        return pendingAt(config, ctx, "DURATION", now().plus(duration));
    }

    /**
     * 执行 wait Until Date 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private NodeResult waitUntilDate(Map<String, Object> config, ExecutionContext ctx) {
        LocalDateTime until;
        try {
            until = parseDateTime(string(config, MapFieldKeys.UNTIL_DATE, null));
        } catch (DateTimeParseException e) {
            return NodeResult.fail("WAIT UNTIL_DATE 的 untilDate 格式不正确");
        }
        if (until == null || !until.isAfter(now())) {
            // 目标时间为空或已过期时不挂起，直接放行到下游。
            return success(config);
        }
        return pendingAt(config, ctx, "UNTIL_DATE", until);
    }

    /**
     * 执行 wait Until Relative Time 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private NodeResult waitUntilRelativeTime(Map<String, Object> config, ExecutionContext ctx) {
        LocalTime targetTime;
        try {
            targetTime = parseTime(string(config, MapFieldKeys.TIME, null), LocalTime.of(9, 0));
        } catch (DateTimeParseException e) {
            return NodeResult.fail("WAIT RELATIVE_TIME 的 time 格式不正确");
        }
        LocalDateTime candidate = LocalDateTime.of(LocalDate.now(clock), targetTime);
        if (!candidate.isAfter(now())) {
            candidate = candidate.plusDays(1);
        }
        return pendingAt(config, ctx, "RELATIVE_TIME", candidate);
    }

    /**
     * 执行 wait For Time Window 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    @SuppressWarnings("unchecked")
    private NodeResult waitForTimeWindow(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> window = config.get(MapFieldKeys.TIME_WINDOW) instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        LocalTime start;
        LocalTime end;
        try {
            start = parseTime(string(window, MapFieldKeys.START, string(config, MapFieldKeys.WINDOW_START, null)), LocalTime.of(9, 0));
            end = parseTime(string(window, MapFieldKeys.END, string(config, MapFieldKeys.WINDOW_END, null)), LocalTime.of(20, 0));
        } catch (DateTimeParseException e) {
            return NodeResult.fail("WAIT TIME_WINDOW 的时间窗口格式不正确");
        }
        LocalTime current = LocalTime.now(clock);

        if (insideWindow(current, start, end)) {
            // 当前已在可执行窗口内时无需登记定时唤醒。
            return success(config);
        }

        LocalDate date = LocalDate.now(clock);
        LocalDateTime nextStart = LocalDateTime.of(date, start);
        if (!nextStart.isAfter(now())) {
            nextStart = nextStart.plusDays(1);
        }
        return pendingAt(config, ctx, "TIME_WINDOW", nextStart);
    }

    /**
     * 执行 wait Until Event 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private NodeResult waitUntilEvent(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, MapFieldKeys.NODE_ID_INTERNAL, null);
        if (nodeId == null || nodeId.isBlank()) {
            return NodeResult.fail("WAIT 节点未注入 __nodeId");
        }
        String eventCode = string(config, MapFieldKeys.EVENT_CODE, null);
        if (eventCode == null || eventCode.isBlank()) {
            return NodeResult.fail("WAIT UNTIL_EVENT 必须配置 eventCode");
        }

        if (!config.containsKey(MapFieldKeys.MAX_WAIT)) {
            return NodeResult.fail("WAIT UNTIL_EVENT 必须配置 maxWait");
        }
        Duration maxWait = durationFrom(config.get(MapFieldKeys.MAX_WAIT), Map.of());
        if (maxWait.isZero() || maxWait.isNegative()) {
            return NodeResult.fail("WAIT UNTIL_EVENT 的 maxWait 必须大于 0");
        }

        LocalDateTime expiresAt = now().plus(maxWait);
        String eventFiltersJson = toJsonOrNull(config.get(MapFieldKeys.EVENT_FILTERS));
        String resumePayload = toJson(resumePayload(nodeId, config));

        // 写入事件等待订阅，后续由事件消费链路命中后恢复当前 execution。
        waitSubscriptionService.createEventWait(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                eventCode,
                eventFiltersJson,
                resumePayload,
                expiresAt
        );
        return pending(expiresAt);
    }

    /**
     * 执行 success 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 方法执行后的业务结果
     */
    private NodeResult success(Map<String, Object> config) {
        String nextNodeId = string(config, MapFieldKeys.NEXT_NODE_ID,
                string(config, MapFieldKeys.SUCCESS_NODE_ID, null));
        return NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.WAIT_STATUS, MapFieldKeys.COMPLETED));
    }

    /**
     * 执行 pending At 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param waitType waitType 类型标识或分类条件
     * @param resumeAt resumeAt 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private NodeResult pendingAt(
            Map<String, Object> config,
            ExecutionContext ctx,
            String waitType,
            LocalDateTime resumeAt
    ) {
        String nodeId = string(config, MapFieldKeys.NODE_ID_INTERNAL, null);
        if (nodeId == null || nodeId.isBlank()) {
            return NodeResult.fail("WAIT 节点未注入 __nodeId");
        }
        // 写入时间等待订阅，由调度器或看门狗到点恢复执行。
        waitSubscriptionService.createTimeWait(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                waitType,
                toJson(resumePayload(nodeId, config)),
                resumeAt
        );
        return pending(resumeAt);
    }

    /**
     * 执行 pending 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param resumeAt resumeAt 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private NodeResult pending(LocalDateTime resumeAt) {
        return NodeResult.pending(
                resumeAt.atZone(zone()).toInstant().toEpochMilli(),
                "WAIT_PENDING",
                "等待节点挂起"
        );
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
        // 恢复时只需要源节点、成功分支和超时分支，避免序列化完整 config。
        payload.put(MapFieldKeys.SUCCESS_NODE_ID, string(config, MapFieldKeys.NEXT_NODE_ID, null));
        payload.put(MapFieldKeys.TIMEOUT_NODE_ID, string(config, MapFieldKeys.TIMEOUT_NODE_ID, null));
        return payload;
    }

    /**
     * 执行 wait Type 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 转换或查询得到的字符串结果
     */
    private String waitType(Map<String, Object> config) {
        // 同时兼容 waitType 与 wait_type 两种配置名。
        return string(config, MapFieldKeys.WAIT_TYPE, string(config, MapFieldKeys.WAIT_TYPE_SNAKE, DEFAULT_WAIT_TYPE)).toUpperCase();
    }

    /**
     * 执行 duration From 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    @SuppressWarnings("unchecked")
    private Duration durationFrom(Object value, Map<String, Object> fallback) {
        if (value instanceof Number number) {
            return duration(number.longValue(), string(fallback, MapFieldKeys.UNIT, "SECONDS"));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> duration = (Map<String, Object>) map;
            long amount = duration.get(MapFieldKeys.VALUE) instanceof Number number ? number.longValue() : 0L;
            return duration(amount, string(duration, MapFieldKeys.UNIT, "SECONDS"));
        }
        long amount = fallback.get(MapFieldKeys.DURATION_VALUE) instanceof Number number ? number.longValue() : 0L;
        return duration(amount, string(fallback, MapFieldKeys.DURATION_UNIT, string(fallback, MapFieldKeys.UNIT, "SECONDS")));
    }

    /**
     * 执行 duration 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param amount amount 方法执行所需的业务参数
     * @param unit unit 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private Duration duration(long amount, String unit) {
        return switch (unit == null ? "SECONDS" : unit.toUpperCase()) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            case "DAY", "DAYS" -> Duration.ofDays(amount);
            default -> Duration.ofSeconds(amount);
        };
    }

    /**
     * 执行 inside Window 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param current current 方法执行所需的业务参数
     * @param start start 方法执行所需的业务参数
     * @param end end 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean insideWindow(LocalTime current, LocalTime start, LocalTime end) {
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        // 跨天窗口，如 22:00-08:00，满足晚于 start 或早于 end 即在窗口内。
        return !current.isBefore(start) || current.isBefore(end);
    }

    /**
     * 构建、解析或转换 parse Date Time 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 方法执行后的业务结果
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value);
    }

    /**
     * 构建、解析或转换 parse Time 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        return LocalTime.parse(value);
    }

    /**
     * 构建、解析或转换 to Json Or Null 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String toJsonOrNull(Object value) {
        return value == null ? null : toJson(value);
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
            throw new IllegalArgumentException("WAIT 配置序列化失败", e);
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
