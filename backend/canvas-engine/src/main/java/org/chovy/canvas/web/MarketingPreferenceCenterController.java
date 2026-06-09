package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.policy.MarketingPreferenceCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/canvas/marketing-preferences")
/**
 * MarketingPreferenceCenterController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class MarketingPreferenceCenterController {

    private final MarketingPreferenceCenterService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 MarketingPreferenceCenterController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingPreferenceCenterController(MarketingPreferenceCenterService service) {
        this(service, null);
    }

    @Autowired
    /**
     * 初始化 MarketingPreferenceCenterController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingPreferenceCenterController(MarketingPreferenceCenterService service,
                                               TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/users/{userId}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 report 流程生成的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.PreferenceReport>> report(@PathVariable String userId) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.report(tenantId, userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/users/{userId}/consents/{channel}")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 updateConsent 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.ConsentRow>> updateConsent(
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ConsentUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.upsertConsent(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.ConsentUpdateCommand(channel, req.consentStatus(), req.source()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/users/{userId}/channels/{channel}")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 updateChannel 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.ChannelRow>> updateChannel(
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ChannelUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.upsertChannel(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.ChannelUpdateCommand(
                        channel, req.address(), req.enabled(), req.verified(), req.metadata()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/users/{userId}/suppressions")
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 addSuppression 流程生成的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.SuppressionRow>> addSuppression(
            @PathVariable String userId,
            @RequestBody SuppressionCreateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.addSuppression(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.SuppressionCreateCommand(
                        req.channel(), req.reason(), req.active(), req.expiresAt()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/suppressions/{id}/deactivate")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 deactivateSuppression 流程生成的业务结果。
     */
    public Mono<R<Void>> deactivateSuppression(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.deactivateSuppression(tenantId, id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
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

    /**
     * ConsentUpdateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ConsentUpdateReq(String consentStatus, String source) {
    }

    /**
     * ChannelUpdateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ChannelUpdateReq(String address, Boolean enabled, Boolean verified, String metadata) {
    }

    /**
     * SuppressionCreateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record SuppressionCreateReq(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
    }
}
