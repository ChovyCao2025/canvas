package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.search.SearchMarketingKeywordCommand;
import org.chovy.canvas.domain.search.SearchMarketingKeywordQuery;
import org.chovy.canvas.domain.search.SearchMarketingKeywordView;
import org.chovy.canvas.domain.search.SearchMarketingImpactWindowQuery;
import org.chovy.canvas.domain.search.SearchMarketingImpactWindowService;
import org.chovy.canvas.domain.search.SearchMarketingImpactWindowView;
import org.chovy.canvas.domain.search.SearchMarketingManualSyncCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationApprovalCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationExecuteCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationQuery;
import org.chovy.canvas.domain.search.SearchMarketingMutationService;
import org.chovy.canvas.domain.search.SearchMarketingMutationView;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityEvaluationCommand;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityMutationCommand;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityQuery;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityStatusCommand;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityView;
import org.chovy.canvas.domain.search.SearchMarketingProviderChangeQuery;
import org.chovy.canvas.domain.search.SearchMarketingProviderChangeView;
import org.chovy.canvas.domain.search.SearchMarketingReadinessService;
import org.chovy.canvas.domain.search.SearchMarketingReadinessView;
import org.chovy.canvas.domain.search.SearchMarketingReconciliationService;
import org.chovy.canvas.domain.search.SearchMarketingReconciliationView;
import org.chovy.canvas.domain.search.SearchMarketingService;
import org.chovy.canvas.domain.search.SearchMarketingSnapshotCommand;
import org.chovy.canvas.domain.search.SearchMarketingSnapshotQuery;
import org.chovy.canvas.domain.search.SearchMarketingSnapshotView;
import org.chovy.canvas.domain.search.SearchMarketingSourceCommand;
import org.chovy.canvas.domain.search.SearchMarketingSourceQuery;
import org.chovy.canvas.domain.search.SearchMarketingSourceView;
import org.chovy.canvas.domain.search.SearchMarketingSummaryQuery;
import org.chovy.canvas.domain.search.SearchMarketingSummaryView;
import org.chovy.canvas.domain.search.SearchMarketingSyncDueRequest;
import org.chovy.canvas.domain.search.SearchMarketingSyncRequest;
import org.chovy.canvas.domain.search.SearchMarketingSyncRunQuery;
import org.chovy.canvas.domain.search.SearchMarketingSyncRunService;
import org.chovy.canvas.domain.search.SearchMarketingSyncRunView;
import org.chovy.canvas.domain.search.SearchMarketingUrlInspectionQuery;
import org.chovy.canvas.domain.search.SearchMarketingUrlInspectionView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;

