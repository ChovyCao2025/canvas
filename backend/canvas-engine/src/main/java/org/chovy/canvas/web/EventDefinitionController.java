package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.dto.EventReportReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.service.EventDefinitionService;
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

    private final EventDefinitionMapper eventMapper;
    private final EventDefinitionCacheService eventDefinitionCacheService;
    private final EventDefinitionService eventDefinitionService;


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
        return Mono.fromCallable(() -> eventDefinitionService.doReportEvent(req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }


}
