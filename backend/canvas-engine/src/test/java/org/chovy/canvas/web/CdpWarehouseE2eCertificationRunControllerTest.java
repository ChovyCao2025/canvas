package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationRunService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseE2eCertificationRunControllerTest {

    @Test
    void routeKeepsLegacyPathAndAddsCanonicalNestedPath() {
        RequestMapping mapping = CdpWarehouseE2eCertificationRunController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value())
                .containsExactlyInAnyOrder(
                        "/warehouse/e2e-certification-runs",
                        "/warehouse/e2e-certification/runs");
    }

    @Test
    void runCertificationUsesCurrentTenantAndRequestParameters() {
        CdpWarehouseE2eCertificationRunService service = mock(CdpWarehouseE2eCertificationRunService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehouseE2eCertificationRunService.CertificationRunView run =
                runView(101L, 9L, "PASS");
        when(service.run(9L, from, to, "HYBRID",
                List.of("bi_daily_active_users", "audience_12"), true, true, true, "qa"))
                .thenReturn(run);
        CdpWarehouseE2eCertificationRunController controller =
                new CdpWarehouseE2eCertificationRunController(service, tenantResolver(9L));

        R<CdpWarehouseE2eCertificationRunService.CertificationRunView> response =
                controller.runCertification(from, to, "HYBRID",
                        List.of("bi_daily_active_users", "audience_12"), true, true, true, "qa").block();

        assertThat(response.getData()).isSameAs(run);
        verify(service).run(9L, from, to, "HYBRID",
                List.of("bi_daily_active_users", "audience_12"), true, true, true, "qa");
    }

    @Test
    void recentRunsUseCurrentTenantAndLimit() {
        CdpWarehouseE2eCertificationRunService service = mock(CdpWarehouseE2eCertificationRunService.class);
        List<CdpWarehouseE2eCertificationRunService.CertificationRunView> runs =
                List.of(runView(101L, 9L, "PASS"));
        when(service.recent(9L, 50)).thenReturn(runs);
        CdpWarehouseE2eCertificationRunController controller =
                new CdpWarehouseE2eCertificationRunController(service, tenantResolver(9L));

        R<List<CdpWarehouseE2eCertificationRunService.CertificationRunView>> response =
                controller.recentRuns(50).block();

        assertThat(response.getData()).isSameAs(runs);
        verify(service).recent(9L, 50);
    }

    @Test
    void getRunUsesCurrentTenantAndId() {
        CdpWarehouseE2eCertificationRunService service = mock(CdpWarehouseE2eCertificationRunService.class);
        CdpWarehouseE2eCertificationRunService.CertificationRunView run =
                runView(101L, 9L, "PASS");
        when(service.get(9L, 101L)).thenReturn(run);
        CdpWarehouseE2eCertificationRunController controller =
                new CdpWarehouseE2eCertificationRunController(service, tenantResolver(9L));

        R<CdpWarehouseE2eCertificationRunService.CertificationRunView> response =
                controller.getRun(101L).block();

        assertThat(response.getData()).isSameAs(run);
        verify(service).get(9L, 101L);
    }

    private CdpWarehouseE2eCertificationRunService.CertificationRunView runView(
            Long id,
            Long tenantId,
            String status) {
        return new CdpWarehouseE2eCertificationRunService.CertificationRunView(
                id,
                tenantId,
                status,
                "HYBRID",
                true,
                true,
                true,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "[\"audience_12\"]",
                "[{\"key\":\"doris_jdbc_connectivity\"}]",
                "{}",
                "{}",
                "{}",
                "{}",
                "{}",
                "operator",
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                null);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
