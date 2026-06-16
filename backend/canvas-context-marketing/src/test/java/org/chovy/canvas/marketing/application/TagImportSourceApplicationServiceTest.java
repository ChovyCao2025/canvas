package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportSourceFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证TagImportSourceApplicationService的关键兼容行为。
 */
class TagImportSourceApplicationServiceTest {

    /**
     * 验证 sources are tenant scoped and list filtering keeps legacy page shape 场景的兼容行为。
     */
    @Test
    void sourcesAreTenantScopedAndListFilteringKeepsLegacyPageShape() {
        TagImportSourceFacade service = new TagImportSourceApplicationService();

        Map<String, Object> defaultTenant = service.listSources(7L, null);
        Map<String, Object> onlyEnabled = service.listSources(7L, 1);
        Map<String, Object> otherTenant = service.listSources(8L, null);

        assertThat(defaultTenant).containsEntry("total", 2L);
        assertThat((List<Map<String, Object>>) defaultTenant.get("list"))
                .extracting(row -> row.get("id"))
                .containsExactly(7002L, 7001L);
        assertThat((List<Map<String, Object>>) onlyEnabled.get("list"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("tenantId", 7L)
                        .containsEntry("id", 7001L)
                        .containsEntry("sourceType", "API_PULL")
                        .containsEntry("status", "enabled")
                        .containsEntry("enabled", 1));
        assertThat((List<Map<String, Object>>) otherTenant.get("list"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("tenantId", 8L)
                        .containsEntry("id", 7001L));
    }

    /**
     * 验证 create update delete and run preserve legacy fields and tenant isolation 场景的兼容行为。
     */
    @Test
    void createUpdateDeleteAndRunPreserveLegacyFieldsAndTenantIsolation() {
        TagImportSourceFacade service = new TagImportSourceApplicationService();

        Map<String, Object> created = service.createSource(42L, sourcePayload(), "tag-admin");
        Map<String, Object> updatePayload = sourcePayload();
        updatePayload.put("name", "CRM API Updated");
        updatePayload.put("method", "GET");
        updatePayload.put("enabled", 0);
        Map<String, Object> updated = service.updateSource(42L, (Long) created.get("id"), updatePayload,
                "tag-editor");

        assertThat(created)
                .containsEntry("tenantId", 42L)
                .containsEntry("sourceType", "API_PULL")
                .containsEntry("method", "POST")
                .containsEntry("status", "enabled")
                .containsEntry("createdBy", "tag-admin");
        assertThat(updated)
                .containsEntry("name", "CRM API Updated")
                .containsEntry("method", "GET")
                .containsEntry("enabled", 0)
                .containsEntry("status", "disabled")
                .containsEntry("updatedBy", "tag-editor");
        assertThatThrownBy(() -> service.runSource(42L, (Long) created.get("id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag import source is disabled");

        service.deleteSource(42L, (Long) created.get("id"));
        assertThatThrownBy(() -> service.updateSource(42L, (Long) created.get("id"), Map.of("name", "Missing"),
                "tag-editor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag import source not found");
    }

    /**
     * 验证 defaults validation and run result match compatibility rules 场景的兼容行为。
     */
    @Test
    void defaultsValidationAndRunResultMatchCompatibilityRules() {
        TagImportSourceFacade service = new TagImportSourceApplicationService();

        Map<String, Object> created = service.createSource(null, Map.of(
                "name", "Default Source",
                "url", "https://default.example.test/tags",
                "fieldMapping", "{\"idType\":\"idType\",\"idValue\":\"idValue\",\"tagCode\":\"tagCode\"}"), "");
        Map<String, Object> result = service.runSource(null, (Long) created.get("id"));

        assertThat(created)
                .containsEntry("tenantId", 7L)
                .containsEntry("method", "GET")
                .containsEntry("recordsPath", "$")
                .containsEntry("enabled", 1)
                .containsEntry("status", "enabled")
                .containsEntry("createdBy", "operator-1");
        assertThat(result)
                .containsEntry("status", "SUCCESS")
                .containsEntry("totalRows", 2)
                .containsEntry("successRows", 2)
                .containsEntry("failedRows", 0);
        assertThatThrownBy(() -> service.createSource(7L, Map.of("url", "https://x.example.test"), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        assertThatThrownBy(() -> service.createSource(7L, Map.of(
                "name", "Bad",
                "url", "https://x.example.test",
                "fieldMapping", "not-json"), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldMapping must be JSON");
    }

    /**
     * 执行sourcePayload业务操作。
     */
    private static Map<String, Object> sourcePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "CRM API");
        payload.put("url", "https://crm.example.test/tags");
        payload.put("method", "post");
        payload.put("headersJson", "{\"Authorization\":\"Bearer token\"}");
        payload.put("bodyTemplate", "{\"page\":1}");
        payload.put("pageParam", "page");
        payload.put("pageSizeParam", "size");
        payload.put("pageSize", 200);
        payload.put("recordsPath", "$.data");
        payload.put("fieldMapping", "{\"idType\":\"id_type\",\"idValue\":\"id_value\",\"tagCode\":\"tag\"}");
        payload.put("enabled", 1);
        return payload;
    }
}