/**
 * SearchMarketingController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/search-marketing")
public class SearchMarketingController {

    private final SearchMarketingService service;
    private final SearchMarketingMutationService mutationService;
    private final SearchMarketingSyncRunService syncRunService;
    private final SearchMarketingReadinessService readinessService;
    private final SearchMarketingReconciliationService reconciliationService;
    private final SearchMarketingImpactWindowService impactWindowService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 SearchMarketingController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public SearchMarketingController(SearchMarketingService service,
                                     SearchMarketingMutationService mutationService,
                                     TenantContextResolver tenantContextResolver) {
        this(service, mutationService, null, null, null, null, tenantContextResolver);
    }

    /**
     * 创建 SearchMarketingController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public SearchMarketingController(SearchMarketingService service,
                                     SearchMarketingMutationService mutationService,
                                     SearchMarketingSyncRunService syncRunService,
                                     TenantContextResolver tenantContextResolver) {
        this(service, mutationService, syncRunService, null, null, null, tenantContextResolver);
    }

    /**
     * 创建 SearchMarketingController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunService 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reconciliationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param impactWindowService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public SearchMarketingController(SearchMarketingService service,
                                     SearchMarketingMutationService mutationService,
                                     SearchMarketingSyncRunService syncRunService,
                                     SearchMarketingReadinessService readinessService,
                                     SearchMarketingReconciliationService reconciliationService,
                                     SearchMarketingImpactWindowService impactWindowService,
                                     TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.mutationService = mutationService;
        this.syncRunService = syncRunService;
        this.readinessService = readinessService;
        this.reconciliationService = reconciliationService;
        this.impactWindowService = impactWindowService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询当前租户的搜索营销数据源。
     * 可按供应商、渠道和启用状态筛选，用于管理搜索平台账号或站点接入。
     * 本接口只读。
     *
     * @param provider 供应商过滤条件，可选。
     * @param channel 渠道过滤条件，可选。
     * @param enabled 是否启用，可选。
     * @param limit 返回数量上限，可选。
     * @return 搜索营销数据源列表。
     */
    @GetMapping("/sources")
    public Mono<R<List<SearchMarketingSourceView>>> listSources(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listSources(tenantId(context),
                                new SearchMarketingSourceQuery(provider, channel, enabled, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 按旧参数顺序查询搜索营销数据源的兼容入口。
     * 该方法复用正式列表接口，仍会解析当前租户并保持相同的权限边界。
     * 本方法只读。
     *
     * @param channel 渠道过滤条件。
     * @param provider 供应商过滤条件。
     * @param limit 返回数量上限。
     * @return 搜索营销数据源列表。
     */
    public Mono<R<List<SearchMarketingSourceView>>> listSources(String channel, String provider, Integer limit) {
        return listSources(provider, channel, null, limit);
    }
    /**
     * 创建或更新当前租户的搜索营销数据源。
     * 数据源配置决定后续关键词、快照、同步任务和变更执行的供应商上下文。
     * 副作用是新增或覆盖数据源配置。
     *
     * @param command 数据源供应商、渠道、认证引用和启用状态。
     * @return 保存后的数据源视图。
     */
    @PostMapping("/sources")
    public Mono<R<SearchMarketingSourceView>> upsertSource(@RequestBody SearchMarketingSourceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSource(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的搜索营销关键词。
     * 可按渠道和关键词状态筛选，用于关键词监控、优化建议和报表汇总。
     * 本接口只读。
     *
     * @param channel 渠道过滤条件，可选。
     * @param status 关键词状态过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 关键词列表。
     */
    @GetMapping("/keywords")
    public Mono<R<List<SearchMarketingKeywordView>>> listKeywords(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listKeywords(tenantId(context),
                                new SearchMarketingKeywordQuery(channel, status, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新当前租户的搜索营销关键词。
     * 关键词会参与后续排名、点击、曝光等快照采集和机会识别。
     * 副作用是新增或覆盖关键词配置。
     *
     * @param command 关键词文本、渠道、归属数据源和状态。
     * @return 保存后的关键词视图。
     */
    @PostMapping("/keywords")
    public Mono<R<SearchMarketingKeywordView>> upsertKeyword(@RequestBody SearchMarketingKeywordCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertKeyword(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的搜索营销表现快照。
     * 快照按渠道、数据源、关键词和日期范围筛选，用于趋势图、汇总和机会评估。
     * 本接口只读。
     *
     * @param channel 渠道过滤条件，可选。
     * @param sourceId 数据源主键，可选。
     * @param keywordId 关键词主键，可选。
     * @param startDate 快照开始日期，可选。
     * @param endDate 快照结束日期，可选。
     * @param limit 返回数量上限，可选。
     * @return 表现快照列表。
     */
    @GetMapping("/snapshots")
    public Mono<R<List<SearchMarketingSnapshotView>>> listSnapshots(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listSnapshots(tenantId(context),
                                new SearchMarketingSnapshotQuery(channel, sourceId, keywordId,
                                        startDate, endDate, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 按旧参数顺序查询搜索营销快照的兼容入口。
     * 该方法转调正式快照查询接口，仍使用当前请求的租户和权限上下文。
     * 本方法只读。
     *
     * @param sourceId 数据源主键。
     * @param keywordId 关键词主键。
     * @param channel 渠道过滤条件。
     * @param startDate 快照开始日期。
     * @param endDate 快照结束日期。
     * @param limit 返回数量上限。
     * @return 表现快照列表。
     */
    public Mono<R<List<SearchMarketingSnapshotView>>> listSnapshots(Long sourceId,
                                                                    Long keywordId,
                                                                    String channel,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    Integer limit) {
        return listSnapshots(channel, sourceId, keywordId, startDate, endDate, limit);
    }
    /**
     * 写入一条搜索营销表现快照。
     * 快照绑定当前租户和操作者，通常由同步任务或人工补录调用。
     * 副作用是新增快照记录，并可能影响后续机会识别和汇总。
     *
     * @param command 快照指标、日期、数据源和关键词。
     * @return 写入后的快照视图。
     */
    @PostMapping("/snapshots")
    public Mono<R<SearchMarketingSnapshotView>> recordSnapshot(@RequestBody SearchMarketingSnapshotCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.recordSnapshot(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户识别出的搜索营销机会。
     * 机会来自快照评估，可按渠道、数据源、状态和严重级别筛选。
     * 本接口只读。
     *
     * @param channel 渠道过滤条件，可选。
     * @param sourceId 数据源主键，可选。
     * @param status 机会状态过滤条件，可选。
     * @param severity 严重级别过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 搜索营销机会列表。
     */
    @GetMapping("/opportunities")
    public Mono<R<List<SearchMarketingOpportunityView>>> listOpportunities(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listOpportunities(tenantId(context),
                                new SearchMarketingOpportunityQuery(channel, sourceId, status, severity, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 按旧参数顺序查询搜索营销机会的兼容入口。
     * 该方法复用正式机会查询接口，仍沿用当前租户的访问边界。
     * 本方法只读。
     *
     * @param sourceId 数据源主键。
     * @param status 机会状态过滤条件。
     * @param channel 渠道过滤条件。
     * @param limit 返回数量上限。
     * @return 搜索营销机会列表。
     */
    public Mono<R<List<SearchMarketingOpportunityView>>> listOpportunities(Long sourceId,
                                                                           String status,
                                                                           String channel,
                                                                           Integer limit) {
        return listOpportunities(channel, sourceId, status, null, limit);
    }
    /**
     * 基于当前租户的快照数据评估搜索营销机会。
     * 评估会按命令中的范围识别排名、流量或索引问题，并生成或更新机会记录。
     * 副作用是写入机会状态和评估结果。
     *
     * @param command 评估范围、阈值和目标渠道。
     * @return 本次评估得到的机会列表。
     */
    @PostMapping("/opportunities/evaluate")
    public Mono<R<List<SearchMarketingOpportunityView>>> evaluateOpportunities(
            @RequestBody SearchMarketingOpportunityEvaluationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.evaluateOpportunities(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 更新指定搜索营销机会的处理状态。
     * 机会必须属于当前租户；状态流转用于区分新发现、处理中、忽略或已解决等阶段。
     * 副作用是修改机会状态并记录操作者。
     *
     * @param opportunityId 机会主键。
     * @param command 目标状态和处理说明。
     * @return 更新后的机会视图。
     */
    @PostMapping("/opportunities/{opportunityId}/status")
    public Mono<R<SearchMarketingOpportunityView>> updateOpportunityStatus(
            @PathVariable Long opportunityId,
            @RequestBody SearchMarketingOpportunityStatusCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.updateOpportunityStatus(tenantId(context), opportunityId, command,
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 基于指定机会生成一条待审批的搜索营销变更建议。
     * 机会必须属于当前租户；建议会继承机会上下文并进入变更审批流程。
     * 副作用是创建变更记录，初始状态通常为待审批。
     *
     * @param opportunityId 机会主键。
     * @param command 变更内容、理由和执行参数。
     * @return 创建的变更视图。
     */
    @PostMapping("/opportunities/{opportunityId}/mutations")
    public Mono<R<SearchMarketingMutationView>> proposeOpportunityMutation(
            @PathVariable Long opportunityId,
            @RequestBody SearchMarketingOpportunityMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.proposeFromOpportunity(tenantId(context), opportunityId, command,
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 直接提交一条搜索营销变更建议。
     * 变更归属当前租户和操作者，可用于人工提出关键词、落地页或投放配置调整。
     * 副作用是创建待审批的变更记录。
     *
     * @param command 变更目标、内容和业务原因。
     * @return 创建的变更视图。
     */
    @PostMapping("/mutations")
    public Mono<R<SearchMarketingMutationView>> proposeMutation(@RequestBody SearchMarketingMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.propose(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 审批通过指定搜索营销变更。
     * 变更必须属于当前租户；审批通过后才允许进入执行阶段。
     * 副作用是推进审批状态并记录审批人。
     *
     * @param mutationId 变更 ID。
     * @param command 审批意见和约束条件。
     * @return 审批后的变更视图。
     */
    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<R<SearchMarketingMutationView>> approveMutation(
            @PathVariable Long mutationId,
            @RequestBody SearchMarketingMutationApprovalCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.approve(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 执行已审批的搜索营销变更。
     * 变更必须属于当前租户且满足审批状态要求；执行会调用供应商或写入待对账记录。
     * 副作用是推进变更执行状态，并可能产生供应商变更记录。
     *
     * @param mutationId 变更 ID。
     * @param command 执行参数和幂等上下文。
     * @return 执行后的变更视图。
     */
    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<R<SearchMarketingMutationView>> executeMutation(
            @PathVariable Long mutationId,
            @RequestBody SearchMarketingMutationExecuteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.execute(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的搜索营销变更记录。
     * 可按数据源、执行状态和审批状态筛选，用于审批台和执行追踪。
     * 本接口只读。
     *
     * @param sourceId 数据源主键，可选。
     * @param status 执行状态过滤条件，可选。
     * @param approvalStatus 审批状态过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 变更记录列表。
     */
    @GetMapping("/mutations")
    public Mono<R<List<SearchMarketingMutationView>>> listMutations(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.list(tenantId(context),
                                new SearchMarketingMutationQuery(sourceId, status, approvalStatus, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的 URL 索引检查记录。
     * 可按数据源、索引状态和日期范围筛选，用于追踪落地页是否被搜索引擎收录。
     * 本接口只读。
     *
     * @param sourceId 数据源主键，可选。
     * @param indexedState 索引状态过滤条件，可选。
     * @param startDate 检查开始日期，可选。
     * @param endDate 检查结束日期，可选。
     * @param limit 返回数量上限，可选。
     * @return URL 检查记录列表。
     */
    @GetMapping("/url-inspections")
    public Mono<R<List<SearchMarketingUrlInspectionView>>> listUrlInspections(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String indexedState,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireSyncRunService().listUrlInspections(tenantId(context),
                                new SearchMarketingUrlInspectionQuery(sourceId, indexedState,
                                        startDate, endDate, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的搜索营销同步运行记录。
     * 可按数据源、运行类型和运行状态筛选，用于观察同步任务进度和失败原因。
     * 本接口只读。
     *
     * @param sourceId 数据源主键，可选。
     * @param runType 同步运行类型，可选。
     * @param status 运行状态过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 同步运行记录列表。
     */
    @GetMapping("/sync-runs")
    public Mono<R<List<SearchMarketingSyncRunView>>> listSyncRuns(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String runType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireSyncRunService().list(tenantId(context),
                                new SearchMarketingSyncRunQuery(sourceId, runType, status, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 以手动命令格式触发指定数据源同步的兼容入口。
     * 数据源按当前租户校验；未指定运行类型时默认同步表现数据。
     * 副作用是创建同步运行记录，并可能写入快照、URL 检查和供应商回执。
     *
     * @param sourceId 数据源主键。
     * @param command 手动同步窗口、游标和运行类型。
     * @return 同步运行视图。
     */
    public Mono<R<SearchMarketingSyncRunView>> syncSource(
            Long sourceId,
            SearchMarketingManualSyncCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireSyncRunService().runManual(
                                tenantId(context),
                                sourceId,
                                syncRunType(command),
                                command == null ? null : command.windowStart(),
                                command == null ? null : command.windowEnd(),
                                command == null ? null : command.cursorValue(),
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 手动触发指定搜索营销数据源同步。
     * 数据源必须属于当前租户；可选请求体用于指定同步窗口、游标和运行类型。
     * 副作用是创建同步运行记录，并可能写入新的表现快照或检查结果。
     *
     * @param sourceId 数据源主键。
     * @param request 同步窗口、游标和运行类型，可选。
     * @return 同步运行视图。
     */
    @PostMapping("/sources/{sourceId}/sync")
    public Mono<R<SearchMarketingSyncRunView>> syncSource(
            @PathVariable Long sourceId,
            @RequestBody(required = false) SearchMarketingSyncRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireSyncRunService().runManual(
                                tenantId(context),
                                sourceId,
                                syncRunType(request),
                                request == null ? null : request.windowStart(),
                                request == null ? null : request.windowEnd(),
                                request == null ? null : request.cursorValue(),
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 批量触发当前租户内到期的数据源同步。
     * 未指定数量时默认最多处理 50 个到期任务，避免一次性拉起过多外部调用。
     * 副作用是为命中的数据源创建同步运行记录。
     *
     * @param request 批量同步数量上限，可选。
     * @return 本次创建的同步运行列表。
     */
    @PostMapping("/sources/sync-due")
    public Mono<R<List<SearchMarketingSyncRunView>>> syncDue(
            @RequestBody(required = false) SearchMarketingSyncDueRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireSyncRunService().runDue(
                                tenantId(context),
                                request == null || request.limit() == null ? 50 : request.limit(),
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 按数量上限触发到期同步的兼容入口。
     * 该方法会构造正式请求体并复用租户校验、批量选择和运行记录创建逻辑。
     * 副作用与到期同步接口一致。
     *
     * @param limit 返回数量上限。
     * @return 本次创建的同步运行列表。
     */
    public Mono<R<List<SearchMarketingSyncRunView>>> syncDue(Integer limit) {
        return syncDue(new SearchMarketingSyncDueRequest(limit));
    }
    /**
     * 查询当前租户的供应商变更回执。
     * 这些记录用于追踪本系统变更与外部搜索平台最终状态的一致性。
     * 本接口只读。
     *
     * @param sourceId 数据源主键，可选。
     * @param mutationId 变更 ID，可选。
     * @param provider 供应商过滤条件，可选。
     * @param reconciliationStatus 对账状态过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 供应商变更记录列表。
     */
    @GetMapping("/provider-changes")
    public Mono<R<List<SearchMarketingProviderChangeView>>> listProviderChanges(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long mutationId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String reconciliationStatus,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReconciliationService().list(tenantId(context),
                                new SearchMarketingProviderChangeQuery(sourceId, mutationId, provider,
                                        reconciliationStatus, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 对指定搜索营销变更执行供应商状态对账。
     * 变更必须属于当前租户；对账成功后会在可用时安排影响窗口，用于后续评估变更效果。
     * 副作用是更新对账状态，并可能创建影响窗口任务。
     *
     * @param mutationId 变更 ID。
     * @return 对账结果视图。
     */
    @PostMapping("/mutations/{mutationId}/reconcile")
    public Mono<R<SearchMarketingReconciliationView>> reconcileMutation(@PathVariable Long mutationId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    SearchMarketingReconciliationView view = requireReconciliationService()
                            .reconcile(tenantId(context), mutationId, actor(context));
                    if ("RECONCILED".equals(view.status()) && impactWindowService != null) {
                        // 只有供应商侧确认生效后，才安排影响窗口观察真实业务效果。
                        impactWindowService.scheduleForReconciledMutation(tenantId(context), mutationId, actor(context));
                    }
                    return R.ok(view);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的搜索营销影响窗口。
     * 影响窗口用于观察某次机会或变更在执行后的指标变化，可按状态和决策结果筛选。
     * 本接口只读。
     *
     * @param opportunityId 机会主键，可选。
     * @param mutationId 变更 ID，可选。
     * @param sourceId 数据源主键，可选。
     * @param status 窗口状态过滤条件，可选。
     * @param decision 效果决策过滤条件，可选。
     * @param limit 返回数量上限，可选。
     * @return 影响窗口列表。
     */
    @GetMapping("/impact-windows")
    public Mono<R<List<SearchMarketingImpactWindowView>>> listImpactWindows(
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) Long mutationId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireImpactWindowService().list(tenantId(context),
                                new SearchMarketingImpactWindowQuery(opportunityId, mutationId, sourceId,
                                        status, decision, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 批量评估当前租户内已到期的影响窗口。
     * 评估会比较窗口内外指标，给出是否保留、回滚或继续观察的决策结果。
     * 副作用是更新影响窗口状态和决策。
     *
     * @param request 批量评估数量上限，可选。
     * @return 本次评估的影响窗口列表。
     */
    @PostMapping("/impact-windows/evaluate-due")
    public Mono<R<List<SearchMarketingImpactWindowView>>> evaluateDueImpactWindows(
            @RequestBody(required = false) SearchMarketingSyncDueRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireImpactWindowService().evaluateDue(
                                tenantId(context),
                                request == null || request.limit() == null ? 50 : request.limit(),
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 按数量上限评估到期影响窗口的兼容入口。
     * 该方法复用正式批量评估接口，仍会解析当前租户并记录操作者。
     * 副作用与到期影响窗口评估接口一致。
     *
     * @param limit 返回数量上限。
     * @return 本次评估的影响窗口列表。
     */
    public Mono<R<List<SearchMarketingImpactWindowView>>> evaluateDueImpactWindows(Integer limit) {
        return evaluateDueImpactWindows(new SearchMarketingSyncDueRequest(limit));
    }
    /**
     * 查询当前租户搜索营销能力的就绪度。
     * 结果用于判断数据源、同步服务和必要配置是否满足运行条件。
     * 本接口只读。
     *
     * @return 搜索营销就绪度视图。
     */
    @GetMapping("/readiness")
    public Mono<R<SearchMarketingReadinessView>> readiness() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReadinessService().readiness(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的搜索营销指标汇总。
     * 可按渠道、数据源、关键词和日期范围聚合快照数据，用于看板和趋势分析。
     * 本接口只读。
     *
     * @param channel 渠道过滤条件，可选。
     * @param sourceId 数据源主键，可选。
     * @param keywordId 关键词主键，可选。
     * @param startDate 汇总开始日期，可选。
     * @param endDate 汇总结束日期，可选。
     * @return 搜索营销汇总视图。
     */
    @GetMapping("/summary")
    public Mono<R<SearchMarketingSummaryView>> summary(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.summary(tenantId(context),
                                new SearchMarketingSummaryQuery(channel, sourceId, keywordId, startDate, endDate))))
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
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireSyncRunService 流程生成的业务结果。
     */
    private SearchMarketingSyncRunService requireSyncRunService() {
        if (syncRunService == null) {
            throw new IllegalStateException("search marketing sync run service is not configured");
        }
        return syncRunService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireReadinessService 流程生成的业务结果。
     */
    private SearchMarketingReadinessService requireReadinessService() {
        if (readinessService != null) {
            return readinessService;
        }
        if (syncRunService != null) {
            return new SearchMarketingReadinessService(syncRunService);
        }
        throw new IllegalStateException("search marketing readiness service is not configured");
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireReconciliationService 流程生成的业务结果。
     */
    private SearchMarketingReconciliationService requireReconciliationService() {
        if (reconciliationService == null) {
            throw new IllegalStateException("search marketing reconciliation service is not configured");
        }
        return reconciliationService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireImpactWindowService 流程生成的业务结果。
     */
    private SearchMarketingImpactWindowService requireImpactWindowService() {
        if (impactWindowService == null) {
            throw new IllegalStateException("search marketing impact window service is not configured");
        }
        return impactWindowService;
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    private String syncRunType(SearchMarketingSyncRequest request) {
        return request == null || request.runType() == null || request.runType().isBlank()
                ? "PERFORMANCE"
                : request.runType();
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    private String syncRunType(SearchMarketingManualSyncCommand command) {
        return command == null || command.runType() == null || command.runType().isBlank()
                ? "PERFORMANCE"
                : command.runType();
    }
}
