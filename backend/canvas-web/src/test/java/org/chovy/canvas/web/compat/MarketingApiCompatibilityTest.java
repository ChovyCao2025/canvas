package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignFacade;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkView;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessView;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.chovy.canvas.marketing.application.MarketingCampaignApplicationService;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class MarketingApiCompatibilityTest {

    private static final Long TENANT_ID = 7L;
    private static final String ACTOR = "operator-1";

    @Test
    void createCampaignRoutePreservesSuccessEnvelopeAndNormalizedCampaignFields() {
        webClient(new MarketingCampaignApplicationService(new InMemoryMarketingCampaignRepository()))
                .post()
                .uri("/canvas/marketing-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "campaignKey": " Spring Launch 2026! ",
                          "campaignName": "Spring launch",
                          "objective": "acquisition",
                          "status": "active",
                          "primaryChannel": "paid_media",
                          "ownerTeam": "Growth",
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
                .jsonPath("$.data.campaignKey").isEqualTo("spring-launch-2026")
                .jsonPath("$.data.status").isEqualTo("ACTIVE")
                .jsonPath("$.data.primaryChannel").isEqualTo("PAID_MEDIA")
                .jsonPath("$.data.budgetAmount").value(amount ->
                        assertThat(new BigDecimal(String.valueOf(amount))).isEqualByComparingTo("1200.50"))
                .jsonPath("$.data.currency").isEqualTo("USD");
    }

    @Test
    void listCampaignsRoutePreservesFilterLimitAndArrayEnvelope() {
        InMemoryMarketingCampaignRepository repository = new InMemoryMarketingCampaignRepository();
        repository.save(campaign(10L, TENANT_ID, "spring-launch", CampaignStatus.ACTIVE));
        repository.save(campaign(11L, TENANT_ID, "fall-launch", CampaignStatus.DRAFT));

        webClient(new MarketingCampaignApplicationService(repository))
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/marketing-campaigns")
                        .queryParam("status", "active")
                        .queryParam("limit", 500)
                        .build())
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

        assertThat(repository.lastListLimit).isEqualTo(200);
    }

    @Test
    void linkResourceRoutePreservesDependencyEnvelope() {
        InMemoryMarketingCampaignRepository repository = new InMemoryMarketingCampaignRepository();
        repository.save(campaign(10L, TENANT_ID, "spring-launch", CampaignStatus.ACTIVE));

        webClient(new MarketingCampaignApplicationService(repository))
                .post()
                .uri("/canvas/marketing-campaigns/links")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "campaignId": 10,
                          "resourceType": " journey ",
                          "resourceId": 300,
                          "resourceKey": " Launch Journey#1 ",
                          "resourceName": "Launch journey",
                          "resourceRoute": "/canvas/300",
                          "dependencyRole": "primary",
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
                .jsonPath("$.data.resourceType").isEqualTo("JOURNEY")
                .jsonPath("$.data.resourceKey").isEqualTo("launch-journey-1")
                .jsonPath("$.data.dependencyRole").isEqualTo("PRIMARY")
                .jsonPath("$.data.linkStatus").isEqualTo("ACTIVE")
                .jsonPath("$.data.requiredForLaunch").isEqualTo(true);
    }

    @Test
    void listLinksRoutePreservesLinkedResourceEnvelope() {
        InMemoryMarketingCampaignRepository repository = new InMemoryMarketingCampaignRepository();
        repository.save(campaign(10L, TENANT_ID, "spring-launch", CampaignStatus.ACTIVE));
        repository.saveLink(link(20L, TENANT_ID, 10L, "JOURNEY", "launch-journey", "PRIMARY"));

        webClient(new MarketingCampaignApplicationService(repository))
                .get()
                .uri("/canvas/marketing-campaigns/10/links")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].resourceType").isEqualTo("JOURNEY")
                .jsonPath("$.data[0].resourceKey").isEqualTo("launch-journey")
                .jsonPath("$.data[0].dependencyRole").isEqualTo("PRIMARY")
                .jsonPath("$.data[0].linkStatus").isEqualTo("ACTIVE")
                .jsonPath("$.data[0].requiredForLaunch").isEqualTo(true);
    }

    @Test
    void readinessRoutePreservesLaunchReadinessEnvelope() {
        InMemoryMarketingCampaignRepository repository = new InMemoryMarketingCampaignRepository();
        repository.save(campaign(10L, TENANT_ID, "spring-launch", CampaignStatus.ACTIVE));
        repository.saveLink(link(20L, TENANT_ID, 10L, "JOURNEY", "launch-journey", "PRIMARY"));
        repository.saveLink(link(21L, TENANT_ID, 10L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT"));

        webClient(new MarketingCampaignApplicationService(repository))
                .get()
                .uri("/canvas/marketing-campaigns/10/readiness")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.status").isEqualTo("READY")
                .jsonPath("$.data.productionReady").isEqualTo(true)
                .jsonPath("$.data.activeRequiredLinkCount").isEqualTo(2)
                .jsonPath("$.data.blockerCount").isEqualTo(0)
                .jsonPath("$.data.warningCount").isEqualTo(0)
                .jsonPath("$.data.links").isArray()
                .jsonPath("$.data.links.length()").isEqualTo(2);
    }

    @Test
    void unlinkResourceRoutePreservesSuccessEnvelopeAfterDelegatingUnlink() {
        InMemoryMarketingCampaignRepository repository = new InMemoryMarketingCampaignRepository();
        repository.save(campaign(10L, TENANT_ID, "spring-launch", CampaignStatus.ACTIVE));
        repository.saveLink(link(20L, TENANT_ID, 10L, "JOURNEY", "launch-journey", "PRIMARY"));

        webClient(new MarketingCampaignApplicationService(repository))
                .delete()
                .uri("/canvas/marketing-campaigns/links/20")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success");

        assertThat(repository.deletedLinkIds).containsExactly(20L);
        assertThat(repository.findLinkById(TENANT_ID, 20L)).isNull();
    }

    private static WebTestClient webClient(MarketingCampaignFacade facade) {
        return WebTestClient.bindToController(new MarketingCampaignControllerAdapter(facade)).build();
    }

    private static MarketingCampaign campaign(Long id, Long tenantId, String key, CampaignStatus status) {
        return MarketingCampaign.createExisting(
                id,
                tenantId,
                CampaignKey.of(key, "campaignKey"),
                key,
                "ACQUISITION",
                status,
                "PAID_MEDIA",
                "Growth",
                null,
                null,
                BigDecimal.ZERO,
                "CNY",
                Map.of(),
                ACTOR,
                ACTOR,
                null,
                null);
    }

    private static MarketingCampaignLink link(Long id,
                                              Long tenantId,
                                              Long campaignId,
                                              String type,
                                              String key,
                                              String role) {
        return MarketingCampaignLink.createExisting(
                id,
                tenantId,
                campaignId,
                type,
                300L + id,
                CampaignKey.of(key, "resourceKey"),
                key,
                "/resources/" + key,
                role,
                "ACTIVE",
                true,
                Map.of(),
                ACTOR,
                ACTOR,
                null,
                null);
    }

    @RestController
    @RequestMapping("/canvas/marketing-campaigns")
    private static final class MarketingCampaignControllerAdapter {
        private final MarketingCampaignFacade facade;

        private MarketingCampaignControllerAdapter(MarketingCampaignFacade facade) {
            this.facade = facade;
        }

        @PostMapping
        Mono<CompatibilityEnvelope<MarketingCampaignView>> upsertCampaign(@RequestBody MarketingCampaignCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.upsertCampaign(TENANT_ID, command, ACTOR)));
        }

        @GetMapping
        Mono<CompatibilityEnvelope<List<MarketingCampaignView>>> listCampaigns(
                @RequestParam(required = false) String status,
                @RequestParam(required = false) Integer limit) {
            return Mono.just(CompatibilityEnvelope.ok(facade.listCampaigns(TENANT_ID, status, limit)));
        }

        @PostMapping("/links")
        Mono<CompatibilityEnvelope<MarketingCampaignLinkView>> linkResource(
                @RequestBody MarketingCampaignLinkCommand command) {
            return Mono.just(CompatibilityEnvelope.ok(facade.linkResource(TENANT_ID, command, ACTOR)));
        }

        @GetMapping("/{campaignId}/links")
        Mono<CompatibilityEnvelope<List<MarketingCampaignLinkView>>> listLinks(@PathVariable Long campaignId) {
            return Mono.just(CompatibilityEnvelope.ok(facade.listLinks(TENANT_ID, campaignId)));
        }

        @GetMapping("/{campaignId}/readiness")
        Mono<CompatibilityEnvelope<MarketingCampaignReadinessView>> readiness(@PathVariable Long campaignId) {
            return Mono.just(CompatibilityEnvelope.ok(facade.readiness(TENANT_ID, campaignId)));
        }

        @DeleteMapping("/links/{linkId}")
        Mono<CompatibilityEnvelope<Void>> unlinkResource(@PathVariable Long linkId) {
            facade.unlinkResource(TENANT_ID, linkId);
            return Mono.just(CompatibilityEnvelope.ok());
        }
    }

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Void> ok() {
            return ok(null);
        }
    }

    private static final class InMemoryMarketingCampaignRepository implements MarketingCampaignRepository {
        private final Map<Long, MarketingCampaign> campaignsById = new LinkedHashMap<>();
        private final Map<Long, MarketingCampaignLink> linksById = new LinkedHashMap<>();
        private final List<Long> deletedLinkIds = new ArrayList<>();
        private long nextCampaignId = 100L;
        private long nextLinkId = 200L;
        private int lastListLimit;

        @Override
        public MarketingCampaign findByTenantAndKey(Long tenantId, CampaignKey campaignKey) {
            return campaignsById.values().stream()
                    .filter(campaign -> campaign.tenantId().equals(tenantId))
                    .filter(campaign -> campaign.campaignKey().equals(campaignKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public MarketingCampaign findById(Long tenantId, Long campaignId) {
            MarketingCampaign campaign = campaignsById.get(campaignId);
            return campaign != null && campaign.tenantId().equals(tenantId) ? campaign : null;
        }

        @Override
        public MarketingCampaign save(MarketingCampaign campaign) {
            Long id = campaign.id() == null ? nextCampaignId++ : campaign.id();
            MarketingCampaign saved = campaign.withId(id);
            campaignsById.put(id, saved);
            return saved;
        }

        @Override
        public List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit) {
            lastListLimit = limit;
            return campaignsById.values().stream()
                    .filter(campaign -> campaign.tenantId().equals(tenantId))
                    .filter(campaign -> status == null || campaign.status() == status)
                    .sorted(Comparator.comparing(MarketingCampaign::campaignKey))
                    .limit(limit)
                    .toList();
        }

        @Override
        public MarketingCampaignLink findLink(Long tenantId,
                                              Long campaignId,
                                              String resourceType,
                                              CampaignKey resourceKey) {
            return linksById.values().stream()
                    .filter(link -> link.tenantId().equals(tenantId))
                    .filter(link -> link.campaignId().equals(campaignId))
                    .filter(link -> link.resourceType().equals(resourceType))
                    .filter(link -> link.resourceKey().equals(resourceKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public MarketingCampaignLink saveLink(MarketingCampaignLink link) {
            Long id = link.id() == null ? nextLinkId++ : link.id();
            MarketingCampaignLink saved = link.withId(id);
            linksById.put(id, saved);
            return saved;
        }

        @Override
        public List<MarketingCampaignLink> listLinks(Long tenantId, Long campaignId) {
            return linksById.values().stream()
                    .filter(link -> link.tenantId().equals(tenantId))
                    .filter(link -> link.campaignId().equals(campaignId))
                    .sorted(Comparator.comparing(MarketingCampaignLink::resourceType)
                            .thenComparing(link -> link.resourceKey().value()))
                    .toList();
        }

        @Override
        public MarketingCampaignLink findLinkById(Long tenantId, Long linkId) {
            MarketingCampaignLink link = linksById.get(linkId);
            return link != null && link.tenantId().equals(tenantId) ? link : null;
        }

        @Override
        public void deleteLink(Long tenantId, Long linkId) {
            deletedLinkIds.add(linkId);
            linksById.remove(linkId);
        }
    }
}
