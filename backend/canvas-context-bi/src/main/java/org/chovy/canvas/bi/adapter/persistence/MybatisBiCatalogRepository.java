package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.bi.domain.BiChart;
import org.chovy.canvas.bi.domain.BiChartRepository;
import org.chovy.canvas.bi.domain.BiDashboard;
import org.chovy.canvas.bi.domain.BiDashboardRepository;
import org.chovy.canvas.bi.domain.BiDataset;
import org.chovy.canvas.bi.domain.BiDatasetRepository;
import org.chovy.canvas.bi.domain.BiPermissionGrant;
import org.chovy.canvas.bi.domain.BiPermissionRepository;
import org.chovy.canvas.bi.domain.BiResourceKey;
import org.chovy.canvas.bi.domain.BiResourceStatus;
import org.chovy.canvas.bi.domain.BiWorkspace;
import org.chovy.canvas.bi.domain.BiWorkspaceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisBiCatalogRepository implements BiWorkspaceRepository, BiDatasetRepository, BiChartRepository,
        BiDashboardRepository, BiPermissionRepository {

    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDatasetFieldMapper fieldMapper;
    private final BiMetricMapper metricMapper;
    private final BiChartMapper chartMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiDashboardWidgetMapper dashboardWidgetMapper;
    private final BiResourcePermissionMapper permissionMapper;
    private final BiPersistenceConverter converter;

    public MybatisBiCatalogRepository(BiWorkspaceMapper workspaceMapper,
                                      BiDatasetMapper datasetMapper,
                                      BiDatasetFieldMapper fieldMapper,
                                      BiMetricMapper metricMapper,
                                      BiChartMapper chartMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper dashboardWidgetMapper,
                                      BiResourcePermissionMapper permissionMapper,
                                      BiPersistenceConverter converter) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.fieldMapper = fieldMapper;
        this.metricMapper = metricMapper;
        this.chartMapper = chartMapper;
        this.dashboardMapper = dashboardMapper;
        this.dashboardWidgetMapper = dashboardWidgetMapper;
        this.permissionMapper = permissionMapper;
        this.converter = converter;
    }

    @Override
    public BiWorkspace findWorkspace(Long tenantId, Long workspaceId) {
        BiWorkspaceDO row = workspaceMapper.selectById(workspaceId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return converter.toWorkspace(row);
    }

    @Override
    public BiWorkspace saveWorkspace(BiWorkspace workspace) {
        BiWorkspaceDO row = converter.toWorkspaceRow(workspace);
        if (row.getId() == null) {
            workspaceMapper.insert(row);
        } else {
            workspaceMapper.updateById(row);
        }
        return converter.toWorkspace(row);
    }

    @Override
    public BiDataset findDatasetByKey(Long tenantId, Long workspaceId, BiResourceKey datasetKey) {
        BiDatasetDO row = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetDO::getDatasetKey, datasetKey.value())
                .last("LIMIT 1"));
        return toDataset(row);
    }

    @Override
    public BiDataset findDatasetById(Long tenantId, Long datasetId) {
        BiDatasetDO row = datasetMapper.selectById(datasetId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return toDataset(row);
    }

    @Override
    public List<BiDataset> listAvailableDatasets(Long tenantId) {
        Long workspaceId = defaultWorkspaceId(tenantId);
        return datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                        .eq(BiDatasetDO::getTenantId, tenantId)
                        .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                        .ne(BiDatasetDO::getStatus, BiResourceStatus.ARCHIVED.name())
                        .orderByDesc(BiDatasetDO::getUpdatedAt)
                        .orderByAsc(BiDatasetDO::getDatasetKey))
                .stream()
                .map(this::toDataset)
                .filter(dataset -> dataset != null)
                .toList();
    }

    @Override
    public BiDataset findAvailableDatasetByKeyWithTenantFallback(Long tenantId, BiResourceKey datasetKey) {
        BiDataset dataset = findAvailableDatasetByKey(tenantId, datasetKey);
        if (dataset != null || Long.valueOf(0L).equals(tenantId)) {
            return dataset;
        }
        return findAvailableDatasetByKey(0L, datasetKey);
    }

    private BiDataset findAvailableDatasetByKey(Long tenantId, BiResourceKey datasetKey) {
        Long workspaceId = defaultWorkspaceId(tenantId);
        BiDatasetDO row = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetDO::getDatasetKey, datasetKey.value())
                .ne(BiDatasetDO::getStatus, BiResourceStatus.ARCHIVED.name())
                .orderByDesc(BiDatasetDO::getUpdatedAt)
                .last("LIMIT 1"));
        return toDataset(row);
    }

    @Override
    public BiDataset saveDataset(BiDataset dataset) {
        BiDatasetDO row = converter.toDatasetRow(dataset);
        if (row.getId() == null) {
            datasetMapper.insert(row);
        } else {
            datasetMapper.updateById(row);
        }
        replaceDatasetChildren(row, dataset);
        return toDataset(row);
    }

    @Override
    public BiChart findChartByKey(Long tenantId, Long workspaceId, BiResourceKey chartKey) {
        BiChartDO row = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                .eq(BiChartDO::getTenantId, tenantId)
                .eq(BiChartDO::getWorkspaceId, workspaceId)
                .eq(BiChartDO::getChartKey, chartKey.value())
                .last("LIMIT 1"));
        return toChart(row);
    }

    @Override
    public List<BiChart> listChartsByKeys(Long tenantId, Long workspaceId, List<BiResourceKey> chartKeys) {
        if (chartKeys == null || chartKeys.isEmpty()) {
            return List.of();
        }
        List<String> keyValues = chartKeys.stream().map(BiResourceKey::value).toList();
        return chartMapper.selectList(new LambdaQueryWrapper<BiChartDO>()
                        .eq(BiChartDO::getTenantId, tenantId)
                        .eq(BiChartDO::getWorkspaceId, workspaceId)
                        .in(BiChartDO::getChartKey, keyValues))
                .stream()
                .map(this::toChart)
                .filter(chart -> chart != null)
                .toList();
    }

    @Override
    public List<BiChart> listAvailableCharts(Long tenantId) {
        Long workspaceId = defaultWorkspaceId(tenantId);
        return chartMapper.selectList(new LambdaQueryWrapper<BiChartDO>()
                        .eq(BiChartDO::getTenantId, tenantId)
                        .eq(BiChartDO::getWorkspaceId, workspaceId)
                        .ne(BiChartDO::getStatus, BiResourceStatus.ARCHIVED.name())
                        .orderByDesc(BiChartDO::getUpdatedAt)
                        .orderByAsc(BiChartDO::getChartKey))
                .stream()
                .map(this::toChart)
                .filter(chart -> chart != null)
                .toList();
    }

    @Override
    public BiChart findAvailableChartByKey(Long tenantId, BiResourceKey chartKey) {
        Long workspaceId = defaultWorkspaceId(tenantId);
        BiChartDO row = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                .eq(BiChartDO::getTenantId, tenantId)
                .eq(BiChartDO::getWorkspaceId, workspaceId)
                .eq(BiChartDO::getChartKey, chartKey.value())
                .ne(BiChartDO::getStatus, BiResourceStatus.ARCHIVED.name())
                .orderByDesc(BiChartDO::getUpdatedAt)
                .last("LIMIT 1"));
        return toChart(row);
    }

    @Override
    public BiChart saveChart(BiChart chart) {
        BiChartDO row = converter.toChartRow(chart);
        if (row.getId() == null) {
            chartMapper.insert(row);
        } else {
            chartMapper.updateById(row);
        }
        return toChart(row);
    }

    @Override
    public BiDashboard findDashboardByKey(Long tenantId, Long workspaceId, BiResourceKey dashboardKey) {
        BiDashboardDO row = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                .eq(BiDashboardDO::getTenantId, tenantId)
                .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardDO::getDashboardKey, dashboardKey.value())
                .last("LIMIT 1"));
        return toDashboard(row);
    }

    @Override
    public List<BiDashboard> listAvailableDashboards(Long tenantId) {
        Long workspaceId = defaultWorkspaceId(tenantId);
        return dashboardMapper.selectList(new LambdaQueryWrapper<BiDashboardDO>()
                        .eq(BiDashboardDO::getTenantId, tenantId)
                        .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                        .ne(BiDashboardDO::getStatus, BiResourceStatus.ARCHIVED.name())
                        .orderByDesc(BiDashboardDO::getUpdatedAt)
                        .orderByAsc(BiDashboardDO::getDashboardKey))
                .stream()
                .map(this::toDashboard)
                .filter(dashboard -> dashboard != null)
                .toList();
    }

    @Override
    public BiDashboard findAvailableDashboardByKey(Long tenantId, BiResourceKey dashboardKey) {
        Long workspaceId = defaultWorkspaceId(tenantId);
        BiDashboardDO row = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                .eq(BiDashboardDO::getTenantId, tenantId)
                .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardDO::getDashboardKey, dashboardKey.value())
                .ne(BiDashboardDO::getStatus, BiResourceStatus.ARCHIVED.name())
                .orderByDesc(BiDashboardDO::getUpdatedAt)
                .last("LIMIT 1"));
        return toDashboard(row);
    }

    @Override
    public BiDashboard saveDashboard(BiDashboard dashboard) {
        BiDashboardDO row = converter.toDashboardRow(dashboard);
        if (row.getId() == null) {
            dashboardMapper.insert(row);
        } else {
            dashboardMapper.updateById(row);
        }
        replaceDashboardWidgets(row, dashboard);
        return toDashboard(row);
    }

    @Override
    public BiPermissionGrant saveGrant(BiPermissionGrant grant) {
        BiResourcePermissionDO row = converter.toPermissionRow(grant);
        if (row.getId() == null) {
            permissionMapper.insert(row);
        } else {
            permissionMapper.updateById(row);
        }
        return converter.toPermissionGrant(row);
    }

    @Override
    public void deleteGrant(Long tenantId,
                            Long workspaceId,
                            String resourceType,
                            Long resourceId,
                            String subjectType,
                            String subjectId,
                            String actionKey) {
        permissionMapper.delete(new LambdaQueryWrapper<BiResourcePermissionDO>()
                .eq(BiResourcePermissionDO::getTenantId, tenantId)
                .eq(BiResourcePermissionDO::getWorkspaceId, workspaceId)
                .eq(BiResourcePermissionDO::getResourceType, resourceType)
                .eq(BiResourcePermissionDO::getResourceId, resourceId)
                .eq(BiResourcePermissionDO::getSubjectType, subjectType)
                .eq(BiResourcePermissionDO::getSubjectId, subjectId)
                .eq(BiResourcePermissionDO::getActionKey, actionKey));
    }

    @Override
    public List<BiPermissionGrant> listResourceGrants(Long tenantId,
                                                      Long workspaceId,
                                                      String resourceType,
                                                      Long resourceId) {
        return permissionMapper.selectList(new LambdaQueryWrapper<BiResourcePermissionDO>()
                        .eq(BiResourcePermissionDO::getTenantId, tenantId)
                        .eq(BiResourcePermissionDO::getWorkspaceId, workspaceId)
                        .eq(BiResourcePermissionDO::getResourceType, resourceType)
                        .eq(BiResourcePermissionDO::getResourceId, resourceId))
                .stream()
                .map(converter::toPermissionGrant)
                .toList();
    }

    private Long defaultWorkspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, DEFAULT_WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1"));
        return workspace == null || workspace.getId() == null ? 0L : workspace.getId();
    }

    private BiDataset toDataset(BiDatasetDO row) {
        if (row == null) {
            return null;
        }
        List<BiDatasetFieldDO> fields = fieldMapper.selectList(new LambdaQueryWrapper<BiDatasetFieldDO>()
                .eq(BiDatasetFieldDO::getTenantId, row.getTenantId())
                .eq(BiDatasetFieldDO::getDatasetId, row.getId())
                .orderByAsc(BiDatasetFieldDO::getSortOrder));
        List<BiMetricDO> metrics = metricMapper.selectList(new LambdaQueryWrapper<BiMetricDO>()
                .eq(BiMetricDO::getTenantId, row.getTenantId())
                .eq(BiMetricDO::getDatasetId, row.getId())
                .orderByAsc(BiMetricDO::getMetricKey));
        return converter.toDataset(row, fields, metrics);
    }

    private BiChart toChart(BiChartDO row) {
        if (row == null) {
            return null;
        }
        BiDataset dataset = findDatasetById(row.getTenantId(), row.getDatasetId());
        return converter.toChart(row, dataset);
    }

    private BiDashboard toDashboard(BiDashboardDO row) {
        if (row == null) {
            return null;
        }
        List<String> chartKeys = dashboardWidgetMapper.selectList(new LambdaQueryWrapper<BiDashboardWidgetDO>()
                        .eq(BiDashboardWidgetDO::getTenantId, row.getTenantId())
                        .eq(BiDashboardWidgetDO::getDashboardId, row.getId())
                        .orderByAsc(BiDashboardWidgetDO::getId))
                .stream()
                .map(this::chartKeyFromWidget)
                .filter(key -> key != null && !key.isBlank())
                .toList();
        return converter.toDashboard(row, chartKeys);
    }

    private void replaceDatasetChildren(BiDatasetDO row, BiDataset dataset) {
        if (row.getId() == null) {
            return;
        }
        fieldMapper.delete(new LambdaQueryWrapper<BiDatasetFieldDO>()
                .eq(BiDatasetFieldDO::getTenantId, row.getTenantId())
                .eq(BiDatasetFieldDO::getDatasetId, row.getId()));
        for (var field : dataset.fields()) {
            fieldMapper.insert(converter.toDatasetFieldRow(row.getTenantId(), row.getId(), field));
        }
        metricMapper.delete(new LambdaQueryWrapper<BiMetricDO>()
                .eq(BiMetricDO::getTenantId, row.getTenantId())
                .eq(BiMetricDO::getDatasetId, row.getId()));
        for (var metric : dataset.metrics()) {
            metricMapper.insert(converter.toMetricRow(row.getTenantId(), row.getWorkspaceId(), row.getId(), metric));
        }
    }

    private void replaceDashboardWidgets(BiDashboardDO row, BiDashboard dashboard) {
        if (row.getId() == null) {
            return;
        }
        dashboardWidgetMapper.deleteByDashboard(row.getTenantId(), row.getId());
        for (String chartKey : dashboard.chartKeys()) {
            BiChart chart = findChartByKey(row.getTenantId(), row.getWorkspaceId(), BiResourceKey.of(chartKey, "chartKey"));
            dashboardWidgetMapper.insert(converter.toDashboardWidgetRow(
                    row.getTenantId(),
                    row.getId(),
                    chartKey,
                    chart == null ? null : chart.id()));
        }
    }

    private String chartKeyFromWidget(BiDashboardWidgetDO widget) {
        if (widget.getChartId() != null) {
            BiChart chart = toChart(chartMapper.selectById(widget.getChartId()));
            if (chart != null) {
                return chart.chartKey().value();
            }
        }
        return widget.getWidgetKey();
    }
}
