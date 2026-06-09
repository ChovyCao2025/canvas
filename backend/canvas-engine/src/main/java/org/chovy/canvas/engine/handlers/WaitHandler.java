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
     * 构造等待节点处理器，使用系统默认时钟计算等待恢复时间。
     *
     * @param waitSubscriptionService 等待订阅服务，负责持久化事件等待和定时等待
     * @param objectMapper JSON 序列化器，用于保存恢复载荷
     */
    @Autowired
    public WaitHandler(WaitSubscriptionService waitSubscriptionService, ObjectMapper objectMapper) {
        this(waitSubscriptionService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 构造等待节点处理器，允许测试传入固定时钟以验证恢复时间。
     *
     * @param waitSubscriptionService 等待订阅服务，负责持久化事件等待和定时等待
     * @param objectMapper JSON 序列化器，用于保存恢复载荷
     * @param clock 恢复时间计算使用的时钟
     */
    WaitHandler(WaitSubscriptionService waitSubscriptionService, ObjectMapper objectMapper, Clock clock) {
        this.waitSubscriptionService = waitSubscriptionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 执行等待节点：首次进入时创建等待订阅并挂起，恢复进入时按恢复状态继续或超时路由。
     *
     * <p>首次进入会根据 waitType 写入事件等待或时间等待订阅，这是本节点的外部副作用；方法本身不声明事务，
     * 持久化边界由 {@link WaitSubscriptionService} 负责。恢复时由调度器或事件消费链路注入
     * {@code waitResumeStatus}：completed 走成功下一跳，timeout/expired 走超时分支。</p>
     *
     * @param config 节点配置，包含 waitType、duration、untilDate、timeWindow、eventCode、nextNodeId 和 timeoutNodeId
     * @param ctx 执行上下文，提供 executionId、canvasId、versionId、userId 等等待订阅所需标识
     * @return 节点结果；首次进入通常返回 pending，恢复进入返回成功或超时结果
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
     * 处理固定时长等待，计算当前时间加 duration 后的恢复时间并登记定时等待。
     */
    private NodeResult waitForDuration(Map<String, Object> config, ExecutionContext ctx) {
        Duration duration = durationFrom(config.get(MapFieldKeys.DURATION), config);
        if (duration.isZero() || duration.isNegative()) {
            return NodeResult.fail("WAIT DURATION 的等待时长必须大于 0");
        }
        return pendingAt(config, ctx, "DURATION", now().plus(duration));
    }

    /**
     * 处理指定日期时间等待；目标时间已过时直接放行，未来时间登记定时等待。
     */
    private NodeResult waitUntilDate(Map<String, Object> config, ExecutionContext ctx) {
        LocalDateTime until;
        try {
            until = parseDateTime(string(config, MapFieldKeys.UNTIL_DATE, null));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
     * 处理每天固定时刻等待；当天时间已过则顺延到下一天同一时刻。
     */
    private NodeResult waitUntilRelativeTime(Map<String, Object> config, ExecutionContext ctx) {
        LocalTime targetTime;
        try {
            targetTime = parseTime(string(config, MapFieldKeys.TIME, null), LocalTime.of(9, 0));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
     * 处理可执行时间窗口等待；当前位于窗口内则立即继续，否则登记到下一个窗口开始时间恢复。
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
     * 处理事件等待，校验事件编码和最大等待时间后创建事件订阅并返回 pending。
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
     * 构造等待完成后的成功结果，优先使用 nextNodeId，兼容旧配置中的 successNodeId。
     */
    private NodeResult success(Map<String, Object> config) {
        String nextNodeId = string(config, MapFieldKeys.NEXT_NODE_ID,
                string(config, MapFieldKeys.SUCCESS_NODE_ID, null));
        return NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.WAIT_STATUS, MapFieldKeys.COMPLETED));
    }

    /**
     * 创建时间等待订阅并返回挂起结果，恢复载荷只保留恢复所需的节点和分支信息。
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
     * 构造统一的 WAIT_PENDING 结果，将本地时间转换为引擎使用的 epoch 毫秒。
     */
    private NodeResult pending(LocalDateTime resumeAt) {
        return NodeResult.pending(
                resumeAt.atZone(zone()).toInstant().toEpochMilli(),
                "WAIT_PENDING",
                "等待节点挂起"
        );
    }

    /**
     * 构造等待恢复载荷，避免把完整节点配置写入订阅表，只保留源节点和成功/超时分支。
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
     * 解析等待类型，兼容 waitType 与 wait_type 两种配置名并统一为大写。
     */
    private String waitType(Map<String, Object> config) {
        // 同时兼容 waitType 与 wait_type 两种配置名。
        return string(config, MapFieldKeys.WAIT_TYPE, string(config, MapFieldKeys.WAIT_TYPE_SNAKE, DEFAULT_WAIT_TYPE)).toUpperCase();
    }

    /**
     * 从数字、对象或旧版平铺字段中解析等待时长。
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
     * 根据数值和单位构造等待时长。
     *
     * @param amount 时长数值
     * @param unit 时长单位
     * @return Duration 时长
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
     * 判断当前时间是否落在业务时间窗口内。
     *
     * @param current 当前时间
     * @param start 窗口开始时间
     * @param end 窗口结束时间
     * @return true 表示当前时间在窗口内
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
     * 解析本地时间，空值时返回默认值。
     *
     * @param value 原始时间字符串
     * @param fallback 默认时间
     * @return 解析后的 LocalTime
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("WAIT 配置序列化失败", e);
        }
    }

    /**
     * 读取字符串配置。
     *
     * @param config 节点配置
     * @param key 配置 key
     * @param fallback 默认值
     * @return 字符串值或默认值
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }

    /**
     * 读取当前时钟时间。
     *
     * @return 当前 LocalDateTime
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 读取当前时钟时区。
     *
     * @return 当前 ZoneId
     */
    private ZoneId zone() {
        return clock.getZone();
    }
}
