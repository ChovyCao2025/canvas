package org.chovy.canvas.web.bi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.bi.api.BiCatalogFacade;
import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartDashboardReferenceView;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetFilterView;
import org.chovy.canvas.bi.api.BiDashboardPresetInteractionView;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardPresetWidgetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardReadinessView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldView;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiMetricView;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiPermissionGrantView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEnginePoolView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueItemView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiQueryFieldView;
import org.chovy.canvas.bi.api.BiQueryMetricView;
import org.chovy.canvas.bi.api.BiWorkspaceCommand;
import org.chovy.canvas.bi.api.BiWorkspaceView;
import org.chovy.canvas.bi.domain.BiAccessRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class BiCatalogControllerCompatibilityTest {

    private static final Long TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final Long WORKSPACE_ID = 5L;
    private static final String ACTOR = "analyst";
    private static final String HEADER_ACTOR = "operator";
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-06-12T13:14:08");

    @Test
    void catalogRoutesPreserveStableEnvelopePathKeysAndTenantActorHeaders() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/workspaces")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceKey": "growth-team",
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
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.workspaceKey").isEqualTo("growth-team")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/datasets/resources/orders-daily/draft")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.fields[0].fieldKey").isEqualTo("order-date")
                .jsonPath("$.data.metrics[0].metricKey").isEqualTo("gmv")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/draft")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/dashboards/resources/marketing-overview/draft")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dashboardDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.chartKeys[0]").isEqualTo("orders-trend")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/dashboards/resources/marketing-overview")
                        .queryParam("workspaceId", WORKSPACE_ID)
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboard.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.charts[0].chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.datasets[0].datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.readiness.status").isEqualTo("READY")
                .jsonPath("$.data.readiness.productionReady").isEqualTo(true);

        client.post()
                .uri("/canvas/bi/permissions/resources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(permissionGrantJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.resourceType").isEqualTo("dashboard")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

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
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.allowed").isEqualTo(true)
                .jsonPath("$.data.matchedSubjectType").isEqualTo("ROLE")
                .jsonPath("$.data.signature").isEqualTo("DASHBOARD:300:VIEW:alice:[ANALYST]");

        assertThat(facade.tenantIds).containsOnly(HEADER_TENANT_ID);
        assertThat(facade.actors).containsExactly(
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR,
                HEADER_ACTOR);
        assertThat(facade.datasetCommand.datasetKey()).isEqualTo("orders-daily");
        assertThat(facade.chartCommand.chartKey()).isEqualTo("orders-trend");
        assertThat(facade.dashboardCommand.dashboardKey()).isEqualTo("marketing-overview");
        assertThat(facade.accessRequest.roles()).containsExactly("ANALYST");
    }

    @Test
    void catalogRoutesUseCompatibilityDefaultsWhenTenantAndActorHeadersAreAbsent() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .post()
                .uri("/canvas/bi/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceKey": "growth-team",
                          "name": "Growth workspace",
                          "description": "Growth BI workspace",
                          "status": "published"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        assertThat(facade.tenantIds).containsExactly(TENANT_ID);
        assertThat(facade.actors).containsExactly(ACTOR);
    }

    @Test
    void datasetResourceListRoutePreservesEnvelopeDefaultTenantAndDoesNotRequireWorkspaceId() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri("/canvas/bi/datasets/resources")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].datasetKey").isEqualTo("orders-daily");

        assertThat(facade.tenantIds).containsExactly(TENANT_ID);
        assertThat(facade.listDatasetResourcesCalls).isEqualTo(1);
    }

    @Test
    void datasetResourceListRouteUsesTenantHeaderWithoutWorkspaceId() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri("/canvas/bi/datasets/resources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue());

        assertThat(facade.tenantIds).containsExactly(HEADER_TENANT_ID);
        assertThat(facade.listDatasetResourcesCalls).isEqualTo(1);
    }

    @Test
    void datasetResourceDetailRoutePreservesEnvelopeAndDatasetKeyLookup() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri("/canvas/bi/datasets/resources/orders-daily")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily");

        assertThat(facade.tenantIds).containsExactly(HEADER_TENANT_ID);
        assertThat(facade.detailDatasetKeys).containsExactly("orders-daily");
    }

    @Test
    void queryDatasetCatalogRoutesExposeCompactBuiltInCatalogAndBadRequestForUnknownKey() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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
                .jsonPath("$.data[0].datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data[0].tableExpression").doesNotExist()
                .jsonPath("$.data[0].tenantColumn").doesNotExist()
                .jsonPath("$.data[0].fields[*].fieldKey").value(keys ->
                        assertThat(asStringList(keys)).contains("stat_date", "canvas_name", "trigger_type"))
                .jsonPath("$.data[0].fields[0].fieldKey").isEqualTo("canvas_name")
                .jsonPath("$.data[0].fields[0].columnExpression").doesNotExist()
                .jsonPath("$.data[0].metrics[*].metricKey").value(keys ->
                        assertThat(asStringList(keys)).contains("total_executions", "success_rate"))
                .jsonPath("$.data[0].metrics[0].expression").doesNotExist();

        client.get()
                .uri("/canvas/bi/datasets/canvas_daily_stats")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
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

        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID, TENANT_ID);
        assertThat(facade.listQueryDatasetsCalls).isEqualTo(1);
        assertThat(facade.queryDatasetKeys).containsExactly("canvas_daily_stats", "missing_dataset");
    }

    @Test
    void dashboardPresetCatalogRoutesExposeCompactBuiltInCatalogAndBadRequestForUnknownKey() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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
                .jsonPath("$.data[0].dashboardKey").isEqualTo("canvas-effect")
                .jsonPath("$.data[0].datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data[0].widgets[0].widgetKey").isEqualTo("trend-executions")
                .jsonPath("$.data[0].widgets[0].chartType").isEqualTo("LINE")
                .jsonPath("$.data[0].filters[0].filterKey").isEqualTo("filter-stat-date")
                .jsonPath("$.data[0].filters[1].parentFilterKeys[0]").isEqualTo("filter-stat-date")
                .jsonPath("$.data[0].interactions[0].interactionKey").isEqualTo("open-canvas-stats")
                .jsonPath("$.data[0].subscriptionChannels[0]").isEqualTo("EMAIL")
                .jsonPath("$.data[0].embedScopes[0]").isEqualTo("INTERNAL_CANVAS");

        client.get()
                .uri("/canvas/bi/dashboards/presets/canvas-effect")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboardKey").isEqualTo("canvas-effect")
                .jsonPath("$.data.title").isEqualTo("画布效果分析")
                .jsonPath("$.data.widgets[*].widgetKey").value(keys ->
                        assertThat(asStringList(keys)).contains("trend-executions", "detail-canvas"))
                .jsonPath("$.data.filters[*].filterKey").value(keys ->
                        assertThat(asStringList(keys)).contains("filter-stat-date", "filter-canvas"))
                .jsonPath("$.data.interactions[*].interactionType").value(types ->
                        assertThat(asStringList(types)).contains("FILTER_LINKAGE", "HYPERLINK"));

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

        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID, TENANT_ID);
        assertThat(facade.listDashboardPresetCalls).isEqualTo(1);
        assertThat(facade.dashboardPresetKeys).containsExactly("canvas-effect", "missing-dashboard");
    }

    @Test
    void quickEngineCapacityRoutePreservesEnvelopeDefaultTenantAndLimit() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri("/canvas/bi/capacity/quick-engine")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.alertLevel").isEqualTo("NORMAL")
                .jsonPath("$.data.alertEnabled").isEqualTo(false)
                .jsonPath("$.data.overLimit").doesNotExist()
                .jsonPath("$.data.tenantPoolPolicy.poolKey").isEqualTo("STANDARD")
                .jsonPath("$.data.concurrencyQueue.runningQueries").isEqualTo(2)
                .jsonPath("$.data.details[0].resourceKey").isEqualTo("canvas_daily_stats");

        assertThat(facade.tenantIds).containsExactly(TENANT_ID);
        assertThat(facade.capacityLimits).containsExactly(50);
    }

    @Test
    void quickEngineQueueRoutePreservesOptionalFiltersTenantHeaderAndLimit() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/capacity/quick-engine/queue")
                        .queryParam("poolKey", "gold")
                        .queryParam("status", "queued")
                        .queryParam("limit", 25)
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.poolKey").isEqualTo("GOLD")
                .jsonPath("$.data.queued").isEqualTo(3)
                .jsonPath("$.data.claimed").isEqualTo(2)
                .jsonPath("$.data.total").isEqualTo(15)
                .jsonPath("$.data.running").doesNotExist()
                .jsonPath("$.data.failed").doesNotExist()
                .jsonPath("$.data.jobs[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.jobs[0].poolKey").isEqualTo("GOLD")
                .jsonPath("$.data.jobs[0].status").isEqualTo("QUEUED")
                .jsonPath("$.data.jobs[0].attemptCount").isEqualTo(0)
                .jsonPath("$.data.jobs[0].expiresAt").exists()
                .jsonPath("$.data.jobs[0].attempts").doesNotExist()
                .jsonPath("$.data.jobs[0].visibleAt").doesNotExist()
                .jsonPath("$.data.jobs[0].failureReason").doesNotExist();

        assertThat(facade.tenantIds).containsExactly(HEADER_TENANT_ID);
        assertThat(facade.queueFilters).containsExactly("gold|queued|25");
    }

    @Test
    void quickEnginePolicyRoutesPreserveEnvelopeLegacyJsonNamesAndHeaderDefaults() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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
                .jsonPath("$.data.warningThresholdPercent").isEqualTo(70)
                .jsonPath("$.data.criticalThresholdPercent").isEqualTo(90)
                .jsonPath("$.data.notificationChannels[0]").isEqualTo("EMAIL")
                .jsonPath("$.data.notificationReceivers[0]").isEqualTo("bi-ops")
                .jsonPath("$.data.receivers").doesNotExist()
                .jsonPath("$.data.updatedBy").isEqualTo(ACTOR)
                .jsonPath("$.data.updatedAt").exists();

        client.post()
                .uri("/canvas/bi/capacity/quick-engine/tenant-pool-policy")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "poolKey": " gold_pool ",
                          "maxConcurrentQueries": 16,
                          "queueTimeoutSeconds": 300,
                          "poolWeight": 250
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.poolKey").isEqualTo("GOLD_POOL")
                .jsonPath("$.data.maxConcurrentQueries").isEqualTo(16)
                .jsonPath("$.data.queueLimit").isEqualTo(50)
                .jsonPath("$.data.queueTimeoutSeconds").isEqualTo(300)
                .jsonPath("$.data.poolWeight").isEqualTo(250)
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.updatedAt").exists();

        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.actors).containsExactly(ACTOR, HEADER_ACTOR);
        assertThat(facade.alertPolicyCommand.enabled()).isNull();
        assertThat(facade.alertPolicyCommand.notificationChannels()).containsExactly(" email ", "LARK", "EMAIL", " ");
        assertThat(facade.tenantPoolPolicyCommand.poolKey()).isEqualTo(" gold_pool ");
    }

    @Test
    void chartResourceReadRoutesPreserveCompactLegacyEnvelopeWithoutWorkspaceId() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri("/canvas/bi/charts/resources")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].chartKey").isEqualTo("orders-trend");

        webClient(facade)
                .get()
                .uri("/canvas/bi/charts/resources/orders-trend")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily");

        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.listChartResourcesCalls).isEqualTo(1);
        assertThat(facade.detailChartKeys).containsExactly("orders-trend");
    }

    @Test
    void chartImpactRoutePreservesCompactLegacyEnvelopeDefaultTenantAndFields() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
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
                .jsonPath("$.data.dashboards[0].dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.dashboards[0].title").isEqualTo("Marketing overview")
                .jsonPath("$.data.dashboards[0].widgetKey").isEqualTo("orders-trend")
                .jsonPath("$.data.dashboards[0].widgetTitle").isEqualTo("Orders trend")
                .jsonPath("$.data.dashboards[0].status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.portals").isArray()
                .jsonPath("$.data.portals.length()").isEqualTo(0)
                .jsonPath("$.data.subscriptions").isArray()
                .jsonPath("$.data.subscriptions.length()").isEqualTo(0);

        assertThat(facade.tenantIds).containsExactly(TENANT_ID);
        assertThat(facade.impactChartKeys).containsExactly("orders-trend");
    }

    @Test
    void chartImpactIllegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        facade.failChartImpact = true;

        webClient(facade)
                .get()
                .uri("/canvas/bi/charts/resources/missing-chart/impact")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI chart not found: missing-chart")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        facade.failCharts = true;

        webClient(facade)
                .post()
                .uri("/canvas/bi/charts/resources/orders-trend/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("dataset is not available for BI chart")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void datasetResourceDetailIllegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        facade.failDatasetDetail = true;

        webClient(facade)
                .get()
                .uri("/canvas/bi/datasets/resources/missing-dataset")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI dataset not found")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void chartResourceDetailIllegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        facade.failChartDetail = true;

        webClient(facade)
                .get()
                .uri("/canvas/bi/charts/resources/missing-chart")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI chart not found")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void dashboardResourceReadRoutesPreserveCompactLegacyEnvelopeWithoutWorkspaceId() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].dashboardKey").isEqualTo("marketing-overview");

        client.get()
                .uri("/canvas/bi/dashboards/resources/marketing-overview")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.chartKeys[0]").isEqualTo("orders-trend");

        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.listDashboardResourcesCalls).isEqualTo(1);
        assertThat(facade.detailDashboardKeys).containsExactly("marketing-overview");
        assertThat(facade.readModelDashboardKeys).isEmpty();
    }

    @Test
    void dashboardReadModelRouteStillRequiresWorkspaceIdQueryParam() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/dashboards/resources/marketing-overview")
                        .queryParam("workspaceId", WORKSPACE_ID)
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.dashboard.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.charts[0].chartKey").isEqualTo("orders-trend");

        assertThat(facade.tenantIds).containsExactly(HEADER_TENANT_ID);
        assertThat(facade.detailDashboardKeys).isEmpty();
        assertThat(facade.readModelDashboardKeys).containsExactly("marketing-overview");
    }

    @Test
    void dashboardResourceDetailIllegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        facade.failDashboardDetail = true;

        webClient(facade)
                .get()
                .uri("/canvas/bi/dashboards/resources/missing-dashboard")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("BI dashboard not found")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void resourceFavoriteRoutesPreserveEnvelopeDefaultsFilteringAndIdempotentDelete() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/favorites")
                        .queryParam("resourceType", "dashboard")
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data[0].actor").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data[0].resourceType").isEqualTo("DASHBOARD")
                .jsonPath("$.data[0].resourceKey").isEqualTo("marketing-overview");

        client.delete()
                .uri("/canvas/bi/resources/favorites/dashboard/marketing-overview")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.favoriteCommand.resourceType()).isEqualTo(" Dashboard ");
        assertThat(facade.favoriteCommand.resourceKey()).isEqualTo(" Marketing Overview ");
        assertThat(facade.favoriteResourceTypeFilters).containsExactly("dashboard");
        assertThat(facade.unfavoriteKeys).containsExactly("dashboard|marketing-overview");
        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.actors).containsExactly(ACTOR, HEADER_ACTOR, HEADER_ACTOR);
    }

    private static WebTestClient webClient(BiCatalogFacade facade) {
        return WebTestClient.bindToController(new BiCatalogController(facade)).build();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object value) {
        return (List<String>) value;
    }

    private static String datasetDraftJson() {
        return """
                {
                  "workspaceId": 5,
                  "datasetKey": "body-key",
                  "name": "Orders daily",
                  "datasetType": "sql",
                  "sourceRefId": 99,
                  "tableExpression": "fact_order",
                  "tenantColumn": "tenant_id",
                  "model": {"grain": "day"},
                  "fields": [
                    {
                      "fieldKey": "order-date",
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
                  "status": "draft"
                }
                """;
    }

    private static String chartDraftJson() {
        return """
                {
                  "workspaceId": 5,
                  "chartKey": "body-key",
                  "name": "Orders trend",
                  "chartType": "line",
                  "datasetKey": "orders-daily",
                  "query": {"dimensions": ["order_date"], "metrics": ["gmv"]},
                  "style": {"palette": "ops"},
                  "interaction": {"drilldown": true},
                  "status": "published"
                }
                """;
    }

    private static String dashboardDraftJson() {
        return """
                {
                  "workspaceId": 5,
                  "dashboardKey": "body-key",
                  "name": "Marketing overview",
                  "description": "Executive daily view",
                  "theme": {"mode": "light"},
                  "filters": {"region": "CN"},
                  "chartKeys": ["orders-trend"],
                  "status": "draft"
                }
                """;
    }

    private static String permissionGrantJson() {
        return """
                {
                  "workspaceId": 5,
                  "resourceType": "dashboard",
                  "resourceId": 300,
                  "subjectType": "ROLE",
                  "subjectId": "analyst",
                  "actionKey": "view",
                  "effect": "allow"
                }
                """;
    }

    private static final class RecordingBiCatalogFacade implements BiCatalogFacade {
        private final List<Long> tenantIds = new ArrayList<>();
        private final List<String> actors = new ArrayList<>();
        private BiDatasetCommand datasetCommand;
        private BiChartCommand chartCommand;
        private BiDashboardCommand dashboardCommand;
        private BiAccessRequest accessRequest;
        private final List<String> detailDatasetKeys = new ArrayList<>();
        private final List<String> detailChartKeys = new ArrayList<>();
        private final List<String> impactChartKeys = new ArrayList<>();
        private final List<String> detailDashboardKeys = new ArrayList<>();
        private final List<String> readModelDashboardKeys = new ArrayList<>();
        private final List<String> queryDatasetKeys = new ArrayList<>();
        private final List<String> dashboardPresetKeys = new ArrayList<>();
        private final List<Integer> capacityLimits = new ArrayList<>();
        private final List<String> queueFilters = new ArrayList<>();
        private final List<String> favoriteResourceTypeFilters = new ArrayList<>();
        private final List<String> unfavoriteKeys = new ArrayList<>();
        private BiQuickEngineCapacityAlertPolicyCommand alertPolicyCommand;
        private BiQuickEngineTenantPoolPolicyCommand tenantPoolPolicyCommand;
        private BiResourceFavoriteCommand favoriteCommand;
        private int listDatasetResourcesCalls;
        private int listQueryDatasetsCalls;
        private int listChartResourcesCalls;
        private int listDashboardResourcesCalls;
        private int listDashboardPresetCalls;
        private boolean failCharts;
        private boolean failDatasetDetail;
        private boolean failChartDetail;
        private boolean failChartImpact;
        private boolean failDashboardDetail;

        @Override
        public BiWorkspaceView upsertWorkspace(Long tenantId, BiWorkspaceCommand command, String actor) {
            recordMutation(tenantId, actor);
            return new BiWorkspaceView(
                    1L,
                    tenantId,
                    command.workspaceKey(),
                    command.name(),
                    command.description(),
                    command.status(),
                    actor,
                    NOW,
                    NOW);
        }

        @Override
        public BiDatasetView upsertDataset(Long tenantId, BiDatasetCommand command, String actor) {
            recordMutation(tenantId, actor);
            datasetCommand = command;
            return new BiDatasetView(
                    100L,
                    tenantId,
                    command.workspaceId(),
                    command.datasetKey(),
                    command.name(),
                    command.datasetType(),
                    command.sourceRefId(),
                    command.tableExpression(),
                    command.tenantColumn(),
                    command.model(),
                    command.fields().stream().map(this::fieldView).toList(),
                    command.metrics().stream().map(this::metricView).toList(),
                    command.status(),
                    actor,
                    NOW,
                    NOW);
        }

        @Override
        public List<BiDatasetView> listDatasetResources(Long tenantId) {
            tenantIds.add(tenantId);
            listDatasetResourcesCalls++;
            return List.of(datasetView(tenantId, sampleDatasetCommand(), HEADER_ACTOR));
        }

        @Override
        public BiDatasetView getDatasetResource(Long tenantId, String datasetKey) {
            if (failDatasetDetail) {
                throw new IllegalArgumentException("BI dataset not found");
            }
            tenantIds.add(tenantId);
            detailDatasetKeys.add(datasetKey);
            return datasetView(tenantId, sampleDatasetCommand(datasetKey), HEADER_ACTOR);
        }

        @Override
        public List<BiQueryDatasetView> listQueryDatasets(Long tenantId) {
            tenantIds.add(tenantId);
            listQueryDatasetsCalls++;
            return List.of(queryDatasetView("canvas_daily_stats"));
        }

        @Override
        public BiQueryDatasetView getQueryDataset(Long tenantId, String datasetKey) {
            tenantIds.add(tenantId);
            queryDatasetKeys.add(datasetKey);
            if (!"canvas_daily_stats".equals(datasetKey)) {
                throw new IllegalArgumentException("Unknown BI dataset: " + datasetKey);
            }
            return queryDatasetView(datasetKey);
        }

        @Override
        public List<BiDashboardPresetView> listDashboardPresets(Long tenantId) {
            tenantIds.add(tenantId);
            listDashboardPresetCalls++;
            return List.of(dashboardPresetView("canvas-effect"));
        }

        @Override
        public BiDashboardPresetView getDashboardPreset(Long tenantId, String dashboardKey) {
            tenantIds.add(tenantId);
            dashboardPresetKeys.add(dashboardKey);
            if (!"canvas-effect".equals(dashboardKey)) {
                throw new IllegalArgumentException("Unknown BI dashboard preset: " + dashboardKey);
            }
            return dashboardPresetView(dashboardKey);
        }

        @Override
        public BiQuickEngineCapacitySummaryView quickEngineCapacity(Long tenantId, Integer limit) {
            tenantIds.add(tenantId);
            capacityLimits.add(limit);
            return quickEngineCapacitySummary(tenantId);
        }

        @Override
        public BiQuickEngineQueueSnapshotView quickEngineQueue(Long tenantId, String poolKey, String status,
                                                               Integer limit) {
            tenantIds.add(tenantId);
            queueFilters.add("%s|%s|%d".formatted(poolKey, status, limit));
            return quickEngineQueueSnapshot(tenantId, "GOLD");
        }

        @Override
        public BiResourceFavoriteView favoriteResource(Long tenantId, BiResourceFavoriteCommand command,
                                                       String actor) {
            recordMutation(tenantId, actor);
            favoriteCommand = command;
            return favoriteView(tenantId, actor, "DASHBOARD", "marketing-overview", command.title());
        }

        @Override
        public List<BiResourceFavoriteView> listFavoriteResources(Long tenantId, String actor, String resourceType) {
            recordMutation(tenantId, actor);
            favoriteResourceTypeFilters.add(resourceType);
            return List.of(favoriteView(tenantId, actor, "DASHBOARD", "marketing-overview", "Marketing overview"));
        }

        @Override
        public void unfavoriteResource(Long tenantId, String actor, String resourceType, String resourceKey) {
            recordMutation(tenantId, actor);
            unfavoriteKeys.add(resourceType + "|" + resourceKey);
        }

        @Override
        public BiQuickEngineCapacityAlertPolicyView updateQuickEngineCapacityAlertPolicy(
                Long tenantId,
                BiQuickEngineCapacityAlertPolicyCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            alertPolicyCommand = command;
            return new BiQuickEngineCapacityAlertPolicyView(
                    command.enabled() == null ? false : command.enabled(),
                    command.capacityLimitRows() == null ? 1_000_000L : command.capacityLimitRows(),
                    command.warningThresholdPercent() == null ? 80 : command.warningThresholdPercent(),
                    command.criticalThresholdPercent() == null ? 95 : command.criticalThresholdPercent(),
                    List.of("EMAIL", "LARK"),
                    List.of("bi-ops", "analyst"),
                    actor,
                    NOW);
        }

        @Override
        public BiQuickEngineTenantPoolPolicyView updateQuickEngineTenantPoolPolicy(
                Long tenantId,
                BiQuickEngineTenantPoolPolicyCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            tenantPoolPolicyCommand = command;
            return new BiQuickEngineTenantPoolPolicyView(
                    "GOLD_POOL",
                    command.maxConcurrentQueries() == null ? 8 : command.maxConcurrentQueries(),
                    command.queueLimit() == null ? 50 : command.queueLimit(),
                    command.queueTimeoutSeconds() == null ? 120 : command.queueTimeoutSeconds(),
                    command.poolWeight() == null ? 100 : command.poolWeight(),
                    actor,
                    NOW);
        }

        @Override
        public BiChartView upsertChart(Long tenantId, BiChartCommand command, String actor) {
            if (failCharts) {
                throw new IllegalArgumentException("dataset is not available for BI chart");
            }
            recordMutation(tenantId, actor);
            chartCommand = command;
            return chartView(tenantId, command, actor);
        }

        @Override
        public List<BiChartView> listChartResources(Long tenantId) {
            tenantIds.add(tenantId);
            listChartResourcesCalls++;
            return List.of(chartView(tenantId, sampleChartCommand(), HEADER_ACTOR));
        }

        @Override
        public BiChartView getChartResource(Long tenantId, String chartKey) {
            if (failChartDetail) {
                throw new IllegalArgumentException("BI chart not found");
            }
            tenantIds.add(tenantId);
            detailChartKeys.add(chartKey);
            return chartView(tenantId, sampleChartCommand(chartKey), HEADER_ACTOR);
        }

        @Override
        public BiChartReferenceImpactView chartReferenceImpact(Long tenantId, String chartKey) {
            if (failChartImpact) {
                throw new IllegalArgumentException("BI chart not found: " + chartKey);
            }
            tenantIds.add(tenantId);
            impactChartKeys.add(chartKey);
            return new BiChartReferenceImpactView(
                    chartKey,
                    "Orders trend",
                    "orders-daily",
                    List.of(new BiChartDashboardReferenceView(
                            "marketing-overview",
                            "Marketing overview",
                            chartKey,
                            "Orders trend",
                            "PUBLISHED")),
                    List.of(),
                    List.of());
        }

        @Override
        public BiDashboardView upsertDashboard(Long tenantId, BiDashboardCommand command, String actor) {
            recordMutation(tenantId, actor);
            dashboardCommand = command;
            return dashboardView(tenantId, command, actor);
        }

        @Override
        public List<BiDashboardView> listDashboardResources(Long tenantId) {
            tenantIds.add(tenantId);
            listDashboardResourcesCalls++;
            return List.of(dashboardView(tenantId, sampleDashboardCommand(), HEADER_ACTOR));
        }

        @Override
        public BiDashboardView getDashboardResource(Long tenantId, String dashboardKey) {
            if (failDashboardDetail) {
                throw new IllegalArgumentException("BI dashboard not found");
            }
            tenantIds.add(tenantId);
            detailDashboardKeys.add(dashboardKey);
            return dashboardView(tenantId, sampleDashboardCommand(dashboardKey), HEADER_ACTOR);
        }

        @Override
        public BiDashboardReadModelView dashboardReadModel(Long tenantId, Long workspaceId, String dashboardKey) {
            tenantIds.add(tenantId);
            readModelDashboardKeys.add(dashboardKey);
            BiChartCommand chart = chartCommand == null ? sampleChartCommand() : chartCommand;
            BiDatasetCommand dataset = datasetCommand == null ? sampleDatasetCommand() : datasetCommand;
            return new BiDashboardReadModelView(
                    dashboardView(tenantId, new BiDashboardCommand(
                            workspaceId,
                            dashboardKey,
                            "Marketing overview",
                            "Executive daily view",
                            Map.of("mode", "light"),
                            Map.of("region", "CN"),
                            List.of(chart.chartKey()),
                            "draft"), HEADER_ACTOR),
                    List.of(chartView(tenantId, chart, HEADER_ACTOR)),
                    List.of(datasetView(tenantId, dataset, HEADER_ACTOR)),
                    new BiDashboardReadinessView("READY", true, 1, 0, List.of(), List.of()));
        }

        @Override
        public BiPermissionGrantView grantPermission(Long tenantId, BiPermissionGrantCommand command, String actor) {
            recordMutation(tenantId, actor);
            return new BiPermissionGrantView(
                    400L,
                    tenantId,
                    command.workspaceId(),
                    command.resourceType(),
                    command.resourceId(),
                    command.subjectType(),
                    command.subjectId(),
                    command.actionKey(),
                    command.effect(),
                    actor,
                    NOW);
        }

        @Override
        public BiPermissionDecisionView effectiveAccess(BiAccessRequest request) {
            tenantIds.add(request.tenantId());
            accessRequest = request;
            return new BiPermissionDecisionView(
                    true,
                    "ALLOW",
                    "ROLE",
                    "analyst",
                    "matched role grant",
                    "%s:%d:%s:%s:%s".formatted(
                            request.resourceType(),
                            request.resourceId(),
                            request.actionKey(),
                            request.actor(),
                            request.roles()));
        }

        private void recordMutation(Long tenantId, String actor) {
            tenantIds.add(tenantId);
            actors.add(actor);
        }

        private BiResourceFavoriteView favoriteView(Long tenantId,
                                                    String actor,
                                                    String resourceType,
                                                    String resourceKey,
                                                    String title) {
            return new BiResourceFavoriteView(
                    tenantId,
                    actor,
                    resourceType,
                    resourceKey,
                    title,
                    NOW,
                    NOW);
        }

        private BiDashboardView dashboardView(Long tenantId, BiDashboardCommand command, String actor) {
            return new BiDashboardView(
                    300L,
                    tenantId,
                    command.workspaceId(),
                    command.dashboardKey(),
                    command.name(),
                    command.description(),
                    command.theme(),
                    command.filters(),
                    command.chartKeys(),
                    command.status(),
                    1,
                    actor,
                    NOW,
                    NOW);
        }

        private BiChartView chartView(Long tenantId, BiChartCommand command, String actor) {
            return new BiChartView(
                    200L,
                    tenantId,
                    command.workspaceId(),
                    command.chartKey(),
                    command.name(),
                    command.chartType(),
                    100L,
                    command.datasetKey(),
                    command.query(),
                    command.style(),
                    command.interaction(),
                    command.status(),
                    actor,
                    NOW,
                    NOW);
        }

        private BiDatasetView datasetView(Long tenantId, BiDatasetCommand command, String actor) {
            return new BiDatasetView(
                    100L,
                    tenantId,
                    command.workspaceId(),
                    command.datasetKey(),
                    command.name(),
                    command.datasetType(),
                    command.sourceRefId(),
                    command.tableExpression(),
                    command.tenantColumn(),
                    command.model(),
                    command.fields().stream().map(this::fieldView).toList(),
                    command.metrics().stream().map(this::metricView).toList(),
                    command.status(),
                    actor,
                    NOW,
                    NOW);
        }

        private BiDatasetFieldView fieldView(BiDatasetFieldCommand command) {
            return new BiDatasetFieldView(
                    command.fieldKey(),
                    command.displayName(),
                    command.columnExpression(),
                    command.roleKey(),
                    command.dataType(),
                    command.defaultAggregation(),
                    Boolean.TRUE.equals(command.visible()),
                    command.sortOrder() == null ? 0 : command.sortOrder());
        }

        private BiMetricView metricView(BiMetricCommand command) {
            return new BiMetricView(
                    command.metricKey(),
                    command.displayName(),
                    command.expression(),
                    command.aggregation(),
                    command.dataType(),
                    command.unit());
        }

        private BiQueryDatasetView queryDatasetView(String datasetKey) {
            return new BiQueryDatasetView(
                    datasetKey,
                    List.of(
                            new BiQueryFieldView("canvas_name", "DIMENSION", "STRING"),
                            new BiQueryFieldView("stat_date", "DIMENSION", "DATE"),
                            new BiQueryFieldView("trigger_type", "DIMENSION", "STRING")),
                    List.of(
                            new BiQueryMetricView("success_rate", "PERCENT"),
                            new BiQueryMetricView("total_executions", "NUMBER")));
        }

        private BiDashboardPresetView dashboardPresetView(String dashboardKey) {
            return new BiDashboardPresetView(
                    dashboardKey,
                    "画布效果分析",
                    "QuickBI-like preset",
                    "canvas_daily_stats",
                    List.of(
                            new BiDashboardPresetWidgetView(
                                    "trend-executions",
                                    "执行趋势",
                                    "LINE",
                                    List.of("stat_date"),
                                    List.of("total_executions"),
                                    0,
                                    3,
                                    12,
                                    6,
                                    "time-series"),
                            new BiDashboardPresetWidgetView(
                                    "detail-canvas",
                                    "画布明细",
                                    "TABLE",
                                    List.of("stat_date", "canvas_name", "trigger_type"),
                                    List.of("total_executions"),
                                    0,
                                    9,
                                    20,
                                    7,
                                    "detail")),
                    List.of(
                            new BiDashboardPresetFilterView(
                                    "filter-stat-date",
                                    "stat_date",
                                    "统计日期",
                                    "DATE_RANGE",
                                    true,
                                    "LAST_7_DAYS",
                                    List.of(),
                                    List.of(),
                                    Map.of(),
                                    "SAME_SOURCE",
                                    null,
                                    null,
                                    false),
                            new BiDashboardPresetFilterView(
                                    "filter-canvas",
                                    "canvas_name",
                                    "画布名称",
                                    "SEARCH_SELECT",
                                    false,
                                    null,
                                    List.of(),
                                    List.of("filter-stat-date"),
                                    Map.of(),
                                    "SAME_SOURCE",
                                    null,
                                    null,
                                    false)),
                    List.of(
                            new BiDashboardPresetInteractionView(
                                    "open-canvas-stats",
                                    "detail-canvas",
                                    null,
                                    "HYPERLINK",
                                    "canvas_id",
                                    "/canvas/{canvas_id}/stats"),
                            new BiDashboardPresetInteractionView(
                                    "linkage-trend-to-detail",
                                    "trend-executions",
                                    "detail-canvas",
                                    "FILTER_LINKAGE",
                                    "stat_date",
                                    null)),
                    List.of("EMAIL", "LARK", "WEBHOOK"),
                    List.of("INTERNAL_CANVAS", "EXTERNAL_TICKET"));
        }

        private BiQuickEngineCapacitySummaryView quickEngineCapacitySummary(Long tenantId) {
            return new BiQuickEngineCapacitySummaryView(
                    tenantId,
                    1_000_000L,
                    420_000L,
                    42.0,
                    "NORMAL",
                    false,
                    Map.of(
                            "enabled", false,
                            "warningThresholdPercent", 80,
                            "criticalThresholdPercent", 95,
                            "notificationChannels", List.of(),
                            "notificationReceivers", List.of()),
                    new BiQuickEnginePoolView("STANDARD", 8, 50, 120, 100),
                    Map.of(
                            "runningQueries", 2,
                            "queuedQueries", 3,
                            "blockedQueries", 1,
                            "failedQueries", 0,
                            "completedQueries", 9,
                            "runningUsagePercent", 25.0,
                            "queueUsagePercent", 6.0,
                            "pressureLevel", "NORMAL"),
                    List.of(Map.of(
                            "category", "DATASET_ACCELERATION",
                            "usedRows", 420_000L,
                            "resourceCount", 2)),
                    List.of(Map.of(
                            "category", "DATASET_ACCELERATION",
                            "resourceKey", "canvas_daily_stats",
                            "usedRows", 240_000L,
                            "activeTableCount", 2,
                            "owner", "analyst")),
                    List.of(Map.of(
                            "username", "analyst",
                            "usedRows", 240_000L,
                            "resourceCount", 1,
                            "activeTableCount", 2)));
        }

        private BiQuickEngineQueueSnapshotView quickEngineQueueSnapshot(Long tenantId, String poolKey) {
            return new BiQuickEngineQueueSnapshotView(
                    tenantId,
                    poolKey,
                    3L,
                    2L,
                    9L,
                    1L,
                    15L,
                    List.of(new BiQuickEngineQueueItemView(
                            81L,
                            tenantId,
                            poolKey,
                            "hash-queue",
                            "canvas_daily_stats",
                            "analyst",
                            "QUEUED",
                            0,
                            NOW,
                            NOW.plusMinutes(2),
                            null,
                            null,
                            null,
                            null,
                            NOW,
                            NOW)));
        }

        private BiChartCommand sampleChartCommand() {
            return sampleChartCommand("orders-trend");
        }

        private BiChartCommand sampleChartCommand(String chartKey) {
            return new BiChartCommand(
                    WORKSPACE_ID,
                    chartKey,
                    "Orders trend",
                    "line",
                    "orders-daily",
                    Map.of("dimensions", List.of("order_date")),
                    Map.of("palette", "ops"),
                    Map.of("drilldown", true),
                    "published");
        }

        private BiDatasetCommand sampleDatasetCommand() {
            return sampleDatasetCommand("orders-daily");
        }

        private BiDatasetCommand sampleDatasetCommand(String datasetKey) {
            return new BiDatasetCommand(
                    WORKSPACE_ID,
                    datasetKey,
                    "Orders daily",
                    "sql",
                    99L,
                    "fact_order",
                    "tenant_id",
                    Map.of("grain", "day"),
                    List.of(new BiDatasetFieldCommand(
                            "order-date",
                            "Order date",
                            "order_date",
                            "DIMENSION",
                            "DATE",
                            "NONE",
                            true,
                            1)),
                    List.of(new BiMetricCommand(
                            "gmv",
                            "GMV",
                            "sum(pay_amount)",
                            "SUM",
                            "DECIMAL",
                            "CNY")),
                    "draft");
        }

        private BiDashboardCommand sampleDashboardCommand() {
            return sampleDashboardCommand("marketing-overview");
        }

        private BiDashboardCommand sampleDashboardCommand(String dashboardKey) {
            return new BiDashboardCommand(
                    WORKSPACE_ID,
                    dashboardKey,
                    "Marketing overview",
                    "Executive daily view",
                    Map.of("mode", "light"),
                    Map.of("region", "CN"),
                    List.of("orders-trend"),
                    "published");
        }
    }
}
