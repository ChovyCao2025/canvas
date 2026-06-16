package org.chovy.canvas.web.bi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.bi.api.BiCatalogFacade;
import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardCloneCommand;
import org.chovy.canvas.bi.api.BiDashboardExportPackageView;
import org.chovy.canvas.bi.api.BiDashboardImportCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateCommand;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiAlertRuleCommand;
import org.chovy.canvas.bi.api.BiAlertRuleView;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentCleanupResult;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentDownload;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentView;
import org.chovy.canvas.bi.api.BiDeliveryAuditSummary;
import org.chovy.canvas.bi.api.BiDeliveryLogView;
import org.chovy.canvas.bi.api.BiDeliveryRetryResult;
import org.chovy.canvas.bi.api.BiDeliveryRunResult;
import org.chovy.canvas.bi.api.BiDeliverySchedulerResult;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiColumnPermissionCommand;
import org.chovy.canvas.bi.api.BiColumnPermissionView;
import org.chovy.canvas.bi.api.BiPermissionAuditEntryView;
import org.chovy.canvas.bi.api.BiPermissionRequestCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestReviewCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestView;
import org.chovy.canvas.bi.api.BiResourcePermissionCommand;
import org.chovy.canvas.bi.api.BiResourcePermissionView;
import org.chovy.canvas.bi.api.BiRowPermissionCommand;
import org.chovy.canvas.bi.api.BiRowPermissionView;
import org.chovy.canvas.bi.api.BiAiRequestCommand;
import org.chovy.canvas.bi.api.BiAiResponseView;
import org.chovy.canvas.bi.api.BiBigScreenResourceCommand;
import org.chovy.canvas.bi.api.BiBigScreenResourceView;
import org.chovy.canvas.bi.api.BiPortalResourceCommand;
import org.chovy.canvas.bi.api.BiPortalResourceView;
import org.chovy.canvas.bi.api.BiPublishApprovalCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalReviewCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiDatasourceHealthSnapshotView;
import org.chovy.canvas.bi.api.BiDatasourceHealthSloView;
import org.chovy.canvas.bi.api.BiDatasourceHealthView;
import org.chovy.canvas.bi.api.BiDatasourceApiPreviewCommand;
import org.chovy.canvas.bi.api.BiDatasourceApiPreviewView;
import org.chovy.canvas.bi.api.BiDatasourceConnectionTestResult;
import org.chovy.canvas.bi.api.BiDatasourceConnectorView;
import org.chovy.canvas.bi.api.BiDatasourceCredentialRotationCommand;
import org.chovy.canvas.bi.api.BiDatasourceCredentialRotationView;
import org.chovy.canvas.bi.api.BiDatasourceFileMaterializationResult;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingCommand;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaPreviewView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaSnapshotView;
import org.chovy.canvas.bi.api.BiEmbedQueryCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketCleanupResult;
import org.chovy.canvas.bi.api.BiEmbedTicketCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketPayloadView;
import org.chovy.canvas.bi.api.BiEmbedTicketVerifyCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketView;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationResult;
import org.chovy.canvas.bi.api.BiQueryCachePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryCachePolicyView;
import org.chovy.canvas.bi.api.BiQueryCacheStatsView;
import org.chovy.canvas.bi.api.BiQueryCancelResult;
import org.chovy.canvas.bi.api.BiQueryCommand;
import org.chovy.canvas.bi.api.BiQueryCompileResult;
import org.chovy.canvas.bi.api.BiQueryContractGateCommand;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiQueryExplainResult;
import org.chovy.canvas.bi.api.BiQueryGateCommand;
import org.chovy.canvas.bi.api.BiQueryGateResult;
import org.chovy.canvas.bi.api.BiQueryGovernanceAuditEntryView;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyView;
import org.chovy.canvas.bi.api.BiQueryGovernanceSummaryView;
import org.chovy.canvas.bi.api.BiQueryHistoryDetailView;
import org.chovy.canvas.bi.api.BiQueryHistoryItemView;
import org.chovy.canvas.bi.api.BiQueryResultView;
import org.chovy.canvas.bi.api.BiResourceCommentCommand;
import org.chovy.canvas.bi.api.BiResourceCommentView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiResourceLocationCommand;
import org.chovy.canvas.bi.api.BiResourceLocationView;
import org.chovy.canvas.bi.api.BiResourceLockCommand;
import org.chovy.canvas.bi.api.BiResourceLockView;
import org.chovy.canvas.bi.api.BiResourceMoveCommand;
import org.chovy.canvas.bi.api.BiResourceOwnershipView;
import org.chovy.canvas.bi.api.BiResourceTransferCommand;
import org.chovy.canvas.bi.api.BiResourceVersionView;
import org.chovy.canvas.bi.api.BiSelfServiceExportCleanupResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportCommand;
import org.chovy.canvas.bi.api.BiSelfServiceExportDownload;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobDetailView;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobView;
import org.chovy.canvas.bi.api.BiSelfServiceExportQueueResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportRetryResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportReviewCommand;
import org.chovy.canvas.bi.api.BiSelfServicePreviewCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceView;
import org.chovy.canvas.bi.api.BiSubscriptionCommand;
import org.chovy.canvas.bi.api.BiSubscriptionView;
import org.chovy.canvas.bi.api.BiWorkspaceCommand;
import org.chovy.canvas.bi.api.BiWorkspaceView;
import org.chovy.canvas.bi.domain.BiAccessRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/bi")
public class BiCatalogController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "analyst";

    private final BiCatalogFacade facade;

    public BiCatalogController(BiCatalogFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces")
    public Mono<CompatibilityEnvelope<BiWorkspaceView>> upsertWorkspace(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody WorkspaceRequest request) {
        return envelope(() -> facade.upsertWorkspace(
                tenantIdOrDefault(tenantId),
                new BiWorkspaceCommand(
                        request.workspaceKey(),
                        request.name(),
                        request.description(),
                        request.status()),
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/{datasetKey}/draft")
    public Mono<CompatibilityEnvelope<BiDatasetView>> saveDatasetDraft(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasetDraftRequest request) {
        return envelope(() -> facade.upsertDataset(
                tenantIdOrDefault(tenantId),
                        request.toCommand(datasetKey),
                        actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources")
    public Mono<CompatibilityEnvelope<BiDatasetView>> saveDatasetResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasetDraftRequest request) {
        return saveDatasetDraft(compatibilityKey(request.datasetKey(), request.name()), tenantId, actor, request);
    }

    @GetMapping("/datasets/resources")
    public Mono<CompatibilityEnvelope<List<BiDatasetView>>> listDatasetResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDatasetResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/datasets/resources/{datasetKey}")
    public Mono<CompatibilityEnvelope<BiDatasetView>> getDatasetResource(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getDatasetResource(tenantIdOrDefault(tenantId), datasetKey));
    }

    @PostMapping("/datasets/resources/{datasetKey}/publish")
    public Mono<CompatibilityEnvelope<BiDatasetView>> publishDatasetResource(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.publishDatasetResource(
                tenantIdOrDefault(tenantId),
                datasetKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/datasets/resources/{datasetKey}")
    public Mono<CompatibilityEnvelope<BiDatasetView>> archiveDatasetResource(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.archiveDatasetResource(
                tenantIdOrDefault(tenantId),
                datasetKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/datasets/resources")
    public Mono<CompatibilityEnvelope<BiDatasetView>> archiveDatasetResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ResourceKeyRequest request) {
        return envelope(() -> facade.archiveDatasetResource(
                tenantIdOrDefault(tenantId),
                request.datasetKey(),
                actorOrDefault(actor)));
    }

    @GetMapping("/datasets/resources/{datasetKey}/versions")
    public Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listDatasetResourceVersions(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listDatasetResourceVersions(tenantIdOrDefault(tenantId), datasetKey, limit));
    }

    @PostMapping("/datasets/resources/{datasetKey}/versions/{version}/restore")
    public Mono<CompatibilityEnvelope<BiDatasetView>> restoreDatasetResourceVersion(
            @PathVariable String datasetKey,
            @PathVariable Integer version,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.restoreDatasetResourceVersion(
                tenantIdOrDefault(tenantId),
                datasetKey,
                version,
                actorOrDefault(actor)));
    }

    @GetMapping("/datasets/resources/{datasetKey}/acceleration-policy")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> datasetAccelerationPolicy(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.datasetAccelerationPolicy(tenantIdOrDefault(tenantId), datasetKey));
    }

    @PostMapping("/datasets/resources/{datasetKey}/acceleration-policy")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertDatasetAccelerationPolicy(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> facade.upsertDatasetAccelerationPolicy(
                tenantIdOrDefault(tenantId),
                datasetKey,
                request == null ? Map.of() : request,
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/{datasetKey}/acceleration-refresh")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> refreshDatasetAcceleration(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.refreshDatasetAcceleration(
                tenantIdOrDefault(tenantId),
                datasetKey,
                actorOrDefault(actor)));
    }

    @GetMapping("/datasets/resources/{datasetKey}/acceleration-runs")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listDatasetAccelerationRuns(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        return envelope(() -> facade.listDatasetAccelerationRuns(tenantIdOrDefault(tenantId), datasetKey, limit));
    }

    @GetMapping("/datasets/resources/{datasetKey}/acceleration-capacity")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> datasetAccelerationCapacity(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.datasetAccelerationCapacity(tenantIdOrDefault(tenantId), datasetKey, limit));
    }

    @PostMapping("/datasets/resources/{datasetKey}/acceleration-cleanup")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> cleanupDatasetAcceleration(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "2") int retainTables) {
        return envelope(() -> facade.cleanupDatasetAcceleration(
                tenantIdOrDefault(tenantId),
                datasetKey,
                retainTables,
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/from-datasource-schema")
    public Mono<CompatibilityEnvelope<BiDatasetView>> createDatasetFromDatasourceSchemaAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasetFromDatasourceRequest request) {
        return envelope(() -> facade.upsertDataset(
                tenantIdOrDefault(tenantId),
                request.toCommand(),
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/from-datasource-schema/multi-table")
    public Mono<CompatibilityEnvelope<BiDatasetView>> createMultiTableDatasetFromDatasourceSchemaAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasetFromDatasourceRequest request) {
        return envelope(() -> facade.upsertDataset(
                tenantIdOrDefault(tenantId),
                request.toCommand(),
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/sql-preview")
    public Mono<CompatibilityEnvelope<BiQueryResultView>> previewSqlDatasetAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody SqlPreviewAliasRequest request) {
        return envelope(() -> facade.executeQuery(
                tenantIdOrDefault(tenantId),
                request.toCommand(),
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/acceleration-scheduler/run")
    public Mono<CompatibilityEnvelope<BiDeliverySchedulerResult>> runDatasetAccelerationSchedulerAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.runDeliveryScheduler(tenantIdOrDefault(tenantId), actorOrDefault(actor)));
    }

    @GetMapping("/datasets")
    public Mono<CompatibilityEnvelope<List<BiQueryDatasetView>>> listQueryDatasets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listQueryDatasets(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/datasets/{datasetKey}")
    public Mono<CompatibilityEnvelope<BiQueryDatasetView>> getQueryDataset(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getQueryDataset(tenantIdOrDefault(tenantId), datasetKey));
    }

    @GetMapping("/dashboards/presets")
    public Mono<CompatibilityEnvelope<List<BiDashboardPresetView>>> listDashboardPresets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDashboardPresets(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/dashboards/presets/{dashboardKey}")
    public Mono<CompatibilityEnvelope<BiDashboardPresetView>> getDashboardPreset(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getDashboardPreset(tenantIdOrDefault(tenantId), dashboardKey));
    }

    @GetMapping("/capacity/quick-engine")
    public Mono<CompatibilityEnvelope<BiQuickEngineCapacitySummaryView>> quickEngineCapacity(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "50") Integer limit) {
        return envelope(() -> facade.quickEngineCapacity(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/capacity/quick-engine/queue")
    public Mono<CompatibilityEnvelope<BiQuickEngineQueueSnapshotView>> quickEngineQueue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String poolKey,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") Integer limit) {
        return envelope(() -> facade.quickEngineQueue(tenantIdOrDefault(tenantId), poolKey, status, limit));
    }

    @PostMapping("/resources/favorites")
    public Mono<CompatibilityEnvelope<BiResourceFavoriteView>> favoriteResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceFavoriteCommand command) {
        return envelope(() -> facade.favoriteResource(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/resources/favorites")
    public Mono<CompatibilityEnvelope<List<BiResourceFavoriteView>>> listFavoriteResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(required = false) String resourceType) {
        return envelope(() -> facade.listFavoriteResources(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                resourceType));
    }

    @DeleteMapping("/resources/favorites/{resourceType}/{resourceKey}")
    public Mono<CompatibilityEnvelope<Void>> unfavoriteResource(
            @PathVariable String resourceType,
            @PathVariable String resourceKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.unfavoriteResource(tenantIdOrDefault(tenantId), actorOrDefault(actor), resourceType, resourceKey);
            return null;
        });
    }

    @PostMapping("/ai/ask")
    public Mono<CompatibilityEnvelope<BiAiResponseView>> askAi(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiAiRequestCommand command) {
        return aiEnvelope(tenantId, actor, "ask", command);
    }

    @PostMapping("/ai/interpret")
    public Mono<CompatibilityEnvelope<BiAiResponseView>> interpretAi(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiAiRequestCommand command) {
        return aiEnvelope(tenantId, actor, "interpret", command);
    }

    @PostMapping("/ai/report")
    public Mono<CompatibilityEnvelope<BiAiResponseView>> reportAi(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiAiRequestCommand command) {
        return aiEnvelope(tenantId, actor, "report", command);
    }

    @PostMapping("/ai/dashboard-draft")
    public Mono<CompatibilityEnvelope<BiAiResponseView>> dashboardDraftAi(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiAiRequestCommand command) {
        return aiEnvelope(tenantId, actor, "dashboard-draft", command);
    }

    @PostMapping("/ai/insights")
    public Mono<CompatibilityEnvelope<BiAiResponseView>> insightsAi(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiAiRequestCommand command) {
        return aiEnvelope(tenantId, actor, "insights", command);
    }

    @PostMapping("/resources/comments")
    public Mono<CompatibilityEnvelope<BiResourceCommentView>> addResourceComment(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceCommentCommand command) {
        return envelope(() -> facade.addResourceComment(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/resources/comments")
    public Mono<CompatibilityEnvelope<List<BiResourceCommentView>>> listResourceComments(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey) {
        return envelope(() -> facade.listResourceComments(
                tenantIdOrDefault(tenantId),
                resourceType,
                resourceKey));
    }

    @DeleteMapping("/resources/comments/{commentId}")
    public Mono<CompatibilityEnvelope<Void>> deleteResourceComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.deleteResourceComment(tenantIdOrDefault(tenantId), actorOrDefault(actor), commentId);
            return null;
        });
    }

    @PostMapping("/resources/locks/acquire")
    public Mono<CompatibilityEnvelope<BiResourceLockView>> acquireResourceLock(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceLockCommand command) {
        return envelope(() -> facade.acquireResourceLock(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/resources/locks")
    public Mono<CompatibilityEnvelope<BiResourceLockView>> currentResourceLock(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String resourceType,
            @RequestParam String resourceKey) {
        return envelope(() -> facade.currentResourceLock(
                tenantIdOrDefault(tenantId),
                resourceType,
                resourceKey));
    }

    @PostMapping("/resources/locks/release")
    public Mono<CompatibilityEnvelope<Void>> releaseResourceLock(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceLockCommand command) {
        return envelope(() -> {
            facade.releaseResourceLock(tenantIdOrDefault(tenantId), actorOrDefault(actor), command);
            return null;
        });
    }

    @PostMapping("/resources/locations")
    public Mono<CompatibilityEnvelope<BiResourceLocationView>> updateResourceLocation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceLocationCommand command) {
        return envelope(() -> facade.updateResourceLocation(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/resources/move")
    public Mono<CompatibilityEnvelope<BiResourceLocationView>> moveResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceMoveCommand command) {
        return envelope(() -> facade.moveResource(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/resources/locations")
    public Mono<CompatibilityEnvelope<List<BiResourceLocationView>>> listResourceLocations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String resourceType) {
        return envelope(() -> facade.listResourceLocations(tenantIdOrDefault(tenantId), resourceType));
    }

    @PostMapping("/resources/transfer")
    public Mono<CompatibilityEnvelope<BiResourceOwnershipView>> transferResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceTransferCommand command) {
        return envelope(() -> facade.transferResource(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/resources/ownerships")
    public Mono<CompatibilityEnvelope<List<BiResourceOwnershipView>>> listResourceOwnerships(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String resourceType) {
        return envelope(() -> facade.listResourceOwnerships(tenantIdOrDefault(tenantId), resourceType));
    }

    @GetMapping("/resources/publish-approvals")
    public Mono<CompatibilityEnvelope<List<BiPublishApprovalView>>> listPublishApprovals(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listPublishApprovals(
                tenantIdOrDefault(tenantId),
                resourceType,
                resourceKey,
                status));
    }

    @PostMapping("/resources/publish-approvals")
    public Mono<CompatibilityEnvelope<BiPublishApprovalView>> requestPublishApproval(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiPublishApprovalCommand command) {
        return envelope(() -> facade.requestPublishApproval(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/resources/publish-approvals/{approvalId}/review")
    public Mono<CompatibilityEnvelope<BiPublishApprovalView>> reviewPublishApproval(
            @PathVariable Long approvalId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiPublishApprovalReviewCommand command) {
        BiPublishApprovalReviewCommand merged = new BiPublishApprovalReviewCommand(
                approvalId,
                command == null ? null : command.status(),
                command == null ? null : command.reviewComment());
        return envelope(() -> facade.reviewPublishApproval(
                tenantIdOrDefault(tenantId),
                merged,
                actorOrDefault(actor)));
    }

    @PostMapping("/capacity/quick-engine/alert-policy")
    public Mono<CompatibilityEnvelope<BiQuickEngineCapacityAlertPolicyView>> updateQuickEngineCapacityAlertPolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQuickEngineCapacityAlertPolicyCommand command) {
        return envelope(() -> facade.updateQuickEngineCapacityAlertPolicy(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/capacity/quick-engine/tenant-pool-policy")
    public Mono<CompatibilityEnvelope<BiQuickEngineTenantPoolPolicyView>> updateQuickEngineTenantPoolPolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQuickEngineTenantPoolPolicyCommand command) {
        return envelope(() -> facade.updateQuickEngineTenantPoolPolicy(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/charts/resources/{chartKey}/draft")
    public Mono<CompatibilityEnvelope<BiChartView>> saveChartDraft(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ChartDraftRequest request) {
        return envelope(() -> facade.upsertChart(
                tenantIdOrDefault(tenantId),
                        request.toCommand(chartKey),
                        actorOrDefault(actor)));
    }

    @PostMapping("/charts/resources")
    public Mono<CompatibilityEnvelope<BiChartView>> saveChartResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ChartDraftRequest request) {
        return saveChartDraft(compatibilityKey(request.chartKey(), request.name()), tenantId, actor, request);
    }

    @GetMapping("/charts/resources")
    public Mono<CompatibilityEnvelope<List<BiChartView>>> listChartResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listChartResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/charts/resources/{chartKey}")
    public Mono<CompatibilityEnvelope<BiChartView>> getChartResource(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getChartResource(tenantIdOrDefault(tenantId), chartKey));
    }

    @GetMapping("/charts/resources/{chartKey}/impact")
    public Mono<CompatibilityEnvelope<BiChartReferenceImpactView>> chartReferenceImpact(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.chartReferenceImpact(tenantIdOrDefault(tenantId), chartKey));
    }

    @PostMapping("/charts/resources/{chartKey}/publish")
    public Mono<CompatibilityEnvelope<BiChartView>> publishChartResource(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.publishChartResource(
                tenantIdOrDefault(tenantId),
                chartKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/charts/resources/{chartKey}")
    public Mono<CompatibilityEnvelope<Void>> archiveChartResource(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.archiveChartResource(tenantIdOrDefault(tenantId), chartKey, actorOrDefault(actor));
            return null;
        });
    }

    @DeleteMapping(value = "/charts/resources", params = "chartKey")
    public Mono<CompatibilityEnvelope<Void>> archiveChartResourceQueryAlias(
            @RequestParam String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return archiveChartResource(chartKey, tenantId, actor);
    }

    @DeleteMapping(value = "/charts/resources", params = "!chartKey")
    public Mono<CompatibilityEnvelope<Void>> archiveChartResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ResourceKeyRequest request) {
        return archiveChartResource(request.chartKey(), tenantId, actor);
    }

    @GetMapping("/charts/resources/{chartKey}/versions")
    public Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listChartResourceVersions(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listChartResourceVersions(tenantIdOrDefault(tenantId), chartKey));
    }

    @PostMapping("/charts/resources/{chartKey}/versions/{version}/restore")
    public Mono<CompatibilityEnvelope<BiChartView>> restoreChartResourceVersion(
            @PathVariable String chartKey,
            @PathVariable Integer version,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.restoreChartResourceVersion(
                tenantIdOrDefault(tenantId),
                chartKey,
                version,
                actorOrDefault(actor)));
    }

    @PostMapping("/dashboards/resources/{dashboardKey}/draft")
    public Mono<CompatibilityEnvelope<BiDashboardView>> saveDashboardDraft(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DashboardDraftRequest request) {
        return envelope(() -> facade.upsertDashboard(
                tenantIdOrDefault(tenantId),
                        request.toCommand(dashboardKey),
                        actorOrDefault(actor)));
    }

    @PostMapping("/dashboards/resources")
    public Mono<CompatibilityEnvelope<BiDashboardView>> saveDashboardResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DashboardDraftRequest request) {
        return saveDashboardDraft(compatibilityKey(request.dashboardKey(), request.name()), tenantId, actor, request);
    }

    @GetMapping(value = "/dashboards/resources", params = "!workspaceId")
    public Mono<CompatibilityEnvelope<List<BiDashboardView>>> listDashboardResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDashboardResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping(value = "/dashboards/resources", params = "workspaceId")
    public Mono<CompatibilityEnvelope<List<BiDashboardView>>> listDashboardResourcesByWorkspaceAlias(
            @RequestParam Long workspaceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return listDashboardResources(tenantId);
    }

    @GetMapping(value = "/dashboards/resources/{dashboardKey}", params = "!workspaceId")
    public Mono<CompatibilityEnvelope<BiDashboardView>> getDashboardResource(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getDashboardResource(tenantIdOrDefault(tenantId), dashboardKey));
    }

    @GetMapping(value = "/dashboards/resources/{dashboardKey}", params = "workspaceId")
    public Mono<CompatibilityEnvelope<BiDashboardReadModelView>> dashboardReadModel(
            @PathVariable String dashboardKey,
            @RequestParam Long workspaceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.dashboardReadModel(tenantIdOrDefault(tenantId), workspaceId, dashboardKey));
    }

    @PostMapping("/dashboards/resources/{dashboardKey}/clone")
    public Mono<CompatibilityEnvelope<BiDashboardView>> cloneDashboardResource(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DashboardCloneRequest request) {
        return envelope(() -> facade.cloneDashboardResource(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                dashboardKey,
                request.toCommand()));
    }

    @GetMapping("/dashboards/resources/{dashboardKey}/export")
    public Mono<CompatibilityEnvelope<BiDashboardExportPackageView>> exportDashboardResource(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.exportDashboardResource(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                dashboardKey));
    }

    @GetMapping("/dashboards/resources/{dashboardKey}/export-file")
    public Mono<ResponseEntity<byte[]>> exportDashboardResourceFile(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return Mono.fromSupplier(() -> {
            var file = facade.exportDashboardResourceFile(
                    tenantIdOrDefault(tenantId),
                    actorOrDefault(actor),
                    dashboardKey);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.contentType()))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(file.filename()).build().toString())
                    .body(file.content());
        });
    }

    @PostMapping("/dashboards/resources/import")
    public Mono<CompatibilityEnvelope<BiDashboardView>> importDashboardResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiDashboardImportCommand command) {
        return envelope(() -> facade.importDashboardResource(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                command));
    }

    @PostMapping(value = "/dashboards/resources/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<CompatibilityEnvelope<BiDashboardView>> importDashboardResourceFile(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestPart("file") FilePart file,
            @RequestParam String dashboardKey,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        BiDashboardView dashboard = new BiDashboardView(
                null,
                tenantIdOrDefault(tenantId),
                1L,
                dashboardKey,
                title == null || title.isBlank() ? dashboardKey : title,
                "Imported from " + file.filename(),
                Map.of(),
                Map.of(),
                List.of(),
                "DRAFT",
                1,
                actorOrDefault(actor),
                null,
                null);
        BiDashboardExportPackageView packageView = new BiDashboardExportPackageView(
                "DASHBOARD",
                dashboardKey,
                dashboard,
                Map.of("filename", file.filename()),
                null,
                actorOrDefault(actor));
        return envelope(() -> facade.importDashboardResource(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                new BiDashboardImportCommand(packageView, dashboardKey, title, overwrite)));
    }

    @PostMapping("/dashboards/resources/{dashboardKey}/publish")
    public Mono<CompatibilityEnvelope<BiDashboardView>> publishDashboardResource(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.publishDashboardResource(
                tenantIdOrDefault(tenantId),
                dashboardKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/dashboards/resources/{dashboardKey}")
    public Mono<CompatibilityEnvelope<BiDashboardView>> archiveDashboardResource(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.archiveDashboardResource(
                tenantIdOrDefault(tenantId),
                dashboardKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping(value = "/dashboards/resources", params = "dashboardKey")
    public Mono<CompatibilityEnvelope<BiDashboardView>> archiveDashboardResourceQueryAlias(
            @RequestParam String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return archiveDashboardResource(dashboardKey, tenantId, actor);
    }

    @DeleteMapping(value = "/dashboards/resources", params = "!dashboardKey")
    public Mono<CompatibilityEnvelope<BiDashboardView>> archiveDashboardResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ResourceKeyRequest request) {
        return archiveDashboardResource(request.dashboardKey(), tenantId, actor);
    }

    @GetMapping("/dashboards/resources/{dashboardKey}/versions")
    public Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listDashboardResourceVersions(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listDashboardResourceVersions(
                tenantIdOrDefault(tenantId),
                dashboardKey,
                limit));
    }

    @PostMapping("/dashboards/resources/{dashboardKey}/versions/{version}/restore")
    public Mono<CompatibilityEnvelope<BiDashboardView>> restoreDashboardResourceVersion(
            @PathVariable String dashboardKey,
            @PathVariable Integer version,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.restoreDashboardResourceVersion(
                tenantIdOrDefault(tenantId),
                dashboardKey,
                version,
                actorOrDefault(actor)));
    }

    @GetMapping("/dashboards/resources/{dashboardKey}/runtime-state")
    public Mono<CompatibilityEnvelope<BiDashboardRuntimeStateView>> getDashboardRuntimeState(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.getDashboardRuntimeState(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                dashboardKey));
    }

    @PostMapping("/dashboards/resources/{dashboardKey}/runtime-state")
    public Mono<CompatibilityEnvelope<BiDashboardRuntimeStateView>> saveDashboardRuntimeState(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiDashboardRuntimeStateCommand command) {
        return envelope(() -> facade.saveDashboardRuntimeState(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                dashboardKey,
                command));
    }

    @GetMapping("/portals/resources")
    public Mono<CompatibilityEnvelope<List<BiPortalResourceView>>> listPortalResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listPortalResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/portals/resources/{portalKey}")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> getPortalResource(
            @PathVariable String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getPortalResource(tenantIdOrDefault(tenantId), portalKey));
    }

    @PostMapping("/portals/resources/{portalKey}/draft")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> savePortalDraft(
            @PathVariable String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody PortalDraftRequest request) {
        return envelope(() -> facade.savePortalDraft(
                tenantIdOrDefault(tenantId),
                portalKey,
                        request.toCommand(),
                        actorOrDefault(actor)));
    }

    @PostMapping("/portals/resources")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> savePortalResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody PortalDraftRequest request) {
        return savePortalDraft(compatibilityKey(request.portalKey(), request.title()), tenantId, actor, request);
    }

    @PostMapping("/portals/resources/{portalKey}/publish")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> publishPortalResource(
            @PathVariable String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.publishPortalResource(
                tenantIdOrDefault(tenantId),
                portalKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/portals/resources/{portalKey}")
    public Mono<CompatibilityEnvelope<Void>> archivePortalResource(
            @PathVariable String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.archivePortalResource(tenantIdOrDefault(tenantId), portalKey, actorOrDefault(actor));
            return null;
        });
    }

    @DeleteMapping(value = "/portals/resources", params = "portalKey")
    public Mono<CompatibilityEnvelope<Void>> archivePortalResourceQueryAlias(
            @RequestParam String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return archivePortalResource(portalKey, tenantId, actor);
    }

    @DeleteMapping(value = "/portals/resources", params = "!portalKey")
    public Mono<CompatibilityEnvelope<Void>> archivePortalResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ResourceKeyRequest request) {
        return archivePortalResource(request.portalKey(), tenantId, actor);
    }

    @GetMapping("/portals/runtime")
    public Mono<CompatibilityEnvelope<List<BiPortalResourceView>>> listPortalRuntime(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listPortalResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/portals/runtime/{portalKey}")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> getPortalRuntime(
            @PathVariable String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getPortalResource(tenantIdOrDefault(tenantId), portalKey));
    }

    @GetMapping("/portals/resources/{portalKey}/versions")
    public Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listPortalResourceVersions(
            @PathVariable String portalKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listPortalResourceVersions(tenantIdOrDefault(tenantId), portalKey));
    }

    @PostMapping("/portals/resources/{portalKey}/versions/{version}/restore")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> restorePortalResourceVersion(
            @PathVariable String portalKey,
            @PathVariable Integer version,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.restorePortalResourceVersion(
                tenantIdOrDefault(tenantId),
                portalKey,
                version,
                actorOrDefault(actor)));
    }

    @GetMapping("/big-screens/resources")
    public Mono<CompatibilityEnvelope<List<BiBigScreenResourceView>>> listBigScreenResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listBigScreenResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/big-screens/resources/{screenKey}")
    public Mono<CompatibilityEnvelope<BiBigScreenResourceView>> getBigScreenResource(
            @PathVariable String screenKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getBigScreenResource(tenantIdOrDefault(tenantId), screenKey));
    }

    @PostMapping("/big-screens/resources/{screenKey}/draft")
    public Mono<CompatibilityEnvelope<BiBigScreenResourceView>> saveBigScreenDraft(
            @PathVariable String screenKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BigScreenDraftRequest request) {
        return envelope(() -> facade.saveBigScreenDraft(
                tenantIdOrDefault(tenantId),
                screenKey,
                        request.toCommand(),
                        actorOrDefault(actor)));
    }

    @PostMapping("/big-screens/resources")
    public Mono<CompatibilityEnvelope<BiBigScreenResourceView>> saveBigScreenResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BigScreenDraftRequest request) {
        return saveBigScreenDraft(compatibilityKey(request.screenKey(), request.title()), tenantId, actor, request);
    }

    @PostMapping("/big-screens/resources/{screenKey}/publish")
    public Mono<CompatibilityEnvelope<BiBigScreenResourceView>> publishBigScreenResource(
            @PathVariable String screenKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.publishBigScreenResource(
                tenantIdOrDefault(tenantId),
                screenKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/big-screens/resources/{screenKey}")
    public Mono<CompatibilityEnvelope<Void>> archiveBigScreenResource(
            @PathVariable String screenKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.archiveBigScreenResource(tenantIdOrDefault(tenantId), screenKey, actorOrDefault(actor));
            return null;
        });
    }

    @DeleteMapping(value = "/big-screens/resources", params = "screenKey")
    public Mono<CompatibilityEnvelope<Void>> archiveBigScreenResourceQueryAlias(
            @RequestParam String screenKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return archiveBigScreenResource(screenKey, tenantId, actor);
    }

    @DeleteMapping(value = "/big-screens/resources", params = "!screenKey")
    public Mono<CompatibilityEnvelope<Void>> archiveBigScreenResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ResourceKeyRequest request) {
        return archiveBigScreenResource(request.screenKey(), tenantId, actor);
    }

    @GetMapping("/big-screens/resources/{screenKey}/versions")
    public Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listBigScreenResourceVersions(
            @PathVariable String screenKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listBigScreenResourceVersions(tenantIdOrDefault(tenantId), screenKey));
    }

    @PostMapping("/big-screens/resources/{screenKey}/versions/{version}/restore")
    public Mono<CompatibilityEnvelope<BiBigScreenResourceView>> restoreBigScreenResourceVersion(
            @PathVariable String screenKey,
            @PathVariable Integer version,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.restoreBigScreenResourceVersion(
                tenantIdOrDefault(tenantId),
                screenKey,
                version,
                actorOrDefault(actor)));
    }

    @GetMapping("/spreadsheets/resources")
    public Mono<CompatibilityEnvelope<List<BiSpreadsheetResourceView>>> listSpreadsheetResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listSpreadsheetResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/spreadsheets/resources/{spreadsheetKey}")
    public Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> getSpreadsheetResource(
            @PathVariable String spreadsheetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getSpreadsheetResource(tenantIdOrDefault(tenantId), spreadsheetKey));
    }

    @PostMapping("/spreadsheets/resources/{spreadsheetKey}/draft")
    public Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> saveSpreadsheetDraft(
            @PathVariable String spreadsheetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody SpreadsheetDraftRequest request) {
        return envelope(() -> facade.saveSpreadsheetDraft(
                tenantIdOrDefault(tenantId),
                spreadsheetKey,
                        request.toCommand(),
                        actorOrDefault(actor)));
    }

    @PostMapping("/spreadsheets/resources")
    public Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> saveSpreadsheetResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody SpreadsheetDraftRequest request) {
        return saveSpreadsheetDraft(compatibilityKey(request.spreadsheetKey(), request.name()), tenantId, actor, request);
    }

    @PostMapping("/spreadsheets/resources/{spreadsheetKey}/publish")
    public Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> publishSpreadsheetResource(
            @PathVariable String spreadsheetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.publishSpreadsheetResource(
                tenantIdOrDefault(tenantId),
                spreadsheetKey,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/spreadsheets/resources/{spreadsheetKey}")
    public Mono<CompatibilityEnvelope<Void>> archiveSpreadsheetResource(
            @PathVariable String spreadsheetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.archiveSpreadsheetResource(tenantIdOrDefault(tenantId), spreadsheetKey, actorOrDefault(actor));
            return null;
        });
    }

    @DeleteMapping(value = "/spreadsheets/resources", params = "spreadsheetKey")
    public Mono<CompatibilityEnvelope<Void>> archiveSpreadsheetResourceQueryAlias(
            @RequestParam String spreadsheetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return archiveSpreadsheetResource(spreadsheetKey, tenantId, actor);
    }

    @DeleteMapping(value = "/spreadsheets/resources", params = "!spreadsheetKey")
    public Mono<CompatibilityEnvelope<Void>> archiveSpreadsheetResourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ResourceKeyRequest request) {
        return archiveSpreadsheetResource(request.spreadsheetKey(), tenantId, actor);
    }

    @GetMapping("/spreadsheets/resources/{spreadsheetKey}/versions")
    public Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listSpreadsheetResourceVersions(
            @PathVariable String spreadsheetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> limited(
                facade.listSpreadsheetResourceVersions(tenantIdOrDefault(tenantId), spreadsheetKey),
                limit));
    }

    @PostMapping("/spreadsheets/resources/{spreadsheetKey}/versions/{version}/restore")
    public Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> restoreSpreadsheetResourceVersion(
            @PathVariable String spreadsheetKey,
            @PathVariable Integer version,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.restoreSpreadsheetResourceVersion(
                tenantIdOrDefault(tenantId),
                spreadsheetKey,
                version,
                actorOrDefault(actor)));
    }

    @PostMapping("/query/compile")
    public Mono<CompatibilityEnvelope<BiQueryCompileResult>> compileQuery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryCommand command) {
        return envelope(() -> facade.compileQuery(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/query/execute")
    public Mono<CompatibilityEnvelope<BiQueryResultView>> executeQuery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryCommand command) {
        return envelope(() -> facade.executeQuery(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/query/explain")
    public Mono<CompatibilityEnvelope<BiQueryExplainResult>> explainQuery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryCommand command) {
        return envelope(() -> facade.explainQuery(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/query/cancel/{sqlHash}")
    public Mono<CompatibilityEnvelope<BiQueryCancelResult>> cancelQuery(
            @PathVariable String sqlHash,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.cancelQuery(tenantIdOrDefault(tenantId), sqlHash, actorOrDefault(actor)));
    }

    @PostMapping("/query/execute-gated")
    public Mono<CompatibilityEnvelope<BiQueryGateResult>> executeGatedQuery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryGateCommand command) {
        return envelope(() -> facade.executeGatedQuery(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/query/execute-contract-gated")
    public Mono<CompatibilityEnvelope<BiQueryGateResult>> executeContractGatedQuery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryContractGateCommand command) {
        return envelope(() -> facade.executeContractGatedQuery(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/query/history")
    public Mono<CompatibilityEnvelope<List<BiQueryHistoryItemView>>> listQueryHistory(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listQueryHistory(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/query/history/{historyId}")
    public Mono<CompatibilityEnvelope<BiQueryHistoryDetailView>> queryHistoryDetail(
            @PathVariable Long historyId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.queryHistoryDetail(tenantIdOrDefault(tenantId), historyId));
    }

    @GetMapping("/query/governance-summary")
    public Mono<CompatibilityEnvelope<BiQueryGovernanceSummaryView>> queryGovernanceSummary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "100") int limit) {
        return envelope(() -> facade.queryGovernanceSummary(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/query/governance-policy")
    public Mono<CompatibilityEnvelope<BiQueryGovernancePolicyView>> queryGovernancePolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.queryGovernancePolicy(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/query/governance-policy")
    public Mono<CompatibilityEnvelope<BiQueryGovernancePolicyView>> updateQueryGovernancePolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryGovernancePolicyCommand command) {
        return envelope(() -> facade.updateQueryGovernancePolicy(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/query/governance-audit")
    public Mono<CompatibilityEnvelope<List<BiQueryGovernanceAuditEntryView>>> queryGovernanceAudit(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.queryGovernanceAudit(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/query/cache-policy")
    public Mono<CompatibilityEnvelope<BiQueryCachePolicyView>> queryCachePolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.queryCachePolicy(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/query/cache-policy")
    public Mono<CompatibilityEnvelope<BiQueryCachePolicyView>> updateQueryCachePolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQueryCachePolicyCommand command) {
        return envelope(() -> facade.updateQueryCachePolicy(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/query/cache/invalidate")
    public Mono<CompatibilityEnvelope<BiQueryCacheInvalidationResult>> invalidateQueryCache(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody BiQueryCacheInvalidationCommand command) {
        return envelope(() -> facade.invalidateQueryCache(tenantIdOrDefault(tenantId), command));
    }

    @GetMapping("/query/cache-stats")
    public Mono<CompatibilityEnvelope<BiQueryCacheStatsView>> queryCacheStats(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.queryCacheStats(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/datasources/health")
    public Mono<CompatibilityEnvelope<List<BiDatasourceHealthView>>> datasourceHealth() {
        return envelope(facade::datasourceHealth);
    }

    @GetMapping("/datasources/health/history")
    public Mono<CompatibilityEnvelope<List<BiDatasourceHealthSnapshotView>>> datasourceHealthHistory(
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.datasourceHealthHistory(limit));
    }

    @GetMapping("/datasources/health/slo")
    public Mono<CompatibilityEnvelope<BiDatasourceHealthSloView>> datasourceHealthSlo(
            @RequestParam(defaultValue = "100") int limit) {
        return envelope(() -> facade.datasourceHealthSlo(limit));
    }

    @GetMapping("/datasources/connectors")
    public Mono<CompatibilityEnvelope<List<BiDatasourceConnectorView>>> datasourceConnectors() {
        return envelope(facade::datasourceConnectors);
    }

    @GetMapping("/datasources/onboarding")
    public Mono<CompatibilityEnvelope<List<BiDatasourceOnboardingView>>> listDatasources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDatasources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/datasources")
    public Mono<CompatibilityEnvelope<List<BiDatasourceOnboardingView>>> listDatasourcesAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return listDatasources(tenantId);
    }

    @PostMapping("/datasources/onboarding")
    public Mono<CompatibilityEnvelope<BiDatasourceOnboardingView>> createDatasource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiDatasourceOnboardingCommand command) {
        return envelope(() -> facade.createDatasource(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/datasources")
    public Mono<CompatibilityEnvelope<BiDatasourceOnboardingView>> createDatasourceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasourceAliasRequest request) {
        return createDatasource(tenantId, actor, request.toCommand());
    }

    @PostMapping(value = "/datasources/file-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<CompatibilityEnvelope<BiDatasourceOnboardingView>> uploadDatasourceFile(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestPart("file") FilePart file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sheetName,
            @RequestParam(defaultValue = ",") String delimiter,
            @RequestParam(defaultValue = "true") boolean headerRow,
            @RequestParam(defaultValue = "UTF-8") String encoding) {
        return envelope(() -> facade.uploadDatasourceFile(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                file.filename(),
                name,
                description,
                sheetName,
                delimiter,
                headerRow,
                encoding));
    }

    @PostMapping(value = "/datasources/file-upload/materialize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<CompatibilityEnvelope<BiDatasourceFileMaterializationResult>> materializeDatasourceFile(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestPart("file") FilePart file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sheetName,
            @RequestParam(defaultValue = ",") String delimiter,
            @RequestParam(defaultValue = "true") boolean headerRow,
            @RequestParam(defaultValue = "UTF-8") String encoding,
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String datasetName,
            @RequestParam(defaultValue = "tenant_id") String tenantColumn,
            @RequestParam(defaultValue = "200") int schemaLimit,
            @RequestParam(defaultValue = "100000") long maxRows) {
        return envelope(() -> facade.materializeDatasourceFile(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                file.filename(),
                name,
                description,
                sheetName,
                delimiter,
                headerRow,
                encoding,
                datasetKey,
                datasetName,
                tenantColumn,
                schemaLimit,
                maxRows));
    }

    @PutMapping("/datasources/onboarding/{id}")
    public Mono<CompatibilityEnvelope<BiDatasourceOnboardingView>> updateDatasource(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiDatasourceOnboardingCommand command) {
        return envelope(() -> facade.updateDatasource(
                tenantIdOrDefault(tenantId),
                id,
                command,
                actorOrDefault(actor)));
    }

    @PutMapping("/datasources")
    public Mono<CompatibilityEnvelope<BiDatasourceOnboardingView>> updateDatasourceAlias(
            @RequestParam(required = false) Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasourceAliasRequest request) {
        return updateDatasource(id == null ? request.id() : id, tenantId, actor, request.toCommand());
    }

    @PostMapping("/datasources/{id}/connection-test")
    public Mono<CompatibilityEnvelope<BiDatasourceConnectionTestResult>> testDatasourceConnection(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.testDatasourceConnection(tenantIdOrDefault(tenantId), id));
    }

    @PostMapping("/datasources/{id}/credential-rotation")
    public Mono<CompatibilityEnvelope<BiDatasourceCredentialRotationView>> rotateDatasourceCredential(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) BiDatasourceCredentialRotationCommand command) {
        return envelope(() -> facade.rotateDatasourceCredential(
                tenantIdOrDefault(tenantId),
                id,
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/datasources/{id}/schema-preview")
    public Mono<CompatibilityEnvelope<BiDatasourceSchemaPreviewView>> previewDatasourceSchema(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "100") int limit) {
        return envelope(() -> facade.previewDatasourceSchema(tenantIdOrDefault(tenantId), id, limit));
    }

    @PostMapping("/datasources/{id}/api-preview")
    public Mono<CompatibilityEnvelope<BiDatasourceApiPreviewView>> previewDatasourceApi(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) BiDatasourceApiPreviewCommand command) {
        return envelope(() -> facade.previewDatasourceApi(tenantIdOrDefault(tenantId), id, command));
    }

    @PostMapping("/datasources/{id}/schema-sync")
    public Mono<CompatibilityEnvelope<BiDatasourceSchemaSnapshotView>> syncDatasourceSchema(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "100") int limit,
            @RequestBody(required = false) BiDatasourceApiPreviewCommand command) {
        return envelope(() -> facade.syncDatasourceSchema(
                tenantIdOrDefault(tenantId),
                id,
                limit,
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/datasources/{id}/schema-snapshot")
    public Mono<CompatibilityEnvelope<BiDatasourceSchemaSnapshotView>> latestDatasourceSchemaSnapshot(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.latestDatasourceSchemaSnapshot(tenantIdOrDefault(tenantId), id));
    }

    @GetMapping("/datasources/{id}/schema-snapshots")
    public Mono<CompatibilityEnvelope<List<BiDatasourceSchemaSnapshotView>>> listDatasourceSchemaSnapshots(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listDatasourceSchemaSnapshots(tenantIdOrDefault(tenantId), id, limit));
    }

    @PostMapping("/embed-tickets")
    public Mono<CompatibilityEnvelope<BiEmbedTicketView>> createEmbedTicket(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiEmbedTicketCommand command) {
        return envelope(() -> facade.createEmbedTicket(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/embed-tickets/verify")
    public Mono<CompatibilityEnvelope<BiEmbedTicketPayloadView>> verifyEmbedTicket(
            @RequestBody BiEmbedTicketVerifyCommand command,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return envelope(() -> facade.verifyEmbedTicket(command, originOrReferer(origin, referer)));
    }

    @PostMapping("/embed/query/execute")
    public Mono<CompatibilityEnvelope<BiQueryResultView>> executeEmbedQuery(
            @RequestBody BiEmbedQueryCommand command,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return envelope(() -> facade.executeEmbedQuery(command, originOrReferer(origin, referer)));
    }

    @PostMapping("/embed-tickets/cleanup")
    public Mono<CompatibilityEnvelope<BiEmbedTicketCleanupResult>> cleanupEmbedTickets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "100") int limit) {
        return envelope(() -> facade.cleanupEmbedTickets(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/embed/resources/dashboard")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> embedDashboardResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody EmbedResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return envelope(() -> dashboardEmbedPayload(facade.embedDashboardResource(
                tenantIdOrDefault(tenantId),
                request.resourceKey(),
                request.ticket(),
                originOrReferer(origin, referer))));
    }

    @PostMapping("/embed/resources/dashboard/runtime-state")
    public Mono<CompatibilityEnvelope<BiDashboardRuntimeStateView>> embedDashboardRuntimeState(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody EmbedResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return envelope(() -> facade.embedDashboardRuntimeState(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                request.resourceKey(),
                request.ticket(),
                originOrReferer(origin, referer)));
    }

    @PostMapping("/embed/resources/portal")
    public Mono<CompatibilityEnvelope<BiPortalResourceView>> embedPortalResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody EmbedResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return envelope(() -> facade.embedPortalResource(
                tenantIdOrDefault(tenantId),
                request.resourceKey(),
                request.ticket(),
                originOrReferer(origin, referer)));
    }

    @GetMapping("/permissions/resources")
    public Mono<CompatibilityEnvelope<List<BiResourcePermissionView>>> listResourcePermissions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) Long resourceId) {
        return envelope(() -> facade.listResourcePermissions(
                tenantIdOrDefault(tenantId),
                resourceType,
                resourceKey,
                resourceId));
    }

    @PostMapping("/permissions/resources")
    public Mono<CompatibilityEnvelope<BiResourcePermissionView>> upsertResourcePermission(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourcePermissionCommand command) {
        return envelope(() -> facade.upsertResourcePermission(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/permissions/resources/{id}")
    public Mono<CompatibilityEnvelope<Void>> deleteResourcePermission(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.deleteResourcePermission(tenantIdOrDefault(tenantId), actorOrDefault(actor), id);
            return null;
        });
    }

    @GetMapping("/permissions/rows")
    public Mono<CompatibilityEnvelope<List<BiRowPermissionView>>> listRowPermissions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String datasetKey) {
        return envelope(() -> facade.listRowPermissions(tenantIdOrDefault(tenantId), datasetKey));
    }

    @PostMapping("/permissions/rows")
    public Mono<CompatibilityEnvelope<BiRowPermissionView>> upsertRowPermission(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiRowPermissionCommand command) {
        return envelope(() -> facade.upsertRowPermission(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @DeleteMapping("/permissions/rows/{id}")
    public Mono<CompatibilityEnvelope<Void>> deleteRowPermission(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.deleteRowPermission(tenantIdOrDefault(tenantId), actorOrDefault(actor), id);
            return null;
        });
    }

    @GetMapping("/permissions/columns")
    public Mono<CompatibilityEnvelope<List<BiColumnPermissionView>>> listColumnPermissions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String datasetKey) {
        return envelope(() -> facade.listColumnPermissions(tenantIdOrDefault(tenantId), datasetKey));
    }

    @PostMapping("/permissions/columns")
    public Mono<CompatibilityEnvelope<BiColumnPermissionView>> upsertColumnPermission(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiColumnPermissionCommand command) {
        return envelope(() -> facade.upsertColumnPermission(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/permissions/columns/{id}")
    public Mono<CompatibilityEnvelope<Void>> deleteColumnPermission(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.deleteColumnPermission(tenantIdOrDefault(tenantId), actorOrDefault(actor), id);
            return null;
        });
    }

    @GetMapping("/permissions/audit")
    public Mono<CompatibilityEnvelope<List<BiPermissionAuditEntryView>>> permissionAudit(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.permissionAudit(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/permissions/requests")
    public Mono<CompatibilityEnvelope<List<BiPermissionRequestView>>> listPermissionRequests(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listPermissionRequests(
                tenantIdOrDefault(tenantId),
                resourceType,
                resourceKey,
                status));
    }

    @PostMapping("/permissions/requests")
    public Mono<CompatibilityEnvelope<BiPermissionRequestView>> requestPermission(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiPermissionRequestCommand command) {
        return envelope(() -> facade.requestPermission(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/permissions/requests/{id}/review")
    public Mono<CompatibilityEnvelope<BiPermissionRequestView>> reviewPermissionRequest(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiPermissionRequestReviewCommand command) {
        BiPermissionRequestReviewCommand merged = new BiPermissionRequestReviewCommand(
                id,
                command == null ? null : command.status(),
                command == null ? null : command.reviewComment());
        return envelope(() -> facade.reviewPermissionRequest(
                tenantIdOrDefault(tenantId),
                merged,
                actorOrDefault(actor)));
    }

    @GetMapping("/permissions/effective-access")
    public Mono<CompatibilityEnvelope<BiPermissionDecisionView>> effectiveAccess(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam Long workspaceId,
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam String actor,
            @RequestParam(value = "roles", required = false) List<String> roles,
            @RequestParam("action") String action) {
        return envelope(() -> facade.effectiveAccess(new BiAccessRequest(
                tenantIdOrDefault(tenantId),
                workspaceId,
                resourceType,
                resourceId,
                actor,
                roles == null ? Set.of() : Set.copyOf(roles),
                action)));
    }

    @GetMapping("/subscriptions")
    public Mono<CompatibilityEnvelope<List<BiSubscriptionView>>> listSubscriptions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listSubscriptions(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/subscriptions")
    public Mono<CompatibilityEnvelope<BiSubscriptionView>> upsertSubscription(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiSubscriptionCommand command) {
        return envelope(() -> facade.upsertSubscription(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/subscriptions/{id}")
    public Mono<CompatibilityEnvelope<Void>> deleteSubscription(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> {
            facade.deleteSubscription(tenantIdOrDefault(tenantId), id);
            return null;
        });
    }

    @PostMapping("/subscriptions/{id}/run")
    public Mono<CompatibilityEnvelope<BiDeliveryRunResult>> runSubscription(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.runSubscriptionDelivery(
                tenantIdOrDefault(tenantId),
                id,
                actorOrDefault(actor)));
    }

    @GetMapping("/alerts")
    public Mono<CompatibilityEnvelope<List<BiAlertRuleView>>> listAlerts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listAlertRules(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/alerts")
    public Mono<CompatibilityEnvelope<BiAlertRuleView>> upsertAlert(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiAlertRuleCommand command) {
        return envelope(() -> facade.upsertAlertRule(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @DeleteMapping("/alerts/{id}")
    public Mono<CompatibilityEnvelope<Void>> deleteAlert(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> {
            facade.deleteAlertRule(tenantIdOrDefault(tenantId), id);
            return null;
        });
    }

    @PostMapping("/alerts/{id}/run")
    public Mono<CompatibilityEnvelope<BiDeliveryRunResult>> runAlert(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.runAlertDelivery(
                tenantIdOrDefault(tenantId),
                id,
                actorOrDefault(actor)));
    }

    @GetMapping("/delivery-logs")
    public Mono<CompatibilityEnvelope<List<BiDeliveryLogView>>> listDeliveryLogs(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listDeliveryLogs(tenantIdOrDefault(tenantId), jobType, jobId, limit));
    }

    @GetMapping("/delivery-audit")
    public Mono<CompatibilityEnvelope<BiDeliveryAuditSummary>> auditDeliveryLogs(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.auditDeliveryLogs(
                tenantIdOrDefault(tenantId),
                jobType,
                status,
                channel,
                jobId,
                limit));
    }

    @PostMapping("/delivery-logs/retry")
    public Mono<CompatibilityEnvelope<BiDeliveryRetryResult>> retryDeliveryLogs(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.retryDeliveryLogs(tenantIdOrDefault(tenantId), actorOrDefault(actor), limit));
    }

    @GetMapping("/delivery-attachments")
    public Mono<CompatibilityEnvelope<List<BiDeliveryAttachmentView>>> listDeliveryAttachments(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Long deliveryLogId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listDeliveryAttachments(
                tenantIdOrDefault(tenantId),
                jobType,
                jobId,
                deliveryLogId,
                limit));
    }

    @GetMapping("/delivery-attachments/{id}/download")
    public Mono<ResponseEntity<byte[]>> downloadDeliveryAttachment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return Mono.fromCallable(() -> {
            try {
                BiDeliveryAttachmentDownload file = facade.downloadDeliveryAttachment(
                        tenantIdOrDefault(tenantId),
                        id,
                        actorOrDefault(actor));
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename(file.filename())
                                        .build()
                                        .toString())
                        .body(file.bytes());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/delivery-attachments/cleanup")
    public Mono<CompatibilityEnvelope<BiDeliveryAttachmentCleanupResult>> cleanupDeliveryAttachments(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "100") int limit) {
        return envelope(() -> facade.cleanupDeliveryAttachments(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/delivery-scheduler/run")
    public Mono<CompatibilityEnvelope<BiDeliverySchedulerResult>> runDeliveryScheduler(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.runDeliveryScheduler(tenantIdOrDefault(tenantId), actorOrDefault(actor)));
    }

    @PostMapping("/self-service/preview")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> previewSelfServiceExport(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody BiSelfServicePreviewCommand command) {
        return envelope(() -> facade.previewSelfServiceExport(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                roleOrDefault(role),
                command));
    }

    @PostMapping("/self-service")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> previewSelfServiceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody BiSelfServicePreviewCommand command) {
        return previewSelfServiceExport(tenantId, actor, role, command);
    }

    @PostMapping("/self-service/exports")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportJobView>> createSelfServiceExport(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody BiSelfServiceExportCommand command) {
        return envelope(() -> facade.createSelfServiceExport(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                roleOrDefault(role),
                command));
    }

    @GetMapping("/self-service/exports")
    public Mono<CompatibilityEnvelope<List<BiSelfServiceExportJobView>>> listSelfServiceExports(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listSelfServiceExports(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/self-service")
    public Mono<CompatibilityEnvelope<List<BiSelfServiceExportJobView>>> listSelfServiceAlias(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return listSelfServiceExports(tenantId, limit);
    }

    @PostMapping("/self-service/exports/{id}/review")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportJobView>> reviewSelfServiceExport(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody BiSelfServiceExportReviewCommand command) {
        return envelope(() -> facade.reviewSelfServiceExport(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                roleOrDefault(role),
                id,
                command));
    }

    @GetMapping("/self-service/exports/{id}")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportJobDetailView>> getSelfServiceExport(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getSelfServiceExportDetail(tenantIdOrDefault(tenantId), id));
    }

    @GetMapping("/self-service/exports/{id}/download")
    public Mono<ResponseEntity<byte[]>> downloadSelfServiceExport(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return Mono.fromCallable(() -> {
            try {
                BiSelfServiceExportDownload file = facade.downloadSelfServiceExport(
                        tenantIdOrDefault(tenantId),
                        actorOrDefault(actor),
                        id);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename(file.filename())
                                        .build()
                                        .toString())
                        .body(file.bytes());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/self-service/exports/{id}/cancel")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportJobView>> cancelSelfServiceExport(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.cancelSelfServiceExport(tenantIdOrDefault(tenantId), actorOrDefault(actor), id));
    }

    @PostMapping("/self-service/exports/cleanup")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportCleanupResult>> cleanupSelfServiceExports(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "100") int limit) {
        return envelope(() -> facade.cleanupSelfServiceExports(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/self-service/exports/retry")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportRetryResult>> retrySelfServiceExports(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.retrySelfServiceExports(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                roleOrDefault(role),
                limit));
    }

    @PostMapping("/self-service/exports/queue/run")
    public Mono<CompatibilityEnvelope<BiSelfServiceExportQueueResult>> runSelfServiceExportQueue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.runSelfServiceExportQueue(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                roleOrDefault(role),
                limit));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            } catch (SecurityException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<CompatibilityEnvelope<BiAiResponseView>> aiEnvelope(
            Long tenantId,
            String actor,
            String operation,
            BiAiRequestCommand command) {
        return envelope(() -> facade.aiAssistant(
                tenantIdOrDefault(tenantId),
                operation,
                command,
                actorOrDefault(actor)));
    }

    private static String originOrReferer(String origin, String referer) {
        return origin == null || origin.isBlank() ? referer : origin;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? "ANALYST" : role.trim();
    }

    private static String compatibilityKey(String requestedKey, String displayName) {
        if (requestedKey != null && !requestedKey.isBlank() && !requestedKey.startsWith("body-key")) {
            return requestedKey.trim();
        }
        if (displayName == null || displayName.isBlank()) {
            return requestedKey;
        }
        return displayName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static Map<String, Object> dashboardEmbedPayload(BiDashboardView dashboard) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceKey", dashboard.dashboardKey());
        payload.put("dashboardKey", dashboard.dashboardKey());
        payload.put("dashboard", dashboard);
        return payload;
    }

    private record ResourceKeyRequest(
            String datasetKey,
            String chartKey,
            String dashboardKey,
            String portalKey,
            String screenKey,
            String spreadsheetKey) {
    }

    private record DatasetFromDatasourceRequest(
            Long workspaceId,
            String datasetKey,
            String name,
            String datasetType,
            Long sourceRefId,
            String tableName,
            String tableExpression,
            List<String> tableNames,
            String tenantColumn,
            Map<String, Object> model,
            String status) {

        private BiDatasetCommand toCommand() {
            String expression = tableExpression;
            if ((expression == null || expression.isBlank()) && tableName != null && !tableName.isBlank()) {
                expression = tableName;
            }
            if ((expression == null || expression.isBlank()) && tableNames != null && !tableNames.isEmpty()) {
                expression = String.join(",", tableNames);
            }
            return new BiDatasetCommand(
                    workspaceId,
                    datasetKey,
                    name,
                    datasetType == null || datasetType.isBlank() ? "datasource" : datasetType,
                    sourceRefId,
                    expression,
                    tenantColumn,
                    model == null ? Map.of() : model,
                    List.of(),
                    List.of(),
                    status == null || status.isBlank() ? "draft" : status);
        }
    }

    private record SqlPreviewAliasRequest(
            String datasetKey,
            List<String> dimensions,
            List<String> metrics,
            List<Map<String, Object>> filters,
            List<Map<String, Object>> sorts,
            Integer limit,
            Integer offset,
            Map<String, String> sqlParameters) {

        private BiQueryCommand toCommand() {
            return new BiQueryCommand(
                    datasetKey,
                    null,
                    dimensions,
                    metrics,
                    filters,
                    sorts,
                    limit == null ? 20 : limit,
                    offset == null ? 0 : offset,
                    sqlParameters);
        }
    }

    private record DatasourceAliasRequest(
            Long id,
            String connectorType,
            String name,
            String url,
            String username,
            String password,
            String sourceKey,
            String description,
            Boolean enabled,
            String status,
            Map<String, Object> connectorConfig) {

        private BiDatasourceOnboardingCommand toCommand() {
            return new BiDatasourceOnboardingCommand(
                    connectorType,
                    name,
                    url,
                    username,
                    password,
                    sourceKey,
                    description,
                    enabled,
                    status,
                    connectorConfig);
        }
    }

    private record EmbedResourceRequest(
            String ticket,
            String resourceType,
            String resourceKey) {
    }

    private record WorkspaceRequest(
            String workspaceKey,
            String name,
            String description,
            String status) {
    }

    private record DatasetDraftRequest(
            Long workspaceId,
            String datasetKey,
            String name,
            String datasetType,
            Long sourceRefId,
            String tableExpression,
            String tenantColumn,
            Map<String, Object> model,
            List<DatasetFieldRequest> fields,
            List<MetricRequest> metrics,
            String status) {

        private BiDatasetCommand toCommand(String pathDatasetKey) {
            return new BiDatasetCommand(
                    workspaceId,
                    pathDatasetKey,
                    name,
                    datasetType,
                    sourceRefId,
                    tableExpression,
                    tenantColumn,
                    model,
                    fields == null ? List.of() : fields.stream().map(DatasetFieldRequest::toCommand).toList(),
                    metrics == null ? List.of() : metrics.stream().map(MetricRequest::toCommand).toList(),
                    status);
        }
    }

    private record DatasetFieldRequest(
            String fieldKey,
            String displayName,
            String columnExpression,
            String roleKey,
            String dataType,
            String defaultAggregation,
            Boolean visible,
            Integer sortOrder) {

        private BiDatasetFieldCommand toCommand() {
            return new BiDatasetFieldCommand(
                    fieldKey,
                    displayName,
                    columnExpression,
                    roleKey,
                    dataType,
                    defaultAggregation,
                    visible,
                    sortOrder);
        }
    }

    private record MetricRequest(
            String metricKey,
            String displayName,
            String expression,
            String aggregation,
            String dataType,
            String unit) {

        private BiMetricCommand toCommand() {
            return new BiMetricCommand(metricKey, displayName, expression, aggregation, dataType, unit);
        }
    }

    private record ChartDraftRequest(
            Long workspaceId,
            String chartKey,
            String name,
            String chartType,
            String datasetKey,
            Map<String, Object> query,
            Map<String, Object> style,
            Map<String, Object> interaction,
            String status) {

        private BiChartCommand toCommand(String pathChartKey) {
            return new BiChartCommand(
                    workspaceId,
                    pathChartKey,
                    name,
                    chartType,
                    datasetKey,
                    query,
                    style,
                    interaction,
                    status);
        }
    }

    private record DashboardDraftRequest(
            Long workspaceId,
            String dashboardKey,
            String name,
            String description,
            Map<String, Object> theme,
            Map<String, Object> filters,
            List<String> chartKeys,
            String status) {

        private BiDashboardCommand toCommand(String pathDashboardKey) {
            return new BiDashboardCommand(
                    workspaceId,
                    pathDashboardKey,
                    name,
                    description,
                    theme,
                    filters,
                    chartKeys,
                    status);
        }
    }

    private record DashboardCloneRequest(
            String dashboardKey,
            String name,
            String description) {

        private BiDashboardCloneCommand toCommand() {
            return new BiDashboardCloneCommand(dashboardKey, name, description);
        }
    }

    private record PortalDraftRequest(
            String portalKey,
            String title,
            String description,
            List<String> dashboardKeys,
            Map<String, Object> layout,
            Map<String, Object> settings,
            String status) {

        private BiPortalResourceCommand toCommand() {
            return new BiPortalResourceCommand(
                    portalKey,
                    title,
                    description,
                    dashboardKeys == null ? List.of() : dashboardKeys,
                    layout == null ? Map.of() : layout,
                    settings == null ? Map.of() : settings,
                    status);
        }
    }

    private record BigScreenDraftRequest(
            String screenKey,
            String title,
            String description,
            List<String> dashboardKeys,
            Map<String, Object> layout,
            Map<String, Object> settings,
            String status) {

        private BiBigScreenResourceCommand toCommand() {
            return new BiBigScreenResourceCommand(
                    screenKey,
                    title,
                    description,
                    dashboardKeys == null ? List.of() : dashboardKeys,
                    layout == null ? Map.of() : layout,
                    settings == null ? Map.of() : settings,
                    status);
        }
    }

    private record SpreadsheetDraftRequest(
            String spreadsheetKey,
            String name,
            String description,
            List<Map<String, Object>> sheets,
            Map<String, Object> dataBinding,
            Map<String, Object> style,
            String status) {

        private BiSpreadsheetResourceCommand toCommand() {
            return new BiSpreadsheetResourceCommand(
                    spreadsheetKey,
                    name,
                    description,
                    sheets == null ? List.of() : sheets,
                    dataBinding == null ? Map.of() : dataBinding,
                    style == null ? Map.of() : style,
                    status);
        }
    }

    private record PermissionGrantRequest(
            Long workspaceId,
            String resourceType,
            Long resourceId,
            String subjectType,
            String subjectId,
            String actionKey,
            String effect) {

        private BiPermissionGrantCommand toCommand() {
            return new BiPermissionGrantCommand(
                    workspaceId,
                    resourceType,
                    resourceId,
                    subjectType,
                    subjectId,
                    actionKey,
                    effect);
        }
    }

    private static <T> List<T> limited(List<T> values, Integer limit) {
        if (limit == null || limit < 0 || limit >= values.size()) {
            return values;
        }
        return values.subList(0, limit);
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }
}
