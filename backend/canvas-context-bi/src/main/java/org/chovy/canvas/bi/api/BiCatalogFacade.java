package org.chovy.canvas.bi.api;

import org.chovy.canvas.bi.domain.BiAccessRequest;

import java.util.List;

public interface BiCatalogFacade {

    BiWorkspaceView upsertWorkspace(Long tenantId, BiWorkspaceCommand command, String actor);

    BiDatasetView upsertDataset(Long tenantId, BiDatasetCommand command, String actor);

    List<BiDatasetView> listDatasetResources(Long tenantId);

    BiDatasetView getDatasetResource(Long tenantId, String datasetKey);

    List<BiQueryDatasetView> listQueryDatasets(Long tenantId);

    BiQueryDatasetView getQueryDataset(Long tenantId, String datasetKey);

    List<BiDashboardPresetView> listDashboardPresets(Long tenantId);

    BiDashboardPresetView getDashboardPreset(Long tenantId, String dashboardKey);

    BiQuickEngineCapacitySummaryView quickEngineCapacity(Long tenantId, Integer limit);

    BiQuickEngineQueueSnapshotView quickEngineQueue(Long tenantId, String poolKey, String status, Integer limit);

    BiResourceFavoriteView favoriteResource(Long tenantId, BiResourceFavoriteCommand command, String actor);

    List<BiResourceFavoriteView> listFavoriteResources(Long tenantId, String actor, String resourceType);

    void unfavoriteResource(Long tenantId, String actor, String resourceType, String resourceKey);

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

    BiDashboardView upsertDashboard(Long tenantId, BiDashboardCommand command, String actor);

    List<BiDashboardView> listDashboardResources(Long tenantId);

    BiDashboardView getDashboardResource(Long tenantId, String dashboardKey);

    BiDashboardReadModelView dashboardReadModel(Long tenantId, Long workspaceId, String dashboardKey);

    BiPermissionGrantView grantPermission(Long tenantId, BiPermissionGrantCommand command, String actor);

    BiPermissionDecisionView effectiveAccess(BiAccessRequest request);
}
