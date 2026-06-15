package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AbExperimentFacade;
import org.junit.jupiter.api.Test;

class AbExperimentApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void managesExperimentsGroupsAndGovernanceWithinTenant() {
        AbExperimentFacade service = new AbExperimentApplicationService(CLOCK);

        Map<String, Object> experiment = service.create(7L, Map.of(
                "experimentKey", "checkout-test",
                "name", "Checkout test",
                "enabled", 1), "operator-1");
        Map<String, Object> updated = service.update(7L, 1L, Map.of(
                "name", "Checkout test v2",
                "trafficPercent", 80), "operator-2");
        Map<String, Object> group = service.createGroup(7L, 1L, Map.of(
                "groupKey", "A",
                "name", "Control",
                "weight", 60), "operator-3");
        Map<String, Object> updatedGroup = service.updateGroup(7L, 1L, 1L, Map.of(
                "weight", 70), "operator-4");
        Map<String, Object> evaluation = service.evaluateGovernance(7L, 1L, "A", "operator-5");

        assertThat(experiment).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("experimentKey", "checkout-test")
                .containsEntry("enabled", 1)
                .containsEntry("updatedBy", "operator-1");
        assertThat(updated).containsEntry("id", 1L)
                .containsEntry("name", "Checkout test v2")
                .containsEntry("trafficPercent", 80)
                .containsEntry("updatedBy", "operator-2");
        assertThat(group).containsEntry("id", 1L)
                .containsEntry("experimentId", 1L)
                .containsEntry("groupKey", "A")
                .containsEntry("weight", 60);
        assertThat(updatedGroup).containsEntry("weight", 70)
                .containsEntry("updatedBy", "operator-4");
        assertThat(evaluation).containsEntry("experimentId", 1L)
                .containsEntry("controlVariantKey", "A")
                .containsEntry("variantCount", 1)
                .containsEntry("eligible", true)
                .containsEntry("evaluatedBy", "operator-5");

        Map<String, Object> page = service.list(7L, Map.of("page", 1, "size", 20, "enabled", 1));
        assertThat(page).containsEntry("total", 1L);
        assertThat((List<?>) page.get("records")).hasSize(1);
        assertThat(service.listGroups(7L, 1L, false)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("groupKey", "A"));
        assertThat(service.list(8L, Map.of())).containsEntry("total", 0L);
    }

    @Test
    void deletesDisableRowsAndValidationIsTenantScoped() {
        AbExperimentFacade service = new AbExperimentApplicationService(CLOCK);
        service.create(7L, Map.of("experimentKey", "pricing"), "operator-1");
        service.createGroup(7L, 1L, Map.of("groupKey", "A"), "operator-1");

        Map<String, Object> deletedGroup = service.deleteGroup(7L, 1L, 1L);
        Map<String, Object> deletedExperiment = service.delete(7L, 1L);

        assertThat(deletedGroup).containsEntry("id", 1L)
                .containsEntry("enabled", 0);
        assertThat(deletedExperiment).containsEntry("id", 1L)
                .containsEntry("enabled", 0);
        assertThat(service.listGroups(7L, 1L, false)).isEmpty();
        assertThat(service.listGroups(7L, 1L, true)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("enabled", 0));

        assertThatThrownBy(() -> service.update(8L, 1L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AB experiment not found");
        assertThatThrownBy(() -> service.updateGroup(7L, 1L, 99L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AB experiment group not found");
        assertThatThrownBy(() -> service.evaluateGovernance(7L, 1L, "missing", "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Control variant not found");
    }
}
