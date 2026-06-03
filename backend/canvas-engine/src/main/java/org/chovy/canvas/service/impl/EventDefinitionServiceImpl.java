package org.chovy.canvas.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.chovy.canvas.perf.PerfRunContext;
import org.chovy.canvas.service.EventDefinitionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * 事件定义与事件上报服务实现。
 *
 * <p>负责校验事件、执行幂等去重、写入事件日志，并把行为事件路由到已发布画布和等待恢复流程。
 * <p>该类连接事件中心、Redis 路由、执行引擎和 WAIT 恢复服务，是行为触发链路的核心编排点。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EventDefinitionServiceImpl implements EventDefinitionService {

    /** 事件日志 Mapper，用于记录事件上报和画布触发数量。 */
    private final EventLogMapper logMapper;
    /** 画布执行服务，负责触发和恢复 DAG 执行。 */
    private final CanvasExecutionService canvasExecutionService;  // 取代 CanvasDisruptorService，内含溢出降级能力
    /** 触发路由服务，负责 Redis 路由注册、清理和查询。 */
    private final TriggerRouteService triggerRouteService;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** 等待恢复服务，用于唤醒因事件挂起的 WAIT 节点。 */
    private final WaitResumeService waitResumeService;
    /** 事件定义缓存服务。 */
    private final EventDefinitionCacheService eventDefinitionCacheService;
    /** 雪花 ID 生成器。 */
    private final Snowflake snowflake;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;
    /** Redis key 工具，集中生成执行相关 key。 */
    private final RedisKeyUtil redisKeys;

    /**
     * 事件上报主流程，分四步：
     *
     * <pre>
     * 1. validateReportReq     — 参数 + 事件定义校验（二级缓存，L1 Caffeine 10min / L2 Redis 1h）
     * 2. triggerAllCanvas      — 查 Redis 路由表获取订阅画布，逐个写 Disruptor RingBuffer（异步）
     * 3. writeEventLog         — 记录 event_log（canvasTriggered 已知，写入真实数量）
     * 4. resumeWaitsAndBuild   — 唤醒因该事件挂起的 WAIT 节点，并构建返回 Map
     * </pre>
     *
     * 步骤2是纯异步（Disruptor → CanvasExecutionService），步骤4的 wait 恢复是同步完成 DB 状态更新
     * 后 fire-and-forget 触发执行。两条触发路径对同一个用户是独立的，互不等待。
     */
    @Override
    public Map<String, Object> doReportEvent(EventReportReq req) {
        // 步骤1：校验参数非空 + 事件定义存在且已启用（走二级缓存，穿透时查 DB）
        validateReportReq(req);

        // 校验是否是重复上报
        if (isDuplicateEvent(req)) {
            // 幂等命中时不写 event_log，也不触发画布或 WAIT 恢复，保持事件处理一次性。
            return duplicateResponse(req);
        }
        String eventId = resolveEventId(req);

        Map<String, Object> payload = req.getAttributes() != null ? req.getAttributes() : Map.of();

        // 步骤2：查 Redis 路由表，向所有订阅画布发布触发（含 Disruptor 溢出降级逻辑）
        Set<String> canvasIds = triggerAllCanvas(req, payload, eventId);

        // 步骤3：写 event_log
        EventLogDO eventLog = writeEventLog(req, payload, canvasIds);

        // 步骤4：恢复 WAIT 节点，并组装返回 Map
        return resumeWaitsAndBuildResult(req, payload, eventId, eventLog, canvasIds);
    }

    /**
     * 恢复因本次事件挂起的等待节点，并组装返回 Map。
     *
     * <p>WAIT 恢复路径：
     * <ol>
     *   <li>查 DB 找 status=ACTIVE 且 eventCode + userId 匹配的订阅记录（含过期时间过滤）</li>
     *   <li>乐观锁更新 DB：ACTIVE → COMPLETED（CAS 防并发双触）</li>
     *   <li>fire-and-forget 调用 CanvasExecutionService#trigger，恢复画布执行</li>
     * </ol>
     *
     * <p>⚠️ 已知问题（见 BUG-1/BUG-2 注释）：
     * WAIT 恢复触发走全量 triggerInternal 路径，会经过冷却期检查和配额扣减，
     * 在配置了 cooldownSeconds 的画布上可能导致恢复被拒绝或配额被重复消耗。
     */
    private Map<String, Object> resumeWaitsAndBuildResult(
            EventReportReq req, Map<String, Object> payload,
            String eventId, EventLogDO eventLog, Set<String> canvasIds) {
        // 查询并恢复挂起的 WAIT 节点，返回实际恢复数量
        int waitsResumed = waitResumeService.resumeEventWaits(
                req.getEventCode(),
                req.getUserId(),
                payload,
                eventId
        );

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put(MapFieldKeys.EVENT_LOG_ID, eventLog.getId());
        resp.put(MapFieldKeys.EVENT_CODE, req.getEventCode());
        resp.put(MapFieldKeys.USER_ID, req.getUserId());
        resp.put(MapFieldKeys.CANVAS_TRIGGERED, canvasIds.size());
        resp.put(MapFieldKeys.WAITS_RESUMED, waitsResumed);
        resp.put(MapFieldKeys.STATUS, "ACCEPTED");
        return resp;
    }

    /**
     * 查路由表，向所有订阅该 eventCode 的画布发布触发事件（含溢出降级，Fix 2）。
     *
     * <p>路由表存储在 Redis Set（key: canvas:trigger:behavior:{eventCode}），
     * 画布发布时由 TriggerRouteService#registerBehavior 写入，无本地缓存。
     *
     * <p>溢出行为（Disruptor RingBuffer 满时）：
     * 之前：直接丢弃 + WARN 日志。
     * 现在：降级写入 RocketMQ 溢出重试队列，与 MQ 触发保持对称。
     */
    private Set<String> triggerAllCanvas(EventReportReq req, Map<String, Object> payload, String eventId) {
        Set<String> canvasIds = triggerRouteService.getCanvasByBehavior(req.getEventCode());
        canvasIds.forEach(cidStr -> {
            try {
                Long cid = Long.parseLong(cidStr);
                // msgId = eventId + canvasId，保证同一事件对不同画布的 dedup key 不冲突
                String msgId = eventId + "-" + cidStr;
                // 写入 Disruptor；RingBuffer 满时自动降级 MQ 延迟重试（含最大次数保护）
                canvasExecutionService.publishEventWithOverflowFallback(
                        cid, req.getUserId(), req.getEventCode(), payload, msgId);
                log.info("[EVENT] 触发画布 canvasId={} eventCode={} userId={}",
                        cid, req.getEventCode(), req.getUserId());
            } catch (Exception e) {
                log.warn("[EVENT] 触发画布失败 canvasId={}: {}", cidStr, e.getMessage());
            }
        });

        if (canvasIds.isEmpty()) {
            log.info("[EVENT] 无已发布画布订阅事件 eventCode={}", req.getEventCode());
        }
        return canvasIds;
    }

    /**
     * 写事件日志到 event_log 表。
     * canvasTriggered 字段在 triggerAllCanvas 之后写入，反映实际触发数量
     * （不含 WAIT 恢复数量，后者在 resumeWaitsAndBuildResult 中统计）。
     */
    private EventLogDO writeEventLog(EventReportReq req, Map<String, Object> payload, Set<String> canvasIds) {
        EventLogDO eventLog = new EventLogDO();
        eventLog.setEventCode(req.getEventCode());
        eventLog.setUserId(req.getUserId());
        eventLog.setPerfRunId(PerfRunContext.extract(payload));
        try {
            eventLog.setAttributes(req.getAttributes() != null
                    ? objectMapper.writeValueAsString(req.getAttributes()) : null);
        } catch (Exception ignored) {
            // 属性序列化失败不阻断事件触发，日志保留主体字段即可。
        }
        eventLog.setCanvasTriggered(canvasIds.size());
        eventLog.setCanvasCount(canvasIds.size());
        logMapper.insert(eventLog);
        return eventLog;
    }

    /** 基于 idempotencyKey 做 Redis 24 小时去重，重复上报直接短路。 */
    private boolean isDuplicateEvent(EventReportReq req) {
        if (!StringUtils.hasText(req.getIdempotencyKey())) return false;
        String dedupKey = redisKeys.eventDedup(req.getIdempotencyKey());
        boolean isFirstSeen = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofHours(24)));
        if (!isFirstSeen) {
            log.debug("[EVENT] idempotencyKey 重复上报，忽略 eventCode={} key={}",
                    req.getEventCode(), req.getIdempotencyKey());
        }
        return !isFirstSeen;
    }

    /** 构造重复上报时的轻量响应，不再写事件日志或触发画布。 */
    private Map<String, Object> duplicateResponse(EventReportReq req) {
        Map<String, Object> dup = new LinkedHashMap<>();
        dup.put(MapFieldKeys.EVENT_CODE, req.getEventCode());
        dup.put(MapFieldKeys.USER_ID, req.getUserId());
        dup.put(MapFieldKeys.STATUS, "DUPLICATED");
        return dup;
    }

    /** 优先使用幂等键生成稳定事件 ID，否则使用雪花 ID。 */
    private String resolveEventId(EventReportReq req) {
        return StringUtils.hasText(req.getIdempotencyKey())
                ? "evt-idem-" + req.getIdempotencyKey()
                : "evt-" + snowflake.nextIdStr();
    }

    /**
     * 校验请求参数和事件定义。
     *
     * <p>事件定义走二级缓存（L1: Caffeine 200条/10min 刷新，L2: Redis 1h，
     * null 值短 TTL 1min 防穿透，本地单飞防击穿，TTL 抖动防雪崩）。
     * 缓存命中时不查 DB，因此事件被禁用后最多 1min（null 值 TTL）才生效。
     */
    private void validateReportReq(EventReportReq req) {
        if (req.getEventCode() == null || req.getEventCode().isBlank())
            throw new IllegalArgumentException("eventCode 不能为空");
        if (req.getUserId() == null || req.getUserId().isBlank())
            throw new IllegalArgumentException("userId 不能为空");

        // 走二级缓存查事件定义，enabled=1（已发布）才返回，否则视为不存在
        EventDefinitionDO def = eventDefinitionCacheService.getPublishedByCode(req.getEventCode());
        if (def == null)
            throw new IllegalArgumentException("事件未定义或已禁用: " + req.getEventCode());
    }
}
