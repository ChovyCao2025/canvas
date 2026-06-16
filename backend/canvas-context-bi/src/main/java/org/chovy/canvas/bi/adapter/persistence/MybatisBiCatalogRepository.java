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
/**
 * MybatisBiCatalogRepository 仓储接口。
 */
@Repository
public class MybatisBiCatalogRepository implements BiWorkspaceRepository, BiDatasetRepository, BiChartRepository,
        BiDashboardRepository, BiPermissionRepository {
    /**
     * DEFAULT_WORKSPACE_KEY 对应的业务键。
     */
    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";

    /**
     * workspaceMapper 字段值。
     */
    private final BiWorkspaceMapper workspaceMapper;

    /**
     * datasetMapper 字段值。
     */
    private final BiDatasetMapper datasetMapper;

    /**
     * fieldMapper 字段值。
     */
    private final BiDatasetFieldMapper fieldMapper;

    /**
     * metricMapper 字段值。
     */
    private final BiMetricMapper metricMapper;

    /**
     * chartMapper 字段值。
     */
    private final BiChartMapper chartMapper;

    /**
     * dashboardMapper 字段值。
     */
    private final BiDashboardMapper dashboardMapper;

    /**
     * dashboardWidgetMapper 字段值。
     */
    private final BiDashboardWidgetMapper dashboardWidgetMapper;

    /**
     * permissionMapper 字段值。
     */
    private final BiResourcePermissionMapper permissionMapper;

    /**
     * converter 字段值。
     */
    private final BiPersistenceConverter converter;

    /**
     * 执行 Mybatis Bi Catalog Repository 相关处理。
     */
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
    /**
     * 执行 find Workspace 相关处理。
     */
    @Override
    public BiWorkspace findWorkspace(Long tenantId, Long workspaceId) {
        BiWorkspaceDO row = workspaceMapper.selectById(workspaceId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return converter.toWorkspace(row);
    }
    /**
     * 执行 save Workspace 相关处理。
     */
    @Override
    public BiWorkspace saveWorkspace(BiWorkspace workspace) {
        BiWorkspaceDO row = converter.toWorkspaceRow(workspace);
        // 依赖数据库自增主键区分新增和更新，保持领域对象的 upsert 语义。
        if (row.getId() == null) {
            workspaceMapper.insert(row);
        } else {
            workspaceMapper.updateById(row);
        }
        return converter.toWorkspace(row);
    }
    /**
     * 执行 find Dataset By Key 相关处理。
     */
    @Override
    public BiDataset findDatasetByKey(Long tenantId, Long workspaceId, BiResourceKey datasetKey) {
        BiDatasetDO row = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetDO::getDatasetKey, datasetKey.value())
                .last("LIMIT 1"));
        return toDataset(row);
    }
    /**
     * 执行 find Dataset By Id 相关处理。
     */
    @Override
    public BiDataset findDatasetById(Long tenantId, Long datasetId) {
        BiDatasetDO row = datasetMapper.selectById(datasetId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return toDataset(row);
    }
    /**
     * 查询列表数据。
     */
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
    /**
     * 执行 find Available Dataset By Key With Tenant Fallback 相关处理。
     */
    @Override
    public BiDataset findAvailableDatasetByKeyWithTenantFallback(Long tenantId, BiResourceKey datasetKey) {
        BiDataset dataset = findAvailableDatasetByKey(tenantId, datasetKey);
        if (dataset != null || Long.valueOf(0L).equals(tenantId)) {
            return dataset;
        }
        return findAvailableDatasetByKey(0L, datasetKey);
    }
    /**
     * 执行 find Available Dataset By Key 相关处理。
     */
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
    /**
     * 执行 save Dataset 相关处理。
     */
    @Override
    public BiDataset saveDataset(BiDataset dataset) {
        BiDatasetDO row = converter.toDatasetRow(dataset);
        // 先持久化数据集主表，确保字段和指标子表能引用最新的数据集 id。
        if (row.getId() == null) {
            datasetMapper.insert(row);
        } else {
            datasetMapper.updateById(row);
        }
        replaceDatasetChildren(row, dataset);
        return toDataset(row);
    }
    /**
     * 执行 find Chart By Key 相关处理。
     */
    @Override
    public BiChart findChartByKey(Long tenantId, Long workspaceId, BiResourceKey chartKey) {
        BiChartDO row = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                .eq(BiChartDO::getTenantId, tenantId)
                .eq(BiChartDO::getWorkspaceId, workspaceId)
                .eq(BiChartDO::getChartKey, chartKey.value())
                .last("LIMIT 1"));
        return toChart(row);
    }
    /**
     * 查询列表数据。
     */
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
    /**
     * 查询列表数据。
     */
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
    /**
     * 执行 find Available Chart By Key 相关处理。
     */
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
    /**
     * 执行 save Chart 相关处理。
     */
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
    /**
     * 执行 find Dashboard By Key 相关处理。
     */
    @Override
    public BiDashboard findDashboardByKey(Long tenantId, Long workspaceId, BiResourceKey dashboardKey) {
        BiDashboardDO row = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                .eq(BiDashboardDO::getTenantId, tenantId)
                .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardDO::getDashboardKey, dashboardKey.value())
                .last("LIMIT 1"));
        return toDashboard(row);
    }
    /**
     * 查询列表数据。
     */
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
    /**
     * 执行 find Available Dashboard By Key 相关处理。
     */
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
    /**
     * 执行 save Dashboard 相关处理。
     */
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
    /**
     * 执行 save Grant 相关处理。
     */
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
    /**
     * 删除业务数据。
     */
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
    /**
     * 查询列表数据。
     */
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
    /**
     * 生成默认值。
     */
    private Long defaultWorkspaceId(Long tenantId) {
        // 同一次查询按租户倒序取值，优先租户工作空间，缺失时回退公共租户。
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, DEFAULT_WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1"));
        return workspace == null || workspace.getId() == null ? 0L : workspace.getId();
    }
    /**
     * 转换为目标数据结构。
     */
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
    /**
     * 转换为目标数据结构。
     */
    private BiChart toChart(BiChartDO row) {
        if (row == null) {
            return null;
        }
        BiDataset dataset = findDatasetById(row.getTenantId(), row.getDatasetId());
        return converter.toChart(row, dataset);
    }
    /**
     * 转换为目标数据结构。
     */
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
    /**
     * 执行 replace Dataset Children 相关处理。
     */
    private void replaceDatasetChildren(BiDatasetDO row, BiDataset dataset) {
        if (row.getId() == null) {
            return;
        }
        // 子表采用全量替换，避免字段或指标被删除后仍残留旧配置。
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
    /**
     * 执行 replace Dashboard Widgets 相关处理。
     */
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
    /**
     * 执行 chart Key From Widget 相关处理。
     */
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
