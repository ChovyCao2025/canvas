package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.datasource.BiDatasourceColumnPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaSnapshotView;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceTablePreview;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * BiDatasetFromDatasourceService 编排 domain.bi.dataset 场景的领域业务规则。
 */
@Service
public class BiDatasetFromDatasourceService {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern TABLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Set<String> JOIN_TYPES = Set.of("INNER", "LEFT", "RIGHT", "FULL");
    private static final Set<String> JOIN_CONDITION_OPERATORS = Set.of("=", "<>", ">", ">=", "<", "<=");
    private static final Set<String> JOIN_CONDITION_CONNECTORS = Set.of("AND", "OR");

    private final BiDatasourceRuntimeService datasourceRuntimeService;
    private final BiDatasetResourceService datasetResourceService;
    private final BiPermissionService permissionService;

    /**
     * 创建 BiDatasetFromDatasourceService 实例并注入 domain.bi.dataset 场景依赖。
     * @param datasourceRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetFromDatasourceService(BiDatasourceRuntimeService datasourceRuntimeService,
                                          BiDatasetResourceService datasetResourceService) {
        this(datasourceRuntimeService, datasetResourceService, null);
    }

    /**
     * 创建 BiDatasetFromDatasourceService 实例并注入 domain.bi.dataset 场景依赖。
     * @param datasourceRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiDatasetFromDatasourceService(BiDatasourceRuntimeService datasourceRuntimeService,
                                          BiDatasetResourceService datasetResourceService,
                                          BiPermissionService permissionService) {
        this.datasourceRuntimeService = datasourceRuntimeService;
        this.datasetResourceService = datasetResourceService;
        this.permissionService = permissionService;
    }

    /**
     * 基于单表数据源创建 BI 数据集草稿，生成字段、指标和来源血缘信息。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param role 当前操作人的角色，用于判断管理、发布和审批豁免权限
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiDatasetResource createTableDataset(Long tenantId,
                                                String username,
                                                String role,
                                                BiDatasetFromDatasourceCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("BI datasource dataset command is required");
        }
        Long dataSourceConfigId = required(command.dataSourceConfigId(), "dataSourceConfigId");
        if (permissionService != null) {
            permissionService.enforceResourceAccess(
                    tenantId,
                    0L,
                    "DATASOURCE",
                    dataSourceConfigId,
                    new BiQueryContext(tenantId, username, role),
                    BiPermissionService.ACTION_USE);
        }
        BiDatasourceSchemaSnapshotView snapshot = datasourceRuntimeService.latestSchemaSnapshot(
                dataSourceConfigId,
                tenantId);
        if (snapshot == null || !"SUCCESS".equalsIgnoreCase(defaultString(snapshot.syncStatus()))) {
            throw new IllegalStateException("successful schema snapshot is required before creating a BI dataset");
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        BiDatasourceTablePreview table = snapshot.tables().stream()
                .filter(candidate -> required(command.tableName(), "tableName").equals(candidate.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("datasource table not found: " + command.tableName()));
        String tenantColumn = required(command.tenantColumn(), "tenantColumn");
        Map<String, BiDatasourceColumnPreview> tableColumns = indexColumns(table.columns());
        boolean tenantInjectedSnapshot = canInjectTenantColumn(snapshot);
        boolean tenantColumnPresent = tableColumns.containsKey(tenantColumn);
        if (!tenantColumnPresent && !tenantInjectedSnapshot) {
            throw new IllegalArgumentException("tenant column is required in datasource table: " + tenantColumn);
        }

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiDatasourceColumnPreview> selectedColumns = selectedColumns(
                command,
                table,
                tableColumns,
                tenantColumnPresent ? tenantColumn : null);
        List<BiDatasetFieldResource> fields = fields(selectedColumns, tenantColumn);
        List<String> dimensions = fields.stream()
                .filter(field -> field.visible() && "DIMENSION".equals(field.role()))
                .map(BiDatasetFieldResource::fieldKey)
                .toList();
        List<BiMetricResource> metrics = metrics(fields, dimensions, username);
        BiDatasetResource resource = new BiDatasetResource(
                required(command.datasetKey(), "datasetKey"),
                required(command.name(), "name"),
                "TABLE",
                table.name(),
                tenantColumn,
                model(snapshot, table.name(), command),
                fields,
                metrics,
                "DRAFT",
                "DATASOURCE_SCHEMA");
        return datasetResourceService.saveDraft(tenantId, username, role, resource);
    }

    /**
     * 基于多表关系创建 BI 数据集草稿，校验关联条件并沉淀可查询的语义模型。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param role 当前操作人的角色，用于判断管理、发布和审批豁免权限
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiDatasetResource createMultiTableDataset(Long tenantId,
                                                     String username,
                                                     String role,
                                                     BiDatasetFromDatasourceMultiTableCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("BI datasource multi-table dataset command is required");
        }
        Long dataSourceConfigId = required(command.dataSourceConfigId(), "dataSourceConfigId");
        if (permissionService != null) {
            permissionService.enforceResourceAccess(
                    tenantId,
                    0L,
                    "DATASOURCE",
                    dataSourceConfigId,
                    new BiQueryContext(tenantId, username, role),
                    BiPermissionService.ACTION_USE);
        }
        BiDatasourceSchemaSnapshotView snapshot = datasourceRuntimeService.latestSchemaSnapshot(
                dataSourceConfigId,
                tenantId);
        if (snapshot == null || !"SUCCESS".equalsIgnoreCase(defaultString(snapshot.syncStatus()))) {
            throw new IllegalStateException("successful schema snapshot is required before creating a BI dataset");
        }
        if (command.tables().size() < 2) {
            throw new IllegalArgumentException("at least two datasource tables are required for multi-table modeling");
        }
        if (command.joins().isEmpty()) {
            throw new IllegalArgumentException("at least one datasource join is required for multi-table modeling");
        }

        String baseTableName = validateTableIdentifier(required(command.baseTableName(), "baseTableName"));
        String tenantColumn = validateIdentifier(required(command.tenantColumn(), "tenantColumn"), "tenantColumn");
        Map<String, BiDatasourceTablePreview> snapshotTables = indexTables(snapshot.tables());
        Map<String, ModeledTable> modeledTables = modeledTables(command, snapshotTables, baseTableName, tenantColumn);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        ModeledTable baseTable = modeledTables.values().stream()
                .filter(table -> baseTableName.equals(table.tableName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("base datasource table must be selected: " + baseTableName));
        List<ModeledJoin> joins = modeledJoins(command.joins(), modeledTables);
        validateConnectedTables(modeledTables, baseTable, joins);

        List<ModeledColumn> modeledColumns = modeledColumns(modeledTables, baseTable, tenantColumn);
        List<BiDatasetFieldResource> fields = modeledFields(modeledColumns, tenantColumn);
        List<String> dimensions = fields.stream()
                .filter(field -> field.visible() && "DIMENSION".equals(field.role()))
                .map(BiDatasetFieldResource::fieldKey)
                .toList();
        List<BiMetricResource> metrics = metrics(fields, dimensions, username);
        BiDatasetResource resource = new BiDatasetResource(
                required(command.datasetKey(), "datasetKey"),
                required(command.name(), "name"),
                "SQL",
                sqlTableExpression(baseTable, joins, modeledColumns),
                tenantColumn,
                multiTableModel(snapshot, command, modeledTables, joins),
                fields,
                metrics,
                "DRAFT",
                "DATASOURCE_SCHEMA");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return datasetResourceService.saveDraft(tenantId, username, role, resource);
    }

    /**
     * 执行 model 流程，围绕 model 完成校验、计算或结果组装。
     *
     * @param snapshot snapshot 参数，用于 model 流程中的校验、计算或对象转换。
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @return 返回 model 流程生成的业务结果。
     */
    private Map<String, Object> model(BiDatasourceSchemaSnapshotView snapshot, String tableName) {
        return model(snapshot, tableName, null);
    }

