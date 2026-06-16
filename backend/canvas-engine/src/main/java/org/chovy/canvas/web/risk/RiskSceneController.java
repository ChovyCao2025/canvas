package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.governance.RiskSceneService;
import org.chovy.canvas.domain.risk.governance.RiskSceneView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 租户级风控场景接口，为策略工作台和调用方提供可用场景目录。
 */
@RestController
@RequestMapping("/canvas/risk/scenes")
public class RiskSceneController {

    /**
     * 场景服务，用于承接对应业务能力和领域编排。
     */
    private final RiskSceneService sceneService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建风控场景控制器。
     */
    @Autowired
    public RiskSceneController(RiskSceneService sceneService,
                               TenantContextResolver tenantContextResolver) {
        this.sceneService = sceneService;
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 查询当前租户可用的风控场景。
     */
    @GetMapping
    public Mono<R<List<RiskSceneView>>> listScenes() {
        return currentReader().flatMap(context -> Mono.fromCallable(() ->
                R.ok(sceneService.listScenes(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 读取具备风控场景查看权限的租户上下文。
     */
    private Mono<TenantContext> currentReader() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, RoleNames.ADMIN, "system"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk scene read permission required")))
                .filter(context -> context.isSuperAdmin()
                        || context.isTenantAdmin()
                        || RoleNames.OPERATOR.equals(context.role()))
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk scene read permission required")));
    }
}
