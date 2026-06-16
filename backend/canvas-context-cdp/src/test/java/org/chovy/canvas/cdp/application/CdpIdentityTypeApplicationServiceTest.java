package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpIdentityTypeFacade;
import org.chovy.canvas.cdp.domain.CdpIdentityTypeCatalog;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpIdentityTypeApplicationService 的核心行为。
 */
class CdpIdentityTypeApplicationServiceTest {

    /**
     * 创建List And Update Preserve Legacy Identity Type Behavior。
     */
    @Test
    void createListAndUpdatePreserveLegacyIdentityTypeBehavior() {
        CdpIdentityTypeFacade service = new CdpIdentityTypeApplicationService();

        Map<String, Object> first = service.create(Map.of(
                "code", " EMAIL ",
                "name", " Email Address "));
        Map<String, Object> second = service.create(Map.of(
                "code", "member_id",
                "name", "Member ID",
                "enabled", 0,
                "allowImport", 1,
                "priority", 50));
        Map<String, Object> third = service.create(Map.of(
                "code", "phone",
                "name", "Phone",
                "enabled", 1,
                "allowImport", 0));

        assertThat(first).containsEntry("id", 1L)
                .containsEntry("code", "email")
                .containsEntry("name", "Email Address")
                .containsEntry("enabled", 1)
                .containsEntry("allowImport", 1)
                .containsEntry("multiValue", 0)
                .containsEntry("priority", 100)
                .containsEntry("participateMapping", 0);
        assertThat(second).containsEntry("id", 2L).containsEntry("priority", 50);
        assertThat(third).containsEntry("id", 3L).containsEntry("allowImport", 0);

        Map<String, Object> importable = service.list(null, 1);
        assertThat(importable).containsEntry("total", 2L);
        assertThat((List<?>) importable.get("list"))
                .extracting(row -> String.valueOf(((Map<?, ?>) row).get("code")))
                .containsExactly("email", "member_id");

        Map<String, Object> enabledImportable = service.list(1, 1);
        assertThat(enabledImportable).containsEntry("total", 1L);
        assertThat((List<?>) enabledImportable.get("list"))
                .extracting(row -> (Long) ((Map<?, ?>) row).get("id"))
                .containsExactly(1L);

        Map<String, Object> updated = service.update(1L, Map.of(
                "id", 99L,
                "code", " EMAIL_NEW ",
                "name", " Email New ",
                "allowImport", 0,
                "priority", 5));

        assertThat(updated).containsEntry("id", 1L)
                .containsEntry("code", "email_new")
                .containsEntry("name", "Email New")
                .containsEntry("allowImport", 0)
                .containsEntry("priority", 5);
    }

    /**
     * 执行 validationAndDeleteGuardsMatchLegacyContract 对应的 CDP 业务操作。
     */
    @Test
    void validationAndDeleteGuardsMatchLegacyContract() {
        CdpIdentityTypeCatalog catalog = new CdpIdentityTypeCatalog();
        CdpIdentityTypeFacade service = new CdpIdentityTypeApplicationService(catalog);

        assertThatThrownBy(() -> service.create(Map.of("code", "A", "name", "Too Short")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid code");
        assertThatThrownBy(() -> service.create(Map.of("code", "9bad", "name", "Bad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid code");
        assertThatThrownBy(() -> service.create(Map.of("code", "valid_code", "name", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        Map<String, Object> created = service.create(Map.of("code", "customer_id", "name", "Customer ID"));
        Long id = (Long) created.get("id");
        catalog.recordIdentityUse(id);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity type not found");
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity type is in use");

        catalog.clearIdentityUse(id);
        assertThat(service.delete(id)).containsEntry("deleted", true).containsEntry("id", id);
        assertThat(service.list(null, null)).containsEntry("total", 0L);
    }
}
