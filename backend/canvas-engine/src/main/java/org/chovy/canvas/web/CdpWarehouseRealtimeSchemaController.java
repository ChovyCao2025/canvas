package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CdpWarehouseRealtimeSchemaController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/realtime/schemas")
public class CdpWarehouseRealtimeSchemaController {

    /** 承接实时链路 Schema 注册、兼容性策略和启用状态管理。 */
    private final CdpWarehouseRealtimeSchemaService schemaService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseRealtimeSchemaController 实例并注入 web 场景依赖。
     * @param schemaService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeSchemaController(CdpWarehouseRealtimeSchemaService schemaService) {
        this(schemaService, null);
    }

    /**
     * 创建 CdpWarehouseRealtimeSchemaController 实例并注入 web 场景依赖。
     * @param schemaService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseRealtimeSchemaController(CdpWarehouseRealtimeSchemaService schemaService,
                                                TenantContextResolver tenantContextResolver) {
        this.schemaService = schemaService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 CDP 数仓 Realtime Schema 请求接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 schemaService.register 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Realtime Schema 请求后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWarehouseRealtimeSchemaService.SchemaVersionView>> register(
            @RequestBody SchemaVersionReq req) {
        SchemaVersionReq request = req == null ? new SchemaVersionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(schemaService.register(
                                normalizeTenant(context), request.toCommand(), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Realtime Schema列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 schemaService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param pipelineKey pipeline 唯一键。
     * @param schemaRole 请求参数，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWarehouseRealtimeSchemaService.SchemaVersionView>>> list(
            @RequestParam String pipelineKey,
            @RequestParam(required = false) String schemaRole,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(schemaService.list(normalizeTenant(context), pipelineKey, schemaRole, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Realtime Schema 请求接口，对应 GET /latest。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 schemaService.latest 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param pipelineKey pipeline 唯一键。
     * @param schemaRole 请求参数。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Realtime Schema 请求后的业务数据。
     */
    @GetMapping("/latest")
    public Mono<R<CdpWarehouseRealtimeSchemaService.SchemaVersionView>> latest(
            @RequestParam String pipelineKey,
            @RequestParam String schemaRole) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(schemaService.latest(normalizeTenant(context), pipelineKey, schemaRole)))
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
    private String operator(SchemaVersionReq request, TenantContext context) {
        if (request != null && request.getRegisteredBy() != null && !request.getRegisteredBy().isBlank()) {
            return request.getRegisteredBy().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    /**
     * SchemaVersionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class SchemaVersionReq {
        /** 实时链路唯一键，用于定位契约、作业、Schema 和检查点。 */
        private String pipelineKey;
        /** Schema 在实时链路中的角色，用于区分源端、处理端或目标端结构。 */
        private String schemaRole;
        /** Schema 版本号，用于兼容性校验和链路变更追踪。 */
        private String schemaVersion;
        /** Schema 结构 JSON，用于注册实时数据格式或目录数据集字段。 */
        private String schemaJson;
        /** 兼容性策略，用于判定新 Schema 是否允许上线。 */
        private String compatibilityPolicy;
        /** 是否作为当前有效配置参与目录、血缘或 Schema 治理。 */
        private Boolean active;
        /** 注册实时 Schema 的人员标识，用于审计来源和责任归属。 */
        private String registeredBy;

        /**
         * 执行 目标command 对应的内部处理流程。
         * @return 返回内部处理结果
         */
        CdpWarehouseRealtimeSchemaService.SchemaVersionCommand toCommand() {
            return new CdpWarehouseRealtimeSchemaService.SchemaVersionCommand(
                    pipelineKey,
                    schemaRole,
                    schemaVersion,
                    schemaJson,
                    compatibilityPolicy,
                    active);
        }
    }
}
