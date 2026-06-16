package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractAuditEventView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAutomationService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloEvaluationView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractView;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * MarketingIntegrationContractController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-integrations")
public class MarketingIntegrationContractController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingIntegrationContractService service;
    /**
     * probe服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingIntegrationContractProbeService probeService;
    /**
     * probeautomation服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingIntegrationContractProbeAutomationService probeAutomationService;
    /**
     * slo服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingIntegrationContractSloService sloService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingIntegrationContractController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeAutomationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param sloService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingIntegrationContractController(MarketingIntegrationContractService service,
                                                  MarketingIntegrationContractProbeService probeService,
                                                  MarketingIntegrationContractProbeAutomationService probeAutomationService,
                                                  MarketingIntegrationContractSloService sloService,
                                                  TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.probeService = probeService;
        this.probeAutomationService = probeAutomationService;
        this.sloService = sloService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新 营销 Integration Contract接口，对应 POST /contracts。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 营销 Integration Contract后的业务数据。
     */
    @PostMapping("/contracts")
    public Mono<R<MarketingIntegrationContractView>> upsertContract(
            @RequestBody MarketingIntegrationContractCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertContract(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Integration Contract列表接口，对应 GET /contracts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param providerFamily 请求参数，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contracts")
    public Mono<R<List<MarketingIntegrationContractView>>> listContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerFamily,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listContracts(tenantId(context), status, providerFamily, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Integration Contract列表接口，对应 GET /contracts/{contractId}/audit-events。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param contractId contract ID。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contracts/{contractId}/audit-events")
    public Mono<R<List<MarketingIntegrationContractAuditEventView>>> listContractAuditEvents(
            @PathVariable Long contractId,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listAuditEvents(tenantId(context), contractId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档 营销 Integration Contract接口，对应 DELETE /contracts/{contractId}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param contractId contract ID。
     * @return 异步返回统一响应，包含归档 营销 Integration Contract后的业务数据。
     */
    @DeleteMapping("/contracts/{contractId}")
    public Mono<R<MarketingIntegrationContractView>> archiveContract(@PathVariable Long contractId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.archiveContract(tenantId(context), contractId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发营销 Integration Contract运行接口，对应 POST /contracts/{contractId}/probe-runs。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 probeService.recordProbeRun 完成业务处理。
     * 副作用：会触发一次运行流程，会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param contractId contract ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含触发营销 Integration Contract运行后的业务数据。
     */
    @PostMapping("/contracts/{contractId}/probe-runs")
    public Mono<R<MarketingIntegrationContractProbeRunView>> recordProbeRun(
            @PathVariable Long contractId,
            @RequestBody MarketingIntegrationContractProbeRunCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.recordProbeRun(tenantId(context), contractId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Integration Contract列表接口，对应 GET /contract-probe-runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 probeService.listProbeRuns 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param providerFamily 请求参数，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contract-probe-runs")
    public Mono<R<List<MarketingIntegrationContractProbeRunView>>> listProbeRuns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerFamily,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.listProbeRuns(tenantId(context), status, providerFamily, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发营销 Integration Contract运行接口，对应 POST /contract-probe-runs/scan。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 probeAutomationService.scanProductionContracts 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含触发营销 Integration Contract运行后的业务数据。
     */
    @PostMapping("/contract-probe-runs/scan")
    public Mono<R<MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary>> scanProbeRuns(
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeAutomationService.scanProductionContracts(tenantId(context), limit, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Integration Contract列表接口，对应 GET /contract-slo-evaluations。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 sloService.listProductionSloEvaluations 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contract-slo-evaluations")
    public Mono<R<List<MarketingIntegrationContractSloEvaluationView>>> listContractSloEvaluations(
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(sloService.listProductionSloEvaluations(tenantId(context), limit)))
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
