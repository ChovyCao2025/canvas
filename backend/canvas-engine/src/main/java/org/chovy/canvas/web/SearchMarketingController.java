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

    public SearchMarketingController(SearchMarketingService service,
                                     SearchMarketingMutationService mutationService,
                                     TenantContextResolver tenantContextResolver) {
        this(service, mutationService, null, null, null, null, tenantContextResolver);
    }

    public SearchMarketingController(SearchMarketingService service,
                                     SearchMarketingMutationService mutationService,
                                     SearchMarketingSyncRunService syncRunService,
                                     TenantContextResolver tenantContextResolver) {
        this(service, mutationService, syncRunService, null, null, null, tenantContextResolver);
    }

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

    public Mono<R<List<SearchMarketingSourceView>>> listSources(String channel, String provider, Integer limit) {
        return listSources(provider, channel, null, limit);
    }

    @PostMapping("/sources")
    public Mono<R<SearchMarketingSourceView>> upsertSource(@RequestBody SearchMarketingSourceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSource(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @PostMapping("/keywords")
    public Mono<R<SearchMarketingKeywordView>> upsertKeyword(@RequestBody SearchMarketingKeywordCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertKeyword(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    public Mono<R<List<SearchMarketingSnapshotView>>> listSnapshots(Long sourceId,
                                                                    Long keywordId,
                                                                    String channel,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    Integer limit) {
        return listSnapshots(channel, sourceId, keywordId, startDate, endDate, limit);
    }

    @PostMapping("/snapshots")
    public Mono<R<SearchMarketingSnapshotView>> recordSnapshot(@RequestBody SearchMarketingSnapshotCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.recordSnapshot(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    public Mono<R<List<SearchMarketingOpportunityView>>> listOpportunities(Long sourceId,
                                                                           String status,
                                                                           String channel,
                                                                           Integer limit) {
        return listOpportunities(channel, sourceId, status, null, limit);
    }

    @PostMapping("/opportunities/evaluate")
    public Mono<R<List<SearchMarketingOpportunityView>>> evaluateOpportunities(
            @RequestBody SearchMarketingOpportunityEvaluationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.evaluateOpportunities(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/opportunities/{opportunityId}/status")
    public Mono<R<SearchMarketingOpportunityView>> updateOpportunityStatus(
            @PathVariable Long opportunityId,
            @RequestBody SearchMarketingOpportunityStatusCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.updateOpportunityStatus(tenantId(context), opportunityId, command,
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/opportunities/{opportunityId}/mutations")
    public Mono<R<SearchMarketingMutationView>> proposeOpportunityMutation(
            @PathVariable Long opportunityId,
            @RequestBody SearchMarketingOpportunityMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.proposeFromOpportunity(tenantId(context), opportunityId, command,
                                actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations")
    public Mono<R<SearchMarketingMutationView>> proposeMutation(@RequestBody SearchMarketingMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.propose(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<R<SearchMarketingMutationView>> approveMutation(
            @PathVariable Long mutationId,
            @RequestBody SearchMarketingMutationApprovalCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.approve(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<R<SearchMarketingMutationView>> executeMutation(
            @PathVariable Long mutationId,
            @RequestBody SearchMarketingMutationExecuteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.execute(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    public Mono<R<List<SearchMarketingSyncRunView>>> syncDue(Integer limit) {
        return syncDue(new SearchMarketingSyncDueRequest(limit));
    }

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

    @PostMapping("/mutations/{mutationId}/reconcile")
    public Mono<R<SearchMarketingReconciliationView>> reconcileMutation(@PathVariable Long mutationId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    SearchMarketingReconciliationView view = requireReconciliationService()
                            .reconcile(tenantId(context), mutationId, actor(context));
                    if ("RECONCILED".equals(view.status()) && impactWindowService != null) {
                        impactWindowService.scheduleForReconciledMutation(tenantId(context), mutationId, actor(context));
                    }
                    return R.ok(view);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    public Mono<R<List<SearchMarketingImpactWindowView>>> evaluateDueImpactWindows(Integer limit) {
        return evaluateDueImpactWindows(new SearchMarketingSyncDueRequest(limit));
    }

    @GetMapping("/readiness")
    public Mono<R<SearchMarketingReadinessView>> readiness() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReadinessService().readiness(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    private SearchMarketingSyncRunService requireSyncRunService() {
        if (syncRunService == null) {
            throw new IllegalStateException("search marketing sync run service is not configured");
        }
        return syncRunService;
    }

    private SearchMarketingReadinessService requireReadinessService() {
        if (readinessService != null) {
            return readinessService;
        }
        if (syncRunService != null) {
            return new SearchMarketingReadinessService(syncRunService);
        }
        throw new IllegalStateException("search marketing readiness service is not configured");
    }

    private SearchMarketingReconciliationService requireReconciliationService() {
        if (reconciliationService == null) {
            throw new IllegalStateException("search marketing reconciliation service is not configured");
        }
        return reconciliationService;
    }

    private SearchMarketingImpactWindowService requireImpactWindowService() {
        if (impactWindowService == null) {
            throw new IllegalStateException("search marketing impact window service is not configured");
        }
        return impactWindowService;
    }

    private String syncRunType(SearchMarketingSyncRequest request) {
        return request == null || request.runType() == null || request.runType().isBlank()
                ? "PERFORMANCE"
                : request.runType();
    }

    private String syncRunType(SearchMarketingManualSyncCommand command) {
        return command == null || command.runType() == null || command.runType().isBlank()
                ? "PERFORMANCE"
                : command.runType();
    }
}
