package org.chovy.canvas.bi.adapter.persistence;

import org.chovy.canvas.bi.domain.BiChart;
import org.chovy.canvas.bi.domain.BiDashboard;
import org.chovy.canvas.bi.domain.BiDataset;
import org.chovy.canvas.bi.domain.BiDatasetField;
import org.chovy.canvas.bi.domain.BiMetric;
import org.chovy.canvas.bi.domain.BiPermissionGrant;
import org.chovy.canvas.bi.domain.BiResourceKey;
import org.chovy.canvas.bi.domain.BiResourceStatus;
import org.chovy.canvas.bi.domain.BiWorkspace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BiPersistenceConverter {

    public BiWorkspaceDO toWorkspaceRow(BiWorkspace workspace) {
        if (workspace == null) {
            return null;
        }
        BiWorkspaceDO row = new BiWorkspaceDO();
        row.setId(workspace.id());
        row.setTenantId(workspace.tenantId());
        row.setWorkspaceKey(workspace.workspaceKey().value());
        row.setName(workspace.name());
        row.setDescription(workspace.description());
        row.setStatus(workspace.status().name());
        row.setCreatedBy(workspace.createdBy());
        row.setCreatedAt(workspace.createdAt());
        row.setUpdatedAt(workspace.updatedAt());
        return row;
    }

    public BiWorkspace toWorkspace(BiWorkspaceDO row) {
        if (row == null) {
            return null;
        }
        return new BiWorkspace(
                row.getId(),
                row.getTenantId(),
                BiResourceKey.of(row.getWorkspaceKey(), "workspaceKey"),
                row.getName(),
                row.getDescription(),
                BiResourceStatus.from(row.getStatus()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    public BiDatasetDO toDatasetRow(BiDataset dataset) {
        if (dataset == null) {
            return null;
        }
        BiDatasetDO row = new BiDatasetDO();
        row.setId(dataset.id());
        row.setTenantId(dataset.tenantId());
        row.setWorkspaceId(dataset.workspaceId());
        row.setDatasetKey(dataset.datasetKey().value());
        row.setName(dataset.name());
        row.setDatasetType(dataset.datasetType());
        row.setSourceRefId(dataset.sourceRefId());
        row.setTableExpression(dataset.tableExpression());
        row.setTenantColumn(dataset.tenantColumn());
        row.setModelJson(SimpleBiJsonCodec.toJson(dataset.model()));
        row.setStatus(dataset.status().name());
        row.setCreatedBy(dataset.createdBy());
        row.setCreatedAt(dataset.createdAt());
        row.setUpdatedAt(dataset.updatedAt());
        return row;
    }

    public BiDataset toDataset(BiDatasetDO row, List<BiDatasetFieldDO> fieldRows, List<BiMetricDO> metricRows) {
        if (row == null) {
            return null;
        }
        return new BiDataset(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                BiResourceKey.of(row.getDatasetKey(), "datasetKey"),
                row.getName(),
                row.getDatasetType(),
                row.getSourceRefId(),
                row.getTableExpression(),
                row.getTenantColumn(),
                SimpleBiJsonCodec.fromJsonObject(row.getModelJson()),
                safe(fieldRows).stream().map(this::toDatasetField).toList(),
                safe(metricRows).stream().map(this::toMetric).toList(),
                BiResourceStatus.from(row.getStatus()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    public BiDatasetFieldDO toDatasetFieldRow(Long tenantId, Long datasetId, BiDatasetField field) {
        BiDatasetFieldDO row = new BiDatasetFieldDO();
        row.setTenantId(tenantId);
        row.setDatasetId(datasetId);
        row.setFieldKey(field.fieldKey().value());
        row.setDisplayName(field.displayName());
        row.setColumnExpression(field.columnExpression());
        row.setRoleKey(field.roleKey());
        row.setDataType(field.dataType());
        row.setDefaultAggregation(field.defaultAggregation());
        row.setVisible(field.visible());
        row.setSortOrder(field.sortOrder());
        return row;
    }

    public BiDatasetField toDatasetField(BiDatasetFieldDO row) {
        return new BiDatasetField(
                BiResourceKey.of(row.getFieldKey(), "fieldKey"),
                row.getDisplayName(),
                row.getColumnExpression(),
                row.getRoleKey(),
                row.getDataType(),
                row.getDefaultAggregation(),
                row.getVisible() == null || row.getVisible(),
                row.getSortOrder() == null ? 0 : row.getSortOrder());
    }

    public BiMetricDO toMetricRow(Long tenantId, Long workspaceId, Long datasetId, BiMetric metric) {
        BiMetricDO row = new BiMetricDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetId(datasetId);
        row.setMetricKey(metric.metricKey().value());
        row.setDisplayName(metric.displayName());
        row.setExpression(metric.expression());
        row.setAggregation(metric.aggregation());
        row.setDataType(metric.dataType());
        row.setUnit(metric.unit());
        row.setStatus(BiResourceStatus.PUBLISHED.name());
        return row;
    }

    public BiMetric toMetric(BiMetricDO row) {
        return new BiMetric(
                BiResourceKey.of(row.getMetricKey(), "metricKey"),
                row.getDisplayName(),
                row.getExpression(),
                row.getAggregation(),
                row.getDataType(),
                row.getUnit());
    }

    public BiChartDO toChartRow(BiChart chart) {
        if (chart == null) {
            return null;
        }
        BiChartDO row = new BiChartDO();
        row.setId(chart.id());
        row.setTenantId(chart.tenantId());
        row.setWorkspaceId(chart.workspaceId());
        row.setChartKey(chart.chartKey().value());
        row.setName(chart.name());
        row.setChartType(chart.chartType());
        row.setDatasetId(chart.datasetId());
        row.setQueryJson(SimpleBiJsonCodec.toJson(chart.query()));
        row.setStyleJson(SimpleBiJsonCodec.toJson(chart.style()));
        row.setInteractionJson(SimpleBiJsonCodec.toJson(chart.interaction()));
        row.setStatus(chart.status().name());
        row.setCreatedBy(chart.createdBy());
        row.setCreatedAt(chart.createdAt());
        row.setUpdatedAt(chart.updatedAt());
        return row;
    }

    public BiChart toChart(BiChartDO row, BiDataset dataset) {
        if (row == null || dataset == null) {
            return null;
        }
        return new BiChart(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                BiResourceKey.of(row.getChartKey(), "chartKey"),
                row.getName(),
                row.getChartType(),
                row.getDatasetId(),
                dataset.datasetKey(),
                SimpleBiJsonCodec.fromJsonObject(row.getQueryJson()),
                SimpleBiJsonCodec.fromJsonObject(row.getStyleJson()),
                SimpleBiJsonCodec.fromJsonObject(row.getInteractionJson()),
                BiResourceStatus.from(row.getStatus()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    public BiDashboardDO toDashboardRow(BiDashboard dashboard) {
        if (dashboard == null) {
            return null;
        }
        BiDashboardDO row = new BiDashboardDO();
        row.setId(dashboard.id());
        row.setTenantId(dashboard.tenantId());
        row.setWorkspaceId(dashboard.workspaceId());
        row.setDashboardKey(dashboard.dashboardKey().value());
        row.setName(dashboard.name());
        row.setDescription(dashboard.description());
        row.setThemeJson(SimpleBiJsonCodec.toJson(dashboard.theme()));
        row.setFilterJson(SimpleBiJsonCodec.toJson(dashboard.filters()));
        row.setStatus(dashboard.status().name());
        row.setVersion(dashboard.version());
        row.setCreatedBy(dashboard.createdBy());
        row.setCreatedAt(dashboard.createdAt());
        row.setUpdatedAt(dashboard.updatedAt());
        return row;
    }

    public BiDashboard toDashboard(BiDashboardDO row) {
        return toDashboard(row, List.of());
    }

    public BiDashboard toDashboard(BiDashboardDO row, List<String> chartKeys) {
        if (row == null) {
            return null;
        }
        return new BiDashboard(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                BiResourceKey.of(row.getDashboardKey(), "dashboardKey"),
                row.getName(),
                row.getDescription(),
                SimpleBiJsonCodec.fromJsonObject(row.getThemeJson()),
                SimpleBiJsonCodec.fromJsonObject(row.getFilterJson()),
                chartKeys,
                BiResourceStatus.from(row.getStatus()),
                row.getVersion() == null ? 1 : row.getVersion(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    public BiDashboardWidgetDO toDashboardWidgetRow(Long tenantId, Long dashboardId, String chartKey, Long chartId) {
        BiDashboardWidgetDO row = new BiDashboardWidgetDO();
        row.setTenantId(tenantId);
        row.setDashboardId(dashboardId);
        row.setWidgetKey(BiResourceKey.of(chartKey, "chartKey").value());
        row.setChartId(chartId);
        row.setWidgetType("CHART");
        row.setTitle(row.getWidgetKey());
        row.setLayoutJson("{}");
        row.setQueryOverrideJson("{}");
        row.setInteractionJson("{}");
        return row;
    }

    public BiResourcePermissionDO toPermissionRow(BiPermissionGrant grant) {
        if (grant == null) {
            return null;
        }
        BiResourcePermissionDO row = new BiResourcePermissionDO();
        row.setId(grant.id());
        row.setTenantId(grant.tenantId());
        row.setWorkspaceId(grant.workspaceId());
        row.setResourceType(grant.resourceType());
        row.setResourceId(grant.resourceId());
        row.setSubjectType(grant.subjectType());
        row.setSubjectId(grant.subjectId());
        row.setActionKey(grant.actionKey());
        row.setEffect(grant.effect());
        row.setCreatedBy(grant.createdBy());
        row.setCreatedAt(grant.createdAt());
        return row;
    }

    public BiPermissionGrant toPermissionGrant(BiResourcePermissionDO row) {
        if (row == null) {
            return null;
        }
        return new BiPermissionGrant(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceId(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getActionKey(),
                row.getEffect(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    static final class SimpleBiJsonCodec {
        private SimpleBiJsonCodec() {
        }

        static String toJson(Map<String, Object> value) {
            if (value == null || value.isEmpty()) {
                return "{}";
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(quote(entry.getKey())).append(':').append(valueToJson(entry.getValue()));
            }
            return builder.append('}').toString();
        }

        static Map<String, Object> fromJsonObject(String json) {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            String trimmed = json.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return Map.of();
            }
            String body = trimmed.substring(1, trimmed.length() - 1).trim();
            if (body.isEmpty()) {
                return Map.of();
            }
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            for (String pair : body.split(",")) {
                int separator = pair.indexOf(':');
                if (separator <= 0) {
                    continue;
                }
                String key = unquote(pair.substring(0, separator).trim());
                String value = unquote(pair.substring(separator + 1).trim());
                result.put(key, value);
            }
            return Map.copyOf(result);
        }

        private static String valueToJson(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof Iterable<?> iterable) {
                StringBuilder builder = new StringBuilder("[");
                boolean first = true;
                for (Object item : iterable) {
                    if (!first) {
                        builder.append(',');
                    }
                    first = false;
                    builder.append(valueToJson(item));
                }
                return builder.append(']').toString();
            }
            return quote(String.valueOf(value));
        }

        private static String quote(String value) {
            return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        private static String unquote(String value) {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
                return trimmed.substring(1, trimmed.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
            return "null".equals(trimmed) ? null : trimmed;
        }
    }
}
