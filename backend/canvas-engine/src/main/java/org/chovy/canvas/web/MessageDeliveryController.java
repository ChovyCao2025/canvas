package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.delivery.DeliveryOutboxDO;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.chovy.canvas.engine.delivery.DeliveryReceiptLog;
import org.chovy.canvas.engine.delivery.DeliveryReconciliationJob;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * MessageDeliveryController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/message-deliveries")
public class MessageDeliveryController {

    /**
     * outbox服务，用于承接对应业务能力和领域编排。
     */
    private final DeliveryOutboxService outboxService;
    /**
     * reconciliationjob，用于保存请求处理过程中需要的业务数据。
     */
    private final DeliveryReconciliationJob reconciliationJob;

    /**
     * 创建 MessageDeliveryController 实例并注入 web 场景依赖。
     * @param outboxService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reconciliationJob reconciliation job 参数，用于 MessageDeliveryController 流程中的校验、计算或对象转换。
     */
    public MessageDeliveryController(DeliveryOutboxService outboxService,
                                     DeliveryReconciliationJob reconciliationJob) {
        this.outboxService = outboxService;
        this.reconciliationJob = reconciliationJob;
    }
    /**
     * 查询消息 Delivery列表接口，对应 GET 请求。
     * 接口按传入租户 ID 定位数据，调用方需具备目标租户访问权限。
     * 主要委托 outboxService.search, DeliveryOutboxService.DeliverySearchCriteria 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tenantId 租户 ID，可选。
     * @param canvasId 画布 ID，可选。
     * @param executionId execution ID，可选。
     * @param userId user ID，可选。
     * @param channel 渠道过滤条件，可选。
     * @param provider 供应商过滤条件，可选。
     * @param status 状态过滤条件，可选。
     * @param providerMessageId provider Message ID，可选。
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 20。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping
    public Mono<R<PageResult<DeliveryOutboxDO>>> list(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerMessageId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> R.ok(outboxService.search(new DeliveryOutboxService.DeliverySearchCriteria(
                        tenantId, canvasId, executionId, userId, channel, provider, status, providerMessageId, page, size))))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 获取消息 Delivery详情接口，对应 GET /{id}。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 outboxService.findById 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取消息 Delivery详情后的业务数据。
     */
    @GetMapping("/{id}")
    public Mono<R<DeliveryOutboxDO>> detail(@PathVariable Long id) {
        return Mono.fromCallable(() -> outboxService.findById(id)
                        .map(R::ok)
                        .orElseGet(() -> R.fail("message delivery not found: " + id)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 消息 Delivery 请求接口，对应 GET /{id}/receipts。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 outboxService.receiptHistory 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{id}/receipts")
    public Mono<R<List<DeliveryReceiptLog>>> receipts(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(outboxService.receiptHistory(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 消息 Delivery 请求接口，对应 POST /{id}/replay。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 outboxService.replayDead 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含键值结果。
     */
    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replayDead(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            boolean replayed = outboxService.replayDead(id);
            if (!replayed) {
                return R.<Map<String, Object>>fail("delivery is not replayable: " + id);
            }
            return R.ok(Map.<String, Object>of("outboxId", id, "status", DeliveryOutboxService.STATUS_PENDING));
        }).subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 消息 Delivery 请求接口，对应 POST /reconcile。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含键值结果。
     */
    @PostMapping("/reconcile")
    public Mono<R<Map<String, Object>>> reconcile() {
        return Mono.fromCallable(() -> {
            int requeued = reconciliationJob.reconcile();
            return R.ok(Map.<String, Object>of("requeued", requeued));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
