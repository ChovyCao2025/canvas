package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAggregationService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseBackfillService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseOperationsService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionService;
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

/**
 * CdpWarehouseController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse")
public class CdpWarehouseController {

    /** 承接数仓运营任务、补算和巡检类写入操作。 */
    private final CdpWarehouseOperationsService operationsService;
    /** 承接数仓运行记录、重试和事故数据的保留清理逻辑。 */
    private final CdpWarehouseRetentionService retentionService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseController 实例并注入 web 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseController(CdpWarehouseOperationsService operationsService) {
        this(operationsService, null, null);
    }

    /**
     * 创建 CdpWarehouseController 实例并注入 web 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseController(CdpWarehouseOperationsService operationsService,
                                  TenantContextResolver tenantContextResolver) {
        this(operationsService, null, tenantContextResolver);
    }

    /**
     * 创建 CdpWarehouseController 实例并注入 web 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param retentionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseController(CdpWarehouseOperationsService operationsService,
                                  CdpWarehouseRetentionService retentionService,
                                  TenantContextResolver tenantContextResolver) {
        this.operationsService = operationsService;
        this.retentionService = retentionService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓状态接口，对应 GET /status。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 operationsService.status 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含查询 CDP 数仓状态后的业务数据。
     */
    @GetMapping("/status")
    public Mono<R<CdpWarehouseOperationsService.WarehouseStatus>> status(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.status(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 请求接口，对应 POST /backfill。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.triggerBackfill 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 请求后的业务数据。
     */
    @PostMapping("/backfill")
    public Mono<R<CdpWarehouseBackfillService.BackfillResult>> backfill(@RequestBody BackfillReq req) {
        BackfillReq request = req == null ? new BackfillReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.triggerBackfill(
                                tenantId,
                                request.getLastId(),
                                request.getLimit(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 请求接口，对应 POST /aggregate。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.triggerAggregation 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 请求后的业务数据。
     */
    @PostMapping("/aggregate")
    public Mono<R<CdpWarehouseAggregationService.AggregationResult>> aggregate(@RequestBody AggregateReq req) {
        AggregateReq request = req == null ? new AggregateReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.triggerAggregation(
                                tenantId,
                                request.getFrom(),
                                request.getTo(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 请求接口，对应 GET /offline-cycle/plan。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 operationsService.planOfflineCycle 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param backfillLimit 请求参数，默认值为 1000。
     * @param aggregationWindowMinutes 请求参数，默认值为 30。
     * @param now 请求参数，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 请求后的业务数据。
     */
    @GetMapping("/offline-cycle/plan")
    public Mono<R<CdpWarehouseOperationsService.OfflineCyclePlan>> offlineCyclePlan(
            @RequestParam(defaultValue = "1000") int backfillLimit,
            @RequestParam(defaultValue = "30") int aggregationWindowMinutes,
            @RequestParam(required = false) LocalDateTime now) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.planOfflineCycle(
                                tenantId, now, backfillLimit, aggregationWindowMinutes)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 CDP 数仓运行接口，对应 POST /offline-cycle/run。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.runOfflineCycle 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含触发 CDP 数仓运行后的业务数据。
     */
    @PostMapping("/offline-cycle/run")
    public Mono<R<CdpWarehouseOperationsService.OfflineCycleResult>> offlineCycleRun(
            @RequestBody OfflineCycleReq req) {
        OfflineCycleReq request = req == null ? new OfflineCycleReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.runOfflineCycle(
                                tenantId,
                                request.getNow(),
                                request.getBackfillLimit(),
                                request.getAggregationWindowMinutes(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 请求接口，对应 GET /retention/plan。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param syncRunRetentionDays 请求参数，默认值为 30。
     * @param realtimeRetryRetentionDays 请求参数，默认值为 14。
     * @param resolvedIncidentRetentionDays 请求参数，默认值为 90。
     * @param now 请求参数，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 请求后的业务数据。
     */
    @GetMapping("/retention/plan")
    public Mono<R<CdpWarehouseRetentionService.RetentionPlan>> retentionPlan(
            @RequestParam(defaultValue = "30") int syncRunRetentionDays,
            @RequestParam(defaultValue = "14") int realtimeRetryRetentionDays,
            @RequestParam(defaultValue = "90") int resolvedIncidentRetentionDays,
            @RequestParam(required = false) LocalDateTime now) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(requireRetentionService().plan(
                                tenantId,
                                now,
                                syncRunRetentionDays,
                                realtimeRetryRetentionDays,
                                resolvedIncidentRetentionDays)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 CDP 数仓运行接口，对应 POST /retention/run。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含触发 CDP 数仓运行后的业务数据。
     */
    @PostMapping("/retention/run")
    public Mono<R<CdpWarehouseRetentionService.RetentionCleanupResult>> retentionRun(
            @RequestBody RetentionReq req) {
        RetentionReq request = req == null ? new RetentionReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(requireRetentionService().cleanup(
                                tenantId,
                                request.getNow(),
                                request.getSyncRunRetentionDays(),
                                request.getRealtimeRetryRetentionDays(),
                                request.getResolvedIncidentRetentionDays(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireRetentionService 流程生成的业务结果。
     */
    private CdpWarehouseRetentionService requireRetentionService() {
        if (retentionService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param configured" configured"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("warehouse retention service is not configured");
        }
        return retentionService;
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
     * BackfillReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class BackfillReq {
        /** 增量扫描的上一条记录 ID，用于控制批量推进位置。 */
        private Long lastId;
        /** 本次批处理最大条数，用于控制调度负载和处理节奏。 */
        private int limit = 1000;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * AggregateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class AggregateReq {
        /** 检查或补偿窗口开始时间，用于限定本次治理处理范围。 */
        private LocalDateTime from;
        /** 检查或补偿窗口结束时间，用于限定本次治理处理范围。 */
        private LocalDateTime to;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * OfflineCycleReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class OfflineCycleReq {
        /** 本次调度或清理计算的基准时间，便于测试和手工重放。 */
        private LocalDateTime now;
        /** 单次补算最多处理的记录数，避免补偿任务过载。 */
        private int backfillLimit = 1000;
        /** 聚合窗口分钟数，用于汇总水位、运行和质量信号。 */
        private int aggregationWindowMinutes = 30;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * RetentionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class RetentionReq {
        /** 本次调度或清理计算的基准时间，便于测试和手工重放。 */
        private LocalDateTime now;
        /** 同步运行记录保留天数，用于运营数据清理策略。 */
        private int syncRunRetentionDays = 30;
        /** 实时重试记录保留天数，用于控制失败补偿数据规模。 */
        private int realtimeRetryRetentionDays = 14;
        /** 已解决事故保留天数，用于平衡追溯需要和存储成本。 */
        private int resolvedIncidentRetentionDays = 90;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }
}
