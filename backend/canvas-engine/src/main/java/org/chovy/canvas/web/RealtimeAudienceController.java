package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.cdp.RealtimeAudienceService;
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

@RestController
@RequestMapping("/cdp")
@RequiredArgsConstructor
/**
 * RealtimeAudienceController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class RealtimeAudienceController {
    private final TenantContextResolver tenantContextResolver;
    private final RealtimeAudienceService service;

    @PostMapping("/realtime-audiences/{id}/events")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 processEvent 流程生成的业务结果。
     */
    public Mono<R<RealtimeAudienceService.EventResult>> processEvent(
            @PathVariable Long id,
            @RequestBody RealtimeEventRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.processEvent(
                                tenantId(ctx),
                                id,
                                request.toEvent(),
                                request.removeOnNoMatchOrDefault())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/realtime-audiences/{id}/snapshot")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 snapshot 流程生成的业务结果。
     */
    public Mono<R<RealtimeAudienceService.SnapshotResult>> snapshot(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.createSnapshot(tenantId(ctx), id, "MANUAL", ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/realtime-audiences/{id}/snapshots")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 snapshots 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<RealtimeAudienceService.SnapshotRow>>> snapshots(@PathVariable Long id,
                                                                        @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.listSnapshots(tenantId(ctx), id, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/audiences/{leftId}/overlap/{rightId}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param leftId 业务对象 ID，用于定位具体记录。
     * @param rightId 业务对象 ID，用于定位具体记录。
     * @return 返回 overlap 流程生成的业务结果。
     */
    public Mono<R<RealtimeAudienceService.OverlapResult>> overlap(@PathVariable Long leftId,
                                                                  @PathVariable Long rightId) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.overlap(leftId, rightId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/audiences/merge")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param leftId 业务对象 ID，用于定位具体记录。
     * @param rightId 业务对象 ID，用于定位具体记录。
     * @return 返回 merge 汇总后的集合、分页或映射视图。
     */
    public Mono<R<RealtimeAudienceService.SetOperationResult>> merge(@RequestParam Long leftId,
                                                                     @RequestParam Long rightId) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.merge(leftId, rightId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/audiences/exclude")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param baseId 业务对象 ID，用于定位具体记录。
     * @param excludedId 业务对象 ID，用于定位具体记录。
     * @return 返回 exclude 汇总后的集合、分页或映射视图。
     */
    public Mono<R<RealtimeAudienceService.SetOperationResult>> exclude(@RequestParam Long baseId,
                                                                       @RequestParam Long excludedId) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.exclude(baseId, excludedId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }

    /**
     * RealtimeEventRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record RealtimeEventRequest(String sourceEventId,
                                       String userId,
                                       java.time.Instant eventTime,
                                       java.util.Map<String, Object> properties,
                                       Boolean removeOnNoMatch) {
        /**
         * 组装输出结构或完成对象转换。
         *
         * @return 返回组装或转换后的结果对象。
         */
        public RealtimeAudienceService.CdpEvent toEvent() {
            return new RealtimeAudienceService.CdpEvent(sourceEventId, userId, eventTime, properties);
        }

        /**
         * 清理、停用或释放指定业务资源。
         *
         * @return 返回 remove on no match or default 的布尔判断结果。
         */
        boolean removeOnNoMatchOrDefault() {
            return removeOnNoMatch == null || removeOnNoMatch;
        }
    }
}
