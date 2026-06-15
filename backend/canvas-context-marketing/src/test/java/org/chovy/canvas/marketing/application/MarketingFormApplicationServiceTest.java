package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingFormFacade;
import org.junit.jupiter.api.Test;

class MarketingFormApplicationServiceTest {

    @Test
    void formsAreTenantScopedAndSeededDeterministicallyForCompatibility() {
        MarketingFormFacade service = new MarketingFormApplicationService();

        List<Map<String, Object>> defaultTenantForms = service.listForms(7L);
        List<Map<String, Object>> otherTenantForms = service.listForms(8L);

        assertThat(defaultTenantForms).extracting(row -> row.get("id")).containsExactly(5001L, 5002L);
        assertThat(otherTenantForms).extracting(row -> row.get("id")).containsExactly(5001L);
        assertThat(service.getForm(7L, 5001L))
                .containsEntry("tenantId", 7L)
                .containsEntry("publicKey", "lead-capture")
                .containsEntry("status", "active")
                .containsEntry("successMessage", "Thanks");
        assertThatThrownBy(() -> service.getForm(8L, 5002L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marketing form not found");
    }

    @Test
    void createUpdateAndStatusFollowLegacyManagementBehavior() {
        MarketingFormFacade service = new MarketingFormApplicationService();

        Map<String, Object> created = service.createForm(42L, Map.of(
                "publicKey", "event-signup",
                "name", "Event Signup",
                "description", "June campaign",
                "fieldSchemaJson", "[{\"name\":\"email\"}]",
                "submitActionJson", "{\"event\":\"signup\"}",
                "successMessage", "Thanks",
                "active", true), "form-admin");
        Map<String, Object> updated = service.updateForm(42L, (Long) created.get("id"), Map.of(
                "name", "Event Signup Updated",
                "description", "Updated campaign",
                "successMessage", "Updated",
                "active", false), "form-editor");
        Map<String, Object> inactive = service.setStatus(42L, (Long) created.get("id"), Map.of("active", false),
                "status-admin");

        assertThat(created)
                .containsEntry("tenantId", 42L)
                .containsEntry("publicKey", "event-signup")
                .containsEntry("name", "Event Signup")
                .containsEntry("status", "active")
                .containsEntry("createdBy", "form-admin")
                .containsEntry("updatedBy", "form-admin");
        assertThat(updated)
                .containsEntry("name", "Event Signup Updated")
                .containsEntry("description", "Updated campaign")
                .containsEntry("successMessage", "Updated")
                .containsEntry("status", "inactive")
                .containsEntry("updatedBy", "form-editor");
        assertThat(inactive)
                .containsEntry("status", "inactive")
                .containsEntry("updatedBy", "status-admin");
    }

    @Test
    void submissionsSupportTenantFormFilteringAndCompatibilityLimits() {
        MarketingFormFacade service = new MarketingFormApplicationService();

        List<Map<String, Object>> limited = service.submissions(7L, null, 1);
        List<Map<String, Object>> formOne = service.submissions(7L, 5001L, 10);
        List<Map<String, Object>> otherTenant = service.submissions(8L, null, 10);

        assertThat(limited).hasSize(1);
        assertThat(formOne).hasSize(2)
                .allSatisfy(row -> assertThat(row).containsEntry("tenantId", 7L).containsEntry("formId", 5001L));
        assertThat(otherTenant).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("tenantId", 8L).containsEntry("formId", 5001L));
        assertThat(service.submissions(7L, null, -1)).hasSize(3);
        assertThat(service.submissions(7L, null, 500)).hasSize(3);
    }

    @Test
    void defaultsAndValidationMatchCompatibilityRules() {
        MarketingFormFacade service = new MarketingFormApplicationService();

        Map<String, Object> created = service.createForm(null, Map.of("name", "Default tenant form"), "");

        assertThat(created)
                .containsEntry("tenantId", 7L)
                .containsEntry("publicKey", "form-5003")
                .containsEntry("createdBy", "operator-1")
                .containsEntry("status", "active");
        assertThatThrownBy(() -> service.createForm(7L, Map.of("name", " "), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        assertThatThrownBy(() -> service.updateForm(7L, 5001L, Map.of("fieldSchemaJson", "not-json"), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldSchemaJson must be JSON");
    }
}
