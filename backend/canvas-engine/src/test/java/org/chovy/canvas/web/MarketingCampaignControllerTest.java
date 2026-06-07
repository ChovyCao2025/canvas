package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingCampaignCommand;
import org.chovy.canvas.domain.marketing.MarketingCampaignLinkCommand;
import org.chovy.canvas.domain.marketing.MarketingCampaignLinkView;
import org.chovy.canvas.domain.marketing.MarketingCampaignReadinessView;
import org.chovy.canvas.domain.marketing.MarketingCampaignService;
import org.chovy.canvas.domain.marketing.MarketingCampaignView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingCampaignControllerTest {

    @Test
    void upsertCampaignUsesCurrentTenantAndOperator() {
        MarketingCampaignService service = mock(MarketingCampaignService.class);
        MarketingCampaignCommand command = campaignCommand();
        when(service.upsertCampaign(7L, command, "operator-1")).thenReturn(campaignView());
        MarketingCampaignController controller = new MarketingCampaignController(service, resolver());

        StepVerifier.create(controller.upsertCampaign(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().campaignKey()).isEqualTo("spring-launch");
                })
                .verifyComplete();

        verify(service).upsertCampaign(7L, command, "operator-1");
    }

    @Test
    void listCampaignsUsesCurrentTenantAndFilters() {
        MarketingCampaignService service = mock(MarketingCampaignService.class);
        when(service.listCampaigns(7L, "ACTIVE", 20)).thenReturn(List.of(campaignView()));
        MarketingCampaignController controller = new MarketingCampaignController(service, resolver());

        StepVerifier.create(controller.listCampaigns("ACTIVE", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.id()).isEqualTo(10L)))
                .verifyComplete();

        verify(service).listCampaigns(7L, "ACTIVE", 20);
    }

    @Test
    void linkResourceAndListLinksUseCurrentTenant() {
        MarketingCampaignService service = mock(MarketingCampaignService.class);
        MarketingCampaignLinkCommand command = linkCommand();
        when(service.linkResource(7L, command, "operator-1")).thenReturn(linkView());
        when(service.listLinks(7L, 10L)).thenReturn(List.of(linkView()));
        MarketingCampaignController controller = new MarketingCampaignController(service, resolver());

        StepVerifier.create(controller.linkResource(command))
                .assertNext(response -> assertThat(response.getData().resourceKey()).isEqualTo("launch-journey"))
                .verifyComplete();
        StepVerifier.create(controller.listLinks(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.campaignId()).isEqualTo(10L)))
                .verifyComplete();

        verify(service).linkResource(7L, command, "operator-1");
        verify(service).listLinks(7L, 10L);
    }

    @Test
    void unlinkResourceUsesCurrentTenant() {
        MarketingCampaignService service = mock(MarketingCampaignService.class);
        MarketingCampaignController controller = new MarketingCampaignController(service, resolver());

        StepVerifier.create(controller.unlinkResource(20L))
                .assertNext(response -> assertThat(response.getCode()).isZero())
                .verifyComplete();

        verify(service).unlinkResource(7L, 20L);
    }

    @Test
    void readinessUsesCurrentTenant() {
        MarketingCampaignService service = mock(MarketingCampaignService.class);
        when(service.readiness(7L, 10L)).thenReturn(readinessView());
        MarketingCampaignController controller = new MarketingCampaignController(service, resolver());

        StepVerifier.create(controller.readiness(10L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().status()).isEqualTo("READY");
                    assertThat(response.getData().productionReady()).isTrue();
                })
                .verifyComplete();

        verify(service).readiness(7L, 10L);
    }

    private static TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private static MarketingCampaignCommand campaignCommand() {
        return new MarketingCampaignCommand(
                "spring-launch",
                "Spring launch",
                "ACQUISITION",
                "ACTIVE",
                "PAID_MEDIA",
                "Growth",
                null,
                null,
                new BigDecimal("1200.00"),
                "CNY",
                Map.of());
    }

    private static MarketingCampaignLinkCommand linkCommand() {
        return new MarketingCampaignLinkCommand(
                10L,
                "JOURNEY",
                300L,
                "launch-journey",
                "Launch journey",
                "/canvas/300",
                "PRIMARY",
                "ACTIVE",
                true,
                Map.of());
    }

    private static MarketingCampaignView campaignView() {
        return new MarketingCampaignView(
                10L,
                7L,
                "spring-launch",
                "Spring launch",
                "ACQUISITION",
                "ACTIVE",
                "PAID_MEDIA",
                "Growth",
                null,
                null,
                new BigDecimal("1200.00"),
                "CNY",
                Map.of(),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static MarketingCampaignLinkView linkView() {
        return new MarketingCampaignLinkView(
                20L,
                7L,
                10L,
                "JOURNEY",
                300L,
                "launch-journey",
                "Launch journey",
                "/canvas/300",
                "PRIMARY",
                "ACTIVE",
                true,
                Map.of(),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static MarketingCampaignReadinessView readinessView() {
        return new MarketingCampaignReadinessView(
                7L,
                10L,
                "spring-launch",
                "Spring launch",
                "2026-06-06T10:00",
                "READY",
                true,
                2,
                2,
                0,
                0,
                List.of(),
                List.of(),
                List.of(linkView()));
    }
}
