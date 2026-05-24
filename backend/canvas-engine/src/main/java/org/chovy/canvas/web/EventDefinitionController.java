package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.NodeType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private final EventDefinitionCacheService eventDefinitionCacheService;
    private final WaitResumeService waitResumeService;

    // ── 事件定义 CRUD ────────────────────────────────────────────

    @GetMapping("/event-definitions")
    public Mono<R<PageResult<EventDefinitionDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            var wrapper = new LambdaQueryWrapper<EventDefinitionDO>()
                    .eq(enabled != null, EventDefinitionDO::getEnabled, enabled)
                    .orderByAsc(EventDefinitionDO::getId);
            Page<EventDefinitionDO> p = eventMapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/event-definitions")
    public Mono<R<EventDefinitionDO>> create(@RequestBody EventDefinitionDO body) {
        return Mono.fromCallable(() -> {
                    eventMapper.insert(body);
                    return R.ok(body);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/event-definitions/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody EventDefinitionDO body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
                    eventMapper.updateById(body);
                    if (body.getEventCode() != null) {
                        eventDefinitionCacheService.invalidatePublishedByCode(body.getEventCode());
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

                    // 1. 验证事件定义存在（走 cache SDK，TTL=10min）
                    EventDefinitionDO def = eventDefinitionCacheService.getPublishedByCode(req.getEventCode());
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
                    EventLogDO eventLog = new EventLogDO();
                    eventLog.setEventCode(req.getEventCode());
                    eventLog.setUserId(req.getUserId());
                    eventLog.setPerfRunId(PerfRunContext.extract(payload));
                    try {
                        eventLog.setAttributes(req.getAttributes() != null
                                ? objectMapper.writeValueAsString(req.getAttributes()) : null);
                    } catch (Exception ignored) {
                    }
                    eventLog.setCanvasTriggered(canvasIds.size());
                    eventLog.setCanvasCount(canvasIds.size());
                    logMapper.insert(eventLog);

                    int waitsResumed = waitResumeService.resumeEventWaits(
                            req.getEventCode(),
                            req.getUserId(),
                            payload,
                            eventId
                    );

                    Map<String, Object> resp = new java.util.LinkedHashMap<>();
                    resp.put(MapFieldKeys.EVENT_LOG_ID, eventLog.getId());
                    resp.put(MapFieldKeys.EVENT_CODE, req.getEventCode());
                    resp.put(MapFieldKeys.USER_ID, req.getUserId());
                    resp.put(MapFieldKeys.CANVAS_TRIGGERED, canvasIds.size());
                    resp.put(MapFieldKeys.WAITS_RESUMED, waitsResumed);
                    resp.put(MapFieldKeys.STATUS, "ACCEPTED");
                    return resp;
                }).subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }


}
