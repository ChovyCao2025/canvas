package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseFieldGovernanceFacade;
import org.junit.jupiter.api.Test;

class CdpWarehouseFieldGovernanceApplicationServiceTest {

    @Test
    void upsertListAndEvaluateApplyTenantOverrideAndNormalizePolicyFields() {
        CdpWarehouseFieldGovernanceFacade service = new CdpWarehouseFieldGovernanceApplicationService();

        Map<String, Object> policy = service.upsertPolicy(9L, Map.ofEntries(
                Map.entry("datasetKey", "canvas_daily_stats"),
                Map.entry("fieldKey", "canvas_id"),
                Map.entry("physicalName", "canvas_dws.canvas_daily_stats"),
                Map.entry("columnName", "canvas_id"),
                Map.entry("valueType", "number"),
                Map.entry("semanticType", "id"),
                Map.entry("piiLevel", "normal"),
                Map.entry("accessPolicy", "mask"),
                Map.entry("minRole", "tenant_admin"),
                Map.entry("allowedUsages", "select,filter,group"),
                Map.entry("ownerName", "data-platform")));

        assertThat(policy).containsEntry("tenantId", 9L)
                .containsEntry("valueType", "NUMBER")
                .containsEntry("semanticType", "ID")
                .containsEntry("accessPolicy", "MASK")
                .containsEntry("minRole", "TENANT_ADMIN")
                .containsEntry("allowedUsages", "SELECT,FILTER,GROUP")
                .containsEntry("lifecycleStatus", "ACTIVE");

        assertThat(service.listPolicies(9L, "canvas_daily_stats", "active"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("fieldKey", "canvas_id")
                        .containsEntry("accessPolicy", "MASK"));
        assertThat(service.listPolicies(8L, "canvas_daily_stats", null)).isEmpty();

        Map<String, Object> denied = service.evaluateBiQuery(9L, "alice", "OPERATOR", Map.of(
                "datasetKey", "canvas_daily_stats",
                "dimensions", List.of("canvas_id")));

        assertThat(denied).containsEntry("tenantId", 9L)
                .containsEntry("datasetKey", "canvas_daily_stats")
                .containsEntry("actor", "alice")
                .containsEntry("role", "OPERATOR")
                .containsEntry("actionKey", "BI_EVALUATE")
                .containsEntry("allowed", false);
        assertThat((List<Map<String, Object>>) denied.get("decisions"))
                .anySatisfy(decision -> assertThat(decision)
                        .containsEntry("fieldKey", "canvas_id")
                        .containsEntry("usage", "SELECT")
                        .containsEntry("decision", "DENY"));

        Map<String, Object> allowed = service.evaluateBiQuery(9L, "admin", "TENANT_ADMIN", Map.of(
                "datasetKey", "canvas_daily_stats",
                "dimensions", List.of("canvas_id")));

        assertThat(allowed).containsEntry("allowed", true)
                .containsEntry("reason", "allowed");
    }

    @Test
    void rejectsMissingRequiredFieldsAndDeniesDisallowedUsage() {
        CdpWarehouseFieldGovernanceFacade service = new CdpWarehouseFieldGovernanceApplicationService();

        assertThatThrownBy(() -> service.upsertPolicy(9L, Map.of(
                "datasetKey", "canvas_daily_stats",
                "fieldKey", "",
                "physicalName", "canvas_dws.canvas_daily_stats",
                "columnName", "canvas_id",
                "valueType", "NUMBER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fieldKey is required");

        service.upsertPolicy(9L, Map.of(
                "datasetKey", "canvas_daily_stats",
                "fieldKey", "total_executions",
                "physicalName", "canvas_dws.canvas_daily_stats",
                "columnName", "total_executions",
                "valueType", "number",
                "accessPolicy", "allow",
                "allowedUsages", "select"));

        Map<String, Object> evaluation = service.evaluateBiQuery(9L, "alice", "OPERATOR", Map.of(
                "datasetKey", "canvas_daily_stats",
                "sorts", List.of(Map.of("fieldKey", "total_executions", "direction", "DESC"))));

        assertThat(evaluation).containsEntry("allowed", false);
        assertThat(String.valueOf(evaluation.get("reason"))).contains("SORT is not allowed");
    }
}
