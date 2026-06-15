package org.chovy.canvas.bi.api;

import org.chovy.canvas.bi.domain.BiAccessRequest;

import java.util.List;

public interface BiCatalogFacade {

    BiWorkspaceView upsertWorkspace(Long tenantId, BiWorkspaceCommand command, String actor);

    BiDatasetView upsertDataset(Long tenantId, BiDatasetCommand command, String actor);

    List<BiDatasetView> listDatasetResources(Long tenantId);

    BiDatasetView getDatasetResource(Long tenantId, String datasetKey);

    BiDatasetView publishDatasetResource(Long tenantId, String datasetKey, String actor);

    BiDatasetView archiveDatasetResource(Long tenantId, String datasetKey, String actor);

    List<BiResourceVersionView> listDatasetResourceVersions(Long tenantId, String datasetKey, int limit);

    BiDatasetView restoreDatasetResourceVersion(Long tenantId, String datasetKey, Integer version, String actor);

    java.util.Map<String, Object> datasetAccelerationPolicy(Long tenantId, String datasetKey);

    java.util.Map<String, Object> upsertDatasetAccelerationPolicy(
            Long tenantId,
            String datasetKey,
            java.util.Map<String, Object> command,
            String actor);

    java.util.Map<String, Object> refreshDatasetAcceleration(Long tenantId, String datasetKey, String actor);

    List<java.util.Map<String, Object>> listDatasetAccelerationRuns(Long tenantId, String datasetKey, int limit);

    java.util.Map<String, Object> datasetAccelerationCapacity(Long tenantId, String datasetKey, int limit);

    java.util.Map<String, Object> cleanupDatasetAcceleration(
            Long tenantId,
            String datasetKey,
            int retainTables,
            String actor);

    List<BiQueryDatasetView> listQueryDatasets(Long tenantId);

    BiQueryDatasetView getQueryDataset(Long tenantId, String datasetKey);

    List<BiDashboardPresetView> listDashboardPresets(Long tenantId);

    BiDashboardPresetView getDashboardPreset(Long tenantId, String dashboardKey);

    BiQuickEngineCapacitySummaryView quickEngineCapacity(Long tenantId, Integer limit);

    BiQuickEngineQueueSnapshotView quickEngineQueue(Long tenantId, String poolKey, String status, Integer limit);

    BiResourceFavoriteView favoriteResource(Long tenantId, BiResourceFavoriteCommand command, String actor);

    List<BiResourceFavoriteView> listFavoriteResources(Long tenantId, String actor, String resourceType);

    void unfavoriteResource(Long tenantId, String actor, String resourceType, String resourceKey);

    BiAiResponseView aiAssistant(Long tenantId, String operation, BiAiRequestCommand command, String actor);

    BiResourceCommentView addResourceComment(Long tenantId, BiResourceCommentCommand command, String actor);

    List<BiResourceCommentView> listResourceComments(Long tenantId, String resourceType, String resourceKey);

    void deleteResourceComment(Long tenantId, String actor, Long commentId);

    BiResourceLockView acquireResourceLock(Long tenantId, BiResourceLockCommand command, String actor);

    BiResourceLockView currentResourceLock(Long tenantId, String resourceType, String resourceKey);

    void releaseResourceLock(Long tenantId, String actor, BiResourceLockCommand command);

    BiResourceLocationView updateResourceLocation(Long tenantId, BiResourceLocationCommand command, String actor);

    BiResourceLocationView moveResource(Long tenantId, BiResourceMoveCommand command, String actor);

    List<BiResourceLocationView> listResourceLocations(Long tenantId, String resourceType);

    BiResourceOwnershipView transferResource(Long tenantId, BiResourceTransferCommand command, String actor);

    List<BiResourceOwnershipView> listResourceOwnerships(Long tenantId, String resourceType);

    List<BiPublishApprovalView> listPublishApprovals(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status);

    BiPublishApprovalView requestPublishApproval(Long tenantId, BiPublishApprovalCommand command, String actor);

    BiPublishApprovalView reviewPublishApproval(
            Long tenantId,
            BiPublishApprovalReviewCommand command,
            String actor);

    BiQuickEngineCapacityAlertPolicyView updateQuickEngineCapacityAlertPolicy(
            Long tenantId,
            BiQuickEngineCapacityAlertPolicyCommand command,
            String actor);

