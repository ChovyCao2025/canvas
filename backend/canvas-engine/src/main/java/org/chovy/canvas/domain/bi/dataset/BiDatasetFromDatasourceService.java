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

@Service
public class BiDatasetFromDatasourceService {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern TABLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Set<String> JOIN_TYPES = Set.of("INNER", "LEFT", "RIGHT", "FULL");
    private static final Set<String> JOIN_CONDITION_OPERATORS = Set.of("=", "<>", ">", ">=", "<", "<=");

    private final BiDatasourceRuntimeService datasourceRuntimeService;
    private final BiDatasetResourceService datasetResourceService;
    private final BiPermissionService permissionService;

    public BiDatasetFromDatasourceService(BiDatasourceRuntimeService datasourceRuntimeService,
                                          BiDatasetResourceService datasetResourceService) {
        this(datasourceRuntimeService, datasetResourceService, null);
    }

    @Autowired
    public BiDatasetFromDatasourceService(BiDatasourceRuntimeService datasourceRuntimeService,
                                          BiDatasetResourceService datasetResourceService,
                                          BiPermissionService permissionService) {
        this.datasourceRuntimeService = datasourceRuntimeService;
        this.datasetResourceService = datasetResourceService;
        this.permissionService = permissionService;
    }

    public BiDatasetResource createTableDataset(Long tenantId,
                                                String username,
                                                String role,
                                                BiDatasetFromDatasourceCommand command) {
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

    public BiDatasetResource createMultiTableDataset(Long tenantId,
                                                     String username,
                                                     String role,
                                                     BiDatasetFromDatasourceMultiTableCommand command) {
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
        return datasetResourceService.saveDraft(tenantId, username, role, resource);
    }

    private Map<String, Object> model(BiDatasourceSchemaSnapshotView snapshot, String tableName) {
        return model(snapshot, tableName, null);
    }

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

    private Map<String, Object> multiTableModel(BiDatasourceSchemaSnapshotView snapshot,
                                                BiDatasetFromDatasourceMultiTableCommand command,
                                                Map<String, ModeledTable> tables,
                                                List<ModeledJoin> joins) {
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
        return model;
    }

    private Map<String, Object> graphModel(BiDatasetFromDatasourceGraphCommand graph,
                                           Map<String, ModeledTable> tables,
                                           List<ModeledJoin> joins) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("layoutMode", graphLayoutMode(graph == null ? null : graph.layoutMode()));
        model.put("nodes", graphNodes(graph, tables));
        model.put("edges", graphEdges(joins));
        return model;
    }

    private List<Map<String, Object>> graphNodes(BiDatasetFromDatasourceGraphCommand graph,
                                                 Map<String, ModeledTable> tables) {
        Map<String, BiDatasetFromDatasourceGraphNodeCommand> commandNodes = new LinkedHashMap<>();
        if (graph != null) {
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
            node.put("selectedColumnsCount", table.selectedColumns().size());
            nodes.add(node);
            index++;
        }
        return nodes;
    }

