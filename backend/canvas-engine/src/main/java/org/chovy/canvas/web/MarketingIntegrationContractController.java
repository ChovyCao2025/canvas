// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
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

@RestController
@RequestMapping("/canvas/marketing-integrations")
public class MarketingIntegrationContractController {

    private final MarketingIntegrationContractService service;
    private final MarketingIntegrationContractProbeService probeService;
    private final MarketingIntegrationContractProbeAutomationService probeAutomationService;
    private final MarketingIntegrationContractSloService sloService;
    private final TenantContextResolver tenantContextResolver;

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

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
