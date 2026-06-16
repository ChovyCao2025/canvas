package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseTableGovernanceService;
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
 * CdpWarehouseTableGovernanceController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/tables")
public class CdpWarehouseTableGovernanceController {

    /** 承接数仓治理配置、校验和评估的业务逻辑。 */
    private final CdpWarehouseTableGovernanceService governanceService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseTableGovernanceController 实例并注入 web 场景依赖。
     * @param governanceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseTableGovernanceController(CdpWarehouseTableGovernanceService governanceService) {
        this(governanceService, null);
    }

    /**
     * 创建 CdpWarehouseTableGovernanceController 实例并注入 web 场景依赖。
     * @param governanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseTableGovernanceController(CdpWarehouseTableGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver) {
        this.governanceService = governanceService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Table Governance列表接口，对应 GET /contracts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 governanceService.listContracts 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param layer 请求参数，可选。
     * @param lifecycleStatus 请求参数，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contracts")
    public Mono<R<List<CdpWarehouseTableGovernanceService.TableContractView>>> listContracts(
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String lifecycleStatus) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.listContracts(
                                normalizeTenant(context), layer, lifecycleStatus)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 CDP 数仓 Table Governance接口，对应 POST /contracts。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.upsertContract 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建或更新 CDP 数仓 Table Governance后的业务数据。
     */
    @PostMapping("/contracts")
    public Mono<R<CdpWarehouseTableGovernanceService.TableContractView>> upsertContract(
            @RequestBody TableContractReq req) {
        TableContractReq request = req == null ? new TableContractReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.upsertContract(
                                normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Table Governance 请求接口，对应 POST /contracts/{tableKey}/inspect。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.inspectContract 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tableKey table 唯一键。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Table Governance 请求后的业务数据。
     */
    @PostMapping("/contracts/{tableKey}/inspect")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionReport>> inspectContract(
            @PathVariable String tableKey,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectContract(
                                normalizeTenant(context), tableKey, operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Table Governance 请求接口，对应 POST /inspect。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.inspectAll 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Table Governance 请求后的业务数据。
     */
    @PostMapping("/inspect")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionSummary>> inspectAll(
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectAll(
                                normalizeTenant(context), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Table Governance 请求接口，对应 POST /contracts/{tableKey}/inspect-live。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.inspectLiveContract 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tableKey table 唯一键。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Table Governance 请求后的业务数据。
     */
    @PostMapping("/contracts/{tableKey}/inspect-live")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionReport>> inspectLiveContract(
            @PathVariable String tableKey,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectLiveContract(
                                normalizeTenant(context), tableKey, operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Table Governance 请求接口，对应 POST /inspect-live。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.inspectLiveAll 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Table Governance 请求后的业务数据。
     */
    @PostMapping("/inspect-live")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionSummary>> inspectLiveAll(
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectLiveAll(
                                normalizeTenant(context), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Table Governance 请求接口，对应 POST /contracts/{tableKey}/remediation-plan。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.planRemediation 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tableKey table 唯一键。
     * @param live 请求参数，默认值为 true。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Table Governance 请求后的业务数据。
     */
    @PostMapping("/contracts/{tableKey}/remediation-plan")
    public Mono<R<CdpWarehouseTableGovernanceService.TableRemediationPlan>> remediationPlan(
            @PathVariable String tableKey,
            @RequestParam(defaultValue = "true") boolean live,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.planRemediation(
                                normalizeTenant(context), tableKey, live, operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Table Governance 请求接口，对应 POST /remediation-plan。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 governanceService.planAllRemediation 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param live 请求参数，默认值为 true。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Table Governance 请求后的业务数据。
     */
    @PostMapping("/remediation-plan")
    public Mono<R<CdpWarehouseTableGovernanceService.RemediationSummary>> remediationPlanAll(
            @RequestParam(defaultValue = "true") boolean live,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.planAllRemediation(
                                normalizeTenant(context), live, operator(request, context))))
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
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 operator 生成的文本或业务键。
     */
    private String operator(InspectionReq request, TenantContext context) {
        if (request != null && request.getOperator() != null && !request.getOperator().isBlank()) {
            return request.getOperator().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    /**
     * TableContractReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class TableContractReq {
        /** 数仓表契约唯一键，用于定位治理配置和巡检记录。 */
        private String tableKey;
        /** 数据集唯一键，用于关联目录、指标、字段治理和表契约。 */
        private String datasetKey;
        /** 数仓分层标识，用于区分 ODS、DWD、DWS、ADS 等治理边界。 */
        private String layer;
        /** 物理表或字段所在对象名称，用于对接实际存储引擎。 */
        private String physicalName;
        /** 实时或表存储引擎类型，用于选择探测、控制和治理适配方式。 */
        private String engineType;
        /** 期望 DDL 资产路径，用于表结构治理巡检比对。 */
        private String ddlAssetPath;
        /** 分区列名称，用于校验表分区设计和数据生命周期策略。 */
        private String partitionColumn;
        /** 分区粒度，用于判断表分区是否符合治理规范。 */
        private String partitionGranularity;
        /** 数据保留天数，用于生命周期治理和清理策略校验。 */
        private Integer retentionDays;
        /** 副本数量，用于校验存储可用性和成本约束。 */
        private Integer replicaCount;
        /** 分桶数量，用于校验查询性能和数据分布策略。 */
        private Integer bucketCount;
        /** 分布列配置，用于校验表的分桶或分片策略。 */
        private String distributionColumns;
        /** 存储策略，用于描述冷热分层、介质或压缩等治理要求。 */
        private String storagePolicy;
        /** 生命周期状态，用于控制治理对象是否可上线、运行或下线。 */
        private String lifecycleStatus;
        /** 责任人名称，用于治理归属、告警通知和问题追踪。 */
        private String ownerName;
        /** 补充说明，用于记录配置目的、血缘关系或治理背景。 */
        private String description;
        /** 期望表属性 JSON，用于巡检实际建表属性是否漂移。 */
        private String expectedPropertiesJson;

        /**
         * 执行 目标command 对应的内部处理流程。
         * @return 返回内部处理结果
         */
        CdpWarehouseTableGovernanceService.TableContractCommand toCommand() {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new CdpWarehouseTableGovernanceService.TableContractCommand(
                    tableKey,
                    datasetKey,
                    layer,
                    physicalName,
                    engineType,
                    ddlAssetPath,
                    partitionColumn,
                    partitionGranularity,
                    retentionDays,
                    replicaCount,
                    bucketCount,
                    distributionColumns,
                    storagePolicy,
                    lifecycleStatus,
                    ownerName,
                    description,
                    expectedPropertiesJson);
        }
    }

    @Data
    /**
     * InspectionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class InspectionReq {
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }
}
