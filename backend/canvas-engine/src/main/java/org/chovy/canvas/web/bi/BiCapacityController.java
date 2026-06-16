package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/bi/capacity")
/**
 * BiCapacityController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class BiCapacityController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * quickenginecapacity服务，用于承接对应业务能力和领域编排。
     */
    private final BiQuickEngineCapacityService quickEngineCapacityService;
    /**
     * quickenginequeue服务，用于承接对应业务能力和领域编排。
     */
    private final BiQuickEngineQueueService quickEngineQueueService;

    @Autowired
    /**
     * 初始化 BiCapacityController 实例。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param quickEngineCapacityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param quickEngineQueueService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiCapacityController(TenantContextResolver tenantContextResolver,
                                BiQuickEngineCapacityService quickEngineCapacityService,
                                BiQuickEngineQueueService quickEngineQueueService) {
        this.tenantContextResolver = tenantContextResolver;
        this.quickEngineCapacityService = quickEngineCapacityService;
        this.quickEngineQueueService = quickEngineQueueService;
    }

    /**
     * 初始化 BiCapacityController 实例。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param quickEngineCapacityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiCapacityController(TenantContextResolver tenantContextResolver,
                                BiQuickEngineCapacityService quickEngineCapacityService) {
        this(tenantContextResolver, quickEngineCapacityService, null);
    }

    @GetMapping("/quick-engine")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 quickEngineCapacity 流程生成的业务结果。
     */
    public Mono<R<BiQuickEngineCapacitySummaryView>> quickEngineCapacity(
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineCapacityService.summary(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/quick-engine/queue")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 quickEngineQueue 流程生成的业务结果。
     */
    public Mono<R<BiQuickEngineQueueSnapshotView>> quickEngineQueue(
            @RequestParam(required = false) String poolKey,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineQueueService.snapshot(context.tenantId(), poolKey, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/quick-engine/alert-policy")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<BiQuickEngineCapacityAlertPolicyView>> upsertQuickEngineCapacityAlertPolicy(
            @RequestBody BiQuickEngineCapacityAlertPolicyCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineCapacityService.upsertAlertPolicy(
                                context.tenantId(),
                                command,
                                context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/quick-engine/tenant-pool-policy")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<BiQuickEngineTenantPoolPolicyView>> upsertQuickEngineTenantPoolPolicy(
            @RequestBody BiQuickEngineTenantPoolPolicyCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineCapacityService.upsertTenantPoolPolicy(
                                context.tenantId(),
                                command,
                                context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
