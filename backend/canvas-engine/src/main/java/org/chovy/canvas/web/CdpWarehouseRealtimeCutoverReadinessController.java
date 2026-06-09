package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCutoverReadinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CdpWarehouseRealtimeCutoverReadinessController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/realtime/cutover-readiness")
public class CdpWarehouseRealtimeCutoverReadinessController {

    private final CdpWarehouseRealtimeCutoverReadinessService cutoverReadinessService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseRealtimeCutoverReadinessController 实例并注入 web 场景依赖。
     * @param cutoverReadinessService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeCutoverReadinessController(
            CdpWarehouseRealtimeCutoverReadinessService cutoverReadinessService) {
        this(cutoverReadinessService, null);
    }

    /**
     * 创建 CdpWarehouseRealtimeCutoverReadinessController 实例并注入 web 场景依赖。
     * @param cutoverReadinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseRealtimeCutoverReadinessController(
            CdpWarehouseRealtimeCutoverReadinessService cutoverReadinessService,
            TenantContextResolver tenantContextResolver) {
        this.cutoverReadinessService = cutoverReadinessService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Realtime Cutover Readiness就绪度接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 cutoverReadinessService.evaluate, CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param targetMode 请求参数，默认值为 FLINK_FIRST。
     * @param pipelineKeys 请求参数，可选。
     * @param contractKeys 请求参数，可选。
     * @param certificationMode 请求参数，默认值为 HYBRID。
     * @param maxCertificationAgeMinutes 请求参数，默认值为 60。
     * @return 异步返回统一响应，包含查询 CDP 数仓 Realtime Cutover Readiness就绪度后的业务数据。
     */
    @GetMapping
    public Mono<R<CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision>> cutoverReadiness(
            @RequestParam(defaultValue = "FLINK_FIRST") String targetMode,
            @RequestParam(name = "pipelineKey", required = false) List<String> pipelineKeys,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "HYBRID") String certificationMode,
            @RequestParam(defaultValue = "60") Long maxCertificationAgeMinutes) {
        List<String> safePipelineKeys = pipelineKeys == null ? List.of() : pipelineKeys;
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(cutoverReadinessService.evaluate(
                                tenantId,
                                new CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand(
                                        targetMode,
                                        safePipelineKeys,
                                        safeContractKeys,
                                        certificationMode,
                                        maxCertificationAgeMinutes))))
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
