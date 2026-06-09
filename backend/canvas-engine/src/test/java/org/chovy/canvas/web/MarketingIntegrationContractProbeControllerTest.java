package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractProbeControllerTest {

    @Test
    void recordProbeUsesCurrentTenantAndOperator() {
        MarketingIntegrationContractProbeService service =
                mock(MarketingIntegrationContractProbeService.class);
        MarketingIntegrationContractProbeCommand command = command();
        when(service.recordProbe(7L, 10L, command, "operator-1")).thenReturn(view("PASS"));
        MarketingIntegrationContractProbeController controller =
                new MarketingIntegrationContractProbeController(service, resolver());

        StepVerifier.create(controller.recordProbe(10L, command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().contractId()).isEqualTo(10L);
                    assertThat(response.getData().status()).isEqualTo("PASS");
                })
                .verifyComplete();

        verify(service).recordProbe(7L, 10L, command, "operator-1");
    }

    @Test
    void listContractProbesUsesCurrentTenantAndLimit() {
        MarketingIntegrationContractProbeService service =
                mock(MarketingIntegrationContractProbeService.class);
        when(service.listContractProbes(7L, 10L, 20)).thenReturn(List.of(view("PASS")));
        MarketingIntegrationContractProbeController controller =
                new MarketingIntegrationContractProbeController(service, resolver());

        StepVerifier.create(controller.listContractProbes(10L, 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.probeKey()).isEqualTo("live-connectivity")))
                .verifyComplete();

        verify(service).listContractProbes(7L, 10L, 20);
    }

    @Test
    void listRecentProbesUsesCurrentTenantAndStatusFilter() {
        MarketingIntegrationContractProbeService service =
                mock(MarketingIntegrationContractProbeService.class);
        when(service.listRecentProbes(7L, "FAIL", 50)).thenReturn(List.of(view("FAIL")));
        MarketingIntegrationContractProbeController controller =
                new MarketingIntegrationContractProbeController(service, resolver());

        StepVerifier.create(controller.listRecentProbes("FAIL", 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.status()).isEqualTo("FAIL")))
                .verifyComplete();

        verify(service).listRecentProbes(7L, "FAIL", 50);
    }

    private static TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private static MarketingIntegrationContractProbeCommand command() {
        return new MarketingIntegrationContractProbeCommand(
                "live-connectivity",
                "PRODUCTION",
                "PASS",
                200,
                123L,
                null,
                null,
                null,
                null,
                LocalDateTime.parse("2026-06-06T10:00:00"),
                Map.of("traceId", "trace-1"));
    }

    private static MarketingIntegrationContractProbeView view(String status) {
        return new MarketingIntegrationContractProbeView(
                100L,
                7L,
                10L,
                "google-ads-keyword-write",
                "live-connectivity",
                "PRODUCTION",
                status,
                200,
                123L,
                null,
                null,
                null,
                null,
                LocalDateTime.parse("2026-06-06T10:00:00"),
                Map.of("traceId", "trace-1"),
                "operator-1",
                null);
    }
}
