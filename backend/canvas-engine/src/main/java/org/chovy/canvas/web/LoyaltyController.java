package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.loyalty.LoyaltyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * LoyaltyController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/loyalty")
public class LoyaltyController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * loyalty服务，用于承接对应业务能力和领域编排。
     */
    private final LoyaltyService loyaltyService;

    /**
     * 创建 LoyaltyController 实例并注入 web 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param loyaltyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LoyaltyController(TenantContextResolver tenantContextResolver,
                             LoyaltyService loyaltyService) {
        this.tenantContextResolver = tenantContextResolver;
        this.loyaltyService = loyaltyService;
    }
    /**
     * 处理 会员忠诚度 请求接口，对应 GET /users/{userId}/account。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 loyaltyService.account 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @return 异步返回统一响应，包含处理 会员忠诚度 请求后的业务数据。
     */
    @GetMapping("/users/{userId}/account")
    public Mono<R<LoyaltyService.LoyaltyAccountView>> account(@PathVariable String userId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.account(normalizeTenant(context), userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会员忠诚度 请求接口，对应 POST /users/{userId}/earn。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 loyaltyService.earn 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 会员忠诚度 请求后的业务数据。
     */
    @PostMapping("/users/{userId}/earn")
    public Mono<R<LoyaltyService.LoyaltyAccountView>> earn(@PathVariable String userId,
                                                           @RequestBody LoyaltyService.EarnCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.earn(normalizeTenant(context), userId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会员忠诚度 请求接口，对应 POST /users/{userId}/redeem。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 loyaltyService.redeem 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 会员忠诚度 请求后的业务数据。
     */
    @PostMapping("/users/{userId}/redeem")
    public Mono<R<LoyaltyService.RedemptionView>> redeem(@PathVariable String userId,
                                                         @RequestBody LoyaltyService.RedemptionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.redeem(normalizeTenant(context), userId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会员忠诚度 请求接口，对应 GET /users/{userId}/benefits。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 loyaltyService.eligibleBenefits 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/users/{userId}/benefits")
    public Mono<R<List<LoyaltyService.BenefitEligibilityView>>> benefits(@PathVariable String userId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.eligibleBenefits(normalizeTenant(context), userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
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

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
