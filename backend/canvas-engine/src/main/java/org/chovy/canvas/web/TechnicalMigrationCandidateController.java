package org.chovy.canvas.web;

import org.chovy.canvas.architecture.TechnicalMigrationCandidateService;
import org.chovy.canvas.architecture.TechnicalMigrationCandidateEvidenceRecord;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * TechnicalMigrationCandidateController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/architecture/migration-candidates")
public class TechnicalMigrationCandidateController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final TechnicalMigrationCandidateService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 TechnicalMigrationCandidateController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public TechnicalMigrationCandidateController(TechnicalMigrationCandidateService service,
                                                 TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 技术迁移候选项 请求接口，对应 POST /evidence。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含处理 技术迁移候选项 请求后的业务数据。
     */
    @PostMapping("/evidence")
    public Mono<R<TechnicalMigrationCandidateEvidenceRecord>> register(
            @RequestBody TechnicalMigrationCandidateService.EvidenceRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.register(context, request)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
