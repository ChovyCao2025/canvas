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

public class CdpWarehouseCatalogCatalog {

    private final List<DatasetState> datasets = new ArrayList<>();
    private final List<LineageState> lineageEdges = new ArrayList<>();
    private long nextLineageId = 1L;

    public synchronized List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status) {
        return datasets.stream()
                .filter(dataset -> Objects.equals(dataset.tenantId, tenantId))
                .filter(dataset -> layer == null || layer.isBlank() || normalize(layer).equals(dataset.layer))
                .filter(dataset -> status == null || status.isBlank() || normalize(status).equals(dataset.status))
                .sorted(Comparator.comparing(dataset -> dataset.datasetKey))
                .map(DatasetState::toMap)
                .toList();
    }

    public synchronized Map<String, Object> upsertDataset(
            Long tenantId,
            CdpWarehouseCatalogFacade.DatasetCommand command) {
        DatasetState dataset = DatasetState.from(tenantId, command);
        datasets.removeIf(existing -> Objects.equals(existing.tenantId, tenantId)
                && Objects.equals(existing.datasetKey, dataset.datasetKey));
        datasets.add(dataset);
        return dataset.toMap();
    }

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

    public synchronized Map<String, Object> lineage(
            Long tenantId,
            String datasetKey,
            CdpWarehouseCatalogFacade.Direction direction) {
        String key = require(datasetKey, "datasetKey is required");
        List<LineageState> edges = directEdges(tenantId, key, direction);
        return graph(key, direction, edges, null, false, List.of());
    }

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

    private Map<String, Object> nodeMap(String datasetKey) {
        return datasets.stream()
                .filter(dataset -> Objects.equals(dataset.datasetKey, datasetKey))
                .findFirst()
                .map(DatasetState::toMap)
                .orElseGet(() -> Map.of("datasetKey", datasetKey));
    }

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

    private static String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalize(String value) {
        return defaultString(value, "").toUpperCase(Locale.ROOT);
    }

    private record PathState(String datasetKey, List<String> datasetKeys, int depth) {
    }

    private record DatasetState(
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
    }

    private record LineageState(
            Long id,
            Long tenantId,
            String upstreamDatasetKey,
            String downstreamDatasetKey,
            String transformType,
            String transformRef,
            String dependencyType,
            String description,
            boolean active) {
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
    }
}