    /**
     * 执行 model 流程，围绕 model 完成校验、计算或结果组装。
     *
     * @param snapshot snapshot 参数，用于 model 流程中的校验、计算或对象转换。
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 model 流程生成的业务结果。
     */
    private Map<String, Object> model(BiDatasourceSchemaSnapshotView snapshot,
                                      String tableName,
                                      BiDatasetFromDatasourceCommand command) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("dataSourceConfigId", snapshot.dataSourceConfigId());
        model.put("sourceKey", snapshot.sourceKey());
        model.put("connectorType", snapshot.connectorType());
        model.put("schemaSnapshotId", snapshot.id());
        model.put("tableName", tableName);
        if (command != null && command.apiResponseVariables() != null && !command.apiResponseVariables().isEmpty()) {
            model.put("apiResponseVariables", command.apiResponseVariables());
        }
        return model;
    }

    /**
     * 执行 multiTableModel 流程，围绕 multi table model 完成校验、计算或结果组装。
     *
     * @param snapshot snapshot 参数，用于 multiTableModel 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param tables tables 参数，用于 multiTableModel 流程中的校验、计算或对象转换。
     * @param joins joins 参数，用于 multiTableModel 流程中的校验、计算或对象转换。
     * @return 返回 multiTableModel 流程生成的业务结果。
     */
    private Map<String, Object> multiTableModel(BiDatasourceSchemaSnapshotView snapshot,
                                                BiDatasetFromDatasourceMultiTableCommand command,
                                                Map<String, ModeledTable> tables,
                                                List<ModeledJoin> joins) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "MULTI_TABLE");
        model.put("dataSourceConfigId", snapshot.dataSourceConfigId());
        model.put("sourceKey", snapshot.sourceKey());
        model.put("connectorType", snapshot.connectorType());
        model.put("schemaSnapshotId", snapshot.id());
        model.put("baseTableName", command.baseTableName());
        model.put("tables", tables.values().stream()
                .map(table -> Map.of(
                        "tableName", table.tableName(),
                        "alias", table.alias(),
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        "selectedColumns", table.selectedColumns().stream()
                                .map(BiDatasourceColumnPreview::name)
                                .toList()))
                .toList());
        model.put("joins", joins.stream()
                .map(join -> {
                    Map<String, Object> modeledJoin = new LinkedHashMap<>();
                    modeledJoin.put("joinType", join.joinType());
                    modeledJoin.put("leftAlias", join.left().alias());
                    modeledJoin.put("leftColumn", join.leftColumn());
                    modeledJoin.put("rightAlias", join.right().alias());
                    modeledJoin.put("rightColumn", join.rightColumn());
                    modeledJoin.put("conditions", join.conditions().stream()
                            .map(this::joinConditionModel)
                            .toList());
                    return modeledJoin;
                })
                .toList());
        model.put("graph", graphModel(command.graph(), tables, joins));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return model;
    }

    /**
     * 执行 graphModel 流程，围绕 graph model 完成校验、计算或结果组装。
     *
     * @param graph graph 参数，用于 graphModel 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 graphModel 流程中的校验、计算或对象转换。
     * @param joins joins 参数，用于 graphModel 流程中的校验、计算或对象转换。
     * @return 返回 graphModel 流程生成的业务结果。
     */
    private Map<String, Object> graphModel(BiDatasetFromDatasourceGraphCommand graph,
                                           Map<String, ModeledTable> tables,
                                           List<ModeledJoin> joins) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("layoutMode", graphLayoutMode(graph == null ? null : graph.layoutMode()));
        model.put("nodes", graphNodes(graph, tables));
        model.put("edges", graphEdges(joins));
        return model;
    }

    /**
     * 执行 graphNodes 流程，围绕 graph nodes 完成校验、计算或结果组装。
     *
     * @param graph graph 参数，用于 graphNodes 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 graphNodes 流程中的校验、计算或对象转换。
     * @return 返回 graphNodes 流程生成的业务结果。
     */
    private List<Map<String, Object>> graphNodes(BiDatasetFromDatasourceGraphCommand graph,
                                                 Map<String, ModeledTable> tables) {
        Map<String, BiDatasetFromDatasourceGraphNodeCommand> commandNodes = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (graph != null) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (BiDatasetFromDatasourceGraphNodeCommand node : graph.nodes()) {
                if (node == null) {
                    continue;
                }
                String alias = validateIdentifier(required(node.alias(), "graph node alias"), "graph node alias");
                ModeledTable table = tables.get(alias);
                if (table == null) {
                    throw new IllegalArgumentException("graph node alias not found: " + alias);
                }
                String tableName = validateTableIdentifier(required(node.tableName(), "graph node tableName"));
                if (!table.tableName().equals(tableName)) {
                    throw new IllegalArgumentException("graph node table does not match alias: " + alias);
                }
                commandNodes.putIfAbsent(alias, node);
            }
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        int index = 0;
        for (ModeledTable table : tables.values()) {
            BiDatasetFromDatasourceGraphNodeCommand commandNode = commandNodes.get(table.alias());
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("tableName", table.tableName());
            node.put("alias", table.alias());
            node.put("x", graphCoordinate(commandNode == null ? null : commandNode.x(), 80 + (index % 3) * 280));
            node.put("y", graphCoordinate(commandNode == null ? null : commandNode.y(), 80 + (index / 3) * 180));
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            node.put("selectedColumnsCount", table.selectedColumns().size());
            nodes.add(node);
            index++;
        }
        return nodes;
    }

    /**
     * 执行 graphEdges 流程，围绕 graph edges 完成校验、计算或结果组装。
     *
     * @param joins joins 参数，用于 graphEdges 流程中的校验、计算或对象转换。
     * @return 返回 graphEdges 流程生成的业务结果。
     */
    private List<Map<String, Object>> graphEdges(List<ModeledJoin> joins) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return joins.stream()
                .map(join -> {
                    Map<String, Object> edge = new LinkedHashMap<>();
                    edge.put("sourceAlias", join.left().alias());
                    edge.put("targetAlias", join.right().alias());
                    edge.put("joinType", join.joinType());
                    edge.put("conditionCount", join.conditions().size());
                    edge.put("conditions", join.conditions().stream()
                            .map(this::joinConditionModel)
                            .toList());
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return edge;
                })
                .toList();
    }

    /**
     * 执行 graphLayoutMode 流程，围绕 graph layout mode 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 graph layout mode 生成的文本或业务键。
     */
    private String graphLayoutMode(String value) {
        String mode = defaultString(value).trim().toUpperCase(Locale.ROOT);
        if (mode.isBlank()) {
            return "GRAPH_CANVAS";
        }
        if (!IDENTIFIER.matcher(mode).matches()) {
            throw new IllegalArgumentException("graph layout mode must be a safe identifier");
        }
        return mode;
    }

    /**
     * 执行 graphCoordinate 流程，围绕 graph coordinate 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 graphCoordinate 流程中的校验、计算或对象转换。
     * @return 返回 graph coordinate 计算得到的数量、金额或指标值。
     */
    private int graphCoordinate(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(4000, value));
    }

    /**
     * 执行 indexColumns 流程，围绕 index columns 完成校验、计算或结果组装。
     *
     * @param columns columns 参数，用于 indexColumns 流程中的校验、计算或对象转换。
     * @return 返回 indexColumns 流程生成的业务结果。
     */
    private Map<String, BiDatasourceColumnPreview> indexColumns(List<BiDatasourceColumnPreview> columns) {
        Map<String, BiDatasourceColumnPreview> result = new LinkedHashMap<>();
        for (BiDatasourceColumnPreview column : columns) {
            result.put(column.name(), column);
        }
        return result;
    }

    /**
     * 执行 indexTables 流程，围绕 index tables 完成校验、计算或结果组装。
     *
     * @param tables tables 参数，用于 indexTables 流程中的校验、计算或对象转换。
     * @return 返回 indexTables 流程生成的业务结果。
     */
    private Map<String, BiDatasourceTablePreview> indexTables(List<BiDatasourceTablePreview> tables) {
        Map<String, BiDatasourceTablePreview> result = new LinkedHashMap<>();
        for (BiDatasourceTablePreview table : tables == null ? List.<BiDatasourceTablePreview>of() : tables) {
            result.put(table.name(), table);
        }
        return result;
    }

    /**
     * 执行 modeledTables 流程，围绕 modeled tables 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param snapshotTables snapshot tables 参数，用于 modeledTables 流程中的校验、计算或对象转换。
     * @param baseTableName 名称文本，用于展示或唯一性校验。
     * @param tenantColumn tenant column 参数，用于 modeledTables 流程中的校验、计算或对象转换。
     * @return 返回 modeledTables 流程生成的业务结果。
     */
    private Map<String, ModeledTable> modeledTables(BiDatasetFromDatasourceMultiTableCommand command,
                                                    Map<String, BiDatasourceTablePreview> snapshotTables,
                                                    String baseTableName,
                                                    String tenantColumn) {
        // 多表建模只能引用最近成功 schema snapshot 中的表，避免用户提交任意 SQL 标识符绕过血缘和权限口径。
        Map<String, ModeledTable> result = new LinkedHashMap<>();
        int index = 1;
        for (BiDatasetFromDatasourceTableCommand tableCommand : command.tables()) {
            if (tableCommand == null) {
                throw new IllegalArgumentException("datasource table model is required");
            }
            String tableName = validateTableIdentifier(required(tableCommand.tableName(), "tableName"));
            BiDatasourceTablePreview table = snapshotTables.get(tableName);
            if (table == null) {
                throw new IllegalArgumentException("datasource table not found: " + tableName);
            }
            Map<String, BiDatasourceColumnPreview> columnsByName = indexColumns(table.columns());
            String alias = tableCommand.alias() == null || tableCommand.alias().isBlank()
                    /**
                     * 按默认值规则处理输入值。
                     *
                     * @param tableName 名称文本，用于展示或唯一性校验。
                     * @return 返回 defaultAlias 流程生成的业务结果。
                     */
                    ? defaultAlias(tableName, index)
                    /**
                     * 校验输入、权限或业务前置条件。
                     *
                     * @return 返回布尔判断结果。
                     */
                    : validateIdentifier(tableCommand.alias().trim(), "table alias");
            if (result.containsKey(alias)) {
                throw new IllegalArgumentException("duplicate datasource table alias: " + alias);
            }
            List<BiDatasourceColumnPreview> selectedColumns = selectedColumns(
                    tableCommand.selectedColumns(),
                    table,
                    columnsByName,
                    baseTableName.equals(tableName) ? tenantColumn : null);
            result.put(alias, new ModeledTable(tableName, alias, table, selectedColumns, columnsByName));
            index++;
        }
        return result;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param requestedColumns requested columns 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @param table table 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @param tableColumns table columns 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @param requiredColumn required column 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<BiDatasourceColumnPreview> selectedColumns(List<String> requestedColumns,
                                                            BiDatasourceTablePreview table,
                                                            Map<String, BiDatasourceColumnPreview> tableColumns,
                                                            String requiredColumn) {
        List<String> requested = requestedColumns == null || requestedColumns.isEmpty()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                ? table.columns().stream().map(BiDatasourceColumnPreview::name).toList()
                : requestedColumns;
        Set<String> ordered = new LinkedHashSet<>(requested);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (requiredColumn != null && !ordered.contains(requiredColumn)) {
            ordered.add(requiredColumn);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiDatasourceColumnPreview> selected = new ArrayList<>();
        for (String columnName : ordered) {
            String safeColumnName = validateIdentifier(required(columnName, "columnName"), "columnName");
            BiDatasourceColumnPreview column = tableColumns.get(safeColumnName);
            if (column == null) {
                throw new IllegalArgumentException("datasource column not found: " + safeColumnName);
            }
            selected.add(column);
        }
        selected.sort((left, right) -> Integer.compare(left.ordinalPosition(), right.ordinalPosition()));
        return selected;
    }

    /**
     * 执行 modeledJoins 流程，围绕 modeled joins 完成校验、计算或结果组装。
     *
     * @param joinCommands join commands 参数，用于 modeledJoins 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 modeledJoins 流程中的校验、计算或对象转换。
     * @return 返回 modeled joins 汇总后的集合、分页或映射视图。
     */
    private List<ModeledJoin> modeledJoins(List<BiDatasetFromDatasourceJoinCommand> joinCommands,
                                           Map<String, ModeledTable> tables) {
        List<ModeledJoin> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetFromDatasourceJoinCommand joinCommand : joinCommands) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (joinCommand == null) {
                throw new IllegalArgumentException("datasource join model is required");
            }
            String joinType = required(joinCommand.joinType(), "joinType").toUpperCase(Locale.ROOT);
            if (!JOIN_TYPES.contains(joinType)) {
                throw new IllegalArgumentException("unsupported datasource join type: " + joinCommand.joinType());
            }
            ModeledTable left = tableByAlias(tables, joinCommand.leftAlias(), "leftAlias");
            ModeledTable right = tableByAlias(tables, joinCommand.rightAlias(), "rightAlias");
            List<ModeledJoinCondition> conditions = modeledJoinConditions(joinCommand, left, right);
            result.add(new ModeledJoin(joinType, left, right, conditions));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 modeledJoinConditions 流程，围绕 modeled join conditions 完成校验、计算或结果组装。
     *
     * @param joinCommand 命令对象，描述本次业务动作及其参数。
     * @param left left 参数，用于 modeledJoinConditions 流程中的校验、计算或对象转换。
     * @param right right 参数，用于 modeledJoinConditions 流程中的校验、计算或对象转换。
     * @return 返回 modeled join conditions 汇总后的集合、分页或映射视图。
     */
    private List<ModeledJoinCondition> modeledJoinConditions(BiDatasetFromDatasourceJoinCommand joinCommand,
                                                             ModeledTable left,
                                                             ModeledTable right) {
        // 关联条件必须落在已选择的左右表列集合中，防止 JOIN 片段引用快照外字段或拼接危险表达式。
        List<BiDatasetFromDatasourceJoinConditionCommand> conditionCommands = joinCommand.conditions().isEmpty()
                ? List.of(new BiDatasetFromDatasourceJoinConditionCommand(joinCommand.leftColumn(), joinCommand.rightColumn()))
                : joinCommand.conditions();
        List<ModeledJoinCondition> result = new ArrayList<>();
        for (BiDatasetFromDatasourceJoinConditionCommand conditionCommand : conditionCommands) {
            if (conditionCommand == null) {
                throw new IllegalArgumentException("datasource join condition is required");
            }
            String leftColumn = validateIdentifier(required(conditionCommand.leftColumn(), "leftColumn"), "leftColumn");
            String rightColumn = validateIdentifier(required(conditionCommand.rightColumn(), "rightColumn"), "rightColumn");
            String operator = joinConditionOperator(conditionCommand.operator());
            String connector = result.isEmpty() ? null : joinConditionConnector(conditionCommand.connector());
            if (!left.columnsByName().containsKey(leftColumn)) {
                throw new IllegalArgumentException("datasource join column not found: " + left.alias() + "." + leftColumn);
            }
            if (!right.columnsByName().containsKey(rightColumn)) {
                throw new IllegalArgumentException("datasource join column not found: " + right.alias() + "." + rightColumn);
            }
            ModeledJoinCondition condition = new ModeledJoinCondition(
                    leftColumn,
                    rightColumn,
                    operator,
                    connector,
                    conditionCommand.groupStart(),
                    conditionCommand.groupEnd());
            if (!result.contains(condition)) {
                result.add(condition);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("at least one datasource join condition is required");
        }
        validateJoinConditionGroups(result);
        return result;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param conditions conditions 参数，用于 validateJoinConditionGroups 流程中的校验、计算或对象转换。
     */
    private void validateJoinConditionGroups(List<ModeledJoinCondition> conditions) {
        int depth = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ModeledJoinCondition condition : conditions) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (condition.groupStart()) {
                depth++;
            }
            if (condition.groupEnd()) {
                depth--;
            }
            if (depth < 0) {
                throw new IllegalArgumentException("datasource join condition group is not balanced");
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("datasource join condition group is not balanced");
        }
    }

    /**
     * 解析操作人标识。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 join condition operator 生成的文本或业务键。
     */
    private String joinConditionOperator(String operator) {
        String normalized = operator == null || operator.isBlank()
                ? "="
                : operator.trim();
        if (!JOIN_CONDITION_OPERATORS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported datasource join condition operator: " + operator);
        }
        return normalized;
    }

    /**
     * 执行 joinConditionConnector 流程，围绕 join condition connector 完成校验、计算或结果组装。
     *
     * @param connector connector 参数，用于 joinConditionConnector 流程中的校验、计算或对象转换。
     * @return 返回 join condition connector 生成的文本或业务键。
     */
    private String joinConditionConnector(String connector) {
        String normalized = connector == null || connector.isBlank()
                ? "AND"
                : connector.trim().toUpperCase(Locale.ROOT);
        if (!JOIN_CONDITION_CONNECTORS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported datasource join condition connector: " + connector);
        }
        return "OR".equals(normalized) ? "OR" : null;
    }

    /**
     * 执行 joinConditionModel 流程，围绕 join condition model 完成校验、计算或结果组装。
     *
     * @param condition condition 参数，用于 joinConditionModel 流程中的校验、计算或对象转换。
     * @return 返回 joinConditionModel 流程生成的业务结果。
     */
    private Map<String, Object> joinConditionModel(ModeledJoinCondition condition) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("leftColumn", condition.leftColumn());
        model.put("rightColumn", condition.rightColumn());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"=".equals(condition.operator())) {
            model.put("operator", condition.operator());
        }
        if (condition.connector() != null) {
            model.put("connector", condition.connector());
        }
        if (condition.groupStart()) {
            model.put("groupStart", true);
        }
        if (condition.groupEnd()) {
            model.put("groupEnd", true);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return model;
    }

    /**
     * 执行 tableByAlias 流程，围绕 table by alias 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 tableByAlias 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 tableByAlias 流程中的校验、计算或对象转换。
     * @param alias alias 参数，用于 tableByAlias 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 tableByAlias 流程生成的业务结果。
     */
    private ModeledTable tableByAlias(Map<String, ModeledTable> tables, String alias, String fieldName) {
        String safeAlias = validateIdentifier(required(alias, fieldName), fieldName);
        ModeledTable table = tables.get(safeAlias);
        if (table == null) {
            throw new IllegalArgumentException("datasource table alias not found: " + safeAlias);
        }
        return table;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tables tables 参数，用于 validateConnectedTables 流程中的校验、计算或对象转换。
     * @param baseTable base table 参数，用于 validateConnectedTables 流程中的校验、计算或对象转换。
     * @param joins joins 参数，用于 validateConnectedTables 流程中的校验、计算或对象转换。
     */
    private void validateConnectedTables(Map<String, ModeledTable> tables,
                                         ModeledTable baseTable,
                                         List<ModeledJoin> joins) {
        // 至少每张建模表都要出现在一个 join 中，避免生成包含孤立表的语义模型和误导性的血缘图。
        Set<String> connected = new LinkedHashSet<>();
        connected.add(baseTable.alias());
        for (ModeledJoin join : joins) {
            connected.add(join.left().alias());
            connected.add(join.right().alias());
        }
        for (String alias : tables.keySet()) {
            if (!connected.contains(alias)) {
                throw new IllegalArgumentException("datasource table is not connected by a join: " + alias);
            }
        }
    }

    /**
     * 执行 modeledColumns 流程，围绕 modeled columns 完成校验、计算或结果组装。
     *
     * @param tables tables 参数，用于 modeledColumns 流程中的校验、计算或对象转换。
     * @param baseTable base table 参数，用于 modeledColumns 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 modeledColumns 流程中的校验、计算或对象转换。
     * @return 返回 modeled columns 汇总后的集合、分页或映射视图。
     */
    private List<ModeledColumn> modeledColumns(Map<String, ModeledTable> tables,
                                               ModeledTable baseTable,
                                               String tenantColumn) {
        // 多表字段 key 需要全局唯一；基表租户列保留原 key，便于查询编译器继续做租户裁剪。
        List<ModeledColumn> result = new ArrayList<>();
        Set<String> fieldKeys = new LinkedHashSet<>();
        for (ModeledTable table : tables.values()) {
            for (BiDatasourceColumnPreview column : table.selectedColumns()) {
                validateIdentifier(column.name(), "columnName");
                String preferredKey = baseTable.alias().equals(table.alias()) && tenantColumn.equals(column.name())
                        ? tenantColumn
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        : normalizeFieldKey(table.tableName() + "_" + column.name());
                String fieldKey = uniqueFieldKey(preferredKey, fieldKeys);
                result.add(new ModeledColumn(table, column, fieldKey));
            }
        }
        return result;
    }

    /**
     * 执行 sqlTableExpression 流程，围绕 sql table expression 完成校验、计算或结果组装。
     *
     * @param baseTable base table 参数，用于 sqlTableExpression 流程中的校验、计算或对象转换。
     * @param joins joins 参数，用于 sqlTableExpression 流程中的校验、计算或对象转换。
     * @param columns columns 参数，用于 sqlTableExpression 流程中的校验、计算或对象转换。
     * @return 返回 sql table expression 生成的文本或业务键。
     */
    private String sqlTableExpression(ModeledTable baseTable,
                                      List<ModeledJoin> joins,
                                      List<ModeledColumn> columns) {
        // 这里生成的是受控 SELECT 派生表表达式，输入均已通过快照和标识符校验，不接受用户原始 SQL 片段。
        List<String> selectParts = columns.stream()
                .map(column -> column.table().alias() + "." + column.column().name() + " AS " + column.fieldKey())
                .toList();
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", selectParts))
                .append(" FROM ")
                .append(baseTable.tableName())
                .append(" ")
                .append(baseTable.alias());
        for (ModeledJoin join : joins) {
            sql.append(" ")
                    .append(join.joinType())
                    .append(" JOIN ")
                    .append(join.right().tableName())
                    .append(" ")
                    .append(join.right().alias())
                    .append(" ON ")
                    .append(join.onExpression());
        }
        return sql.toString();
    }

    /**
     * 查询或读取业务数据。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param table table 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @param tableColumns table columns 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<BiDatasourceColumnPreview> selectedColumns(BiDatasetFromDatasourceCommand command,
                                                            BiDatasourceTablePreview table,
                                                            Map<String, BiDatasourceColumnPreview> tableColumns,
                                                            String tenantColumn) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<String> requested = command.selectedColumns().isEmpty()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                ? table.columns().stream().map(BiDatasourceColumnPreview::name).toList()
                : command.selectedColumns();
        Set<String> ordered = new LinkedHashSet<>(requested);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantColumn != null && !ordered.contains(tenantColumn)) {
            ordered.add(tenantColumn);
        }
        List<BiDatasourceColumnPreview> selected = new ArrayList<>();
        for (String columnName : ordered) {
            BiDatasourceColumnPreview column = tableColumns.get(columnName);
            if (column == null) {
                throw new IllegalArgumentException("datasource column not found: " + columnName);
            }
            selected.add(column);
        }
        selected.sort((left, right) -> Integer.compare(left.ordinalPosition(), right.ordinalPosition()));
        return selected;
    }

    /**
     * 执行 fields 流程，围绕 fields 完成校验、计算或结果组装。
     *
     * @param columns columns 参数，用于 fields 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 fields 流程中的校验、计算或对象转换。
     * @return 返回 fields 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasetFieldResource> fields(List<BiDatasourceColumnPreview> columns, String tenantColumn) {
        // 准备本次处理所需的上下文和中间变量。
        List<BiDatasetFieldResource> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasourceColumnPreview column : columns) {
            boolean tenantField = tenantColumn.equals(column.name());
            String dataType = dataType(column);
            String role = tenantField || !"NUMBER".equals(dataType) ? "DIMENSION" : "MEASURE";
            result.add(new BiDatasetFieldResource(
                    column.name(),
                    displayName(column.name()),
                    column.name(),
                    role,
                    dataType,
                    semanticType(dataType),
                    "MEASURE".equals(role) ? "SUM" : null,
                    null,
                    null,
                    !tenantField,
                    "NORMAL",
                    column.ordinalPosition()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param snapshot snapshot 参数，用于 isApiSnapshot 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isApiSnapshot(BiDatasourceSchemaSnapshotView snapshot) {
        return snapshot != null
                && ("API".equalsIgnoreCase(defaultString(snapshot.connectorType()))
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 defaultString 流程生成的业务结果。
                 */
                || defaultString(snapshot.sourceKey()).toLowerCase(Locale.ROOT).startsWith("api-"));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param snapshot snapshot 参数，用于 isFileSnapshot 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isFileSnapshot(BiDatasourceSchemaSnapshotView snapshot) {
        return snapshot != null
                && ("CSV_EXCEL".equalsIgnoreCase(defaultString(snapshot.connectorType()))
                || "FILE".equalsIgnoreCase(defaultString(snapshot.connectorType()))
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 defaultString 流程生成的业务结果。
                 */
                || defaultString(snapshot.sourceKey()).toLowerCase(Locale.ROOT).startsWith("file-"));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param snapshot snapshot 参数，用于 canInjectTenantColumn 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean canInjectTenantColumn(BiDatasourceSchemaSnapshotView snapshot) {
        // API 和文件来源通常没有真实租户列，允许后续物化阶段注入 tenant_id 以统一租户口径。
        return isApiSnapshot(snapshot) || isFileSnapshot(snapshot);
    }

    /**
     * 执行 modeledFields 流程，围绕 modeled fields 完成校验、计算或结果组装。
     *
     * @param columns columns 参数，用于 modeledFields 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 modeledFields 流程中的校验、计算或对象转换。
     * @return 返回 modeled fields 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasetFieldResource> modeledFields(List<ModeledColumn> columns, String tenantColumn) {
        // 准备本次处理所需的上下文和中间变量。
        List<BiDatasetFieldResource> result = new ArrayList<>();
        int sortOrder = 1;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ModeledColumn column : columns) {
            boolean tenantField = tenantColumn.equals(column.fieldKey());
            String dataType = dataType(column.column());
            String role = tenantField || !"NUMBER".equals(dataType) ? "DIMENSION" : "MEASURE";
            result.add(new BiDatasetFieldResource(
                    column.fieldKey(),
                    displayName(column.fieldKey()),
                    column.fieldKey(),
                    role,
                    dataType,
                    semanticType(dataType),
                    "MEASURE".equals(role) ? "SUM" : null,
                    null,
                    null,
                    !tenantField,
                    "NORMAL",
                    sortOrder++));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 metrics 流程，围绕 metrics 完成校验、计算或结果组装。
     *
     * @param fields fields 参数，用于 metrics 流程中的校验、计算或对象转换。
     * @param dimensions dimensions 参数，用于 metrics 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 metrics 汇总后的集合、分页或映射视图。
     */
    private List<BiMetricResource> metrics(List<BiDatasetFieldResource> fields, List<String> dimensions, String username) {
        // 准备本次处理所需的上下文和中间变量。
        List<BiMetricResource> result = new ArrayList<>();
        result.add(new BiMetricResource(
                "row_count",
                "Row Count",
                "COUNT(1)",
                "COUNT",
                "NUMBER",
                null,
                "#,##0",
                dimensions,
                owner(username),
                "Datasource row count",
                "ACTIVE"));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetFieldResource field : fields) {
            if (!field.visible() || !"MEASURE".equals(field.role())) {
                continue;
            }
            String aggregation = field.defaultAggregation() == null ? "SUM" : field.defaultAggregation();
            result.add(new BiMetricResource(
                    field.fieldKey(),
                    field.displayName(),
                    aggregation + "(" + field.columnExpression() + ")",
                    aggregation,
                    field.dataType(),
                    field.unit(),
                    field.formatPattern(),
                    dimensions,
                    owner(username),
                    "Datasource metric for " + field.displayName(),
                    "ACTIVE"));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 dataType 流程，围绕 data type 完成校验、计算或结果组装。
     *
     * @param column column 参数，用于 dataType 流程中的校验、计算或对象转换。
     * @return 返回 data type 生成的文本或业务键。
     */
    private String dataType(BiDatasourceColumnPreview column) {
        return switch (column.dataType()) {
            case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT, Types.FLOAT, Types.REAL, Types.DOUBLE,
                    Types.NUMERIC, Types.DECIMAL -> "NUMBER";
            case Types.DATE -> "DATE";
            case Types.TIME, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, Types.TIME_WITH_TIMEZONE -> "DATETIME";
            case Types.BOOLEAN, Types.BIT -> "BOOLEAN";
            default -> "STRING";
        };
    }

    /**
     * 执行 semanticType 流程，围绕 semantic type 完成校验、计算或结果组装。
     *
     * @param dataType 类型标识，用于选择对应处理分支。
     * @return 返回 semantic type 生成的文本或业务键。
     */
    private String semanticType(String dataType) {
        return switch (dataType) {
            case "DATE" -> "DATE";
            case "DATETIME" -> "DATETIME";
            default -> null;
        };
    }

    /**
     * 执行 displayName 流程，围绕 display name 完成校验、计算或结果组装。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 display name 生成的文本或业务键。
     */
    private String displayName(String key) {
        String[] parts = key.toLowerCase(Locale.ROOT).split("[_\\s-]+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? key : String.join(" ", words);
    }

    /**
     * 执行 owner 流程，围绕 owner 完成校验、计算或结果组装。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 owner 生成的文本或业务键。
     */
    private String owner(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回布尔判断结果。
     */
    private String validateIdentifier(String value, String fieldName) {
        // 仅允许简单 SQL 标识符，禁止点号、空格、函数和引号进入列名、别名等可拼接位置。
        String identifier = required(value, fieldName).trim();
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a safe SQL identifier");
        }
        return identifier;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private String validateTableIdentifier(String value) {
        // 表名允许一层 schema 前缀，但不允许任意表达式，确保快照表名和生成 SQL 的数据口径一致。
        String identifier = required(value, "tableName").trim();
        if (!TABLE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("tableName must be a safe SQL table identifier");
        }
        return identifier;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @param index index 参数，用于 defaultAlias 流程中的校验、计算或对象转换。
     * @return 返回 default alias 生成的文本或业务键。
     */
    private String defaultAlias(String tableName, int index) {
        String alias = normalizeFieldKey(tableName.contains(".")
                ? tableName.substring(tableName.lastIndexOf('.') + 1)
                : tableName);
        return alias.isBlank() ? "t" + index : alias;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeFieldKey(String value) {
        String normalized = defaultString(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            return "field";
        }
        if (!Character.isLetter(normalized.charAt(0)) && normalized.charAt(0) != '_') {
            return "f_" + normalized;
        }
        return normalized;
    }

    /**
     * 执行 uniqueFieldKey 流程，围绕 unique field key 完成校验、计算或结果组装。
     *
     * @param preferred preferred 参数，用于 uniqueFieldKey 流程中的校验、计算或对象转换。
     * @param existing existing 参数，用于 uniqueFieldKey 流程中的校验、计算或对象转换。
     * @return 返回 unique field key 生成的文本或业务键。
     */
    private String uniqueFieldKey(String preferred, Set<String> existing) {
        String key = preferred;
        int suffix = 2;
        while (!existing.add(key)) {
            key = preferred + "_" + suffix++;
        }
        return key;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 流程生成的业务结果。
     */
    private <T> T required(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value instanceof String string && string.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * ModeledTable 数据记录。
     */
    private record ModeledTable(
            String tableName,
            String alias,
            BiDatasourceTablePreview table,
            List<BiDatasourceColumnPreview> selectedColumns,
            Map<String, BiDatasourceColumnPreview> columnsByName
    ) {
    }

    /**
     * ModeledJoin 数据记录。
     */
    private record ModeledJoin(
            String joinType,
            ModeledTable left,
            ModeledTable right,
            List<ModeledJoinCondition> conditions
    ) {
        private ModeledJoin {
            conditions = List.copyOf(conditions);
        }

        /**
         * 执行 leftColumn 流程，围绕 left column 完成校验、计算或结果组装。
         *
         * @return 返回 left column 生成的文本或业务键。
         */
        private String leftColumn() {
            return conditions.getFirst().leftColumn();
        }

        /**
         * 执行 rightColumn 流程，围绕 right column 完成校验、计算或结果组装。
         *
         * @return 返回 right column 生成的文本或业务键。
         */
        private String rightColumn() {
            return conditions.getFirst().rightColumn();
        }

        /**
         * 执行 onExpression 流程，围绕 on expression 完成校验、计算或结果组装。
         *
         * @return 返回 on expression 生成的文本或业务键。
         */
        private String onExpression() {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (conditions.isEmpty()) {
                throw new IllegalArgumentException("at least one datasource join condition is required");
            }
            List<String> expressions = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (int index = 0; index < conditions.size(); index++) {
                ModeledJoinCondition condition = conditions.get(index);
                String expression = (condition.groupStart() ? "(" : "")
                        + left.alias() + "." + condition.leftColumn()
                        + " " + condition.operator() + " " + right.alias() + "." + condition.rightColumn()
                        + (condition.groupEnd() ? ")" : "");
                if (index == 0) {
                    expressions.add(expression);
                } else {
                    expressions.add((condition.connector() == null ? "AND" : condition.connector()) + " " + expression);
                }
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return String.join(" ", expressions);
        }
    }

    /**
     * ModeledJoinCondition 数据记录。
     */
    private record ModeledJoinCondition(
            String leftColumn,
            String rightColumn,
            String operator,
            String connector,
            boolean groupStart,
            boolean groupEnd
    ) {
    }

    /**
     * ModeledColumn 数据记录。
     */
    private record ModeledColumn(
            ModeledTable table,
            BiDatasourceColumnPreview column,
            String fieldKey
    ) {
    }
}
