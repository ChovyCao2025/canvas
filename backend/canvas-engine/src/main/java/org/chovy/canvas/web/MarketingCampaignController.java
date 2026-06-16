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

/**
 * MarketingCampaignController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-campaigns")
public class MarketingCampaignController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingCampaignService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingCampaignController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
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
