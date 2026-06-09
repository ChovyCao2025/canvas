package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseIncidentService;
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
 * CdpWarehouseIncidentController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/incidents")
public class CdpWarehouseIncidentController {

    /** 承接数仓事故相关的判定、创建和状态流转逻辑。 */
    private final CdpWarehouseIncidentService incidentService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseIncidentController 实例并注入 web 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseIncidentController(CdpWarehouseIncidentService incidentService) {
        this(incidentService, null);
    }

    /**
     * 创建 CdpWarehouseIncidentController 实例并注入 web 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseIncidentController(CdpWarehouseIncidentService incidentService,
                                          TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Incident列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 incidentService.listIncidents 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWarehouseIncidentService.IncidentView>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.listIncidents(tenantId, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Incident 请求接口，对应 POST /{id}/ack。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 incidentService.acknowledge 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Incident 请求后的业务数据。
     */
    @PostMapping("/{id}/ack")
    public Mono<R<Boolean>> acknowledge(@PathVariable Long id, @RequestBody OperatorReq req) {
        OperatorReq request = req == null ? new OperatorReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.acknowledge(tenantId, id, request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Incident 请求接口，对应 POST /{id}/resolve。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 incidentService.resolve 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Incident 请求后的业务数据。
     */
    @PostMapping("/{id}/resolve")
    public Mono<R<Boolean>> resolve(@PathVariable Long id, @RequestBody OperatorReq req) {
        OperatorReq request = req == null ? new OperatorReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.resolve(tenantId, id, request.getOperator())))
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
     * OperatorReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class OperatorReq {
        /** 执行本次请求的操作人，用于审计治理动作和回溯来源。 */
        private String operator;
    }
}
