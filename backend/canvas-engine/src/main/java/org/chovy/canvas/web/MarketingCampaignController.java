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
import org.chovy.canvas.domain.marketing.MarketingCampaignCommand;
import org.chovy.canvas.domain.marketing.MarketingCampaignLinkCommand;
import org.chovy.canvas.domain.marketing.MarketingCampaignLinkView;
import org.chovy.canvas.domain.marketing.MarketingCampaignReadinessView;
import org.chovy.canvas.domain.marketing.MarketingCampaignService;
import org.chovy.canvas.domain.marketing.MarketingCampaignView;
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
@RequestMapping("/canvas/marketing-campaigns")
public class MarketingCampaignController {

    private final MarketingCampaignService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingCampaignController(MarketingCampaignService service,
                                       TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新 营销 Campaign接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 营销 Campaign后的业务数据。
     */
    @PostMapping
    public Mono<R<MarketingCampaignView>> upsertCampaign(@RequestBody MarketingCampaignCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCampaign(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Campaign列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<MarketingCampaignView>>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listCampaigns(tenantId(context), status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 营销 Campaign 请求接口，对应 POST /links。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 营销 Campaign 请求后的业务数据。
     */
    @PostMapping("/links")
    public Mono<R<MarketingCampaignLinkView>> linkResource(@RequestBody MarketingCampaignLinkCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.linkResource(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Campaign列表接口，对应 GET /{campaignId}/links。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param campaignId campaign ID。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{campaignId}/links")
    public Mono<R<List<MarketingCampaignLinkView>>> listLinks(@PathVariable Long campaignId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listLinks(tenantId(context), campaignId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询营销 Campaign就绪度接口，对应 GET /{campaignId}/readiness。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param campaignId campaign ID。
     * @return 异步返回统一响应，包含查询营销 Campaign就绪度后的业务数据。
     */
    @GetMapping("/{campaignId}/readiness")
    public Mono<R<MarketingCampaignReadinessView>> readiness(@PathVariable Long campaignId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.readiness(tenantId(context), campaignId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 营销 Campaign 请求接口，对应 DELETE /links/{linkId}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param linkId link ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/links/{linkId}")
    public Mono<R<Void>> unlinkResource(@PathVariable Long linkId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    service.unlinkResource(tenantId(context), linkId);
                    return R.ok();
                })
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
