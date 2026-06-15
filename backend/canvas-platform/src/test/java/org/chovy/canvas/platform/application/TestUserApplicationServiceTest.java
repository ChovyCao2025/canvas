package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.chovy.canvas.platform.api.TestUserFacade;
import org.junit.jupiter.api.Test;

class TestUserApplicationServiceTest {

    @Test
    void listsSetsAndUsersByTenantWithLegacyDoFieldNames() {
        TestUserFacade service = new TestUserApplicationService();

        assertThat(service.listSets(0L))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("id", 100L)
                        .containsEntry("tenantId", 0L)
                        .containsEntry("name", "Default rerun users")
                        .containsEntry("createdBy", "system")
                        .containsKeys("createdAt", "updatedAt"));

        assertThat(service.listUsers(0L, 100L))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("id", 1001L)
                        .containsEntry("tenantId", 0L)
                        .containsEntry("setId", 100L)
                        .containsEntry("userId", "test-user-1")
                        .containsEntry("displayName", "Test User One")
                        .containsEntry("profileJson", "{\"tier\":\"VIP\",\"city\":\"Shanghai\"}")
                        .containsEntry("inputParams", "{\"coupon\":\"WELCOME\"}"));

        assertThat(service.listUsers(7L, 100L)).isEmpty();
    }

    @Test
    void createSetAndUserUseTenantAndPreserveJsonStringResponseShape() {
        TestUserFacade service = new TestUserApplicationService();

        Map<String, Object> set = service.createSet(7L, Map.of("name", "QA cohort", "description", "nightly"),
                "operator-1");
        Map<String, Object> user = service.createUser(7L, (Long) set.get("id"), Map.of(
                "userId", "qa-1",
                "displayName", "QA One",
                "profile", Map.of("tier", "GOLD"),
                "inputParams", Map.of("coupon", "QA10")));

        assertThat(set)
                .containsEntry("tenantId", 7L)
                .containsEntry("name", "QA cohort")
                .containsEntry("createdBy", "operator-1");
        assertThat(user)
                .containsEntry("tenantId", 7L)
                .containsEntry("setId", set.get("id"))
                .containsEntry("userId", "qa-1")
                .containsEntry("displayName", "QA One")
                .containsEntry("profileJson", "{\"tier\":\"GOLD\"}")
                .containsEntry("inputParams", "{\"coupon\":\"QA10\"}");
    }

    @Test
    void detailAndPreviewAreTenantScopedAndPreviewParsesMaps() {
        TestUserFacade service = new TestUserApplicationService();

        Map<String, Object> detail = service.getUser(0L, 1001L);
        Map<String, Object> preview = service.preview(0L, 1001L);

        assertThat(detail)
                .containsEntry("profileJson", "{\"tier\":\"VIP\",\"city\":\"Shanghai\"}")
                .containsEntry("inputParams", "{\"coupon\":\"WELCOME\"}");
        assertThat(preview)
                .containsEntry("id", 1001L)
                .containsEntry("userId", "test-user-1")
                .containsEntry("displayName", "Test User One");
        assertThat((Map<String, Object>) preview.get("profile"))
                .containsEntry("tier", "VIP")
                .containsEntry("city", "Shanghai");
        assertThat((Map<String, Object>) preview.get("inputParams"))
                .containsEntry("coupon", "WELCOME");
        assertThat((Map<String, Object>) preview.get("context"))
                .containsEntry("tenantId", 0L)
                .containsEntry("setId", 100L);

        assertThatThrownBy(() -> service.getUser(7L, 1001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test user not found");
    }

    @Test
    void createRequiresMeaningfulNameAndUserId() {
        TestUserFacade service = new TestUserApplicationService();

        assertThatThrownBy(() -> service.createSet(0L, Map.of("description", "missing"), "system"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        assertThatThrownBy(() -> service.createUser(0L, 100L, Map.of("displayName", "Missing id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");
    }
}
