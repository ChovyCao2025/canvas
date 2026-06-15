package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.chovy.canvas.cdp.api.CdpWriteKeyFacade;
import org.junit.jupiter.api.Test;

class CdpWriteKeyApplicationServiceTest {

    @Test
    void createsRawKeyOnceAndListsOnlyTenantRowsWithoutSecret() {
        CdpWriteKeyFacade service = new CdpWriteKeyApplicationService();

        CdpWriteKeyFacade.CreateResult created = service.create(42L,
                new CdpWriteKeyFacade.CreateCommand("Web SDK", "WEB", 120, 10000L, "browser ingestion"),
                "operator-1");
        service.create(7L, new CdpWriteKeyFacade.CreateCommand("Other", "SERVER", 60, 5000L, null), "operator-2");

        assertThat(created)
                .returns(1L, CdpWriteKeyFacade.CreateResult::id)
                .returns("Web SDK", CdpWriteKeyFacade.CreateResult::name)
                .returns("WEB", CdpWriteKeyFacade.CreateResult::platform)
                .returns(120, CdpWriteKeyFacade.CreateResult::rateLimitQps)
                .returns(10000L, CdpWriteKeyFacade.CreateResult::dailyQuota);
        assertThat(created.rawKey()).startsWith("ck_live_");
        assertThat(created.keyPrefix()).isEqualTo(created.rawKey().substring(0, 12));

        assertThat(service.list(42L))
                .singleElement()
                .returns(created.id(), CdpWriteKeyFacade.KeyRow::id)
                .returns(created.keyPrefix(), CdpWriteKeyFacade.KeyRow::keyPrefix)
                .returns("ACTIVE", CdpWriteKeyFacade.KeyRow::status)
                .returns("browser ingestion", CdpWriteKeyFacade.KeyRow::description);
    }

    @Test
    void disableIsTenantScopedAndIdempotentlyMarksKeyDisabled() {
        CdpWriteKeyFacade service = new CdpWriteKeyApplicationService();
        CdpWriteKeyFacade.CreateResult created = service.create(42L,
                new CdpWriteKeyFacade.CreateCommand("Server", "SERVER", 80, 8000L, null), "operator-1");

        service.disable(42L, created.id());
        service.disable(42L, created.id());

        assertThat(service.list(42L))
                .singleElement()
                .returns("DISABLED", CdpWriteKeyFacade.KeyRow::status);
        assertThatThrownBy(() -> service.disable(7L, created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write key is not found");
    }

    @Test
    void validationUsesCompatibilityMessagesAndDefaultsQuotas() {
        CdpWriteKeyFacade service = new CdpWriteKeyApplicationService();

        CdpWriteKeyFacade.CreateResult created = service.create(42L,
                new CdpWriteKeyFacade.CreateCommand("Mobile", null, null, null, " "), " ");

        assertThat(created)
                .returns("WEB", CdpWriteKeyFacade.CreateResult::platform)
                .returns(100, CdpWriteKeyFacade.CreateResult::rateLimitQps)
                .returns(null, CdpWriteKeyFacade.CreateResult::dailyQuota);
        assertThat(service.list(42L)).singleElement()
                .returns("system", CdpWriteKeyFacade.KeyRow::createdBy);

        assertThatThrownBy(() -> service.create(42L,
                new CdpWriteKeyFacade.CreateCommand(" ", "WEB", 1, 1L, null), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be blank");
        assertThatThrownBy(() -> service.disable(42L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write key is not found");
    }
}
