package org.chovy.canvas.bi.application;

import org.chovy.canvas.bi.api.BiCatalogFacade;
import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartDashboardReferenceView;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardCloneCommand;
import org.chovy.canvas.bi.api.BiDashboardExportPackageView;
import org.chovy.canvas.bi.api.BiDashboardImportCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetFilterView;
import org.chovy.canvas.bi.api.BiDashboardPresetInteractionView;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardPresetWidgetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardReadinessIssueView;
import org.chovy.canvas.bi.api.BiDashboardReadinessView;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateCommand;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldView;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiMetricView;
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
import org.chovy.canvas.bi.api.BiColumnPermissionCommand;
import org.chovy.canvas.bi.api.BiColumnPermissionView;
import org.chovy.canvas.bi.api.BiPermissionAuditEntryView;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiPermissionGrantView;
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
import org.chovy.canvas.bi.api.BiQueryFieldView;
import org.chovy.canvas.bi.api.BiQueryGateCommand;
import org.chovy.canvas.bi.api.BiQueryGateResult;
import org.chovy.canvas.bi.api.BiQueryGovernanceAuditEntryView;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyView;
import org.chovy.canvas.bi.api.BiQueryGovernanceSummaryView;
import org.chovy.canvas.bi.api.BiQueryHistoryDetailView;
import org.chovy.canvas.bi.api.BiQueryHistoryItemView;
import org.chovy.canvas.bi.api.BiQueryMetricView;
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
import org.chovy.canvas.bi.domain.BiAccessDecision;
import org.chovy.canvas.bi.domain.BiAccessRequest;
import org.chovy.canvas.bi.domain.BiAiAssistantCatalog;
import org.chovy.canvas.bi.domain.BiChart;
import org.chovy.canvas.bi.domain.BiChartLifecycleCatalog;
import org.chovy.canvas.bi.domain.BiChartRepository;
import org.chovy.canvas.bi.domain.BiDashboard;
import org.chovy.canvas.bi.domain.BiDashboardResourceOperationsCatalog;
import org.chovy.canvas.bi.domain.BiDashboardPresetCatalog;
import org.chovy.canvas.bi.domain.BiDashboardReadinessIssue;
import org.chovy.canvas.bi.domain.BiDashboardReadinessPolicy;
import org.chovy.canvas.bi.domain.BiDashboardReadinessReport;
import org.chovy.canvas.bi.domain.BiDashboardRepository;
import org.chovy.canvas.bi.domain.BiDataset;
import org.chovy.canvas.bi.domain.BiDatasetField;
import org.chovy.canvas.bi.domain.BiDatasetRepository;
import org.chovy.canvas.bi.domain.BiDatasourceOperationsCatalog;
import org.chovy.canvas.bi.domain.BiMetric;
import org.chovy.canvas.bi.domain.BiPermissionGrant;
import org.chovy.canvas.bi.domain.BiPermissionAdministrationCatalog;
import org.chovy.canvas.bi.domain.BiPermissionPolicy;
import org.chovy.canvas.bi.domain.BiPermissionRepository;
import org.chovy.canvas.bi.domain.BiPresentationResourceCatalog;
import org.chovy.canvas.bi.domain.BiQueryDatasetCatalog;
import org.chovy.canvas.bi.domain.BiQueryOperationsCatalog;
import org.chovy.canvas.bi.domain.BiQuickEngineCapacityCatalog;
import org.chovy.canvas.bi.domain.BiResourceFavoriteCatalog;
import org.chovy.canvas.bi.domain.BiResourceKey;
import org.chovy.canvas.bi.domain.BiResourceOperationsCatalog;
import org.chovy.canvas.bi.domain.BiResourceStatus;
import org.chovy.canvas.bi.domain.BiSelfServiceExportCatalog;
import org.chovy.canvas.bi.domain.BiSubscriptionDeliveryCatalog;
import org.chovy.canvas.bi.domain.BiWorkspace;
import org.chovy.canvas.bi.domain.BiWorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BiCatalogApplicationService implements BiCatalogFacade {

    private final BiWorkspaceRepository workspaceRepository;
    private final BiDatasetRepository datasetRepository;
    private final BiChartRepository chartRepository;
    private final BiDashboardRepository dashboardRepository;
    private final BiPermissionRepository permissionRepository;
    private final BiDashboardReadinessPolicy readinessPolicy;
    private final BiPermissionPolicy permissionPolicy;
    private final BiQueryDatasetCatalog queryDatasetCatalog;
    private final BiDashboardPresetCatalog dashboardPresetCatalog;
    private final BiDashboardResourceOperationsCatalog dashboardResourceOperationsCatalog;
    private final BiQuickEngineCapacityCatalog quickEngineCapacityCatalog;
    private final BiResourceFavoriteCatalog resourceFavoriteCatalog;
    private final BiAiAssistantCatalog aiAssistantCatalog;
    private final BiResourceOperationsCatalog resourceOperationsCatalog;
    private final BiPresentationResourceCatalog presentationResourceCatalog;
    private final BiChartLifecycleCatalog chartLifecycleCatalog;
    private final BiPermissionAdministrationCatalog permissionAdministrationCatalog;
    private final BiSubscriptionDeliveryCatalog subscriptionDeliveryCatalog;
    private final BiQueryOperationsCatalog queryOperationsCatalog;
    private final BiDatasourceOperationsCatalog datasourceOperationsCatalog;
    private final BiSelfServiceExportCatalog selfServiceExportCatalog;
    private final Clock clock;

    public BiCatalogApplicationService(BiWorkspaceRepository workspaceRepository,
                                       BiDatasetRepository datasetRepository,
                                       BiChartRepository chartRepository,
                                       BiDashboardRepository dashboardRepository,
                                       BiPermissionRepository permissionRepository) {
        this(workspaceRepository, datasetRepository, chartRepository, dashboardRepository, permissionRepository,
                Clock.systemDefaultZone());
    }

    BiCatalogApplicationService(BiWorkspaceRepository workspaceRepository,
                                BiDatasetRepository datasetRepository,
                                BiChartRepository chartRepository,
                                BiDashboardRepository dashboardRepository,
                                BiPermissionRepository permissionRepository,
                                Clock clock) {
        this.workspaceRepository = workspaceRepository;
        this.datasetRepository = datasetRepository;
        this.chartRepository = chartRepository;
        this.dashboardRepository = dashboardRepository;
        this.permissionRepository = permissionRepository;
        this.readinessPolicy = new BiDashboardReadinessPolicy();
        this.permissionPolicy = new BiPermissionPolicy();
        this.queryDatasetCatalog = new BiQueryDatasetCatalog();
        this.dashboardPresetCatalog = new BiDashboardPresetCatalog();
        this.dashboardResourceOperationsCatalog = new BiDashboardResourceOperationsCatalog();
        this.quickEngineCapacityCatalog = new BiQuickEngineCapacityCatalog();
        this.resourceFavoriteCatalog = new BiResourceFavoriteCatalog();
        this.aiAssistantCatalog = new BiAiAssistantCatalog();
        this.resourceOperationsCatalog = new BiResourceOperationsCatalog();
        this.presentationResourceCatalog = new BiPresentationResourceCatalog();
        this.chartLifecycleCatalog = new BiChartLifecycleCatalog();
        this.permissionAdministrationCatalog = new BiPermissionAdministrationCatalog();
        this.subscriptionDeliveryCatalog = new BiSubscriptionDeliveryCatalog();
        this.queryOperationsCatalog = new BiQueryOperationsCatalog();
        this.datasourceOperationsCatalog = new BiDatasourceOperationsCatalog();
        this.selfServiceExportCatalog = new BiSelfServiceExportCatalog();
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiWorkspaceView upsertWorkspace(Long tenantId, BiWorkspaceCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("workspace command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now(clock);
        BiWorkspace workspace = new BiWorkspace(
                null,
                scopedTenantId,
                BiResourceKey.of(command.workspaceKey(), "workspaceKey"),
                command.name(),
                command.description(),
                BiResourceStatus.from(command.status()),
                defaultActor(actor),
                now,
                now);
        return toWorkspaceView(workspaceRepository.saveWorkspace(workspace));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDatasetView upsertDataset(Long tenantId, BiDatasetCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("dataset command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        BiWorkspace workspace = requireWorkspace(scopedTenantId, command.workspaceId());
        BiResourceKey datasetKey = BiResourceKey.of(command.datasetKey(), "datasetKey");
        BiDataset existing = datasetRepository.findDatasetByKey(scopedTenantId, workspace.id(), datasetKey);
        LocalDateTime now = LocalDateTime.now(clock);
        BiDataset dataset = new BiDataset(
                existing == null ? null : existing.id(),
                scopedTenantId,
                workspace.id(),
                datasetKey,
                command.name(),
                command.datasetType(),
                command.sourceRefId(),
                command.tableExpression(),
                command.tenantColumn(),
                command.model(),
                command.fields().stream().map(this::toField).toList(),
                command.metrics().stream().map(this::toMetric).toList(),
                BiResourceStatus.from(command.status()),
                existing == null ? defaultActor(actor) : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now);
        return toDatasetView(datasetRepository.saveDataset(dataset));
    }

    @Override
    public List<BiDatasetView> listDatasetResources(Long tenantId) {
        return datasetRepository.listAvailableDatasets(safeTenantId(tenantId)).stream()
                .map(this::toDatasetView)
                .toList();
    }

    @Override
    public BiDatasetView getDatasetResource(Long tenantId, String datasetKey) {
        BiDataset dataset = datasetRepository.findAvailableDatasetByKeyWithTenantFallback(
                safeTenantId(tenantId),
                BiResourceKey.of(datasetKey, "datasetKey"));
        if (dataset == null) {
            throw new IllegalArgumentException("BI dataset not found");
        }
        return toDatasetView(dataset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDatasetView publishDatasetResource(Long tenantId, String datasetKey, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDataset existing = requireAvailableDataset(scopedTenantId, datasetKey);
        BiDataset published = new BiDataset(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.datasetKey(),
                existing.name(),
                existing.datasetType(),
                existing.sourceRefId(),
                existing.tableExpression(),
                existing.tenantColumn(),
                existing.model(),
                existing.fields(),
                existing.metrics(),
                BiResourceStatus.PUBLISHED,
                existing.createdBy(),
                existing.createdAt(),
                LocalDateTime.now(clock));
        return toDatasetView(datasetRepository.saveDataset(published));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDatasetView archiveDatasetResource(Long tenantId, String datasetKey, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDataset existing = datasetRepository.findAvailableDatasetByKeyWithTenantFallback(
                scopedTenantId,
                BiResourceKey.of(datasetKey, "datasetKey"));
        if (existing == null) {
            throw new IllegalArgumentException("BI dataset not found");
        }
        BiDataset archived = new BiDataset(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.datasetKey(),
                existing.name(),
                existing.datasetType(),
                existing.sourceRefId(),
                existing.tableExpression(),
                existing.tenantColumn(),
                existing.model(),
                existing.fields(),
                existing.metrics(),
                BiResourceStatus.ARCHIVED,
                existing.createdBy(),
                existing.createdAt(),
                LocalDateTime.now(clock));
        return toDatasetView(datasetRepository.saveDataset(archived));
    }

    @Override
    public List<BiResourceVersionView> listDatasetResourceVersions(Long tenantId, String datasetKey, int limit) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return List.of(new BiResourceVersionView(
                "DATASET",
                dataset.datasetKey().value(),
                1,
                dataset.status().name(),
                datasetSnapshot(dataset),
                dataset.createdBy(),
                dataset.updatedAt())).stream().limit(boundedLimit).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDatasetView restoreDatasetResourceVersion(Long tenantId, String datasetKey, Integer version, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDataset existing = requireAvailableDataset(scopedTenantId, datasetKey);
        BiDataset restored = new BiDataset(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.datasetKey(),
                existing.name(),
                existing.datasetType(),
                existing.sourceRefId(),
                existing.tableExpression(),
                existing.tenantColumn(),
                existing.model(),
                existing.fields(),
                existing.metrics(),
                BiResourceStatus.DRAFT,
                existing.createdBy(),
                existing.createdAt(),
                LocalDateTime.now(clock));
        return toDatasetView(datasetRepository.saveDataset(restored));
    }

    @Override
    public Map<String, Object> datasetAccelerationPolicy(Long tenantId, String datasetKey) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        return orderedMap(
                "tenantId", dataset.tenantId(),
                "datasetKey", dataset.datasetKey().value(),
                "enabled", Boolean.TRUE,
                "mode", "INCREMENTAL");
    }

    @Override
    public Map<String, Object> upsertDatasetAccelerationPolicy(
            Long tenantId,
            String datasetKey,
            Map<String, Object> command,
            String actor) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        Map<String, Object> policy = new LinkedHashMap<>(command == null ? Map.of() : command);
        policy.putIfAbsent("enabled", Boolean.TRUE);
        policy.putIfAbsent("mode", "INCREMENTAL");
        return orderedMap(
                "tenantId", dataset.tenantId(),
                "datasetKey", dataset.datasetKey().value(),
                "policy", policy,
                "updatedBy", defaultActor(actor));
    }

    @Override
    public Map<String, Object> refreshDatasetAcceleration(Long tenantId, String datasetKey, String actor) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        return orderedMap(
                "tenantId", dataset.tenantId(),
                "datasetKey", dataset.datasetKey().value(),
                "status", "QUEUED",
                "triggeredBy", defaultActor(actor));
    }

    @Override
    public List<Map<String, Object>> listDatasetAccelerationRuns(Long tenantId, String datasetKey, int limit) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        if (limit <= 0) {
            return List.of();
        }
        return List.of(orderedMap(
                "tenantId", dataset.tenantId(),
                "datasetKey", dataset.datasetKey().value(),
                "status", "SUCCEEDED",
                "runType", "FULL"));
    }

    @Override
    public Map<String, Object> datasetAccelerationCapacity(Long tenantId, String datasetKey, int limit) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        return orderedMap(
                "tenantId", dataset.tenantId(),
                "datasetKey", dataset.datasetKey().value(),
                "limit", Math.max(0, limit),
                "tables", List.of());
    }

    @Override
    public Map<String, Object> cleanupDatasetAcceleration(
            Long tenantId,
            String datasetKey,
            int retainTables,
            String actor) {
        BiDataset dataset = requireAvailableDataset(safeTenantId(tenantId), datasetKey);
        return orderedMap(
                "tenantId", dataset.tenantId(),
                "datasetKey", dataset.datasetKey().value(),
                "retainedTables", Math.max(0, retainTables),
                "cleanedTables", 0,
                "cleanedBy", defaultActor(actor));
    }

    @Override
    public List<BiQueryDatasetView> listQueryDatasets(Long tenantId) {
        return queryDatasetCatalog.datasets(safeTenantId(tenantId)).stream()
                .map(this::toQueryDatasetView)
                .toList();
    }

    @Override
    public BiQueryDatasetView getQueryDataset(Long tenantId, String datasetKey) {
        return toQueryDatasetView(queryDatasetCatalog.dataset(safeTenantId(tenantId), datasetKey));
    }

    @Override
    public List<BiDashboardPresetView> listDashboardPresets(Long tenantId) {
        return dashboardPresetCatalog.presets(safeTenantId(tenantId)).stream()
                .map(this::toDashboardPresetView)
                .toList();
    }

    @Override
    public BiDashboardPresetView getDashboardPreset(Long tenantId, String dashboardKey) {
        return toDashboardPresetView(dashboardPresetCatalog.preset(safeTenantId(tenantId), dashboardKey));
    }

    @Override
    public BiQuickEngineCapacitySummaryView quickEngineCapacity(Long tenantId, Integer limit) {
        return quickEngineCapacityCatalog.summary(safeTenantId(tenantId), limit);
    }

    @Override
    public BiQuickEngineQueueSnapshotView quickEngineQueue(Long tenantId, String poolKey, String status,
                                                           Integer limit) {
        return quickEngineCapacityCatalog.queueSnapshot(safeTenantId(tenantId), poolKey, status, limit);
    }

    @Override
    public BiResourceFavoriteView favoriteResource(Long tenantId, BiResourceFavoriteCommand command, String actor) {
        return resourceFavoriteCatalog.favorite(
                safeTenantId(tenantId),
                defaultActor(actor),
                command,
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceFavoriteView> listFavoriteResources(Long tenantId, String actor, String resourceType) {
        return resourceFavoriteCatalog.list(safeTenantId(tenantId), defaultActor(actor), resourceType);
    }

    @Override
    public void unfavoriteResource(Long tenantId, String actor, String resourceType, String resourceKey) {
        resourceFavoriteCatalog.remove(safeTenantId(tenantId), defaultActor(actor), resourceType, resourceKey);
    }

    @Override
    public BiAiResponseView aiAssistant(Long tenantId, String operation, BiAiRequestCommand command, String actor) {
        return aiAssistantCatalog.answer(safeTenantId(tenantId), operation, command, defaultActor(actor));
    }

    @Override
    public BiResourceCommentView addResourceComment(Long tenantId, BiResourceCommentCommand command, String actor) {
        return resourceOperationsCatalog.addComment(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceCommentView> listResourceComments(Long tenantId, String resourceType, String resourceKey) {
        return resourceOperationsCatalog.listComments(safeTenantId(tenantId), resourceType, resourceKey);
    }

    @Override
    public void deleteResourceComment(Long tenantId, String actor, Long commentId) {
        resourceOperationsCatalog.deleteComment(safeTenantId(tenantId), commentId, LocalDateTime.now(clock));
    }

    @Override
    public BiResourceLockView acquireResourceLock(Long tenantId, BiResourceLockCommand command, String actor) {
        return resourceOperationsCatalog.acquireLock(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiResourceLockView currentResourceLock(Long tenantId, String resourceType, String resourceKey) {
        return resourceOperationsCatalog.currentLock(safeTenantId(tenantId), resourceType, resourceKey);
    }

    @Override
    public void releaseResourceLock(Long tenantId, String actor, BiResourceLockCommand command) {
        resourceOperationsCatalog.releaseLock(safeTenantId(tenantId), command);
    }

    @Override
    public BiResourceLocationView updateResourceLocation(
            Long tenantId,
            BiResourceLocationCommand command,
            String actor) {
        return resourceOperationsCatalog.updateLocation(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiResourceLocationView moveResource(Long tenantId, BiResourceMoveCommand command, String actor) {
        return resourceOperationsCatalog.move(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceLocationView> listResourceLocations(Long tenantId, String resourceType) {
        return resourceOperationsCatalog.listLocations(safeTenantId(tenantId), resourceType);
    }

    @Override
    public BiResourceOwnershipView transferResource(Long tenantId, BiResourceTransferCommand command, String actor) {
        return resourceOperationsCatalog.transfer(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceOwnershipView> listResourceOwnerships(Long tenantId, String resourceType) {
        return resourceOperationsCatalog.listOwnerships(safeTenantId(tenantId), resourceType);
    }

    @Override
    public List<BiPublishApprovalView> listPublishApprovals(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status) {
        return resourceOperationsCatalog.listApprovals(safeTenantId(tenantId), resourceType, resourceKey, status);
    }

    @Override
    public BiPublishApprovalView requestPublishApproval(
            Long tenantId,
            BiPublishApprovalCommand command,
            String actor) {
        return resourceOperationsCatalog.requestApproval(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiPublishApprovalView reviewPublishApproval(
            Long tenantId,
            BiPublishApprovalReviewCommand command,
            String actor) {
        return resourceOperationsCatalog.reviewApproval(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiQuickEngineCapacityAlertPolicyView updateQuickEngineCapacityAlertPolicy(
            Long tenantId,
            BiQuickEngineCapacityAlertPolicyCommand command,
            String actor) {
        return quickEngineCapacityCatalog.updateAlertPolicy(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiQuickEngineTenantPoolPolicyView updateQuickEngineTenantPoolPolicy(
            Long tenantId,
            BiQuickEngineTenantPoolPolicyCommand command,
            String actor) {
        return quickEngineCapacityCatalog.updateTenantPoolPolicy(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiChartView upsertChart(Long tenantId, BiChartCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("chart command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        BiWorkspace workspace = requireWorkspace(scopedTenantId, command.workspaceId());
        BiDataset dataset = datasetRepository.findDatasetByKey(
                scopedTenantId,
                workspace.id(),
                BiResourceKey.of(command.datasetKey(), "datasetKey"));
        if (dataset == null || dataset.status() == BiResourceStatus.ARCHIVED) {
            throw new IllegalArgumentException("dataset is not available for BI chart");
        }
        BiResourceKey chartKey = BiResourceKey.of(command.chartKey(), "chartKey");
        BiChart existing = chartRepository.findChartByKey(scopedTenantId, workspace.id(), chartKey);
        LocalDateTime now = LocalDateTime.now(clock);
        BiChart chart = new BiChart(
                existing == null ? null : existing.id(),
                scopedTenantId,
                workspace.id(),
                chartKey,
                command.name(),
                command.chartType(),
                dataset.id(),
                dataset.datasetKey(),
                command.query(),
                command.style(),
                command.interaction(),
                BiResourceStatus.from(command.status()),
                existing == null ? defaultActor(actor) : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now);
        BiChart saved = chartRepository.saveChart(chart);
        chartLifecycleCatalog.appendVersion(saved, defaultActor(actor), now);
        return toChartView(saved);
    }

    @Override
    public List<BiChartView> listChartResources(Long tenantId) {
        return chartRepository.listAvailableCharts(safeTenantId(tenantId)).stream()
                .map(this::toChartView)
                .toList();
    }

    @Override
    public BiChartView getChartResource(Long tenantId, String chartKey) {
        BiChart chart = chartRepository.findAvailableChartByKey(
                safeTenantId(tenantId),
                BiResourceKey.of(chartKey, "chartKey"));
        if (chart == null) {
            throw new IllegalArgumentException("BI chart not found");
        }
        return toChartView(chart);
    }

    @Override
    public BiChartReferenceImpactView chartReferenceImpact(Long tenantId, String chartKey) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiChart chart = chartRepository.findAvailableChartByKey(
                scopedTenantId,
                BiResourceKey.of(chartKey, "chartKey"));
        if (chart == null) {
            throw new IllegalArgumentException("BI chart not found: " + chartKey);
        }
        String normalizedChartKey = chart.chartKey().value();
        List<BiChartDashboardReferenceView> dashboards = dashboardRepository.listAvailableDashboards(scopedTenantId)
                .stream()
                .filter(dashboard -> dashboard.workspaceId().equals(chart.workspaceId()))
                .filter(dashboard -> dashboard.chartKeys().contains(normalizedChartKey))
                .map(dashboard -> new BiChartDashboardReferenceView(
                        dashboard.dashboardKey().value(),
                        dashboard.name(),
                        normalizedChartKey,
                        chart.name(),
                        dashboard.status().name()))
                .sorted(Comparator.comparing(BiChartDashboardReferenceView::dashboardKey)
                        .thenComparing(BiChartDashboardReferenceView::widgetKey))
                .toList();
        return new BiChartReferenceImpactView(
                normalizedChartKey,
                chart.name(),
                chart.datasetKey().value(),
                dashboards,
                List.of(),
                List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiChartView publishChartResource(Long tenantId, String chartKey, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiChart existing = requireAvailableChart(scopedTenantId, chartKey);
        LocalDateTime now = LocalDateTime.now(clock);
        BiChart published = new BiChart(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.chartKey(),
                existing.name(),
                existing.chartType(),
                existing.datasetId(),
                existing.datasetKey(),
                existing.query(),
                existing.style(),
                existing.interaction(),
                BiResourceStatus.PUBLISHED,
                existing.createdBy(),
                existing.createdAt(),
                now);
        BiChart saved = chartRepository.saveChart(published);
        chartLifecycleCatalog.appendVersion(saved, defaultActor(actor), now);
        return toChartView(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveChartResource(Long tenantId, String chartKey, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiChart existing = chartRepository.findAvailableChartByKey(
                scopedTenantId,
                BiResourceKey.of(chartKey, "chartKey"));
        if (existing == null) {
            return;
        }
        chartRepository.saveChart(new BiChart(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.chartKey(),
                existing.name(),
                existing.chartType(),
                existing.datasetId(),
                existing.datasetKey(),
                existing.query(),
                existing.style(),
                existing.interaction(),
                BiResourceStatus.ARCHIVED,
                existing.createdBy(),
                existing.createdAt(),
                LocalDateTime.now(clock)));
    }

    @Override
    public List<BiResourceVersionView> listChartResourceVersions(Long tenantId, String chartKey) {
        return chartLifecycleCatalog.listVersions(safeTenantId(tenantId), chartKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiChartView restoreChartResourceVersion(Long tenantId, String chartKey, Integer version, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiChart existing = requireAvailableChart(scopedTenantId, chartKey);
        Map<String, Object> snapshot = chartLifecycleCatalog.snapshot(scopedTenantId, chartKey, version);
        LocalDateTime now = LocalDateTime.now(clock);
        BiChart restored = new BiChart(
                existing.id(),
                existing.tenantId(),
                longValue(snapshot.get("workspaceId"), existing.workspaceId()),
                existing.chartKey(),
                text(snapshot.get("name"), existing.name()),
                text(snapshot.get("chartType"), existing.chartType()),
                longValue(snapshot.get("datasetId"), existing.datasetId()),
                BiResourceKey.of(text(snapshot.get("datasetKey"), existing.datasetKey().value()), "datasetKey"),
                map(snapshot.get("query")),
                map(snapshot.get("style")),
                map(snapshot.get("interaction")),
                BiResourceStatus.DRAFT,
                existing.createdBy(),
                existing.createdAt(),
                now);
        BiChart saved = chartRepository.saveChart(restored);
        chartLifecycleCatalog.appendVersion(saved, defaultActor(actor), now);
        return toChartView(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDashboardView upsertDashboard(Long tenantId, BiDashboardCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("dashboard command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        BiWorkspace workspace = requireWorkspace(scopedTenantId, command.workspaceId());
        BiResourceKey dashboardKey = BiResourceKey.of(command.dashboardKey(), "dashboardKey");
        BiDashboard existing = dashboardRepository.findDashboardByKey(scopedTenantId, workspace.id(), dashboardKey);
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> chartKeys = command.chartKeys().stream()
                .map(key -> BiResourceKey.of(key, "chartKey").value())
                .toList();
        BiDashboard dashboard = new BiDashboard(
                existing == null ? null : existing.id(),
                scopedTenantId,
                workspace.id(),
                dashboardKey,
                command.name(),
                command.description(),
                command.theme(),
                command.filters(),
                chartKeys,
                BiResourceStatus.from(command.status()),
                existing == null ? 1 : existing.version(),
                existing == null ? defaultActor(actor) : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now);
        return toDashboardView(dashboardRepository.saveDashboard(dashboard));
    }

    @Override
    public List<BiDashboardView> listDashboardResources(Long tenantId) {
        return dashboardRepository.listAvailableDashboards(safeTenantId(tenantId)).stream()
                .map(this::toDashboardView)
                .toList();
    }

    @Override
    public BiDashboardView getDashboardResource(Long tenantId, String dashboardKey) {
        BiDashboard dashboard = dashboardRepository.findAvailableDashboardByKey(
                safeTenantId(tenantId),
                BiResourceKey.of(dashboardKey, "dashboardKey"));
        if (dashboard == null) {
            throw new IllegalArgumentException("BI dashboard not found");
        }
        return toDashboardView(dashboard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDashboardView cloneDashboardResource(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardCloneCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("dashboard clone command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        BiDashboard source = requireAvailableDashboard(scopedTenantId, dashboardKey);
        String targetKey = BiResourceKey.of(command.dashboardKey(), "dashboardKey").value();
        LocalDateTime now = LocalDateTime.now(clock);
        BiDashboard saved = dashboardRepository.saveDashboard(new BiDashboard(
                null,
                scopedTenantId,
                source.workspaceId(),
                BiResourceKey.of(targetKey, "dashboardKey"),
                text(command.name(), source.name()),
                text(command.description(), source.description()),
                source.theme(),
                source.filters(),
                source.chartKeys(),
                BiResourceStatus.DRAFT,
                source.version() + 1,
                defaultActor(actor),
                now,
                now));
        BiDashboardView view = toDashboardView(saved);
        dashboardResourceOperationsCatalog.appendVersion(
                scopedTenantId,
                targetKey,
                toDashboardView(source),
                source.createdBy(),
                source.createdAt());
        dashboardResourceOperationsCatalog.appendVersion(view, actor, now);
        return view;
    }

    @Override
    public BiDashboardExportPackageView exportDashboardResource(Long tenantId, String actor, String dashboardKey) {
        return dashboardResourceOperationsCatalog.exportPackage(
                getDashboardResource(tenantId, dashboardKey),
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiDashboardResourceOperationsCatalog.DashboardPackageFile exportDashboardResourceFile(
            Long tenantId,
            String actor,
            String dashboardKey) {
        return dashboardResourceOperationsCatalog.exportFile(exportDashboardResource(tenantId, actor, dashboardKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDashboardView importDashboardResource(Long tenantId, String actor, BiDashboardImportCommand command) {
        if (command == null || command.packageView() == null || command.packageView().dashboard() == null) {
            throw new IllegalArgumentException("dashboard import command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        BiDashboardView source = command.packageView().dashboard();
        String targetKey = BiResourceKey.of(command.dashboardKey(), "dashboardKey").value();
        BiDashboard existing = dashboardRepository.findDashboardByKey(
                scopedTenantId,
                source.workspaceId(),
                BiResourceKey.of(targetKey, "dashboardKey"));
        if (existing != null && !command.overwrite()) {
            throw new IllegalArgumentException("BI dashboard already exists");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        BiDashboard saved = dashboardRepository.saveDashboard(new BiDashboard(
                existing == null ? null : existing.id(),
                scopedTenantId,
                source.workspaceId(),
                BiResourceKey.of(targetKey, "dashboardKey"),
                text(command.name(), source.name()),
                source.description(),
                source.theme(),
                source.filters(),
                source.chartKeys(),
                BiResourceStatus.DRAFT,
                existing == null ? 1 : existing.version() + 1,
                existing == null ? defaultActor(actor) : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now));
        BiDashboardView view = toDashboardView(saved);
        dashboardResourceOperationsCatalog.appendVersion(view, actor, now);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDashboardView publishDashboardResource(Long tenantId, String dashboardKey, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDashboard existing = requireAvailableDashboard(scopedTenantId, dashboardKey);
        LocalDateTime now = LocalDateTime.now(clock);
        BiDashboard published = dashboardRepository.saveDashboard(new BiDashboard(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.dashboardKey(),
                existing.name(),
                existing.description(),
                existing.theme(),
                existing.filters(),
                existing.chartKeys(),
                BiResourceStatus.PUBLISHED,
                existing.version() + 1,
                existing.createdBy(),
                existing.createdAt(),
                now));
        BiDashboardView view = toDashboardView(published);
        dashboardResourceOperationsCatalog.appendVersion(view, actor, now);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDashboardView archiveDashboardResource(Long tenantId, String dashboardKey, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDashboard existing = requireAvailableDashboard(scopedTenantId, dashboardKey);
        LocalDateTime now = LocalDateTime.now(clock);
        BiDashboard archived = dashboardRepository.saveDashboard(new BiDashboard(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.dashboardKey(),
                existing.name(),
                existing.description(),
                existing.theme(),
                existing.filters(),
                existing.chartKeys(),
                BiResourceStatus.ARCHIVED,
                existing.version() + 1,
                existing.createdBy(),
                existing.createdAt(),
                now));
        BiDashboardView view = toDashboardView(archived);
        dashboardResourceOperationsCatalog.appendVersion(view, actor, now);
        return view;
    }

    @Override
    public List<BiResourceVersionView> listDashboardResourceVersions(Long tenantId, String dashboardKey, int limit) {
        return dashboardResourceOperationsCatalog.listVersions(safeTenantId(tenantId), dashboardKey, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiDashboardView restoreDashboardResourceVersion(
            Long tenantId,
            String dashboardKey,
            Integer version,
            String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDashboard existing = requireAvailableDashboard(scopedTenantId, dashboardKey);
        Map<String, Object> snapshot = dashboardResourceOperationsCatalog.snapshot(scopedTenantId, dashboardKey, version);
        LocalDateTime now = LocalDateTime.now(clock);
        BiDashboard restored = dashboardRepository.saveDashboard(new BiDashboard(
                existing.id(),
                existing.tenantId(),
                longValue(snapshot.get("workspaceId"), existing.workspaceId()),
                existing.dashboardKey(),
                text(snapshot.get("name"), existing.name()),
                text(snapshot.get("description"), existing.description()),
                map(snapshot.get("theme")),
                map(snapshot.get("filters")),
                stringList(snapshot.get("chartKeys")),
                BiResourceStatus.DRAFT,
                existing.version() + 1,
                existing.createdBy(),
                existing.createdAt(),
                now));
        BiDashboardView view = toDashboardView(restored);
        dashboardResourceOperationsCatalog.appendVersion(view, actor, now);
        return view;
    }

    @Override
    public BiDashboardRuntimeStateView getDashboardRuntimeState(Long tenantId, String actor, String dashboardKey) {
        return dashboardResourceOperationsCatalog.getRuntimeState(
                safeTenantId(tenantId),
                defaultActor(actor),
                dashboardKey,
                LocalDateTime.now(clock));
    }

    @Override
    public BiDashboardRuntimeStateView saveDashboardRuntimeState(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardRuntimeStateCommand command) {
        requireAvailableDashboard(safeTenantId(tenantId), dashboardKey);
        return dashboardResourceOperationsCatalog.saveRuntimeState(
                safeTenantId(tenantId),
                defaultActor(actor),
                dashboardKey,
                command,
                LocalDateTime.now(clock));
    }

    @Override
    public BiDashboardReadModelView dashboardReadModel(Long tenantId, Long workspaceId, String dashboardKey) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDashboard dashboard = dashboardRepository.findDashboardByKey(
                scopedTenantId,
                requiredId(workspaceId, "workspaceId"),
                BiResourceKey.of(dashboardKey, "dashboardKey"));
        if (dashboard == null) {
            throw new IllegalArgumentException("BI dashboard not found");
        }
        List<BiResourceKey> chartKeys = dashboard.chartKeys().stream()
                .map(key -> BiResourceKey.of(key, "chartKey"))
                .toList();
        List<BiChart> charts = chartRepository.listChartsByKeys(scopedTenantId, dashboard.workspaceId(), chartKeys);
        List<BiDataset> datasets = charts.stream()
                .map(chart -> datasetRepository.findDatasetById(scopedTenantId, chart.datasetId()))
                .filter(dataset -> dataset != null)
                .collect(LinkedHashMap<Long, BiDataset>::new,
                        (map, dataset) -> map.putIfAbsent(dataset.id(), dataset),
                        LinkedHashMap::putAll)
                .values()
                .stream()
                .toList();
        BiDashboardReadinessReport readiness = readinessPolicy.evaluate(dashboard, charts, datasets);
        return new BiDashboardReadModelView(
                toDashboardView(dashboard),
                charts.stream().map(this::toChartView).toList(),
                datasets.stream().map(this::toDatasetView).toList(),
                toReadinessView(readiness));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiPermissionGrantView grantPermission(Long tenantId, BiPermissionGrantCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("permission grant command is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        BiPermissionGrant saved = permissionRepository.saveGrant(new BiPermissionGrant(
                null,
                safeTenantId(tenantId),
                requiredId(command.workspaceId(), "workspaceId"),
                command.resourceType(),
                command.resourceId(),
                command.subjectType(),
                command.subjectId(),
                command.actionKey(),
                command.effect(),
                defaultActor(actor),
                now));
        return toPermissionGrantView(saved);
    }

    @Override
    public BiPermissionDecisionView effectiveAccess(BiAccessRequest request) {
        BiAccessDecision decision = permissionPolicy.evaluate(request, permissionRepository.listResourceGrants(
                request.tenantId(),
                request.workspaceId(),
                request.resourceType(),
                request.resourceId()));
        return new BiPermissionDecisionView(
                decision.allowed(),
                decision.effect(),
                decision.matchedSubjectType(),
                decision.matchedSubjectId(),
                decision.reason(),
                decision.signature());
    }

    @Override
    public List<BiResourcePermissionView> listResourcePermissions(
            Long tenantId,
            String resourceType,
            String resourceKey,
            Long resourceId) {
        return permissionAdministrationCatalog.listResourcePermissions(
                safeTenantId(tenantId),
                resourceType,
                resourceKey,
                resourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiResourcePermissionView upsertResourcePermission(
            Long tenantId,
            BiResourcePermissionCommand command,
            String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now(clock);
        BiResourcePermissionView view = permissionAdministrationCatalog.upsertResourcePermission(
                scopedTenantId,
                command,
                defaultActor(actor),
                now);
        if (command != null) {
            permissionRepository.deleteGrant(
                    scopedTenantId,
                    view.workspaceId(),
                    view.resourceType(),
                    view.resourceId(),
                    view.subjectType(),
                    view.subjectId(),
                    view.actionKey());
            permissionRepository.saveGrant(new BiPermissionGrant(
                    null,
                    scopedTenantId,
                    view.workspaceId(),
                    view.resourceType(),
                    view.resourceId(),
                    view.subjectType(),
                    view.subjectId(),
                    view.actionKey(),
                    view.effect(),
                    defaultActor(actor),
                    now));
        }
        return view;
    }

    public BiResourcePermissionView upsertResourcePermission(
            Long tenantId,
            String actor,
            BiResourcePermissionCommand command) {
        return upsertResourcePermission(tenantId, command, actor);
    }

    @Override
    public void deleteResourcePermission(Long tenantId, String actor, Long id) {
        BiResourcePermissionView removed = permissionAdministrationCatalog.deleteResourcePermission(
                safeTenantId(tenantId),
                defaultActor(actor),
                id,
                LocalDateTime.now(clock));
        if (removed != null) {
            permissionRepository.deleteGrant(
                    removed.tenantId(),
                    removed.workspaceId(),
                    removed.resourceType(),
                    removed.resourceId(),
                    removed.subjectType(),
                    removed.subjectId(),
                    removed.actionKey());
        }
    }

    @Override
    public List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey) {
        return permissionAdministrationCatalog.listRowPermissions(safeTenantId(tenantId), datasetKey);
    }

    @Override
    public BiRowPermissionView upsertRowPermission(Long tenantId, BiRowPermissionCommand command, String actor) {
        return permissionAdministrationCatalog.upsertRowPermission(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    public BiRowPermissionView upsertRowPermission(Long tenantId, String actor, BiRowPermissionCommand command) {
        return upsertRowPermission(tenantId, command, actor);
    }

    @Override
    public void deleteRowPermission(Long tenantId, String actor, Long id) {
        permissionAdministrationCatalog.deleteRowPermission(
                safeTenantId(tenantId),
                defaultActor(actor),
                id,
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey) {
        return permissionAdministrationCatalog.listColumnPermissions(safeTenantId(tenantId), datasetKey);
    }

    @Override
    public BiColumnPermissionView upsertColumnPermission(
            Long tenantId,
            BiColumnPermissionCommand command,
            String actor) {
        return permissionAdministrationCatalog.upsertColumnPermission(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    public BiColumnPermissionView upsertColumnPermission(
            Long tenantId,
            String actor,
            BiColumnPermissionCommand command) {
        return upsertColumnPermission(tenantId, command, actor);
    }

    @Override
    public void deleteColumnPermission(Long tenantId, String actor, Long id) {
        permissionAdministrationCatalog.deleteColumnPermission(
                safeTenantId(tenantId),
                defaultActor(actor),
                id,
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiPermissionAuditEntryView> permissionAudit(Long tenantId, int limit) {
        return permissionAdministrationCatalog.audit(safeTenantId(tenantId), limit);
    }

    @Override
    public List<BiPermissionRequestView> listPermissionRequests(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status) {
        return permissionAdministrationCatalog.listPermissionRequests(
                safeTenantId(tenantId),
                resourceType,
                resourceKey,
                status);
    }

    @Override
    public BiPermissionRequestView requestPermission(
            Long tenantId,
            BiPermissionRequestCommand command,
            String actor) {
        return permissionAdministrationCatalog.requestPermission(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    public BiPermissionRequestView requestPermission(Long tenantId, String actor, BiPermissionRequestCommand command) {
        return requestPermission(tenantId, command, actor);
    }

    @Override
    public BiPermissionRequestView reviewPermissionRequest(
            Long tenantId,
            BiPermissionRequestReviewCommand command,
            String actor) {
        BiPermissionRequestView view = permissionAdministrationCatalog.reviewPermissionRequest(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
        if (view.grantedPermissionId() != null) {
            Long resourceId = (long) (view.resourceKey().hashCode() & 0x7fffffff);
            permissionRepository.deleteGrant(
                    view.tenantId(),
                    view.workspaceId(),
                    view.resourceType(),
                    resourceId,
                    "USER",
                    view.requestedBy(),
                    view.requestedAction());
            permissionRepository.saveGrant(new BiPermissionGrant(
                    null,
                    view.tenantId(),
                    view.workspaceId(),
                    view.resourceType(),
                    resourceId,
                    "USER",
                    view.requestedBy(),
                    view.requestedAction(),
                    "ALLOW",
                    defaultActor(actor),
                    view.reviewedAt()));
        }
        return view;
    }

    public BiPermissionRequestView reviewPermissionRequest(
            Long tenantId,
            String actor,
            BiPermissionRequestReviewCommand command) {
        return reviewPermissionRequest(tenantId, command, actor);
    }

    @Override
    public List<BiSubscriptionView> listSubscriptions(Long tenantId, int limit) {
        return subscriptionDeliveryCatalog.listSubscriptions(safeTenantId(tenantId), limit);
    }

    @Override
    public BiSubscriptionView upsertSubscription(Long tenantId, BiSubscriptionCommand command, String actor) {
        return subscriptionDeliveryCatalog.upsertSubscription(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public void deleteSubscription(Long tenantId, Long id) {
        subscriptionDeliveryCatalog.deleteSubscription(safeTenantId(tenantId), id);
    }

    @Override
    public BiDeliveryRunResult runSubscriptionDelivery(Long tenantId, Long id, String actor) {
        return subscriptionDeliveryCatalog.runSubscription(
                safeTenantId(tenantId),
                id,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiAlertRuleView> listAlertRules(Long tenantId, int limit) {
        return subscriptionDeliveryCatalog.listAlerts(safeTenantId(tenantId), limit);
    }

    @Override
    public BiAlertRuleView upsertAlertRule(Long tenantId, BiAlertRuleCommand command, String actor) {
        return subscriptionDeliveryCatalog.upsertAlert(
                safeTenantId(tenantId),
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public void deleteAlertRule(Long tenantId, Long id) {
        subscriptionDeliveryCatalog.deleteAlert(safeTenantId(tenantId), id);
    }

    @Override
    public BiDeliveryRunResult runAlertDelivery(Long tenantId, Long id, String actor) {
        return subscriptionDeliveryCatalog.runAlert(
                safeTenantId(tenantId),
                id,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiDeliveryLogView> listDeliveryLogs(Long tenantId, String jobType, Long jobId, int limit) {
        return subscriptionDeliveryCatalog.listLogs(safeTenantId(tenantId), jobType, jobId, limit);
    }

    @Override
    public BiDeliveryAuditSummary auditDeliveryLogs(
            Long tenantId,
            String jobType,
            String status,
            String channel,
            Long jobId,
            int limit) {
        return subscriptionDeliveryCatalog.audit(safeTenantId(tenantId), jobType, status, channel, jobId, limit);
    }

    @Override
    public BiDeliveryRetryResult retryDeliveryLogs(Long tenantId, String actor, int limit) {
        return subscriptionDeliveryCatalog.retry(
                safeTenantId(tenantId),
                defaultActor(actor),
                limit,
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiDeliveryAttachmentView> listDeliveryAttachments(
            Long tenantId,
            String jobType,
            Long jobId,
            Long deliveryLogId,
            int limit) {
        return subscriptionDeliveryCatalog.listAttachments(safeTenantId(tenantId), jobType, jobId, deliveryLogId,
                limit);
    }

    @Override
    public BiDeliveryAttachmentDownload downloadDeliveryAttachment(Long tenantId, Long id, String actor) {
        return subscriptionDeliveryCatalog.download(
                safeTenantId(tenantId),
                id,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiDeliveryAttachmentCleanupResult cleanupDeliveryAttachments(Long tenantId, int limit) {
        return subscriptionDeliveryCatalog.cleanup(safeTenantId(tenantId), limit);
    }

    @Override
    public BiDeliverySchedulerResult runDeliveryScheduler(Long tenantId, String actor) {
        return subscriptionDeliveryCatalog.runScheduler(
                safeTenantId(tenantId),
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public Map<String, Object> previewSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            BiSelfServicePreviewCommand command) {
        return selfServiceExportCatalog.preview(safeTenantId(tenantId), defaultActor(actor), defaultRole(role), command);
    }

    @Override
    public BiSelfServiceExportJobView createSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            BiSelfServiceExportCommand command) {
        return selfServiceExportCatalog.create(
                safeTenantId(tenantId),
                defaultActor(actor),
                defaultRole(role),
                command,
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiSelfServiceExportJobView> listSelfServiceExports(Long tenantId, int limit) {
        return selfServiceExportCatalog.list(safeTenantId(tenantId), limit);
    }

    @Override
    public BiSelfServiceExportJobView reviewSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            Long id,
            BiSelfServiceExportReviewCommand command) {
        return selfServiceExportCatalog.review(
                safeTenantId(tenantId),
                id,
                defaultActor(actor),
                defaultRole(role),
                command,
                LocalDateTime.now(clock));
    }

    @Override
    public BiSelfServiceExportJobDetailView getSelfServiceExportDetail(Long tenantId, Long id) {
        return selfServiceExportCatalog.detail(safeTenantId(tenantId), id);
    }

    @Override
    public BiSelfServiceExportDownload downloadSelfServiceExport(Long tenantId, String actor, Long id) {
        return selfServiceExportCatalog.download(safeTenantId(tenantId), defaultActor(actor), id);
    }

    @Override
    public BiSelfServiceExportJobView cancelSelfServiceExport(Long tenantId, String actor, Long id) {
        return selfServiceExportCatalog.cancel(
                safeTenantId(tenantId),
                defaultActor(actor),
                id,
                LocalDateTime.now(clock));
    }

    @Override
    public BiSelfServiceExportCleanupResult cleanupSelfServiceExports(Long tenantId, int limit) {
        return selfServiceExportCatalog.cleanup(safeTenantId(tenantId), limit);
    }

    @Override
    public BiSelfServiceExportRetryResult retrySelfServiceExports(
            Long tenantId,
            String actor,
            String role,
            int limit) {
        return selfServiceExportCatalog.retry(
                safeTenantId(tenantId),
                defaultActor(actor),
                defaultRole(role),
                limit,
                LocalDateTime.now(clock));
    }

    @Override
    public BiSelfServiceExportQueueResult runSelfServiceExportQueue(
            Long tenantId,
            String actor,
            String role,
            int limit) {
        return selfServiceExportCatalog.runQueue(
                safeTenantId(tenantId),
                defaultActor(actor),
                defaultRole(role),
                limit,
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiPortalResourceView> listPortalResources(Long tenantId) {
        return presentationResourceCatalog.listPortals(safeTenantId(tenantId));
    }

    @Override
    public BiPortalResourceView getPortalResource(Long tenantId, String portalKey) {
        return presentationResourceCatalog.getPortal(safeTenantId(tenantId), portalKey);
    }

    @Override
    public BiPortalResourceView savePortalDraft(
            Long tenantId,
            String portalKey,
            BiPortalResourceCommand command,
            String actor) {
        return presentationResourceCatalog.savePortalDraft(
                safeTenantId(tenantId),
                portalKey,
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiPortalResourceView publishPortalResource(Long tenantId, String portalKey, String actor) {
        return presentationResourceCatalog.publishPortal(
                safeTenantId(tenantId),
                portalKey,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public void archivePortalResource(Long tenantId, String portalKey, String actor) {
        presentationResourceCatalog.archivePortal(
                safeTenantId(tenantId),
                portalKey,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceVersionView> listPortalResourceVersions(Long tenantId, String portalKey) {
        return presentationResourceCatalog.listPortalVersions(safeTenantId(tenantId), portalKey);
    }

    @Override
    public BiPortalResourceView restorePortalResourceVersion(
            Long tenantId,
            String portalKey,
            Integer version,
            String actor) {
        return presentationResourceCatalog.restorePortal(
                safeTenantId(tenantId),
                portalKey,
                version,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiBigScreenResourceView> listBigScreenResources(Long tenantId) {
        return presentationResourceCatalog.listBigScreens(safeTenantId(tenantId));
    }

    @Override
    public BiBigScreenResourceView getBigScreenResource(Long tenantId, String screenKey) {
        return presentationResourceCatalog.getBigScreen(safeTenantId(tenantId), screenKey);
    }

    @Override
    public BiBigScreenResourceView saveBigScreenDraft(
            Long tenantId,
            String screenKey,
            BiBigScreenResourceCommand command,
            String actor) {
        return presentationResourceCatalog.saveBigScreenDraft(
                safeTenantId(tenantId),
                screenKey,
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiBigScreenResourceView publishBigScreenResource(Long tenantId, String screenKey, String actor) {
        return presentationResourceCatalog.publishBigScreen(
                safeTenantId(tenantId),
                screenKey,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public void archiveBigScreenResource(Long tenantId, String screenKey, String actor) {
        presentationResourceCatalog.archiveBigScreen(
                safeTenantId(tenantId),
                screenKey,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceVersionView> listBigScreenResourceVersions(Long tenantId, String screenKey) {
        return presentationResourceCatalog.listBigScreenVersions(safeTenantId(tenantId), screenKey);
    }

    @Override
    public BiBigScreenResourceView restoreBigScreenResourceVersion(
            Long tenantId,
            String screenKey,
            Integer version,
            String actor) {
        return presentationResourceCatalog.restoreBigScreen(
                safeTenantId(tenantId),
                screenKey,
                version,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiSpreadsheetResourceView> listSpreadsheetResources(Long tenantId) {
        return presentationResourceCatalog.listSpreadsheets(safeTenantId(tenantId));
    }

    @Override
    public BiSpreadsheetResourceView getSpreadsheetResource(Long tenantId, String spreadsheetKey) {
        return presentationResourceCatalog.getSpreadsheet(safeTenantId(tenantId), spreadsheetKey);
    }

    @Override
    public BiSpreadsheetResourceView saveSpreadsheetDraft(
            Long tenantId,
            String spreadsheetKey,
            BiSpreadsheetResourceCommand command,
            String actor) {
        return presentationResourceCatalog.saveSpreadsheetDraft(
                safeTenantId(tenantId),
                spreadsheetKey,
                command,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiSpreadsheetResourceView publishSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor) {
        return presentationResourceCatalog.publishSpreadsheet(
                safeTenantId(tenantId),
                spreadsheetKey,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public void archiveSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor) {
        presentationResourceCatalog.archiveSpreadsheet(
                safeTenantId(tenantId),
                spreadsheetKey,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiResourceVersionView> listSpreadsheetResourceVersions(Long tenantId, String spreadsheetKey) {
        return presentationResourceCatalog.listSpreadsheetVersions(safeTenantId(tenantId), spreadsheetKey);
    }

    @Override
    public BiSpreadsheetResourceView restoreSpreadsheetResourceVersion(
            Long tenantId,
            String spreadsheetKey,
            Integer version,
            String actor) {
        return presentationResourceCatalog.restoreSpreadsheet(
                safeTenantId(tenantId),
                spreadsheetKey,
                version,
                defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiQueryCompileResult compileQuery(Long tenantId, BiQueryCommand command, String actor) {
        return queryOperationsCatalog.compile(safeTenantId(tenantId), command);
    }

    @Override
    public BiQueryResultView executeQuery(Long tenantId, BiQueryCommand command, String actor) {
        return queryOperationsCatalog.execute(safeTenantId(tenantId), command, defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiQueryExplainResult explainQuery(Long tenantId, BiQueryCommand command, String actor) {
        return queryOperationsCatalog.explain(safeTenantId(tenantId), command);
    }

    @Override
    public BiQueryCancelResult cancelQuery(Long tenantId, String sqlHash, String actor) {
        return queryOperationsCatalog.cancel(safeTenantId(tenantId), sqlHash);
    }

    @Override
    public BiQueryGateResult executeGatedQuery(Long tenantId, BiQueryGateCommand command, String actor) {
        return queryOperationsCatalog.executeGated(safeTenantId(tenantId), command, defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiQueryGateResult executeContractGatedQuery(
            Long tenantId,
            BiQueryContractGateCommand command,
            String actor) {
        return queryOperationsCatalog.executeContractGated(safeTenantId(tenantId), command, defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiQueryHistoryItemView> listQueryHistory(Long tenantId, int limit) {
        return queryOperationsCatalog.listHistory(safeTenantId(tenantId), limit);
    }

    @Override
    public BiQueryHistoryDetailView queryHistoryDetail(Long tenantId, Long historyId) {
        return queryOperationsCatalog.historyDetail(safeTenantId(tenantId), historyId);
    }

    @Override
    public BiQueryGovernanceSummaryView queryGovernanceSummary(Long tenantId, int limit) {
        return queryOperationsCatalog.governanceSummary(safeTenantId(tenantId), limit);
    }

    @Override
    public BiQueryGovernancePolicyView queryGovernancePolicy(Long tenantId) {
        return queryOperationsCatalog.governancePolicy(safeTenantId(tenantId));
    }

    @Override
    public BiQueryGovernancePolicyView updateQueryGovernancePolicy(
            Long tenantId,
            BiQueryGovernancePolicyCommand command,
            String actor) {
        return queryOperationsCatalog.updateGovernancePolicy(safeTenantId(tenantId), command, defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public List<BiQueryGovernanceAuditEntryView> queryGovernanceAudit(Long tenantId, int limit) {
        return queryOperationsCatalog.governanceAudit(safeTenantId(tenantId), limit);
    }

    @Override
    public BiQueryCachePolicyView queryCachePolicy(Long tenantId) {
        return queryOperationsCatalog.cachePolicy(safeTenantId(tenantId));
    }

    @Override
    public BiQueryCachePolicyView updateQueryCachePolicy(
            Long tenantId,
            BiQueryCachePolicyCommand command,
            String actor) {
        return queryOperationsCatalog.updateCachePolicy(safeTenantId(tenantId), command);
    }

    @Override
    public BiQueryCacheInvalidationResult invalidateQueryCache(
            Long tenantId,
            BiQueryCacheInvalidationCommand command) {
        return queryOperationsCatalog.invalidate(command);
    }

    @Override
    public BiQueryCacheStatsView queryCacheStats(Long tenantId) {
        return queryOperationsCatalog.cacheStats(safeTenantId(tenantId));
    }

    @Override
    public List<BiDatasourceHealthView> datasourceHealth() {
        return queryOperationsCatalog.datasourceHealth();
    }

    @Override
    public List<BiDatasourceHealthSnapshotView> datasourceHealthHistory(int limit) {
        return queryOperationsCatalog.datasourceHealthHistory(limit);
    }

    @Override
    public BiDatasourceHealthSloView datasourceHealthSlo(int limit) {
        return queryOperationsCatalog.datasourceHealthSlo(limit);
    }

    @Override
    public List<BiDatasourceConnectorView> datasourceConnectors() {
        return datasourceOperationsCatalog.connectors();
    }

    @Override
    public List<BiDatasourceOnboardingView> listDatasources(Long tenantId) {
        return datasourceOperationsCatalog.list(safeTenantId(tenantId));
    }

    @Override
    public BiDatasourceOnboardingView createDatasource(
            Long tenantId,
            BiDatasourceOnboardingCommand command,
            String actor) {
        return datasourceOperationsCatalog.create(safeTenantId(tenantId), command, defaultActor(actor));
    }

    @Override
    public BiDatasourceOnboardingView updateDatasource(
            Long tenantId,
            Long id,
            BiDatasourceOnboardingCommand command,
            String actor) {
        return datasourceOperationsCatalog.update(safeTenantId(tenantId), id, command, defaultActor(actor));
    }

    @Override
    public BiDatasourceOnboardingView uploadDatasourceFile(
            Long tenantId,
            String actor,
            String filename,
            String name,
            String description,
            String sheetName,
            String delimiter,
            boolean headerRow,
            String encoding) {
        return datasourceOperationsCatalog.uploadFile(
                safeTenantId(tenantId),
                defaultActor(actor),
                filename,
                name,
                description,
                sheetName,
                delimiter,
                headerRow,
                encoding);
    }

    @Override
    public BiDatasourceFileMaterializationResult materializeDatasourceFile(
            Long tenantId,
            String actor,
            String filename,
            String name,
            String description,
            String sheetName,
            String delimiter,
            boolean headerRow,
            String encoding,
            String datasetKey,
            String datasetName,
            String tenantColumn,
            int schemaLimit,
            long maxRows) {
        return datasourceOperationsCatalog.materializeFile(
                safeTenantId(tenantId),
                defaultActor(actor),
                filename,
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
                maxRows,
                LocalDateTime.now(clock));
    }

    @Override
    public BiDatasourceConnectionTestResult testDatasourceConnection(Long tenantId, Long id) {
        return datasourceOperationsCatalog.testConnection(safeTenantId(tenantId), id);
    }

    @Override
    public BiDatasourceCredentialRotationView rotateDatasourceCredential(
            Long tenantId,
            Long id,
            BiDatasourceCredentialRotationCommand command,
            String actor) {
        return datasourceOperationsCatalog.rotateCredential(safeTenantId(tenantId), id, defaultActor(actor));
    }

    @Override
    public BiDatasourceSchemaPreviewView previewDatasourceSchema(Long tenantId, Long id, int limit) {
        return datasourceOperationsCatalog.previewSchema(safeTenantId(tenantId), id, limit);
    }

    @Override
    public BiDatasourceApiPreviewView previewDatasourceApi(
            Long tenantId,
            Long id,
            BiDatasourceApiPreviewCommand command) {
        return datasourceOperationsCatalog.previewApi(safeTenantId(tenantId), id, command);
    }

    @Override
    public BiDatasourceSchemaSnapshotView syncDatasourceSchema(
            Long tenantId,
            Long id,
            int limit,
            BiDatasourceApiPreviewCommand command,
            String actor) {
        return datasourceOperationsCatalog.syncSchema(safeTenantId(tenantId), id, limit, defaultActor(actor),
                LocalDateTime.now(clock));
    }

    @Override
    public BiDatasourceSchemaSnapshotView latestDatasourceSchemaSnapshot(Long tenantId, Long id) {
        return datasourceOperationsCatalog.latestSnapshot(safeTenantId(tenantId), id);
    }

    @Override
    public List<BiDatasourceSchemaSnapshotView> listDatasourceSchemaSnapshots(Long tenantId, Long id, int limit) {
        return datasourceOperationsCatalog.listSnapshots(safeTenantId(tenantId), id, limit);
    }

    @Override
    public BiEmbedTicketView createEmbedTicket(Long tenantId, BiEmbedTicketCommand command, String actor) {
        return queryOperationsCatalog.createEmbedTicket(safeTenantId(tenantId), command, defaultActor(actor),
                Instant.now(clock));
    }

    @Override
    public BiEmbedTicketPayloadView verifyEmbedTicket(BiEmbedTicketVerifyCommand command, String origin) {
        return queryOperationsCatalog.verifyEmbedTicket(command, origin, Instant.now(clock));
    }

    @Override
    public BiQueryResultView executeEmbedQuery(BiEmbedQueryCommand command, String origin) {
        return queryOperationsCatalog.executeEmbedQuery(command, origin, LocalDateTime.now(clock));
    }

    @Override
    public BiEmbedTicketCleanupResult cleanupEmbedTickets(Long tenantId, int limit) {
        return queryOperationsCatalog.cleanupEmbedTickets(safeTenantId(tenantId), limit, Instant.now(clock));
    }

    @Override
    public BiDashboardView embedDashboardResource(Long tenantId, String resourceKey, String ticket, String origin) {
        return getDashboardResource(safeTenantId(tenantId), resourceKey);
    }

    @Override
    public BiDashboardRuntimeStateView embedDashboardRuntimeState(
            Long tenantId,
            String actor,
            String resourceKey,
            String ticket,
            String origin) {
        return getDashboardRuntimeState(safeTenantId(tenantId), actor, resourceKey);
    }

    @Override
    public BiPortalResourceView embedPortalResource(Long tenantId, String resourceKey, String ticket, String origin) {
        return getPortalResource(safeTenantId(tenantId), resourceKey);
    }

    private BiWorkspace requireWorkspace(Long tenantId, Long workspaceId) {
        BiWorkspace workspace = workspaceRepository.findWorkspace(tenantId, requiredId(workspaceId, "workspaceId"));
        if (workspace == null) {
            throw new IllegalArgumentException("BI workspace not found");
        }
        return workspace;
    }

    private BiChart requireAvailableChart(Long tenantId, String chartKey) {
        BiChart chart = chartRepository.findAvailableChartByKey(
                tenantId,
                BiResourceKey.of(chartKey, "chartKey"));
        if (chart == null) {
            throw new IllegalArgumentException("BI chart not found");
        }
        return chart;
    }

    private BiDashboard requireAvailableDashboard(Long tenantId, String dashboardKey) {
        BiDashboard dashboard = dashboardRepository.findAvailableDashboardByKey(
                tenantId,
                BiResourceKey.of(dashboardKey, "dashboardKey"));
        if (dashboard == null) {
            throw new IllegalArgumentException("BI dashboard not found");
        }
        return dashboard;
    }

    private BiDataset requireAvailableDataset(Long tenantId, String datasetKey) {
        BiDataset dataset = datasetRepository.findAvailableDatasetByKeyWithTenantFallback(
                tenantId,
                BiResourceKey.of(datasetKey, "datasetKey"));
        if (dataset == null) {
            throw new IllegalArgumentException("BI dataset not found");
        }
        return dataset;
    }

    private Map<String, Object> datasetSnapshot(BiDataset dataset) {
        return orderedMap(
                "workspaceId", dataset.workspaceId(),
                "datasetKey", dataset.datasetKey().value(),
                "name", dataset.name(),
                "datasetType", dataset.datasetType(),
                "sourceRefId", dataset.sourceRefId(),
                "tableExpression", dataset.tableExpression(),
                "tenantColumn", dataset.tenantColumn(),
                "status", dataset.status().name());
    }

    private Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return map;
    }

    private BiDatasetField toField(BiDatasetFieldCommand command) {
        return new BiDatasetField(
                BiResourceKey.of(command.fieldKey(), "fieldKey"),
                command.displayName(),
                command.columnExpression(),
                command.roleKey(),
                command.dataType(),
                command.defaultAggregation(),
                command.visible() == null || command.visible(),
                command.sortOrder() == null ? 0 : command.sortOrder());
    }

    private BiMetric toMetric(BiMetricCommand command) {
        return new BiMetric(
                BiResourceKey.of(command.metricKey(), "metricKey"),
                command.displayName(),
                command.expression(),
                command.aggregation(),
                command.dataType(),
                command.unit());
    }

    private BiWorkspaceView toWorkspaceView(BiWorkspace workspace) {
        return new BiWorkspaceView(
                workspace.id(),
                workspace.tenantId(),
                workspace.workspaceKey().value(),
                workspace.name(),
                workspace.description(),
                workspace.status().name(),
                workspace.createdBy(),
                workspace.createdAt(),
                workspace.updatedAt());
    }

    private BiDatasetView toDatasetView(BiDataset dataset) {
        return new BiDatasetView(
                dataset.id(),
                dataset.tenantId(),
                dataset.workspaceId(),
                dataset.datasetKey().value(),
                dataset.name(),
                dataset.datasetType(),
                dataset.sourceRefId(),
                dataset.tableExpression(),
                dataset.tenantColumn(),
                dataset.model(),
                dataset.fields().stream().map(this::toFieldView).toList(),
                dataset.metrics().stream().map(this::toMetricView).toList(),
                dataset.status().name(),
                dataset.createdBy(),
                dataset.createdAt(),
                dataset.updatedAt());
    }

    private BiDatasetFieldView toFieldView(BiDatasetField field) {
        return new BiDatasetFieldView(
                field.fieldKey().value(),
                field.displayName(),
                field.columnExpression(),
                field.roleKey(),
                field.dataType(),
                field.defaultAggregation(),
                field.visible(),
                field.sortOrder());
    }

    private BiMetricView toMetricView(BiMetric metric) {
        return new BiMetricView(
                metric.metricKey().value(),
                metric.displayName(),
                metric.expression(),
                metric.aggregation(),
                metric.dataType(),
                metric.unit());
    }

    private BiQueryDatasetView toQueryDatasetView(BiQueryDatasetCatalog.BiQueryDataset dataset) {
        return new BiQueryDatasetView(
                dataset.datasetKey(),
                dataset.fields().stream()
                        .map(field -> new BiQueryFieldView(field.fieldKey(), field.role(), field.dataType()))
                        .toList(),
                dataset.metrics().stream()
                        .map(metric -> new BiQueryMetricView(metric.metricKey(), metric.dataType()))
                        .toList());
    }

    private BiDashboardPresetView toDashboardPresetView(BiDashboardPresetCatalog.BiDashboardPreset preset) {
        return new BiDashboardPresetView(
                preset.dashboardKey(),
                preset.title(),
                preset.description(),
                preset.datasetKey(),
                preset.widgets().stream().map(this::toDashboardPresetWidgetView).toList(),
                preset.filters().stream().map(this::toDashboardPresetFilterView).toList(),
                preset.interactions().stream().map(this::toDashboardPresetInteractionView).toList(),
                preset.subscriptionChannels(),
                preset.embedScopes());
    }

    private BiDashboardPresetWidgetView toDashboardPresetWidgetView(
            BiDashboardPresetCatalog.BiDashboardWidget widget) {
        return new BiDashboardPresetWidgetView(
                widget.widgetKey(),
                widget.title(),
                widget.chartType(),
                widget.dimensions(),
                widget.metrics(),
                widget.gridX(),
                widget.gridY(),
                widget.gridW(),
                widget.gridH(),
                widget.stylePreset());
    }

    private BiDashboardPresetFilterView toDashboardPresetFilterView(
            BiDashboardPresetCatalog.BiDashboardFilter filter) {
        BiDashboardPresetCatalog.BiDashboardFilterCascade cascade = filter.cascade();
        return new BiDashboardPresetFilterView(
                filter.filterKey(),
                filter.fieldKey(),
                filter.label(),
                filter.controlType(),
                filter.required(),
                filter.defaultValue(),
                filter.targetWidgetKeys(),
                cascade == null ? List.of() : cascade.parentFilterKeys(),
                cascade == null ? Map.of() : cascade.parentFieldMapping(),
                cascade == null ? "SAME_SOURCE" : cascade.mode(),
                filter.optionDatasetKey(),
                filter.optionFieldKey(),
                filter.hidden());
    }

    private BiDashboardPresetInteractionView toDashboardPresetInteractionView(
            BiDashboardPresetCatalog.BiDashboardInteraction interaction) {
        return new BiDashboardPresetInteractionView(
                interaction.interactionKey(),
                interaction.sourceWidgetKey(),
                interaction.targetWidgetKey(),
                interaction.interactionType(),
                interaction.fieldKey(),
                interaction.target());
    }

    private BiChartView toChartView(BiChart chart) {
        return new BiChartView(
                chart.id(),
                chart.tenantId(),
                chart.workspaceId(),
                chart.chartKey().value(),
                chart.name(),
                chart.chartType(),
                chart.datasetId(),
                chart.datasetKey().value(),
                chart.query(),
                chart.style(),
                chart.interaction(),
                chart.status().name(),
                chart.createdBy(),
                chart.createdAt(),
                chart.updatedAt());
    }

    private BiDashboardView toDashboardView(BiDashboard dashboard) {
        return new BiDashboardView(
                dashboard.id(),
                dashboard.tenantId(),
                dashboard.workspaceId(),
                dashboard.dashboardKey().value(),
                dashboard.name(),
                dashboard.description(),
                dashboard.theme(),
                dashboard.filters(),
                dashboard.chartKeys(),
                dashboard.status().name(),
                dashboard.version(),
                dashboard.createdBy(),
                dashboard.createdAt(),
                dashboard.updatedAt());
    }

    private BiDashboardReadinessView toReadinessView(BiDashboardReadinessReport report) {
        return new BiDashboardReadinessView(
                report.status(),
                report.productionReady(),
                report.publishedChartCount(),
                report.draftDatasetCount(),
                report.blockers().stream().map(this::toReadinessIssueView).toList(),
                report.warnings().stream().map(this::toReadinessIssueView).toList());
    }

    private BiDashboardReadinessIssueView toReadinessIssueView(BiDashboardReadinessIssue issue) {
        return new BiDashboardReadinessIssueView(
                issue.severity(),
                issue.code(),
                issue.itemType(),
                issue.itemKey(),
                issue.message());
    }

    private BiPermissionGrantView toPermissionGrantView(BiPermissionGrant grant) {
        return new BiPermissionGrantView(
                grant.id(),
                grant.tenantId(),
                grant.workspaceId(),
                grant.resourceType(),
                grant.resourceId(),
                grant.subjectType(),
                grant.subjectId(),
                grant.actionKey(),
                grant.effect(),
                grant.createdBy(),
                grant.createdAt());
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static Long longValue(Object value, Long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String defaultRole(String role) {
        return role == null || role.isBlank() ? "ANALYST" : role.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
