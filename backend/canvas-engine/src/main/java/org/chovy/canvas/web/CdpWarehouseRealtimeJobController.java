package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobControlService;
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
 * CdpWarehouseRealtimeJobController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/realtime/jobs")
public class CdpWarehouseRealtimeJobController {

    /** 承接实时作业注册、心跳和控制指令流转逻辑。 */
    private final CdpWarehouseRealtimeJobControlService jobService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseRealtimeJobController 实例并注入 web 场景依赖。
     * @param jobService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeJobController(CdpWarehouseRealtimeJobControlService jobService) {
        this(jobService, null);
    }

    /**
     * 创建 CdpWarehouseRealtimeJobController 实例并注入 web 场景依赖。
     * @param jobService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseRealtimeJobController(CdpWarehouseRealtimeJobControlService jobService,
                                             TenantContextResolver tenantContextResolver) {
        this.jobService = jobService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 CDP 数仓 Realtime Job 请求接口，对应 POST /heartbeats。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 jobService.heartbeat 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Realtime Job 请求后的业务数据。
     */
    @PostMapping("/heartbeats")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobInstanceView>> heartbeat(
            @RequestBody HeartbeatReq req) {
        HeartbeatReq request = req == null ? new HeartbeatReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.heartbeat(normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Realtime Job状态接口，对应 GET /status。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 jobService.status 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param pipelineKey pipeline 唯一键，可选。
     * @param maxHeartbeatAgeSeconds 请求参数，默认值为 300。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含查询 CDP 数仓 Realtime Job状态后的业务数据。
     */
    @GetMapping("/status")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobStatusSummary>> status(
            @RequestParam(required = false) String pipelineKey,
            @RequestParam(defaultValue = "300") long maxHeartbeatAgeSeconds,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.status(normalizeTenant(context), pipelineKey, maxHeartbeatAgeSeconds, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Realtime Job 请求接口，对应 POST /actions。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 jobService.requestAction 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Realtime Job 请求后的业务数据。
     */
    @PostMapping("/actions")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobActionView>> requestAction(
            @RequestBody ActionReq req) {
        ActionReq request = req == null ? new ActionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.requestAction(
                                normalizeTenant(context), request.toCommand(), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Realtime Job 请求接口，对应 GET /actions/pending。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 jobService.pendingActions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param pipelineKey pipeline 唯一键。
     * @param jobKey job 唯一键。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/actions/pending")
    public Mono<R<List<CdpWarehouseRealtimeJobControlService.JobActionView>>> pendingActions(
            @RequestParam String pipelineKey,
            @RequestParam String jobKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.pendingActions(normalizeTenant(context), pipelineKey, jobKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Realtime Job 请求接口，对应 POST /actions/{actionId}/ack。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 jobService.acknowledge 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param actionId action ID。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Realtime Job 请求后的业务数据。
     */
    @PostMapping("/actions/{actionId}/ack")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobActionView>> acknowledge(
            @PathVariable Long actionId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.acknowledge(normalizeTenant(context), actionId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Realtime Job 请求接口，对应 POST /actions/{actionId}/complete。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 jobService.complete 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param actionId action ID。
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Realtime Job 请求后的业务数据。
     */
    @PostMapping("/actions/{actionId}/complete")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobActionView>> complete(
            @PathVariable Long actionId,
            @RequestBody(required = false) CompleteReq req) {
        CompleteReq request = req == null ? new CompleteReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.complete(
                                normalizeTenant(context), actionId, request.getStatus(), request.getResultMessage())))
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
    private String operator(ActionReq request, TenantContext context) {
        if (request != null && request.getRequestedBy() != null && !request.getRequestedBy().isBlank()) {
            return request.getRequestedBy().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    /**
     * HeartbeatReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class HeartbeatReq {
        /** 实时链路唯一键，用于定位契约、作业、Schema 和检查点。 */
        private String pipelineKey;
        /** 实时作业唯一键，用于定位外部引擎任务和控制指令。 */
        private String jobKey;
        /** 实时或表存储引擎类型，用于选择探测、控制和治理适配方式。 */
        private String engineType;
        /** 实时引擎侧作业 ID，用于将平台作业映射到外部运行实例。 */
        private String engineJobId;
        /** 部署引用，用于关联实时作业的发布版本或运行环境。 */
        private String deploymentRef;
        /** 实时作业当前运行状态，用于健康判断和控制台展示。 */
        private String runtimeStatus;
        /** 实时作业期望状态，用于表达启动、停止或暂停目标。 */
        private String desiredStatus;
        /** 实时作业最近心跳时间，用于判断作业是否失联。 */
        private LocalDateTime heartbeatAt;
        /** 实时作业心跳载荷 JSON，用于保存引擎返回的运行指标。 */
        private String heartbeatPayloadJson;
        /** 异常说明，用于展示失败原因和辅助排障。 */
        private String errorMessage;
        /** 责任人名称，用于治理归属、告警通知和问题追踪。 */
        private String ownerName;

        CdpWarehouseRealtimeJobControlService.HeartbeatCommand toCommand() {
            return new CdpWarehouseRealtimeJobControlService.HeartbeatCommand(
                    pipelineKey,
                    jobKey,
                    engineType,
                    engineJobId,
                    deploymentRef,
                    runtimeStatus,
                    desiredStatus,
                    heartbeatAt,
                    heartbeatPayloadJson,
                    errorMessage,
                    ownerName);
        }
    }

    @Data
    /**
     * ActionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class ActionReq {
        /** 实时链路唯一键，用于定位契约、作业、Schema 和检查点。 */
        private String pipelineKey;
        /** 实时作业唯一键，用于定位外部引擎任务和控制指令。 */
        private String jobKey;
        /** 控制动作，用于表达启动、停止、重启等实时作业指令。 */
        private String action;
        /** 发起变更、控制或回滚的原因，用于审批和审计追溯。 */
        private String reason;
        /** 控制指令发起人，用于实时作业操作审计。 */
        private String requestedBy;

        CdpWarehouseRealtimeJobControlService.ActionRequestCommand toCommand() {
            return new CdpWarehouseRealtimeJobControlService.ActionRequestCommand(
                    pipelineKey,
                    jobKey,
                    action,
                    reason);
        }
    }

    @Data
    /**
     * CompleteReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class CompleteReq {
        /** 业务状态，用于控制配置启用、检查结果或回执处理分支。 */
        private String status;
        /** 处理结果说明，用于返回控制动作或回执更新的执行反馈。 */
        private String resultMessage;
    }
}
