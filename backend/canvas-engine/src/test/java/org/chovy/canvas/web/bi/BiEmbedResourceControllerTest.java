package org.chovy.canvas.web.bi;

import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateView;
import org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistry;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketRequest;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiEmbedResourceControllerTest {

    private static final String EMBED_SECRET = "bi-embed-test-secret-with-at-least-32-bytes";
    private static final Clock EMBED_CLOCK = Clock.fixed(Instant.parse("2026-06-05T01:50:00Z"), ZoneOffset.UTC);

    @Test
    void returnsDashboardResourceUsingSignedTicketTenant() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com"),
                3,
                null
        )).ticket();
        BiDashboardResourceService dashboardResourceService = mock(BiDashboardResourceService.class);
        var resource = new BiDashboardResource(
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"),
                "PUBLISHED",
                4,
                "PERSISTED");
        when(dashboardResourceService.get(7L, "canvas-effect")).thenReturn(resource);
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                dashboardResourceService);

        StepVerifier.create(controller.getDashboardResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "canvas-effect"),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> {
                    assertThat(response.getData().preset().dashboardKey()).isEqualTo("canvas-effect");
                    assertThat(response.getData().status()).isEqualTo("PUBLISHED");
                    assertThat(response.getData().version()).isEqualTo(4);
                    assertThat(response.getData().source()).isEqualTo("PERSISTED");
                })
                .verifyComplete();

        verify(dashboardResourceService).get(7L, "canvas-effect");
    }

    @Test
    void rejectsDashboardResourceWhenTicketDoesNotMatchRequestedResourceBeforeConsumingAccess() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com"),
                1,
                null
        )).ticket();
        BiDashboardResourceService dashboardResourceService = mock(BiDashboardResourceService.class);
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                dashboardResourceService);

        StepVerifier.create(controller.getDashboardResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "other-dashboard"),
                        "https://reports.example.com",
                        null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("does not match"))
                .verify();

        verify(dashboardResourceService, never()).get(7L, "other-dashboard");

        when(dashboardResourceService.get(7L, "canvas-effect")).thenReturn(new BiDashboardResource(
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"),
                "PUBLISHED",
                4,
                "PERSISTED"));

        StepVerifier.create(controller.getDashboardResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "canvas-effect"),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> assertThat(response.getData().preset().dashboardKey()).isEqualTo("canvas-effect"))
                .verifyComplete();
    }

    @Test
    void returnsDashboardRuntimeStateUsingSignedTicketTenantUserAndResource() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com"),
                2,
                null
        )).ticket();
        BiDashboardResourceService dashboardResourceService = mock(BiDashboardResourceService.class);
        BiDashboardRuntimeStateService runtimeStateService = mock(BiDashboardRuntimeStateService.class);
        when(runtimeStateService.get(7L, "external-viewer", "canvas-effect"))
                .thenReturn(new BiDashboardRuntimeStateView(
                        "canvas-effect",
                        "external-viewer",
                        Map.of("filter-canvas", "Remembered Journey"),
                        LocalDateTime.parse("2026-06-05T09:30:00")));
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                dashboardResourceService,
                runtimeStateService);

        StepVerifier.create(controller.getDashboardRuntimeState(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "canvas-effect"),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> {
                    assertThat(response.getData().dashboardKey()).isEqualTo("canvas-effect");
                    assertThat(response.getData().username()).isEqualTo("external-viewer");
                    assertThat(response.getData().parameters()).containsEntry("filter-canvas", "Remembered Journey");
                })
                .verifyComplete();

        verify(runtimeStateService).get(7L, "external-viewer", "canvas-effect");
    }

    @Test
    void rejectsDashboardRuntimeStateWhenTicketDoesNotMatchRequestedResourceBeforeConsumingAccess() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com"),
                1,
                null
        )).ticket();
        BiDashboardRuntimeStateService runtimeStateService = mock(BiDashboardRuntimeStateService.class);
        when(runtimeStateService.get(7L, "external-viewer", "canvas-effect"))
                .thenReturn(new BiDashboardRuntimeStateView(
                        "canvas-effect",
                        "external-viewer",
                        Map.of("filter-canvas", "Remembered Journey"),
                        LocalDateTime.parse("2026-06-05T09:30:00")));
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                mock(BiDashboardResourceService.class),
                runtimeStateService);

        StepVerifier.create(controller.getDashboardRuntimeState(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "other-dashboard"),
                        "https://reports.example.com",
                        null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("does not match"))
                .verify();

        verify(runtimeStateService, never()).get(7L, "external-viewer", "other-dashboard");

        StepVerifier.create(controller.getDashboardRuntimeState(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "canvas-effect"),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> assertThat(response.getData().parameters())
                        .containsEntry("filter-canvas", "Remembered Journey"))
                .verifyComplete();
    }
}
