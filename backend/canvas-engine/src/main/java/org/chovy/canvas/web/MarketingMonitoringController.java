package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDeliveryView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDispatchView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorIngestResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemIngestCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollRunView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollingService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRevokeCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthCallbackCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourceCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourceView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotView;
import org.chovy.canvas.domain.monitoring.MarketingMonitoringService;
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
 * MarketingMonitoringController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitoringController {

    private final MarketingMonitoringService service;
    private final MarketingMonitorAlertFanoutService alertFanoutService;
    private final MarketingMonitorPollingService pollingService;
    private final MarketingMonitorInferenceService inferenceService;
    private final MarketingMonitorProviderCredentialService providerCredentialService;
    private final MarketingMonitorProviderOAuthAuthorizationService providerOAuthAuthorizationService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingMonitoringController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertFanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, null, null, null, null, tenantContextResolver);
    }

    /**
     * 创建 MarketingMonitoringController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertFanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, pollingService, null, null, null, tenantContextResolver);
    }

    /**
     * 创建 MarketingMonitoringController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertFanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param inferenceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         MarketingMonitorInferenceService inferenceService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, pollingService, inferenceService, null, null, tenantContextResolver);
    }

    /**
     * 创建 MarketingMonitoringController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertFanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param inferenceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param providerCredentialService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         MarketingMonitorInferenceService inferenceService,
                                         MarketingMonitorProviderCredentialService providerCredentialService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, pollingService, inferenceService, providerCredentialService, null,
                tenantContextResolver);
    }

    /**
     * 创建 MarketingMonitoringController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertFanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param inferenceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param providerCredentialService 依赖组件，用于完成数据访问或外部能力调用。
     * @param providerOAuthAuthorizationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         MarketingMonitorInferenceService inferenceService,
                                         MarketingMonitorProviderCredentialService providerCredentialService,
                                         MarketingMonitorProviderOAuthAuthorizationService providerOAuthAuthorizationService,
                                         TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.alertFanoutService = alertFanoutService;
        this.pollingService = pollingService;
        this.inferenceService = inferenceService;
        this.providerCredentialService = providerCredentialService;
        this.providerOAuthAuthorizationService = providerOAuthAuthorizationService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新营销监控源配置。
     * 监控源归属当前租户，操作者从租户上下文提取；配置会影响后续内容采集、轮询和告警生成。
     * 副作用是新增或覆盖监控源配置。
     *
     * @param command 监控源渠道、供应商和采集参数。
     * @return 保存后的监控源视图。
     */
    @PostMapping("/sources")
    public Mono<R<MarketingMonitorSourceView>> upsertSource(
            @RequestBody MarketingMonitorSourceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSource(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 接收入站监控内容并写入当前租户的监控流。
     * 入站内容通常来自外部平台回调或内部采集任务，服务层会完成去重、情绪识别线索和告警候选写入。
     * 副作用是新增或更新监控条目，并可能派生告警。
     *
     * @param command 入站内容、来源和外部标识。
     * @return 入站处理结果。
     */
    @PostMapping("/items")
    public Mono<R<MarketingMonitorIngestResult>> ingestItem(
            @RequestBody MarketingMonitorItemIngestCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.ingestItem(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的监控内容条目。
     * 可按情绪标签和竞品键筛选，返回数量会被限制在安全范围内。
     * 本接口只读。
     *
     * @param sentimentLabel 情绪标签过滤条件，可选。
     * @param competitorKey 竞品业务键，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 监控内容列表。
     */
    @GetMapping("/items")
    public Mono<R<List<MarketingMonitorItemView>>> items(
            @RequestParam(required = false) String sentimentLabel,
            @RequestParam(required = false) String competitorKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.items(tenantId(context),
                                new MarketingMonitorItemQuery(sentimentLabel, competitorKey, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的营销监控告警。
     * 可按告警状态筛选，适用于告警中心列表和未处理告警看板。
     * 本接口只读。
     *
     * @param status 告警状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 告警列表。
     */
    @GetMapping("/alerts")
    public Mono<R<List<MarketingMonitorAlertView>>> alerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.alerts(tenantId(context),
                                new MarketingMonitorAlertQuery(status, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 将当前租户下的指定告警标记为已处理。
     * 告警 ID 必须属于当前租户；操作者会写入状态流转记录。
     * 副作用是推进告警状态，避免继续出现在待处理队列。
     *
     * @param alertId 告警主键。
     * @return 处理后的告警视图。
     */
    @PostMapping("/alerts/{alertId}/resolve")
    public Mono<R<MarketingMonitorAlertView>> resolveAlert(@PathVariable Long alertId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.resolveAlert(tenantId(context), alertId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新告警通知渠道。
     * 渠道归属当前租户，用于告警派发时选择消息、邮件或其他通知目标。
     * 副作用是保存渠道配置，可能覆盖同一渠道键的既有配置。
     *
     * @param command 渠道类型、目标和启用状态。
     * @return 保存后的告警渠道视图。
     */
    @PostMapping("/alert-channels")
    public Mono<R<MarketingMonitorAlertChannelView>> upsertAlertChannel(
            @RequestBody MarketingMonitorAlertChannelCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(alertFanoutService.upsertChannel(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 手动派发指定告警到当前租户配置的通知渠道。
     * 告警和渠道都按租户隔离；派发结果会记录每个渠道的投递状态。
     * 副作用是创建告警投递记录，并可能触发外部通知。
     *
     * @param alertId 告警主键。
     * @return 告警派发结果。
     */
    @PostMapping("/alerts/{alertId}/dispatch")
    public Mono<R<MarketingMonitorAlertDispatchView>> dispatchAlert(@PathVariable Long alertId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(alertFanoutService.dispatchAlert(tenantId(context), alertId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的告警投递记录。
     * 支持按告警和投递状态筛选，用于查看通知是否成功送达。
     * 本接口只读。
     *
     * @param alertId 告警主键，可选。
     * @param status 投递状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 告警投递记录列表。
     */
    @GetMapping("/alert-deliveries")
    public Mono<R<List<MarketingMonitorAlertDeliveryView>>> alertDeliveries(
            @RequestParam(required = false) Long alertId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(alertFanoutService.deliveries(tenantId(context), alertId, status,
                                boundedLimit(limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 配置指定监控源的轮询策略。
     * 监控源必须属于当前租户；配置会决定后续自动采集的频率、窗口和启停状态。
     * 副作用是写入或更新轮询配置。
     *
     * @param sourceId 监控源主键。
     * @param command 轮询频率、窗口和启用状态。
     * @return 监控源轮询配置视图。
     */
    @PostMapping("/sources/{sourceId}/polling")
    public Mono<R<MarketingMonitorSourcePollingView>> configureSourcePolling(
            @PathVariable Long sourceId,
            @RequestBody MarketingMonitorSourcePollingCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService()
                                .configurePolling(tenantId(context), sourceId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 立即触发指定监控源的一次采集。
     * 监控源按当前租户校验；可选命令用于覆盖本次采集窗口或游标。
     * 副作用是创建轮询运行记录，并可能写入新的监控内容和告警。
     *
     * @param sourceId 监控源主键。
     * @param command 本次采集参数，可选。
     * @return 轮询运行视图。
     */
    @PostMapping("/sources/{sourceId}/poll")
    public Mono<R<MarketingMonitorPollRunView>> pollSource(
            @PathVariable Long sourceId,
            @RequestBody(required = false) MarketingMonitorPollCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService()
                                .pollSource(tenantId(context), sourceId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 基于监控内容构建趋势快照。
     * 快照归属当前租户，可用于品牌、竞品或来源维度的趋势看板。
     * 副作用是写入一条趋势快照记录。
     *
     * @param command 快照维度、时间窗口和统计参数。
     * @return 趋势快照视图。
     */
    @PostMapping("/trends/snapshots/build")
    public Mono<R<MarketingMonitorTrendSnapshotView>> buildTrendSnapshot(
            @RequestBody MarketingMonitorTrendSnapshotCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService()
                                .buildTrendSnapshot(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的趋势快照。
     * 可按监控源、品牌和竞品筛选，返回数量会进行上限保护。
     * 本接口只读。
     *
     * @param sourceId 监控源主键，可选。
     * @param brandKey 品牌业务键，可选。
     * @param competitorKey 竞品业务键，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 趋势快照列表。
     */
    @GetMapping("/trends/snapshots")
    public Mono<R<List<MarketingMonitorTrendSnapshotView>>> trendSnapshots(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String brandKey,
            @RequestParam(required = false) String competitorKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService().trendSnapshots(tenantId(context),
                                new MarketingMonitorTrendSnapshotQuery(
                                        sourceId,
                                        brandKey,
                                        competitorKey,
                                boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 对指定监控内容执行一次模型推理分析。
     * 内容条目必须属于当前租户；路径中的 itemId 会覆盖请求体中的条目字段，保证分析对象明确。
     * 副作用是写入推理结果，并可能影响后续情绪、风险或告警判断。
     *
     * @param itemId 监控内容主键。
     * @param command 模型、模板、超时和回退参数，可选。
     * @return 推理结果视图。
     */
    @PostMapping("/items/{itemId}/inferences")
    public Mono<R<MarketingMonitorInferenceView>> analyzeInference(
            @PathVariable Long itemId,
            @RequestBody(required = false) MarketingMonitorInferenceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredInferenceService()
                                .analyze(tenantId(context), inferenceCommand(itemId, command), actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的监控推理记录。
     * 可按内容、情绪、模型、供应商状态和是否使用回退模型筛选。
     * 本接口只读。
     *
     * @param itemId 监控内容主键，可选。
     * @param sentimentLabel 情绪标签过滤条件，可选。
     * @param modelKey 模型业务键，可选。
     * @param providerStatus 供应商调用状态，可选。
     * @param fallbackUsed 是否使用回退模型，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 推理记录列表。
     */
    @GetMapping("/inferences")
    public Mono<R<List<MarketingMonitorInferenceView>>> inferences(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String sentimentLabel,
            @RequestParam(required = false) String modelKey,
            @RequestParam(required = false) String providerStatus,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredInferenceService().list(tenantId(context),
                                new MarketingMonitorInferenceQuery(
                                        itemId,
                                        sentimentLabel,
                                        modelKey,
                                        providerStatus,
                                        fallbackUsed,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新外部监控供应商凭证。
     * 凭证归属当前租户，操作者来自租户上下文；服务层负责处理密钥、OAuth 引用和状态校验。
     * 副作用是新增或覆盖供应商凭证配置。
     *
     * @param command 供应商类型、认证方式和凭证内容。
     * @return 保存后的供应商凭证视图。
     */
    @PostMapping("/provider-credentials")
    public Mono<R<MarketingMonitorProviderCredentialView>> upsertProviderCredential(
            @RequestBody MarketingMonitorProviderCredentialCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .upsert(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的监控供应商凭证。
     * 可按供应商类型、认证方式和凭证状态筛选；敏感字段由视图层控制暴露范围。
     * 本接口只读。
     *
     * @param providerType 供应商类型过滤条件，可选。
     * @param authType 认证方式过滤条件，可选。
     * @param status 凭证状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 供应商凭证列表。
     */
    @GetMapping("/provider-credentials")
    public Mono<R<List<MarketingMonitorProviderCredentialView>>> providerCredentials(
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String authType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService().list(tenantId(context),
                                new MarketingMonitorProviderCredentialQuery(
                                        providerType,
                                        authType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 手动刷新指定供应商凭证。
     * 凭证业务键必须属于当前租户；空请求体会按默认刷新参数执行。
     * 副作用是触发刷新流程，更新凭证状态、有效期或错误信息。
     *
     * @param credentialKey 凭证业务键。
     * @param command 刷新参数，可选。
     * @return 刷新后的凭证视图。
     */
    @PostMapping("/provider-credentials/{credentialKey}/refresh")
    public Mono<R<MarketingMonitorProviderCredentialView>> refreshProviderCredential(
            @PathVariable String credentialKey,
            @RequestBody(required = false) MarketingMonitorProviderCredentialRefreshCommand command) {
        // 空请求体表示使用服务默认策略刷新该凭证。
        MarketingMonitorProviderCredentialRefreshCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialRefreshCommand(null)
                : command;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .refresh(tenantId(context), credentialKey, effectiveCommand, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 批量刷新当前租户内已到期或即将到期的供应商凭证。
     * 可指定扫描窗口和数量上限；未指定时使用默认窗口并限制批量规模。
     * 副作用是对命中的凭证执行刷新并记录每条刷新结果。
     *
     * @param command 到期扫描窗口和批量上限，可选。
     * @return 批量刷新结果。
     */
    @PostMapping("/provider-credentials/refresh-due")
    public Mono<R<MarketingMonitorProviderCredentialDueRefreshResult>> refreshDueProviderCredentials(
            @RequestBody(required = false) MarketingMonitorProviderCredentialDueRefreshCommand command) {
        // 对批量任务的 limit 做统一上限保护，避免一次刷新过多外部凭证。
        MarketingMonitorProviderCredentialDueRefreshCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialDueRefreshCommand(null, null)
                /**
                 * 执行 MarketingMonitorProviderCredentialDueRefreshCommand 流程，围绕 marketing monitor provider credential due refresh command 完成校验、计算或结果组装。
                 *
                 * @return 返回 MarketingMonitorProviderCredentialDueRefreshCommand 流程生成的业务结果。
                 */
                : new MarketingMonitorProviderCredentialDueRefreshCommand(
                        command.windowMinutes(),
                        boundedLimit(command.limit() == null ? 50 : command.limit()));
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .refreshDue(tenantId(context), effectiveCommand, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 撤销指定供应商凭证。
     * 凭证必须属于当前租户；可选参数用于传递撤销原因、外部回执或操作者备注。
     * 副作用是推进凭证到撤销状态，并可能调用供应商接口释放授权。
     *
     * @param credentialKey 凭证业务键。
     * @param command 撤销参数，可选。
     * @return 撤销后的凭证视图。
     */
    @PostMapping("/provider-credentials/{credentialKey}/revoke")
    public Mono<R<MarketingMonitorProviderCredentialView>> revokeProviderCredential(
            @PathVariable String credentialKey,
            @RequestBody(required = false) MarketingMonitorProviderCredentialRevokeCommand command) {
        // 保持撤销接口可无请求体调用，撤销细节交给服务默认值处理。
        MarketingMonitorProviderCredentialRevokeCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialRevokeCommand(null, null, null, null, null)
                : command;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .revoke(tenantId(context), credentialKey, effectiveCommand, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 禁用指定供应商凭证，阻止后续监控采集继续使用它。
     * 凭证必须属于当前租户；禁用只改变本系统凭证状态，不等同于外部授权撤销。
     * 副作用是更新凭证状态。
     *
     * @param credentialKey 凭证业务键。
     * @return 禁用后的凭证视图。
     */
    @PostMapping("/provider-credentials/{credentialKey}/disable")
    public Mono<R<MarketingMonitorProviderCredentialView>> disableProviderCredential(
            @PathVariable String credentialKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .disable(tenantId(context), credentialKey, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的供应商凭证事件。
     * 事件包含创建、刷新、撤销、失败等凭证生命周期记录，便于审计和排障。
     * 本接口只读。
     *
     * @param credentialKey 凭证业务键，可选。
     * @param eventType 事件类型过滤条件，可选。
     * @param status 事件状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 凭证事件列表。
     */
    @GetMapping("/provider-credentials/events")
    public Mono<R<List<MarketingMonitorProviderCredentialEventView>>> providerCredentialEvents(
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService().events(tenantId(context),
                                new MarketingMonitorProviderCredentialEventQuery(
                                        credentialKey,
                                        eventType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 发起供应商 OAuth 授权流程。
     * 授权状态绑定当前租户和操作者，用于后续回调校验并关联供应商凭证。
     * 副作用是创建授权会话，返回跳转地址或授权状态信息。
     *
     * @param command 供应商、回调地址和授权范围。
     * @return OAuth 授权视图。
     */
    @PostMapping("/provider-credentials/oauth/authorizations")
    public Mono<R<MarketingMonitorProviderOAuthAuthorizationView>> startProviderOAuthAuthorization(
            @RequestBody MarketingMonitorProviderOAuthAuthorizationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService()
                                .startAuthorization(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 完成供应商 OAuth 回调处理。
     * 回调会按当前租户和授权 state 校验来源，成功后写入或更新对应供应商凭证。
     * 副作用是推进授权状态，并可能创建可用于采集的凭证。
     *
     * @param command 授权码、state 和供应商回调参数。
     * @return 完成后的 OAuth 授权视图。
     */
    @PostMapping("/provider-credentials/oauth/callback")
    public Mono<R<MarketingMonitorProviderOAuthAuthorizationView>> completeProviderOAuthAuthorization(
            @RequestBody MarketingMonitorProviderOAuthCallbackCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService()
                                .completeAuthorization(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的 OAuth 授权会话。
     * 可按凭证、供应商和授权状态筛选，用于查看授权进度或失败原因。
     * 本接口只读。
     *
     * @param credentialKey 凭证业务键，可选。
     * @param providerType 供应商类型过滤条件，可选。
     * @param status 授权状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return OAuth 授权会话列表。
     */
    @GetMapping("/provider-credentials/oauth/authorizations")
    public Mono<R<List<MarketingMonitorProviderOAuthAuthorizationView>>> providerOAuthAuthorizations(
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService().list(tenantId(context),
                                new MarketingMonitorProviderOAuthAuthorizationQuery(
                                        credentialKey,
                                        providerType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的 OAuth 授权事件。
     * 事件记录授权发起、回调、失败和凭证绑定等关键节点，便于审计授权链路。
     * 本接口只读。
     *
     * @param authState OAuth state，可选。
     * @param credentialKey 凭证业务键，可选。
     * @param eventType 事件类型过滤条件，可选。
     * @param status 事件状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return OAuth 授权事件列表。
     */
    @GetMapping("/provider-credentials/oauth/events")
    public Mono<R<List<MarketingMonitorProviderOAuthAuthorizationEventView>>> providerOAuthAuthorizationEvents(
            @RequestParam(required = false) String authState,
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService().events(tenantId(context),
                                new MarketingMonitorProviderOAuthAuthorizationEventQuery(
                                        authState,
                                        credentialKey,
                                        eventType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredPollingService 流程生成的业务结果。
     */
    private MarketingMonitorPollingService requiredPollingService() {
        if (pollingService == null) {
            throw new IllegalStateException("marketing monitoring polling service is not configured");
        }
        return pollingService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredInferenceService 流程生成的业务结果。
     */
    private MarketingMonitorInferenceService requiredInferenceService() {
        if (inferenceService == null) {
            throw new IllegalStateException("marketing monitoring inference service is not configured");
        }
        return inferenceService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredProviderCredentialService 流程生成的业务结果。
     */
    private MarketingMonitorProviderCredentialService requiredProviderCredentialService() {
        if (providerCredentialService == null) {
            throw new IllegalStateException("marketing monitoring provider credential service is not configured");
        }
        return providerCredentialService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requiredProviderOAuthAuthorizationService 流程生成的业务结果。
     */
    private MarketingMonitorProviderOAuthAuthorizationService requiredProviderOAuthAuthorizationService() {
        if (providerOAuthAuthorizationService == null) {
            throw new IllegalStateException("marketing monitoring provider OAuth authorization service is not configured");
        }
        return providerOAuthAuthorizationService;
    }

    /**
     * 执行 inferenceCommand 流程，围绕 inference command 完成校验、计算或结果组装。
     *
     * @param itemId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 inferenceCommand 流程生成的业务结果。
     */
    private MarketingMonitorInferenceCommand inferenceCommand(Long itemId,
                                                              MarketingMonitorInferenceCommand command) {
        return new MarketingMonitorInferenceCommand(
                itemId,
                command == null ? null : command.providerId(),
                command == null ? null : command.templateId(),
                command == null ? null : command.modelKey(),
                command == null ? null : command.modelVersion(),
                command == null ? null : command.forceFallback(),
                command == null ? null : command.params(),
                command == null ? null : command.timeoutMs(),
                command == null ? null : command.metadata());
    }
}
