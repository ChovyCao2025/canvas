package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.governance.RiskStrategyCommand;
import org.chovy.canvas.domain.risk.governance.RiskStrategyDiffView;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService;
import org.chovy.canvas.domain.risk.governance.RiskStrategyTransitionRequest;
import org.chovy.canvas.domain.risk.governance.RiskStrategyView;
import org.chovy.canvas.domain.risk.governance.RiskStrategyVersionView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
 * 租户级风控策略接口，提供草稿、校验、审批、激活、回滚和差异对比能力。
 */
@RestController
@RequestMapping("/canvas/risk/strategies")
public class RiskStrategyController {

    /**
     * 策略服务，用于承接对应业务能力和领域编排。
     */
    private final RiskStrategyService strategyService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建风控策略控制器。
     */
    @Autowired
    public RiskStrategyController(RiskStrategyService strategyService,
                                  TenantContextResolver tenantContextResolver) {
        this.strategyService = strategyService;
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 创建策略草稿版本。
     */
    @PostMapping
    public Mono<R<RiskStrategyView>> createDraft(@RequestBody RiskStrategyCommand command) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.createDraft(context.tenantId(), command, actor(context)))));
    }

    /**
     * 查询当前租户策略列表，可按场景过滤。
     */
    @GetMapping
    public Mono<R<List<RiskStrategyView>>> listStrategies(@RequestParam(required = false) String sceneKey) {
        return currentReader().flatMap(context -> blocking(() ->
                R.ok(strategyService.listStrategies(context.tenantId(), sceneKey))));
    }

    /**
     * 查询单个策略。
     */
    @GetMapping("/{strategyKey}")
    public Mono<R<RiskStrategyView>> getStrategy(@PathVariable String strategyKey) {
        return currentReader().flatMap(context -> blocking(() ->
                R.ok(strategyService.getStrategy(context.tenantId(), strategyKey))));
    }

    /**
     * 查询策略版本列表。
     */
    @GetMapping("/{strategyKey}/versions")
    public Mono<R<List<RiskStrategyVersionView>>> listVersions(@PathVariable String strategyKey) {
        return currentReader().flatMap(context -> blocking(() ->
                R.ok(strategyService.listVersions(context.tenantId(), strategyKey))));
    }

    /**
     * 校验策略版本。
     */
    @PostMapping("/{strategyKey}/versions/{version}/validate")
    public Mono<R<RiskStrategyVersionView>> validateVersion(@PathVariable String strategyKey,
                                                            @PathVariable int version) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.validate(context.tenantId(), strategyKey, version, actor(context)))));
    }

    /**
     * 标记策略版本已完成仿真。
     */
    @PostMapping("/{strategyKey}/versions/{version}/simulate")
    public Mono<R<RiskStrategyVersionView>> markSimulated(@PathVariable String strategyKey,
                                                          @PathVariable int version,
                                                          @RequestBody(required = false) RiskStrategyTransitionRequest request) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.markSimulated(context.tenantId(), strategyKey, version, actor(context)))));
    }

    /**
     * 提交策略版本进入审批。
     */
    @PostMapping("/{strategyKey}/versions/{version}/submit")
    public Mono<R<RiskStrategyVersionView>> submitVersion(@PathVariable String strategyKey,
                                                          @PathVariable int version,
                                                          @RequestBody(required = false) RiskStrategyTransitionRequest request) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.submit(context.tenantId(), strategyKey, version, actor(context)))));
    }

    /**
     * 审批策略版本。
     */
    @PostMapping("/{strategyKey}/versions/{version}/approve")
    public Mono<R<RiskStrategyVersionView>> approveVersion(@PathVariable String strategyKey,
                                                           @PathVariable int version,
                                                           @RequestBody(required = false) RiskStrategyTransitionRequest request) {
        // 审批权限比编排更严格，便于落实制作人与复核人分离。
        return currentApprover().flatMap(context -> blocking(() ->
                R.ok(strategyService.approve(context.tenantId(), strategyKey, version, actor(context)))));
    }

    /**
     * 激活策略版本。
     */
    @PostMapping("/{strategyKey}/versions/{version}/activate")
    public Mono<R<RiskStrategyView>> activateVersion(@PathVariable String strategyKey,
                                                     @PathVariable int version,
                                                     @RequestBody(required = false) RiskStrategyTransitionRequest request) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.activate(context.tenantId(), strategyKey, version, actor(context)))));
    }

    /**
     * 回滚策略到目标版本。
     */
    @PostMapping("/{strategyKey}/rollback")
    public Mono<R<RiskStrategyView>> rollback(@PathVariable String strategyKey,
                                              @RequestBody RiskStrategyTransitionRequest request) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.rollback(context.tenantId(), strategyKey,
                        request == null || request.targetVersion() == null ? 0 : request.targetVersion(),
                        actor(context)))));
    }

    /**
     * 暂停当前活跃策略。
     */
    @PostMapping("/{strategyKey}/pause")
    public Mono<R<RiskStrategyView>> pause(@PathVariable String strategyKey,
                                           @RequestBody(required = false) RiskStrategyTransitionRequest request) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.pause(context.tenantId(), strategyKey, actor(context)))));
    }

    /**
     * 对比两个策略版本差异。
     */
    @GetMapping("/{strategyKey}/versions/{left}/diff/{right}")
    public Mono<R<RiskStrategyDiffView>> diffVersions(@PathVariable String strategyKey,
                                                      @PathVariable int left,
                                                      @PathVariable int right) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(strategyService.diff(context.tenantId(), strategyKey, left, right))));
    }

    /**
     * 获取具备策略编排权限的租户上下文。
     */
    private Mono<TenantContext> currentWriter() {
        // 运营人员可以编排和预发策略，但审批仍仅限管理员。
        return currentReader()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk strategy write permission required")));
    }

    /**
     * 获取具备策略读取权限的租户上下文。
     */
    private Mono<TenantContext> currentReader() {
        return current().filter(context -> context.isSuperAdmin()
                        || context.isTenantAdmin()
                        || RoleNames.OPERATOR.equals(context.role()))
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk strategy read permission required")));
    }

    /**
     * 获取具备策略审批权限的租户上下文。
     */
    private Mono<TenantContext> currentApprover() {
        return current().filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk strategy approve permission required")));
    }

    /**
     * 读取当前租户上下文，测试环境未注入解析器时回退系统管理员。
     */
    private Mono<TenantContext> current() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, RoleNames.ADMIN, "system"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk strategy permission required")));
    }

    /**
     * 在线程池执行阻塞型策略治理操作。
     */
    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callable) {
        // 治理服务会修改内存或持久化状态，不能在响应式事件循环上执行。
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从租户上下文获取审计操作者。
     */
    private String actor(TenantContext context) {
        return context.username() == null ? "system" : context.username();
    }
}
