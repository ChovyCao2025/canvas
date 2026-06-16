package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AnalyticsQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * AnalyticsController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final AnalyticsQueryService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 AnalyticsController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AnalyticsController(AnalyticsQueryService service) {
        this(service, null);
    }

    /**
     * 创建 AnalyticsController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public AnalyticsController(AnalyticsQueryService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 分析统计 请求接口，对应 GET /events/counts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param startDate 请求参数。
     * @param endDate 请求参数。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping({"/events/counts", "/events"})
    public Mono<R<List<AnalyticsQueryService.EventCountRow>>> eventCounts(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.eventCounts(tenantId, startDate, endDate)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 分析统计 请求接口，对应 GET /events/count。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param startDate 请求参数。
     * @param endDate 请求参数。
     * @param eventCode 请求参数，可选。
     * @return 异步返回统一响应，包含处理 分析统计 请求后的业务数据。
     */
    @GetMapping("/events/count")
    public Mono<R<AnalyticsQueryService.EventTotal>> countEvents(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String eventCode) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.countEvents(tenantId, startDate, endDate, eventCode)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 分析统计 请求接口，对应 GET /users/{userId}/timeline。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @param startDate 请求参数。
     * @param endDate 请求参数。
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 50。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping("/users/{userId}/timeline")
    public Mono<R<PageResult<AnalyticsQueryService.UserTimelineRow>>> userTimeline(
            @PathVariable String userId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.userTimeline(tenantId, userId, startDate, endDate, page, size)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 分析统计 请求接口，对应 GET /events/attributes/{attribute}/distribution。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param attribute 路径参数。
     * @param startDate 请求参数。
     * @param endDate 请求参数。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping({"/events/attributes/{attribute}/distribution", "/attributes/{attribute}/distribution"})
    public Mono<R<List<AnalyticsQueryService.AttributeDistributionRow>>> attributeDistribution(
            @PathVariable String attribute,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.attributeDistribution(tenantId, attribute, startDate, endDate)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/funnels/{funnelKey}")
    public Mono<R<AnalyticsQueryService.FunnelResult>> funnelResult(
            @PathVariable String funnelKey,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.funnelResult(tenantId, funnelKey, startDate, endDate)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/alerts/preview")
    public Mono<R<AnalyticsQueryService.AlertPreviewResult>> alertPreview(
            @RequestBody AnalyticsQueryService.AlertPreviewRequest request) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.alertPreview(tenantId, request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports")
    public Mono<R<AnalyticsQueryService.ExportJobView>> createExport(
            @RequestBody AnalyticsQueryService.ExportRequest request) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.createExport(tenantId, request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/exports/{id}")
    /**
     * 执行 exportStatus 对应的接口或辅助流程。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<AnalyticsQueryService.ExportJobView>> exportStatus(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.exportStatus(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }
}
