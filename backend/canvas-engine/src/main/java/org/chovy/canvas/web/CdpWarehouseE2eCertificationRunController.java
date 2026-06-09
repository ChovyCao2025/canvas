package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CdpWarehouseE2eCertificationRunController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping({"/warehouse/e2e-certification-runs", "/warehouse/e2e-certification/runs"})
public class CdpWarehouseE2eCertificationRunController {

    private final CdpWarehouseE2eCertificationRunService runService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseE2eCertificationRunController 实例并注入 web 场景依赖。
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseE2eCertificationRunController(CdpWarehouseE2eCertificationRunService runService) {
        this(runService, null);
    }

    /**
     * 创建 CdpWarehouseE2eCertificationRunController 实例并注入 web 场景依赖。
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseE2eCertificationRunController(CdpWarehouseE2eCertificationRunService runService,
                                                     TenantContextResolver tenantContextResolver) {
        this.runService = runService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 触发 CDP 数仓 E2e Certification Run运行接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 runService.run 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param from 请求参数，可选。
     * @param to 请求参数，可选。
     * @param mode 请求参数，默认值为 HYBRID。
     * @param contractKeys 请求参数，可选。
     * @param requirePhysical 请求参数，默认值为 true。
     * @param requireRealtime 请求参数，默认值为 true。
     * @param requireDataPathProof 请求参数，默认值为 true。
     * @param requestedBy 请求参数，默认值为 system。
     * @return 异步返回统一响应，包含触发 CDP 数仓 E2e Certification Run运行后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWarehouseE2eCertificationRunService.CertificationRunView>> runCertification(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "true") boolean requirePhysical,
            @RequestParam(defaultValue = "true") boolean requireRealtime,
            @RequestParam(defaultValue = "true") boolean requireDataPathProof,
            @RequestParam(defaultValue = "system") String requestedBy) {
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(runService.run(
                                tenantId, from, to, mode, safeContractKeys, requirePhysical, requireRealtime,
                                requireDataPathProof, requestedBy)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 CDP 数仓 E2e Certification Run运行接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 runService.recent 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWarehouseE2eCertificationRunService.CertificationRunView>>> recentRuns(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(runService.recent(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 CDP 数仓 E2e Certification Run详情接口，对应 GET /{id}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 runService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取 CDP 数仓 E2e Certification Run详情后的业务数据。
     */
    @GetMapping("/{id}")
    public Mono<R<CdpWarehouseE2eCertificationRunService.CertificationRunView>> getRun(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(runService.get(tenantId, id)))
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
