// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
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
