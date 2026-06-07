package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCutoverReadinessService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeCutoverReadinessControllerTest {

    @Test
    void cutoverReadinessUsesTenantAndRequestParameters() {
        CdpWarehouseRealtimeCutoverReadinessService service =
                mock(CdpWarehouseRealtimeCutoverReadinessService.class);
        CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision decision = decision();
        when(service.evaluate(eq(9L), org.mockito.ArgumentMatchers.any())).thenReturn(decision);
        CdpWarehouseRealtimeCutoverReadinessController controller =
                new CdpWarehouseRealtimeCutoverReadinessController(service, tenantResolver());

        R<CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision> response =
                controller.cutoverReadiness(
                        "FLINK_FIRST",
                        List.of("mysql_cdp_event_log_to_doris_ods"),
                        List.of("audience_12"),
                        "HYBRID",
                        60L).block();

        assertThat(response.getData()).isSameAs(decision);
        verify(service).evaluate(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "FLINK_FIRST".equals(command.targetMode())
                        && command.pipelineKeys().equals(List.of("mysql_cdp_event_log_to_doris_ods"))
                        && command.contractKeys().equals(List.of("audience_12"))
                        && "HYBRID".equals(command.certificationMode())
                        && Long.valueOf(60L).equals(command.maxCertificationAgeMinutes())));
    }

    private CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision decision() {
        return new CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision(
                9L,
                "FLINK_FIRST",
                "PASS",
                true,
                "realtime warehouse cutover FLINK_FIRST PASS",
                List.of(new CdpWarehouseRealtimeCutoverReadinessService.CutoverGate(
                        "e2e_certification",
                        "PASS",
                        "fresh PASS",
                        7L,
                        LocalDateTime.parse("2026-06-06T03:55:00"),
                        LocalDateTime.parse("2026-06-06T04:55:00"))));
    }

    private TenantContextResolver tenantResolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(9L, "TENANT_ADMIN", "alice")));
        return resolver;
    }
}
