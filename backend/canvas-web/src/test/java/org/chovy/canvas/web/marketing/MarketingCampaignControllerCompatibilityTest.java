package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignFacade;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkView;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessFinding;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessView;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingCampaignControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "growth-operator";
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-06-12T20:40:00");

    @Test
    void upsertCampaignPreservesEnvelopeTenantActorAndCommandMapping() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-campaigns")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "campaignKey": "spring-launch",
                          "campaignName": "Spring launch",
                          "objective": "acquisition",
                          "status": "active",
                          "primaryChannel": "paid_media",
                          "ownerTeam": "Growth",
                          "startAt": "2026-06-13T09:00:00",
                          "endAt": "2026-06-30T18:30:00",
                          "budgetAmount": 1200.50,
                          "currency": "usd",
                          "brief": {"northStar": "signup"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.campaignKey").isEqualTo("spring-launch")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.upsertTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.upsertActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.upsertCommand).isEqualTo(new MarketingCampaignCommand(
                "spring-launch",
                "Spring launch",
                "acquisition",
                "active",
                "paid_media",
                "Growth",
                LocalDateTime.parse("2026-06-13T09:00:00"),
                LocalDateTime.parse("2026-06-30T18:30:00"),
                new BigDecimal("1200.50"),
                "usd",
                Map.of("northStar", "signup")));
    }

    @Test
    void listCampaignsPropagatesStatusLimitAndReturnsArrayEnvelope() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/marketing-campaigns")
                        .queryParam("status", "active")
                        .queryParam("limit", 25)
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].campaignKey").isEqualTo("spring-launch")
                .jsonPath("$.data[0].status").isEqualTo("ACTIVE");

        assertThat(facade.listTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.listStatus).isEqualTo("active");
        assertThat(facade.listLimit).isEqualTo(25);
    }

    @Test
    void linkResourcePreservesEnvelopeAndCommandMapping() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-campaigns/links")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "campaignId": 10,
                          "resourceType": "journey",
                          "resourceId": 300,
                          "resourceKey": "launch-journey",
                          "resourceName": "Launch journey",
                          "resourceRoute": "/canvas/300",
                          "dependencyRole": "primary",
                          "linkStatus": "active",
                          "requiredForLaunch": true,
                          "metadata": {"stage": "launch"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.resourceType").isEqualTo("journey")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.linkTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.linkActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.linkCommand).isEqualTo(new MarketingCampaignLinkCommand(
                10L,
                "journey",
                300L,
                "launch-journey",
                "Launch journey",
                "/canvas/300",
                "primary",
                "active",
                true,
                Map.of("stage", "launch")));
    }

    @Test
    void listLinksAndReadinessReturnCompatibilityEnvelopes() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/marketing-campaigns/10/links")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].campaignId").isEqualTo(10)
                .jsonPath("$.data[0].resourceKey").isEqualTo("launch-journey");

        client.get()
                .uri("/canvas/marketing-campaigns/10/readiness")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.campaignId").isEqualTo(10)
                .jsonPath("$.data.status").isEqualTo("READY")
                .jsonPath("$.data.productionReady").isEqualTo(true)
                .jsonPath("$.data.links").isArray()
                .jsonPath("$.data.links.length()").isEqualTo(1);

        assertThat(facade.listLinksTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.listLinksCampaignId).isEqualTo(10L);
        assertThat(facade.readinessTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.readinessCampaignId).isEqualTo(10L);
    }

    @Test
    void unlinkDelegatesAndReturnsNullDataSuccessEnvelope() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();

        webClient(facade)
                .delete()
                .uri("/canvas/marketing-campaigns/links/20")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.unlinkTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.unlinkLinkId).isEqualTo(20L);
    }

    @Test
    void mutatingRoutesUseDefaultTenantAndActorWhenHeadersAreAbsent() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "campaignKey": "spring-launch",
                          "campaignName": "Spring launch"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.createdBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.upsertTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.upsertActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMarketingCampaignFacade facade = new RecordingMarketingCampaignFacade();
        facade.failUpsert = true;

        webClient(facade)
                .post()
                .uri("/canvas/marketing-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "campaignKey": "bad-campaign"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("campaignKey is invalid")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MarketingCampaignFacade facade) {
        return WebTestClient.bindToController(new MarketingCampaignController(facade)).build();
    }

    private static final class RecordingMarketingCampaignFacade implements MarketingCampaignFacade {
        private Long upsertTenantId;
        private MarketingCampaignCommand upsertCommand;
        private String upsertActor;
        private Long listTenantId;
        private String listStatus;
        private Integer listLimit;
        private Long linkTenantId;
        private MarketingCampaignLinkCommand linkCommand;
        private String linkActor;
        private Long listLinksTenantId;
        private Long listLinksCampaignId;
        private Long readinessTenantId;
        private Long readinessCampaignId;
        private Long unlinkTenantId;
        private Long unlinkLinkId;
        private boolean failUpsert;

        @Override
        public MarketingCampaignView upsertCampaign(Long tenantId, MarketingCampaignCommand command, String actor) {
            if (failUpsert) {
                throw new IllegalArgumentException("campaignKey is invalid");
            }
            upsertTenantId = tenantId;
            upsertCommand = command;
            upsertActor = actor;
            return campaignView(tenantId, command.campaignKey(), command.campaignName(), "ACTIVE", actor);
        }

        @Override
        public List<MarketingCampaignView> listCampaigns(Long tenantId, String status, Integer limit) {
            listTenantId = tenantId;
            listStatus = status;
            listLimit = limit;
            return List.of(campaignView(tenantId, "spring-launch", "Spring launch", "ACTIVE", DEFAULT_ACTOR));
        }

        @Override
        public MarketingCampaignLinkView linkResource(Long tenantId, MarketingCampaignLinkCommand command, String actor) {
            linkTenantId = tenantId;
            linkCommand = command;
            linkActor = actor;
            return linkView(tenantId, command.campaignId(), command.resourceType(), command.resourceKey(), actor);
        }

        @Override
        public List<MarketingCampaignLinkView> listLinks(Long tenantId, Long campaignId) {
            listLinksTenantId = tenantId;
            listLinksCampaignId = campaignId;
            return List.of(linkView(tenantId, campaignId, "JOURNEY", "launch-journey", DEFAULT_ACTOR));
        }

        @Override
        public MarketingCampaignReadinessView readiness(Long tenantId, Long campaignId) {
            readinessTenantId = tenantId;
            readinessCampaignId = campaignId;
            return new MarketingCampaignReadinessView(
                    tenantId,
                    campaignId,
                    "spring-launch",
                    "Spring launch",
                    "2026-06-12T20:40:00Z",
                    "READY",
                    true,
                    1,
                    1,
                    0,
                    0,
                    List.<MarketingCampaignReadinessFinding>of(),
                    List.<MarketingCampaignReadinessFinding>of(),
                    List.of(linkView(tenantId, campaignId, "JOURNEY", "launch-journey", DEFAULT_ACTOR)));
        }

        @Override
        public void unlinkResource(Long tenantId, Long linkId) {
            unlinkTenantId = tenantId;
            unlinkLinkId = linkId;
        }

        private MarketingCampaignView campaignView(Long tenantId,
                                                   String campaignKey,
                                                   String campaignName,
                                                   String status,
                                                   String actor) {
            return new MarketingCampaignView(
                    10L,
                    tenantId,
                    campaignKey,
                    campaignName,
                    "ACQUISITION",
                    status,
                    "PAID_MEDIA",
                    "Growth",
                    null,
                    null,
                    new BigDecimal("1200.50"),
                    "USD",
                    Map.of("northStar", "signup"),
                    actor,
                    actor,
                    NOW,
                    NOW);
        }

        private MarketingCampaignLinkView linkView(Long tenantId,
                                                   Long campaignId,
                                                   String resourceType,
                                                   String resourceKey,
                                                   String actor) {
            return new MarketingCampaignLinkView(
                    20L,
                    tenantId,
                    campaignId,
                    resourceType,
                    300L,
                    resourceKey,
                    "Launch journey",
                    "/canvas/300",
                    "PRIMARY",
                    "ACTIVE",
                    true,
                    Map.of("stage", "launch"),
                    actor,
                    actor,
                    NOW,
                    NOW);
        }
    }
}
