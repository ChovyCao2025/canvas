package org.chovy.canvas.bi.application;

import org.chovy.canvas.bi.api.BiCatalogFacade;
import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartDashboardReferenceView;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetFilterView;
import org.chovy.canvas.bi.api.BiDashboardPresetInteractionView;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardPresetWidgetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardReadinessIssueView;
import org.chovy.canvas.bi.api.BiDashboardReadinessView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldView;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiMetricView;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiPermissionGrantView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiQueryFieldView;
import org.chovy.canvas.bi.api.BiQueryMetricView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiWorkspaceCommand;
import org.chovy.canvas.bi.api.BiWorkspaceView;
import org.chovy.canvas.bi.domain.BiAccessDecision;
import org.chovy.canvas.bi.domain.BiAccessRequest;
import org.chovy.canvas.bi.domain.BiChart;
import org.chovy.canvas.bi.domain.BiChartRepository;
import org.chovy.canvas.bi.domain.BiDashboard;
import org.chovy.canvas.bi.domain.BiDashboardPresetCatalog;
import org.chovy.canvas.bi.domain.BiDashboardReadinessIssue;
import org.chovy.canvas.bi.domain.BiDashboardReadinessPolicy;
import org.chovy.canvas.bi.domain.BiDashboardReadinessReport;
import org.chovy.canvas.bi.domain.BiDashboardRepository;
import org.chovy.canvas.bi.domain.BiDataset;
import org.chovy.canvas.bi.domain.BiDatasetField;
import org.chovy.canvas.bi.domain.BiDatasetRepository;
import org.chovy.canvas.bi.domain.BiMetric;
import org.chovy.canvas.bi.domain.BiPermissionGrant;
import org.chovy.canvas.bi.domain.BiPermissionPolicy;
import org.chovy.canvas.bi.domain.BiPermissionRepository;
import org.chovy.canvas.bi.domain.BiQueryDatasetCatalog;
import org.chovy.canvas.bi.domain.BiQuickEngineCapacityCatalog;
import org.chovy.canvas.bi.domain.BiResourceFavoriteCatalog;
import org.chovy.canvas.bi.domain.BiResourceKey;
import org.chovy.canvas.bi.domain.BiResourceStatus;
import org.chovy.canvas.bi.domain.BiWorkspace;
import org.chovy.canvas.bi.domain.BiWorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final BiQuickEngineCapacityCatalog quickEngineCapacityCatalog;
    private final BiResourceFavoriteCatalog resourceFavoriteCatalog;
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
        this.quickEngineCapacityCatalog = new BiQuickEngineCapacityCatalog();
        this.resourceFavoriteCatalog = new BiResourceFavoriteCatalog();
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
        return toChartView(chartRepository.saveChart(chart));
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

    private BiWorkspace requireWorkspace(Long tenantId, Long workspaceId) {
        BiWorkspace workspace = workspaceRepository.findWorkspace(tenantId, requiredId(workspaceId, "workspaceId"));
        if (workspace == null) {
            throw new IllegalArgumentException("BI workspace not found");
        }
        return workspace;
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

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
