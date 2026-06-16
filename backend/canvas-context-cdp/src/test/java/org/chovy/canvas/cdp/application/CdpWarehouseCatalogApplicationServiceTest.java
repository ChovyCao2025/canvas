package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.DatasetCommand;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.Direction;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.LineageCommand;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseCatalogApplicationService 的核心行为。
 */
class CdpWarehouseCatalogApplicationServiceTest {

    /**
     * 返回默认的s Null Tenant To Zero And Keeps Datasets Tenant Scoped。
     */
    @Test
    void defaultsNullTenantToZeroAndKeepsDatasetsTenantScoped() {
        CdpWarehouseCatalogFacade service = new CdpWarehouseCatalogApplicationService();

        service.upsertDataset(null, dataset("dwd_user_profile", "DWD", "ACTIVE", "first"));
        service.upsertDataset(42L, dataset("dwd_user_profile", "DWD", "ACTIVE", "tenant-42"));

        assertThat(service.listDatasets(null, null, null))
                .extracting(row -> row.get("tenantId"))
                .containsExactly(0L);
        assertThat(service.listDatasets(42L, null, null))
                .extracting(row -> row.get("displayName"))
                .containsExactly("tenant-42");
        assertThat(service.listDatasets(7L, null, null)).isEmpty();
    }

    /**
     * 执行 filtersDatasetsAndUpsertReplacesExistingDatasetFields 对应的 CDP 业务操作。
     */
    @Test
    void filtersDatasetsAndUpsertReplacesExistingDatasetFields() {
        CdpWarehouseCatalogFacade service = new CdpWarehouseCatalogApplicationService();

        service.upsertDataset(0L, dataset("dwd_user_profile", "dwd", "active", "User Profile"));
        service.upsertDataset(0L, dataset("ads_order_summary", "ads", "draft", "Order Summary"));
        service.upsertDataset(0L, new DatasetCommand(
                "dwd_user_profile",
                "DWD",
                "dwd.user_profile_v2",
                null,
                "Customer",
                "crm",
                "owner-b",
                "replacement",
                30,
                "high",
                "inactive",
                "{\"fields\":[]}"));

        List<Map<String, Object>> activeDwd = service.listDatasets(0L, "DWD", "ACTIVE");
        List<Map<String, Object>> inactiveDwd = service.listDatasets(0L, "dwd", "inactive");

        assertThat(activeDwd).isEmpty();
        assertThat(inactiveDwd).hasSize(1);
        assertThat(inactiveDwd.getFirst())
                .containsEntry("datasetKey", "dwd_user_profile")
                .containsEntry("physicalName", "dwd.user_profile_v2")
                .containsEntry("displayName", "dwd_user_profile")
                .containsEntry("piiLevel", "HIGH")
                .containsEntry("status", "INACTIVE")
                .containsEntry("schemaJson", "{\"fields\":[]}");
    }

    /**
     * 执行 buildsDirectAndTransitiveLineageGraphsWithDirectionAndDepth 对应的 CDP 业务操作。
     */
    @Test
    void buildsDirectAndTransitiveLineageGraphsWithDirectionAndDepth() {
        CdpWarehouseCatalogFacade service = new CdpWarehouseCatalogApplicationService();
        service.upsertDataset(0L, dataset("ods_event", "ODS", "ACTIVE", "Event"));
        service.upsertDataset(0L, dataset("dwd_user_profile", "DWD", "ACTIVE", "User"));
        service.upsertDataset(0L, dataset("ads_segment", "ADS", "ACTIVE", "Segment"));
        service.upsertDataset(0L, dataset("ads_campaign", "ADS", "ACTIVE", "Campaign"));
        service.createLineageEdge(0L, lineage("ods_event", "dwd_user_profile", "sql-1", true));
        service.createLineageEdge(0L, lineage("dwd_user_profile", "ads_segment", "sql-2", true));
        service.createLineageEdge(0L, lineage("ads_segment", "ads_campaign", "sql-3", true));
        service.createLineageEdge(0L, lineage("ads_campaign", "ads_segment", "inactive", false));
        service.createLineageEdge(42L, lineage("tenant_only", "dwd_user_profile", "foreign", true));

        Map<String, Object> upstream = service.lineage(0L, "dwd_user_profile", Direction.UPSTREAM);
        Map<String, Object> both = service.lineage(0L, "dwd_user_profile", Direction.BOTH);
        Map<String, Object> transitive = service.transitiveLineage(0L, "dwd_user_profile", Direction.DOWNSTREAM, 2);

        assertThat((List<?>) upstream.get("edges")).hasSize(1);
        assertThat((List<?>) both.get("edges")).hasSize(2);
        assertThat((List<Map<String, Object>>) transitive.get("edges"))
                .extracting(edge -> edge.get("downstreamDatasetKey"))
                .containsExactly("ads_segment", "ads_campaign");
        assertThat((List<Map<String, Object>>) transitive.get("paths"))
                .extracting(path -> path.get("datasetKeys"))
                .containsExactly(List.of("dwd_user_profile", "ads_segment"),
                        List.of("dwd_user_profile", "ads_segment", "ads_campaign"));
        assertThat(transitive).containsEntry("maxDepth", 2)
                .containsEntry("truncated", false);
    }

    /**
     * 校验s Required Dataset Lineage And Depth Fields。
     */
    @Test
    void validatesRequiredDatasetLineageAndDepthFields() {
        CdpWarehouseCatalogFacade service = new CdpWarehouseCatalogApplicationService();

        assertThatThrownBy(() -> service.upsertDataset(0L, dataset("", "DWD", "ACTIVE", "bad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datasetKey is required");
        assertThatThrownBy(() -> service.createLineageEdge(0L, lineage("same", "same", "sql", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");
        assertThatThrownBy(() -> service.transitiveLineage(0L, "dwd_user_profile", Direction.BOTH, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepth must be positive");
    }

    /**
     * 执行 dataset 对应的 CDP 业务操作。
     */
    private static DatasetCommand dataset(String datasetKey, String layer, String status, String displayName) {
        return new DatasetCommand(
                datasetKey,
                layer,
                datasetKey,
                displayName,
                "Customer",
                "crm",
                "owner-a",
                "description",
                15,
                "normal",
                status,
                "{\"type\":\"struct\"}");
    }

    /**
     * 执行 lineage 对应的 CDP 业务操作。
     */
    private static LineageCommand lineage(String upstream, String downstream, String transformRef, Boolean active) {
        return new LineageCommand(upstream, downstream, "sql", transformRef, "hard", "lineage", active);
    }
}
