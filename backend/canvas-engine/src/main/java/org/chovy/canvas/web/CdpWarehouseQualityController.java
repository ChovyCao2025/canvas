package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
 * CdpWarehouseQualityController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/quality")
public class CdpWarehouseQualityController {

    /** 承接数仓质量校验和补偿检查的业务逻辑。 */
    private final CdpWarehouseQualityService qualityService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseQualityController 实例并注入 web 场景依赖。
     * @param qualityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseQualityController(CdpWarehouseQualityService qualityService) {
        this(qualityService, null);
    }

    /**
     * 创建 CdpWarehouseQualityController 实例并注入 web 场景依赖。
     * @param qualityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseQualityController(CdpWarehouseQualityService qualityService,
                                         TenantContextResolver tenantContextResolver) {
        this.qualityService = qualityService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Quality列表接口，对应 GET /checks。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 qualityService.recentChecks 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/checks")
    public Mono<R<List<CdpWarehouseQualityService.QualityCheckResult>>> listChecks(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(qualityService.recentChecks(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Quality 请求接口，对应 POST /reconcile-ods。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 qualityService.reconcileOds 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Quality 请求后的业务数据。
     */
    @PostMapping("/reconcile-ods")
    public Mono<R<CdpWarehouseQualityService.QualityCheckResult>> reconcileOds(
            @RequestBody ReconcileOdsReq req) {
        ReconcileOdsReq request = req == null ? new ReconcileOdsReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(qualityService.reconcileOds(
                                tenantId,
                                request.getFrom(),
                                request.getTo(),
                                request.getTolerance(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Quality 请求接口，对应 POST /aggregate-lag。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 qualityService.checkAggregateLag 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Quality 请求后的业务数据。
     */
    @PostMapping("/aggregate-lag")
    public Mono<R<CdpWarehouseQualityService.QualityCheckResult>> aggregateLag(
            @RequestBody AggregateLagReq req) {
        AggregateLagReq request = req == null ? new AggregateLagReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(qualityService.checkAggregateLag(
                                tenantId,
                                request.getNow(),
                                request.getMaxLagMinutes(),
                                request.getOperator())))
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

    @Data
    /**
     * ReconcileOdsReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class ReconcileOdsReq {
        /** 检查或补偿窗口开始时间，用于限定本次治理处理范围。 */
        private LocalDateTime from;
        /** 检查或补偿窗口结束时间，用于限定本次治理处理范围。 */
        private LocalDateTime to;
        /** 质量检查允许的数量差异容忍值，用于判断是否触发异常。 */
        private long tolerance;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * AggregateLagReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class AggregateLagReq {
        /** 本次调度或清理计算的基准时间，便于测试和手工重放。 */
        private LocalDateTime now;
        /** 水位或同步结果允许的最大延迟分钟数。 */
        private long maxLagMinutes = 30;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }
}
