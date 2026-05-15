package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.EventDefinition;
import org.chovy.canvas.domain.meta.EventDefinitionMapper;
import org.chovy.canvas.domain.meta.EventLog;
import org.chovy.canvas.domain.meta.EventLogMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 事件定义管理 + 事件上报接口。
 *
 * 上报流程：
 *   POST /canvas/events/report
 *   → 验证事件定义存在
 *   → 记录 event_log
 *   → 通过 Disruptor 异步触发匹配当前 eventCode 的所有画布
 */
@Slf4j
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class EventDefinitionController {

    private final EventDefinitionMapper  eventMapper;
    private final EventLogMapper         logMapper;
    private final CanvasDisruptorService disruptorService;
    private final ObjectMapper           objectMapper;

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
        return Mono.fromCallable(() -> { eventMapper.insert(body); return R.ok(body); })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/event-definitions/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody EventDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> { eventMapper.updateById(body); return R.<Void>ok(); })
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

            // 1. 验证事件定义存在
            EventDefinition def = eventMapper.selectOne(
                    new LambdaQueryWrapper<EventDefinition>()
                            .eq(EventDefinition::getEventCode, req.getEventCode())
                            .eq(EventDefinition::getEnabled, 1));
            if (def == null)
                throw new IllegalArgumentException("事件未定义或已禁用: " + req.getEventCode());

            // 2. 记录事件日志
            EventLog log = new EventLog();
            log.setEventCode(req.getEventCode());
            log.setUserId(req.getUserId());
            try {
                log.setAttributes(req.getAttributes() != null
                        ? objectMapper.writeValueAsString(req.getAttributes()) : null);
            } catch (Exception ignored) {}
            log.setCanvasTriggered(0);
            log.setCanvasCount(0);
            logMapper.insert(log);

            // 3. 通过 Disruptor 异步触发所有匹配 eventCode 的画布
            String eventId = "evt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            disruptorService.publish(
                    null,                          // canvasId=null：由路由表根据 eventCode 广播
                    req.getUserId(),
                    "EVENT",
                    "BEHAVIOR_IN_APP",
                    req.getEventCode(),
                    req.getAttributes() != null ? req.getAttributes() : Map.of(),
                    eventId);

            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("eventLogId", log.getId());
            resp.put("eventCode",  req.getEventCode());
            resp.put("userId",     req.getUserId());
            resp.put("status",     "ACCEPTED");
            return resp;
        }).subscribeOn(Schedulers.boundedElastic())
          .map(result -> R.<Map<String, Object>>ok(result));
    }

    @Data
    static class EventReportReq {
        /** 事件编码，必须在 event_definition 中已定义 */
        private String eventCode;
        /** 触发用户 ID */
        private String userId;
        /** 事件属性，key-value 结构，与事件定义中的 attributes 对应 */
        private Map<String, Object> attributes;
        /** 幂等 key，可选 */
        private String idempotencyKey;
    }
}
