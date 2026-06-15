package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.chovy.canvas.bi.api.BiCatalogFacade;
import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiPermissionGrantView;
import org.chovy.canvas.bi.api.BiColumnPermissionCommand;
import org.chovy.canvas.bi.api.BiColumnPermissionView;
import org.chovy.canvas.bi.api.BiPermissionAuditEntryView;
import org.chovy.canvas.bi.api.BiPermissionRequestCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestReviewCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestView;
import org.chovy.canvas.bi.api.BiResourcePermissionCommand;
import org.chovy.canvas.bi.api.BiResourcePermissionView;
import org.chovy.canvas.bi.api.BiRowPermissionCommand;
import org.chovy.canvas.bi.api.BiRowPermissionView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiResourceVersionView;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceView;
import org.chovy.canvas.bi.api.BiWorkspaceCommand;
import org.chovy.canvas.bi.api.BiWorkspaceView;
import org.chovy.canvas.bi.application.BiCatalogApplicationService;
import org.chovy.canvas.bi.domain.BiAccessRequest;
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
import org.chovy.canvas.web.bi.BiCatalogController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class BiApiCompatibilityTest {

    private static final Long TENANT_ID = 7L;
    private static final Long WORKSPACE_ID = 5L;
    private static final String ACTOR = "analyst";

    @Test
    void workspaceAndDatasetDraftRoutesPreserveEnvelopeTenantScopeAndNormalizedCatalogFields() {
        TestFixture fixture = new TestFixture();

        webClient(fixture.facade)
                .post()
                .uri("/canvas/bi/workspaces")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceKey": " Growth Team ",
                          "name": "Growth workspace",
                          "description": "Growth BI workspace",
                          "status": "published"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(WORKSPACE_ID.intValue())
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.workspaceKey").isEqualTo("growth-team")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        webClient(fixture.facade)
                .post()
                .uri("/canvas/bi/datasets/resources/orders-daily/draft")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceId": 5,
                          "datasetKey": " Body Key Should Lose ",
                          "name": "Orders daily",
                          "datasetType": "sql",
                          "sourceRefId": 99,
                          "tableExpression": "fact_order",
                          "tenantColumn": "tenant_id",
                          "model": {"grain": "day", "owner": "growth"},
                          "fields": [
                            {
                              "fieldKey": "Order Date",
                              "displayName": "Order date",
                              "columnExpression": "order_date",
                              "roleKey": "DIMENSION",
                              "dataType": "DATE",
                              "defaultAggregation": "NONE",
                              "visible": true,
                              "sortOrder": 1
                            }
                          ],
                          "metrics": [
                            {
                              "metricKey": "Gross GMV",
                              "displayName": "Gross GMV",
                              "expression": "sum(pay_amount)",
                              "aggregation": "SUM",
                              "dataType": "DECIMAL",
                              "unit": "CNY"
                            }
                          ],
                          "status": "draft"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.workspaceId").isEqualTo(WORKSPACE_ID.intValue())
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.model.grain").isEqualTo("day")
                .jsonPath("$.data.model.owner").isEqualTo("growth")
                .jsonPath("$.data.fields[0].fieldKey").isEqualTo("order-date")
                .jsonPath("$.data.fields[0].displayName").isEqualTo("Order date")
                .jsonPath("$.data.metrics[0].metricKey").isEqualTo("gross-gmv")
                .jsonPath("$.data.metrics[0].displayName").isEqualTo("Gross GMV")
                .jsonPath("$.data.status").isEqualTo("DRAFT");

        BiDataset saved = fixture.repository.datasetsById.get(100L);
        assertThat(saved.tenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.datasetKey().value()).isEqualTo("orders-daily");
        assertThat(saved.model()).containsEntry("grain", "day");
    }

    @Test
    void chartDraftRouteRejectsMissingOrArchivedDatasetBeforeSuccessfulMutation() {
        TestFixture fixture = fixtureWithWorkspaceAndDataset("orders-daily", "published");
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/charts/resources/missing-chart/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Missing chart", "missing-dataset", "published"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("dataset is not available for BI chart");

        client.post()
                .uri("/canvas/bi/datasets/resources/archived-dataset/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetDraftJson("archived"))
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/canvas/bi/charts/resources/archived-chart/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Archived chart", "archived-dataset", "published"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("dataset is not available for BI chart");

        assertThat(fixture.repository.chartsById).isEmpty();

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Orders trend", "orders-daily", "published"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.query.dimensions[0]").isEqualTo("order_date")
                .jsonPath("$.data.style.palette").isEqualTo("ops");

        assertThat(fixture.repository.chartsById).hasSize(1);
    }

    @Test
    void chartLifecycleRoutesPreserveLegacyEnvelopeAndFinalModuleStateTransitions() {
        TestFixture fixture = fixtureWithWorkspaceAndDataset("orders-daily", "published");
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Orders trend", "orders-daily", "draft"))
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/publish")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Orders by region", "orders-daily", "draft"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/charts/resources/orders-trend/versions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].resourceType").isEqualTo("CHART")
                .jsonPath("$.data[0].resourceKey").isEqualTo("orders-trend")
                .jsonPath("$.data[0].version").isEqualTo(3)
                .jsonPath("$.data[1].version").isEqualTo(2)
                .jsonPath("$.data[2].version").isEqualTo(1);

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/versions/1/restore")
                .header("X-Actor", "restorer")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.name").isEqualTo("Orders trend")
                .jsonPath("$.data.style.palette").isEqualTo("ops");

        client.delete()
                .uri("/canvas/bi/charts/resources/orders-trend")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        client.get()
                .uri("/canvas/bi/charts/resources/orders-trend")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI chart not found");
    }

    @Test
    void datasetResourceReadRoutesPreserveCompactLegacyEnvelopeWithoutWorkspaceId() {
        TestFixture fixture = fixtureWithWorkspaceAndDataset("orders-daily", "published");
        WebTestClient client = webClient(fixture.facade);
        client.post()
                .uri("/canvas/bi/datasets/resources/archived-dataset/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetDraftJson("archived"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/datasets/resources")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data[0].status").isEqualTo("PUBLISHED");

        client.get()
                .uri("/canvas/bi/datasets/resources/orders-daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily");

        client.get()
                .uri("/canvas/bi/datasets/resources/archived-dataset")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI dataset not found")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void queryDatasetCatalogRoutesPreserveLegacyEnvelopeAndCompactBuiltInCatalog() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = WebTestClient.bindToController(new BiCatalogController(fixture.facade)).build();

        client.get()
                .uri("/canvas/bi/datasets")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data[0].tableExpression").doesNotExist()
                .jsonPath("$.data[0].tenantColumn").doesNotExist()
                .jsonPath("$.data[0].fields[*].fieldKey").value(keys ->
                        assertThat(asStringList(keys)).contains("stat_date", "canvas_name", "trigger_type"))
                .jsonPath("$.data[0].metrics[*].metricKey").value(keys ->
                        assertThat(asStringList(keys)).contains("total_executions", "success_rate"))
                .jsonPath("$.data[0].metrics[0].expression").doesNotExist();

        client.get()
                .uri("/canvas/bi/datasets/canvas_daily_stats")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data.fields[*].fieldKey").value(keys ->
                        assertThat(asStringList(keys)).contains("stat_date", "canvas_name", "trigger_type"))
                .jsonPath("$.data.metrics[*].metricKey").value(keys ->
                        assertThat(asStringList(keys)).contains("total_executions", "success_rate"));

        client.get()
                .uri("/canvas/bi/datasets/missing_dataset")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("Unknown BI dataset: missing_dataset")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void datasourceOperationsRoutesPreserveLegacyCompatibilityEnvelopeAndTenantScopedState() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = WebTestClient.bindToController(new BiCatalogController(fixture.facade)).build();

        client.get()
                .uri("/canvas/bi/datasources/connectors")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[*].connectorType").value(types ->
                        assertThat(asStringList(types)).containsExactly("API_JSON", "FILE_CSV", "MYSQL", "POSTGRESQL"));

        client.post()
                .uri("/canvas/bi/datasources/onboarding")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "connectorType": " mysql ",
                          "name": " Orders Warehouse ",
                          "url": "jdbc:mysql://db.internal:3306/orders",
                          "username": "report_user",
                          "password": "secret",
                          "description": "Primary warehouse",
                          "enabled": true,
                          "connectorConfig": {"schema": "dw"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.sourceKey").isEqualTo("orders-warehouse")
                .jsonPath("$.data.connectorType").isEqualTo("MYSQL")
                .jsonPath("$.data.maskedUrl").isEqualTo("jdbc:mysql://db.internal:****/orders")
                .jsonPath("$.data.maskedUsername").isEqualTo("r***r");

        client.get()
                .uri("/canvas/bi/datasources/onboarding")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].sourceKey").isEqualTo("orders-warehouse");

        client.post()
                .uri("/canvas/bi/datasources/1/connection-test")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.success").isEqualTo(true)
                .jsonPath("$.data.sourceKey").isEqualTo("orders-warehouse");

        client.post()
                .uri("/canvas/bi/datasources/1/schema-sync")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", "syncer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"limit\": 2}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.syncStatus").isEqualTo("SUCCESS")
                .jsonPath("$.data.syncedBy").isEqualTo("syncer")
                .jsonPath("$.data.tables[0].columns[0].name").isEqualTo("id");

        client.get()
                .uri("/canvas/bi/datasources/1/schema-snapshots")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].dataSourceConfigId").isEqualTo(1);

        List<BiDatasourceOnboardingView> sources = fixture.facade.listDatasources(TENANT_ID);
        assertThat(sources).extracting(BiDatasourceOnboardingView::sourceKey)
                .containsExactly("orders-warehouse");
    }

    @Test
    void dashboardPresetCatalogRoutesPreserveLegacyEnvelopeAndCompactBuiltInCatalog() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);

        client.get()
                .uri("/canvas/bi/dashboards/presets")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].dashboardKey").isEqualTo("canvas-effect")
                .jsonPath("$.data[0].datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data[0].widgets[*].widgetKey").value(keys ->
                        assertThat(asStringList(keys)).contains("trend-executions", "detail-canvas"))
                .jsonPath("$.data[0].filters[*].filterKey").value(keys ->
                        assertThat(asStringList(keys)).contains("filter-stat-date", "filter-canvas"))
                .jsonPath("$.data[0].interactions[*].interactionType").value(types ->
                        assertThat(asStringList(types)).contains("FILTER_LINKAGE", "HYPERLINK"))
                .jsonPath("$.data[0].subscriptionChannels[0]").isEqualTo("EMAIL")
                .jsonPath("$.data[0].embedScopes[0]").isEqualTo("INTERNAL_CANVAS");

        client.get()
                .uri("/canvas/bi/dashboards/presets/canvas-effect")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboardKey").isEqualTo("canvas-effect")
                .jsonPath("$.data.title").isEqualTo("画布效果分析")
                .jsonPath("$.data.filters[1].parentFilterKeys[0]").isEqualTo("filter-stat-date");

        client.get()
                .uri("/canvas/bi/dashboards/presets/missing-dashboard")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("Unknown BI dashboard preset: missing-dashboard")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void quickEngineCapacityReadRoutesPreserveLegacyEnvelopeAndCompactSnapshot() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/capacity/quick-engine")
                        .queryParam("limit", 2)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.capacityLimitRows").isEqualTo(1_000_000)
                .jsonPath("$.data.alertLevel").isEqualTo("NORMAL")
                .jsonPath("$.data.alertEnabled").isEqualTo(false)
                .jsonPath("$.data.overLimit").doesNotExist()
                .jsonPath("$.data.alertPolicy.warningThresholdPercent").isEqualTo(80)
                .jsonPath("$.data.alertPolicy.enabled").isEqualTo(false)
                .jsonPath("$.data.alertPolicy.notificationChannels.length()").isEqualTo(0)
                .jsonPath("$.data.alertPolicy.notificationReceivers.length()").isEqualTo(0)
                .jsonPath("$.data.alertPolicy.receivers").doesNotExist()
                .jsonPath("$.data.tenantPoolPolicy.poolKey").isEqualTo("STANDARD")
                .jsonPath("$.data.concurrencyQueue.runningQueries").isEqualTo(2)
                .jsonPath("$.data.categories[0].category").isEqualTo("DATASET_ACCELERATION")
                .jsonPath("$.data.details.length()").isEqualTo(2)
                .jsonPath("$.data.details[0].resourceKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data.userRankings[0].username").isEqualTo("analyst");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/capacity/quick-engine/queue")
                        .queryParam("poolKey", " gold ")
                        .queryParam("status", " queued ")
                        .queryParam("limit", 2000)
                        .build())
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.poolKey").isEqualTo("GOLD")
                .jsonPath("$.data.queued").isEqualTo(3)
                .jsonPath("$.data.claimed").isEqualTo(2)
                .jsonPath("$.data.total").isEqualTo(15)
                .jsonPath("$.data.running").doesNotExist()
                .jsonPath("$.data.failed").doesNotExist()
                .jsonPath("$.data.jobs[*].status").value(statuses ->
                        assertThat(asStringList(statuses)).containsOnly("QUEUED"))
                .jsonPath("$.data.jobs[*].poolKey").value(poolKeys ->
                        assertThat(asStringList(poolKeys)).containsOnly("GOLD"))
                .jsonPath("$.data.jobs[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.jobs[0].attemptCount").isEqualTo(1)
                .jsonPath("$.data.jobs[0].expiresAt").exists()
                .jsonPath("$.data.jobs[0].attempts").doesNotExist()
                .jsonPath("$.data.jobs[0].visibleAt").doesNotExist()
                .jsonPath("$.data.jobs[0].failureReason").doesNotExist();

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/capacity/quick-engine/queue")
                        .queryParam("poolKey", "gold")
                        .queryParam("status", "blocked")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.jobs[0].status").isEqualTo("BLOCKED")
                .jsonPath("$.data.jobs[0].blockedReason").isEqualTo("queue admission timeout")
                .jsonPath("$.data.jobs[0].failureReason").doesNotExist();
    }

    @Test
    void quickEnginePolicyRoutesPreserveLegacyEnvelopeAndValidationFailures() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/capacity/quick-engine/alert-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "capacityLimitRows": 2000000,
                          "warningThresholdPercent": 70,
                          "criticalThresholdPercent": 90,
                          "notificationChannels": [" email ", "LARK", "EMAIL", " "],
                          "notificationReceivers": [" bi-ops ", "analyst", "bi-ops"]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.enabled").isEqualTo(false)
                .jsonPath("$.data.capacityLimitRows").isEqualTo(2_000_000)
                .jsonPath("$.data.notificationChannels[0]").isEqualTo("EMAIL")
                .jsonPath("$.data.notificationChannels.length()").isEqualTo(2)
                .jsonPath("$.data.notificationReceivers[0]").isEqualTo("bi-ops")
                .jsonPath("$.data.notificationReceivers.length()").isEqualTo(2)
                .jsonPath("$.data.receivers").doesNotExist()
                .jsonPath("$.data.updatedBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/capacity/quick-engine/tenant-pool-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "poolKey": "gold_pool",
                          "maxConcurrentQueries": 16,
                          "queueTimeoutSeconds": 300,
                          "poolWeight": 250
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.poolKey").isEqualTo("GOLD_POOL")
                .jsonPath("$.data.queueLimit").isEqualTo(50)
                .jsonPath("$.data.updatedBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/capacity/quick-engine/tenant-pool-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "poolKey": "bad key",
                          "maxConcurrentQueries": 1
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("poolKey must match [A-Z0-9_-]{1,64}")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void chartResourceReadRoutesPreserveCompactLegacyEnvelopeWithoutWorkspaceId() {
        TestFixture fixture = fixtureWithWorkspaceDatasetAndChart();
        WebTestClient client = webClient(fixture.facade);
        client.post()
                .uri("/canvas/bi/charts/resources/archived-chart/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Archived chart", "orders-daily", "archived"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/charts/resources")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data[0].status").isEqualTo("PUBLISHED");

        client.get()
                .uri("/canvas/bi/charts/resources/orders-trend")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily");

        client.get()
                .uri("/canvas/bi/charts/resources/archived-chart")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI chart not found")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void chartImpactRoutePreservesLegacyEnvelopeDashboardReferencesAndMissingFailures() {
        TestFixture fixture = fixtureWithWorkspaceDatasetChartAndDashboard();
        WebTestClient client = webClient(fixture.facade);
        client.post()
                .uri("/canvas/bi/dashboards/resources/archived-dashboard/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dashboardDraftJson("Archived dashboard", "archived"))
                .exchange()
                .expectStatus().isOk();
        client.post()
                .uri("/canvas/bi/charts/resources/archived-chart/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Archived chart", "orders-daily", "archived"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/charts/resources/orders-trend/impact")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.chartName").isEqualTo("Orders trend")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.dashboards.length()").isEqualTo(1)
                .jsonPath("$.data.dashboards[0].dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.dashboards[0].title").isEqualTo("Marketing overview")
                .jsonPath("$.data.dashboards[0].widgetKey").isEqualTo("orders-trend")
                .jsonPath("$.data.dashboards[0].widgetTitle").isEqualTo("Orders trend")
                .jsonPath("$.data.dashboards[0].status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.portals").isArray()
                .jsonPath("$.data.portals.length()").isEqualTo(0)
                .jsonPath("$.data.subscriptions").isArray()
                .jsonPath("$.data.subscriptions.length()").isEqualTo(0);

        client.get()
                .uri("/canvas/bi/charts/resources/archived-chart/impact")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI chart not found: archived-chart")
                .jsonPath("$.data").doesNotExist();

        client.get()
                .uri("/canvas/bi/charts/resources/missing-chart/impact")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI chart not found: missing-chart");
    }

    @Test
    void dashboardResourceReadRoutesPreserveCompactLegacyEnvelopeWithoutWorkspaceId() {
        TestFixture fixture = fixtureWithWorkspaceDatasetChartAndDashboard();
        WebTestClient client = webClient(fixture.facade);
        client.post()
                .uri("/canvas/bi/dashboards/resources/archived-dashboard/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dashboardDraftJson("Archived dashboard", "archived"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/dashboards/resources")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data[0].status").isEqualTo("PUBLISHED");

        client.get()
                .uri("/canvas/bi/dashboards/resources/marketing-overview")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.chartKeys[0]").isEqualTo("orders-trend");

        client.get()
                .uri("/canvas/bi/dashboards/resources/archived-dashboard")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI dashboard not found")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void dashboardDraftAndReadModelRoutesPreserveReadinessEnvelope() {
        TestFixture fixture = fixtureWithWorkspaceDatasetAndChart();
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/dashboards/resources/marketing-overview/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceId": 5,
                          "dashboardKey": " Body Key Should Lose ",
                          "name": "Marketing overview",
                          "description": "Executive daily view",
                          "theme": {"mode": "light"},
                          "filters": {"region": "CN"},
                          "chartKeys": ["orders-trend", "missing-chart"],
                          "status": "draft"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.chartKeys[0]").isEqualTo("orders-trend")
                .jsonPath("$.data.chartKeys[1]").isEqualTo("missing-chart")
                .jsonPath("$.data.status").isEqualTo("DRAFT");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/dashboards/resources/marketing-overview")
                        .queryParam("workspaceId", WORKSPACE_ID)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboard.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.charts").isArray()
                .jsonPath("$.data.charts.length()").isEqualTo(1)
                .jsonPath("$.data.charts[0].chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.datasets").isArray()
                .jsonPath("$.data.datasets.length()").isEqualTo(1)
                .jsonPath("$.data.datasets[0].datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.readiness.status").isEqualTo("BLOCKED")
                .jsonPath("$.data.readiness.productionReady").isEqualTo(false)
                .jsonPath("$.data.readiness.publishedChartCount").isEqualTo(1)
                .jsonPath("$.data.readiness.blockers[0].code").isEqualTo("MISSING_CHART");
    }

    @Test
    void spreadsheetLifecycleRoutesPreserveAggregateCompatibilityAndVersionLimit() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(spreadsheetDraftJson("Revenue sheet", "draft"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet")
                .jsonPath("$.data.sheets[0].sheetKey").isEqualTo("Daily")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet/publish")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", "publisher")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.updatedBy").isEqualTo("publisher");

        client.get()
                .uri("/canvas/bi/spreadsheets/resources")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].spreadsheetKey").isEqualTo("revenue-sheet");

        client.get()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/spreadsheets/resources/revenue-sheet/versions")
                        .queryParam("limit", 1)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].resourceType").isEqualTo("SPREADSHEET")
                .jsonPath("$.data[0].resourceKey").isEqualTo("revenue-sheet")
                .jsonPath("$.data[0].version").isEqualTo(2);

        client.post()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet/versions/1/restore")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.version").isEqualTo(3)
                .jsonPath("$.data.status").isEqualTo("DRAFT");

        client.delete()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void permissionRoutesPreserveGrantEnvelopeAndEffectiveAccessDenyPrecedence() {
        TestFixture fixture = fixtureWithWorkspaceDatasetChartAndDashboard();
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/permissions/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(permissionGrantJson("ALL", "*", "allow"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.resourceType").isEqualTo("DASHBOARD")
                .jsonPath("$.data.resourceId").isEqualTo(300)
                .jsonPath("$.data.subjectType").isEqualTo("ALL")
                .jsonPath("$.data.actionKey").isEqualTo("VIEW")
                .jsonPath("$.data.effect").isEqualTo("ALLOW");

        client.post()
                .uri("/canvas/bi/permissions/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(permissionGrantJson("USER", "alice", "deny"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.subjectType").isEqualTo("USER")
                .jsonPath("$.data.subjectId").isEqualTo("alice")
                .jsonPath("$.data.effect").isEqualTo("DENY");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/permissions/effective-access")
                        .queryParam("workspaceId", WORKSPACE_ID)
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceId", 300)
                        .queryParam("actor", "alice")
                        .queryParam("roles", "analyst")
                        .queryParam("action", "view")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.allowed").isEqualTo(false)
                .jsonPath("$.data.effect").isEqualTo("DENY")
                .jsonPath("$.data.matchedSubjectType").isEqualTo("USER")
                .jsonPath("$.data.matchedSubjectId").isEqualTo("alice")
                .jsonPath("$.data.signature").value(signature ->
                        assertThat((String) signature).contains("DASHBOARD:300:VIEW:DENY:USER:alice"));

        client.post()
                .uri("/canvas/bi/permissions/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(permissionGrantJson(301, "ROLE", "analyst", "allow"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/permissions/effective-access")
                        .queryParam("workspaceId", WORKSPACE_ID)
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceId", 301)
                        .queryParam("actor", "bob")
                        .queryParam("roles", "analyst")
                        .queryParam("action", "view")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.allowed").isEqualTo(true)
                .jsonPath("$.data.matchedSubjectType").isEqualTo("ROLE")
                .jsonPath("$.data.matchedSubjectId").isEqualTo("analyst")
                .jsonPath("$.data.signature").value(signature ->
                        assertThat((String) signature).contains("DASHBOARD:301:VIEW:ALLOW:ROLE:analyst"));
    }

    @Test
    void resourceFavoriteRoutesPreserveLegacyEnvelopeTenantActorScopeDedupeFilteringAndIdempotentDelete() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/resources/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": " Dashboard ",
                          "resourceKey": " Marketing Overview ",
                          "title": "Marketing overview"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.actor").isEqualTo(ACTOR)
                .jsonPath("$.data.resourceType").isEqualTo("DASHBOARD")
                .jsonPath("$.data.resourceKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.title").isEqualTo("Marketing overview");

        client.post()
                .uri("/canvas/bi/resources/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "marketing-overview",
                          "title": "Marketing overview duplicate"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.title").isEqualTo("Marketing overview");

        client.post()
                .uri("/canvas/bi/resources/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "chart",
                          "resourceKey": "Orders Trend",
                          "title": "Orders trend"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/resources/favorites")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].resourceType").isEqualTo("CHART")
                .jsonPath("$.data[0].resourceKey").isEqualTo("orders-trend")
                .jsonPath("$.data[1].resourceType").isEqualTo("DASHBOARD")
                .jsonPath("$.data[1].resourceKey").isEqualTo("marketing-overview");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/favorites")
                        .queryParam("resourceType", "dashboard")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].resourceKey").isEqualTo("marketing-overview");

        client.delete()
                .uri("/canvas/bi/resources/favorites/dashboard/marketing-overview")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        client.delete()
                .uri("/canvas/bi/resources/favorites/dashboard/marketing-overview")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0);

        client.get()
                .uri("/canvas/bi/resources/favorites")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].resourceKey").isEqualTo("orders-trend");
    }

    @Test
    void permissionAdministrationRoutesPreserveLegacyEnvelopeTenantScopeAndReviewFlow() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);

        client.post()
                .uri("/canvas/bi/permissions/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": " Dashboard ",
                          "resourceKey": " Marketing Overview ",
                          "subjectType": "role",
                          "subjectId": "analyst",
                          "actionKey": "view",
                          "effect": "allow"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.resourceType").isEqualTo("DASHBOARD")
                .jsonPath("$.data.resourceKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.subjectType").isEqualTo("ROLE")
                .jsonPath("$.data.actionKey").isEqualTo("VIEW")
                .jsonPath("$.data.effect").isEqualTo("ALLOW")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/permissions/rows")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "datasetKey": " Orders Daily ",
                          "ruleKey": " CN Only ",
                          "subjectType": "role",
                          "subjectId": "analyst",
                          "filters": [{"field":"country","op":"EQ","value":"CN"}]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.ruleKey").isEqualTo("cn-only")
                .jsonPath("$.data.filterJson").value(value ->
                        assertThat((String) value).contains("country"));

        client.post()
                .uri("/canvas/bi/permissions/columns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "datasetKey": " Orders Daily ",
                          "fieldKey": " Customer Phone ",
                          "subjectType": "user",
                          "subjectId": "alice",
                          "policy": "mask",
                          "mask": {"strategy":"last4"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.fieldKey").isEqualTo("customer-phone")
                .jsonPath("$.data.policy").isEqualTo("MASK")
                .jsonPath("$.data.maskJson").value(value ->
                        assertThat((String) value).contains("last4"));

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/permissions/resources")
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceKey", "Marketing Overview")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].resourceKey").isEqualTo("marketing-overview");

        client.get()
                .uri("/canvas/bi/permissions/rows?datasetKey=Orders Daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].datasetKey").isEqualTo("orders-daily");

        client.get()
                .uri("/canvas/bi/permissions/columns?datasetKey=Orders Daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].fieldKey").isEqualTo("customer-phone");

        client.post()
                .uri("/canvas/bi/permissions/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview",
                          "requestedAction": "export",
                          "reason": "Need campaign export"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.requestedBy").isEqualTo(ACTOR);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/permissions/requests")
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceKey", "Marketing Overview")
                        .queryParam("status", "pending")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].requestedAction").isEqualTo("EXPORT");

        client.post()
                .uri("/canvas/bi/permissions/requests/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "status": "approved",
                          "reviewComment": "go ahead"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.reviewedBy").isEqualTo(ACTOR)
                .jsonPath("$.data.grantedPermissionId").exists();

        client.delete()
                .uri("/canvas/bi/permissions/resources/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0);
        client.delete()
                .uri("/canvas/bi/permissions/resources/1")
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/permissions/audit?limit=3")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(3)
                .jsonPath("$.data[0].actionKey").isEqualTo("BI_PERMISSION_REQUEST_REVIEW");
    }

    @Test
    void subscriptionDeliveryRoutesPreserveLegacyEnvelopeAndFinalModuleState() {
        TestFixture fixture = new TestFixture();
        WebTestClient client = WebTestClient.bindToController(new BiCatalogController(fixture.facade)).build();

        client.post()
                .uri("/canvas/bi/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "subscriptionKey": " Daily Revenue ",
                          "name": "Daily revenue",
                          "resourceType": " dashboard ",
                          "resourceKey": " Marketing Overview ",
                          "resourceId": 300,
                          "schedule": {"cron": "0 8 * * *"},
                          "receivers": {"email": ["ops@example.com"]},
                          "delivery": {"channel": "email"},
                          "enabled": true
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.subscriptionKey").isEqualTo("daily-revenue")
                .jsonPath("$.data.resourceType").isEqualTo("DASHBOARD")
                .jsonPath("$.data.resourceKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/alerts")
                .header("X-Actor", "operator")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "alertKey": " GMV Spike ",
                          "name": "GMV spike",
                          "datasetKey": " Orders Daily ",
                          "metricKey": " Gross GMV ",
                          "condition": {"op": "GT", "value": 1000},
                          "receivers": {"email": ["growth@example.com"]},
                          "enabled": true
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.alertKey").isEqualTo("gmv-spike")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.metricKey").isEqualTo("gross-gmv")
                .jsonPath("$.data.createdBy").isEqualTo("operator");

        client.post()
                .uri("/canvas/bi/subscriptions/101/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.jobType").isEqualTo("SUBSCRIPTION")
                .jsonPath("$.data.status").isEqualTo("TRIGGERED");
        client.post()
                .uri("/canvas/bi/alerts/201/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.jobType").isEqualTo("ALERT");

        client.post()
                .uri("/canvas/bi/delivery-logs/retry?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.checked").isEqualTo(2)
                .jsonPath("$.data.retried").isEqualTo(2);

        client.get()
                .uri("/canvas/bi/delivery-logs?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(4)
                .jsonPath("$.data[0].status").isEqualTo("DELIVERED");

        client.get()
                .uri("/canvas/bi/delivery-audit?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(4)
                .jsonPath("$.data.triggered").isEqualTo(2)
                .jsonPath("$.data.delivered").isEqualTo(2);

        client.get()
                .uri("/canvas/bi/delivery-attachments?jobType=subscription&jobId=101&limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].attachmentKey").isEqualTo("subscription-101");

        client.get()
                .uri("/canvas/bi/delivery-attachments/401/download")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/plain")
                .expectHeader().valueMatches("Content-Disposition", "attachment; filename=.*daily-revenue.*")
                .expectBody(byte[].class)
                .value(bytes -> assertThat(new String(bytes)).isEqualTo("delivery-401"));

        client.post()
                .uri("/canvas/bi/delivery-scheduler/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.subscriptionsChecked").isEqualTo(1)
                .jsonPath("$.data.alertsChecked").isEqualTo(1);

        client.delete()
                .uri("/canvas/bi/subscriptions/101")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0);
        client.delete()
                .uri("/canvas/bi/alerts/201")
                .exchange()
                .expectStatus().isOk();
    }

    private static WebTestClient webClient(BiCatalogFacade facade) {
        return WebTestClient.bindToController(new BiControllerAdapter(facade)).build();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object value) {
        return (List<String>) value;
    }

    private static <T> List<T> limited(List<T> values, Integer limit) {
        if (limit == null || limit < 0 || limit >= values.size()) {
            return values;
        }
        return values.subList(0, limit);
    }

    private static TestFixture fixtureWithWorkspaceAndDataset(String datasetKey, String status) {
        TestFixture fixture = new TestFixture();
        WebTestClient client = webClient(fixture.facade);
        client.post()
                .uri("/canvas/bi/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workspaceJson())
                .exchange()
                .expectStatus().isOk();
        client.post()
                .uri("/canvas/bi/datasets/resources/" + datasetKey + "/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetDraftJson(status))
                .exchange()
                .expectStatus().isOk();
        return fixture;
    }

    private static TestFixture fixtureWithWorkspaceDatasetAndChart() {
        TestFixture fixture = fixtureWithWorkspaceAndDataset("orders-daily", "published");
        webClient(fixture.facade)
                .post()
                .uri("/canvas/bi/charts/resources/orders-trend/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson("Orders trend", "orders-daily", "published"))
                .exchange()
                .expectStatus().isOk();
        return fixture;
    }

    private static TestFixture fixtureWithWorkspaceDatasetChartAndDashboard() {
        TestFixture fixture = fixtureWithWorkspaceDatasetAndChart();
        webClient(fixture.facade)
                .post()
                .uri("/canvas/bi/dashboards/resources/marketing-overview/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceId": 5,
                          "name": "Marketing overview",
                          "description": "Executive daily view",
                          "theme": {"mode": "light"},
                          "filters": {"region": "CN"},
                          "chartKeys": ["orders-trend"],
                          "status": "published"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();
        return fixture;
    }

    private static String workspaceJson() {
        return """
                {
                  "workspaceKey": "growth-team",
                  "name": "Growth workspace",
                  "description": "Growth BI workspace",
                  "status": "published"
                }
                """;
    }

    private static String datasetDraftJson(String status) {
        return """
                {
                  "workspaceId": 5,
                  "name": "Orders daily",
                  "datasetType": "sql",
                  "sourceRefId": 99,
                  "tableExpression": "fact_order",
                  "tenantColumn": "tenant_id",
                  "model": {"grain": "day"},
                  "fields": [
                    {
                      "fieldKey": "order_date",
                      "displayName": "Order date",
                      "columnExpression": "order_date",
                      "roleKey": "DIMENSION",
                      "dataType": "DATE",
                      "defaultAggregation": "NONE",
                      "visible": true,
                      "sortOrder": 1
                    }
                  ],
                  "metrics": [
                    {
                      "metricKey": "gmv",
                      "displayName": "GMV",
                      "expression": "sum(pay_amount)",
                      "aggregation": "SUM",
                      "dataType": "DECIMAL",
                      "unit": "CNY"
                    }
                  ],
                  "status": "%s"
                }
                """.formatted(status);
    }

    private static String chartDraftJson(String name, String datasetKey, String status) {
        return """
                {
                  "workspaceId": 5,
                  "name": "%s",
                  "chartType": "line",
                  "datasetKey": "%s",
                  "query": {"dimensions": ["order_date"], "metrics": ["gmv"]},
                  "style": {"palette": "ops"},
                  "interaction": {"drilldown": true},
                  "status": "%s"
                }
                """.formatted(name, datasetKey, status);
    }

    private static String dashboardDraftJson(String name, String status) {
        return """
                {
                  "workspaceId": 5,
                  "name": "%s",
                  "description": "Executive daily view",
                  "theme": {"mode": "light"},
                  "filters": {"region": "CN"},
                  "chartKeys": ["orders-trend"],
                  "status": "%s"
                }
                """.formatted(name, status);
    }

    private static String spreadsheetDraftJson(String name, String status) {
        return """
                {
                  "spreadsheetKey": "body-key-ignored",
                  "name": "%s",
                  "description": "Finance workbook",
                  "sheets": [{"sheetKey": " Daily ", "title": "Daily revenue"}],
                  "dataBinding": {"datasetKey": "orders-daily"},
                  "style": {"theme": "compact"},
                  "status": "%s"
                }
                """.formatted(name, status);
    }

    private static String permissionGrantJson(String subjectType, String subjectId, String effect) {
        return permissionGrantJson(300, subjectType, subjectId, effect);
    }

    private static String permissionGrantJson(long resourceId, String subjectType, String subjectId, String effect) {
        return """
                {
                  "workspaceId": 5,
                  "resourceType": "dashboard",
                  "resourceId": %d,
                  "subjectType": "%s",
                  "subjectId": "%s",
                  "actionKey": "view",
                  "effect": "%s"
                }
                """.formatted(resourceId, subjectType, subjectId, effect);
    }

    @RestController
    private static final class BiControllerAdapter {
        private final BiCatalogFacade facade;

        private BiControllerAdapter(BiCatalogFacade facade) {
            this.facade = facade;
        }

        @PostMapping("/canvas/bi/workspaces")
        Mono<CompatibilityEnvelope<BiWorkspaceView>> upsertWorkspace(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody WorkspaceRequest request) {
            return envelope(() -> facade.upsertWorkspace(
                    tenantIdOrDefault(tenantId),
                    new BiWorkspaceCommand(
                            request.workspaceKey(),
                            request.name(),
                            request.description(),
                            request.status()),
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/datasets/resources/{datasetKey}/draft")
        Mono<CompatibilityEnvelope<BiDatasetView>> saveDatasetDraft(
                @PathVariable String datasetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody DatasetDraftRequest request) {
            return envelope(() -> facade.upsertDataset(
                    tenantIdOrDefault(tenantId),
                    request.toCommand(datasetKey),
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/charts/resources/{chartKey}/draft")
        Mono<CompatibilityEnvelope<BiChartView>> saveChartDraft(
                @PathVariable String chartKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody ChartDraftRequest request) {
            return envelope(() -> facade.upsertChart(
                    tenantIdOrDefault(tenantId),
                    request.toCommand(chartKey),
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/dashboards/resources/{dashboardKey}/draft")
        Mono<CompatibilityEnvelope<BiDashboardView>> saveDashboardDraft(
                @PathVariable String dashboardKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody DashboardDraftRequest request) {
            return envelope(() -> facade.upsertDashboard(
                    tenantIdOrDefault(tenantId),
                    request.toCommand(dashboardKey),
                    actorOrDefault(actor)));
        }

        @GetMapping(value = "/canvas/bi/dashboards/resources", params = "!workspaceId")
        Mono<CompatibilityEnvelope<List<BiDashboardView>>> listDashboardResources(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listDashboardResources(tenantIdOrDefault(tenantId)));
        }

        @GetMapping(value = "/canvas/bi/dashboards/resources/{dashboardKey}", params = "!workspaceId")
        Mono<CompatibilityEnvelope<BiDashboardView>> getDashboardResource(
                @PathVariable String dashboardKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.getDashboardResource(tenantIdOrDefault(tenantId), dashboardKey));
        }

        @GetMapping(value = "/canvas/bi/dashboards/resources/{dashboardKey}", params = "workspaceId")
        Mono<CompatibilityEnvelope<BiDashboardReadModelView>> dashboardReadModel(
                @PathVariable String dashboardKey,
                @RequestParam Long workspaceId,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.dashboardReadModel(tenantIdOrDefault(tenantId), workspaceId, dashboardKey));
        }

        @GetMapping("/canvas/bi/datasets/resources")
        Mono<CompatibilityEnvelope<List<BiDatasetView>>> listDatasetResources(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listDatasetResources(tenantIdOrDefault(tenantId)));
        }

        @GetMapping("/canvas/bi/datasets/resources/{datasetKey}")
        Mono<CompatibilityEnvelope<BiDatasetView>> getDatasetResource(
                @PathVariable String datasetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.getDatasetResource(tenantIdOrDefault(tenantId), datasetKey));
        }

        @GetMapping("/canvas/bi/datasets")
        Mono<CompatibilityEnvelope<List<BiQueryDatasetView>>> listQueryDatasets(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listQueryDatasets(tenantIdOrDefault(tenantId)));
        }

        @GetMapping("/canvas/bi/datasets/{datasetKey}")
        Mono<CompatibilityEnvelope<BiQueryDatasetView>> getQueryDataset(
                @PathVariable String datasetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.getQueryDataset(tenantIdOrDefault(tenantId), datasetKey));
        }

        @GetMapping("/canvas/bi/dashboards/presets")
        Mono<CompatibilityEnvelope<List<BiDashboardPresetView>>> listDashboardPresets(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listDashboardPresets(tenantIdOrDefault(tenantId)));
        }

        @GetMapping("/canvas/bi/dashboards/presets/{dashboardKey}")
        Mono<CompatibilityEnvelope<BiDashboardPresetView>> getDashboardPreset(
                @PathVariable String dashboardKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.getDashboardPreset(tenantIdOrDefault(tenantId), dashboardKey));
        }

        @GetMapping("/canvas/bi/capacity/quick-engine")
        Mono<CompatibilityEnvelope<BiQuickEngineCapacitySummaryView>> quickEngineCapacity(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(defaultValue = "50") Integer limit) {
            return envelope(() -> facade.quickEngineCapacity(tenantIdOrDefault(tenantId), limit));
        }

        @GetMapping("/canvas/bi/capacity/quick-engine/queue")
        Mono<CompatibilityEnvelope<BiQuickEngineQueueSnapshotView>> quickEngineQueue(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) String poolKey,
                @RequestParam(required = false) String status,
                @RequestParam(defaultValue = "50") Integer limit) {
            return envelope(() -> facade.quickEngineQueue(tenantIdOrDefault(tenantId), poolKey, status, limit));
        }

        @PostMapping("/canvas/bi/resources/favorites")
        Mono<CompatibilityEnvelope<BiResourceFavoriteView>> favoriteResource(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiResourceFavoriteCommand command) {
            return envelope(() -> facade.favoriteResource(
                    tenantIdOrDefault(tenantId),
                    command,
                    actorOrDefault(actor)));
        }

        @GetMapping("/canvas/bi/resources/favorites")
        Mono<CompatibilityEnvelope<List<BiResourceFavoriteView>>> listFavoriteResources(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestParam(required = false) String resourceType) {
            return envelope(() -> facade.listFavoriteResources(
                    tenantIdOrDefault(tenantId),
                    actorOrDefault(actor),
                    resourceType));
        }

        @DeleteMapping("/canvas/bi/resources/favorites/{resourceType}/{resourceKey}")
        Mono<CompatibilityEnvelope<Void>> unfavoriteResource(
                @PathVariable String resourceType,
                @PathVariable String resourceKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> {
                facade.unfavoriteResource(tenantIdOrDefault(tenantId), actorOrDefault(actor), resourceType,
                        resourceKey);
                return null;
            });
        }

        @PostMapping("/canvas/bi/capacity/quick-engine/alert-policy")
        Mono<CompatibilityEnvelope<BiQuickEngineCapacityAlertPolicyView>> updateQuickEngineCapacityAlertPolicy(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiQuickEngineCapacityAlertPolicyCommand command) {
            return envelope(() -> facade.updateQuickEngineCapacityAlertPolicy(
                    tenantIdOrDefault(tenantId),
                    command,
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/capacity/quick-engine/tenant-pool-policy")
        Mono<CompatibilityEnvelope<BiQuickEngineTenantPoolPolicyView>> updateQuickEngineTenantPoolPolicy(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiQuickEngineTenantPoolPolicyCommand command) {
            return envelope(() -> facade.updateQuickEngineTenantPoolPolicy(
                    tenantIdOrDefault(tenantId),
                    command,
                    actorOrDefault(actor)));
        }

        @GetMapping("/canvas/bi/charts/resources")
        Mono<CompatibilityEnvelope<List<BiChartView>>> listChartResources(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listChartResources(tenantIdOrDefault(tenantId)));
        }

        @GetMapping("/canvas/bi/charts/resources/{chartKey}")
        Mono<CompatibilityEnvelope<BiChartView>> getChartResource(
                @PathVariable String chartKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.getChartResource(tenantIdOrDefault(tenantId), chartKey));
        }

        @GetMapping("/canvas/bi/charts/resources/{chartKey}/impact")
        Mono<CompatibilityEnvelope<BiChartReferenceImpactView>> chartReferenceImpact(
                @PathVariable String chartKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.chartReferenceImpact(tenantIdOrDefault(tenantId), chartKey));
        }

        @PostMapping("/canvas/bi/charts/resources/{chartKey}/publish")
        Mono<CompatibilityEnvelope<BiChartView>> publishChartResource(
                @PathVariable String chartKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> facade.publishChartResource(
                    tenantIdOrDefault(tenantId),
                    chartKey,
                    actorOrDefault(actor)));
        }

        @DeleteMapping("/canvas/bi/charts/resources/{chartKey}")
        Mono<CompatibilityEnvelope<Void>> archiveChartResource(
                @PathVariable String chartKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> {
                facade.archiveChartResource(tenantIdOrDefault(tenantId), chartKey, actorOrDefault(actor));
                return null;
            });
        }

        @GetMapping("/canvas/bi/charts/resources/{chartKey}/versions")
        Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listChartResourceVersions(
                @PathVariable String chartKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listChartResourceVersions(tenantIdOrDefault(tenantId), chartKey));
        }

        @PostMapping("/canvas/bi/charts/resources/{chartKey}/versions/{version}/restore")
        Mono<CompatibilityEnvelope<BiChartView>> restoreChartResourceVersion(
                @PathVariable String chartKey,
                @PathVariable Integer version,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> facade.restoreChartResourceVersion(
                    tenantIdOrDefault(tenantId),
                    chartKey,
                    version,
                    actorOrDefault(actor)));
        }

        @GetMapping("/canvas/bi/spreadsheets/resources")
        Mono<CompatibilityEnvelope<List<BiSpreadsheetResourceView>>> listSpreadsheetResources(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.listSpreadsheetResources(tenantIdOrDefault(tenantId)));
        }

        @GetMapping("/canvas/bi/spreadsheets/resources/{spreadsheetKey}")
        Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> getSpreadsheetResource(
                @PathVariable String spreadsheetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> facade.getSpreadsheetResource(tenantIdOrDefault(tenantId), spreadsheetKey));
        }

        @PostMapping("/canvas/bi/spreadsheets/resources/{spreadsheetKey}/draft")
        Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> saveSpreadsheetDraft(
                @PathVariable String spreadsheetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody SpreadsheetDraftRequest request) {
            return envelope(() -> facade.saveSpreadsheetDraft(
                    tenantIdOrDefault(tenantId),
                    spreadsheetKey,
                    request.toCommand(),
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/spreadsheets/resources/{spreadsheetKey}/publish")
        Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> publishSpreadsheetResource(
                @PathVariable String spreadsheetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> facade.publishSpreadsheetResource(
                    tenantIdOrDefault(tenantId),
                    spreadsheetKey,
                    actorOrDefault(actor)));
        }

        @DeleteMapping("/canvas/bi/spreadsheets/resources/{spreadsheetKey}")
        Mono<CompatibilityEnvelope<Void>> archiveSpreadsheetResource(
                @PathVariable String spreadsheetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> {
                facade.archiveSpreadsheetResource(tenantIdOrDefault(tenantId), spreadsheetKey, actorOrDefault(actor));
                return null;
            });
        }

        @GetMapping("/canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions")
        Mono<CompatibilityEnvelope<List<BiResourceVersionView>>> listSpreadsheetResourceVersions(
                @PathVariable String spreadsheetKey,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) Integer limit) {
            return envelope(() -> limited(
                    facade.listSpreadsheetResourceVersions(tenantIdOrDefault(tenantId), spreadsheetKey),
                    limit));
        }

        @PostMapping("/canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions/{version}/restore")
        Mono<CompatibilityEnvelope<BiSpreadsheetResourceView>> restoreSpreadsheetResourceVersion(
                @PathVariable String spreadsheetKey,
                @PathVariable Integer version,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> facade.restoreSpreadsheetResourceVersion(
                    tenantIdOrDefault(tenantId),
                    spreadsheetKey,
                    version,
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/permissions/resources")
        Mono<CompatibilityEnvelope<BiResourcePermissionView>> upsertResourcePermission(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiResourcePermissionCommand command) {
            return envelope(() -> facade.upsertResourcePermission(
                    tenantIdOrDefault(tenantId),
                    command,
                    actorOrDefault(actor)));
        }

        @GetMapping("/canvas/bi/permissions/resources")
        Mono<CompatibilityEnvelope<List<BiResourcePermissionView>>> listResourcePermissions(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) String resourceType,
                @RequestParam(required = false) String resourceKey,
                @RequestParam(required = false) Long resourceId) {
            return envelope(() -> facade.listResourcePermissions(
                    tenantIdOrDefault(tenantId),
                    resourceType,
                    resourceKey,
                    resourceId));
        }

        @DeleteMapping("/canvas/bi/permissions/resources/{id}")
        Mono<CompatibilityEnvelope<Void>> deleteResourcePermission(
                @PathVariable Long id,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> {
                facade.deleteResourcePermission(tenantIdOrDefault(tenantId), actorOrDefault(actor), id);
                return null;
            });
        }

        @GetMapping("/canvas/bi/permissions/rows")
        Mono<CompatibilityEnvelope<List<BiRowPermissionView>>> listRowPermissions(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) String datasetKey) {
            return envelope(() -> facade.listRowPermissions(tenantIdOrDefault(tenantId), datasetKey));
        }

        @PostMapping("/canvas/bi/permissions/rows")
        Mono<CompatibilityEnvelope<BiRowPermissionView>> upsertRowPermission(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiRowPermissionCommand command) {
            return envelope(() -> facade.upsertRowPermission(tenantIdOrDefault(tenantId), command,
                    actorOrDefault(actor)));
        }

        @DeleteMapping("/canvas/bi/permissions/rows/{id}")
        Mono<CompatibilityEnvelope<Void>> deleteRowPermission(
                @PathVariable Long id,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> {
                facade.deleteRowPermission(tenantIdOrDefault(tenantId), actorOrDefault(actor), id);
                return null;
            });
        }

        @GetMapping("/canvas/bi/permissions/columns")
        Mono<CompatibilityEnvelope<List<BiColumnPermissionView>>> listColumnPermissions(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) String datasetKey) {
            return envelope(() -> facade.listColumnPermissions(tenantIdOrDefault(tenantId), datasetKey));
        }

        @PostMapping("/canvas/bi/permissions/columns")
        Mono<CompatibilityEnvelope<BiColumnPermissionView>> upsertColumnPermission(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiColumnPermissionCommand command) {
            return envelope(() -> facade.upsertColumnPermission(tenantIdOrDefault(tenantId), command,
                    actorOrDefault(actor)));
        }

        @DeleteMapping("/canvas/bi/permissions/columns/{id}")
        Mono<CompatibilityEnvelope<Void>> deleteColumnPermission(
                @PathVariable Long id,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor) {
            return envelope(() -> {
                facade.deleteColumnPermission(tenantIdOrDefault(tenantId), actorOrDefault(actor), id);
                return null;
            });
        }

        @GetMapping("/canvas/bi/permissions/audit")
        Mono<CompatibilityEnvelope<List<BiPermissionAuditEntryView>>> permissionAudit(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(defaultValue = "20") int limit) {
            return envelope(() -> facade.permissionAudit(tenantIdOrDefault(tenantId), limit));
        }

        @GetMapping("/canvas/bi/permissions/requests")
        Mono<CompatibilityEnvelope<List<BiPermissionRequestView>>> listPermissionRequests(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) String resourceType,
                @RequestParam(required = false) String resourceKey,
                @RequestParam(required = false) String status) {
            return envelope(() -> facade.listPermissionRequests(
                    tenantIdOrDefault(tenantId),
                    resourceType,
                    resourceKey,
                    status));
        }

        @PostMapping("/canvas/bi/permissions/requests")
        Mono<CompatibilityEnvelope<BiPermissionRequestView>> requestPermission(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiPermissionRequestCommand command) {
            return envelope(() -> facade.requestPermission(
                    tenantIdOrDefault(tenantId),
                    command,
                    actorOrDefault(actor)));
        }

        @PostMapping("/canvas/bi/permissions/requests/{id}/review")
        Mono<CompatibilityEnvelope<BiPermissionRequestView>> reviewPermissionRequest(
                @PathVariable Long id,
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @RequestBody BiPermissionRequestReviewCommand command) {
            BiPermissionRequestReviewCommand merged = new BiPermissionRequestReviewCommand(
                    id,
                    command == null ? null : command.status(),
                    command == null ? null : command.reviewComment());
            return envelope(() -> facade.reviewPermissionRequest(
                    tenantIdOrDefault(tenantId),
                    merged,
                    actorOrDefault(actor)));
        }

        @GetMapping("/canvas/bi/permissions/effective-access")
        Mono<CompatibilityEnvelope<BiPermissionDecisionView>> effectiveAccess(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam Long workspaceId,
                @RequestParam String resourceType,
                @RequestParam Long resourceId,
                @RequestParam String actor,
                @RequestParam(value = "roles", required = false) List<String> roles,
                @RequestParam("action") String action) {
            return envelope(() -> facade.effectiveAccess(new BiAccessRequest(
                    tenantIdOrDefault(tenantId),
                    workspaceId,
                    resourceType,
                    resourceId,
                    actor,
                    roles == null ? Set.of() : Set.copyOf(roles),
                    action)));
        }

        private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
            return Mono.fromCallable(() -> {
                try {
                    return CompatibilityEnvelope.ok(supplier.get());
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
                }
            });
        }

        @ExceptionHandler(ResponseStatusException.class)
        ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
            int status = exception.getStatusCode().value();
            String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(CompatibilityEnvelope.fail("API_001", status, message));
        }

        private static Long tenantIdOrDefault(Long tenantId) {
            return tenantId == null ? TENANT_ID : tenantId;
        }

        private static String actorOrDefault(String actor) {
            return actor == null || actor.isBlank() ? ACTOR : actor.trim();
        }
    }

    private record WorkspaceRequest(
            String workspaceKey,
            String name,
            String description,
            String status) {
    }

    private record DatasetDraftRequest(
            Long workspaceId,
            String datasetKey,
            String name,
            String datasetType,
            Long sourceRefId,
            String tableExpression,
            String tenantColumn,
            Map<String, Object> model,
            List<DatasetFieldRequest> fields,
            List<MetricRequest> metrics,
            String status) {

        private BiDatasetCommand toCommand(String pathDatasetKey) {
            return new BiDatasetCommand(
                    workspaceId,
                    pathDatasetKey,
                    name,
                    datasetType,
                    sourceRefId,
                    tableExpression,
                    tenantColumn,
                    model,
                    fields == null ? List.of() : fields.stream().map(DatasetFieldRequest::toCommand).toList(),
                    metrics == null ? List.of() : metrics.stream().map(MetricRequest::toCommand).toList(),
                    status);
        }
    }

    private record DatasetFieldRequest(
            String fieldKey,
            String displayName,
            String columnExpression,
            String roleKey,
            String dataType,
            String defaultAggregation,
            Boolean visible,
            Integer sortOrder) {

        private BiDatasetFieldCommand toCommand() {
            return new BiDatasetFieldCommand(
                    fieldKey,
                    displayName,
                    columnExpression,
                    roleKey,
                    dataType,
                    defaultAggregation,
                    visible,
                    sortOrder);
        }
    }

    private record MetricRequest(
            String metricKey,
            String displayName,
            String expression,
            String aggregation,
            String dataType,
            String unit) {

        private BiMetricCommand toCommand() {
            return new BiMetricCommand(metricKey, displayName, expression, aggregation, dataType, unit);
        }
    }

    private record ChartDraftRequest(
            Long workspaceId,
            String chartKey,
            String name,
            String chartType,
            String datasetKey,
            Map<String, Object> query,
            Map<String, Object> style,
            Map<String, Object> interaction,
            String status) {

        private BiChartCommand toCommand(String pathChartKey) {
            return new BiChartCommand(
                    workspaceId,
                    pathChartKey,
                    name,
                    chartType,
                    datasetKey,
                    query,
                    style,
                    interaction,
                    status);
        }
    }

    private record DashboardDraftRequest(
            Long workspaceId,
            String dashboardKey,
            String name,
            String description,
            Map<String, Object> theme,
            Map<String, Object> filters,
            List<String> chartKeys,
            String status) {

        private BiDashboardCommand toCommand(String pathDashboardKey) {
            return new BiDashboardCommand(
                    workspaceId,
                    pathDashboardKey,
                    name,
                    description,
                    theme,
                    filters,
                    chartKeys,
                    status);
        }
    }

    private record SpreadsheetDraftRequest(
            String spreadsheetKey,
            String name,
            String description,
            List<Map<String, Object>> sheets,
            Map<String, Object> dataBinding,
            Map<String, Object> style,
            String status) {

        private BiSpreadsheetResourceCommand toCommand() {
            return new BiSpreadsheetResourceCommand(
                    spreadsheetKey,
                    name,
                    description,
                    sheets == null ? List.of() : sheets,
                    dataBinding == null ? Map.of() : dataBinding,
                    style == null ? Map.of() : style,
                    status);
        }
    }

    private record PermissionGrantRequest(
            Long workspaceId,
            String resourceType,
            Long resourceId,
            String subjectType,
            String subjectId,
            String actionKey,
            String effect) {

        private BiPermissionGrantCommand toCommand() {
            return new BiPermissionGrantCommand(
                    workspaceId,
                    resourceType,
                    resourceId,
                    subjectType,
                    subjectId,
                    actionKey,
                    effect);
        }
    }

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }

    private static final class TestFixture {
        private final InMemoryBiRepository repository = new InMemoryBiRepository();
        private final BiCatalogFacade facade = new BiCatalogApplicationService(
                repository,
                repository,
                repository,
                repository,
                repository);
    }

    private static final class InMemoryBiRepository implements BiWorkspaceRepository, BiDatasetRepository,
            BiChartRepository, BiDashboardRepository, BiPermissionRepository {
        private final Map<Long, BiWorkspace> workspacesById = new LinkedHashMap<>();
        private final Map<Long, BiDataset> datasetsById = new LinkedHashMap<>();
        private final Map<Long, BiChart> chartsById = new LinkedHashMap<>();
        private final Map<Long, BiDashboard> dashboardsById = new LinkedHashMap<>();
        private final List<BiPermissionGrant> grants = new ArrayList<>();
        private long nextWorkspaceId = WORKSPACE_ID;
        private long nextDatasetId = 100L;
        private long nextChartId = 200L;
        private long nextDashboardId = 300L;
        private long nextGrantId = 400L;

        @Override
        public BiWorkspace findWorkspace(Long tenantId, Long workspaceId) {
            BiWorkspace workspace = workspacesById.get(workspaceId);
            return workspace == null || !tenantId.equals(workspace.tenantId()) ? null : workspace;
        }

        @Override
        public BiWorkspace saveWorkspace(BiWorkspace workspace) {
            BiWorkspace saved = workspace.id() == null ? workspace.withId(nextWorkspaceId++) : workspace;
            workspacesById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiDataset findDatasetByKey(Long tenantId, Long workspaceId, BiResourceKey datasetKey) {
            return datasetsById.values().stream()
                    .filter(dataset -> tenantId.equals(dataset.tenantId()))
                    .filter(dataset -> workspaceId.equals(dataset.workspaceId()))
                    .filter(dataset -> dataset.datasetKey().equals(datasetKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiDataset findDatasetById(Long tenantId, Long datasetId) {
            BiDataset dataset = datasetsById.get(datasetId);
            return dataset == null || !tenantId.equals(dataset.tenantId()) ? null : dataset;
        }

        @Override
        public List<BiDataset> listAvailableDatasets(Long tenantId) {
            return datasetsById.values().stream()
                    .filter(dataset -> tenantId.equals(dataset.tenantId()))
                    .filter(dataset -> dataset.status() != BiResourceStatus.ARCHIVED)
                    .sorted((left, right) -> {
                        int updated = right.updatedAt().compareTo(left.updatedAt());
                        if (updated != 0) {
                            return updated;
                        }
                        return left.datasetKey().value().compareTo(right.datasetKey().value());
                    })
                    .toList();
        }

        @Override
        public BiDataset findAvailableDatasetByKeyWithTenantFallback(Long tenantId, BiResourceKey datasetKey) {
            BiDataset tenantDataset = findAvailableDatasetByKey(tenantId, datasetKey);
            return tenantDataset == null ? findAvailableDatasetByKey(0L, datasetKey) : tenantDataset;
        }

        private BiDataset findAvailableDatasetByKey(Long tenantId, BiResourceKey datasetKey) {
            return datasetsById.values().stream()
                    .filter(dataset -> tenantId.equals(dataset.tenantId()))
                    .filter(dataset -> dataset.datasetKey().equals(datasetKey))
                    .filter(dataset -> dataset.status() != BiResourceStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiDataset saveDataset(BiDataset dataset) {
            BiDataset saved = dataset.id() == null ? dataset.withId(nextDatasetId++) : dataset;
            datasetsById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiChart findChartByKey(Long tenantId, Long workspaceId, BiResourceKey chartKey) {
            return chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> workspaceId.equals(chart.workspaceId()))
                    .filter(chart -> chart.chartKey().equals(chartKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<BiChart> listChartsByKeys(Long tenantId, Long workspaceId, List<BiResourceKey> chartKeys) {
            Map<BiResourceKey, BiChart> charts = chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> workspaceId.equals(chart.workspaceId()))
                    .collect(Collectors.toMap(BiChart::chartKey, chart -> chart, (left, right) -> left));
            return chartKeys.stream()
                    .map(charts::get)
                    .filter(chart -> chart != null)
                    .toList();
        }

        @Override
        public List<BiChart> listAvailableCharts(Long tenantId) {
            return chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> chart.status() != BiResourceStatus.ARCHIVED)
                    .sorted((left, right) -> {
                        int updated = right.updatedAt().compareTo(left.updatedAt());
                        if (updated != 0) {
                            return updated;
                        }
                        return left.chartKey().value().compareTo(right.chartKey().value());
                    })
                    .toList();
        }

        @Override
        public BiChart findAvailableChartByKey(Long tenantId, BiResourceKey chartKey) {
            return chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> chart.chartKey().equals(chartKey))
                    .filter(chart -> chart.status() != BiResourceStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiChart saveChart(BiChart chart) {
            BiChart saved = chart.id() == null ? chart.withId(nextChartId++) : chart;
            chartsById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiDashboard findDashboardByKey(Long tenantId, Long workspaceId, BiResourceKey dashboardKey) {
            return dashboardsById.values().stream()
                    .filter(dashboard -> tenantId.equals(dashboard.tenantId()))
                    .filter(dashboard -> workspaceId.equals(dashboard.workspaceId()))
                    .filter(dashboard -> dashboard.dashboardKey().equals(dashboardKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<BiDashboard> listAvailableDashboards(Long tenantId) {
            return dashboardsById.values().stream()
                    .filter(dashboard -> tenantId.equals(dashboard.tenantId()))
                    .filter(dashboard -> dashboard.status() != BiResourceStatus.ARCHIVED)
                    .sorted((left, right) -> {
                        int updated = right.updatedAt().compareTo(left.updatedAt());
                        if (updated != 0) {
                            return updated;
                        }
                        return left.dashboardKey().value().compareTo(right.dashboardKey().value());
                    })
                    .toList();
        }

        @Override
        public BiDashboard findAvailableDashboardByKey(Long tenantId, BiResourceKey dashboardKey) {
            return dashboardsById.values().stream()
                    .filter(dashboard -> tenantId.equals(dashboard.tenantId()))
                    .filter(dashboard -> dashboard.dashboardKey().equals(dashboardKey))
                    .filter(dashboard -> dashboard.status() != BiResourceStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiDashboard saveDashboard(BiDashboard dashboard) {
            BiDashboard saved = dashboard.id() == null ? dashboard.withId(nextDashboardId++) : dashboard;
            dashboardsById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiPermissionGrant saveGrant(BiPermissionGrant grant) {
            BiPermissionGrant saved = grant.id() == null ? grant.withId(nextGrantId++) : grant;
            grants.removeIf(existing -> saved.id().equals(existing.id()));
            grants.add(saved);
            return saved;
        }

        @Override
        public void deleteGrant(Long tenantId,
                                Long workspaceId,
                                String resourceType,
                                Long resourceId,
                                String subjectType,
                                String subjectId,
                                String actionKey) {
            grants.removeIf(grant -> tenantId.equals(grant.tenantId())
                    && workspaceId.equals(grant.workspaceId())
                    && resourceType.equals(grant.resourceType())
                    && resourceId.equals(grant.resourceId())
                    && subjectType.equals(grant.subjectType())
                    && subjectId.equals(grant.subjectId())
                    && actionKey.equals(grant.actionKey()));
        }

        @Override
        public List<BiPermissionGrant> listResourceGrants(Long tenantId,
                                                          Long workspaceId,
                                                          String resourceType,
                                                          Long resourceId) {
            return grants.stream()
                    .filter(grant -> tenantId.equals(grant.tenantId()))
                    .filter(grant -> workspaceId.equals(grant.workspaceId()))
                    .filter(grant -> resourceType.equals(grant.resourceType()))
                    .filter(grant -> resourceId.equals(grant.resourceId()))
                    .toList();
        }
    }
}
