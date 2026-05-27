package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.dto.EventReportReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.service.EventDefinitionService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

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

    /** 事件定义 Mapper，用于读写事件定义。 */
    private final EventDefinitionMapper eventMapper;
    /** 事件定义缓存服务，用于刷新事件路由缓存。 */
    private final EventDefinitionCacheService eventDefinitionCacheService;
    /** 事件定义服务，用于处理事件定义业务校验。 */
    private final EventDefinitionService eventDefinitionService;
    /** JSON 转换器，用于从原始请求体解析事件上报内容。 */
    private final ObjectMapper objectMapper;
    /** 事件上报签名校验服务。 */
    private final EventReportAuthService eventReportAuthService;


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

    /**
     * 处理 create 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/event-definitions")
    public Mono<R<EventDefinitionDO>> create(@RequestBody EventDefinitionDO body) {
        return Mono.fromCallable(() -> {
                    eventMapper.insert(body);
                    invalidateEventCode(body.getEventCode());
                    return R.ok(body);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 update 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/event-definitions/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody EventDefinitionDO body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
                    EventDefinitionDO existing = eventMapper.selectById(id);
                    eventMapper.updateById(body);
                    if (existing != null) invalidateEventCode(existing.getEventCode());
                    invalidateEventCode(body.getEventCode());
                    return R.ok();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 delete 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @DeleteMapping("/event-definitions/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> {
                    EventDefinitionDO existing = eventMapper.selectById(id);
                    eventMapper.deleteById(id);
                    if (existing != null) invalidateEventCode(existing.getEventCode());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }

    /**
     * 处理 report Event 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param req 请求对象，承载调用方提交的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
// ── 事件上报 ─────────────────────────────────────────────────

    /**
     * 事件上报接口。业务方调用此接口，系统存储事件并异步触发对应画布。
     * 不直接耦合到具体画布 ID，由引擎根据 eventCode 路由。
     */
    @PostMapping("/events/report")
    public Mono<R<Map<String, Object>>> reportEvent(
            ServerHttpRequest request,
            @RequestBody Mono<String> rawBody) {
        return rawBody.defaultIfEmpty("")
                .flatMap(body -> Mono.fromCallable(() -> {
                            eventReportAuthService.verify(request.getHeaders(), body);
                            EventReportReq req = objectMapper.readValue(body, EventReportReq.class);
                            return eventDefinitionService.doReportEvent(req);
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    private void invalidateEventCode(String eventCode) {
        if (eventCode != null && !eventCode.isBlank()) {
            eventDefinitionCacheService.invalidatePublishedByCode(eventCode);
        }
    }

}