    private List<Map<String, Object>> graphEdges(List<ModeledJoin> joins) {
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
                    return edge;
                })
                .toList();
    }

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

    private int graphCoordinate(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(4000, value));
    }

    private Map<String, BiDatasourceColumnPreview> indexColumns(List<BiDatasourceColumnPreview> columns) {
        Map<String, BiDatasourceColumnPreview> result = new LinkedHashMap<>();
        for (BiDatasourceColumnPreview column : columns) {
            result.put(column.name(), column);
        }
        return result;
    }

    private Map<String, BiDatasourceTablePreview> indexTables(List<BiDatasourceTablePreview> tables) {
        Map<String, BiDatasourceTablePreview> result = new LinkedHashMap<>();
        for (BiDatasourceTablePreview table : tables == null ? List.<BiDatasourceTablePreview>of() : tables) {
            result.put(table.name(), table);
        }
        return result;
    }

    private Map<String, ModeledTable> modeledTables(BiDatasetFromDatasourceMultiTableCommand command,
                                                    Map<String, BiDatasourceTablePreview> snapshotTables,
                                                    String baseTableName,
                                                    String tenantColumn) {
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
                    ? defaultAlias(tableName, index)
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

    private List<BiDatasourceColumnPreview> selectedColumns(List<String> requestedColumns,
                                                            BiDatasourceTablePreview table,
                                                            Map<String, BiDatasourceColumnPreview> tableColumns,
                                                            String requiredColumn) {
        List<String> requested = requestedColumns == null || requestedColumns.isEmpty()
                ? table.columns().stream().map(BiDatasourceColumnPreview::name).toList()
                : requestedColumns;
        Set<String> ordered = new LinkedHashSet<>(requested);
        if (requiredColumn != null && !ordered.contains(requiredColumn)) {
            ordered.add(requiredColumn);
        }
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

    private List<ModeledJoin> modeledJoins(List<BiDatasetFromDatasourceJoinCommand> joinCommands,
                                           Map<String, ModeledTable> tables) {
        List<ModeledJoin> result = new ArrayList<>();
        for (BiDatasetFromDatasourceJoinCommand joinCommand : joinCommands) {
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
        return result;
    }

    private List<ModeledJoinCondition> modeledJoinConditions(BiDatasetFromDatasourceJoinCommand joinCommand,
                                                             ModeledTable left,
                                                             ModeledTable right) {
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
            if (!left.columnsByName().containsKey(leftColumn)) {
                throw new IllegalArgumentException("datasource join column not found: " + left.alias() + "." + leftColumn);
            }
            if (!right.columnsByName().containsKey(rightColumn)) {
                throw new IllegalArgumentException("datasource join column not found: " + right.alias() + "." + rightColumn);
            }
            ModeledJoinCondition condition = new ModeledJoinCondition(leftColumn, rightColumn, operator);
            if (!result.contains(condition)) {
                result.add(condition);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("at least one datasource join condition is required");
        }
        return result;
    }

    private String joinConditionOperator(String operator) {
        String normalized = operator == null || operator.isBlank()
                ? "="
                : operator.trim();
        if (!JOIN_CONDITION_OPERATORS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported datasource join condition operator: " + operator);
        }
        return normalized;
    }

    private Map<String, Object> joinConditionModel(ModeledJoinCondition condition) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("leftColumn", condition.leftColumn());
        model.put("rightColumn", condition.rightColumn());
        if (!"=".equals(condition.operator())) {
            model.put("operator", condition.operator());
        }
        return model;
    }

    private ModeledTable tableByAlias(Map<String, ModeledTable> tables, String alias, String fieldName) {
        String safeAlias = validateIdentifier(required(alias, fieldName), fieldName);
        ModeledTable table = tables.get(safeAlias);
        if (table == null) {
            throw new IllegalArgumentException("datasource table alias not found: " + safeAlias);
        }
        return table;
    }

    private void validateConnectedTables(Map<String, ModeledTable> tables,
                                         ModeledTable baseTable,
                                         List<ModeledJoin> joins) {
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

    private List<ModeledColumn> modeledColumns(Map<String, ModeledTable> tables,
                                               ModeledTable baseTable,
                                               String tenantColumn) {
        List<ModeledColumn> result = new ArrayList<>();
        Set<String> fieldKeys = new LinkedHashSet<>();
        for (ModeledTable table : tables.values()) {
            for (BiDatasourceColumnPreview column : table.selectedColumns()) {
                validateIdentifier(column.name(), "columnName");
                String preferredKey = baseTable.alias().equals(table.alias()) && tenantColumn.equals(column.name())
                        ? tenantColumn
                        : normalizeFieldKey(table.tableName() + "_" + column.name());
                String fieldKey = uniqueFieldKey(preferredKey, fieldKeys);
                result.add(new ModeledColumn(table, column, fieldKey));
            }
        }
        return result;
    }

    private String sqlTableExpression(ModeledTable baseTable,
                                      List<ModeledJoin> joins,
                                      List<ModeledColumn> columns) {
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

    private List<BiDatasourceColumnPreview> selectedColumns(BiDatasetFromDatasourceCommand command,
                                                            BiDatasourceTablePreview table,
                                                            Map<String, BiDatasourceColumnPreview> tableColumns,
                                                            String tenantColumn) {
        List<String> requested = command.selectedColumns().isEmpty()
                ? table.columns().stream().map(BiDatasourceColumnPreview::name).toList()
                : command.selectedColumns();
        Set<String> ordered = new LinkedHashSet<>(requested);
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

    private List<BiDatasetFieldResource> fields(List<BiDatasourceColumnPreview> columns, String tenantColumn) {
        List<BiDatasetFieldResource> result = new ArrayList<>();
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
        return result;
    }

    private boolean isApiSnapshot(BiDatasourceSchemaSnapshotView snapshot) {
        return snapshot != null
                && ("API".equalsIgnoreCase(defaultString(snapshot.connectorType()))
                || defaultString(snapshot.sourceKey()).toLowerCase(Locale.ROOT).startsWith("api-"));
    }

    private boolean isFileSnapshot(BiDatasourceSchemaSnapshotView snapshot) {
        return snapshot != null
                && ("CSV_EXCEL".equalsIgnoreCase(defaultString(snapshot.connectorType()))
                || "FILE".equalsIgnoreCase(defaultString(snapshot.connectorType()))
                || defaultString(snapshot.sourceKey()).toLowerCase(Locale.ROOT).startsWith("file-"));
    }

    private boolean canInjectTenantColumn(BiDatasourceSchemaSnapshotView snapshot) {
        return isApiSnapshot(snapshot) || isFileSnapshot(snapshot);
    }

    private List<BiDatasetFieldResource> modeledFields(List<ModeledColumn> columns, String tenantColumn) {
        List<BiDatasetFieldResource> result = new ArrayList<>();
        int sortOrder = 1;
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
        return result;
    }

    private List<BiMetricResource> metrics(List<BiDatasetFieldResource> fields, List<String> dimensions, String username) {
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
        return result;
    }

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

    private String semanticType(String dataType) {
        return switch (dataType) {
            case "DATE" -> "DATE";
            case "DATETIME" -> "DATETIME";
            default -> null;
        };
    }

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

    private String owner(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private String validateIdentifier(String value, String fieldName) {
        String identifier = required(value, fieldName).trim();
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a safe SQL identifier");
        }
        return identifier;
    }

    private String validateTableIdentifier(String value) {
        String identifier = required(value, "tableName").trim();
        if (!TABLE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("tableName must be a safe SQL table identifier");
        }
        return identifier;
    }

    private String defaultAlias(String tableName, int index) {
        String alias = normalizeFieldKey(tableName.contains(".")
                ? tableName.substring(tableName.lastIndexOf('.') + 1)
                : tableName);
        return alias.isBlank() ? "t" + index : alias;
    }

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

    private String uniqueFieldKey(String preferred, Set<String> existing) {
        String key = preferred;
        int suffix = 2;
        while (!existing.add(key)) {
            key = preferred + "_" + suffix++;
        }
        return key;
    }

    private <T> T required(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value instanceof String string && string.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private record ModeledTable(
            String tableName,
            String alias,
            BiDatasourceTablePreview table,
            List<BiDatasourceColumnPreview> selectedColumns,
            Map<String, BiDatasourceColumnPreview> columnsByName
    ) {
    }

    private record ModeledJoin(
            String joinType,
            ModeledTable left,
            ModeledTable right,
            List<ModeledJoinCondition> conditions
    ) {
        private ModeledJoin {
            conditions = List.copyOf(conditions);
        }

        private String leftColumn() {
            return conditions.getFirst().leftColumn();
        }

        private String rightColumn() {
            return conditions.getFirst().rightColumn();
        }

        private String onExpression() {
            return conditions.stream()
                    .map(condition -> left.alias() + "." + condition.leftColumn()
                            + " " + condition.operator() + " " + right.alias() + "." + condition.rightColumn())
                    .reduce((leftExpression, rightExpression) -> leftExpression + " AND " + rightExpression)
                    .orElseThrow(() -> new IllegalArgumentException("at least one datasource join condition is required"));
        }
    }

    private record ModeledJoinCondition(
            String leftColumn,
            String rightColumn,
            String operator
    ) {
    }

    private record ModeledColumn(
            ModeledTable table,
            BiDatasourceColumnPreview column,
            String fieldKey
    ) {
    }
}
