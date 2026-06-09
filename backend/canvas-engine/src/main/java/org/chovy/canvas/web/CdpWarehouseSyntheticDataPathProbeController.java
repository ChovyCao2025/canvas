package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CdpWarehouseSyntheticDataPathProbeController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/data-path-probes/synthetic-ods")
public class CdpWarehouseSyntheticDataPathProbeController {

    private final CdpWarehouseSyntheticDataPathProbeService probeService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseSyntheticDataPathProbeController 实例并注入 web 场景依赖。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseSyntheticDataPathProbeController(
            CdpWarehouseSyntheticDataPathProbeService probeService) {
        this(probeService, null);
    }

    /**
     * 创建 CdpWarehouseSyntheticDataPathProbeController 实例并注入 web 场景依赖。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseSyntheticDataPathProbeController(
            CdpWarehouseSyntheticDataPathProbeService probeService,
            TenantContextResolver tenantContextResolver) {
        this.probeService = probeService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 触发 CDP 数仓 Synthetic Data Path Probe运行接口，对应 POST /run。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 probeService.run, CdpWarehouseSyntheticDataPathProbeService.RunCommand 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param probeKey probe 唯一键，可选。
     * @param eventCode 请求参数，可选。
     * @param strict 请求参数，默认值为 true。
     * @param verifyAttempts 请求参数，默认值为 3。
     * @param verifyDelayMs 请求参数，默认值为 100。
     * @param sourceMode 请求参数，默认值为 DIRECT_SINK。
     * @return 异步返回统一响应，包含触发 CDP 数仓 Synthetic Data Path Probe运行后的业务数据。
     */
    @PostMapping("/run")
    public Mono<R<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView>> run(
            @RequestParam(required = false) String probeKey,
            @RequestParam(required = false) String eventCode,
            @RequestParam(defaultValue = "true") boolean strict,
            @RequestParam(defaultValue = "3") int verifyAttempts,
            @RequestParam(defaultValue = "100") int verifyDelayMs,
            @RequestParam(defaultValue = "DIRECT_SINK") String sourceMode) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.run(
                                normalizeTenant(context),
                                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                                        probeKey, eventCode, strict, verifyAttempts, verifyDelayMs, sourceMode))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Synthetic Data Path Probe 请求接口，对应 GET /runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 probeService.recent 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/runs")
    public Mono<R<List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView>>> recent(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.recent(normalizeTenant(context), limit)))
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
}
