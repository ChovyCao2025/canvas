package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpTagDefinitionFacade;
import org.junit.jupiter.api.Test;

class CdpTagDefinitionApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T04:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void managesTagDefinitionsAndValuesWithinTenant() {
        CdpTagDefinitionFacade service = new CdpTagDefinitionApplicationService(CLOCK);

        Map<String, Object> created = service.create(7L, Map.of(
                "tagCode", "vip_level",
                "tagName", "VIP Level",
                "tagType", "PROFILE",
                "enabled", true), "operator-1");
        Map<String, Object> page = service.list(7L, 1, 20, "PROFILE", 1);
        Map<String, Object> updated = service.update(7L, 1L, Map.of(
                "tagName", "VIP Segment",
                "tagType", "PROFILE",
                "enabled", false), "operator-2");
        Map<String, Object> value = service.createValue(7L, "vip_level", Map.of(
                "valueCode", "gold",
                "valueName", "Gold",
                "enabled", true), "operator-3");
        Map<String, Object> values = service.listValues(7L, "vip_level", 1);
        Map<String, Object> updatedValue = service.updateValue(7L, 1L, Map.of(
                "valueName", "Gold Members",
                "enabled", false), "operator-4");
        Map<String, Object> deletedValue = service.deleteValue(7L, 1L, "operator-5");
        Map<String, Object> deleted = service.delete(7L, 1L, "operator-6");

        assertThat(created).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("tagCode", "vip_level")
                .containsEntry("tagName", "VIP Level")
                .containsEntry("tagType", "PROFILE")
                .containsEntry("enabled", true)
                .containsEntry("updatedBy", "operator-1");
        assertThat(page).containsEntry("total", 1L)
                .containsEntry("page", 1)
                .containsEntry("size", 20);
        assertThat((List<?>) page.get("records")).hasSize(1);
        assertThat(updated).containsEntry("tagName", "VIP Segment")
                .containsEntry("enabled", false)
                .containsEntry("updatedBy", "operator-2");
        assertThat(value).containsEntry("id", 1L)
                .containsEntry("tagCode", "vip_level")
                .containsEntry("valueCode", "gold")
                .containsEntry("valueName", "Gold")
                .containsEntry("updatedBy", "operator-3");
        assertThat(values).containsEntry("total", 1L)
                .containsEntry("tagCode", "vip_level");
        assertThat(updatedValue).containsEntry("valueName", "Gold Members")
                .containsEntry("enabled", false)
                .containsEntry("updatedBy", "operator-4");
        assertThat(deletedValue).containsEntry("deleted", true)
                .containsEntry("id", 1L)
                .containsEntry("updatedBy", "operator-5");
        assertThat(deleted).containsEntry("deleted", true)
                .containsEntry("id", 1L)
                .containsEntry("updatedBy", "operator-6");

        assertThat(service.list(8L, 1, 20, null, null)).containsEntry("total", 0L);
    }

    @Test
    void validationDefaultsAndPaginationAreStable() {
        CdpTagDefinitionFacade service = new CdpTagDefinitionApplicationService(CLOCK);
        service.create(null, Map.of("tagCode", "churn_risk"), "");

        assertThat(service.list(null, null, null, null, null))
                .containsEntry("tenantId", 0L)
                .containsEntry("page", 1)
                .containsEntry("size", 20)
                .containsEntry("total", 1L);
        assertThat(service.update(null, 1L, Map.of("tagName", "Churn Risk"), null))
                .containsEntry("updatedBy", "system")
                .containsEntry("tagName", "Churn Risk");

        assertThatThrownBy(() -> service.create(7L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tagCode is required");
        assertThatThrownBy(() -> service.update(7L, 99L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag definition not found");
        assertThatThrownBy(() -> service.createValue(7L, "", Map.of("valueCode", "x"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tagCode is required");
        assertThatThrownBy(() -> service.createValue(0L, "churn_risk", Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valueCode is required");
    }
}
