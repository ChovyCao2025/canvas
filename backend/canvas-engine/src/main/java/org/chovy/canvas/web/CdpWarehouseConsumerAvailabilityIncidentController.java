package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * CdpWarehouseConsumerAvailabilityIncidentController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/availability/consumer-incidents")
public class CdpWarehouseConsumerAvailabilityIncidentController {

    /** 承接数仓事故相关的判定、创建和状态流转逻辑。 */
    private final CdpWarehouseConsumerAvailabilityIncidentService incidentService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseConsumerAvailabilityIncidentController 实例并注入 web 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseConsumerAvailabilityIncidentController(
            CdpWarehouseConsumerAvailabilityIncidentService incidentService) {
        this(incidentService, null);
    }

    /**
     * 创建 CdpWarehouseConsumerAvailabilityIncidentController 实例并注入 web 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseConsumerAvailabilityIncidentController(
            CdpWarehouseConsumerAvailabilityIncidentService incidentService,
            TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 扫描 CDP 数仓 Consumer Availability Incident风险接口，对应 POST /scan。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 incidentService.scan 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体，可选。
     * @return 异步返回统一响应，包含扫描 CDP 数仓 Consumer Availability Incident风险后的业务数据。
     */
    @PostMapping("/scan")
    public Mono<R<CdpWarehouseConsumerAvailabilityIncidentService.ScanResult>> scan(
            @RequestBody(required = false) ScanReq req) {
        ScanReq request = req == null ? new ScanReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(incidentService.scan(
                                normalizeTenant(context),
                                request.getContractKey(),
                                request.getConsumerType(),
                                request.getFrom(),
                                request.getTo(),
                                operator(request.getOperator(), context))))
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
     * ScanReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class ScanReq {
        /** 可用性契约唯一键，用于定位消费方或物化任务的 SLA 要求。 */
        private String contractKey;
        /** 消费方类型，用于区分 BI、投放、API 等下游可用性场景。 */
        private String consumerType;
        /** 检查或补偿窗口开始时间，用于限定本次治理处理范围。 */
        private LocalDateTime from;
        /** 检查或补偿窗口结束时间，用于限定本次治理处理范围。 */
        private LocalDateTime to;
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }
}
