package org.chovy.canvas.cdp.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade;

/**
 * 维护 CdpWarehouseCatalog 的内存目录和查询视图。
 */
public class CdpWarehouseCatalogCatalog {

    private final List<DatasetState> datasets = new ArrayList<>();
    private final List<LineageState> lineageEdges = new ArrayList<>();

    /**
     * next Lineage Id。
     */
    private long nextLineageId = 1L;

    /**
     * 查询Datasets列表。
     */
    public synchronized List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status) {
        return datasets.stream()
                .filter(dataset -> Objects.equals(dataset.tenantId, tenantId))
                .filter(dataset -> layer == null || layer.isBlank() || normalize(layer).equals(dataset.layer))
                .filter(dataset -> status == null || status.isBlank() || normalize(status).equals(dataset.status))
                .sorted(Comparator.comparing(dataset -> dataset.datasetKey))
                .map(DatasetState::toMap)
                .toList();
    }

    /**
     * 执行 upsertDataset 对应的 CDP 业务操作。
     */
    public synchronized Map<String, Object> upsertDataset(
            Long tenantId,
            CdpWarehouseCatalogFacade.DatasetCommand command) {
        DatasetState dataset = DatasetState.from(tenantId, command);
        datasets.removeIf(existing -> Objects.equals(existing.tenantId, tenantId)
                && Objects.equals(existing.datasetKey, dataset.datasetKey));
        datasets.add(dataset);
        return dataset.toMap();
    }

    /**
     * 创建Lineage Edge。
     */
    public synchronized Map<String, Object> createLineageEdge(
            Long tenantId,
            CdpWarehouseCatalogFacade.LineageCommand command) {
        String upstream = require(command == null ? null : command.upstreamDatasetKey(), "upstreamDatasetKey is required");
        String downstream = require(command.downstreamDatasetKey(), "downstreamDatasetKey is required");
        if (upstream.equals(downstream)) {
            throw new IllegalArgumentException("upstreamDatasetKey and downstreamDatasetKey must differ");
        }
        LineageState edge = new LineageState(
                nextLineageId++,
                tenantId,
                upstream,
                downstream,
                defaultString(command.transformType(), "SQL").toUpperCase(Locale.ROOT),
                command.transformRef(),
                defaultString(command.dependencyType(), "HARD").toUpperCase(Locale.ROOT),
                command.description(),
                !Boolean.FALSE.equals(command.active()));
        lineageEdges.add(edge);
        return edge.toMap();
    }

    /**
     * 执行 lineage 对应的 CDP 业务操作。
     */
    public synchronized Map<String, Object> lineage(
            Long tenantId,
            String datasetKey,
            CdpWarehouseCatalogFacade.Direction direction) {
        String key = require(datasetKey, "datasetKey is required");
        List<LineageState> edges = directEdges(tenantId, key, direction);
        return graph(key, direction, edges, null, false, List.of());
    }

    /**
     * 执行 transitiveLineage 对应的 CDP 业务操作。
     */
    public synchronized Map<String, Object> transitiveLineage(
            Long tenantId,
            String datasetKey,
            CdpWarehouseCatalogFacade.Direction direction,
            Integer maxDepth) {
        String key = require(datasetKey, "datasetKey is required");
        int depth = maxDepth == null ? 3 : maxDepth;
        if (depth <= 0) {
            throw new IllegalArgumentException("maxDepth must be positive");
        }
        List<LineageState> edges = new ArrayList<>();
        List<Map<String, Object>> paths = new ArrayList<>();
        Queue<PathState> queue = new ArrayDeque<>();
        queue.add(new PathState(key, List.of(key), 0));
        Set<String> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            PathState current = queue.remove();
            if (current.depth >= depth || !visited.add(current.datasetKey + ":" + current.depth)) {
                continue;
            }
            for (LineageState edge : directEdges(tenantId, current.datasetKey, direction)) {
                if (!edges.contains(edge)) {
                    edges.add(edge);
                }
                String next = nextDataset(current.datasetKey, edge, direction);
                if (next == null || current.datasetKeys.contains(next)) {
                    continue;
                }
                List<String> nextPath = new ArrayList<>(current.datasetKeys);
                nextPath.add(next);
                paths.add(Map.of("datasetKeys", nextPath, "depth", current.depth + 1));
                queue.add(new PathState(next, nextPath, current.depth + 1));
            }
        }
        return graph(key, direction, edges, depth, false, paths);
    }

    /**
     * 执行 directEdges 对应的 CDP 业务操作。
     */
    private List<LineageState> directEdges(Long tenantId, String datasetKey, CdpWarehouseCatalogFacade.Direction direction) {
        return lineageEdges.stream()
                .filter(edge -> Objects.equals(edge.tenantId, tenantId))
                .filter(edge -> edge.active)
                .filter(edge -> switch (direction) {
                    case UPSTREAM -> Objects.equals(edge.downstreamDatasetKey, datasetKey);
                    case DOWNSTREAM -> Objects.equals(edge.upstreamDatasetKey, datasetKey);
                    case BOTH -> Objects.equals(edge.upstreamDatasetKey, datasetKey)
                            || Objects.equals(edge.downstreamDatasetKey, datasetKey);
                })
                .sorted(Comparator.comparing(edge -> edge.id))
                .toList();
    }

    /**
     * 执行 graph 对应的 CDP 业务操作。
     */
    private Map<String, Object> graph(
            String datasetKey,
            CdpWarehouseCatalogFacade.Direction direction,
            List<LineageState> edges,
            Integer maxDepth,
            boolean truncated,
            List<Map<String, Object>> paths) {
        Set<String> datasetKeys = new LinkedHashSet<>();
        datasetKeys.add(datasetKey);
        edges.forEach(edge -> {
            datasetKeys.add(edge.upstreamDatasetKey);
            datasetKeys.add(edge.downstreamDatasetKey);
        });
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("datasetKey", datasetKey);
        graph.put("direction", direction.name());
        graph.put("nodes", datasetKeys.stream().map(this::nodeMap).toList());
        graph.put("edges", edges.stream().map(LineageState::toMap).toList());
        if (maxDepth != null) {
            graph.put("maxDepth", maxDepth);
            graph.put("truncated", truncated);
            graph.put("paths", paths);
        }
        return graph;
    }

    /**
     * 执行 nodeMap 对应的 CDP 业务操作。
     */
    private Map<String, Object> nodeMap(String datasetKey) {
        return datasets.stream()
                .filter(dataset -> Objects.equals(dataset.datasetKey, datasetKey))
                .findFirst()
                .map(DatasetState::toMap)
                .orElseGet(() -> Map.of("datasetKey", datasetKey));
    }

    /**
     * 执行 nextDataset 对应的 CDP 业务操作。
     */
    private static String nextDataset(
            String current,
            LineageState edge,
            CdpWarehouseCatalogFacade.Direction direction) {
        if (direction == CdpWarehouseCatalogFacade.Direction.UPSTREAM) {
            return edge.upstreamDatasetKey;
        }
        if (direction == CdpWarehouseCatalogFacade.Direction.DOWNSTREAM) {
            return edge.downstreamDatasetKey;
        }
        if (Objects.equals(edge.upstreamDatasetKey, current)) {
            return edge.downstreamDatasetKey;
        }
        if (Objects.equals(edge.downstreamDatasetKey, current)) {
            return edge.upstreamDatasetKey;
        }
        return null;
    }

    /**
     * 读取并校验必填的require。
     */
    private static String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 返回默认的String。
     */
    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 归一化normalize。
     */
    private static String normalize(String value) {
        return defaultString(value, "").toUpperCase(Locale.ROOT);
    }

    /**
     * 表示 PathState 的业务数据或处理组件。
     */
    private static final class PathState {

        /**
         * dataset Key。
         */
        private final String datasetKey;

        /**
         * dataset Keys。
         */
        private final List<String> datasetKeys;

        /**
         * depth。
         */
        private final int depth;

        /**
         * 使用记录字段创建 PathState。
         */
        private PathState(
                String datasetKey,
                List<String> datasetKeys,
                int depth) {
            this.datasetKey = datasetKey;
            this.datasetKeys = datasetKeys;
            this.depth = depth;
        }

        /**
         * 返回dataset Key。
         */
        public String datasetKey() {
            return datasetKey;
        }

        /**
         * 返回dataset Keys。
         */
        public List<String> datasetKeys() {
            return datasetKeys;
        }

        /**
         * 返回depth。
         */
        public int depth() {
            return depth;
        }

        /**
         * 按所有字段比较 PathState。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PathState that = (PathState) o;
            return java.util.Objects.equals(datasetKey, that.datasetKey)
                    && java.util.Objects.equals(datasetKeys, that.datasetKeys)
                    && java.util.Objects.equals(depth, that.depth);
        }

        /**
         * 根据所有字段计算 PathState 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(datasetKey, datasetKeys, depth);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "PathState[" + "datasetKey=" + datasetKey + ", datasetKeys=" + datasetKeys + ", depth=" + depth + "]";
        }
    }

    /**
     * 表示 DatasetState 的业务数据或处理组件。
     */
    private static final class DatasetState {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * dataset Key。
         */
        private final String datasetKey;

        /**
         * layer。
         */
        private final String layer;

        /**
         * physical Name。
         */
        private final String physicalName;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * subject Area。
         */
        private final String subjectArea;

        /**
         * source System。
         */
        private final String sourceSystem;

        /**
         * owner Name。
         */
        private final String ownerName;

        /**
         * 描述。
         */
        private final String description;

        /**
         * freshness Sla Minutes。
         */
        private final Integer freshnessSlaMinutes;

        /**
         * pii Level。
         */
        private final String piiLevel;

        /**
         * 状态。
         */
        private final String status;

        /**
         * schema Json。
         */
        private final String schemaJson;

        /**
         * 使用记录字段创建 DatasetState。
         */
        private DatasetState(
                Long tenantId,
                String datasetKey,
                String layer,
                String physicalName,
                String displayName,
                String subjectArea,
                String sourceSystem,
                String ownerName,
                String description,
                Integer freshnessSlaMinutes,
                String piiLevel,
                String status,
                String schemaJson) {
            this.tenantId = tenantId;
            this.datasetKey = datasetKey;
            this.layer = layer;
            this.physicalName = physicalName;
            this.displayName = displayName;
            this.subjectArea = subjectArea;
            this.sourceSystem = sourceSystem;
            this.ownerName = ownerName;
            this.description = description;
            this.freshnessSlaMinutes = freshnessSlaMinutes;
            this.piiLevel = piiLevel;
            this.status = status;
            this.schemaJson = schemaJson;
        }

/**
 * 执行 from 对应的 CDP 业务操作。
 */
private static DatasetState from(Long tenantId, CdpWarehouseCatalogFacade.DatasetCommand command) {
            String datasetKey = require(command == null ? null : command.datasetKey(), "datasetKey is required");
            return new DatasetState(
                    tenantId,
                    datasetKey,
                    normalize(command.layer()),
                    defaultString(command.physicalName(), datasetKey),
                    defaultString(command.displayName(), datasetKey),
                    command.subjectArea(),
                    command.sourceSystem(),
                    command.ownerName(),
                    command.description(),
                    command.freshnessSlaMinutes(),
                    normalize(command.piiLevel()),
                    normalize(command.status()),
                    command.schemaJson());
        }

        /**
         * 转换为Map。
         */
        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("datasetKey", datasetKey);
            row.put("layer", layer);
            row.put("physicalName", physicalName);
            row.put("displayName", displayName);
            row.put("subjectArea", subjectArea);
            row.put("sourceSystem", sourceSystem);
            row.put("ownerName", ownerName);
            row.put("description", description);
            row.put("freshnessSlaMinutes", freshnessSlaMinutes);
            row.put("piiLevel", piiLevel);
            row.put("status", status);
            row.put("schemaJson", schemaJson);
            return row;
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
         * 返回layer。
         */
        public String layer() {
            return layer;
        }

        /**
         * 返回physical Name。
         */
        public String physicalName() {
            return physicalName;
        }

        /**
         * 返回展示名称。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回subject Area。
         */
        public String subjectArea() {
            return subjectArea;
        }

        /**
         * 返回source System。
         */
        public String sourceSystem() {
            return sourceSystem;
        }

        /**
         * 返回owner Name。
         */
        public String ownerName() {
            return ownerName;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 返回freshness Sla Minutes。
         */
        public Integer freshnessSlaMinutes() {
            return freshnessSlaMinutes;
        }

        /**
         * 返回pii Level。
         */
        public String piiLevel() {
            return piiLevel;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回schema Json。
         */
        public String schemaJson() {
            return schemaJson;
        }

        /**
         * 按所有字段比较 DatasetState。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DatasetState that = (DatasetState) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(datasetKey, that.datasetKey)
                    && java.util.Objects.equals(layer, that.layer)
                    && java.util.Objects.equals(physicalName, that.physicalName)
                    && java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(subjectArea, that.subjectArea)
                    && java.util.Objects.equals(sourceSystem, that.sourceSystem)
                    && java.util.Objects.equals(ownerName, that.ownerName)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(freshnessSlaMinutes, that.freshnessSlaMinutes)
                    && java.util.Objects.equals(piiLevel, that.piiLevel)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(schemaJson, that.schemaJson);
        }

        /**
         * 根据所有字段计算 DatasetState 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, datasetKey, layer, physicalName, displayName, subjectArea, sourceSystem, ownerName, description, freshnessSlaMinutes, piiLevel, status, schemaJson);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "DatasetState[" + "tenantId=" + tenantId + ", datasetKey=" + datasetKey + ", layer=" + layer + ", physicalName=" + physicalName + ", displayName=" + displayName + ", subjectArea=" + subjectArea + ", sourceSystem=" + sourceSystem + ", ownerName=" + ownerName + ", description=" + description + ", freshnessSlaMinutes=" + freshnessSlaMinutes + ", piiLevel=" + piiLevel + ", status=" + status + ", schemaJson=" + schemaJson + "]";
        }
    }

    /**
     * 表示 LineageState 的业务数据或处理组件。
     */
    private static final class LineageState {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * upstream Dataset Key。
         */
        private final String upstreamDatasetKey;

        /**
         * downstream Dataset Key。
         */
        private final String downstreamDatasetKey;

        /**
         * transform Type。
         */
        private final String transformType;

        /**
         * transform Ref。
         */
        private final String transformRef;

        /**
         * dependency Type。
         */
        private final String dependencyType;

        /**
         * 描述。
         */
        private final String description;

        /**
         * active。
         */
        private final boolean active;

        /**
         * 使用记录字段创建 LineageState。
         */
        private LineageState(
                Long id,
                Long tenantId,
                String upstreamDatasetKey,
                String downstreamDatasetKey,
                String transformType,
                String transformRef,
                String dependencyType,
                String description,
                boolean active) {
            this.id = id;
            this.tenantId = tenantId;
            this.upstreamDatasetKey = upstreamDatasetKey;
            this.downstreamDatasetKey = downstreamDatasetKey;
            this.transformType = transformType;
            this.transformRef = transformRef;
            this.dependencyType = dependencyType;
            this.description = description;
            this.active = active;
        }

/**
 * 转换为Map。
 */
private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("tenantId", tenantId);
            row.put("upstreamDatasetKey", upstreamDatasetKey);
            row.put("downstreamDatasetKey", downstreamDatasetKey);
            row.put("transformType", transformType);
            row.put("transformRef", transformRef);
            row.put("dependencyType", dependencyType);
            row.put("description", description);
            row.put("active", active);
            return row;
        }

        /**
         * 返回唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回upstream Dataset Key。
         */
        public String upstreamDatasetKey() {
            return upstreamDatasetKey;
        }

        /**
         * 返回downstream Dataset Key。
         */
        public String downstreamDatasetKey() {
            return downstreamDatasetKey;
        }

        /**
         * 返回transform Type。
         */
        public String transformType() {
            return transformType;
        }

        /**
         * 返回transform Ref。
         */
        public String transformRef() {
            return transformRef;
        }

        /**
         * 返回dependency Type。
         */
        public String dependencyType() {
            return dependencyType;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 返回active。
         */
        public boolean active() {
            return active;
        }

        /**
         * 按所有字段比较 LineageState。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LineageState that = (LineageState) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(upstreamDatasetKey, that.upstreamDatasetKey)
                    && java.util.Objects.equals(downstreamDatasetKey, that.downstreamDatasetKey)
                    && java.util.Objects.equals(transformType, that.transformType)
                    && java.util.Objects.equals(transformRef, that.transformRef)
                    && java.util.Objects.equals(dependencyType, that.dependencyType)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(active, that.active);
        }

        /**
         * 根据所有字段计算 LineageState 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, upstreamDatasetKey, downstreamDatasetKey, transformType, transformRef, dependencyType, description, active);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "LineageState[" + "id=" + id + ", tenantId=" + tenantId + ", upstreamDatasetKey=" + upstreamDatasetKey + ", downstreamDatasetKey=" + downstreamDatasetKey + ", transformType=" + transformType + ", transformRef=" + transformRef + ", dependencyType=" + dependencyType + ", description=" + description + ", active=" + active + "]";
        }
    }
}
