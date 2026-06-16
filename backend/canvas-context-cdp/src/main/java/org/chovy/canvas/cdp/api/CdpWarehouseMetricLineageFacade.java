package org.chovy.canvas.cdp.api;

import java.util.List;

/**
 * 定义 CdpWarehouseMetricLineageFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseMetricLineageFacade {

    /**
     * 执行 impact 对应的 CDP 业务操作。
     */
    MetricImpactView impact(Long tenantId, String datasetKey, String metricKey);

    /**
     * 表示 MetricImpactView 的业务数据或处理组件。
     */
    final class MetricImpactView {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * dataset Key。
         */
        private final String datasetKey;

        /**
         * metric Key。
         */
        private final String metricKey;

        /**
         * expression。
         */
        private final String expression;

        /**
         * 值类型。
         */
        private final String valueType;

        /**
         * allowed Dimensions。
         */
        private final List<String> allowedDimensions;

        /**
         * field Dependencies。
         */
        private final List<FieldDependencyView> fieldDependencies;

        /**
         * lineage Nodes。
         */
        private final List<Object> lineageNodes;

        /**
         * lineage Edges。
         */
        private final List<Object> lineageEdges;

        /**
         * transitive Lineage。
         */
        private final Object transitiveLineage;

        /**
         * charts。
         */
        private final List<ChartImpactView> charts;

        /**
         * dashboards。
         */
        private final List<DashboardImpactView> dashboards;

        /**
         * warnings。
         */
        private final List<String> warnings;

        /**
         * 使用记录字段创建 MetricImpactView。
         */
        public MetricImpactView(
                Long tenantId,
                String datasetKey,
                String metricKey,
                String expression,
                String valueType,
                List<String> allowedDimensions,
                List<FieldDependencyView> fieldDependencies,
                List<Object> lineageNodes,
                List<Object> lineageEdges,
                Object transitiveLineage,
                List<ChartImpactView> charts,
                List<DashboardImpactView> dashboards,
                List<String> warnings) {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
            fieldDependencies = fieldDependencies == null ? List.of() : List.copyOf(fieldDependencies);
            lineageNodes = lineageNodes == null ? List.of() : List.copyOf(lineageNodes);
            lineageEdges = lineageEdges == null ? List.of() : List.copyOf(lineageEdges);
            charts = charts == null ? List.of() : List.copyOf(charts);
            dashboards = dashboards == null ? List.of() : List.copyOf(dashboards);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            this.tenantId = tenantId;
            this.datasetKey = datasetKey;
            this.metricKey = metricKey;
            this.expression = expression;
            this.valueType = valueType;
            this.allowedDimensions = allowedDimensions;
            this.fieldDependencies = fieldDependencies;
            this.lineageNodes = lineageNodes;
            this.lineageEdges = lineageEdges;
            this.transitiveLineage = transitiveLineage;
            this.charts = charts;
            this.dashboards = dashboards;
            this.warnings = warnings;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回dataset Key。
         */
        public String datasetKey() {
            return datasetKey;
        }

        /**
         * 返回metric Key。
         */
        public String metricKey() {
            return metricKey;
        }

        /**
         * 返回expression。
         */
        public String expression() {
            return expression;
        }

        /**
         * 返回值类型。
         */
        public String valueType() {
            return valueType;
        }

        /**
         * 返回allowed Dimensions。
         */
        public List<String> allowedDimensions() {
            return allowedDimensions;
        }

        /**
         * 返回field Dependencies。
         */
        public List<FieldDependencyView> fieldDependencies() {
            return fieldDependencies;
        }

        /**
         * 返回lineage Nodes。
         */
        public List<Object> lineageNodes() {
            return lineageNodes;
        }

        /**
         * 返回lineage Edges。
         */
        public List<Object> lineageEdges() {
            return lineageEdges;
        }

        /**
         * 返回transitive Lineage。
         */
        public Object transitiveLineage() {
            return transitiveLineage;
        }

        /**
         * 返回charts。
         */
        public List<ChartImpactView> charts() {
            return charts;
        }

        /**
         * 返回dashboards。
         */
        public List<DashboardImpactView> dashboards() {
            return dashboards;
        }

        /**
         * 返回warnings。
         */
        public List<String> warnings() {
            return warnings;
        }

        /**
         * 按所有字段比较 MetricImpactView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MetricImpactView that = (MetricImpactView) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(datasetKey, that.datasetKey)
                    && java.util.Objects.equals(metricKey, that.metricKey)
                    && java.util.Objects.equals(expression, that.expression)
                    && java.util.Objects.equals(valueType, that.valueType)
                    && java.util.Objects.equals(allowedDimensions, that.allowedDimensions)
                    && java.util.Objects.equals(fieldDependencies, that.fieldDependencies)
                    && java.util.Objects.equals(lineageNodes, that.lineageNodes)
                    && java.util.Objects.equals(lineageEdges, that.lineageEdges)
                    && java.util.Objects.equals(transitiveLineage, that.transitiveLineage)
                    && java.util.Objects.equals(charts, that.charts)
                    && java.util.Objects.equals(dashboards, that.dashboards)
                    && java.util.Objects.equals(warnings, that.warnings);
        }

        /**
         * 根据所有字段计算 MetricImpactView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, datasetKey, metricKey, expression, valueType, allowedDimensions, fieldDependencies, lineageNodes, lineageEdges, transitiveLineage, charts, dashboards, warnings);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "MetricImpactView[" + "tenantId=" + tenantId + ", datasetKey=" + datasetKey + ", metricKey=" + metricKey + ", expression=" + expression + ", valueType=" + valueType + ", allowedDimensions=" + allowedDimensions + ", fieldDependencies=" + fieldDependencies + ", lineageNodes=" + lineageNodes + ", lineageEdges=" + lineageEdges + ", transitiveLineage=" + transitiveLineage + ", charts=" + charts + ", dashboards=" + dashboards + ", warnings=" + warnings + "]";
        }
    }

    /**
     * 表示 FieldDependencyView 的业务数据或处理组件。
     */
    final class FieldDependencyView {

        /**
         * field Key。
         */
        private final String fieldKey;

        /**
         * dependency Type。
         */
        private final String dependencyType;

        /**
         * 使用记录字段创建 FieldDependencyView。
         */
        public FieldDependencyView(
                String fieldKey,
                String dependencyType) {
            this.fieldKey = fieldKey;
            this.dependencyType = dependencyType;
        }

        /**
         * 返回field Key。
         */
        public String fieldKey() {
            return fieldKey;
        }

        /**
         * 返回dependency Type。
         */
        public String dependencyType() {
            return dependencyType;
        }

        /**
         * 按所有字段比较 FieldDependencyView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FieldDependencyView that = (FieldDependencyView) o;
            return java.util.Objects.equals(fieldKey, that.fieldKey)
                    && java.util.Objects.equals(dependencyType, that.dependencyType);
        }

        /**
         * 根据所有字段计算 FieldDependencyView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(fieldKey, dependencyType);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "FieldDependencyView[" + "fieldKey=" + fieldKey + ", dependencyType=" + dependencyType + "]";
        }
    }

    /**
     * 表示 ChartImpactView 的业务数据或处理组件。
     */
    final class ChartImpactView {

        /**
         * chart Key。
         */
        private final String chartKey;

        /**
         * 名称。
         */
        private final String name;

        /**
         * chart Type。
         */
        private final String chartType;

        /**
         * 状态。
         */
        private final String status;

        /**
         * dimensions。
         */
        private final List<String> dimensions;

        /**
         * 使用记录字段创建 ChartImpactView。
         */
        public ChartImpactView(
                String chartKey,
                String name,
                String chartType,
                String status,
                List<String> dimensions) {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            this.chartKey = chartKey;
            this.name = name;
            this.chartType = chartType;
            this.status = status;
            this.dimensions = dimensions;
        }

        /**
         * 返回chart Key。
         */
        public String chartKey() {
            return chartKey;
        }

        /**
         * 返回名称。
         */
        public String name() {
            return name;
        }

        /**
         * 返回chart Type。
         */
        public String chartType() {
            return chartType;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回dimensions。
         */
        public List<String> dimensions() {
            return dimensions;
        }

        /**
         * 按所有字段比较 ChartImpactView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ChartImpactView that = (ChartImpactView) o;
            return java.util.Objects.equals(chartKey, that.chartKey)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(chartType, that.chartType)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(dimensions, that.dimensions);
        }

        /**
         * 根据所有字段计算 ChartImpactView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(chartKey, name, chartType, status, dimensions);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "ChartImpactView[" + "chartKey=" + chartKey + ", name=" + name + ", chartType=" + chartType + ", status=" + status + ", dimensions=" + dimensions + "]";
        }
    }

    /**
     * 表示 DashboardImpactView 的业务数据或处理组件。
     */
    final class DashboardImpactView {

        /**
         * dashboard Key。
         */
        private final String dashboardKey;

        /**
         * title。
         */
        private final String title;

        /**
         * 状态。
         */
        private final String status;

        /**
         * version。
         */
        private final int version;

        /**
         * widget Keys。
         */
        private final List<String> widgetKeys;

        /**
         * 使用记录字段创建 DashboardImpactView。
         */
        public DashboardImpactView(
                String dashboardKey,
                String title,
                String status,
                int version,
                List<String> widgetKeys) {
            widgetKeys = widgetKeys == null ? List.of() : List.copyOf(widgetKeys);
            this.dashboardKey = dashboardKey;
            this.title = title;
            this.status = status;
            this.version = version;
            this.widgetKeys = widgetKeys;
        }

        /**
         * 返回dashboard Key。
         */
        public String dashboardKey() {
            return dashboardKey;
        }

        /**
         * 返回title。
         */
        public String title() {
            return title;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回version。
         */
        public int version() {
            return version;
        }

        /**
         * 返回widget Keys。
         */
        public List<String> widgetKeys() {
            return widgetKeys;
        }

        /**
         * 按所有字段比较 DashboardImpactView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DashboardImpactView that = (DashboardImpactView) o;
            return java.util.Objects.equals(dashboardKey, that.dashboardKey)
                    && java.util.Objects.equals(title, that.title)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(version, that.version)
                    && java.util.Objects.equals(widgetKeys, that.widgetKeys);
        }

        /**
         * 根据所有字段计算 DashboardImpactView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(dashboardKey, title, status, version, widgetKeys);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "DashboardImpactView[" + "dashboardKey=" + dashboardKey + ", title=" + title + ", status=" + status + ", version=" + version + ", widgetKeys=" + widgetKeys + "]";
        }
    }
}
