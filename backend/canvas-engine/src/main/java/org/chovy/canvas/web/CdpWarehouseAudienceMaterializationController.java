package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationScheduleService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * CdpWarehouseAudienceMaterializationController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/audiences")
public class CdpWarehouseAudienceMaterializationController {

    /** 承接数仓运营任务、补算和巡检类写入操作。 */
    private final AudienceMaterializationOperationsService operationsService;
    /** 承接受众物化调度和可用性窗口的编排逻辑。 */
    private final AudienceMaterializationScheduleService scheduleService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseAudienceMaterializationController 实例并注入 web 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseAudienceMaterializationController(
            AudienceMaterializationOperationsService operationsService) {
        this(operationsService, null, null);
    }

    /**
     * 创建 CdpWarehouseAudienceMaterializationController 实例并注入 web 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseAudienceMaterializationController(
            AudienceMaterializationOperationsService operationsService,
            TenantContextResolver tenantContextResolver) {
        this(operationsService, tenantContextResolver, null);
    }

    /**
     * 创建 CdpWarehouseAudienceMaterializationController 实例并注入 web 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public CdpWarehouseAudienceMaterializationController(
            AudienceMaterializationOperationsService operationsService,
            TenantContextResolver tenantContextResolver,
            AudienceMaterializationScheduleService scheduleService) {
        this.operationsService = operationsService;
        this.scheduleService = scheduleService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 物化 CDP 数仓 Audience Materialization数据接口，对应 POST /{audienceId}/materialize。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.materialize 完成业务处理。
     * 副作用：会触发物化流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param audienceId audience ID。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含物化 CDP 数仓 Audience Materialization数据后的业务数据。
     */
    @PostMapping("/{audienceId}/materialize")
    public Mono<R<AudienceMaterializationService.MaterializationResult>> materialize(
            @PathVariable Long audienceId,
            @RequestBody(required = false) MaterializeReq req) {
        MaterializeReq request = req == null ? new MaterializeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.materialize(
                                normalizeTenant(context), audienceId, operator(request.getOperator(), context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 物化 CDP 数仓 Audience Materialization数据接口，对应 POST /{audienceId}/materialize-gated。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.materializeWithAvailabilityGate 完成业务处理。
     * 副作用：会触发物化流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param audienceId audience ID。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含物化 CDP 数仓 Audience Materialization数据后的业务数据。
     */
    @PostMapping("/{audienceId}/materialize-gated")
    public Mono<R<AudienceMaterializationOperationsService.GatedMaterializationResult>> materializeGated(
            @PathVariable Long audienceId,
            @RequestBody(required = false) GatedMaterializeReq req) {
        GatedMaterializeReq request = req == null ? new GatedMaterializeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.materializeWithAvailabilityGate(
                                normalizeTenant(context),
                                audienceId,
                                request.getFrom(),
                                request.getTo(),
                                request.getMode(),
                                request.isAllowWarn(),
                                operator(request.getOperator(), context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 物化 CDP 数仓 Audience Materialization数据接口，对应 POST /{audienceId}/materialize-contract-gated。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.materializeWithConsumerAvailabilityContract 完成业务处理。
     * 副作用：会触发物化流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param audienceId audience ID。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含物化 CDP 数仓 Audience Materialization数据后的业务数据。
     */
    @PostMapping("/{audienceId}/materialize-contract-gated")
    public Mono<R<AudienceMaterializationOperationsService.ContractGatedMaterializationResult>> materializeContractGated(
            @PathVariable Long audienceId,
            @RequestBody(required = false) ContractGatedMaterializeReq req) {
        ContractGatedMaterializeReq request = req == null ? new ContractGatedMaterializeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.materializeWithConsumerAvailabilityContract(
                                normalizeTenant(context),
                                audienceId,
                                request.getContractKey(),
                                request.getFrom(),
                                request.getTo(),
                                operator(request.getOperator(), context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 回滚 CDP 数仓 Audience Materialization操作接口，对应 POST /{audienceId}/materialization/rollback。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 operationsService.rollback 完成业务处理。
     * 副作用：会执行回滚动作。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param audienceId audience ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含回滚 CDP 数仓 Audience Materialization操作后的业务数据。
     */
    @PostMapping("/{audienceId}/materialization/rollback")
    public Mono<R<AudienceMaterializationOperationsService.RollbackView>> rollback(
            @PathVariable Long audienceId,
            @RequestBody RollbackReq req) {
        RollbackReq request = req == null ? new RollbackReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.rollback(
                                normalizeTenant(context),
                                audienceId,
                                request.getTargetVersion(),
                                operator(request.getOperator(), context),
                                request.getReason())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 刷新 CDP 数仓 Audience Materialization接口，对应 POST /materialization/refresh-due。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 scheduleService.refreshDue 完成业务处理。
     * 副作用：会触发刷新流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含刷新 CDP 数仓 Audience Materialization后的业务数据。
     */
    @PostMapping("/materialization/refresh-due")
    public Mono<R<AudienceMaterializationScheduleService.ScheduledRefreshResult>> refreshDue(
            @RequestBody(required = false) RefreshDueReq req) {
        RefreshDueReq request = req == null ? new RefreshDueReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (scheduleService == null) {
                        throw new IllegalStateException("audience materialization schedule service is not configured");
                    }
                    return R.ok(scheduleService.refreshDue(
                            normalizeTenant(context),
                            LocalDateTime.now(),
                            request.getLimit(),
                            operator(request.getOperator(), context)));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 刷新 CDP 数仓 Audience Materialization接口，对应 POST /materialization/refresh-due-gated。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 scheduleService.refreshDueWithAvailabilityGate 完成业务处理。
     * 副作用：会触发刷新流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含刷新 CDP 数仓 Audience Materialization后的业务数据。
     */
    @PostMapping("/materialization/refresh-due-gated")
    public Mono<R<AudienceMaterializationScheduleService.GatedScheduledRefreshResult>> refreshDueGated(
            @RequestBody(required = false) GatedRefreshDueReq req) {
        GatedRefreshDueReq request = req == null ? new GatedRefreshDueReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (scheduleService == null) {
                        throw new IllegalStateException("audience materialization schedule service is not configured");
                    }
                    LocalDateTime now = request.getNow() == null ? LocalDateTime.now() : request.getNow();
                    return R.ok(scheduleService.refreshDueWithAvailabilityGate(
                            normalizeTenant(context),
                            now,
                            request.getLimit(),
                            operator(request.getOperator(), context),
                            request.getFrom(),
                            request.getTo(),
                            request.getMode(),
                            request.isAllowWarn()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 CDP 数仓 Audience Materialization运行接口，对应 GET /materialization-runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 operationsService.recentRuns 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param audienceId audience ID，可选。
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/materialization-runs")
    public Mono<R<List<AudienceMaterializationOperationsService.RunView>>> recentRuns(
            @RequestParam(required = false) Long audienceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.recentRuns(normalizeTenant(context), audienceId, status, limit)))
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

    /**
     * 解析操作人标识。
     *
     * @param requestedOperator requested operator 参数，用于 operator 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 operator 生成的文本或业务键。
     */
    private String operator(String requestedOperator, TenantContext context) {
        if (requestedOperator != null && !requestedOperator.isBlank()) {
            return requestedOperator.trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    /**
     * MaterializeReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class MaterializeReq {
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * GatedMaterializeReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class GatedMaterializeReq {
        /** 检查或补偿窗口开始时间，用于限定本次治理处理范围。 */
        private LocalDateTime from;
        /** 检查或补偿窗口结束时间，用于限定本次治理处理范围。 */
        private LocalDateTime to;
        /** 执行模式，用于区分自动判定、人工触发或混合巡检流程。 */
        private String mode = "HYBRID";
        /** 是否允许告警级别结果继续通过，用于控制物化发布门禁。 */
        private boolean allowWarn;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * ContractGatedMaterializeReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class ContractGatedMaterializeReq {
        /** 可用性契约唯一键，用于定位消费方或物化任务的 SLA 要求。 */
        private String contractKey;
        /** 检查或补偿窗口开始时间，用于限定本次治理处理范围。 */
        private LocalDateTime from;
        /** 检查或补偿窗口结束时间，用于限定本次治理处理范围。 */
        private LocalDateTime to;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * RollbackReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class RollbackReq {
        /** 目标受众版本号，用于指定回滚或切换到的物化版本。 */
        private Long targetVersion;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
        /** 发起变更、控制或回滚的原因，用于审批和审计追溯。 */
        private String reason;
    }

    @Data
    /**
     * RefreshDueReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class RefreshDueReq {
        /** 本次批处理最大条数，用于控制调度负载和处理节奏。 */
        private int limit;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }

    @Data
    /**
     * GatedRefreshDueReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class GatedRefreshDueReq {
        /** 本次调度或清理计算的基准时间，便于测试和手工重放。 */
        private LocalDateTime now;
        /** 检查或补偿窗口开始时间，用于限定本次治理处理范围。 */
        private LocalDateTime from;
        /** 检查或补偿窗口结束时间，用于限定本次治理处理范围。 */
        private LocalDateTime to;
        /** 执行模式，用于区分自动判定、人工触发或混合巡检流程。 */
        private String mode = "HYBRID";
        /** 是否允许告警级别结果继续通过，用于控制物化发布门禁。 */
        private boolean allowWarn;
        /** 本次批处理最大条数，用于控制调度负载和处理节奏。 */
        private int limit;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }
}
