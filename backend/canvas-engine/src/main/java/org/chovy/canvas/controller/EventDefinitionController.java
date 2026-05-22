package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.chovy.canvas.domain.constant.NodeType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.meta.EventDefinition;
import org.chovy.canvas.domain.meta.EventDefinitionMapper;
import org.chovy.canvas.domain.meta.EventLog;
import org.chovy.canvas.domain.meta.EventLogMapper;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 事件定义管理 + 事件上报接口。
 * 上报流程：
 * POST /canvas/events/report
 * → 验证事件定义存在
 * → 记录 event_log
 * → 通过 Disruptor 异步触发匹配当前 eventCode 的所有画布
 */
@Slf4j
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class EventDefinitionController {

    private final EventDefinitionMapper eventMapper;
    private final EventLogMapper logMapper;
    private final CanvasDisruptorService disruptorService;
    private final TriggerRouteService triggerRouteService;
    private final ObjectMapper objectMapper;

    /** 事件定义本地缓存：eventCode → EventDefinition。TTL=10min，最多200条。 */
    private final Cache<String, EventDefinition> eventDefCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    // ── 事件定义 CRUD ────────────────────────────────────────────

    @GetMapping("/event-definitions")
    public Mono<R<PageResult<EventDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            var wrapper = new LambdaQueryWrapper<EventDefinition>()
                    .eq(enabled != null, EventDefinition::getEnabled, enabled)
                    .orderByAsc(EventDefinition::getId);
            Page<EventDefinition> p = eventMapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/event-definitions")
    public Mono<R<EventDefinition>> create(@RequestBody EventDefinition body) {
        return Mono.fromCallable(() -> {
                    eventMapper.insert(body);
                    return R.ok(body);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/event-definitions/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody EventDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
                    eventMapper.updateById(body);
                    if (body.getEventCode() != null) {
                        eventDefCache.invalidate(body.getEventCode());
                    }
                    return R.ok();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/event-definitions/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> eventMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }

    // ── 事件上报 ─────────────────────────────────────────────────

    /**
     * 事件上报接口。业务方调用此接口，系统存储事件并异步触发对应画布。
     * 不直接耦合到具体画布 ID，由引擎根据 eventCode 路由。
     */
    @PostMapping("/events/report")
    public Mono<R<Map<String, Object>>> reportEvent(@RequestBody EventReportReq req) {
        return Mono.fromCallable(() -> {
                    if (req.getEventCode() == null || req.getEventCode().isBlank())
                        throw new IllegalArgumentException("eventCode 不能为空");
                    if (req.getUserId() == null || req.getUserId().isBlank())
                        throw new IllegalArgumentException("userId 不能为空");

                    // 1. 验证事件定义存在（走本地缓存，TTL=10min）
                    EventDefinition def = eventDefCache.get(req.getEventCode(), code ->
                            eventMapper.selectOne(
                                    new LambdaQueryWrapper<EventDefinition>()
                                            .eq(EventDefinition::getEventCode, code)
                                            .eq(EventDefinition::getEnabled, CanvasStatusEnum.PUBLISHED.getCode())));
                    if (def == null)
                        throw new IllegalArgumentException("事件未定义或已禁用: " + req.getEventCode());

                    // 2. 从路由表查所有监听此事件的已发布画布，逐一触发
                    // FIXME: Redis 异常意味着整个链路都无法推进, 考虑降级方案以及是否有做好缓存刷新问题
                    Set<String> canvasIds = triggerRouteService.getCanvasByBehavior(req.getEventCode());
                    // FIXME: 使用雪花算法代替
                    String eventId = "evt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    Map<String, Object> payload = req.getAttributes() != null ? req.getAttributes() : Map.of();

                    canvasIds.forEach(cidStr -> {
                        try {
                            Long cid = Long.parseLong(cidStr);
                            // 事件实际发布
                            // 具体的消费逻辑: org.chovy.canvas.engine.disruptor.CanvasDisruptorService.CanvasDisruptorService
                            disruptorService.publish(cid, req.getUserId(), TriggerType.EVENT,
                                    NodeType.EVENT_TRIGGER, req.getEventCode(), payload, eventId + "-" + cidStr);
                            log.info("[EVENT] 触发画布 canvasId={} eventCode={} userId={}",
                                    cid, req.getEventCode(), req.getUserId());
                        } catch (Exception e) {
                            log.warn("[EVENT] 触发画布失败 canvasId={}: {}", cidStr, e.getMessage());
                        }
                    });

                    if (canvasIds.isEmpty()) {
                        log.info("[EVENT] 无已发布画布订阅事件 eventCode={}", req.getEventCode());
                    }

                    // 3. 记录事件日志（在获取 canvasIds 之后，以写入真实触发数量）
                    EventLog eventLog = new EventLog();
                    eventLog.setEventCode(req.getEventCode());
                    eventLog.setUserId(req.getUserId());
                    try {
                        eventLog.setAttributes(req.getAttributes() != null
                                ? objectMapper.writeValueAsString(req.getAttributes()) : null);
                    } catch (Exception ignored) {
                    }
                    eventLog.setCanvasTriggered(canvasIds.size());
                    eventLog.setCanvasCount(canvasIds.size());
                    logMapper.insert(eventLog);

                    Map<String, Object> resp = new java.util.LinkedHashMap<>();
                    resp.put("eventLogId", eventLog.getId());
                    resp.put("eventCode", req.getEventCode());
                    resp.put("userId", req.getUserId());
                    resp.put("canvasTriggered", canvasIds.size());
                    resp.put("status", "ACCEPTED");
                    return resp;
                }).subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }


}
