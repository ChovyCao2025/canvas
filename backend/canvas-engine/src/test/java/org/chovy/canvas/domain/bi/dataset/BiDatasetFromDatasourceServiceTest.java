package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.datasource.BiDatasourceColumnPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaSnapshotView;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceTablePreview;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasetFromDatasourceServiceTest {

    @Test
    void createsDraftTableDatasetFromLatestSuccessfulSchemaSnapshot() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(successSnapshot());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService, permissionService);

        BiDatasetResource resource = service.createTableDataset(
                42L,
                "alice",
                "TENANT_ADMIN",
                command());

        verify(permissionService).enforceResourceAccess(
                42L,
                0L,
                "DATASOURCE",
                7L,
                new BiQueryContext(42L, "alice", "TENANT_ADMIN"),
                BiPermissionService.ACTION_USE);

        ArgumentCaptor<BiDatasetResource> captor = ArgumentCaptor.forClass(BiDatasetResource.class);
        verify(datasetResourceService).saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                captor.capture());
        BiDatasetResource draft = captor.getValue();
        assertThat(resource).isSameAs(draft);
        assertThat(draft.datasetKey()).isEqualTo("campaign_daily");
        assertThat(draft.name()).isEqualTo("Campaign Daily");
        assertThat(draft.datasetType()).isEqualTo("TABLE");
        assertThat(draft.tableExpression()).isEqualTo("campaign_daily");
        assertThat(draft.tenantColumn()).isEqualTo("tenant_id");
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(draft.source()).isEqualTo("DATASOURCE_SCHEMA");
        assertThat(draft.model()).containsEntry("dataSourceConfigId", 7L)
                .containsEntry("sourceKey", "jdbc-7")
                .containsEntry("connectorType", "MYSQL")
                .containsEntry("schemaSnapshotId", 101L)
                .containsEntry("tableName", "campaign_daily");
        assertThat(draft.fields()).extracting(BiDatasetFieldResource::fieldKey)
                .containsExactly("tenant_id", "stat_date", "campaign_name", "total_cost");
        assertThat(draft.fields()).filteredOn(field -> "tenant_id".equals(field.fieldKey()))
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.role()).isEqualTo("DIMENSION");
                    assertThat(field.visible()).isFalse();
                });
        assertThat(draft.fields()).filteredOn(field -> "stat_date".equals(field.fieldKey()))
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.role()).isEqualTo("DIMENSION");
                    assertThat(field.dataType()).isEqualTo("DATE");
                    assertThat(field.semanticType()).isEqualTo("DATE");
                });
        assertThat(draft.fields()).filteredOn(field -> "total_cost".equals(field.fieldKey()))
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.role()).isEqualTo("MEASURE");
                    assertThat(field.dataType()).isEqualTo("NUMBER");
                    assertThat(field.defaultAggregation()).isEqualTo("SUM");
                });
        assertThat(draft.metrics()).extracting(BiMetricResource::metricKey)
                .containsExactly("row_count", "total_cost");
        assertThat(draft.metrics()).filteredOn(metric -> "row_count".equals(metric.metricKey()))
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.expression()).isEqualTo("COUNT(1)");
                    assertThat(metric.allowedDimensions()).containsExactly("stat_date", "campaign_name");
                });
        assertThat(draft.metrics()).filteredOn(metric -> "total_cost".equals(metric.metricKey()))
                .singleElement()
                .satisfies(metric -> assertThat(metric.expression()).isEqualTo("SUM(total_cost)"));
    }

    @Test
    void createsApiExtractDatasetWithoutTenantColumnAndPersistsRuntimeVariables() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(81L, 42L)).thenReturn(apiSnapshotWithoutTenantColumn());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("ANALYST"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService);

        BiDatasetResource resource = service.createTableDataset(
                42L,
                "alice",
                "ANALYST",
                new BiDatasetFromDatasourceCommand(
                        81L,
                        "api_response",
                        "api_orders",
                        "API Orders",
                        "tenant_id",
                        List.of("order_id", "amount", "paid"),
                        Map.of("campaignId", "cmp-1")));

        ArgumentCaptor<BiDatasetResource> captor = ArgumentCaptor.forClass(BiDatasetResource.class);
        verify(datasetResourceService).saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("ANALYST"),
                captor.capture());
        BiDatasetResource draft = captor.getValue();
        assertThat(resource).isSameAs(draft);
        assertThat(draft.datasetKey()).isEqualTo("api_orders");
        assertThat(draft.datasetType()).isEqualTo("TABLE");
        assertThat(draft.tableExpression()).isEqualTo("api_response");
        assertThat(draft.tenantColumn()).isEqualTo("tenant_id");
        assertThat(draft.model()).containsEntry("dataSourceConfigId", 81L)
                .containsEntry("sourceKey", "api-81")
                .containsEntry("connectorType", "API")
                .containsEntry("tableName", "api_response");
        assertThat(draft.model().get("apiResponseVariables")).isEqualTo(Map.of("campaignId", "cmp-1"));
        assertThat(draft.fields()).extracting(BiDatasetFieldResource::fieldKey)
                .containsExactly("order_id", "amount", "paid");
        assertThat(draft.fields()).filteredOn(field -> "amount".equals(field.fieldKey()))
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.role()).isEqualTo("MEASURE");
                    assertThat(field.defaultAggregation()).isEqualTo("SUM");
                });
        assertThat(draft.metrics()).extracting(BiMetricResource::metricKey)
                .containsExactly("row_count", "amount");
    }

    @Test
    void createsFileExtractDatasetWithoutTenantColumnForUploadedFileMaterialization() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(91L, 42L)).thenReturn(fileSnapshotWithoutTenantColumn());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("ANALYST"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService);

        BiDatasetResource resource = service.createTableDataset(
                42L,
                "alice",
                "ANALYST",
                new BiDatasetFromDatasourceCommand(
                        91L,
                        "orders",
                        "file_91_orders",
                        "Uploaded Orders orders",
                        "tenant_id",
                        List.of("order_id", "amount")));

        ArgumentCaptor<BiDatasetResource> captor = ArgumentCaptor.forClass(BiDatasetResource.class);
        verify(datasetResourceService).saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("ANALYST"),
                captor.capture());
        BiDatasetResource draft = captor.getValue();
        assertThat(resource).isSameAs(draft);
        assertThat(draft.datasetKey()).isEqualTo("file_91_orders");
        assertThat(draft.tableExpression()).isEqualTo("orders");
        assertThat(draft.tenantColumn()).isEqualTo("tenant_id");
        assertThat(draft.model()).containsEntry("dataSourceConfigId", 91L)
                .containsEntry("sourceKey", "file-91")
                .containsEntry("connectorType", "CSV_EXCEL")
                .containsEntry("tableName", "orders");
        assertThat(draft.fields()).extracting(BiDatasetFieldResource::fieldKey)
                .containsExactly("order_id", "amount");
        assertThat(draft.fields()).filteredOn(field -> "amount".equals(field.fieldKey()))
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.role()).isEqualTo("MEASURE");
                    assertThat(field.defaultAggregation()).isEqualTo("SUM");
                });
        assertThat(draft.metrics()).extracting(BiMetricResource::metricKey)
                .containsExactly("row_count", "amount");
    }

    @Test
    void createsDraftMultiTableSqlDatasetFromSchemaSnapshotAndJoinModel() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(multiTableSnapshot());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService, permissionService);

        BiDatasetResource resource = service.createMultiTableDataset(
                42L,
                "alice",
                "TENANT_ADMIN",
                new BiDatasetFromDatasourceMultiTableCommand(
                        7L,
                        "campaign_cost_with_dim",
                        "Campaign Cost With Dim",
                        "campaign_daily",
                        "tenant_id",
                        List.of(
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_daily",
                                        "fact",
                                        List.of("tenant_id", "stat_date", "total_cost")),
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_dim",
                                        "dim",
                                        List.of("campaign_name"))),
                        List.of(new BiDatasetFromDatasourceJoinCommand(
                                "LEFT",
                                "fact",
                                "campaign_id",
                                "dim",
                                "campaign_id"))));

        verify(permissionService).enforceResourceAccess(
                42L,
                0L,
                "DATASOURCE",
                7L,
                new BiQueryContext(42L, "alice", "TENANT_ADMIN"),
                BiPermissionService.ACTION_USE);

        ArgumentCaptor<BiDatasetResource> captor = ArgumentCaptor.forClass(BiDatasetResource.class);
        verify(datasetResourceService).saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                captor.capture());
        BiDatasetResource draft = captor.getValue();
        assertThat(resource).isSameAs(draft);
        assertThat(draft.datasetType()).isEqualTo("SQL");
        assertThat(draft.tableExpression()).isEqualTo(
                "SELECT fact.tenant_id AS tenant_id, fact.stat_date AS campaign_daily_stat_date, "
                        + "fact.total_cost AS campaign_daily_total_cost, dim.campaign_name AS campaign_dim_campaign_name "
                        + "FROM campaign_daily fact LEFT JOIN campaign_dim dim ON fact.campaign_id = dim.campaign_id");
        assertThat(draft.tenantColumn()).isEqualTo("tenant_id");
        assertThat(draft.model()).containsEntry("modelType", "MULTI_TABLE")
                .containsEntry("baseTableName", "campaign_daily")
                .containsEntry("schemaSnapshotId", 101L)
                .containsEntry("dataSourceConfigId", 7L);
        assertThat(draft.fields()).extracting(BiDatasetFieldResource::fieldKey)
                .containsExactly(
                        "tenant_id",
                        "campaign_daily_stat_date",
                        "campaign_daily_total_cost",
                        "campaign_dim_campaign_name");
        assertThat(draft.fields()).filteredOn(field -> "tenant_id".equals(field.fieldKey()))
                .singleElement()
                .satisfies(field -> assertThat(field.visible()).isFalse());
        assertThat(draft.metrics()).extracting(BiMetricResource::metricKey)
                .containsExactly("row_count", "campaign_daily_total_cost");
        assertThat(draft.metrics()).filteredOn(metric -> "campaign_daily_total_cost".equals(metric.metricKey()))
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.expression()).isEqualTo("SUM(campaign_daily_total_cost)");
                    assertThat(metric.allowedDimensions())
                            .containsExactly("campaign_daily_stat_date", "campaign_dim_campaign_name");
                });
    }

    @Test
    void createsDraftMultiTableSqlDatasetWithCompositeJoinConditions() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(multiTableSnapshot());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService);

        BiDatasetResource resource = service.createMultiTableDataset(
                42L,
                "alice",
                "TENANT_ADMIN",
                new BiDatasetFromDatasourceMultiTableCommand(
                        7L,
                        "campaign_cost_with_budget",
                        "Campaign Cost With Budget",
                        "campaign_daily",
                        "tenant_id",
                        List.of(
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_daily",
                                        "fact",
                                        List.of("tenant_id", "stat_date", "total_cost")),
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_dim",
                                        "dim",
                                        List.of("campaign_name"))),
                        List.of(new BiDatasetFromDatasourceJoinCommand(
                                "INNER",
                                "fact",
                                null,
                                "dim",
                                null,
                                List.of(
                                        new BiDatasetFromDatasourceJoinConditionCommand("tenant_id", "tenant_id"),
                                        new BiDatasetFromDatasourceJoinConditionCommand("campaign_id", "campaign_id"))))));

        assertThat(resource.tableExpression()).isEqualTo(
                "SELECT fact.tenant_id AS tenant_id, fact.stat_date AS campaign_daily_stat_date, "
                        + "fact.total_cost AS campaign_daily_total_cost, dim.campaign_name AS campaign_dim_campaign_name "
                        + "FROM campaign_daily fact INNER JOIN campaign_dim dim "
                        + "ON fact.tenant_id = dim.tenant_id AND fact.campaign_id = dim.campaign_id");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> joins = (List<Map<String, Object>>) resource.model().get("joins");
        assertThat(joins).singleElement().satisfies(join -> {
            assertThat(join)
                    .containsEntry("joinType", "INNER")
                    .containsEntry("leftAlias", "fact")
                    .containsEntry("leftColumn", "tenant_id")
                    .containsEntry("rightAlias", "dim")
                    .containsEntry("rightColumn", "tenant_id");
            assertThat(join.get("conditions")).isEqualTo(List.of(
                    Map.of("leftColumn", "tenant_id", "rightColumn", "tenant_id"),
                    Map.of("leftColumn", "campaign_id", "rightColumn", "campaign_id")));
        });
    }

    @Test
    void createsDraftMultiTableSqlDatasetWithNonDefaultJoinConditionOperator() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(multiTableSnapshot());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService);

        BiDatasetResource resource = service.createMultiTableDataset(
                42L,
                "alice",
                "TENANT_ADMIN",
                new BiDatasetFromDatasourceMultiTableCommand(
                        7L,
                        "campaign_cost_with_operator",
                        "Campaign Cost With Operator",
                        "campaign_daily",
                        "tenant_id",
                        List.of(
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_daily",
                                        "fact",
                                        List.of("tenant_id", "stat_date", "total_cost")),
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_dim",
                                        "dim",
                                        List.of("campaign_name"))),
                        List.of(new BiDatasetFromDatasourceJoinCommand(
                                "LEFT",
                                "fact",
                                null,
                                "dim",
                                null,
                                List.of(new BiDatasetFromDatasourceJoinConditionCommand("campaign_id", "campaign_id", "<>"))))));

        assertThat(resource.tableExpression()).isEqualTo(
                "SELECT fact.tenant_id AS tenant_id, fact.stat_date AS campaign_daily_stat_date, "
                        + "fact.total_cost AS campaign_daily_total_cost, dim.campaign_name AS campaign_dim_campaign_name "
                        + "FROM campaign_daily fact LEFT JOIN campaign_dim dim ON fact.campaign_id <> dim.campaign_id");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> joins = (List<Map<String, Object>>) resource.model().get("joins");
        assertThat(joins).singleElement().satisfies(join -> {
            assertThat(join)
                    .containsEntry("joinType", "LEFT")
                    .containsEntry("leftColumn", "campaign_id")
                    .containsEntry("rightColumn", "campaign_id");
            assertThat(join.get("conditions")).isEqualTo(List.of(
                    Map.of("leftColumn", "campaign_id", "rightColumn", "campaign_id", "operator", "<>")));
        });
        @SuppressWarnings("unchecked")
        Map<String, Object> graph = (Map<String, Object>) resource.model().get("graph");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.get("edges");
        assertThat(edges).singleElement().satisfies(edge -> assertThat(edge.get("conditions")).isEqualTo(List.of(
                Map.of("leftColumn", "campaign_id", "rightColumn", "campaign_id", "operator", "<>"))));
    }

    @Test
    void persistsGraphCanvasMetadataForMultiTableDatasourceModel() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetResourceService datasetResourceService = mock(BiDatasetResourceService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(multiTableSnapshot());
        when(datasetResourceService.saveDraft(
                org.mockito.Mockito.eq(42L),
                org.mockito.Mockito.eq("alice"),
                org.mockito.Mockito.eq("TENANT_ADMIN"),
                any(BiDatasetResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, BiDatasetResource.class));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, datasetResourceService);

        BiDatasetResource resource = service.createMultiTableDataset(
                42L,
                "alice",
                "TENANT_ADMIN",
                new BiDatasetFromDatasourceMultiTableCommand(
                        7L,
                        "campaign_graph",
                        "Campaign Graph",
                        "campaign_daily",
                        "tenant_id",
                        List.of(
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_daily",
                                        "fact",
                                        List.of("tenant_id", "stat_date", "total_cost")),
                                new BiDatasetFromDatasourceTableCommand(
                                        "campaign_dim",
                                        "dim",
                                        List.of("campaign_name"))),
                        List.of(new BiDatasetFromDatasourceJoinCommand(
                                "LEFT",
                                "fact",
                                null,
                                "dim",
                                null,
                                List.of(
                                        new BiDatasetFromDatasourceJoinConditionCommand("tenant_id", "tenant_id"),
                                        new BiDatasetFromDatasourceJoinConditionCommand("campaign_id", "campaign_id")))),
                        new BiDatasetFromDatasourceGraphCommand(
                                "GRAPH_CANVAS",
                                List.of(
                                        new BiDatasetFromDatasourceGraphNodeCommand("campaign_daily", "fact", 120, 80),
                                        new BiDatasetFromDatasourceGraphNodeCommand("campaign_dim", "dim", 460, 80)))));

        @SuppressWarnings("unchecked")
        Map<String, Object> graph = (Map<String, Object>) resource.model().get("graph");
        assertThat(graph).containsEntry("layoutMode", "GRAPH_CANVAS");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) graph.get("nodes");
        assertThat(nodes).containsExactly(
                Map.of(
                        "tableName", "campaign_daily",
                        "alias", "fact",
                        "x", 120,
                        "y", 80,
                        "selectedColumnsCount", 3),
                Map.of(
                        "tableName", "campaign_dim",
                        "alias", "dim",
                        "x", 460,
                        "y", 80,
                        "selectedColumnsCount", 1));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.get("edges");
        assertThat(edges).singleElement().satisfies(edge -> {
            assertThat(edge)
                    .containsEntry("sourceAlias", "fact")
                    .containsEntry("targetAlias", "dim")
                    .containsEntry("joinType", "LEFT")
                    .containsEntry("conditionCount", 2);
            assertThat(edge.get("conditions")).isEqualTo(List.of(
                    Map.of("leftColumn", "tenant_id", "rightColumn", "tenant_id"),
                    Map.of("leftColumn", "campaign_id", "rightColumn", "campaign_id")));
        });
    }

    @Test
    void rejectsDatasourceDatasetCreationWithoutSuccessfulSchemaSnapshot() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(new BiDatasourceSchemaSnapshotView(
                101L,
                7L,
                "jdbc-7",
                "warehouse",
                "MYSQL",
                "FAILED",
                "connection refused",
                0,
                0,
                List.of(),
                LocalDateTime.parse("2026-06-06T03:00:00"),
                "alice"));
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, mock(BiDatasetResourceService.class));

        assertThatThrownBy(() -> service.createTableDataset(42L, "alice", "TENANT_ADMIN", command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("successful schema snapshot is required");
    }

    @Test
    void rejectsDatasourceDatasetCreationWhenTenantColumnIsMissingFromSelectedTable() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasourceSchemaSnapshotView snapshot = new BiDatasourceSchemaSnapshotView(
                101L,
                7L,
                "jdbc-7",
                "warehouse",
                "MYSQL",
                "SUCCESS",
                null,
                1,
                1,
                List.of(new BiDatasourceTablePreview(
                        "campaign_daily",
                        "TABLE",
                        List.of(new BiDatasourceColumnPreview("stat_date", "DATE", 91, false, 1)))),
                LocalDateTime.parse("2026-06-06T03:00:00"),
                "alice");
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(snapshot);
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(datasourceRuntimeService, mock(BiDatasetResourceService.class));

        assertThatThrownBy(() -> service.createTableDataset(42L, "alice", "TENANT_ADMIN", command()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant column is required in datasource table");
    }

    @Test
    void rejectsDatasourceDatasetCreationWhenUsePermissionIsDenied() {
        BiDatasourceRuntimeService datasourceRuntimeService = mock(BiDatasourceRuntimeService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        when(datasourceRuntimeService.latestSchemaSnapshot(7L, 42L)).thenReturn(successSnapshot());
        org.mockito.Mockito.doThrow(new BiPermissionService.BiPermissionDeniedException("denied"))
                .when(permissionService)
                .enforceResourceAccess(
                        42L,
                        0L,
                        "DATASOURCE",
                        7L,
                        new BiQueryContext(42L, "alice", "OPERATOR"),
                        BiPermissionService.ACTION_USE);
        BiDatasetFromDatasourceService service =
                new BiDatasetFromDatasourceService(
                        datasourceRuntimeService,
                        mock(BiDatasetResourceService.class),
                        permissionService);

        assertThatThrownBy(() -> service.createTableDataset(42L, "alice", "OPERATOR", command()))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("denied");
    }

    private BiDatasetFromDatasourceCommand command() {
        return new BiDatasetFromDatasourceCommand(
                7L,
                "campaign_daily",
                "campaign_daily",
                "Campaign Daily",
                "tenant_id",
                List.of("tenant_id", "stat_date", "campaign_name", "total_cost"));
    }

    private BiDatasourceSchemaSnapshotView successSnapshot() {
        return new BiDatasourceSchemaSnapshotView(
                101L,
                7L,
                "jdbc-7",
                "warehouse",
                "MYSQL",
                "SUCCESS",
                null,
                1,
                4,
                List.of(new BiDatasourceTablePreview(
                        "campaign_daily",
                        "TABLE",
                        List.of(
                                new BiDatasourceColumnPreview("tenant_id", "BIGINT", -5, false, 1),
                                new BiDatasourceColumnPreview("stat_date", "DATE", 91, false, 2),
                                new BiDatasourceColumnPreview("campaign_name", "VARCHAR", 12, true, 3),
                                new BiDatasourceColumnPreview("total_cost", "DECIMAL", 3, true, 4)))),
                LocalDateTime.parse("2026-06-06T03:00:00"),
                "alice");
    }

    private BiDatasourceSchemaSnapshotView multiTableSnapshot() {
        return new BiDatasourceSchemaSnapshotView(
                101L,
                7L,
                "jdbc-7",
                "warehouse",
                "MYSQL",
                "SUCCESS",
                null,
                2,
                8,
                List.of(
                        new BiDatasourceTablePreview(
                                "campaign_daily",
                                "TABLE",
                                List.of(
                                        new BiDatasourceColumnPreview("tenant_id", "BIGINT", -5, false, 1),
                                        new BiDatasourceColumnPreview("stat_date", "DATE", 91, false, 2),
                                        new BiDatasourceColumnPreview("campaign_id", "BIGINT", -5, false, 3),
                                        new BiDatasourceColumnPreview("total_cost", "DECIMAL", 3, true, 4))),
                        new BiDatasourceTablePreview(
                                "campaign_dim",
                                "TABLE",
                                List.of(
                                        new BiDatasourceColumnPreview("tenant_id", "BIGINT", -5, false, 1),
                                        new BiDatasourceColumnPreview("campaign_id", "BIGINT", -5, false, 2),
                                        new BiDatasourceColumnPreview("campaign_name", "VARCHAR", 12, true, 3),
                                        new BiDatasourceColumnPreview("owner_user", "VARCHAR", 12, true, 4)))),
                LocalDateTime.parse("2026-06-06T03:00:00"),
                "alice");
    }

    private BiDatasourceSchemaSnapshotView apiSnapshotWithoutTenantColumn() {
        return new BiDatasourceSchemaSnapshotView(
                202L,
                81L,
                "api-81",
                "Orders API",
                "API",
                "SUCCESS",
                null,
                1,
                3,
                List.of(new BiDatasourceTablePreview(
                        "api_response",
                        "HTTP_JSON",
                        List.of(
                                new BiDatasourceColumnPreview("order_id", "VARCHAR", 12, false, 1),
                                new BiDatasourceColumnPreview("amount", "DOUBLE", 8, true, 2),
                                new BiDatasourceColumnPreview("paid", "BOOLEAN", 16, true, 3)))),
                LocalDateTime.parse("2026-06-06T03:20:00"),
                "alice");
    }

    private BiDatasourceSchemaSnapshotView fileSnapshotWithoutTenantColumn() {
        return new BiDatasourceSchemaSnapshotView(
                303L,
                91L,
                "file-91",
                "Uploaded Orders",
                "CSV_EXCEL",
                "SUCCESS",
                null,
                1,
                2,
                List.of(new BiDatasourceTablePreview(
                        "orders",
                        "CSV",
                        List.of(
                                new BiDatasourceColumnPreview("order_id", "VARCHAR", 12, true, 1),
                                new BiDatasourceColumnPreview("amount", "DOUBLE", 8, true, 2)))),
                LocalDateTime.parse("2026-06-06T10:15:30"),
                "alice");
    }
}