    BiQuickEngineTenantPoolPolicyView updateQuickEngineTenantPoolPolicy(
            Long tenantId,
            BiQuickEngineTenantPoolPolicyCommand command,
            String actor);

    BiChartView upsertChart(Long tenantId, BiChartCommand command, String actor);

    List<BiChartView> listChartResources(Long tenantId);

    BiChartView getChartResource(Long tenantId, String chartKey);

    BiChartReferenceImpactView chartReferenceImpact(Long tenantId, String chartKey);

    BiChartView publishChartResource(Long tenantId, String chartKey, String actor);

    void archiveChartResource(Long tenantId, String chartKey, String actor);

    List<BiResourceVersionView> listChartResourceVersions(Long tenantId, String chartKey);

    BiChartView restoreChartResourceVersion(Long tenantId, String chartKey, Integer version, String actor);

    BiDashboardView upsertDashboard(Long tenantId, BiDashboardCommand command, String actor);

    List<BiDashboardView> listDashboardResources(Long tenantId);

    BiDashboardView getDashboardResource(Long tenantId, String dashboardKey);

    BiDashboardReadModelView dashboardReadModel(Long tenantId, Long workspaceId, String dashboardKey);

    BiDashboardView cloneDashboardResource(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardCloneCommand command);

    BiDashboardExportPackageView exportDashboardResource(Long tenantId, String actor, String dashboardKey);

    org.chovy.canvas.bi.domain.BiDashboardResourceOperationsCatalog.DashboardPackageFile exportDashboardResourceFile(
            Long tenantId,
            String actor,
            String dashboardKey);

    BiDashboardView importDashboardResource(Long tenantId, String actor, BiDashboardImportCommand command);

    BiDashboardView publishDashboardResource(Long tenantId, String dashboardKey, String actor);

    BiDashboardView archiveDashboardResource(Long tenantId, String dashboardKey, String actor);

    List<BiResourceVersionView> listDashboardResourceVersions(Long tenantId, String dashboardKey, int limit);

    BiDashboardView restoreDashboardResourceVersion(Long tenantId, String dashboardKey, Integer version, String actor);

    BiDashboardRuntimeStateView getDashboardRuntimeState(Long tenantId, String actor, String dashboardKey);

    BiDashboardRuntimeStateView saveDashboardRuntimeState(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardRuntimeStateCommand command);

    BiPermissionGrantView grantPermission(Long tenantId, BiPermissionGrantCommand command, String actor);

    BiPermissionDecisionView effectiveAccess(BiAccessRequest request);

    List<BiResourcePermissionView> listResourcePermissions(
            Long tenantId,
            String resourceType,
            String resourceKey,
            Long resourceId);

    BiResourcePermissionView upsertResourcePermission(
            Long tenantId,
            BiResourcePermissionCommand command,
            String actor);

    void deleteResourcePermission(Long tenantId, String actor, Long id);

