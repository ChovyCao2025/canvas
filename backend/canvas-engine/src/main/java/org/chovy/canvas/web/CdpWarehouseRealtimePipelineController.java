package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineService;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * CdpWarehouseRealtimePipelineController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/realtime/pipelines")
public class CdpWarehouseRealtimePipelineController {

    /** 承接实时链路契约、检查点和运行状态汇总逻辑。 */
    private final CdpWarehouseRealtimePipelineService pipelineService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseRealtimePipelineController 实例并注入 web 场景依赖。
     * @param pipelineService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimePipelineController(CdpWarehouseRealtimePipelineService pipelineService) {
        this(pipelineService, null);
    }

    /**
     * 创建 CdpWarehouseRealtimePipelineController 实例并注入 web 场景依赖。
     * @param pipelineService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseRealtimePipelineController(CdpWarehouseRealtimePipelineService pipelineService,
                                                  TenantContextResolver tenantContextResolver) {
        this.pipelineService = pipelineService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Realtime Pipeline列表接口，对应 GET /contracts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 pipelineService.listPipelines 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param lifecycleStatus 请求参数，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contracts")
    public Mono<R<List<CdpWarehouseRealtimePipelineService.PipelineContractView>>> listContracts(
            @RequestParam(required = false) String lifecycleStatus) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.listPipelines(normalizeTenant(context), lifecycleStatus)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 CDP 数仓 Realtime Pipeline接口，对应 POST /contracts。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 pipelineService.upsertPipeline 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建或更新 CDP 数仓 Realtime Pipeline后的业务数据。
     */
    @PostMapping("/contracts")
    public Mono<R<CdpWarehouseRealtimePipelineService.PipelineContractView>> upsertContract(
            @RequestBody PipelineContractReq req) {
        PipelineContractReq request = req == null ? new PipelineContractReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.upsertPipeline(
                                normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 生成 CDP 数仓 Realtime Pipeline 报告接口，对应 POST /checkpoints。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 pipelineService.reportCheckpoint 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含生成 CDP 数仓 Realtime Pipeline 报告后的业务数据。
     */
    @PostMapping("/checkpoints")
    public Mono<R<CdpWarehouseRealtimePipelineService.CheckpointReport>> reportCheckpoint(
            @RequestBody CheckpointReq req) {
        CheckpointReq request = req == null ? new CheckpointReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.reportCheckpoint(
                                normalizeTenant(context), request.toCommand(operator(request, context)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Realtime Pipeline状态接口，对应 GET /status。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 pipelineService.status 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param recentLimit 请求参数，默认值为 5。
     * @return 异步返回统一响应，包含查询 CDP 数仓 Realtime Pipeline状态后的业务数据。
     */
    @GetMapping("/status")
    public Mono<R<CdpWarehouseRealtimePipelineService.PipelineStatusSummary>> status(
            @RequestParam(defaultValue = "5") int recentLimit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.status(normalizeTenant(context), recentLimit)))
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
    private String operator(CheckpointReq request, TenantContext context) {
        if (request != null && request.getReportedBy() != null && !request.getReportedBy().isBlank()) {
            return request.getReportedBy().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    /**
     * PipelineContractReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class PipelineContractReq {
        /** 实时链路唯一键，用于定位契约、作业、Schema 和检查点。 */
        private String pipelineKey;
        /** 面向运营和治理人员展示的可读名称。 */
        private String displayName;
        /** 实时链路来源类型，用于识别 Kafka、CDC 或外部事件源。 */
        private String sourceType;
        /** 来源系统引用，用于关联实时链路的上游资源。 */
        private String sourceRef;
        /** 来源 Topic 名称，用于定位实时消费入口。 */
        private String sourceTopic;
        /** 消费组名称，用于控制实时链路的消费位点隔离。 */
        private String consumerGroup;
        /** 处理器类型，用于选择实时链路的数据转换逻辑。 */
        private String processorType;
        /** 写入目标类型，用于识别 Doris、湖仓或消息队列等下游。 */
        private String sinkType;
        /** 写入目标引用，用于关联实时链路的下游资源。 */
        private String sinkRef;
        /** 投递语义，用于描述至少一次、恰好一次等处理保证。 */
        private String deliverySemantics;
        /** 检查点间隔秒数，用于控制实时链路状态提交频率。 */
        private Integer checkpointIntervalSeconds;
        /** 允许的最大实时延迟毫秒数，用于触发链路健康告警。 */
        private Long maxLagMs;
        /** 检查点允许的最大存活秒数，用于判断链路是否停止推进。 */
        private Integer maxCheckpointAgeSeconds;
        /** 生命周期状态，用于控制治理对象是否可上线、运行或下线。 */
        private String lifecycleStatus;
        /** 责任人名称，用于治理归属、告警通知和问题追踪。 */
        private String ownerName;
        /** 扩展配置 JSON，承载引擎、链路或探针的非标准参数。 */
        private String configJson;

        CdpWarehouseRealtimePipelineService.PipelineContractCommand toCommand() {
            return new CdpWarehouseRealtimePipelineService.PipelineContractCommand(
                    pipelineKey,
                    displayName,
                    sourceType,
                    sourceRef,
                    sourceTopic,
                    consumerGroup,
                    processorType,
                    sinkType,
                    sinkRef,
                    deliverySemantics,
                    checkpointIntervalSeconds,
                    maxLagMs,
                    maxCheckpointAgeSeconds,
                    lifecycleStatus,
                    ownerName,
                    configJson);
        }
    }

    @Data
    /**
     * CheckpointReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class CheckpointReq {
        /** 实时链路唯一键，用于定位契约、作业、Schema 和检查点。 */
        private String pipelineKey;
        /** 检查点唯一标识，用于幂等记录实时链路进度。 */
        private String checkpointId;
        /** 来源分区标识，用于记录检查点对应的消费分片。 */
        private String sourcePartition;
        /** 来源消费位点，用于追踪实时链路读取进度。 */
        private String sourceOffset;
        /** 已提交位点，用于判断处理进度和重启恢复位置。 */
        private String committedOffset;
        /** 业务水位时间，用于衡量实时数据的新鲜度。 */
        private String watermarkTime;
        /** 检查点产生时间，用于记录实时链路状态提交时刻。 */
        private String checkpointTime;
        /** 检查点延迟毫秒数，用于计算实时链路健康状态。 */
        private Long lagMs;
        /** 检查点覆盖的处理行数，用于观测吞吐和异常波动。 */
        private Long rowCount;
        /** 业务状态，用于控制配置启用、检查结果或回执处理分支。 */
        private String status;
        /** 异常说明，用于展示失败原因和辅助排障。 */
        private String errorMessage;
        /** 检查点上报方，用于记录自动探针或人工补报来源。 */
        private String reportedBy;
        /** 来源 Schema 版本，用于追踪实时链路输入结构。 */
        private String sourceSchemaVersion;
        /** 目标 Schema 版本，用于追踪实时链路输出结构。 */
        private String sinkSchemaVersion;

        CdpWarehouseRealtimePipelineService.CheckpointCommand toCommand(String operator) {
            return new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                    pipelineKey,
                    checkpointId,
                    sourcePartition,
                    sourceOffset,
                    committedOffset,
                    parseDateTime(watermarkTime, "watermarkTime"),
                    parseDateTime(checkpointTime, "checkpointTime"),
                    lagMs,
                    rowCount,
                    status,
                    errorMessage,
                    operator,
                    sourceSchemaVersion,
                    sinkSchemaVersion);
        }

        /**
         * 解析并校验输入数据。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @param fieldName 名称文本，用于展示或唯一性校验。
         * @return 返回解析、归一化或安全处理后的值。
         */
        private static LocalDateTime parseDateTime(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String trimmed = value.trim();
            try {
                return LocalDateTime.parse(trimmed);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DateTimeParseException localFailure) {
                try {
                    return OffsetDateTime.parse(trimmed)
                            .atZoneSameInstant(ZoneId.systemDefault())
                            .toLocalDateTime();
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                } catch (DateTimeParseException offsetFailure) {
                    throw new IllegalArgumentException(fieldName + " must be ISO-8601 datetime", offsetFailure);
                }
            }
        }
    }
}
