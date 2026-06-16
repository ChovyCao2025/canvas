package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.lab.RiskSimulationHistoryView;
import org.chovy.canvas.domain.risk.lab.RiskSimulationRequest;
import org.chovy.canvas.domain.risk.lab.RiskSimulationResult;
import org.chovy.canvas.domain.risk.lab.RiskSimulationService;
import org.chovy.canvas.web.risk.dto.RiskSimulationStartRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 风控实验室控制器，在高风险版本晋级前基于租户样本运行离线仿真。
 */
@RestController
@RequestMapping("/canvas/risk/lab")
public class RiskLabController {

    /**
     * defaultsample限制常量，用于保持控制器内部规则一致。
     */
    private static final int DEFAULT_SAMPLE_LIMIT = 1000;

    /**
     * simulation服务，用于承接对应业务能力和领域编排。
     */
    private final RiskSimulationService simulationService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建风控实验室控制器。
     */
    @Autowired
    public RiskLabController(RiskSimulationService simulationService,
                             TenantContextResolver tenantContextResolver) {
        this.simulationService = simulationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 启动一次风控策略离线仿真。
     */
    @PostMapping("/simulations")
    public Mono<R<RiskSimulationResult>> startSimulation(@RequestBody RiskSimulationStartRequest body) {
        return currentOperator().flatMap(context -> Mono.fromCallable(() ->
                R.ok(simulationService.run(toDomain(context, body))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询风控仿真历史。
     */
    @GetMapping("/simulations")
    public Mono<R<List<RiskSimulationHistoryView>>> listSimulations(@RequestParam(required = false) String sceneKey,
                                                                    @RequestParam(defaultValue = "50") int limit) {
        return currentOperator().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(simulationService.listRuns(context.tenantId(), sceneKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 将 HTTP 请求转换为领域仿真请求。
     */
    private RiskSimulationRequest toDomain(TenantContext context, RiskSimulationStartRequest body) {
        RiskSimulationStartRequest request = body == null
                ? new RiskSimulationStartRequest(null, null, null, null, null, null)
                : body;
        int candidateVersion = request.candidateVersion() == null
                ? version(request.version())
                : request.candidateVersion();
        // 仅传入一个版本时，默认与前一个版本作为基线对比。
        return new RiskSimulationRequest(
                context.tenantId(),
                request.sceneKey(),
                request.strategyKey(),
                Math.max(0, candidateVersion - 1),
                candidateVersion,
                request.sampleLimit() == null ? DEFAULT_SAMPLE_LIMIT : request.sampleLimit());
    }

    /**
     * 解析版本号，缺省时使用版本 1。
     */
    private int version(Integer version) {
        return version == null ? 1 : version;
    }

    /**
     * 读取具备实验室权限的租户上下文。
     */
    private Mono<TenantContext> currentOperator() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, RoleNames.ADMIN, "system"));
        }
        // 运营人员可以运行仿真，但样本边界仍由租户上下文决定。
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk lab permission required")))
                .filter(context -> context.isSuperAdmin()
                        || context.isTenantAdmin()
                        || RoleNames.OPERATOR.equals(context.role()))
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk lab permission required")));
    }
}
