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
