package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseMetricChangeReviewService;
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
 * CdpWarehouseMetricChangeReviewController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/metric-change-reviews")
public class CdpWarehouseMetricChangeReviewController {

    /** 承接指标变更评审单的创建、审批和查询逻辑。 */
    private final CdpWarehouseMetricChangeReviewService reviewService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseMetricChangeReviewController 实例并注入 web 场景依赖。
     * @param reviewService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseMetricChangeReviewController(CdpWarehouseMetricChangeReviewService reviewService) {
        this(reviewService, null);
    }

    /**
     * 创建 CdpWarehouseMetricChangeReviewController 实例并注入 web 场景依赖。
     * @param reviewService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseMetricChangeReviewController(CdpWarehouseMetricChangeReviewService reviewService,
                                                    TenantContextResolver tenantContextResolver) {
        this.reviewService = reviewService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Metric Change Review列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 reviewService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键，可选。
     * @param metricKey metric 唯一键，可选。
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>>> list(
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) String status) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.list(context.tenantId(), datasetKey, metricKey, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Metric Change Review 请求接口，对应 POST 请求。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 reviewService.requestChange 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Metric Change Review 请求后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> requestChange(
            @RequestBody MetricChangeReq req) {
        MetricChangeReq request = req == null ? new MetricChangeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.requestChange(
                                context.tenantId(),
                                context.username(),
                                request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 审批通过 CDP 数仓 Metric Change Review接口，对应 POST /{reviewId}/approve。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 reviewService.approve 完成业务处理。
     * 副作用：会推进审批状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param reviewId review ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含审批通过 CDP 数仓 Metric Change Review后的业务数据。
     */
    @PostMapping("/{reviewId}/approve")
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> approve(
            @PathVariable Long reviewId,
            @RequestBody ReviewDecisionReq req) {
        ReviewDecisionReq request = req == null ? new ReviewDecisionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.approve(
                                context.tenantId(),
                                reviewId,
                                context.username(),
                                request.getReviewNote())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 审批拒绝 CDP 数仓 Metric Change Review接口，对应 POST /{reviewId}/reject。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 reviewService.reject 完成业务处理。
     * 副作用：会推进审批状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param reviewId review ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含审批拒绝 CDP 数仓 Metric Change Review后的业务数据。
     */
    @PostMapping("/{reviewId}/reject")
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> reject(
            @PathVariable Long reviewId,
            @RequestBody ReviewDecisionReq req) {
        ReviewDecisionReq request = req == null ? new ReviewDecisionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.reject(
                                context.tenantId(),
                                reviewId,
                                context.username(),
                                request.getReviewNote())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 数仓 Metric Change Review 请求接口，对应 POST /{reviewId}/apply。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 reviewService.apply 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param reviewId review ID。
     * @return 异步返回统一响应，包含处理 CDP 数仓 Metric Change Review 请求后的业务数据。
     */
    @PostMapping("/{reviewId}/apply")
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> apply(
            @PathVariable Long reviewId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.apply(context.tenantId(), reviewId)))
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
                .defaultIfEmpty(new TenantContext(0L, null, "system"))
                .map(context -> new TenantContext(
                        context.tenantId() == null ? 0L : context.tenantId(),
                        context.role(),
                        context.username() == null || context.username().isBlank() ? "system" : context.username()));
    }

    @Data
    /**
     * MetricChangeReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class MetricChangeReq {
        /** 数据集唯一键，用于关联目录、指标、字段治理和表契约。 */
        private String datasetKey;
        /** 指标唯一键，用于定位需要评审的语义指标。 */
        private String metricKey;
        /** 拟变更的指标表达式，用于评审计算口径影响。 */
        private String proposedExpression;
        /** 拟允许下钻的维度集合，用于评审指标可分析范围。 */
        private List<String> proposedAllowedDimensions;
        /** 发起变更、控制或回滚的原因，用于审批和审计追溯。 */
        private String reason;

        CdpWarehouseMetricChangeReviewService.MetricChangeCommand toCommand() {
            return new CdpWarehouseMetricChangeReviewService.MetricChangeCommand(
                    datasetKey,
                    metricKey,
                    proposedExpression,
                    proposedAllowedDimensions,
                    reason);
        }
    }

    @Data
    /**
     * ReviewDecisionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class ReviewDecisionReq {
        /** 评审意见，用于记录批准或驳回指标变更的依据。 */
        private String reviewNote;
    }
}
