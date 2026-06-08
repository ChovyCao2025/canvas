package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.subscription.BiAlertRuleCommand;
import org.chovy.canvas.domain.bi.subscription.BiAlertRuleView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentCleanupResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentDownload;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentService;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAuditSummary;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryLogView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRetryResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRunResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRuntimeService;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerService;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionAdminService;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionCommand;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * BiSubscriptionController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi")
public class BiSubscriptionController {

    private final TenantContextResolver tenantContextResolver;
    private final BiSubscriptionAdminService subscriptionAdminService;
    private final BiDeliveryRuntimeService deliveryRuntimeService;
    private final BiDeliverySchedulerService deliverySchedulerService;
    private final BiDeliveryAttachmentService deliveryAttachmentService;

    /**
     * 创建 BiSubscriptionController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param subscriptionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                    BiSubscriptionAdminService subscriptionAdminService,
                                    BiDeliveryRuntimeService deliveryRuntimeService) {
        this(tenantContextResolver, subscriptionAdminService, deliveryRuntimeService,
                (BiDeliverySchedulerService) null, (BiDeliveryAttachmentService) null);
    }

    /**
     * 创建 BiSubscriptionController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param subscriptionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param deliverySchedulerServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                    BiSubscriptionAdminService subscriptionAdminService,
                                    BiDeliveryRuntimeService deliveryRuntimeService,
                                    ObjectProvider<BiDeliverySchedulerService> deliverySchedulerServiceProvider) {
        this(tenantContextResolver,
                subscriptionAdminService,
                deliveryRuntimeService,
                deliverySchedulerServiceProvider == null ? null : deliverySchedulerServiceProvider.getIfAvailable(),
                null);
    }

    /**
     * 创建 BiSubscriptionController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param subscriptionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param deliverySchedulerServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAttachmentServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                    BiSubscriptionAdminService subscriptionAdminService,
                                    BiDeliveryRuntimeService deliveryRuntimeService,
                                    ObjectProvider<BiDeliverySchedulerService> deliverySchedulerServiceProvider,
                                    ObjectProvider<BiDeliveryAttachmentService> deliveryAttachmentServiceProvider) {
        this(tenantContextResolver,
                subscriptionAdminService,
                deliveryRuntimeService,
                deliverySchedulerServiceProvider.getIfAvailable(),
                deliveryAttachmentServiceProvider.getIfAvailable());
    }

    /**
     * 执行 BiSubscriptionController 流程，围绕 bi subscription controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param subscriptionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param deliverySchedulerService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAttachmentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    private BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                     BiSubscriptionAdminService subscriptionAdminService,
                                     BiDeliveryRuntimeService deliveryRuntimeService,
                                     BiDeliverySchedulerService deliverySchedulerService,
                                     BiDeliveryAttachmentService deliveryAttachmentService) {
        this.tenantContextResolver = tenantContextResolver;
        this.subscriptionAdminService = subscriptionAdminService;
        this.deliveryRuntimeService = deliveryRuntimeService;
        this.deliverySchedulerService = deliverySchedulerService;
        this.deliveryAttachmentService = deliveryAttachmentService;
    }
    /**
     * 查询 BI 订阅与告警列表接口，对应 GET /subscriptions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 subscriptionAdminService.listSubscriptions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/subscriptions")
    public Mono<R<List<BiSubscriptionView>>> listSubscriptions(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.listSubscriptions(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 BI 订阅与告警接口，对应 POST /subscriptions。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 subscriptionAdminService.upsertSubscription 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 BI 订阅与告警后的业务数据。
     */
    @PostMapping("/subscriptions")
    public Mono<R<BiSubscriptionView>> upsertSubscription(@RequestBody BiSubscriptionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.upsertSubscription(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 删除 BI 订阅与告警接口，对应 DELETE /subscriptions/{id}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 subscriptionAdminService.deleteSubscription 完成业务处理。
     * 副作用：会删除或逻辑移除记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/subscriptions/{id}")
    public Mono<R<Void>> deleteSubscription(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    subscriptionAdminService.deleteSubscription(context.tenantId(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 订阅与告警运行接口，对应 POST /subscriptions/{id}/run。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 deliveryRuntimeService.runSubscription 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含触发 BI 订阅与告警运行后的业务数据。
     */
    @PostMapping("/subscriptions/{id}/run")
    public Mono<R<BiDeliveryRunResult>> runSubscription(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.runSubscription(
                                context.tenantId(),
                                id,
                                context.username(),
                                context.role())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 订阅与告警列表接口，对应 GET /alerts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 subscriptionAdminService.listAlerts 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/alerts")
    public Mono<R<List<BiAlertRuleView>>> listAlerts(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.listAlerts(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 BI 订阅与告警接口，对应 POST /alerts。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 subscriptionAdminService.upsertAlert 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 BI 订阅与告警后的业务数据。
     */
    @PostMapping("/alerts")
    public Mono<R<BiAlertRuleView>> upsertAlert(@RequestBody BiAlertRuleCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.upsertAlert(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 删除 BI 订阅与告警接口，对应 DELETE /alerts/{id}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 subscriptionAdminService.deleteAlert 完成业务处理。
     * 副作用：会删除或逻辑移除记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/alerts/{id}")
    public Mono<R<Void>> deleteAlert(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    subscriptionAdminService.deleteAlert(context.tenantId(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 订阅与告警运行接口，对应 POST /alerts/{id}/run。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 deliveryRuntimeService.runAlert 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含触发 BI 订阅与告警运行后的业务数据。
     */
    @PostMapping("/alerts/{id}/run")
    public Mono<R<BiDeliveryRunResult>> runAlert(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.runAlert(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 订阅与告警列表接口，对应 GET /delivery-logs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 deliveryRuntimeService.listLogs 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param jobType 投递任务类型，可选。
     * @param jobId 投递任务 ID，可选。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/delivery-logs")
    public Mono<R<List<BiDeliveryLogView>>> listDeliveryLogs(@RequestParam(required = false) String jobType,
                                                             @RequestParam(required = false) Long jobId,
                                                             @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.listLogs(context.tenantId(), jobType, jobId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 订阅与告警审计接口，对应 GET /delivery-audit。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 deliveryRuntimeService.auditDeliveries 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param jobType 投递任务类型，可选。
     * @param status 状态过滤条件，可选。
     * @param channel 渠道过滤条件，可选。
     * @param jobId 投递任务 ID，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含查询 BI 订阅与告警审计后的业务数据。
     */
    @GetMapping("/delivery-audit")
    public Mono<R<BiDeliveryAuditSummary>> auditDeliveryLogs(@RequestParam(required = false) String jobType,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String channel,
                                                             @RequestParam(required = false) Long jobId,
                                                             @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.auditDeliveries(
                                context.tenantId(),
                                jobType,
                                status,
                                channel,
                                jobId,
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 重试 BI 订阅与告警任务接口，对应 POST /delivery-logs/retry。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 deliveryRuntimeService.retryPendingDeliveries 完成业务处理。
     * 副作用：会重试待处理任务。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含重试 BI 订阅与告警任务后的业务数据。
     */
    @PostMapping("/delivery-logs/retry")
    public Mono<R<BiDeliveryRetryResult>> retryDeliveryLogs(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.retryPendingDeliveries(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 订阅与告警列表接口，对应 GET /delivery-attachments。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 deliveryAttachmentService.listAttachments 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param jobType 投递任务类型，可选。
     * @param jobId 投递任务 ID，可选。
     * @param deliveryLogId 投递日志 ID，可选。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/delivery-attachments")
    public Mono<R<List<BiDeliveryAttachmentView>>> listDeliveryAttachments(@RequestParam(required = false) String jobType,
                                                                           @RequestParam(required = false) Long jobId,
                                                                           @RequestParam(required = false) Long deliveryLogId,
                                                                           @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliveryAttachmentService == null) {
                        return R.ok(List.<BiDeliveryAttachmentView>of());
                    }
                    return R.ok(deliveryAttachmentService.listAttachments(
                            context.tenantId(),
                            jobType,
                            jobId,
                            deliveryLogId,
                            limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 下载 BI 订阅与告警文件接口，对应 GET /delivery-attachments/{id}/download。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 deliveryAttachmentService.download 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回文件字节流响应。
     */
    @GetMapping("/delivery-attachments/{id}/download")
    public Mono<ResponseEntity<byte[]>> downloadDeliveryAttachment(@PathVariable Long id) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (deliveryAttachmentService == null) {
                        throw new IllegalStateException("BI delivery attachment service is not configured");
                    }
                    BiDeliveryAttachmentDownload file = deliveryAttachmentService.download(
                            context.tenantId(),
                            id,
                            context.username(),
                            context.role());
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(file.filename())
                                            .build()
                                            .toString())
                            .body(file.bytes());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 清理 BI 订阅与告警数据接口，对应 POST /delivery-attachments/cleanup。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 deliveryAttachmentService.cleanupExpiredAttachments 完成业务处理。
     * 副作用：会清理过期或无效数据。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 100。
     * @return 异步返回统一响应，包含清理 BI 订阅与告警数据后的业务数据。
     */
    @PostMapping("/delivery-attachments/cleanup")
    public Mono<R<BiDeliveryAttachmentCleanupResult>> cleanupDeliveryAttachments(@RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliveryAttachmentService == null) {
                        return R.ok(new BiDeliveryAttachmentCleanupResult(0, 0, 0, 0));
                    }
                    return R.ok(deliveryAttachmentService.cleanupExpiredAttachments(context.tenantId(), limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 订阅与告警运行接口，对应 POST /delivery-scheduler/run。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 deliverySchedulerService.runDueOnce 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含触发 BI 订阅与告警运行后的业务数据。
     */
    @PostMapping("/delivery-scheduler/run")
    public Mono<R<BiDeliverySchedulerResult>> runDeliveryScheduler() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliverySchedulerService == null) {
                        return R.ok(new BiDeliverySchedulerResult(0, 0, 0, 0, 0, 0));
                    }
                    return R.ok(deliverySchedulerService.runDueOnce(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            null));
                })
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
}
