package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyTombstoneControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    void createUsesCurrentTenantAndRequestBody() {
        CdpWarehousePrivacyTombstoneService service =
                mock(CdpWarehousePrivacyTombstoneService.class);
        CdpWarehousePrivacyTombstoneService.TombstoneCommand command =
                new CdpWarehousePrivacyTombstoneService.TombstoneCommand(
                        "USER_ID", "user-123456", 201L, "dsr-201",
                        "GDPR delete", "privacy-ops");
        CdpWarehousePrivacyTombstoneService.TombstoneView view = view(101L, "ACTIVE");
        when(service.create(9L, command)).thenReturn(view);
        CdpWarehousePrivacyTombstoneController controller =
                new CdpWarehousePrivacyTombstoneController(service, tenantResolver(9L));

        R<CdpWarehousePrivacyTombstoneService.TombstoneView> response =
                controller.create(command).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).create(9L, command);
    }

    @Test
    void revokeUsesCurrentTenantAndPathId() {
        CdpWarehousePrivacyTombstoneService service =
                mock(CdpWarehousePrivacyTombstoneService.class);
        CdpWarehousePrivacyTombstoneService.RevokeCommand command =
                new CdpWarehousePrivacyTombstoneService.RevokeCommand("privacy-reviewer");
        CdpWarehousePrivacyTombstoneService.TombstoneView view = view(101L, "REVOKED");
        when(service.revoke(9L, 101L, command)).thenReturn(view);
        CdpWarehousePrivacyTombstoneController controller =
                new CdpWarehousePrivacyTombstoneController(service, tenantResolver(9L));

        R<CdpWarehousePrivacyTombstoneService.TombstoneView> response =
                controller.revoke(101L, command).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).revoke(9L, 101L, command);
    }

    @Test
    void createFromErasureRequestUsesCurrentTenantAndRequestEvidenceCommand() {
        CdpWarehousePrivacyTombstoneService service =
                mock(CdpWarehousePrivacyTombstoneService.class);
        CdpWarehousePrivacyTombstoneService.ErasureRequestTombstoneCommand command =
                new CdpWarehousePrivacyTombstoneService.ErasureRequestTombstoneCommand(
                        201L, "post-erasure guard", "privacy-ops");
        CdpWarehousePrivacyTombstoneService.TombstoneView view = view(101L, "ACTIVE");
        when(service.createFromErasureRequest(9L, command)).thenReturn(view);
        CdpWarehousePrivacyTombstoneController controller =
                new CdpWarehousePrivacyTombstoneController(service, tenantResolver(9L));

        R<CdpWarehousePrivacyTombstoneService.TombstoneView> response =
                controller.createFromErasureRequest(command).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).createFromErasureRequest(9L, command);
    }

    @Test
    void listAndDecisionUseCurrentTenantAndNeverExposeRawSubjectInFixture() {
        CdpWarehousePrivacyTombstoneService service =
                mock(CdpWarehousePrivacyTombstoneService.class);
        List<CdpWarehousePrivacyTombstoneService.TombstoneView> rows =
                List.of(view(101L, "ACTIVE"));
        CdpWarehousePrivacyTombstoneService.TombstoneDecision decision =
                new CdpWarehousePrivacyTombstoneService.TombstoneDecision(
                        9L, "USER_ID", "hash-101", "us***56",
                        true, 101L, "dsr-201",
                        "subject is blocked by active privacy tombstone");
        when(service.list(9L, "ACTIVE", 10)).thenReturn(rows);
        when(service.decide(9L, "USER_ID", "user-123456")).thenReturn(decision);
        CdpWarehousePrivacyTombstoneController controller =
                new CdpWarehousePrivacyTombstoneController(service, tenantResolver(9L));

        R<List<CdpWarehousePrivacyTombstoneService.TombstoneView>> listResponse =
                controller.list("ACTIVE", 10).block();
        R<CdpWarehousePrivacyTombstoneService.TombstoneDecision> decisionResponse =
                controller.decision("USER_ID", "user-123456").block();

        assertThat(listResponse.getData()).isSameAs(rows);
        assertThat(decisionResponse.getData()).isSameAs(decision);
        assertThat(decisionResponse.getData().subjectRefMasked()).isEqualTo("us***56");
        verify(service).list(9L, "ACTIVE", 10);
        verify(service).decide(9L, "USER_ID", "user-123456");
    }

    private CdpWarehousePrivacyTombstoneService.TombstoneView view(Long id, String status) {
        return new CdpWarehousePrivacyTombstoneService.TombstoneView(
                id,
                9L,
                "USER_ID",
                "hash-" + id,
                "us***56",
                status,
                201L,
                "dsr-201",
                "GDPR delete",
                0L,
                null,
                "privacy-ops",
                "REVOKED".equals(status) ? "privacy-reviewer" : null,
                "REVOKED".equals(status) ? NOW.plusMinutes(5) : null,
                NOW,
                NOW);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
