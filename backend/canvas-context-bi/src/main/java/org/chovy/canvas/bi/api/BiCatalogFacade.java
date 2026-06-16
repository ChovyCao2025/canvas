package org.chovy.canvas.bi.api;

import org.chovy.canvas.bi.domain.BiAccessRequest;

import java.util.List;
/**
 * BiCatalogFacade 门面接口。
 */
public interface BiCatalogFacade {
    /**
     * 创建或更新业务数据。
     */

    BiWorkspaceView upsertWorkspace(Long tenantId, BiWorkspaceCommand command, String actor);
    /**
     * 创建或更新业务数据。
     */

    BiDatasetView upsertDataset(Long tenantId, BiDatasetCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiDatasetView> listDatasetResources(Long tenantId);
    /**
     * 获取 Dataset Resource。
     */

    BiDatasetView getDatasetResource(Long tenantId, String datasetKey);
    /**
     * 发布业务资源。
     */

    BiDatasetView publishDatasetResource(Long tenantId, String datasetKey, String actor);
    /**
     * 归档业务资源。
     */

    BiDatasetView archiveDatasetResource(Long tenantId, String datasetKey, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceVersionView> listDatasetResourceVersions(Long tenantId, String datasetKey, int limit);
    /**
     * 恢复指定版本的业务资源。
     */

    BiDatasetView restoreDatasetResourceVersion(Long tenantId, String datasetKey, Integer version, String actor);
    /**
     * 执行 dataset Acceleration Policy 相关处理。
     */

    java.util.Map<String, Object> datasetAccelerationPolicy(Long tenantId, String datasetKey);
    /**
     * 创建或更新业务数据。
     */

    java.util.Map<String, Object> upsertDatasetAccelerationPolicy(
            Long tenantId,
            String datasetKey,
            java.util.Map<String, Object> command,
            String actor);
    /**
     * 执行 refresh Dataset Acceleration 相关处理。
     */

    java.util.Map<String, Object> refreshDatasetAcceleration(Long tenantId, String datasetKey, String actor);
    /**
     * 查询列表数据。
     */

    List<java.util.Map<String, Object>> listDatasetAccelerationRuns(Long tenantId, String datasetKey, int limit);
    /**
     * 执行 dataset Acceleration Capacity 相关处理。
     */

    java.util.Map<String, Object> datasetAccelerationCapacity(Long tenantId, String datasetKey, int limit);
    /**
     * 执行 cleanup Dataset Acceleration 相关处理。
     */

    java.util.Map<String, Object> cleanupDatasetAcceleration(
            Long tenantId,
            String datasetKey,
            int retainTables,
            String actor);
    /**
     * 查询列表数据。
     */

    List<BiQueryDatasetView> listQueryDatasets(Long tenantId);
    /**
     * 获取 Query Dataset。
     */

    BiQueryDatasetView getQueryDataset(Long tenantId, String datasetKey);
    /**
     * 查询列表数据。
     */

    List<BiDashboardPresetView> listDashboardPresets(Long tenantId);
    /**
     * 获取 Dashboard Preset。
     */

    BiDashboardPresetView getDashboardPreset(Long tenantId, String dashboardKey);
    /**
     * 执行 quick Engine Capacity 相关处理。
     */

    BiQuickEngineCapacitySummaryView quickEngineCapacity(Long tenantId, Integer limit);
    /**
     * 执行 quick Engine Queue 相关处理。
     */

    BiQuickEngineQueueSnapshotView quickEngineQueue(Long tenantId, String poolKey, String status, Integer limit);
    /**
     * 执行 favorite Resource 相关处理。
     */

    BiResourceFavoriteView favoriteResource(Long tenantId, BiResourceFavoriteCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceFavoriteView> listFavoriteResources(Long tenantId, String actor, String resourceType);
    /**
     * 执行 unfavorite Resource 相关处理。
     */

    void unfavoriteResource(Long tenantId, String actor, String resourceType, String resourceKey);
    /**
     * 执行 ai Assistant 相关处理。
     */

    BiAiResponseView aiAssistant(Long tenantId, String operation, BiAiRequestCommand command, String actor);
    /**
     * 执行 add Resource Comment 相关处理。
     */

    BiResourceCommentView addResourceComment(Long tenantId, BiResourceCommentCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceCommentView> listResourceComments(Long tenantId, String resourceType, String resourceKey);
    /**
     * 删除业务数据。
     */

    void deleteResourceComment(Long tenantId, String actor, Long commentId);
    /**
     * 执行 acquire Resource Lock 相关处理。
     */

    BiResourceLockView acquireResourceLock(Long tenantId, BiResourceLockCommand command, String actor);
    /**
     * 执行 current Resource Lock 相关处理。
     */

    BiResourceLockView currentResourceLock(Long tenantId, String resourceType, String resourceKey);
    /**
     * 执行 release Resource Lock 相关处理。
     */

    void releaseResourceLock(Long tenantId, String actor, BiResourceLockCommand command);
    /**
     * 执行 update Resource Location 相关处理。
     */

    BiResourceLocationView updateResourceLocation(Long tenantId, BiResourceLocationCommand command, String actor);
    /**
     * 执行 move Resource 相关处理。
     */

    BiResourceLocationView moveResource(Long tenantId, BiResourceMoveCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceLocationView> listResourceLocations(Long tenantId, String resourceType);
    /**
     * 执行 transfer Resource 相关处理。
     */

    BiResourceOwnershipView transferResource(Long tenantId, BiResourceTransferCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceOwnershipView> listResourceOwnerships(Long tenantId, String resourceType);
    /**
     * 查询列表数据。
     */

    List<BiPublishApprovalView> listPublishApprovals(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status);
    /**
     * 执行 request Publish Approval 相关处理。
     */

    BiPublishApprovalView requestPublishApproval(Long tenantId, BiPublishApprovalCommand command, String actor);
    /**
     * 执行 review Publish Approval 相关处理。
     */

    BiPublishApprovalView reviewPublishApproval(
            Long tenantId,
            BiPublishApprovalReviewCommand command,
            String actor);
    /**
     * 执行 update Quick Engine Capacity Alert Policy 相关处理。
     */

    BiQuickEngineCapacityAlertPolicyView updateQuickEngineCapacityAlertPolicy(
            Long tenantId,
            BiQuickEngineCapacityAlertPolicyCommand command,
            String actor);
    /**
     * 执行 update Quick Engine Tenant Pool Policy 相关处理。
     */

    BiQuickEngineTenantPoolPolicyView updateQuickEngineTenantPoolPolicy(
            Long tenantId,
            BiQuickEngineTenantPoolPolicyCommand command,
            String actor);
    /**
     * 创建或更新业务数据。
     */

    BiChartView upsertChart(Long tenantId, BiChartCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiChartView> listChartResources(Long tenantId);
    /**
     * 获取 Chart Resource。
     */

    BiChartView getChartResource(Long tenantId, String chartKey);
    /**
     * 执行 chart Reference Impact 相关处理。
     */

    BiChartReferenceImpactView chartReferenceImpact(Long tenantId, String chartKey);
    /**
     * 发布业务资源。
     */

    BiChartView publishChartResource(Long tenantId, String chartKey, String actor);
    /**
     * 归档业务资源。
     */

    void archiveChartResource(Long tenantId, String chartKey, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceVersionView> listChartResourceVersions(Long tenantId, String chartKey);
    /**
     * 恢复指定版本的业务资源。
     */

    BiChartView restoreChartResourceVersion(Long tenantId, String chartKey, Integer version, String actor);
    /**
     * 创建或更新业务数据。
     */

    BiDashboardView upsertDashboard(Long tenantId, BiDashboardCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiDashboardView> listDashboardResources(Long tenantId);
    /**
     * 获取 Dashboard Resource。
     */

    BiDashboardView getDashboardResource(Long tenantId, String dashboardKey);
    /**
     * 执行 dashboard Read Model 相关处理。
     */

    BiDashboardReadModelView dashboardReadModel(Long tenantId, Long workspaceId, String dashboardKey);
    /**
     * 执行 clone Dashboard Resource 相关处理。
     */

    BiDashboardView cloneDashboardResource(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardCloneCommand command);
    /**
     * 执行 export Dashboard Resource 相关处理。
     */

    BiDashboardExportPackageView exportDashboardResource(Long tenantId, String actor, String dashboardKey);
    /**
     * 执行 export Dashboard Resource File 相关处理。
     */

    org.chovy.canvas.bi.domain.BiDashboardResourceOperationsCatalog.DashboardPackageFile exportDashboardResourceFile(
            Long tenantId,
            String actor,
            String dashboardKey);
    /**
     * 执行 import Dashboard Resource 相关处理。
     */

    BiDashboardView importDashboardResource(Long tenantId, String actor, BiDashboardImportCommand command);
    /**
     * 发布业务资源。
     */

    BiDashboardView publishDashboardResource(Long tenantId, String dashboardKey, String actor);
    /**
     * 归档业务资源。
     */

    BiDashboardView archiveDashboardResource(Long tenantId, String dashboardKey, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceVersionView> listDashboardResourceVersions(Long tenantId, String dashboardKey, int limit);
    /**
     * 恢复指定版本的业务资源。
     */

    BiDashboardView restoreDashboardResourceVersion(Long tenantId, String dashboardKey, Integer version, String actor);
    /**
     * 获取 Dashboard Runtime State。
     */

    BiDashboardRuntimeStateView getDashboardRuntimeState(Long tenantId, String actor, String dashboardKey);
    /**
     * 执行 save Dashboard Runtime State 相关处理。
     */

    BiDashboardRuntimeStateView saveDashboardRuntimeState(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardRuntimeStateCommand command);
    /**
     * 执行 grant Permission 相关处理。
     */

    BiPermissionGrantView grantPermission(Long tenantId, BiPermissionGrantCommand command, String actor);
    /**
     * 执行 effective Access 相关处理。
     */

    BiPermissionDecisionView effectiveAccess(BiAccessRequest request);
    /**
     * 查询列表数据。
     */

    List<BiResourcePermissionView> listResourcePermissions(
            Long tenantId,
            String resourceType,
            String resourceKey,
            Long resourceId);
    /**
     * 创建或更新业务数据。
     */

    BiResourcePermissionView upsertResourcePermission(
            Long tenantId,
            BiResourcePermissionCommand command,
            String actor);
    /**
     * 删除业务数据。
     */

    void deleteResourcePermission(Long tenantId, String actor, Long id);
    /**
     * 查询列表数据。
     */

    List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey);
    /**
     * 创建或更新业务数据。
     */

    BiRowPermissionView upsertRowPermission(Long tenantId, BiRowPermissionCommand command, String actor);
    /**
     * 删除业务数据。
     */

    void deleteRowPermission(Long tenantId, String actor, Long id);
    /**
     * 查询列表数据。
     */

    List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey);
    /**
     * 创建或更新业务数据。
     */

    BiColumnPermissionView upsertColumnPermission(Long tenantId, BiColumnPermissionCommand command, String actor);
    /**
     * 删除业务数据。
     */

    void deleteColumnPermission(Long tenantId, String actor, Long id);
    /**
     * 执行 permission Audit 相关处理。
     */

    List<BiPermissionAuditEntryView> permissionAudit(Long tenantId, int limit);
    /**
     * 查询列表数据。
     */

    List<BiPermissionRequestView> listPermissionRequests(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status);
    /**
     * 执行 request Permission 相关处理。
     */

    BiPermissionRequestView requestPermission(Long tenantId, BiPermissionRequestCommand command, String actor);
    /**
     * 执行 review Permission Request 相关处理。
     */

    BiPermissionRequestView reviewPermissionRequest(
            Long tenantId,
            BiPermissionRequestReviewCommand command,
            String actor);
    /**
     * 查询列表数据。
     */

    List<BiSubscriptionView> listSubscriptions(Long tenantId, int limit);
    /**
     * 创建或更新业务数据。
     */

    BiSubscriptionView upsertSubscription(Long tenantId, BiSubscriptionCommand command, String actor);
    /**
     * 删除业务数据。
     */

    void deleteSubscription(Long tenantId, Long id);
    /**
     * 执行 run Subscription Delivery 相关处理。
     */

    BiDeliveryRunResult runSubscriptionDelivery(Long tenantId, Long id, String actor);
    /**
     * 查询列表数据。
     */

    List<BiAlertRuleView> listAlertRules(Long tenantId, int limit);
    /**
     * 创建或更新业务数据。
     */

    BiAlertRuleView upsertAlertRule(Long tenantId, BiAlertRuleCommand command, String actor);
    /**
     * 删除业务数据。
     */

    void deleteAlertRule(Long tenantId, Long id);
    /**
     * 执行 run Alert Delivery 相关处理。
     */

    BiDeliveryRunResult runAlertDelivery(Long tenantId, Long id, String actor);
    /**
     * 查询列表数据。
     */

    List<BiDeliveryLogView> listDeliveryLogs(Long tenantId, String jobType, Long jobId, int limit);

    BiDeliveryAuditSummary auditDeliveryLogs(
            Long tenantId,
            String jobType,
            String status,
            String channel,
            Long jobId,
            int limit);
    /**
     * 执行 retry Delivery Logs 相关处理。
     */

    BiDeliveryRetryResult retryDeliveryLogs(Long tenantId, String actor, int limit);

    List<BiDeliveryAttachmentView> listDeliveryAttachments(
            Long tenantId,
            String jobType,
            Long jobId,
            Long deliveryLogId,
            int limit);
    /**
     * 执行 download Delivery Attachment 相关处理。
     */

    BiDeliveryAttachmentDownload downloadDeliveryAttachment(Long tenantId, Long id, String actor);
    /**
     * 执行 cleanup Delivery Attachments 相关处理。
     */

    BiDeliveryAttachmentCleanupResult cleanupDeliveryAttachments(Long tenantId, int limit);
    /**
     * 执行 run Delivery Scheduler 相关处理。
     */

    BiDeliverySchedulerResult runDeliveryScheduler(Long tenantId, String actor);
    /**
     * 执行 preview Self Service Export 相关处理。
     */

    java.util.Map<String, Object> previewSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            BiSelfServicePreviewCommand command);
    /**
     * 执行 create Self Service Export 相关处理。
     */

    BiSelfServiceExportJobView createSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            BiSelfServiceExportCommand command);
    /**
     * 查询列表数据。
     */

    List<BiSelfServiceExportJobView> listSelfServiceExports(Long tenantId, int limit);

    BiSelfServiceExportJobView reviewSelfServiceExport(
            Long tenantId,
            String actor,
            String role,
            Long id,
            BiSelfServiceExportReviewCommand command);
    /**
     * 获取 Self Service Export Detail。
     */

    BiSelfServiceExportJobDetailView getSelfServiceExportDetail(Long tenantId, Long id);
    /**
     * 执行 download Self Service Export 相关处理。
     */

    BiSelfServiceExportDownload downloadSelfServiceExport(Long tenantId, String actor, Long id);
    /**
     * 执行 cancel Self Service Export 相关处理。
     */

    BiSelfServiceExportJobView cancelSelfServiceExport(Long tenantId, String actor, Long id);
    /**
     * 执行 cleanup Self Service Exports 相关处理。
     */

    BiSelfServiceExportCleanupResult cleanupSelfServiceExports(Long tenantId, int limit);
    /**
     * 执行 retry Self Service Exports 相关处理。
     */

    BiSelfServiceExportRetryResult retrySelfServiceExports(Long tenantId, String actor, String role, int limit);
    /**
     * 执行 run Self Service Export Queue 相关处理。
     */

    BiSelfServiceExportQueueResult runSelfServiceExportQueue(Long tenantId, String actor, String role, int limit);
    /**
     * 执行 compile Query 相关处理。
     */

    BiQueryCompileResult compileQuery(Long tenantId, BiQueryCommand command, String actor);
    /**
     * 执行 execute Query 相关处理。
     */

    BiQueryResultView executeQuery(Long tenantId, BiQueryCommand command, String actor);
    /**
     * 执行 execute Gated Query 相关处理。
     */

    BiQueryGateResult executeGatedQuery(Long tenantId, BiQueryGateCommand command, String actor);
    /**
     * 执行 execute Contract Gated Query 相关处理。
     */

    BiQueryGateResult executeContractGatedQuery(Long tenantId, BiQueryContractGateCommand command, String actor);
    /**
     * 执行 explain Query 相关处理。
     */

    BiQueryExplainResult explainQuery(Long tenantId, BiQueryCommand command, String actor);
    /**
     * 查询列表数据。
     */

    List<BiQueryHistoryItemView> listQueryHistory(Long tenantId, int limit);
    /**
     * 执行 query History Detail 相关处理。
     */

    BiQueryHistoryDetailView queryHistoryDetail(Long tenantId, Long historyId);
    /**
     * 执行 cancel Query 相关处理。
     */

    BiQueryCancelResult cancelQuery(Long tenantId, String sqlHash, String actor);
    /**
     * 执行 query Governance Summary 相关处理。
     */

    BiQueryGovernanceSummaryView queryGovernanceSummary(Long tenantId, int limit);
    /**
     * 执行 query Governance Policy 相关处理。
     */

    BiQueryGovernancePolicyView queryGovernancePolicy(Long tenantId);
    /**
     * 执行 update Query Governance Policy 相关处理。
     */

    BiQueryGovernancePolicyView updateQueryGovernancePolicy(
            Long tenantId,
            BiQueryGovernancePolicyCommand command,
            String actor);
    /**
     * 执行 query Governance Audit 相关处理。
     */

    List<BiQueryGovernanceAuditEntryView> queryGovernanceAudit(Long tenantId, int limit);
    /**
     * 执行 query Cache Policy 相关处理。
     */

    BiQueryCachePolicyView queryCachePolicy(Long tenantId);
    /**
     * 执行 update Query Cache Policy 相关处理。
     */

    BiQueryCachePolicyView updateQueryCachePolicy(Long tenantId, BiQueryCachePolicyCommand command, String actor);
    /**
     * 执行 invalidate Query Cache 相关处理。
     */

    BiQueryCacheInvalidationResult invalidateQueryCache(Long tenantId, BiQueryCacheInvalidationCommand command);
    /**
     * 执行 query Cache Stats 相关处理。
     */

    BiQueryCacheStatsView queryCacheStats(Long tenantId);
    /**
     * 执行 datasource Connectors 相关处理。
     */

    List<BiDatasourceConnectorView> datasourceConnectors();
    /**
     * 查询列表数据。
     */

    List<BiDatasourceOnboardingView> listDatasources(Long tenantId);
    /**
     * 执行 create Datasource 相关处理。
     */

    BiDatasourceOnboardingView createDatasource(
            Long tenantId,
            BiDatasourceOnboardingCommand command,
            String actor);
    /**
     * 执行 update Datasource 相关处理。
     */

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
    /**
     * 验证对应业务场景。
     */

    BiDatasourceConnectionTestResult testDatasourceConnection(Long tenantId, Long id);
    /**
     * 执行 rotate Datasource Credential 相关处理。
     */

    BiDatasourceCredentialRotationView rotateDatasourceCredential(
            Long tenantId,
            Long id,
            BiDatasourceCredentialRotationCommand command,
            String actor);
    /**
     * 执行 preview Datasource Schema 相关处理。
     */

    BiDatasourceSchemaPreviewView previewDatasourceSchema(Long tenantId, Long id, int limit);
    /**
     * 执行 preview Datasource Api 相关处理。
     */

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
    /**
     * 执行 latest Datasource Schema Snapshot 相关处理。
     */

    BiDatasourceSchemaSnapshotView latestDatasourceSchemaSnapshot(Long tenantId, Long id);
    /**
     * 查询列表数据。
     */

    List<BiDatasourceSchemaSnapshotView> listDatasourceSchemaSnapshots(Long tenantId, Long id, int limit);
    /**
     * 执行 datasource Health 相关处理。
     */

    List<BiDatasourceHealthView> datasourceHealth();
    /**
     * 执行 datasource Health History 相关处理。
     */

    List<BiDatasourceHealthSnapshotView> datasourceHealthHistory(int limit);
    /**
     * 执行 datasource Health Slo 相关处理。
     */

    BiDatasourceHealthSloView datasourceHealthSlo(int limit);
    /**
     * 执行 create Embed Ticket 相关处理。
     */

    BiEmbedTicketView createEmbedTicket(Long tenantId, BiEmbedTicketCommand command, String actor);
    /**
     * 执行 verify Embed Ticket 相关处理。
     */

    BiEmbedTicketPayloadView verifyEmbedTicket(BiEmbedTicketVerifyCommand command, String origin);
    /**
     * 执行 execute Embed Query 相关处理。
     */

    BiQueryResultView executeEmbedQuery(BiEmbedQueryCommand command, String origin);
    /**
     * 执行 cleanup Embed Tickets 相关处理。
     */

    BiEmbedTicketCleanupResult cleanupEmbedTickets(Long tenantId, int limit);
    /**
     * 执行 embed Dashboard Resource 相关处理。
     */

    BiDashboardView embedDashboardResource(Long tenantId, String resourceKey, String ticket, String origin);
    /**
     * 执行 embed Dashboard Runtime State 相关处理。
     */

    BiDashboardRuntimeStateView embedDashboardRuntimeState(Long tenantId, String actor, String resourceKey, String ticket, String origin);
    /**
     * 执行 embed Portal Resource 相关处理。
     */

    BiPortalResourceView embedPortalResource(Long tenantId, String resourceKey, String ticket, String origin);
    /**
     * 查询列表数据。
     */

    List<BiPortalResourceView> listPortalResources(Long tenantId);
    /**
     * 获取 Portal Resource。
     */

    BiPortalResourceView getPortalResource(Long tenantId, String portalKey);
    /**
     * 执行 save Portal Draft 相关处理。
     */

    BiPortalResourceView savePortalDraft(
            Long tenantId,
            String portalKey,
            BiPortalResourceCommand command,
            String actor);
    /**
     * 发布业务资源。
     */

    BiPortalResourceView publishPortalResource(Long tenantId, String portalKey, String actor);
    /**
     * 归档业务资源。
     */

    void archivePortalResource(Long tenantId, String portalKey, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceVersionView> listPortalResourceVersions(Long tenantId, String portalKey);
    /**
     * 恢复指定版本的业务资源。
     */

    BiPortalResourceView restorePortalResourceVersion(Long tenantId, String portalKey, Integer version, String actor);
    /**
     * 查询列表数据。
     */

    List<BiBigScreenResourceView> listBigScreenResources(Long tenantId);
    /**
     * 获取 Big Screen Resource。
     */

    BiBigScreenResourceView getBigScreenResource(Long tenantId, String screenKey);
    /**
     * 执行 save Big Screen Draft 相关处理。
     */

    BiBigScreenResourceView saveBigScreenDraft(
            Long tenantId,
            String screenKey,
            BiBigScreenResourceCommand command,
            String actor);
    /**
     * 发布业务资源。
     */

    BiBigScreenResourceView publishBigScreenResource(Long tenantId, String screenKey, String actor);
    /**
     * 归档业务资源。
     */

    void archiveBigScreenResource(Long tenantId, String screenKey, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceVersionView> listBigScreenResourceVersions(Long tenantId, String screenKey);
    /**
     * 恢复指定版本的业务资源。
     */

    BiBigScreenResourceView restoreBigScreenResourceVersion(
            Long tenantId,
            String screenKey,
            Integer version,
            String actor);
    /**
     * 查询列表数据。
     */

    List<BiSpreadsheetResourceView> listSpreadsheetResources(Long tenantId);
    /**
     * 获取 Spreadsheet Resource。
     */

    BiSpreadsheetResourceView getSpreadsheetResource(Long tenantId, String spreadsheetKey);
    /**
     * 执行 save Spreadsheet Draft 相关处理。
     */

    BiSpreadsheetResourceView saveSpreadsheetDraft(
            Long tenantId,
            String spreadsheetKey,
            BiSpreadsheetResourceCommand command,
            String actor);
    /**
     * 发布业务资源。
     */

    BiSpreadsheetResourceView publishSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor);
    /**
     * 归档业务资源。
     */

    void archiveSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor);
    /**
     * 查询列表数据。
     */

    List<BiResourceVersionView> listSpreadsheetResourceVersions(Long tenantId, String spreadsheetKey);
    /**
     * 恢复指定版本的业务资源。
     */

    BiSpreadsheetResourceView restoreSpreadsheetResourceVersion(
            Long tenantId,
            String spreadsheetKey,
            Integer version,
            String actor);
}
