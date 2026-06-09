package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeView;
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
 * MarketingIntegrationContractProbeController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-integrations")
public class MarketingIntegrationContractProbeController {

    private final MarketingIntegrationContractProbeService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingIntegrationContractProbeController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingIntegrationContractProbeController(MarketingIntegrationContractProbeService service,
                                                       TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 记录营销 Integration Contract Probe数据接口，对应 POST /contracts/{contractId}/probes。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param contractId contract ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含记录营销 Integration Contract Probe数据后的业务数据。
     */
    @PostMapping("/contracts/{contractId}/probes")
    public Mono<R<MarketingIntegrationContractProbeView>> recordProbe(
            @PathVariable Long contractId,
            @RequestBody MarketingIntegrationContractProbeCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.recordProbe(tenantId(context), contractId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Integration Contract Probe列表接口，对应 GET /contracts/{contractId}/probes。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param contractId contract ID。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contracts/{contractId}/probes")
    public Mono<R<List<MarketingIntegrationContractProbeView>>> listContractProbes(
            @PathVariable Long contractId,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listContractProbes(tenantId(context), contractId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Integration Contract Probe列表接口，对应 GET /probes。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/probes")
    public Mono<R<List<MarketingIntegrationContractProbeView>>> listRecentProbes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listRecentProbes(tenantId(context), status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
