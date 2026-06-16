package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;
import org.springframework.beans.factory.annotation.Autowired;
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
 * CdpWarehousePrivacyTombstoneController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/privacy/tombstones")
public class CdpWarehousePrivacyTombstoneController {

    /**
     * tombstone服务，用于承接对应业务能力和领域编排。
     */
    private final CdpWarehousePrivacyTombstoneService tombstoneService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehousePrivacyTombstoneController 实例并注入 web 场景依赖。
     * @param tombstoneService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyTombstoneController(CdpWarehousePrivacyTombstoneService tombstoneService) {
        this(tombstoneService, null);
    }

    /**
     * 创建 CdpWarehousePrivacyTombstoneController 实例并注入 web 场景依赖。
     * @param tombstoneService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehousePrivacyTombstoneController(CdpWarehousePrivacyTombstoneService tombstoneService,
                                                  TenantContextResolver tenantContextResolver) {
        this.tombstoneService = tombstoneService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建 CDP 数仓 Privacy Tombstone接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tombstoneService.create 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 CDP 数仓 Privacy Tombstone后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneView>> create(
            @RequestBody CdpWarehousePrivacyTombstoneService.TombstoneCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.create(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 CDP 数仓 Privacy Tombstone接口，对应 POST /from-erasure-request。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tombstoneService.createFromErasureRequest 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 CDP 数仓 Privacy Tombstone后的业务数据。
     */
    @PostMapping("/from-erasure-request")
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneView>> createFromErasureRequest(
            @RequestBody CdpWarehousePrivacyTombstoneService.ErasureRequestTombstoneCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.createFromErasureRequest(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Privacy Tombstone 请求接口，对应 POST /{id}/revoke。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tombstoneService.revoke 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Privacy Tombstone 请求后的业务数据。
     */
    @PostMapping("/{id}/revoke")
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneView>> revoke(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyTombstoneService.RevokeCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.revoke(tenantId, id, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Privacy Tombstone列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tombstoneService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWarehousePrivacyTombstoneService.TombstoneView>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.list(tenantId, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Privacy Tombstone 请求接口，对应 GET /decision。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tombstoneService.decide 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param subjectType 请求参数，默认值为 USER_ID。
     * @param subjectValue 请求参数。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Privacy Tombstone 请求后的业务数据。
     */
    @GetMapping("/decision")
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneDecision>> decision(
            @RequestParam(defaultValue = "USER_ID") String subjectType,
            @RequestParam String subjectValue) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.decide(tenantId, subjectType, subjectValue)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
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
}