    List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey);

    BiRowPermissionView upsertRowPermission(Long tenantId, BiRowPermissionCommand command, String actor);

    void deleteRowPermission(Long tenantId, String actor, Long id);

    List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey);

    BiColumnPermissionView upsertColumnPermission(Long tenantId, BiColumnPermissionCommand command, String actor);

    void deleteColumnPermission(Long tenantId, String actor, Long id);

    List<BiPermissionAuditEntryView> permissionAudit(Long tenantId, int limit);

    List<BiPermissionRequestView> listPermissionRequests(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status);

    BiPermissionRequestView requestPermission(Long tenantId, BiPermissionRequestCommand command, String actor);

    BiPermissionRequestView reviewPermissionRequest(
            Long tenantId,
            BiPermissionRequestReviewCommand command,
            String actor);

    List<BiSubscriptionView> listSubscriptions(Long tenantId, int limit);

    BiSubscriptionView upsertSubscription(Long tenantId, BiSubscriptionCommand command, String actor);

    void deleteSubscription(Long tenantId, Long id);

    BiDeliveryRunResult runSubscriptionDelivery(Long tenantId, Long id, String actor);

    List<BiAlertRuleView> listAlertRules(Long tenantId, int limit);

    BiAlertRuleView upsertAlertRule(Long tenantId, BiAlertRuleCommand command, String actor);

    void deleteAlertRule(Long tenantId, Long id);

    BiDeliveryRunResult runAlertDelivery(Long tenantId, Long id, String actor);

    List<BiDeliveryLogView> listDeliveryLogs(Long tenantId, String jobType, Long jobId, int limit);

    BiDeliveryAuditSummary auditDeliveryLogs(
            Long tenantId,
            String jobType,
            String status,
            String channel,
            Long jobId,
            int limit);

    BiDeliveryRetryResult retryDeliveryLogs(Long tenantId, String actor, int limit);

    List<BiDeliveryAttachmentView> listDeliveryAttachments(
            Long tenantId,
            String jobType,
            Long jobId,
            Long deliveryLogId,
            int limit);

    BiDeliveryAttachmentDownload downloadDeliveryAttachment(Long tenantId, Long id, String actor);

    BiDeliveryAttachmentCleanupResult cleanupDeliveryAttachments(Long tenantId, int limit);

    BiDeliverySchedulerResult runDeliveryScheduler(Long tenantId, String actor);

    java.util.Map<String, Object> previewSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            BiSelfServicePreviewCommand command);

    BiSelfServiceExportJobView createSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            BiSelfServiceExportCommand command);

    List<BiSelfServiceExportJobView> listSelfServiceExports(Long tenantId, int limit);

    BiSelfServiceExportJobView reviewSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            Long id,
            BiSelfServiceExportReviewCommand command);

    BiSelfServiceExportJobDetailView getSelfServiceExportDetail(Long tenantId, Long id);

    BiSelfServiceExportDownload downloadSelfServiceExport(Long tenantId, String actor, Long id);

    BiSelfServiceExportJobView cancelSelfServiceExport(Long tenantId, String actor, Long id);

    BiSelfServiceExportCleanupResult cleanupSelfServiceExports(Long tenantId, int limit);

    BiSelfServiceExportRetryResult retrySelfServiceExports(Long tenantId, String actor, String role, int limit);

    BiSelfServiceExportQueueResult runSelfServiceExportQueue(Long tenantId, String actor, String role, int limit);

    BiQueryCompileResult compileQuery(Long tenantId, BiQueryCommand command, String actor);

    BiQueryResultView executeQuery(Long tenantId, BiQueryCommand command, String actor);

    BiQueryGateResult executeGatedQuery(Long tenantId, BiQueryGateCommand command, String actor);

    BiQueryGateResult executeContractGatedQuery(Long tenantId, BiQueryContractGateCommand command, String actor);

    BiQueryExplainResult explainQuery(Long tenantId, BiQueryCommand command, String actor);

    List<BiQueryHistoryItemView> listQueryHistory(Long tenantId, int limit);

    BiQueryHistoryDetailView queryHistoryDetail(Long tenantId, Long historyId);

    BiQueryCancelResult cancelQuery(Long tenantId, String sqlHash, String actor);

    BiQueryGovernanceSummaryView queryGovernanceSummary(Long tenantId, int limit);

    BiQueryGovernancePolicyView queryGovernancePolicy(Long tenantId);

    BiQueryGovernancePolicyView updateQueryGovernancePolicy(
            Long tenantId,
            BiQueryGovernancePolicyCommand command,
            String actor);

    List<BiQueryGovernanceAuditEntryView> queryGovernanceAudit(Long tenantId, int limit);

    BiQueryCachePolicyView queryCachePolicy(Long tenantId);

    BiQueryCachePolicyView updateQueryCachePolicy(Long tenantId, BiQueryCachePolicyCommand command, String actor);

    BiQueryCacheInvalidationResult invalidateQueryCache(Long tenantId, BiQueryCacheInvalidationCommand command);

    BiQueryCacheStatsView queryCacheStats(Long tenantId);

    List<BiDatasourceConnectorView> datasourceConnectors();

    List<BiDatasourceOnboardingView> listDatasources(Long tenantId);

    BiDatasourceOnboardingView createDatasource(
            Long tenantId,
            BiDatasourceOnboardingCommand command,
            String actor);

    BiDatasourceOnboardingView updateDatasource(
            Long tenantId,
            Long id,
            BiDatasourceOnboardingCommand command,
            String actor);

    BiDatasourceOnboardingView uploadDatasourceFile(
            Long tenantId,
            String actor,
            String filename,
            String name,
            String description,
            String sheetName,
            String delimiter,
            boolean headerRow,
            String encoding);

    BiDatasourceFileMaterializationResult materializeDatasourceFile(
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
            long maxRows);

    BiDatasourceConnectionTestResult testDatasourceConnection(Long tenantId, Long id);

    BiDatasourceCredentialRotationView rotateDatasourceCredential(
            Long tenantId,
            Long id,
            BiDatasourceCredentialRotationCommand command,
            String actor);

    BiDatasourceSchemaPreviewView previewDatasourceSchema(Long tenantId, Long id, int limit);

    BiDatasourceApiPreviewView previewDatasourceApi(
            Long tenantId,
            Long id,
            BiDatasourceApiPreviewCommand command);

    BiDatasourceSchemaSnapshotView syncDatasourceSchema(
            Long tenantId,
            Long id,
            int limit,
            BiDatasourceApiPreviewCommand command,
            String actor);

    BiDatasourceSchemaSnapshotView latestDatasourceSchemaSnapshot(Long tenantId, Long id);

    List<BiDatasourceSchemaSnapshotView> listDatasourceSchemaSnapshots(Long tenantId, Long id, int limit);

    List<BiDatasourceHealthView> datasourceHealth();

    List<BiDatasourceHealthSnapshotView> datasourceHealthHistory(int limit);

    BiDatasourceHealthSloView datasourceHealthSlo(int limit);

    BiEmbedTicketView createEmbedTicket(Long tenantId, BiEmbedTicketCommand command, String actor);

    BiEmbedTicketPayloadView verifyEmbedTicket(BiEmbedTicketVerifyCommand command, String origin);

    BiQueryResultView executeEmbedQuery(BiEmbedQueryCommand command, String origin);

    BiEmbedTicketCleanupResult cleanupEmbedTickets(Long tenantId, int limit);

    BiDashboardView embedDashboardResource(Long tenantId, String resourceKey, String ticket, String origin);

    BiDashboardRuntimeStateView embedDashboardRuntimeState(Long tenantId, String actor, String resourceKey, String ticket, String origin);

    BiPortalResourceView embedPortalResource(Long tenantId, String resourceKey, String ticket, String origin);

    List<BiPortalResourceView> listPortalResources(Long tenantId);

    BiPortalResourceView getPortalResource(Long tenantId, String portalKey);

    BiPortalResourceView savePortalDraft(
            Long tenantId,
            String portalKey,
            BiPortalResourceCommand command,
            String actor);

    BiPortalResourceView publishPortalResource(Long tenantId, String portalKey, String actor);

    void archivePortalResource(Long tenantId, String portalKey, String actor);

    List<BiResourceVersionView> listPortalResourceVersions(Long tenantId, String portalKey);

    BiPortalResourceView restorePortalResourceVersion(Long tenantId, String portalKey, Integer version, String actor);

    List<BiBigScreenResourceView> listBigScreenResources(Long tenantId);

    BiBigScreenResourceView getBigScreenResource(Long tenantId, String screenKey);

    BiBigScreenResourceView saveBigScreenDraft(
            Long tenantId,
            String screenKey,
            BiBigScreenResourceCommand command,
            String actor);

    BiBigScreenResourceView publishBigScreenResource(Long tenantId, String screenKey, String actor);

    void archiveBigScreenResource(Long tenantId, String screenKey, String actor);

    List<BiResourceVersionView> listBigScreenResourceVersions(Long tenantId, String screenKey);

    BiBigScreenResourceView restoreBigScreenResourceVersion(
            Long tenantId,
            String screenKey,
            Integer version,
            String actor);

    List<BiSpreadsheetResourceView> listSpreadsheetResources(Long tenantId);

    BiSpreadsheetResourceView getSpreadsheetResource(Long tenantId, String spreadsheetKey);

    BiSpreadsheetResourceView saveSpreadsheetDraft(
            Long tenantId,
            String spreadsheetKey,
            BiSpreadsheetResourceCommand command,
            String actor);

    BiSpreadsheetResourceView publishSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor);

    void archiveSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor);

    List<BiResourceVersionView> listSpreadsheetResourceVersions(Long tenantId, String spreadsheetKey);

    BiSpreadsheetResourceView restoreSpreadsheetResourceVersion(
            Long tenantId,
            String spreadsheetKey,
            Integer version,
            String actor);
}
