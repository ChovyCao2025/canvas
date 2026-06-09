package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CdpWarehouseAvailabilityController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/availability")
public class CdpWarehouseAvailabilityController {

    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseAvailabilityController 实例并注入 web 场景依赖。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityService availabilityService) {
        this(availabilityService, null, null);
    }

    /**
     * 创建 CdpWarehouseAvailabilityController 实例并注入 web 场景依赖。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityService availabilityService,
                                              TenantContextResolver tenantContextResolver) {
        this(availabilityService, tenantContextResolver, null);
    }

    /**
     * 创建 CdpWarehouseAvailabilityController 实例并注入 web 场景依赖。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityService availabilityService,
                                              TenantContextResolver tenantContextResolver,
                                              CdpWarehouseConsumerAvailabilityService consumerAvailabilityService) {
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 CDP 数仓 Availability 请求接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 availabilityService.evaluate 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param from 请求参数，可选。
     * @param to 请求参数，可选。
     * @param mode 请求参数，默认值为 HYBRID。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Availability 请求后的业务数据。
     */
    @GetMapping
    public Mono<R<CdpWarehouseAvailabilityService.AvailabilityDecision>> availability(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "HYBRID") String mode) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(availabilityService.evaluate(tenantId, from, to, mode)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 记录CDP 数仓 Availability数据接口，对应 POST /assets。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含记录CDP 数仓 Availability数据后的业务数据。
     */
    @PostMapping("/assets")
    public Mono<R<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView>> recordAssetAvailability(
            @RequestBody CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().recordAssetAvailability(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Availability列表接口，对应 GET /assets。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param assetType 请求参数，可选。
     * @param assetKey asset 唯一键，可选。
     * @param mode 请求参数，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/assets")
    public Mono<R<List<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView>>> listAssetAvailability(
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String assetKey,
            @RequestParam(required = false) String mode,
            @RequestParam(defaultValue = "50") Integer limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().listAssetAvailability(tenantId, assetType, assetKey, mode, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 CDP 数仓 Availability接口，对应 POST /contracts。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 CDP 数仓 Availability后的业务数据。
     */
    @PostMapping("/contracts")
    public Mono<R<CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView>> upsertContract(
            @RequestBody CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().upsertContract(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Availability列表接口，对应 GET /contracts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param consumerType 请求参数，可选。
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contracts")
    public Mono<R<List<CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView>>> listContracts(
            @RequestParam(required = false) String consumerType,
            @RequestParam(required = false) String status) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().listContracts(tenantId, consumerType, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 评估 CDP 数仓 Availability接口，对应 POST /contracts/{contractKey}/evaluate。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param contractKey contract 唯一键。
     * @param from 请求参数，可选。
     * @param to 请求参数，可选。
     * @return 异步返回统一响应，包含评估 CDP 数仓 Availability后的业务数据。
     */
    @PostMapping("/contracts/{contractKey}/evaluate")
    public Mono<R<CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation>> evaluateContract(
            @PathVariable String contractKey,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().evaluateContract(tenantId, contractKey, from, to)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 consumerService 流程，围绕 consumer service 完成校验、计算或结果组装。
     *
     * @return 返回 consumerService 流程生成的业务结果。
     */
    private CdpWarehouseConsumerAvailabilityService consumerService() {
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        return consumerAvailabilityService;
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
