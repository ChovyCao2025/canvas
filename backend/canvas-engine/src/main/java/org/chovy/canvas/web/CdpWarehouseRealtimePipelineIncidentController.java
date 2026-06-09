package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * CdpWarehouseRealtimePipelineIncidentController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/realtime/pipelines/incidents")
public class CdpWarehouseRealtimePipelineIncidentController {

    private final CdpWarehouseRealtimePipelineIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseRealtimePipelineIncidentController 实例并注入 web 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimePipelineIncidentController(
            CdpWarehouseRealtimePipelineIncidentService incidentService) {
        this(incidentService, null);
    }

    /**
     * 创建 CdpWarehouseRealtimePipelineIncidentController 实例并注入 web 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseRealtimePipelineIncidentController(
            CdpWarehouseRealtimePipelineIncidentService incidentService,
            TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 扫描 CDP 数仓 Realtime Pipeline Incident风险接口，对应 POST /scan。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 incidentService.scan 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param recentLimit 请求参数，默认值为 5。
     * @return 异步返回统一响应，包含扫描 CDP 数仓 Realtime Pipeline Incident风险后的业务数据。
     */
    @PostMapping("/scan")
    public Mono<R<CdpWarehouseRealtimePipelineIncidentService.ScanResult>> scan(
            @RequestParam(defaultValue = "5") int recentLimit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.scan(tenantId, recentLimit)))
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
}
