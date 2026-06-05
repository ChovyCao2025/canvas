package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePhysicalE2eCertificationService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePhysicalE2eCertificationControllerTest {

    @Test
    void e2eCertificationUsesCurrentTenantWindowModeContractsAndRequirementFlags() {
        CdpWarehousePhysicalE2eCertificationService service =
                mock(CdpWarehousePhysicalE2eCertificationService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                certification(9L, from, to, "HYBRID", true, true);
        when(service.certify(9L, from, to, "HYBRID",
                List.of("bi_daily_active_users", "audience_12"), true, true, true))
                .thenReturn(certification);
        CdpWarehousePhysicalE2eCertificationController controller =
                new CdpWarehousePhysicalE2eCertificationController(service, tenantResolver(9L));

        R<CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification> response =
                controller.e2eCertification(from, to, "HYBRID",
                        List.of("bi_daily_active_users", "audience_12"), true, true, true).block();

        assertThat(response.getData()).isSameAs(certification);
        verify(service).certify(9L, from, to, "HYBRID",
                List.of("bi_daily_active_users", "audience_12"), true, true, true);
    }

    @Test
    void e2eCertificationDefaultsTenantWhenResolverIsAbsent() {
        CdpWarehousePhysicalE2eCertificationService service =
                mock(CdpWarehousePhysicalE2eCertificationService.class);
        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                certification(0L, null, null, "HYBRID", true, true);
        when(service.certify(0L, null, null, "HYBRID", List.of(), true, true, true))
                .thenReturn(certification);
        CdpWarehousePhysicalE2eCertificationController controller =
                new CdpWarehousePhysicalE2eCertificationController(service);

        R<CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification> response =
                controller.e2eCertification(null, null, "HYBRID", null, true, true, true).block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).certify(0L, null, null, "HYBRID", List.of(), true, true, true);
    }

    private CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            boolean requirePhysical,
            boolean requireDataPathProof) {
        return new CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification(
                tenantId,
                "PASS",
                LocalDateTime.of(2026, 6, 5, 11, 1),
                from,
                to,
                mode,
                requirePhysical,
                true,
                requireDataPathProof,
                List.of(new CdpWarehousePhysicalE2eCertificationService.CertificationEvidence(
                        "production_readiness", "PASS", "ok")),
                null,
                null,
                null,
                null,
                null);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
