package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSloPolicyService;
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
 * CdpWarehouseSloPolicyController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/slo-policies")
public class CdpWarehouseSloPolicyController {

    /** 承接数仓 SLO 策略的读取、合并和写入逻辑。 */
    private final CdpWarehouseSloPolicyService sloPolicyService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseSloPolicyController 实例并注入 web 场景依赖。
     * @param sloPolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseSloPolicyController(CdpWarehouseSloPolicyService sloPolicyService) {
        this(sloPolicyService, null);
    }

    /**
     * 创建 CdpWarehouseSloPolicyController 实例并注入 web 场景依赖。
     * @param sloPolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseSloPolicyController(CdpWarehouseSloPolicyService sloPolicyService,
                                           TenantContextResolver tenantContextResolver) {
        this.sloPolicyService = sloPolicyService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Slo Policy列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 sloPolicyService.listPolicies 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWarehouseSloPolicyService.SloPolicyView>>> listPolicies(
            @RequestParam(required = false) String status) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(sloPolicyService.listPolicies(tenantId, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Slo Policy 请求接口，对应 GET /effective。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 sloPolicyService.effectivePolicy 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param policyKey policy 唯一键。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Slo Policy 请求后的业务数据。
     */
    @GetMapping("/effective")
    public Mono<R<CdpWarehouseSloPolicyService.SloPolicyView>> effective(
            @RequestParam(defaultValue = CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY) String policyKey) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(sloPolicyService.effectivePolicy(tenantId, policyKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 CDP 数仓 Slo Policy接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 sloPolicyService.upsertPolicy 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建或更新 CDP 数仓 Slo Policy后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWarehouseSloPolicyService.SloPolicyView>> upsert(@RequestBody SloPolicyReq req) {
        SloPolicyReq request = req == null ? new SloPolicyReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(sloPolicyService.upsertPolicy(tenantId, request.toCommand())))
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
     * SloPolicyReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class SloPolicyReq {
        /** SLO 策略唯一键，用于区分默认策略和租户自定义策略。 */
        private String policyKey;
        /** 面向运营和治理人员展示的可读名称。 */
        private String displayName;
        /** 离线同步任务达到告警级别的最大运行间隔分钟数。 */
        private Integer offlineWarnRunGapMinutes;
        /** 离线同步任务达到失败级别的最大运行间隔分钟数。 */
        private Integer offlineFailRunGapMinutes;
        /** 离线水位延迟达到告警级别的分钟阈值。 */
        private Integer offlineWarnWatermarkLagMinutes;
        /** 离线水位延迟达到失败级别的分钟阈值。 */
        private Integer offlineFailWatermarkLagMinutes;
        /** 受众物化任务达到告警级别的最大运行间隔分钟数。 */
        private Integer audienceWarnRunGapMinutes;
        /** 受众物化任务达到失败级别的最大运行间隔分钟数。 */
        private Integer audienceFailRunGapMinutes;
        /** 业务状态，用于控制配置启用、检查结果或回执处理分支。 */
        private String status;
        /** 责任人名称，用于治理归属、告警通知和问题追踪。 */
        private String ownerName;
        /** 补充说明，用于记录配置目的、血缘关系或治理背景。 */
        private String description;

        /**
         * 执行 目标command 对应的内部处理流程。
         * @return 返回内部处理结果
         */
        CdpWarehouseSloPolicyService.SloPolicyCommand toCommand() {
            return new CdpWarehouseSloPolicyService.SloPolicyCommand(
                    policyKey,
                    displayName,
                    offlineWarnRunGapMinutes,
                    offlineFailRunGapMinutes,
                    offlineWarnWatermarkLagMinutes,
                    offlineFailWatermarkLagMinutes,
                    audienceWarnRunGapMinutes,
                    audienceFailRunGapMinutes,
                    status,
                    ownerName,
                    description);
        }
    }
}
