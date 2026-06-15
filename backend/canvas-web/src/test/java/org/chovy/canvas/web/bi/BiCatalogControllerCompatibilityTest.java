package org.chovy.canvas.web.bi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import org.chovy.canvas.bi.api.BiDashboardCloneCommand;
import org.chovy.canvas.bi.api.BiDashboardExportPackageView;
import org.chovy.canvas.bi.api.BiDashboardImportCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetFilterView;
import org.chovy.canvas.bi.api.BiDashboardPresetInteractionView;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardPresetWidgetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardReadinessView;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateCommand;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldView;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiMetricView;
import org.chovy.canvas.bi.api.BiAlertRuleCommand;
import org.chovy.canvas.bi.api.BiAlertRuleView;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentCleanupResult;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentDownload;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentView;
import org.chovy.canvas.bi.api.BiDeliveryAuditSummary;
import org.chovy.canvas.bi.api.BiDeliveryLogView;
import org.chovy.canvas.bi.api.BiDeliveryRetryResult;
import org.chovy.canvas.bi.api.BiDeliveryRunResult;
import org.chovy.canvas.bi.api.BiDeliverySchedulerResult;
import org.chovy.canvas.bi.api.BiDatasourceApiPreviewCommand;
import org.chovy.canvas.bi.api.BiDatasourceApiPreviewView;
import org.chovy.canvas.bi.api.BiDatasourceConnectionTestResult;
import org.chovy.canvas.bi.api.BiDatasourceConnectorView;
import org.chovy.canvas.bi.api.BiDatasourceCredentialRotationCommand;
import org.chovy.canvas.bi.api.BiDatasourceCredentialRotationView;
import org.chovy.canvas.bi.api.BiDatasourceFileMaterializationResult;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingCommand;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaPreviewView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaSnapshotView;
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
import org.chovy.canvas.bi.api.BiAiRequestCommand;
import org.chovy.canvas.bi.api.BiAiResponseView;
import org.chovy.canvas.bi.api.BiBigScreenResourceCommand;
import org.chovy.canvas.bi.api.BiBigScreenResourceView;
import org.chovy.canvas.bi.api.BiPortalResourceCommand;
import org.chovy.canvas.bi.api.BiPortalResourceView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEnginePoolView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueItemView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiDatasourceHealthSnapshotView;
import org.chovy.canvas.bi.api.BiDatasourceHealthSloView;
import org.chovy.canvas.bi.api.BiDatasourceHealthView;
import org.chovy.canvas.bi.api.BiEmbedQueryCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketCleanupResult;
import org.chovy.canvas.bi.api.BiEmbedTicketCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketPayloadView;
import org.chovy.canvas.bi.api.BiEmbedTicketVerifyCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketView;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationResult;
import org.chovy.canvas.bi.api.BiQueryCachePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryCachePolicyView;
import org.chovy.canvas.bi.api.BiQueryCacheStatsView;
import org.chovy.canvas.bi.api.BiQueryCancelResult;
import org.chovy.canvas.bi.api.BiQueryCommand;
import org.chovy.canvas.bi.api.BiQueryCompileResult;
import org.chovy.canvas.bi.api.BiQueryContractGateCommand;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiQueryExplainResult;
import org.chovy.canvas.bi.api.BiPublishApprovalCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalReviewCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalView;
import org.chovy.canvas.bi.api.BiQueryGateCommand;
import org.chovy.canvas.bi.api.BiQueryGateResult;
import org.chovy.canvas.bi.api.BiQueryGovernanceAuditEntryView;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyView;
import org.chovy.canvas.bi.api.BiQueryGovernanceSummaryView;
import org.chovy.canvas.bi.api.BiQueryHistoryDetailView;
import org.chovy.canvas.bi.api.BiQueryHistoryItemView;
import org.chovy.canvas.bi.api.BiQueryResultView;
import org.chovy.canvas.bi.api.BiResourceCommentCommand;
import org.chovy.canvas.bi.api.BiResourceCommentView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiResourceLocationCommand;
import org.chovy.canvas.bi.api.BiResourceLocationView;
import org.chovy.canvas.bi.api.BiResourceLockCommand;
import org.chovy.canvas.bi.api.BiResourceLockView;
import org.chovy.canvas.bi.api.BiResourceMoveCommand;
import org.chovy.canvas.bi.api.BiResourceOwnershipView;
import org.chovy.canvas.bi.api.BiResourceTransferCommand;
import org.chovy.canvas.bi.api.BiResourceVersionView;
import org.chovy.canvas.bi.api.BiSelfServiceExportCleanupResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportCommand;
import org.chovy.canvas.bi.api.BiSelfServiceExportDownload;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobDetailView;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobView;
import org.chovy.canvas.bi.api.BiSelfServiceExportQueueResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportRetryResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportReviewCommand;
import org.chovy.canvas.bi.api.BiSelfServicePreviewCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceView;
import org.chovy.canvas.bi.api.BiSubscriptionCommand;
import org.chovy.canvas.bi.api.BiSubscriptionView;
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
                .jsonPath("$.data.resourceType").isEqualTo("DASHBOARD")
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
    void datasourceOperationsRoutesPreserveLegacyEnvelopeDefaultsAndDelegateFinalFacade() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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
                .jsonPath("$.data[0].connectorType").isEqualTo("API_JSON");

        client.post()
                .uri("/canvas/bi/datasources/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "connectorType": " mysql ",
                          "name": "Orders Warehouse",
                          "url": "jdbc:mysql://db.internal:3306/orders",
                          "username": "report_user",
                          "password": "secret",
                          "enabled": true,
                          "connectorConfig": {"schema": "dw"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.sourceKey").isEqualTo("orders-warehouse")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.put()
                .uri("/canvas/bi/datasources/onboarding/101")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"connectorType": "postgresql", "name": "Orders Warehouse", "enabled": false}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(101)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/datasources/101/credential-rotation")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"password\": \"new-secret\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(101)
                .jsonPath("$.data.rotatedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/datasources/101/schema-preview?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tables[0].tableName").isEqualTo("orders_warehouse_sample");

        client.post()
                .uri("/canvas/bi/datasources/101/api-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"path\": \"/orders\", \"limit\": 2}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.rows[0].sourceKey").isEqualTo("orders-warehouse");

        client.post()
                .uri("/canvas/bi/datasources/101/schema-sync?limit=5")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"limit\": 2}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.syncStatus").isEqualTo("SUCCESS")
                .jsonPath("$.data.syncedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/datasources/101/schema-snapshot")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.dataSourceConfigId").isEqualTo(101);

        client.get()
                .uri("/canvas/bi/datasources/101/schema-snapshots?limit=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.length()").isEqualTo(1);

        assertThat(facade.createdDatasourceCommand.connectorType()).isEqualTo(" mysql ");
        assertThat(facade.datasourceRotationCommand.password()).isEqualTo("new-secret");
        assertThat(facade.datasourceApiPreviewCommand.path()).isEqualTo("/orders");
        assertThat(facade.datasourceIds).contains(101L);
    }

    @Test
    void remainingBiAliasRoutesPreserveLegacyEnvelopeAndDelegateExistingFacadeMethods() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/datasets/resources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/charts/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/dashboards/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dashboardDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview");

        client.post()
                .uri("/canvas/bi/portals/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "portalKey": "growth-portal",
                          "title": "Growth Portal",
                          "description": "growth",
                          "dashboardKeys": ["marketing-overview"],
                          "status": "draft"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.portalKey").isEqualTo("growth-portal");

        client.post()
                .uri("/canvas/bi/big-screens/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "screenKey": "growth-screen",
                          "title": "Growth Screen",
                          "dashboardKeys": ["marketing-overview"],
                          "status": "draft"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.screenKey").isEqualTo("growth-screen");

        client.post()
                .uri("/canvas/bi/spreadsheets/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "spreadsheetKey": "growth-sheet",
                          "name": "Growth Sheet",
                          "sheets": [{"name": "Summary"}],
                          "status": "draft"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.spreadsheetKey").isEqualTo("growth-sheet");

        client.get()
                .uri("/canvas/bi/datasources")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].sourceKey").isEqualTo("orders-warehouse");

        client.post()
                .uri("/canvas/bi/datasources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"connectorType": "mysql", "name": "Orders Warehouse", "enabled": true}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.sourceKey").isEqualTo("orders-warehouse");

        client.put()
                .uri("/canvas/bi/datasources?id=101")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"connectorType": "postgresql", "name": "Orders Warehouse", "enabled": false}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(101)
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/portals/runtime")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].portalKey").isEqualTo("growth-portal");

        client.get()
                .uri("/canvas/bi/portals/runtime/growth-portal")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.portalKey").isEqualTo("growth-portal");

        client.post()
                .uri("/canvas/bi/self-service")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"query": {"datasetKey": "orders_daily"}, "previewLimit": 2}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.datasetKey").isEqualTo("orders_daily");

        client.get()
                .uri("/canvas/bi/self-service?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo(901);

        client.post()
                .uri("/canvas/bi/embed/resources/dashboard")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"ticket": "embed-7-1", "resourceType": "DASHBOARD", "resourceKey": "marketing-overview"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.resourceKey").isEqualTo("marketing-overview");

        client.post()
                .uri("/canvas/bi/embed/resources/portal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"ticket": "embed-7-1", "resourceType": "PORTAL", "resourceKey": "growth-portal"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.portalKey").isEqualTo("growth-portal");

        client.delete().uri("/canvas/bi/charts/resources?chartKey=orders-trend")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.code").isEqualTo(0);
        client.delete().uri("/canvas/bi/dashboards/resources?dashboardKey=marketing-overview")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.code").isEqualTo(0);
        client.delete().uri("/canvas/bi/portals/resources?portalKey=growth-portal")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.code").isEqualTo(0);
        client.delete().uri("/canvas/bi/big-screens/resources?screenKey=growth-screen")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.code").isEqualTo(0);
        client.delete().uri("/canvas/bi/spreadsheets/resources?spreadsheetKey=growth-sheet")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.code").isEqualTo(0);

        assertThat(facade.tenantIds).contains(TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.actors).contains(ACTOR, HEADER_ACTOR);
        assertThat(facade.datasourceIds).contains(101L);
    }

    @Test
    void selfServiceExportRoutesPreserveLegacyEnvelopeDefaultsAndDownloadHeaders() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/self-service/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"query": {"datasetKey": "orders_daily"}, "previewLimit": 2}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.datasetKey").isEqualTo("orders_daily")
                .jsonPath("$.data.rowCount").isEqualTo(2);

        client.post()
                .uri("/canvas/bi/self-service/exports")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .header("X-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "marketing-overview",
                          "resourceId": 300,
                          "exportFormat": "csv",
                          "query": {"datasetKey": "orders_daily"},
                          "rowLimit": 500,
                          "approvalRequired": true,
                          "sensitive": false,
                          "approvalReason": "monthly close"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.status").isEqualTo("PENDING_REVIEW")
                .jsonPath("$.data.requestedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/self-service/exports?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].id").isEqualTo(901);

        client.post()
                .uri("/canvas/bi/self-service/exports/901/review")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"status\": \"approved\", \"reviewComment\": \"ship it\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.approvalStatus").isEqualTo("APPROVED");

        client.get()
                .uri("/canvas/bi/self-service/exports/901")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.job.id").isEqualTo(901)
                .jsonPath("$.data.partition.tenantId").isEqualTo(TENANT_ID.intValue());

        client.get()
                .uri("/canvas/bi/self-service/exports/901/download")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/csv")
                .expectHeader().valueMatches("Content-Disposition", "attachment; filename=\"bi-export-901.csv\"")
                .expectBody(String.class)
                .isEqualTo("id,status\n901,COMPLETED\n");

        client.post()
                .uri("/canvas/bi/self-service/exports/901/cancel")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("CANCELLED");

        client.post().uri("/canvas/bi/self-service/exports/cleanup?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.checked").isEqualTo(10);
        client.post().uri("/canvas/bi/self-service/exports/retry?limit=3")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.retried").isEqualTo(1);
        client.post().uri("/canvas/bi/self-service/exports/queue/run?limit=4")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.processed").isEqualTo(1);

        assertThat(facade.tenantIds).contains(TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.actors).contains(ACTOR, HEADER_ACTOR);
        assertThat(facade.selfServiceRoles).contains("ANALYST", "ADMIN");
        assertThat(facade.selfServiceCommand.resourceKey()).isEqualTo("marketing-overview");
        assertThat(facade.selfServiceReviewCommand.status()).isEqualTo("approved");
    }

    @Test
    void subscriptionDeliveryRoutesPreserveLegacyEnvelopeDefaultsAndDownloadHeaders() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

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
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.get()
                .uri("/canvas/bi/subscriptions?limit=5")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data[0].subscriptionKey").isEqualTo("daily-revenue");

        client.post()
                .uri("/canvas/bi/alerts")
                .header("X-Actor", HEADER_ACTOR)
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
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/subscriptions/101/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.jobType").isEqualTo("SUBSCRIPTION")
                .jsonPath("$.data.logs[0].status").isEqualTo("TRIGGERED");

        client.post()
                .uri("/canvas/bi/alerts/201/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.jobType").isEqualTo("ALERT");

        client.get()
                .uri("/canvas/bi/delivery-logs?jobType=subscription&jobId=101&limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].jobType").isEqualTo("SUBSCRIPTION");

        client.get()
                .uri("/canvas/bi/delivery-audit?status=triggered&limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.triggered").isEqualTo(1);

        client.post()
                .uri("/canvas/bi/delivery-logs/retry?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.checked").isEqualTo(1)
                .jsonPath("$.data.retried").isEqualTo(1);

        client.get()
                .uri("/canvas/bi/delivery-attachments?jobType=subscription&jobId=101&deliveryLogId=301&limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].attachmentKey").isEqualTo("subscription-101");

        client.get()
                .uri("/canvas/bi/delivery-attachments/401/download")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/plain")
                .expectHeader().valueMatches("Content-Disposition", "attachment; filename=.*delivery-401.txt.*")
                .expectBody(byte[].class)
                .value(bytes -> assertThat(bytes).isEqualTo("delivery-401".getBytes()));

        client.post()
                .uri("/canvas/bi/delivery-attachments/cleanup?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.checked").isEqualTo(1)
                .jsonPath("$.data.filesDeleted").isEqualTo(1);

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
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();
        client.delete()
                .uri("/canvas/bi/alerts/201")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.subscriptionCommand.subscriptionKey()).isEqualTo(" Daily Revenue ");
        assertThat(facade.alertCommand.alertKey()).isEqualTo(" GMV Spike ");
        assertThat(facade.subscriptionIds).contains(101L);
        assertThat(facade.alertIds).contains(201L);
        assertThat(facade.tenantIds).contains(TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.actors).contains(ACTOR, HEADER_ACTOR);
    }

    @Test
    void permissionAdministrationRoutesPreserveEnvelopeDefaultsFiltersDeletesAndRequestReview() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/permissions/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": " dashboard ",
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
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/permissions/resources")
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceKey", "Marketing Overview")
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data[0].resourceType").isEqualTo("DASHBOARD");

        client.post()
                .uri("/canvas/bi/permissions/rows")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "datasetKey": "Orders Daily",
                          "ruleKey": "CN Only",
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
                .jsonPath("$.data.ruleKey").isEqualTo("cn-only");

        client.post()
                .uri("/canvas/bi/permissions/columns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "datasetKey": "Orders Daily",
                          "fieldKey": "Customer Phone",
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
                .jsonPath("$.data.policy").isEqualTo("MASK");

        client.delete()
                .uri("/canvas/bi/permissions/resources/401")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0);
        client.delete()
                .uri("/canvas/bi/permissions/rows/402")
                .exchange()
                .expectStatus().isOk();
        client.delete()
                .uri("/canvas/bi/permissions/columns/403")
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/bi/permissions/audit?limit=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].actionKey").isEqualTo("BI_PERMISSION_CHANGE");

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

        client.post()
                .uri("/canvas/bi/permissions/requests/701/review")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
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
                .jsonPath("$.data.id").isEqualTo(701)
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.reviewedBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.resourcePermissionFilters).contains("dashboard|Marketing Overview|null");
        assertThat(facade.deletedPermissionIds).containsExactly(401L, 402L, 403L);
        assertThat(facade.permissionReviewCommand.requestId()).isEqualTo(701L);
        assertThat(facade.actors).contains(ACTOR, HEADER_ACTOR);
    }

    @Test
    void aiAssistantRoutesPreserveEnvelopeDefaultsAndOperationMapping() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/ai/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "question": "Why did GMV move?",
                          "datasetKey": " Orders Daily ",
                          "limit": 10,
                          "modelKey": "gpt-final",
                          "params": {"region": "CN"}
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
                .jsonPath("$.data.operation").isEqualTo("ASK")
                .jsonPath("$.data.assistantRunId").isEqualTo("bi-ai-7-ask")
                .jsonPath("$.data.metadata.datasetKey").isEqualTo(" Orders Daily ");

        client.post()
                .uri("/canvas/bi/ai/interpret")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "question": "Explain dashboard",
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.actor").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.operation").isEqualTo("INTERPRET")
                .jsonPath("$.data.keyFindings[0]").isEqualTo("operation=interpret");

        client.post()
                .uri("/canvas/bi/ai/report")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "title": "Weekly BI Report",
                          "sections": [{"title": "Revenue", "body": "GMV moved up"}]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.operation").isEqualTo("REPORT")
                .jsonPath("$.data.title").isEqualTo("Weekly BI Report");

        client.post()
                .uri("/canvas/bi/ai/dashboard-draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "prompt": "Draft a revenue dashboard",
                          "datasetKey": "orders-daily"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.operation").isEqualTo("DASHBOARD_DRAFT")
                .jsonPath("$.data.dashboard.dashboardKey").isEqualTo("ai-draft-orders-daily");

        client.post()
                .uri("/canvas/bi/ai/insights")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "question": "Find anomalies",
                          "metrics": {"metric": "gmv"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.operation").isEqualTo("INSIGHTS")
                .jsonPath("$.data.trends[0]").isEqualTo("Current result is prepared for trend review");

        assertThat(facade.aiOperations).containsExactly("ask", "interpret", "report", "dashboard-draft", "insights");
        assertThat(facade.tenantIds).contains(7L, HEADER_TENANT_ID);
        assertThat(facade.actors).contains(ACTOR, HEADER_ACTOR);
        assertThat(facade.aiCommand.question()).isEqualTo("Find anomalies");
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
    void datasetLifecycleRoutesPreserveLegacyResourcePathsAndHeaders() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/datasets/resources/orders-daily/publish")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/datasets/resources/orders-daily/versions?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].resourceType").isEqualTo("DATASET")
                .jsonPath("$.data[0].resourceKey").isEqualTo("orders-daily")
                .jsonPath("$.data[0].version").isEqualTo(3);

        client.post()
                .uri("/canvas/bi/datasets/resources/orders-daily/versions/2/restore")
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/datasets/resources/orders-daily/acceleration-policy")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.enabled").isEqualTo(true);

        client.post()
                .uri("/canvas/bi/datasets/resources/orders-daily/acceleration-policy")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("mode", "FULL"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.policy.mode").isEqualTo("FULL")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/datasets/resources/orders-daily/acceleration-refresh")
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.status").isEqualTo("QUEUED");

        client.get()
                .uri("/canvas/bi/datasets/resources/orders-daily/acceleration-runs?limit=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].datasetKey").isEqualTo("orders-daily");

        client.get()
                .uri("/canvas/bi/datasets/resources/orders-daily/acceleration-capacity?limit=3")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.limit").isEqualTo(3);

        client.post()
                .uri("/canvas/bi/datasets/resources/orders-daily/acceleration-cleanup?retainTables=4")
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.retainedTables").isEqualTo(4)
                .jsonPath("$.data.cleanedBy").isEqualTo(HEADER_ACTOR);

        client.delete()
                .uri("/canvas/bi/datasets/resources/orders-daily")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.status").isEqualTo("ARCHIVED");

        assertThat(facade.datasetLifecycleKeys).containsExactly(
                "publish:orders-daily",
                "versions:orders-daily:5",
                "restore:orders-daily:2",
                "policy:orders-daily",
                "upsert-policy:orders-daily:FULL",
                "refresh:orders-daily",
                "runs:orders-daily:1",
                "capacity:orders-daily:3",
                "cleanup:orders-daily:4",
                "archive:orders-daily");
    }

    @Test
    void remainingBiCollectionResourceAliasesPreserveCompatibilityEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/datasets/resources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/canvas/bi/datasets/resources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"datasetKey":"orders-daily"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.status").isEqualTo("ARCHIVED");

        client.post()
                .uri("/canvas/bi/charts/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chartDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend");

        client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/canvas/bi/charts/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"chartKey":"orders-trend"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        client.post()
                .uri("/canvas/bi/dashboards/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dashboardDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview");

        client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/canvas/bi/dashboards/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"dashboardKey":"marketing-overview"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.status").isEqualTo("ARCHIVED");

        client.post()
                .uri("/canvas/bi/portals/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(portalDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.portalKey").isEqualTo("marketing-portal");

        client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/canvas/bi/portals/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"portalKey":"marketing-portal"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        client.post()
                .uri("/canvas/bi/big-screens/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bigScreenDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.screenKey").isEqualTo("revenue-wall");

        client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/canvas/bi/big-screens/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"screenKey":"revenue-wall"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        client.post()
                .uri("/canvas/bi/spreadsheets/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(spreadsheetDraftJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet");

        client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/canvas/bi/spreadsheets/resources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"spreadsheetKey":"revenue-sheet"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.tenantIds).contains(HEADER_TENANT_ID, TENANT_ID);
        assertThat(facade.actors).contains(HEADER_ACTOR);
    }

    @Test
    void remainingBiOperationalAliasesPreserveCompatibilityEnvelope() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/datasets/resources/from-datasource-schema")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceId": 5,
                          "datasetKey": "orders-daily",
                          "name": "Orders Daily",
                          "tableName": "orders_daily",
                          "tenantColumn": "tenant_id"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily");

        client.post()
                .uri("/canvas/bi/datasets/resources/from-datasource-schema/multi-table")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "workspaceId": 5,
                          "datasetKey": "orders-multi",
                          "name": "Orders Multi",
                          "tableNames": ["orders", "payments"],
                          "tenantColumn": "tenant_id"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-multi");

        client.post()
                .uri("/canvas/bi/datasets/resources/sql-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"datasetKey":"orders-daily","sql":"select * from orders_daily"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders-daily")
                .jsonPath("$.data.rows[0].total_executions").isEqualTo(42);

        client.post()
                .uri("/canvas/bi/datasets/resources/acceleration-scheduler/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.subscriptionsTriggered").isEqualTo(1)
                .jsonPath("$.data.alertsTriggered").isEqualTo(1);

        client.get()
                .uri("/canvas/bi/datasources")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].sourceKey").isEqualTo("orders-warehouse");

        client.post()
                .uri("/canvas/bi/datasources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasourceJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.put()
                .uri("/canvas/bi/datasources")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasourceJsonWithId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(101);

        client.get()
                .uri("/canvas/bi/portals/runtime")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].portalKey").isEqualTo("marketing-portal");

        client.get()
                .uri("/canvas/bi/portals/runtime/marketing-portal")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.portalKey").isEqualTo("marketing-portal");

        client.post()
                .uri("/canvas/bi/embed/resources/dashboard")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"ticket":"embed-7-1","resourceType":"DASHBOARD","resourceKey":"marketing-overview"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview");

        client.post()
                .uri("/canvas/bi/embed/resources/dashboard/runtime-state")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"ticket":"embed-7-1","resourceType":"DASHBOARD","resourceKey":"marketing-overview"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview");

        client.post()
                .uri("/canvas/bi/embed/resources/portal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"ticket":"embed-7-2","resourceType":"PORTAL","resourceKey":"marketing-portal"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.portalKey").isEqualTo("marketing-portal");

        client.post()
                .uri("/canvas/bi/self-service")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"resourceType":"DASHBOARD","resourceKey":"marketing-overview","query":{"datasetKey":"orders_daily"}}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.datasetKey").isEqualTo("orders_daily");

        client.get()
                .uri("/canvas/bi/self-service")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].resourceKey").isEqualTo("marketing-overview");
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
    void chartLifecycleRoutesPreserveEnvelopeDefaultsPathKeysAndVersionOrdering() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/publish")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

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
                .jsonPath("$.data[1].version").isEqualTo(2);

        client.post()
                .uri("/canvas/bi/charts/resources/orders-trend/versions/1/restore")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.chartKey").isEqualTo("orders-trend")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.delete()
                .uri("/canvas/bi/charts/resources/orders-trend")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.tenantIds).contains(TENANT_ID);
        assertThat(facade.actors).contains(HEADER_ACTOR, ACTOR);
        assertThat(facade.chartLifecycleKeys).containsExactly(
                "orders-trend",
                "orders-trend",
                "orders-trend",
                "orders-trend");
        assertThat(facade.restoredChartVersion).isEqualTo(1);
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

        client.post()
                .uri("/canvas/bi/dashboards/resources/marketing-overview/publish")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.dashboardKey").isEqualTo("marketing-overview")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.tenantIds).containsExactly(TENANT_ID, HEADER_TENANT_ID, HEADER_TENANT_ID);
        assertThat(facade.listDashboardResourcesCalls).isEqualTo(1);
        assertThat(facade.detailDashboardKeys).containsExactly("marketing-overview");
        assertThat(facade.dashboardLifecycleKeys).containsExactly("publish:marketing-overview");
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
    void portalAndBigScreenLifecycleRoutesPreserveEnvelopeDefaultsAndPathKeys() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/portals/resources/marketing-portal/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "portalKey": "body-key-ignored",
                          "title": "Marketing portal",
                          "description": "Executive BI portal",
                          "dashboardKeys": [" Marketing Overview "],
                          "layout": {"columns": 12},
                          "settings": {"theme": "light"},
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
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.portalKey").isEqualTo("marketing-portal")
                .jsonPath("$.data.dashboardKeys[0]").isEqualTo("marketing-overview")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/portals/resources/marketing-portal/publish")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.portalKey").isEqualTo("marketing-portal")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/portals/resources")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].portalKey").isEqualTo("marketing-portal");

        client.get()
                .uri("/canvas/bi/portals/resources/marketing-portal")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.portalKey").isEqualTo("marketing-portal");

        client.get()
                .uri("/canvas/bi/portals/resources/marketing-portal/versions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].resourceType").isEqualTo("PORTAL")
                .jsonPath("$.data[0].resourceKey").isEqualTo("marketing-portal")
                .jsonPath("$.data[0].version").isEqualTo(2);

        client.post()
                .uri("/canvas/bi/portals/resources/marketing-portal/versions/1/restore")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.version").isEqualTo(3)
                .jsonPath("$.data.status").isEqualTo("DRAFT");

        client.delete()
                .uri("/canvas/bi/portals/resources/marketing-portal")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        client.post()
                .uri("/canvas/bi/big-screens/resources/revenue-wall/draft")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "screenKey": "body-key-ignored",
                          "title": "Revenue wall",
                          "description": "Command center",
                          "dashboardKeys": [" Marketing Overview "],
                          "layout": {"resolution": "1920x1080"},
                          "settings": {"refreshSeconds": 60},
                          "status": "published"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.screenKey").isEqualTo("revenue-wall")
                .jsonPath("$.data.dashboardKeys[0]").isEqualTo("marketing-overview")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        client.post()
                .uri("/canvas/bi/big-screens/resources/revenue-wall/publish")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.screenKey").isEqualTo("revenue-wall")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED");

        client.get()
                .uri("/canvas/bi/big-screens/resources/revenue-wall")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.screenKey").isEqualTo("revenue-wall");

        client.get()
                .uri("/canvas/bi/big-screens/resources")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].screenKey").isEqualTo("revenue-wall");

        client.get()
                .uri("/canvas/bi/big-screens/resources/revenue-wall/versions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].resourceType").isEqualTo("BIG_SCREEN")
                .jsonPath("$.data[0].resourceKey").isEqualTo("revenue-wall");

        client.post()
                .uri("/canvas/bi/big-screens/resources/revenue-wall/versions/1/restore")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.version").isEqualTo(3);

        client.delete()
                .uri("/canvas/bi/big-screens/resources/revenue-wall")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.portalDraftCommand.portalKey()).isEqualTo("body-key-ignored");
        assertThat(facade.portalKeys).containsExactly(
                "marketing-portal", "marketing-portal", "marketing-portal",
                "marketing-portal", "marketing-portal", "marketing-portal");
        assertThat(facade.bigScreenDraftCommand.screenKey()).isEqualTo("body-key-ignored");
        assertThat(facade.bigScreenKeys).containsExactly(
                "revenue-wall", "revenue-wall", "revenue-wall",
                "revenue-wall", "revenue-wall", "revenue-wall");
        assertThat(facade.tenantIds).containsExactly(
                TENANT_ID, HEADER_TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID,
                HEADER_TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID);
        assertThat(facade.actors).containsExactly(
                ACTOR, HEADER_ACTOR, ACTOR, ACTOR,
                HEADER_ACTOR, ACTOR, ACTOR, ACTOR);
    }

    @Test
    void spreadsheetLifecycleRoutesPreserveEnvelopeDefaultsAndPathKeys() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "spreadsheetKey": "body-key-ignored",
                          "name": "Revenue sheet",
                          "description": "Finance workbook",
                          "sheets": [{"sheetKey": "daily", "title": "Daily revenue"}],
                          "dataBinding": {"datasetKey": "orders-daily"},
                          "style": {"theme": "compact"},
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
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet")
                .jsonPath("$.data.sheets[0].sheetKey").isEqualTo("daily")
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet/publish")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri("/canvas/bi/spreadsheets/resources")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].spreadsheetKey").isEqualTo("revenue-sheet");

        client.get()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.spreadsheetKey").isEqualTo("revenue-sheet");

        client.get()
                .uri("/canvas/bi/spreadsheets/resources/revenue-sheet/versions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
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

        assertThat(facade.spreadsheetDraftCommand.spreadsheetKey()).isEqualTo("body-key-ignored");
        assertThat(facade.spreadsheetKeys).containsExactly(
                "revenue-sheet", "revenue-sheet", "revenue-sheet",
                "revenue-sheet", "revenue-sheet", "revenue-sheet");
        assertThat(facade.tenantIds).containsExactly(
                TENANT_ID, HEADER_TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID);
        assertThat(facade.actors).containsExactly(ACTOR, HEADER_ACTOR, ACTOR, ACTOR);
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

    @Test
    void resourceOperationsRoutesPreserveEnvelopeDefaultsHeadersAndPathOverrides() {
        RecordingBiCatalogFacade facade = new RecordingBiCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/bi/resources/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": " dashboard ",
                          "resourceKey": " Marketing Overview ",
                          "widgetKey": " Revenue Widget ",
                          "commentText": "Looks good"
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
                .jsonPath("$.data.createdBy").isEqualTo(ACTOR);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/comments")
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceKey", "Marketing Overview")
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data[0].resourceKey").isEqualTo("marketing-overview");

        client.delete()
                .uri("/canvas/bi/resources/comments/91")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        client.post()
                .uri("/canvas/bi/resources/locks/acquire")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview",
                          "lockToken": "token-1",
                          "ttlSeconds": 120
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.locked").isEqualTo(true)
                .jsonPath("$.data.lockedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/locks")
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceKey", "Marketing Overview")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.locked").isEqualTo(true);

        client.post()
                .uri("/canvas/bi/resources/locks/release")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview",
                          "lockToken": "token-1"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        client.post()
                .uri("/canvas/bi/resources/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview",
                          "folderKey": "Executive",
                          "sortOrder": 20
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.folderKey").isEqualTo("executive")
                .jsonPath("$.data.movedBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/resources/move")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "chart",
                          "resourceKey": "Orders Trend",
                          "folderKey": "Executive",
                          "sortOrder": 10
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.resourceType").isEqualTo("CHART")
                .jsonPath("$.data.resourceKey").isEqualTo("orders-trend");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/locations")
                        .queryParam("resourceType", "dashboard")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].resourceType").isEqualTo("DASHBOARD");

        client.post()
                .uri("/canvas/bi/resources/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview",
                          "ownerUser": "bi-owner"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.ownerUser").isEqualTo("bi-owner");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/ownerships")
                        .queryParam("resourceType", "dashboard")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].resourceType").isEqualTo("DASHBOARD");

        client.post()
                .uri("/canvas/bi/resources/publish-approvals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "resourceType": "dashboard",
                          "resourceKey": "Marketing Overview",
                          "reason": "Ready"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.requestedBy").isEqualTo(ACTOR);

        client.post()
                .uri("/canvas/bi/resources/publish-approvals/81/review")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "approvalId": 999,
                          "status": "approved",
                          "reviewComment": "ship it"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(81)
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.reviewedBy").isEqualTo(HEADER_ACTOR);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/bi/resources/publish-approvals")
                        .queryParam("resourceType", "dashboard")
                        .queryParam("resourceKey", "Marketing Overview")
                        .queryParam("status", "approved")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].status").isEqualTo("APPROVED");

        assertThat(facade.commentCommand.resourceKey()).isEqualTo(" Marketing Overview ");
        assertThat(facade.deletedCommentIds).containsExactly(91L);
        assertThat(facade.lockCommand.lockToken()).isEqualTo("token-1");
        assertThat(facade.locationCommand.folderKey()).isEqualTo("Executive");
        assertThat(facade.moveCommand.resourceKey()).isEqualTo("Orders Trend");
        assertThat(facade.transferCommand.ownerUser()).isEqualTo("bi-owner");
        assertThat(facade.approvalCommand.reason()).isEqualTo("Ready");
        assertThat(facade.approvalReviewCommand.approvalId()).isEqualTo(81L);
        assertThat(facade.commentFilters).containsExactly("dashboard|Marketing Overview");
        assertThat(facade.locationFilters).containsExactly("dashboard");
        assertThat(facade.ownershipFilters).containsExactly("dashboard");
        assertThat(facade.approvalFilters).containsExactly("dashboard|Marketing Overview|approved");
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

    private static String portalDraftJson() {
        return """
                {
                  "portalKey": "body-key-ignored",
                  "title": "Marketing portal",
                  "description": "Executive BI portal",
                  "dashboardKeys": ["marketing-overview"],
                  "layout": {"columns": 12},
                  "settings": {"theme": "light"},
                  "status": "draft"
                }
                """;
    }

    private static String bigScreenDraftJson() {
        return """
                {
                  "screenKey": "body-key-ignored",
                  "title": "Revenue wall",
                  "description": "Command center",
                  "dashboardKeys": ["marketing-overview"],
                  "layout": {"resolution": "1920x1080"},
                  "settings": {"refreshSeconds": 60},
                  "status": "draft"
                }
                """;
    }

    private static String spreadsheetDraftJson() {
        return """
                {
                  "spreadsheetKey": "body-key-ignored",
                  "name": "Revenue sheet",
                  "description": "Finance workbook",
                  "sheets": [{"sheetKey": "daily", "title": "Daily revenue"}],
                  "dataBinding": {"datasetKey": "orders-daily"},
                  "style": {"theme": "compact"},
                  "status": "draft"
                }
                """;
    }

    private static String datasourceJson() {
        return """
                {
                  "connectorType": "MYSQL",
                  "name": "Orders Warehouse",
                  "url": "jdbc:mysql://db.internal/orders",
                  "username": "reporter",
                  "password": "secret",
                  "sourceKey": "orders-warehouse",
                  "description": "JDBC source",
                  "enabled": true,
                  "status": "SUCCESS",
                  "connectorConfig": {"driverClassName": "com.mysql.cj.jdbc.Driver"}
                }
                """;
    }

    private static String datasourceJsonWithId() {
        return """
                {
                  "id": 101,
                  "connectorType": "MYSQL",
                  "name": "Orders Warehouse",
                  "url": "jdbc:mysql://db.internal/orders",
                  "username": "reporter",
                  "password": "secret",
                  "sourceKey": "orders-warehouse",
                  "description": "JDBC source",
                  "enabled": true,
                  "status": "SUCCESS",
                  "connectorConfig": {"driverClassName": "com.mysql.cj.jdbc.Driver"}
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
        private BiDashboardCloneCommand dashboardCloneCommand;
        private BiDashboardImportCommand dashboardImportCommand;
        private BiDashboardRuntimeStateCommand dashboardRuntimeStateCommand;
        private BiAccessRequest accessRequest;
        private final List<String> detailDatasetKeys = new ArrayList<>();
        private final List<String> detailChartKeys = new ArrayList<>();
        private final List<String> impactChartKeys = new ArrayList<>();
        private final List<String> detailDashboardKeys = new ArrayList<>();
        private final List<String> dashboardLifecycleKeys = new ArrayList<>();
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
        private BiAiRequestCommand aiCommand;
        private final List<String> aiOperations = new ArrayList<>();
        private BiResourceCommentCommand commentCommand;
        private final List<Long> deletedCommentIds = new ArrayList<>();
        private BiResourceLockCommand lockCommand;
        private BiResourceLocationCommand locationCommand;
        private BiResourceMoveCommand moveCommand;
        private BiResourceTransferCommand transferCommand;
        private BiPublishApprovalCommand approvalCommand;
        private BiPublishApprovalReviewCommand approvalReviewCommand;
        private BiResourcePermissionCommand resourcePermissionCommand;
        private BiRowPermissionCommand rowPermissionCommand;
        private BiColumnPermissionCommand columnPermissionCommand;
        private BiPermissionRequestCommand permissionRequestCommand;
        private BiPermissionRequestReviewCommand permissionReviewCommand;
        private BiSubscriptionCommand subscriptionCommand;
        private BiAlertRuleCommand alertCommand;
        private BiDatasourceOnboardingCommand createdDatasourceCommand;
        private BiDatasourceOnboardingCommand updatedDatasourceCommand;
        private BiDatasourceCredentialRotationCommand datasourceRotationCommand;
        private BiDatasourceApiPreviewCommand datasourceApiPreviewCommand;
        private BiSelfServicePreviewCommand selfServicePreviewCommand;
        private BiSelfServiceExportCommand selfServiceCommand;
        private BiSelfServiceExportReviewCommand selfServiceReviewCommand;
        private final List<String> selfServiceRoles = new ArrayList<>();
        private final List<Long> datasourceIds = new ArrayList<>();
        private BiPortalResourceCommand portalDraftCommand;
        private BiBigScreenResourceCommand bigScreenDraftCommand;
        private BiSpreadsheetResourceCommand spreadsheetDraftCommand;
        private final List<String> commentFilters = new ArrayList<>();
        private final List<String> locationFilters = new ArrayList<>();
        private final List<String> ownershipFilters = new ArrayList<>();
        private final List<String> approvalFilters = new ArrayList<>();
        private final List<String> resourcePermissionFilters = new ArrayList<>();
        private final List<String> rowPermissionFilters = new ArrayList<>();
        private final List<String> columnPermissionFilters = new ArrayList<>();
        private final List<String> permissionRequestFilters = new ArrayList<>();
        private final List<Long> deletedPermissionIds = new ArrayList<>();
        private final List<Long> subscriptionIds = new ArrayList<>();
        private final List<Long> alertIds = new ArrayList<>();
        private final List<String> chartLifecycleKeys = new ArrayList<>();
        private final List<String> datasetLifecycleKeys = new ArrayList<>();
        private Integer restoredChartVersion;
        private final List<String> portalKeys = new ArrayList<>();
        private final List<String> bigScreenKeys = new ArrayList<>();
        private final List<String> spreadsheetKeys = new ArrayList<>();
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
        public BiDatasetView publishDatasetResource(Long tenantId, String datasetKey, String actor) {
            recordMutation(tenantId, actor);
            datasetLifecycleKeys.add("publish:" + datasetKey);
            return datasetView(tenantId, sampleDatasetCommand(datasetKey, "PUBLISHED"), actor);
        }

        @Override
        public BiDatasetView archiveDatasetResource(Long tenantId, String datasetKey, String actor) {
            recordMutation(tenantId, actor);
            detailDatasetKeys.add(datasetKey);
            datasetLifecycleKeys.add("archive:" + datasetKey);
            return datasetView(tenantId, new BiDatasetCommand(
                    WORKSPACE_ID,
                    datasetKey,
                    "Archived dataset",
                    "sql",
                    99L,
                    "fact_order",
                    "tenant_id",
                    Map.of(),
                    List.of(),
                    List.of(),
                    "ARCHIVED"), actor);
        }

        @Override
        public List<BiResourceVersionView> listDatasetResourceVersions(Long tenantId, String datasetKey, int limit) {
            tenantIds.add(tenantId);
            datasetLifecycleKeys.add("versions:" + datasetKey + ":" + limit);
            return List.of(
                    new BiResourceVersionView("DATASET", datasetKey, 3, "PUBLISHED", Map.of("name", "Orders"), ACTOR,
                            NOW),
                    new BiResourceVersionView("DATASET", datasetKey, 2, "DRAFT", Map.of("name", "Orders"), ACTOR,
                            NOW));
        }

        @Override
        public BiDatasetView restoreDatasetResourceVersion(Long tenantId, String datasetKey, Integer version, String actor) {
            recordMutation(tenantId, actor);
            datasetLifecycleKeys.add("restore:" + datasetKey + ":" + version);
            return datasetView(tenantId, sampleDatasetCommand(datasetKey, "DRAFT"), actor);
        }

        @Override
        public Map<String, Object> datasetAccelerationPolicy(Long tenantId, String datasetKey) {
            tenantIds.add(tenantId);
            datasetLifecycleKeys.add("policy:" + datasetKey);
            return Map.of("tenantId", tenantId, "datasetKey", datasetKey, "enabled", true, "mode", "INCREMENTAL");
        }

        @Override
        public Map<String, Object> upsertDatasetAccelerationPolicy(
                Long tenantId,
                String datasetKey,
                Map<String, Object> command,
                String actor) {
            recordMutation(tenantId, actor);
            datasetLifecycleKeys.add("upsert-policy:" + datasetKey + ":" + command.get("mode"));
            return Map.of("tenantId", tenantId, "datasetKey", datasetKey, "policy", command, "updatedBy", actor);
        }

        @Override
        public Map<String, Object> refreshDatasetAcceleration(Long tenantId, String datasetKey, String actor) {
            recordMutation(tenantId, actor);
            datasetLifecycleKeys.add("refresh:" + datasetKey);
            return Map.of("tenantId", tenantId, "datasetKey", datasetKey, "status", "QUEUED", "triggeredBy", actor);
        }

        @Override
        public List<Map<String, Object>> listDatasetAccelerationRuns(Long tenantId, String datasetKey, int limit) {
            tenantIds.add(tenantId);
            datasetLifecycleKeys.add("runs:" + datasetKey + ":" + limit);
            return List.of(Map.of("tenantId", tenantId, "datasetKey", datasetKey, "status", "SUCCEEDED"));
        }

        @Override
        public Map<String, Object> datasetAccelerationCapacity(Long tenantId, String datasetKey, int limit) {
            tenantIds.add(tenantId);
            datasetLifecycleKeys.add("capacity:" + datasetKey + ":" + limit);
            return Map.of("tenantId", tenantId, "datasetKey", datasetKey, "limit", limit, "tables", List.of());
        }

        @Override
        public Map<String, Object> cleanupDatasetAcceleration(
                Long tenantId,
                String datasetKey,
                int retainTables,
                String actor) {
            recordMutation(tenantId, actor);
            datasetLifecycleKeys.add("cleanup:" + datasetKey + ":" + retainTables);
            return Map.of(
                    "tenantId", tenantId,
                    "datasetKey", datasetKey,
                    "retainedTables", retainTables,
                    "cleanedTables", 0,
                    "cleanedBy", actor);
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
        public BiAiResponseView aiAssistant(
                Long tenantId,
                String operation,
                BiAiRequestCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            aiOperations.add(operation);
            aiCommand = command;
            String normalizedOperation = operation.toUpperCase().replace('-', '_');
            String datasetKey = command.datasetKey();
            String dashboardKey = "ai-draft-" + (datasetKey == null ? "general" : datasetKey.trim());
            Map<String, Object> metadata = new java.util.LinkedHashMap<>();
            metadata.put("datasetKey", datasetKey);
            metadata.put("params", command.params() == null ? Map.of() : command.params());
            return new BiAiResponseView(
                    tenantId,
                    actor,
                    normalizedOperation,
                    "bi-ai-" + tenantId + "-" + operation,
                    command.question(),
                    "READY",
                    true,
                    "Compact response for " + operation,
                    metadata,
                    "Summary for " + operation,
                    List.of("operation=" + operation),
                    List.of("Review generated BI AI output"),
                    command.title() == null ? "BI AI " + normalizedOperation : command.title(),
                    command.sections() == null ? List.of() : command.sections(),
                    List.of("Review report narrative", "Attach approved dashboard evidence"),
                    Map.of("dashboardKey", dashboardKey),
                    List.of(Map.of("chartKey", dashboardKey + "-trend")),
                    List.of("Current result is prepared for trend review"),
                    List.of("No deterministic anomaly detected in compact mode"),
                    List.of("Use the insight as a draft before publishing BI decisions"));
        }

        @Override
        public BiResourceCommentView addResourceComment(Long tenantId, BiResourceCommentCommand command, String actor) {
            recordMutation(tenantId, actor);
            commentCommand = command;
            return new BiResourceCommentView(
                    91L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "revenue-widget",
                    command.commentText(),
                    actor,
                    NOW,
                    null);
        }

        @Override
        public List<BiResourceCommentView> listResourceComments(
                Long tenantId,
                String resourceType,
                String resourceKey) {
            tenantIds.add(tenantId);
            commentFilters.add(resourceType + "|" + resourceKey);
            return List.of(new BiResourceCommentView(
                    91L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "revenue-widget",
                    "Looks good",
                    ACTOR,
                    NOW,
                    null));
        }

        @Override
        public void deleteResourceComment(Long tenantId, String actor, Long commentId) {
            recordMutation(tenantId, actor);
            deletedCommentIds.add(commentId);
        }

        @Override
        public BiResourceLockView acquireResourceLock(Long tenantId, BiResourceLockCommand command, String actor) {
            recordMutation(tenantId, actor);
            lockCommand = command;
            return new BiResourceLockView(
                    71L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    command.lockToken(),
                    actor,
                    NOW,
                    NOW.plusSeconds(120),
                    true);
        }

        @Override
        public BiResourceLockView currentResourceLock(Long tenantId, String resourceType, String resourceKey) {
            tenantIds.add(tenantId);
            return new BiResourceLockView(
                    71L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "token-1",
                    HEADER_ACTOR,
                    NOW,
                    NOW.plusSeconds(120),
                    true);
        }

        @Override
        public void releaseResourceLock(Long tenantId, String actor, BiResourceLockCommand command) {
            recordMutation(tenantId, actor);
            lockCommand = command;
        }

        @Override
        public BiResourceLocationView updateResourceLocation(
                Long tenantId,
                BiResourceLocationCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            locationCommand = command;
            return new BiResourceLocationView(
                    61L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "executive",
                    command.sortOrder(),
                    actor,
                    NOW);
        }

        @Override
        public BiResourceLocationView moveResource(Long tenantId, BiResourceMoveCommand command, String actor) {
            recordMutation(tenantId, actor);
            moveCommand = command;
            return new BiResourceLocationView(
                    62L,
                    tenantId,
                    5L,
                    "CHART",
                    "orders-trend",
                    "executive",
                    command.sortOrder(),
                    actor,
                    NOW);
        }

        @Override
        public List<BiResourceLocationView> listResourceLocations(Long tenantId, String resourceType) {
            tenantIds.add(tenantId);
            locationFilters.add(resourceType);
            return List.of(new BiResourceLocationView(
                    61L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "executive",
                    20,
                    ACTOR,
                    NOW));
        }

        @Override
        public BiResourceOwnershipView transferResource(Long tenantId, BiResourceTransferCommand command, String actor) {
            recordMutation(tenantId, actor);
            transferCommand = command;
            return new BiResourceOwnershipView(
                    51L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    command.ownerUser(),
                    actor,
                    NOW);
        }

        @Override
        public List<BiResourceOwnershipView> listResourceOwnerships(Long tenantId, String resourceType) {
            tenantIds.add(tenantId);
            ownershipFilters.add(resourceType);
            return List.of(new BiResourceOwnershipView(
                    51L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "bi-owner",
                    ACTOR,
                    NOW));
        }

        @Override
        public List<BiPublishApprovalView> listPublishApprovals(
                Long tenantId,
                String resourceType,
                String resourceKey,
                String status) {
            tenantIds.add(tenantId);
            approvalFilters.add(resourceType + "|" + resourceKey + "|" + status);
            return List.of(new BiPublishApprovalView(
                    81L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "APPROVED",
                    "Ready",
                    ACTOR,
                    NOW,
                    HEADER_ACTOR,
                    NOW,
                    "ship it"));
        }

        @Override
        public BiPublishApprovalView requestPublishApproval(
                Long tenantId,
                BiPublishApprovalCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            approvalCommand = command;
            return new BiPublishApprovalView(
                    81L,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "PENDING",
                    command.reason(),
                    actor,
                    NOW,
                    null,
                    null,
                    null);
        }

        @Override
        public BiPublishApprovalView reviewPublishApproval(
                Long tenantId,
                BiPublishApprovalReviewCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            approvalReviewCommand = command;
            return new BiPublishApprovalView(
                    command.approvalId(),
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "APPROVED",
                    "Ready",
                    ACTOR,
                    NOW,
                    actor,
                    NOW,
                    command.reviewComment());
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
        public BiChartView publishChartResource(Long tenantId, String chartKey, String actor) {
            recordMutation(tenantId, actor);
            chartLifecycleKeys.add(chartKey);
            return chartView(tenantId, sampleChartCommand(chartKey, "published"), actor);
        }

        @Override
        public void archiveChartResource(Long tenantId, String chartKey, String actor) {
            recordMutation(tenantId, actor);
            chartLifecycleKeys.add(chartKey);
        }

        @Override
        public List<BiResourceVersionView> listChartResourceVersions(Long tenantId, String chartKey) {
            tenantIds.add(tenantId);
            chartLifecycleKeys.add(chartKey);
            return List.of(
                    versionView("CHART", chartKey, 3),
                    versionView("CHART", chartKey, 2),
                    versionView("CHART", chartKey, 1));
        }

        @Override
        public BiChartView restoreChartResourceVersion(Long tenantId, String chartKey, Integer version, String actor) {
            recordMutation(tenantId, actor);
            chartLifecycleKeys.add(chartKey);
            restoredChartVersion = version;
            return chartView(tenantId, sampleChartCommand(chartKey, "draft"), actor);
        }

        @Override
        public List<BiSubscriptionView> listSubscriptions(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return List.of(subscriptionView(tenantId, sampleSubscriptionCommand(), HEADER_ACTOR));
        }

        @Override
        public BiSubscriptionView upsertSubscription(Long tenantId, BiSubscriptionCommand command, String actor) {
            recordMutation(tenantId, actor);
            subscriptionCommand = command;
            return subscriptionView(tenantId, command, actor);
        }

        @Override
        public void deleteSubscription(Long tenantId, Long id) {
            tenantIds.add(tenantId);
            subscriptionIds.add(id);
        }

        @Override
        public BiDeliveryRunResult runSubscriptionDelivery(Long tenantId, Long id, String actor) {
            recordMutation(tenantId, actor);
            subscriptionIds.add(id);
            return new BiDeliveryRunResult("SUBSCRIPTION", id, "daily-revenue", "TRIGGERED",
                    List.of(deliveryLogView(tenantId, "SUBSCRIPTION", id, "daily-revenue", "TRIGGERED")));
        }

        @Override
        public List<BiAlertRuleView> listAlertRules(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return List.of(alertRuleView(tenantId, sampleAlertRuleCommand(), HEADER_ACTOR));
        }

        @Override
        public BiAlertRuleView upsertAlertRule(Long tenantId, BiAlertRuleCommand command, String actor) {
            recordMutation(tenantId, actor);
            alertCommand = command;
            return alertRuleView(tenantId, command, actor);
        }

        @Override
        public void deleteAlertRule(Long tenantId, Long id) {
            tenantIds.add(tenantId);
            alertIds.add(id);
        }

        @Override
        public BiDeliveryRunResult runAlertDelivery(Long tenantId, Long id, String actor) {
            recordMutation(tenantId, actor);
            alertIds.add(id);
            return new BiDeliveryRunResult("ALERT", id, "gmv-spike", "TRIGGERED",
                    List.of(deliveryLogView(tenantId, "ALERT", id, "gmv-spike", "TRIGGERED")));
        }

        @Override
        public List<BiDeliveryLogView> listDeliveryLogs(Long tenantId, String jobType, Long jobId, int limit) {
            tenantIds.add(tenantId);
            return List.of(deliveryLogView(tenantId, "SUBSCRIPTION", 101L, "daily-revenue", "TRIGGERED"));
        }

        @Override
        public BiDeliveryAuditSummary auditDeliveryLogs(
                Long tenantId,
                String jobType,
                String status,
                String channel,
                Long jobId,
                int limit) {
            tenantIds.add(tenantId);
            BiDeliveryLogView log = deliveryLogView(tenantId, "SUBSCRIPTION", 101L, "daily-revenue", "TRIGGERED");
            return new BiDeliveryAuditSummary(1, 0, 1, 0, 0, 0, 1, 0, List.of(log));
        }

        @Override
        public BiDeliveryRetryResult retryDeliveryLogs(Long tenantId, String actor, int limit) {
            recordMutation(tenantId, actor);
            BiDeliveryLogView log = deliveryLogView(tenantId, "SUBSCRIPTION", 101L, "daily-revenue", "DELIVERED");
            return new BiDeliveryRetryResult(1, 1, 1, 0, 0, List.of(log));
        }

        @Override
        public List<BiDeliveryAttachmentView> listDeliveryAttachments(
                Long tenantId,
                String jobType,
                Long jobId,
                Long deliveryLogId,
                int limit) {
            tenantIds.add(tenantId);
            return List.of(deliveryAttachmentView(tenantId));
        }

        @Override
        public BiDeliveryAttachmentDownload downloadDeliveryAttachment(Long tenantId, Long id, String actor) {
            recordMutation(tenantId, actor);
            return new BiDeliveryAttachmentDownload(
                    "delivery-" + id + ".txt",
                    "text/plain",
                    ("delivery-" + id).getBytes());
        }

        @Override
        public BiDeliveryAttachmentCleanupResult cleanupDeliveryAttachments(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return new BiDeliveryAttachmentCleanupResult(1, 1, 1, 0);
        }

        @Override
        public BiDeliverySchedulerResult runDeliveryScheduler(Long tenantId, String actor) {
            recordMutation(tenantId, actor);
            return new BiDeliverySchedulerResult(1, 1, 1, 1, 0, 0);
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
        public BiDashboardView cloneDashboardResource(
                Long tenantId,
                String actor,
                String dashboardKey,
                BiDashboardCloneCommand command) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey);
            dashboardCloneCommand = command;
            return dashboardView(tenantId, new BiDashboardCommand(
                    WORKSPACE_ID,
                    command.dashboardKey(),
                    command.name(),
                    command.description(),
                    Map.of(),
                    Map.of(),
                    List.of("orders-trend"),
                    "DRAFT"), actor);
        }

        @Override
        public BiDashboardExportPackageView exportDashboardResource(Long tenantId, String actor, String dashboardKey) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey);
            return new BiDashboardExportPackageView(
                    "DASHBOARD",
                    dashboardKey,
                    dashboardView(tenantId, sampleDashboardCommand(dashboardKey), actor),
                    Map.of("schema", "canvas.bi.dashboard.v1"),
                    NOW,
                    actor);
        }

        @Override
        public org.chovy.canvas.bi.domain.BiDashboardResourceOperationsCatalog.DashboardPackageFile exportDashboardResourceFile(
                Long tenantId,
                String actor,
                String dashboardKey) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey);
            return new org.chovy.canvas.bi.domain.BiDashboardResourceOperationsCatalog.DashboardPackageFile(
                    dashboardKey + "-v1.bi-dashboard.json",
                    "application/json",
                    dashboardKey.getBytes());
        }

        @Override
        public BiDashboardView importDashboardResource(Long tenantId, String actor, BiDashboardImportCommand command) {
            recordMutation(tenantId, actor);
            dashboardImportCommand = command;
            return dashboardView(tenantId, new BiDashboardCommand(
                    WORKSPACE_ID,
                    command.dashboardKey(),
                    command.name(),
                    "Imported",
                    Map.of(),
                    Map.of(),
                    List.of("orders-trend"),
                    "DRAFT"), actor);
        }

        @Override
        public BiDashboardView publishDashboardResource(Long tenantId, String dashboardKey, String actor) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add("publish:" + dashboardKey);
            return dashboardView(tenantId, new BiDashboardCommand(
                    WORKSPACE_ID,
                    dashboardKey,
                    "Published dashboard",
                    "Published",
                    Map.of(),
                    Map.of(),
                    List.of("orders-trend"),
                    "PUBLISHED"), actor);
        }

        @Override
        public BiDashboardView archiveDashboardResource(Long tenantId, String dashboardKey, String actor) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey);
            return dashboardView(tenantId, new BiDashboardCommand(
                    WORKSPACE_ID,
                    dashboardKey,
                    "Archived dashboard",
                    "Archived",
                    Map.of(),
                    Map.of(),
                    List.of("orders-trend"),
                    "ARCHIVED"), actor);
        }

        @Override
        public List<BiResourceVersionView> listDashboardResourceVersions(Long tenantId, String dashboardKey, int limit) {
            tenantIds.add(tenantId);
            dashboardLifecycleKeys.add(dashboardKey);
            return List.of(new BiResourceVersionView(
                    "DASHBOARD",
                    dashboardKey,
                    1,
                    "DRAFT",
                    Map.of("name", "Marketing overview"),
                    HEADER_ACTOR,
                    NOW));
        }

        @Override
        public BiDashboardView restoreDashboardResourceVersion(
                Long tenantId,
                String dashboardKey,
                Integer version,
                String actor) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey + ":" + version);
            return dashboardView(tenantId, sampleDashboardCommand(dashboardKey), actor);
        }

        @Override
        public BiDashboardRuntimeStateView getDashboardRuntimeState(Long tenantId, String actor, String dashboardKey) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey);
            return new BiDashboardRuntimeStateView(tenantId, dashboardKey, Map.of("range", "LAST_30_DAYS"), actor, NOW);
        }

        @Override
        public BiDashboardRuntimeStateView saveDashboardRuntimeState(
                Long tenantId,
                String actor,
                String dashboardKey,
                BiDashboardRuntimeStateCommand command) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(dashboardKey);
            dashboardRuntimeStateCommand = command;
            return new BiDashboardRuntimeStateView(tenantId, dashboardKey, command.parameters(), actor, NOW);
        }

        @Override
        public List<BiPortalResourceView> listPortalResources(Long tenantId) {
            tenantIds.add(tenantId);
            if (portalDraftCommand != null) {
                String portalKey = portalKeys.isEmpty() ? portalDraftCommand.portalKey() : portalKeys.getLast();
                return List.of(portalView(
                        tenantId,
                        portalKey,
                        portalDraftCommand.title(),
                        "DRAFT",
                        1,
                        HEADER_ACTOR));
            }
            return List.of(portalView(tenantId, "marketing-portal", "Marketing portal", "PUBLISHED", 2, HEADER_ACTOR));
        }

        @Override
        public BiPortalResourceView getPortalResource(Long tenantId, String portalKey) {
            tenantIds.add(tenantId);
            portalKeys.add(portalKey);
            return portalView(tenantId, portalKey, "Marketing portal", "PUBLISHED", 2, HEADER_ACTOR);
        }

        @Override
        public BiPortalResourceView savePortalDraft(
                Long tenantId,
                String portalKey,
                BiPortalResourceCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            portalKeys.add(portalKey);
            portalDraftCommand = command;
            return portalView(tenantId, portalKey, command.title(), "DRAFT", 1, actor);
        }

        @Override
        public BiPortalResourceView publishPortalResource(Long tenantId, String portalKey, String actor) {
            recordMutation(tenantId, actor);
            portalKeys.add(portalKey);
            return portalView(tenantId, portalKey, "Marketing portal", "PUBLISHED", 2, actor);
        }

        @Override
        public void archivePortalResource(Long tenantId, String portalKey, String actor) {
            recordMutation(tenantId, actor);
            portalKeys.add(portalKey);
        }

        @Override
        public List<BiResourceVersionView> listPortalResourceVersions(Long tenantId, String portalKey) {
            tenantIds.add(tenantId);
            portalKeys.add(portalKey);
            return List.of(versionView("PORTAL", portalKey, 2), versionView("PORTAL", portalKey, 1));
        }

        @Override
        public BiPortalResourceView restorePortalResourceVersion(
                Long tenantId,
                String portalKey,
                Integer version,
                String actor) {
            recordMutation(tenantId, actor);
            portalKeys.add(portalKey);
            return portalView(tenantId, portalKey, "Marketing portal", "DRAFT", 3, actor);
        }

        @Override
        public List<BiBigScreenResourceView> listBigScreenResources(Long tenantId) {
            tenantIds.add(tenantId);
            return List.of(bigScreenView(tenantId, "revenue-wall", "Revenue wall", "PUBLISHED", 2, HEADER_ACTOR));
        }

        @Override
        public BiBigScreenResourceView getBigScreenResource(Long tenantId, String screenKey) {
            tenantIds.add(tenantId);
            bigScreenKeys.add(screenKey);
            return bigScreenView(tenantId, screenKey, "Revenue wall", "PUBLISHED", 2, HEADER_ACTOR);
        }

        @Override
        public BiBigScreenResourceView saveBigScreenDraft(
                Long tenantId,
                String screenKey,
                BiBigScreenResourceCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            bigScreenKeys.add(screenKey);
            bigScreenDraftCommand = command;
            return bigScreenView(tenantId, screenKey, command.title(), "DRAFT", 1, actor);
        }

        @Override
        public BiBigScreenResourceView publishBigScreenResource(Long tenantId, String screenKey, String actor) {
            recordMutation(tenantId, actor);
            bigScreenKeys.add(screenKey);
            return bigScreenView(tenantId, screenKey, "Revenue wall", "PUBLISHED", 2, actor);
        }

        @Override
        public void archiveBigScreenResource(Long tenantId, String screenKey, String actor) {
            recordMutation(tenantId, actor);
            bigScreenKeys.add(screenKey);
        }

        @Override
        public List<BiResourceVersionView> listBigScreenResourceVersions(Long tenantId, String screenKey) {
            tenantIds.add(tenantId);
            bigScreenKeys.add(screenKey);
            return List.of(versionView("BIG_SCREEN", screenKey, 2), versionView("BIG_SCREEN", screenKey, 1));
        }

        @Override
        public BiBigScreenResourceView restoreBigScreenResourceVersion(
                Long tenantId,
                String screenKey,
                Integer version,
                String actor) {
            recordMutation(tenantId, actor);
            bigScreenKeys.add(screenKey);
            return bigScreenView(tenantId, screenKey, "Revenue wall", "DRAFT", 3, actor);
        }

        @Override
        public List<BiSpreadsheetResourceView> listSpreadsheetResources(Long tenantId) {
            tenantIds.add(tenantId);
            return List.of(spreadsheetView(tenantId, "revenue-sheet", "Revenue sheet", "PUBLISHED", 2, HEADER_ACTOR));
        }

        @Override
        public BiSpreadsheetResourceView getSpreadsheetResource(Long tenantId, String spreadsheetKey) {
            tenantIds.add(tenantId);
            spreadsheetKeys.add(spreadsheetKey);
            return spreadsheetView(tenantId, spreadsheetKey, "Revenue sheet", "PUBLISHED", 2, HEADER_ACTOR);
        }

        @Override
        public BiSpreadsheetResourceView saveSpreadsheetDraft(
                Long tenantId,
                String spreadsheetKey,
                BiSpreadsheetResourceCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            spreadsheetKeys.add(spreadsheetKey);
            spreadsheetDraftCommand = command;
            return spreadsheetView(tenantId, spreadsheetKey, command.name(), "DRAFT", 1, actor);
        }

        @Override
        public BiSpreadsheetResourceView publishSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor) {
            recordMutation(tenantId, actor);
            spreadsheetKeys.add(spreadsheetKey);
            return spreadsheetView(tenantId, spreadsheetKey, "Revenue sheet", "PUBLISHED", 2, actor);
        }

        @Override
        public void archiveSpreadsheetResource(Long tenantId, String spreadsheetKey, String actor) {
            recordMutation(tenantId, actor);
            spreadsheetKeys.add(spreadsheetKey);
        }

        @Override
        public List<BiResourceVersionView> listSpreadsheetResourceVersions(Long tenantId, String spreadsheetKey) {
            tenantIds.add(tenantId);
            spreadsheetKeys.add(spreadsheetKey);
            return List.of(versionView("SPREADSHEET", spreadsheetKey, 2), versionView("SPREADSHEET", spreadsheetKey, 1));
        }

        @Override
        public BiSpreadsheetResourceView restoreSpreadsheetResourceVersion(
                Long tenantId,
                String spreadsheetKey,
                Integer version,
                String actor) {
            recordMutation(tenantId, actor);
            spreadsheetKeys.add(spreadsheetKey);
            return spreadsheetView(tenantId, spreadsheetKey, "Revenue sheet", "DRAFT", 3, actor);
        }

        @Override
        public BiQueryCompileResult compileQuery(Long tenantId, BiQueryCommand command, String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryCompileResult("SELECT stat_date FROM canvas_daily_stats WHERE tenant_id = ?",
                    List.of(tenantId));
        }

        @Override
        public BiQueryResultView executeQuery(Long tenantId, BiQueryCommand command, String actor) {
            recordMutation(tenantId, actor);
            return queryResult(command == null ? "canvas_daily_stats" : command.datasetKey(), "biq-test", false);
        }

        @Override
        public BiQueryGateResult executeGatedQuery(Long tenantId, BiQueryGateCommand command, String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryGateResult(true, "ALLOWED", "availability gate passed",
                    queryResult(command.query().datasetKey(), "biq-gated", false));
        }

        @Override
        public BiQueryGateResult executeContractGatedQuery(
                Long tenantId,
                BiQueryContractGateCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryGateResult(true, "ALLOWED", "contract gate passed",
                    queryResult(command.query().datasetKey(), "biq-contract", false));
        }

        @Override
        public BiQueryExplainResult explainQuery(Long tenantId, BiQueryCommand command, String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryExplainResult("canvas_daily_stats", "biq-test", 1,
                    List.of("Resolve dataset canvas_daily_stats"));
        }

        @Override
        public List<BiQueryHistoryItemView> listQueryHistory(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return List.of(new BiQueryHistoryItemView(
                    1L,
                    "canvas_daily_stats",
                    ACTOR,
                    1,
                    12L,
                    "SUCCESS",
                    "biq-test",
                    null,
                    NOW));
        }

        @Override
        public BiQueryHistoryDetailView queryHistoryDetail(Long tenantId, Long historyId) {
            tenantIds.add(tenantId);
            return new BiQueryHistoryDetailView(
                    historyId,
                    "canvas_daily_stats",
                    ACTOR,
                    new BiQueryCommand("canvas_daily_stats", null, List.of("stat_date"),
                            List.of("total_executions"), List.of(), List.of(), 10, 0, Map.of()),
                    1,
                    12L,
                    "SUCCESS",
                    "biq-test",
                    null,
                    NOW);
        }

        @Override
        public BiQueryCancelResult cancelQuery(Long tenantId, String sqlHash, String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryCancelResult(sqlHash, true, "CANCELLED");
        }

        @Override
        public BiQueryGovernanceSummaryView queryGovernanceSummary(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return new BiQueryGovernanceSummaryView(1, 0, 0, 0, 12L, 30000L, 100000,
                    List.of(Map.of("datasetKey", "canvas_daily_stats", "queries", 1)), List.of());
        }

        @Override
        public BiQueryGovernancePolicyView queryGovernancePolicy(Long tenantId) {
            tenantIds.add(tenantId);
            return new BiQueryGovernancePolicyView(30000L, 100000, List.of());
        }

        @Override
        public BiQueryGovernancePolicyView updateQueryGovernancePolicy(
                Long tenantId,
                BiQueryGovernancePolicyCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryGovernancePolicyView(
                    command.defaultTimeoutMs() == null ? 30000L : command.defaultTimeoutMs(),
                    command.defaultQuotaRows() == null ? 100000 : command.defaultQuotaRows(),
                    command.datasets());
        }

        @Override
        public List<BiQueryGovernanceAuditEntryView> queryGovernanceAudit(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return List.of(new BiQueryGovernanceAuditEntryView(1L, tenantId, "BI_QUERY_GOVERNANCE_POLICY_UPSERT",
                    "canvas_daily_stats", ACTOR, Map.of("status", "ok"), NOW));
        }

        @Override
        public BiQueryCachePolicyView queryCachePolicy(Long tenantId) {
            tenantIds.add(tenantId);
            return new BiQueryCachePolicyView(true, 300L, "CACHE", List.of());
        }

        @Override
        public BiQueryCachePolicyView updateQueryCachePolicy(
                Long tenantId,
                BiQueryCachePolicyCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            return new BiQueryCachePolicyView(
                    command.defaultEnabled() == null || command.defaultEnabled(),
                    command.defaultTtlSeconds() == null ? 300L : command.defaultTtlSeconds(),
                    command.defaultCacheMode() == null ? "CACHE" : command.defaultCacheMode().toUpperCase(),
                    command.resources());
        }

        @Override
        public BiQueryCacheInvalidationResult invalidateQueryCache(
                Long tenantId,
                BiQueryCacheInvalidationCommand command) {
            tenantIds.add(tenantId);
            return new BiQueryCacheInvalidationResult(1, 1, "INVALIDATED");
        }

        @Override
        public BiQueryCacheStatsView queryCacheStats(Long tenantId) {
            tenantIds.add(tenantId);
            return new BiQueryCacheStatsView("final-bi-memory", true, 0, 1000, 300L, 0L, 1L, 0L, 0L);
        }

        @Override
        public List<BiDatasourceHealthView> datasourceHealth() {
            return List.of();
        }

        @Override
        public List<BiDatasourceHealthSnapshotView> datasourceHealthHistory(int limit) {
            return List.of();
        }

        @Override
        public BiDatasourceHealthSloView datasourceHealthSlo(int limit) {
            return new BiDatasourceHealthSloView(0, 0, 0, 100.0, List.of());
        }

        @Override
        public List<BiDatasourceConnectorView> datasourceConnectors() {
            return List.of(new BiDatasourceConnectorView(
                    "API_JSON",
                    "HTTP JSON API",
                    List.of("API"),
                    true,
                    true));
        }

        @Override
        public List<BiDatasourceOnboardingView> listDatasources(Long tenantId) {
            tenantIds.add(tenantId);
            return List.of(datasourceView(101L, tenantId, "orders-warehouse", "Orders Warehouse", HEADER_ACTOR));
        }

        @Override
        public BiDatasourceOnboardingView createDatasource(
                Long tenantId,
                BiDatasourceOnboardingCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            createdDatasourceCommand = command;
            return datasourceView(1L, tenantId, "orders-warehouse", command.name(), actor);
        }

        @Override
        public BiDatasourceOnboardingView updateDatasource(
                Long tenantId,
                Long id,
                BiDatasourceOnboardingCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            datasourceIds.add(id);
            updatedDatasourceCommand = command;
            return datasourceView(id, tenantId, "orders-warehouse", command.name(), actor);
        }

        @Override
        public BiDatasourceOnboardingView uploadDatasourceFile(
                Long tenantId,
                String actor,
                String filename,
                String name,
                String description,
                String sheetName,
                String delimiter,
                boolean headerRow,
                String encoding) {
            recordMutation(tenantId, actor);
            return datasourceView(102L, tenantId, "orders-csv", name, actor);
        }

        @Override
        public BiDatasourceFileMaterializationResult materializeDatasourceFile(
                Long tenantId,
                String actor,
                String filename,
                String name,
                String description,
                String sheetName,
                String delimiter,
                boolean headerRow,
                String encoding,
                String datasetKey,
                String datasetName,
                String tenantColumn,
                int schemaLimit,
                long maxRows) {
            recordMutation(tenantId, actor);
            BiDatasourceOnboardingView source = datasourceView(103L, tenantId, "orders-csv", name, actor);
            return new BiDatasourceFileMaterializationResult(
                    source,
                    datasourceSnapshot(source.id(), actor),
                    Map.of("datasetKey", "orders-daily", "tenantColumn", tenantColumn),
                    Map.of("status", "SUCCESS", "importedRows", maxRows));
        }

        @Override
        public BiDatasourceConnectionTestResult testDatasourceConnection(Long tenantId, Long id) {
            tenantIds.add(tenantId);
            datasourceIds.add(id);
            return new BiDatasourceConnectionTestResult(
                    id,
                    "orders-warehouse",
                    true,
                    "connection available",
                    12L);
        }

        @Override
        public BiDatasourceCredentialRotationView rotateDatasourceCredential(
                Long tenantId,
                Long id,
                BiDatasourceCredentialRotationCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            datasourceIds.add(id);
            datasourceRotationCommand = command;
            return new BiDatasourceCredentialRotationView(id, "orders-warehouse", actor);
        }

        @Override
        public BiDatasourceSchemaPreviewView previewDatasourceSchema(Long tenantId, Long id, int limit) {
            tenantIds.add(tenantId);
            datasourceIds.add(id);
            return new BiDatasourceSchemaPreviewView(
                    id,
                    "orders-warehouse",
                    datasourceTables());
        }

        @Override
        public BiDatasourceApiPreviewView previewDatasourceApi(
                Long tenantId,
                Long id,
                BiDatasourceApiPreviewCommand command) {
            tenantIds.add(tenantId);
            datasourceIds.add(id);
            datasourceApiPreviewCommand = command;
            return new BiDatasourceApiPreviewView(
                    id,
                    "orders-warehouse",
                    List.of(Map.of("fieldKey", "sourceKey", "dataType", "STRING")),
                    List.of(Map.of("sourceKey", "orders-warehouse")));
        }

        @Override
        public BiDatasourceSchemaSnapshotView syncDatasourceSchema(
                Long tenantId,
                Long id,
                int limit,
                BiDatasourceApiPreviewCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            datasourceIds.add(id);
            return datasourceSnapshot(id, actor);
        }

        @Override
        public BiDatasourceSchemaSnapshotView latestDatasourceSchemaSnapshot(Long tenantId, Long id) {
            tenantIds.add(tenantId);
            datasourceIds.add(id);
            return datasourceSnapshot(id, HEADER_ACTOR);
        }

        @Override
        public List<BiDatasourceSchemaSnapshotView> listDatasourceSchemaSnapshots(Long tenantId, Long id, int limit) {
            tenantIds.add(tenantId);
            datasourceIds.add(id);
            return List.of(datasourceSnapshot(id, HEADER_ACTOR));
        }

        @Override
        public Map<String, Object> previewSelfServiceExport(
                Long tenantId,
                String actor,
                String role,
                BiSelfServicePreviewCommand command) {
            recordMutation(tenantId, actor);
            selfServiceRoles.add(role);
            selfServicePreviewCommand = command;
            return Map.of(
                    "datasetKey", command.query().getOrDefault("datasetKey", "orders_daily"),
                    "rowCount", command.previewLimit() == null ? 20 : command.previewLimit(),
                    "rows", List.of(Map.of("datasetKey", command.query().getOrDefault("datasetKey", "orders_daily"))));
        }

        @Override
        public BiSelfServiceExportJobView createSelfServiceExport(
                Long tenantId,
                String actor,
                String role,
                BiSelfServiceExportCommand command) {
            recordMutation(tenantId, actor);
            selfServiceRoles.add(role);
            selfServiceCommand = command;
            return selfServiceJob(tenantId, "PENDING_REVIEW", "PENDING", actor, null, null);
        }

        @Override
        public List<BiSelfServiceExportJobView> listSelfServiceExports(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return List.of(selfServiceJob(tenantId, "COMPLETED", "APPROVED", HEADER_ACTOR, "lead", "worker"));
        }

        @Override
        public BiSelfServiceExportJobView reviewSelfServiceExport(
                Long tenantId,
                String actor,
                String role,
                Long id,
                BiSelfServiceExportReviewCommand command) {
            recordMutation(tenantId, actor);
            selfServiceRoles.add(role);
            selfServiceReviewCommand = command;
            return selfServiceJob(tenantId, "QUEUED", "APPROVED", HEADER_ACTOR, actor, null);
        }

        @Override
        public BiSelfServiceExportJobDetailView getSelfServiceExportDetail(Long tenantId, Long id) {
            tenantIds.add(tenantId);
            return new BiSelfServiceExportJobDetailView(
                    selfServiceJob(tenantId, "COMPLETED", "APPROVED", HEADER_ACTOR, "lead", "worker"),
                    Map.of("tenantId", tenantId),
                    Map.of("requestedBy", HEADER_ACTOR));
        }

        @Override
        public BiSelfServiceExportDownload downloadSelfServiceExport(Long tenantId, String actor, Long id) {
            recordMutation(tenantId, actor);
            return new BiSelfServiceExportDownload(
                    "bi-export-" + id + ".csv",
                    "text/csv",
                    ("id,status\n" + id + ",COMPLETED\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public BiSelfServiceExportJobView cancelSelfServiceExport(Long tenantId, String actor, Long id) {
            recordMutation(tenantId, actor);
            return selfServiceJob(tenantId, "CANCELLED", "APPROVED", HEADER_ACTOR, "lead", actor);
        }

        @Override
        public BiSelfServiceExportCleanupResult cleanupSelfServiceExports(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return new BiSelfServiceExportCleanupResult(limit, 0, 1);
        }

        @Override
        public BiSelfServiceExportRetryResult retrySelfServiceExports(
                Long tenantId,
                String actor,
                String role,
                int limit) {
            recordMutation(tenantId, actor);
            selfServiceRoles.add(role);
            return new BiSelfServiceExportRetryResult(limit, 1,
                    List.of(selfServiceJob(tenantId, "QUEUED", "APPROVED", HEADER_ACTOR, "lead", actor)));
        }

        @Override
        public BiSelfServiceExportQueueResult runSelfServiceExportQueue(
                Long tenantId,
                String actor,
                String role,
                int limit) {
            recordMutation(tenantId, actor);
            selfServiceRoles.add(role);
            return new BiSelfServiceExportQueueResult(limit, 1, 0,
                    List.of(selfServiceJob(tenantId, "COMPLETED", "APPROVED", HEADER_ACTOR, "lead", actor)));
        }

        @Override
        public BiEmbedTicketView createEmbedTicket(Long tenantId, BiEmbedTicketCommand command, String actor) {
            recordMutation(tenantId, actor);
            return new BiEmbedTicketView("embed-7-1", NOW.atZone(java.time.ZoneId.systemDefault()).toInstant(),
                    "/canvas/bi/embed?ticket=embed-7-1");
        }

        @Override
        public BiEmbedTicketPayloadView verifyEmbedTicket(BiEmbedTicketVerifyCommand command, String origin) {
            return new BiEmbedTicketPayloadView(tenantIds.isEmpty() ? TENANT_ID : tenantIds.getLast(), ACTOR,
                    "DASHBOARD", "marketing-overview", "view", Map.of(), Map.of(),
                    List.of("https://example.com"), 5, 60, command.ticket(),
                    NOW.atZone(java.time.ZoneId.systemDefault()).toInstant(),
                    NOW.plusMinutes(5).atZone(java.time.ZoneId.systemDefault()).toInstant());
        }

        @Override
        public BiQueryResultView executeEmbedQuery(BiEmbedQueryCommand command, String origin) {
            return queryResult(command.query().datasetKey(), "biq-embed", false);
        }

        @Override
        public BiEmbedTicketCleanupResult cleanupEmbedTickets(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return new BiEmbedTicketCleanupResult(1, 0, 0);
        }

        @Override
        public BiDashboardView embedDashboardResource(Long tenantId, String resourceKey, String ticket, String origin) {
            tenantIds.add(tenantId);
            detailDashboardKeys.add(resourceKey);
            return dashboardView(tenantId, sampleDashboardCommand(resourceKey), ACTOR);
        }

        @Override
        public BiDashboardRuntimeStateView embedDashboardRuntimeState(
                Long tenantId,
                String actor,
                String resourceKey,
                String ticket,
                String origin) {
            recordMutation(tenantId, actor);
            dashboardLifecycleKeys.add(resourceKey);
            return new BiDashboardRuntimeStateView(tenantId, resourceKey, Map.of("range", "LAST_30_DAYS"), actor, NOW);
        }

        @Override
        public BiPortalResourceView embedPortalResource(Long tenantId, String resourceKey, String ticket, String origin) {
            tenantIds.add(tenantId);
            portalKeys.add(resourceKey);
            return portalView(tenantId, resourceKey, "Marketing portal", "PUBLISHED", 2, ACTOR);
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

        @Override
        public List<BiResourcePermissionView> listResourcePermissions(
                Long tenantId,
                String resourceType,
                String resourceKey,
                Long resourceId) {
            tenantIds.add(tenantId);
            resourcePermissionFilters.add(resourceType + "|" + resourceKey + "|" + resourceId);
            return List.of(resourcePermissionView(401L, tenantId, "DASHBOARD", "marketing-overview", "ROLE",
                    "analyst", "VIEW", "ALLOW", ACTOR));
        }

        @Override
        public BiResourcePermissionView upsertResourcePermission(
                Long tenantId,
                BiResourcePermissionCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            resourcePermissionCommand = command;
            return resourcePermissionView(401L, tenantId, "DASHBOARD", "marketing-overview", "ROLE",
                    "analyst", "VIEW", "ALLOW", actor);
        }

        @Override
        public void deleteResourcePermission(Long tenantId, String actor, Long id) {
            recordMutation(tenantId, actor);
            deletedPermissionIds.add(id);
        }

        @Override
        public List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey) {
            tenantIds.add(tenantId);
            rowPermissionFilters.add(datasetKey);
            return List.of(rowPermissionView(402L, tenantId, "orders-daily", "cn-only", ACTOR));
        }

        @Override
        public BiRowPermissionView upsertRowPermission(Long tenantId, BiRowPermissionCommand command, String actor) {
            recordMutation(tenantId, actor);
            rowPermissionCommand = command;
            return rowPermissionView(402L, tenantId, "orders-daily", "cn-only", actor);
        }

        @Override
        public void deleteRowPermission(Long tenantId, String actor, Long id) {
            recordMutation(tenantId, actor);
            deletedPermissionIds.add(id);
        }

        @Override
        public List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey) {
            tenantIds.add(tenantId);
            columnPermissionFilters.add(datasetKey);
            return List.of(columnPermissionView(403L, tenantId, "orders-daily", "customer-phone", ACTOR));
        }

        @Override
        public BiColumnPermissionView upsertColumnPermission(
                Long tenantId,
                BiColumnPermissionCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            columnPermissionCommand = command;
            return columnPermissionView(403L, tenantId, "orders-daily", "customer-phone", actor);
        }

        @Override
        public void deleteColumnPermission(Long tenantId, String actor, Long id) {
            recordMutation(tenantId, actor);
            deletedPermissionIds.add(id);
        }

        @Override
        public List<BiPermissionAuditEntryView> permissionAudit(Long tenantId, int limit) {
            tenantIds.add(tenantId);
            return List.of(new BiPermissionAuditEntryView(
                    801L,
                    ACTOR,
                    "BI_PERMISSION_CHANGE",
                    "BI_PERMISSION",
                    "{\"limit\":" + limit + "}",
                    NOW));
        }

        @Override
        public List<BiPermissionRequestView> listPermissionRequests(
                Long tenantId,
                String resourceType,
                String resourceKey,
                String status) {
            tenantIds.add(tenantId);
            permissionRequestFilters.add(resourceType + "|" + resourceKey + "|" + status);
            return List.of(permissionRequestView(701L, tenantId, "PENDING", ACTOR, null));
        }

        @Override
        public BiPermissionRequestView requestPermission(
                Long tenantId,
                BiPermissionRequestCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            permissionRequestCommand = command;
            return permissionRequestView(701L, tenantId, "PENDING", actor, null);
        }

        @Override
        public BiPermissionRequestView reviewPermissionRequest(
                Long tenantId,
                BiPermissionRequestReviewCommand command,
                String actor) {
            recordMutation(tenantId, actor);
            permissionReviewCommand = command;
            return permissionRequestView(command.requestId(), tenantId, "APPROVED", ACTOR, actor);
        }

        private void recordMutation(Long tenantId, String actor) {
            tenantIds.add(tenantId);
            actors.add(actor);
        }

        private BiResourcePermissionView resourcePermissionView(
                Long id,
                Long tenantId,
                String resourceType,
                String resourceKey,
                String subjectType,
                String subjectId,
                String actionKey,
                String effect,
                String actor) {
            return new BiResourcePermissionView(
                    id,
                    tenantId,
                    5L,
                    resourceType,
                    resourceKey,
                    300L,
                    subjectType,
                    subjectId,
                    actionKey,
                    effect,
                    actor,
                    NOW);
        }

        private BiRowPermissionView rowPermissionView(
                Long id,
                Long tenantId,
                String datasetKey,
                String ruleKey,
                String actor) {
            return new BiRowPermissionView(
                    id,
                    tenantId,
                    datasetKey,
                    100L,
                    ruleKey,
                    "ROLE",
                    "analyst",
                    "[{\"field\":\"country\",\"op\":\"EQ\",\"value\":\"CN\"}]",
                    true,
                    NOW);
        }

        private BiColumnPermissionView columnPermissionView(
                Long id,
                Long tenantId,
                String datasetKey,
                String fieldKey,
                String actor) {
            return new BiColumnPermissionView(
                    id,
                    tenantId,
                    datasetKey,
                    100L,
                    fieldKey,
                    "USER",
                    "alice",
                    "MASK",
                    "{\"strategy\":\"last4\"}",
                    true,
                    NOW);
        }

        private BiPermissionRequestView permissionRequestView(
                Long id,
                Long tenantId,
                String status,
                String requestedBy,
                String reviewedBy) {
            return new BiPermissionRequestView(
                    id,
                    tenantId,
                    5L,
                    "DASHBOARD",
                    "marketing-overview",
                    "EXPORT",
                    requestedBy,
                    NOW,
                    "Need campaign export",
                    status,
                    reviewedBy,
                    reviewedBy == null ? null : NOW,
                    reviewedBy == null ? null : "go ahead",
                    reviewedBy == null ? null : 401L);
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

        private BiPortalResourceView portalView(Long tenantId,
                                                String portalKey,
                                                String title,
                                                String status,
                                                Integer version,
                                                String actor) {
            return new BiPortalResourceView(
                    tenantId,
                    portalKey,
                    title,
                    "Executive BI portal",
                    List.of("marketing-overview"),
                    Map.of("columns", 12),
                    Map.of("theme", "light"),
                    status,
                    version,
                    actor,
                    actor,
                    NOW,
                    NOW);
        }

        private BiBigScreenResourceView bigScreenView(Long tenantId,
                                                      String screenKey,
                                                      String title,
                                                      String status,
                                                      Integer version,
                                                      String actor) {
            return new BiBigScreenResourceView(
                    tenantId,
                    screenKey,
                    title,
                    "Command center",
                    List.of("marketing-overview"),
                    Map.of("resolution", "1920x1080"),
                    Map.of("refreshSeconds", 60),
                    status,
                    version,
                    actor,
                    actor,
                    NOW,
                    NOW);
        }

        private BiSpreadsheetResourceView spreadsheetView(Long tenantId,
                                                          String spreadsheetKey,
                                                          String name,
                                                          String status,
                                                          Integer version,
                                                          String actor) {
            return new BiSpreadsheetResourceView(
                    tenantId,
                    spreadsheetKey,
                    name,
                    "Finance workbook",
                    List.of(Map.of("sheetKey", "daily", "title", "Daily revenue")),
                    Map.of("datasetKey", "orders-daily"),
                    Map.of("theme", "compact"),
                    status,
                    version,
                    actor,
                    actor,
                    NOW,
                    NOW);
        }

        private BiResourceVersionView versionView(String resourceType, String resourceKey, Integer version) {
            return new BiResourceVersionView(
                    resourceType,
                    resourceKey,
                    version,
                    version == 1 ? "DRAFT" : "PUBLISHED",
                    Map.of("title", resourceKey),
                    version == 1 ? ACTOR : HEADER_ACTOR,
                    NOW.plusMinutes(version));
        }

        private BiSubscriptionView subscriptionView(Long tenantId, BiSubscriptionCommand command, String actor) {
            return new BiSubscriptionView(
                    101L,
                    tenantId,
                    WORKSPACE_ID,
                    "daily-revenue",
                    command.name(),
                    "DASHBOARD",
                    "marketing-overview",
                    command.resourceId(),
                    command.schedule(),
                    command.receivers(),
                    command.delivery(),
                    true,
                    actor,
                    NOW,
                    NOW);
        }

        private BiAlertRuleView alertRuleView(Long tenantId, BiAlertRuleCommand command, String actor) {
            return new BiAlertRuleView(
                    201L,
                    tenantId,
                    WORKSPACE_ID,
                    "gmv-spike",
                    command.name(),
                    "orders-daily",
                    100L,
                    "gross-gmv",
                    command.condition(),
                    command.receivers(),
                    true,
                    actor,
                    NOW,
                    NOW);
        }

        private BiDeliveryLogView deliveryLogView(
                Long tenantId,
                String jobType,
                Long jobId,
                String jobKey,
                String status) {
            return new BiDeliveryLogView(
                    301L,
                    tenantId,
                    WORKSPACE_ID,
                    jobType,
                    jobId,
                    jobKey,
                    "DASHBOARD",
                    300L,
                    "EMAIL",
                    Map.of("email", "ops@example.com"),
                    Map.of("jobKey", jobKey),
                    BigDecimal.ZERO,
                    status,
                    "Delivery " + status.toLowerCase(java.util.Locale.ROOT),
                    null,
                    0,
                    3,
                    null,
                    null,
                    null,
                    ACTOR,
                    NOW,
                    NOW);
        }

        private BiDeliveryAttachmentView deliveryAttachmentView(Long tenantId) {
            return new BiDeliveryAttachmentView(
                    401L,
                    tenantId,
                    WORKSPACE_ID,
                    "SUBSCRIPTION",
                    101L,
                    "daily-revenue",
                    301L,
                    "DASHBOARD",
                    300L,
                    "subscription-101",
                    "TEXT",
                    "delivery-401.txt",
                    "text/plain",
                    "/canvas/bi/delivery-attachments/401/download",
                    "MEMORY",
                    "delivery-401",
                    12L,
                    7,
                    NOW.plusDays(7),
                    0,
                    null,
                    "AVAILABLE",
                    null,
                    ACTOR,
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
                    command.status().toUpperCase(java.util.Locale.ROOT),
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

        private BiSelfServiceExportJobView selfServiceJob(
                Long tenantId,
                String status,
                String approvalStatus,
                String requestedBy,
                String reviewedBy,
                String processedBy) {
            return new BiSelfServiceExportJobView(
                    901L,
                    tenantId,
                    "DASHBOARD",
                    "marketing-overview",
                    300L,
                    "CSV",
                    Map.of("datasetKey", "orders_daily"),
                    500,
                    status,
                    approvalStatus,
                    "monthly close",
                    "ship it",
                    requestedBy,
                    reviewedBy,
                    processedBy,
                    NOW,
                    NOW);
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

        private BiQueryResultView queryResult(String datasetKey, String sqlHash, boolean cached) {
            return new BiQueryResultView(
                    datasetKey == null ? "canvas_daily_stats" : datasetKey,
                    List.of(
                            Map.of("fieldKey", "stat_date", "role", "DIMENSION"),
                            Map.of("fieldKey", "total_executions", "role", "METRIC")),
                    List.of(Map.of("stat_date", "2026-06-14", "total_executions", 42)),
                    1,
                    12L,
                    sqlHash,
                    cached);
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

        private BiDatasourceOnboardingView datasourceView(
                Long id,
                Long tenantId,
                String sourceKey,
                String name,
                String actor) {
            return new BiDatasourceOnboardingView(
                    id,
                    tenantId,
                    sourceKey,
                    "MYSQL",
                    name,
                    "jdbc:mysql://db.internal:****/orders",
                    "r***r",
                    "JDBC source",
                    true,
                    "SUCCESS",
                    Map.of("driverClassName", "com.mysql.cj.jdbc.Driver"),
                    actor,
                    actor);
        }

        private BiDatasourceSchemaSnapshotView datasourceSnapshot(Long sourceId, String actor) {
            return new BiDatasourceSchemaSnapshotView(
                    201L,
                    sourceId,
                    "orders-warehouse",
                    "SUCCESS",
                    actor,
                    1,
                    datasourceTables(),
                    NOW);
        }

        private List<Map<String, Object>> datasourceTables() {
            return List.of(Map.of(
                    "tableName", "orders_warehouse_sample",
                    "columns", List.of(
                            Map.of("name", "id", "dataType", "LONG"),
                            Map.of("name", "tenant_id", "dataType", "LONG"),
                            Map.of("name", "amount", "dataType", "DECIMAL"))));
        }

        private BiChartCommand sampleChartCommand() {
            return sampleChartCommand("orders-trend");
        }

        private BiChartCommand sampleChartCommand(String chartKey) {
            return sampleChartCommand(chartKey, "published");
        }

        private BiChartCommand sampleChartCommand(String chartKey, String status) {
            return new BiChartCommand(
                    WORKSPACE_ID,
                    chartKey,
                    "Orders trend",
                    "line",
                    "orders-daily",
                    Map.of("dimensions", List.of("order_date")),
                    Map.of("palette", "ops"),
                    Map.of("drilldown", true),
                    status);
        }

        private BiDatasetCommand sampleDatasetCommand() {
            return sampleDatasetCommand("orders-daily");
        }

        private BiDatasetCommand sampleDatasetCommand(String datasetKey) {
            return sampleDatasetCommand(datasetKey, "draft");
        }

        private BiDatasetCommand sampleDatasetCommand(String datasetKey, String status) {
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
                    status);
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

        private BiSubscriptionCommand sampleSubscriptionCommand() {
            return new BiSubscriptionCommand(
                    "daily-revenue",
                    "Daily revenue",
                    "DASHBOARD",
                    "marketing-overview",
                    300L,
                    Map.of("cron", "0 8 * * *"),
                    Map.of("email", List.of("ops@example.com")),
                    Map.of("channel", "email"),
                    true);
        }

        private BiAlertRuleCommand sampleAlertRuleCommand() {
            return new BiAlertRuleCommand(
                    "gmv-spike",
                    "GMV spike",
                    "orders-daily",
                    "gross-gmv",
                    Map.of("op", "GT", "value", 1000),
                    Map.of("email", List.of("growth@example.com")),
                    true);
        }
    }
}
