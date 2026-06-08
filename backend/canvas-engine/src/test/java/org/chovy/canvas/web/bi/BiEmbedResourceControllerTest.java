package org.chovy.canvas.web.bi;

import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateView;
import org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistry;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketRequest;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
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
import static org.mockito.Mockito.times;
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

    @Test
    void returnsPortalResourceUsingSignedTicketTenantAndUser() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "PORTAL",
                "executive-home",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                null
        )).ticket();
        BiPortalRuntimeService portalRuntimeService = mock(BiPortalRuntimeService.class);
        BiPortalResource portal = new BiPortalResource(
                "executive-home",
                "Executive Home",
                Map.of("title", "Executive Portal"),
                List.of(new BiPortalMenuResource(
                        "overview",
                        null,
                        "Overview",
                        "DASHBOARD",
                        "canvas-effect",
                        null,
                        null,
                        Map.of(),
                        1)),
                "PUBLISHED",
                "PERSISTED");
        when(portalRuntimeService.getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer")))
                .thenReturn(portal);
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                mock(BiDashboardResourceService.class),
                mock(BiDashboardRuntimeStateService.class),
                portalRuntimeService);

        StepVerifier.create(controller.getPortalResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "PORTAL",
                                "executive-home"),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> {
                    assertThat(response.getData().portalKey()).isEqualTo("executive-home");
                    assertThat(response.getData().menus()).hasSize(1);
                    assertThat(response.getData().theme()).containsEntry("title", "Executive Portal");
                })
                .verifyComplete();

        verify(portalRuntimeService).getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer"));
    }

    @Test
    void returnsDashboardResourceForPortalMenuDashboardUsingSignedPortalTicket() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "PORTAL",
                "executive-home",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                null
        )).ticket();
        BiDashboardResourceService dashboardResourceService = mock(BiDashboardResourceService.class);
        BiPortalRuntimeService portalRuntimeService = mock(BiPortalRuntimeService.class);
        when(portalRuntimeService.getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer")))
                .thenReturn(portalWithMenus(List.of(
                        new BiPortalMenuResource(
                                "overview",
                                null,
                                "Overview",
                                "DASHBOARD",
                                "canvas-effect",
                                null,
                                null,
                                Map.of(),
                                1))));
        when(dashboardResourceService.get(7L, "canvas-effect")).thenReturn(new BiDashboardResource(
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"),
                "PUBLISHED",
                4,
                "PERSISTED"));
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                dashboardResourceService,
                mock(BiDashboardRuntimeStateService.class),
                portalRuntimeService);

        StepVerifier.create(controller.getDashboardResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "canvas-effect"),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> assertThat(response.getData().preset().dashboardKey()).isEqualTo("canvas-effect"))
                .verifyComplete();

        verify(portalRuntimeService, times(2)).getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer"));
        verify(dashboardResourceService).get(7L, "canvas-effect");
    }

    @Test
    void returnsDashboardRuntimeStateForPortalMenuDashboardUsingSignedPortalTicket() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "PORTAL",
                "executive-home",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                null
        )).ticket();
        BiDashboardRuntimeStateService runtimeStateService = mock(BiDashboardRuntimeStateService.class);
        BiPortalRuntimeService portalRuntimeService = mock(BiPortalRuntimeService.class);
        when(portalRuntimeService.getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer")))
                .thenReturn(portalWithMenus(List.of(
                        new BiPortalMenuResource(
                                "overview",
                                null,
                                "Overview",
                                "DASHBOARD",
                                "canvas-effect",
                                null,
                                null,
                                Map.of(),
                                1))));
        when(runtimeStateService.get(7L, "external-viewer", "canvas-effect"))
                .thenReturn(new BiDashboardRuntimeStateView(
                        "canvas-effect",
                        "external-viewer",
                        Map.of("filter-canvas", "Remembered Portal Journey"),
                        LocalDateTime.parse("2026-06-05T09:35:00")));
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                mock(BiDashboardResourceService.class),
                runtimeStateService,
                portalRuntimeService);

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
                    assertThat(response.getData().parameters()).containsEntry("filter-canvas", "Remembered Portal Journey");
                })
                .verifyComplete();

        verify(portalRuntimeService, times(2)).getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer"));
        verify(runtimeStateService).get(7L, "external-viewer", "canvas-effect");
    }

    @Test
    void rejectsDashboardRuntimeStateForPortalTicketWhenDashboardIsNotInPortalMenus() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "PORTAL",
                "executive-home",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                null
        )).ticket();
        BiDashboardRuntimeStateService runtimeStateService = mock(BiDashboardRuntimeStateService.class);
        BiPortalRuntimeService portalRuntimeService = mock(BiPortalRuntimeService.class);
        when(portalRuntimeService.getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer")))
                .thenReturn(portalWithMenus(List.of(
                        new BiPortalMenuResource(
                                "overview",
                                null,
                                "Overview",
                                "DASHBOARD",
                                "canvas-effect",
                                null,
                                null,
                                Map.of(),
                                1))));
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                mock(BiDashboardResourceService.class),
                runtimeStateService,
                portalRuntimeService);

        StepVerifier.create(controller.getDashboardRuntimeState(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "sales-dashboard"),
                        "https://reports.example.com",
                        null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("portal menu"))
                .verify();

        verify(runtimeStateService, never()).get(7L, "external-viewer", "sales-dashboard");
    }

    @Test
    void rejectsDashboardResourceForPortalTicketWhenDashboardIsNotInPortalMenus() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "PORTAL",
                "executive-home",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                null
        )).ticket();
        BiDashboardResourceService dashboardResourceService = mock(BiDashboardResourceService.class);
        BiPortalRuntimeService portalRuntimeService = mock(BiPortalRuntimeService.class);
        when(portalRuntimeService.getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer")))
                .thenReturn(portalWithMenus(List.of(
                        new BiPortalMenuResource(
                                "overview",
                                null,
                                "Overview",
                                "DASHBOARD",
                                "canvas-effect",
                                null,
                                null,
                                Map.of(),
                                1))));
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                dashboardResourceService,
                mock(BiDashboardRuntimeStateService.class),
                portalRuntimeService);

        StepVerifier.create(controller.getDashboardResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "DASHBOARD",
                                "sales-dashboard"),
                        "https://reports.example.com",
                        null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("portal menu"))
                .verify();

        verify(dashboardResourceService, never()).get(7L, "sales-dashboard");
    }

    @Test
    void rejectsPortalResourceWhenTicketTargetsDashboardBeforeConsumingAccess() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                1,
                null
        )).ticket();
        BiPortalRuntimeService portalRuntimeService = mock(BiPortalRuntimeService.class);
        BiEmbedResourceController controller = new BiEmbedResourceController(
                embedTicketService,
                mock(BiDashboardResourceService.class),
                mock(BiDashboardRuntimeStateService.class),
                portalRuntimeService);

        StepVerifier.create(controller.getPortalResource(
                        new BiEmbedResourceController.EmbedDashboardResourceRequest(
                                ticket,
                                "PORTAL",
                                "executive-home"),
                        "https://reports.example.com",
                        null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("portal tickets"))
                .verify();

        verify(portalRuntimeService, never()).getPublished(7L, "executive-home", new BiQueryContext(7L, "external-viewer"));
    }

    private BiPortalResource portalWithMenus(List<BiPortalMenuResource> menus) {
        return new BiPortalResource(
                "executive-home",
                "Executive Home",
                Map.of("title", "Executive Portal"),
                menus,
                "PUBLISHED",
                "PERSISTED");
    }
}
