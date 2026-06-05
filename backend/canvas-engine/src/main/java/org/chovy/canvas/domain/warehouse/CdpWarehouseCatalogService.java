package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseDatasetCatalogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseLineageEdgeDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseDatasetCatalogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseLineageEdgeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CdpWarehouseCatalogService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String PII_NORMAL = "NORMAL";
    private static final String DEPENDENCY_HARD = "HARD";
    private static final String TRANSFORM_MANUAL = "MANUAL";
    private static final int DEFAULT_TRANSITIVE_DEPTH = 3;
    private static final int MAX_TRANSITIVE_DEPTH = 8;
    private static final int MAX_TRANSITIVE_PATHS = 500;

    private final CdpWarehouseDatasetCatalogMapper datasetMapper;
    private final CdpWarehouseLineageEdgeMapper lineageMapper;

    public CdpWarehouseCatalogService(CdpWarehouseDatasetCatalogMapper datasetMapper,
                                      CdpWarehouseLineageEdgeMapper lineageMapper) {
        this.datasetMapper = datasetMapper;
        this.lineageMapper = lineageMapper;
    }

    public DatasetView upsertDataset(Long tenantId, DatasetCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("dataset command is required");
        }
        CdpWarehouseDatasetCatalogDO row = new CdpWarehouseDatasetCatalogDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setDatasetKey(required(command.datasetKey(), "datasetKey"));
        row.setLayer(upperRequired(command.layer(), "layer"));
        row.setPhysicalName(required(command.physicalName(), "physicalName"));
        row.setDisplayName(defaultString(command.displayName(), row.getDatasetKey()));
        row.setSubjectArea(blankToNull(command.subjectArea()));
        row.setSourceSystem(blankToNull(command.sourceSystem()));
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setDescription(blankToNull(command.description()));
        row.setFreshnessSlaMinutes(command.freshnessSlaMinutes());
        row.setPiiLevel(upperDefault(command.piiLevel(), PII_NORMAL));
        row.setStatus(upperDefault(command.status(), STATUS_ACTIVE));
        row.setSchemaJson(blankToNull(command.schemaJson()));
        datasetMapper.upsert(row);
        return toDataset(row);
    }

    public LineageEdgeView createLineageEdge(Long tenantId, LineageCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("lineage command is required");
        }
        String upstream = required(command.upstreamDatasetKey(), "upstreamDatasetKey");
        String downstream = required(command.downstreamDatasetKey(), "downstreamDatasetKey");
        if (upstream.equals(downstream)) {
            throw new IllegalArgumentException("upstream and downstream datasets must differ");
        }
        CdpWarehouseLineageEdgeDO row = new CdpWarehouseLineageEdgeDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setUpstreamDatasetKey(upstream);
        row.setDownstreamDatasetKey(downstream);
        row.setTransformType(upperDefault(command.transformType(), TRANSFORM_MANUAL));
        row.setTransformRef(defaultString(command.transformRef(), ""));
        row.setDependencyType(upperDefault(command.dependencyType(), DEPENDENCY_HARD));
        row.setDescription(blankToNull(command.description()));
        row.setActive(command.active() == null ? Boolean.TRUE : command.active());
        lineageMapper.upsert(row);
        return toEdge(row);
    }

    public List<DatasetView> listDatasets(Long tenantId, String layer, String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseDatasetCatalogDO> query = new LambdaQueryWrapper<CdpWarehouseDatasetCatalogDO>()
                .in(CdpWarehouseDatasetCatalogDO::getTenantId, tenantScope(scopedTenantId))
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getTenantId)
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getLayer)
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getDatasetKey);
        if (hasText(layer)) {
            query.eq(CdpWarehouseDatasetCatalogDO::getLayer, layer.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(status)) {
            query.eq(CdpWarehouseDatasetCatalogDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, DatasetView> byKey = new LinkedHashMap<>();
        for (CdpWarehouseDatasetCatalogDO row : safeList(datasetMapper.selectList(query))) {
            byKey.put(row.getDatasetKey(), toDataset(row));
        }
        return new ArrayList<>(byKey.values());
    }

    public LineageGraph lineage(Long tenantId, String datasetKey, Direction direction) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String targetKey = required(datasetKey, "datasetKey");
        Direction scopedDirection = direction == null ? Direction.BOTH : direction;
        LambdaQueryWrapper<CdpWarehouseLineageEdgeDO> edgeQuery = new LambdaQueryWrapper<CdpWarehouseLineageEdgeDO>()
                .in(CdpWarehouseLineageEdgeDO::getTenantId, tenantScope(scopedTenantId))
                .eq(CdpWarehouseLineageEdgeDO::getActive, true)
                .orderByAsc(CdpWarehouseLineageEdgeDO::getTenantId)
                .orderByAsc(CdpWarehouseLineageEdgeDO::getUpstreamDatasetKey)
                .orderByAsc(CdpWarehouseLineageEdgeDO::getDownstreamDatasetKey);
        if (scopedDirection == Direction.UPSTREAM) {
            edgeQuery.eq(CdpWarehouseLineageEdgeDO::getDownstreamDatasetKey, targetKey);
        } else if (scopedDirection == Direction.DOWNSTREAM) {
            edgeQuery.eq(CdpWarehouseLineageEdgeDO::getUpstreamDatasetKey, targetKey);
        } else {
            edgeQuery.and(q -> q.eq(CdpWarehouseLineageEdgeDO::getUpstreamDatasetKey, targetKey)
                    .or()
                    .eq(CdpWarehouseLineageEdgeDO::getDownstreamDatasetKey, targetKey));
        }

        Map<String, LineageEdgeView> edges = new LinkedHashMap<>();
        Set<String> datasetKeys = new LinkedHashSet<>();
        datasetKeys.add(targetKey);
        for (CdpWarehouseLineageEdgeDO edge : safeList(lineageMapper.selectList(edgeQuery))) {
            String key = edge.getUpstreamDatasetKey() + "|" + edge.getDownstreamDatasetKey() + "|"
                    + defaultString(edge.getTransformRef(), "");
            edges.put(key, toEdge(edge));
            datasetKeys.add(edge.getUpstreamDatasetKey());
            datasetKeys.add(edge.getDownstreamDatasetKey());
        }

        Map<String, DatasetView> nodes = loadDatasetNodes(scopedTenantId, datasetKeys);
        for (String key : datasetKeys) {
            nodes.putIfAbsent(key, DatasetView.stub(scopedTenantId, key));
        }
        return new LineageGraph(scopedTenantId, targetKey, scopedDirection,
                new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    public TransitiveLineageGraph transitiveLineage(Long tenantId,
                                                    String datasetKey,
                                                    Direction direction,
                                                    Integer maxDepth) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String targetKey = required(datasetKey, "datasetKey");
        Direction scopedDirection = direction == null ? Direction.BOTH : direction;
        List<String> warnings = new ArrayList<>();
        int depthLimit = boundedDepth(maxDepth, warnings);

        List<LineageEdgeView> allEdges = activeLineageEdges(scopedTenantId);
        Map<String, List<LineageEdgeView>> byUpstream = new LinkedHashMap<>();
        Map<String, List<LineageEdgeView>> byDownstream = new LinkedHashMap<>();
        for (LineageEdgeView edge : allEdges) {
            byUpstream.computeIfAbsent(edge.upstreamDatasetKey(), ignored -> new ArrayList<>()).add(edge);
            byDownstream.computeIfAbsent(edge.downstreamDatasetKey(), ignored -> new ArrayList<>()).add(edge);
        }

        Map<String, TraversalNodeState> nodeStates = new LinkedHashMap<>();
        Map<String, LineageEdgeView> discoveredEdges = new LinkedHashMap<>();
        List<LineagePathView> paths = new ArrayList<>();
        Deque<TraversalState> queue = new ArrayDeque<>();
        Set<String> expanded = new HashSet<>();
        nodeStates.put(targetKey, new TraversalNodeState(targetKey, 0, LineageRelation.SELF));
        queue.add(new TraversalState(targetKey, 0, LineageRelation.SELF, List.of(targetKey)));

        boolean truncated = false;
        while (!queue.isEmpty()) {
            TraversalState state = queue.removeFirst();
            if (!expanded.add(state.datasetKey())) {
                continue;
            }
            List<TraversalStep> steps = traversalSteps(state.datasetKey(), scopedDirection, byUpstream, byDownstream);
            if (state.depth() >= depthLimit) {
                if (!steps.isEmpty()) {
                    truncated = true;
                }
                continue;
            }
            for (TraversalStep step : steps) {
                LineageRelation nextRelation = nextRelation(state.relation(), step.relation());
                discoveredEdges.put(edgeKey(step.edge()), step.edge());
                if (state.path().contains(step.nextDatasetKey())) {
                    warnings.add("lineage cycle detected: "
                            + String.join(" -> ", appendPath(state.path(), step.nextDatasetKey())));
                    continue;
                }
                List<String> nextPath = appendPath(state.path(), step.nextDatasetKey());
                updateNodeState(nodeStates, step.nextDatasetKey(), state.depth() + 1, nextRelation);
                if (paths.size() < MAX_TRANSITIVE_PATHS) {
                    paths.add(new LineagePathView(nextPath, state.depth() + 1, nextRelation));
                } else {
                    truncated = true;
                }
                queue.add(new TraversalState(step.nextDatasetKey(), state.depth() + 1, nextRelation, nextPath));
            }
        }

        Map<String, DatasetView> datasets = loadDatasetNodes(scopedTenantId, nodeStates.keySet());
        List<LineageNodeView> nodes = nodeStates.values().stream()
                .map(state -> new LineageNodeView(
                        datasets.getOrDefault(state.datasetKey(), DatasetView.stub(scopedTenantId, state.datasetKey())),
                        state.depth(),
                        state.relation()))
                .toList();
        return new TransitiveLineageGraph(
                scopedTenantId,
                targetKey,
                scopedDirection,
                depthLimit,
                truncated,
                nodes,
                new ArrayList<>(discoveredEdges.values()),
                paths,
                warnings);
    }

    private List<LineageEdgeView> activeLineageEdges(Long tenantId) {
        LambdaQueryWrapper<CdpWarehouseLineageEdgeDO> edgeQuery = new LambdaQueryWrapper<CdpWarehouseLineageEdgeDO>()
                .in(CdpWarehouseLineageEdgeDO::getTenantId, tenantScope(tenantId))
                .eq(CdpWarehouseLineageEdgeDO::getActive, true)
                .orderByAsc(CdpWarehouseLineageEdgeDO::getTenantId)
                .orderByAsc(CdpWarehouseLineageEdgeDO::getUpstreamDatasetKey)
                .orderByAsc(CdpWarehouseLineageEdgeDO::getDownstreamDatasetKey);
        Map<String, LineageEdgeView> byKey = new LinkedHashMap<>();
        for (CdpWarehouseLineageEdgeDO edge : safeList(lineageMapper.selectList(edgeQuery))) {
            LineageEdgeView view = toEdge(edge);
            byKey.put(edgeKey(view), view);
        }
        return new ArrayList<>(byKey.values());
    }

    private List<TraversalStep> traversalSteps(String datasetKey,
                                               Direction direction,
                                               Map<String, List<LineageEdgeView>> byUpstream,
                                               Map<String, List<LineageEdgeView>> byDownstream) {
        List<TraversalStep> steps = new ArrayList<>();
        if (direction == Direction.UPSTREAM || direction == Direction.BOTH) {
            for (LineageEdgeView edge : byDownstream.getOrDefault(datasetKey, List.of())) {
                steps.add(new TraversalStep(edge.upstreamDatasetKey(), LineageRelation.UPSTREAM, edge));
            }
        }
        if (direction == Direction.DOWNSTREAM || direction == Direction.BOTH) {
            for (LineageEdgeView edge : byUpstream.getOrDefault(datasetKey, List.of())) {
                steps.add(new TraversalStep(edge.downstreamDatasetKey(), LineageRelation.DOWNSTREAM, edge));
            }
        }
        return steps;
    }

    private void updateNodeState(Map<String, TraversalNodeState> nodeStates,
                                 String datasetKey,
                                 int depth,
                                 LineageRelation relation) {
        TraversalNodeState current = nodeStates.get(datasetKey);
        if (current == null) {
            nodeStates.put(datasetKey, new TraversalNodeState(datasetKey, depth, relation));
            return;
        }
        nodeStates.put(datasetKey, new TraversalNodeState(
                datasetKey,
                Math.min(current.depth(), depth),
                mergeRelation(current.relation(), relation)));
    }

    private LineageRelation nextRelation(LineageRelation current, LineageRelation edgeRelation) {
        if (current == LineageRelation.SELF) {
            return edgeRelation;
        }
        return mergeRelation(current, edgeRelation);
    }

    private LineageRelation mergeRelation(LineageRelation current, LineageRelation incoming) {
        if (current == incoming) {
            return current;
        }
        if (current == LineageRelation.SELF) {
            return incoming;
        }
        if (incoming == LineageRelation.SELF) {
            return current;
        }
        return LineageRelation.BOTH;
    }

    private List<String> appendPath(List<String> path, String nextDatasetKey) {
        List<String> nextPath = new ArrayList<>(path);
        nextPath.add(nextDatasetKey);
        return List.copyOf(nextPath);
    }

    private int boundedDepth(Integer value, List<String> warnings) {
        if (value == null) {
            return DEFAULT_TRANSITIVE_DEPTH;
        }
        if (value <= 0) {
            throw new IllegalArgumentException("maxDepth must be positive");
        }
        if (value > MAX_TRANSITIVE_DEPTH) {
            warnings.add("maxDepth capped at " + MAX_TRANSITIVE_DEPTH);
            return MAX_TRANSITIVE_DEPTH;
        }
        return value;
    }

    private String edgeKey(LineageEdgeView edge) {
        return edge.upstreamDatasetKey() + "|" + edge.downstreamDatasetKey() + "|"
                + defaultString(edge.transformRef(), "");
    }

    private Map<String, DatasetView> loadDatasetNodes(Long tenantId, Set<String> datasetKeys) {
        Map<String, DatasetView> nodes = new LinkedHashMap<>();
        if (datasetKeys.isEmpty()) {
            return nodes;
        }
        LambdaQueryWrapper<CdpWarehouseDatasetCatalogDO> query = new LambdaQueryWrapper<CdpWarehouseDatasetCatalogDO>()
                .in(CdpWarehouseDatasetCatalogDO::getTenantId, tenantScope(tenantId))
                .in(CdpWarehouseDatasetCatalogDO::getDatasetKey, datasetKeys)
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getTenantId)
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getDatasetKey);
        for (CdpWarehouseDatasetCatalogDO row : safeList(datasetMapper.selectList(query))) {
            nodes.put(row.getDatasetKey(), toDataset(row));
        }
        return nodes;
    }

    private DatasetView toDataset(CdpWarehouseDatasetCatalogDO row) {
        return new DatasetView(
                row.getId(),
                row.getTenantId(),
                row.getDatasetKey(),
                row.getLayer(),
                row.getPhysicalName(),
                row.getDisplayName(),
                row.getSubjectArea(),
                row.getSourceSystem(),
                row.getOwnerName(),
                row.getDescription(),
                row.getFreshnessSlaMinutes(),
                row.getPiiLevel(),
                row.getStatus(),
                row.getSchemaJson());
    }

    private LineageEdgeView toEdge(CdpWarehouseLineageEdgeDO row) {
        return new LineageEdgeView(
                row.getId(),
                row.getTenantId(),
                row.getUpstreamDatasetKey(),
                row.getDownstreamDatasetKey(),
                row.getTransformType(),
                row.getTransformRef(),
                row.getDependencyType(),
                row.getDescription(),
                row.getActive() == null || row.getActive());
    }

    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public enum Direction {
        UPSTREAM,
        DOWNSTREAM,
        BOTH
    }

    public enum LineageRelation {
        SELF,
        UPSTREAM,
        DOWNSTREAM,
        BOTH
    }

    public record DatasetCommand(
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
    }

    public record LineageCommand(
            String upstreamDatasetKey,
            String downstreamDatasetKey,
            String transformType,
            String transformRef,
            String dependencyType,
            String description,
            Boolean active) {
    }

    public record DatasetView(
            Long id,
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

        static DatasetView stub(Long tenantId, String datasetKey) {
            return new DatasetView(null, tenantId, datasetKey, "UNKNOWN", datasetKey, datasetKey,
                    null, null, null, null, null, PII_NORMAL, "UNKNOWN", null);
        }
    }

    public record LineageEdgeView(
            Long id,
            Long tenantId,
            String upstreamDatasetKey,
            String downstreamDatasetKey,
            String transformType,
            String transformRef,
            String dependencyType,
            String description,
            boolean active) {
    }

    public record LineageGraph(
            Long tenantId,
            String datasetKey,
            Direction direction,
            List<DatasetView> nodes,
            List<LineageEdgeView> edges) {
    }

    public record LineageNodeView(
            DatasetView dataset,
            int depth,
            LineageRelation relation) {
    }

    public record LineagePathView(
            List<String> datasetKeys,
            int depth,
            LineageRelation relation) {
        public LineagePathView {
            datasetKeys = datasetKeys == null ? List.of() : List.copyOf(datasetKeys);
        }
    }

    public record TransitiveLineageGraph(
            Long tenantId,
            String datasetKey,
            Direction direction,
            int maxDepth,
            boolean truncated,
            List<LineageNodeView> nodes,
            List<LineageEdgeView> edges,
            List<LineagePathView> paths,
            List<String> warnings) {
        public TransitiveLineageGraph {
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
            edges = edges == null ? List.of() : List.copyOf(edges);
            paths = paths == null ? List.of() : List.copyOf(paths);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    private record TraversalState(
            String datasetKey,
            int depth,
            LineageRelation relation,
            List<String> path) {
    }

    private record TraversalNodeState(
            String datasetKey,
            int depth,
            LineageRelation relation) {
    }

    private record TraversalStep(
            String nextDatasetKey,
            LineageRelation relation,
            LineageEdgeView edge) {
    }
}
