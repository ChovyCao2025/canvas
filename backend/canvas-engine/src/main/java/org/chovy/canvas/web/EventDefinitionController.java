package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.validation.ApiRequestValidation;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.dto.EventReportReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.service.EventDefinitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
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
@Validated
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
    /**
     * 查询Event Definition列表接口，对应 GET /event-definitions。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 20。
     * @param enabled 请求参数，可选。
     * @return 异步返回统一响应，包含分页结果。
     */
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
            @Valid @RequestBody Mono<String> rawBody) {
        return rawBody.defaultIfEmpty("")
                .flatMap(body -> Mono.fromCallable(() -> {
                            eventReportAuthService.verify(request.getHeaders(), body);
                            EventReportReq req = parseAndValidateEventReport(body);
                            return eventDefinitionService.doReportEvent(req);
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    /**
     * 解析并校验输入数据。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private EventReportReq parseAndValidateEventReport(String body) {
        try {
            return ApiRequestValidation.validate(objectMapper.readValue(body, EventReportReq.class));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体 JSON 不合法", e);
        }
    }

    /**
     * 执行 invalidateEventCode 流程，围绕 invalidate event code 完成校验、计算或结果组装。
     *
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     */
    private void invalidateEventCode(String eventCode) {
        if (eventCode != null && !eventCode.isBlank()) {
            eventDefinitionCacheService.invalidatePublishedByCode(eventCode);
        }
    }

}
