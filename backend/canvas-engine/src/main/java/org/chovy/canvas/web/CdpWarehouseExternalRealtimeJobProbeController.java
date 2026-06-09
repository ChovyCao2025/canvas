package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeService;
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
 * CdpWarehouseExternalRealtimeJobProbeController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/realtime/job-probes")
public class CdpWarehouseExternalRealtimeJobProbeController {

    /** 承接外部实时作业探针目标的注册和健康采集逻辑。 */
    private final CdpWarehouseExternalRealtimeJobProbeService probeService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseExternalRealtimeJobProbeController 实例并注入 web 场景依赖。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseExternalRealtimeJobProbeController(
            CdpWarehouseExternalRealtimeJobProbeService probeService) {
        this(probeService, null);
    }

    /**
     * 创建 CdpWarehouseExternalRealtimeJobProbeController 实例并注入 web 场景依赖。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseExternalRealtimeJobProbeController(
            CdpWarehouseExternalRealtimeJobProbeService probeService,
            TenantContextResolver tenantContextResolver) {
        this.probeService = probeService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 获取 CDP 数仓 External Realtime Job Probe详情接口，对应 POST /targets。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 probeService.upsertTarget 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含获取 CDP 数仓 External Realtime Job Probe详情后的业务数据。
     */
    @PostMapping("/targets")
    public Mono<R<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>> upsertTarget(
            @RequestBody TargetReq req) {
        TargetReq request = req == null ? new TargetReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.upsertTarget(normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 External Realtime Job Probe列表接口，对应 GET /targets。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 probeService.listTargets 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param includeDisabled 请求参数，默认值为 false。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/targets")
    public Mono<R<List<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>>> listTargets(
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.listTargets(normalizeTenant(context), includeDisabled, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 External Realtime Job Probe 请求接口，对应 POST /targets/{targetId}/enabled。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 probeService.setEnabled 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param targetId target ID。
     * @param enabled 请求参数。
     * @return 异步返回统一响应，包含处理 CDP 数仓 External Realtime Job Probe 请求后的业务数据。
     */
    @PostMapping("/targets/{targetId}/enabled")
    public Mono<R<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>> setEnabled(
            @PathVariable Long targetId,
            @RequestParam boolean enabled) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.setEnabled(normalizeTenant(context), targetId, enabled)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 扫描 CDP 数仓 External Realtime Job Probe风险接口，对应 POST /scan。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 probeService.scan, CdpWarehouseExternalRealtimeJobProbeService.ScanCommand 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param targetId target ID，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含扫描 CDP 数仓 External Realtime Job Probe风险后的业务数据。
     */
    @PostMapping("/scan")
    public Mono<R<CdpWarehouseExternalRealtimeJobProbeService.ScanSummary>> scan(
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.scan(
                                normalizeTenant(context),
                                new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(targetId, limit))))
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

    @Data
    /**
     * TargetReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class TargetReq {
        /** 实时链路唯一键，用于定位契约、作业、Schema 和检查点。 */
        private String pipelineKey;
        /** 实时作业唯一键，用于定位外部引擎任务和控制指令。 */
        private String jobKey;
        /** 实时或表存储引擎类型，用于选择探测、控制和治理适配方式。 */
        private String engineType;
        /** 外部作业探针访问地址，用于拉取运行状态和心跳信息。 */
        private String endpointUrl;
        /** 外部作业认证引用，避免在请求体中直接传递敏感凭据。 */
        private String authRef;
        /** 外部调度或计算引擎中的作业标识，用于跨系统关联。 */
        private String externalJobId;
        /** 外部连接器名称，用于识别探针采集的系统来源。 */
        private String connectorName;
        /** 部署引用，用于关联实时作业的发布版本或运行环境。 */
        private String deploymentRef;
        /** 是否启用探针目标，决定调度采集时是否纳入检查。 */
        private Boolean enabled;
        /** 责任人名称，用于治理归属、告警通知和问题追踪。 */
        private String ownerName;
        /** 探针数据允许的最大陈旧秒数，超过后视为健康风险。 */
        private Integer maxStalenessSeconds;
        /** 扩展配置 JSON，承载引擎、链路或探针的非标准参数。 */
        private String configJson;

        CdpWarehouseExternalRealtimeJobProbeService.TargetCommand toCommand() {
            return new CdpWarehouseExternalRealtimeJobProbeService.TargetCommand(
                    pipelineKey,
                    jobKey,
                    engineType,
                    endpointUrl,
                    authRef,
                    externalJobId,
                    connectorName,
                    deploymentRef,
                    enabled,
                    ownerName,
                    maxStalenessSeconds,
                    configJson);
        }
    }
}
