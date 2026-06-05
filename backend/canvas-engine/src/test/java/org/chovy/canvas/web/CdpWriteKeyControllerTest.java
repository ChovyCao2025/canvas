package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpWriteKeyDO;
import org.chovy.canvas.domain.cdp.CdpWriteKeyAuthService;
import org.chovy.canvas.dto.cdp.CdpWriteKeyCreateReq;
import org.chovy.canvas.dto.cdp.CdpWriteKeyCreateResp;
import org.chovy.canvas.dto.cdp.CdpWriteKeyRowDTO;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWriteKeyControllerTest {

    @Test
    void createReturnsRawKeyOnceAndListReturnsOnlyPrefix() {
        CdpWriteKeyAuthService writeKeyService = mock(CdpWriteKeyAuthService.class);
        CdpWriteKeyController controller =
                new CdpWriteKeyController(tenantResolver(42L, "alice"), writeKeyService);
        when(writeKeyService.generateRawKey()).thenReturn("ck_raw_secret");
        when(writeKeyService.create(eq(42L), any(), eq("alice"), eq("ck_raw_secret")))
                .thenReturn(row("ck_raw_secret"));
        when(writeKeyService.listTenantKeys(42L)).thenReturn(List.of(row("ck_raw_secret")));

        CdpWriteKeyCreateResp created = controller.create(
                        new CdpWriteKeyCreateReq("Website", "WEB", 100, null, ""))
                .block()
                .getData();
        List<CdpWriteKeyRowDTO> rows = controller.list().block().getData();

        assertThat(created.writeKey()).isEqualTo("ck_raw_secret");
        assertThat(created.keyPrefix()).isEqualTo("ck_raw_secre");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).keyPrefix()).isEqualTo("ck_raw_secre");
        verify(writeKeyService).listTenantKeys(42L);
    }

    @Test
    void disableUsesCurrentTenant() {
        CdpWriteKeyAuthService writeKeyService = mock(CdpWriteKeyAuthService.class);
        CdpWriteKeyController controller =
                new CdpWriteKeyController(tenantResolver(42L, "alice"), writeKeyService);

        R<Void> response = controller.disable(7L).block();

        assertThat(response.getCode()).isZero();
        verify(writeKeyService).disable(42L, 7L);
    }

    private CdpWriteKeyDO row(String raw) {
        CdpWriteKeyDO row = new CdpWriteKeyDO();
        row.setId(7L);
        row.setTenantId(42L);
        row.setName("Website");
        row.setKeyPrefix(raw.substring(0, Math.min(raw.length(), 12)));
        row.setPlatform("WEB");
        row.setStatus(CdpWriteKeyDO.ACTIVE);
        row.setRateLimitQps(100);
        row.setDescription("");
        row.setCreatedAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        row.setUpdatedAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        return row;
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
