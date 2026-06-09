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
/**
 * CdpWarehouseCatalogService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseCatalogService 实例。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lineageMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseCatalogService(CdpWarehouseDatasetCatalogMapper datasetMapper,
                                      CdpWarehouseLineageEdgeMapper lineageMapper) {
        this.datasetMapper = datasetMapper;
        this.lineageMapper = lineageMapper;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public DatasetView upsertDataset(Long tenantId, DatasetCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        datasetMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toDataset(row);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param layer layer 参数，用于 listDatasets 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<DatasetView> listDatasets(Long tenantId, String layer, String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseDatasetCatalogDO> query = new LambdaQueryWrapper<CdpWarehouseDatasetCatalogDO>()
                .in(CdpWarehouseDatasetCatalogDO::getTenantId, tenantScope(scopedTenantId))
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getTenantId)
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getLayer)
                .orderByAsc(CdpWarehouseDatasetCatalogDO::getDatasetKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(layer)) {
            query.eq(CdpWarehouseDatasetCatalogDO::getLayer, layer.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(status)) {
            query.eq(CdpWarehouseDatasetCatalogDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, DatasetView> byKey = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseDatasetCatalogDO row : safeList(datasetMapper.selectList(query))) {
            byKey.put(row.getDatasetKey(), toDataset(row));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(byKey.values());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param direction direction 参数，用于 lineage 流程中的校验、计算或对象转换。
     * @return 返回 lineage 流程生成的业务结果。
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new LineageGraph(scopedTenantId, targetKey, scopedDirection,
                new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param direction direction 参数，用于 transitiveLineage 流程中的校验、计算或对象转换。
     * @param maxDepth max depth 参数，用于 transitiveLineage 流程中的校验、计算或对象转换。
     * @return 返回 transitiveLineage 流程生成的业务结果。
     */
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 active lineage edges 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param direction direction 参数，用于 traversalSteps 流程中的校验、计算或对象转换。
     * @param byUpstream by upstream 参数，用于 traversalSteps 流程中的校验、计算或对象转换。
     * @param byDownstream by downstream 参数，用于 traversalSteps 流程中的校验、计算或对象转换。
     * @return 返回 traversal steps 汇总后的集合、分页或映射视图。
     */
    private List<TraversalStep> traversalSteps(String datasetKey,
                                               Direction direction,
                                               Map<String, List<LineageEdgeView>> byUpstream,
                                               Map<String, List<LineageEdgeView>> byDownstream) {
        List<TraversalStep> steps = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (direction == Direction.UPSTREAM || direction == Direction.BOTH) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (LineageEdgeView edge : byDownstream.getOrDefault(datasetKey, List.of())) {
                steps.add(new TraversalStep(edge.upstreamDatasetKey(), LineageRelation.UPSTREAM, edge));
            }
        }
        if (direction == Direction.DOWNSTREAM || direction == Direction.BOTH) {
            for (LineageEdgeView edge : byUpstream.getOrDefault(datasetKey, List.of())) {
                steps.add(new TraversalStep(edge.downstreamDatasetKey(), LineageRelation.DOWNSTREAM, edge));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return steps;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param nodeStates node states 参数，用于 updateNodeState 流程中的校验、计算或对象转换。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param depth depth 参数，用于 updateNodeState 流程中的校验、计算或对象转换。
     * @param relation relation 参数，用于 updateNodeState 流程中的校验、计算或对象转换。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param current current 参数，用于 nextRelation 流程中的校验、计算或对象转换。
     * @param edgeRelation edge relation 参数，用于 nextRelation 流程中的校验、计算或对象转换。
     * @return 返回 nextRelation 流程生成的业务结果。
     */
    private LineageRelation nextRelation(LineageRelation current, LineageRelation edgeRelation) {
        if (current == LineageRelation.SELF) {
            return edgeRelation;
        }
        return mergeRelation(current, edgeRelation);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param current current 参数，用于 mergeRelation 流程中的校验、计算或对象转换。
     * @param incoming incoming 参数，用于 mergeRelation 流程中的校验、计算或对象转换。
     * @return 返回 mergeRelation 流程生成的业务结果。
     */
    private LineageRelation mergeRelation(LineageRelation current, LineageRelation incoming) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (current == incoming) {
            return current;
        }
        if (current == LineageRelation.SELF) {
            return incoming;
        }
        if (incoming == LineageRelation.SELF) {
            return current;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return LineageRelation.BOTH;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param path path 参数，用于 appendPath 流程中的校验、计算或对象转换。
     * @param nextDatasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 append path 汇总后的集合、分页或映射视图。
     */
    private List<String> appendPath(List<String> path, String nextDatasetKey) {
        List<String> nextPath = new ArrayList<>(path);
        nextPath.add(nextDatasetKey);
        return List.copyOf(nextPath);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param warnings warnings 参数，用于 boundedDepth 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedDepth(Integer value, List<String> warnings) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param edge edge 参数，用于 edgeKey 流程中的校验、计算或对象转换。
     * @return 返回 edge key 生成的文本或业务键。
     */
    private String edgeKey(LineageEdgeView edge) {
        return edge.upstreamDatasetKey() + "|" + edge.downstreamDatasetKey() + "|"
                + defaultString(edge.transformRef(), "");
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKeys dataset keys 参数，用于 loadDatasetNodes 流程中的校验、计算或对象转换。
     * @return 返回 loadDatasetNodes 流程生成的业务结果。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * Direction 承载对应领域的业务规则、流程编排和结果转换。
     */
    public enum Direction {
        UPSTREAM,
        DOWNSTREAM,
        BOTH
    }

    /**
     * LineageRelation 承载对应领域的业务规则、流程编排和结果转换。
     */
    public enum LineageRelation {
        SELF,
        UPSTREAM,
        DOWNSTREAM,
        BOTH
    }

    /**
     * DatasetCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * LineageCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record LineageCommand(
            String upstreamDatasetKey,
            String downstreamDatasetKey,
            String transformType,
            String transformRef,
            String dependencyType,
            String description,
            Boolean active) {
    }

    /**
     * DatasetView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param datasetKey 业务键，用于在同一租户下定位资源。
         * @return 返回 stub 流程生成的业务结果。
         */
        static DatasetView stub(Long tenantId, String datasetKey) {
            return new DatasetView(null, tenantId, datasetKey, "UNKNOWN", datasetKey, datasetKey,
                    null, null, null, null, null, PII_NORMAL, "UNKNOWN", null);
        }
    }

    /**
     * LineageEdgeView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * LineageGraph 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record LineageGraph(
            Long tenantId,
            String datasetKey,
            Direction direction,
            List<DatasetView> nodes,
            List<LineageEdgeView> edges) {
    }

    /**
     * LineageNodeView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record LineageNodeView(
            DatasetView dataset,
            int depth,
            LineageRelation relation) {
    }

    /**
     * LineagePathView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record LineagePathView(
            List<String> datasetKeys,
            int depth,
            LineageRelation relation) {
        public LineagePathView {
            datasetKeys = datasetKeys == null ? List.of() : List.copyOf(datasetKeys);
        }
    }

    /**
     * TransitiveLineageGraph 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * TraversalState 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record TraversalState(
            String datasetKey,
            int depth,
            LineageRelation relation,
            List<String> path) {
    }

    /**
     * TraversalNodeState 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record TraversalNodeState(
            String datasetKey,
            int depth,
            LineageRelation relation) {
    }

    /**
     * TraversalStep 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record TraversalStep(
            String nextDatasetKey,
            LineageRelation relation,
            LineageEdgeView edge) {
    }
}
