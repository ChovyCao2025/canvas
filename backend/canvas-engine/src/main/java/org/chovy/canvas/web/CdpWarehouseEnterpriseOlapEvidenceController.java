package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceCollectionService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CdpWarehouseEnterpriseOlapEvidenceController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/enterprise-olap/evidence")
public class CdpWarehouseEnterpriseOlapEvidenceController {

    private final CdpWarehouseEnterpriseOlapEvidenceService service;
    private final CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseEnterpriseOlapEvidenceController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseEnterpriseOlapEvidenceController(
            CdpWarehouseEnterpriseOlapEvidenceService service,
            TenantContextResolver tenantContextResolver) {
        this(service, null, tenantContextResolver);
    }

    /**
     * 创建 CdpWarehouseEnterpriseOlapEvidenceController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param collectionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseEnterpriseOlapEvidenceController(
            CdpWarehouseEnterpriseOlapEvidenceService service,
            CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService,
            TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.collectionService = collectionService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 记录CDP 数仓 Enterprise Olap Evidence数据接口，对应 POST 请求。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含记录CDP 数仓 Enterprise Olap Evidence数据后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView>> record(
            @RequestBody CdpWarehouseEnterpriseOlapEvidenceService.EvidenceCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(
                                service.recordOperatorEvidence(context.tenantId(), command, context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Enterprise Olap Evidence 请求接口，对应 GET /latest。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含处理 CDP 数仓 Enterprise Olap Evidence 请求后的业务数据。
     */
    @GetMapping("/latest")
    public Mono<R<CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle>> latest() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.latestEvidence(context.tenantId())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Enterprise Olap Evidence 请求接口，对应 GET /proof。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/proof")
    public Mono<R<List<CdpWarehouseProductionReadinessProofService.ProofEvidence>>> proof() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.proofEvidence(context.tenantId())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Enterprise Olap Evidence 请求接口，对应 POST /collect。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含处理 CDP 数仓 Enterprise Olap Evidence 请求后的业务数据。
     */
    @PostMapping("/collect")
    public Mono<R<CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView>> collect() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(collectionService().run(
                                context.tenantId(), "MANUAL", context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Enterprise Olap Evidence 请求接口，对应 GET /collections。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/collections")
    public Mono<R<List<CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView>>> collections(
            @RequestParam(defaultValue = "20") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(collectionService().recentRuns(
                                context.tenantId(), limit)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 collectionService 流程，围绕 collection service 完成校验、计算或结果组装。
     *
     * @return 返回 collection service 汇总后的集合、分页或映射视图。
     */
    private CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService() {
        if (collectionService == null) {
            throw new IllegalStateException("enterprise OLAP evidence collection service is not configured");
        }
        return collectionService;
    }
}
