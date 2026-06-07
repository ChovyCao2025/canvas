package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.AiDecisionFeedbackCommand;
import org.chovy.canvas.domain.ai.AiDecisionFeedbackView;
import org.chovy.canvas.domain.ai.AiDecisionModelService;
import org.chovy.canvas.domain.ai.AiDecisionRecommendationQuery;
import org.chovy.canvas.domain.ai.AiDecisionRecommendationView;
import org.chovy.canvas.domain.ai.AiDecisionRecomputeCommand;
import org.chovy.canvas.domain.ai.AiDecisionRunView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiDecisionControllerTest {

    private final AiDecisionModelService service = mock(AiDecisionModelService.class);
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final AiDecisionController controller = new AiDecisionController(service, tenantResolver);

    @Test
    void recomputeRequiresAdminAndPassesTenantActorAndRequest() {
        tenant(RoleNames.TENANT_ADMIN);
        AiDecisionRecomputeCommand command = new AiDecisionRecomputeCommand(
                LocalDate.of(2026, 6, 6),
                "DAILY_MARKETING",
                List.of("u1"),
                true,
                new BigDecimal("100.0000"),
                Map.of("source", "manual"));
        when(service.recompute(7L, command, "alice")).thenReturn(runView());

        StepVerifier.create(controller.recompute(command).map(response -> response.getData()))
                .assertNext(result -> assertThat(result.id()).isEqualTo(501L))
                .verifyComplete();

        verify(service).recompute(7L, command, "alice");
    }

    @Test
    void rejectsNonAdminRoles() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.recompute(null))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("admin"))
                .verify();
    }

    @Test
    void latestRunPassesTenantAndDecisionScope() {
        tenant(RoleNames.SUPER_ADMIN);
        when(service.latestRun(7L, "DAILY_MARKETING")).thenReturn(Optional.of(runView()));

        StepVerifier.create(controller.latestRun("DAILY_MARKETING").map(response -> response.getData()))
                .assertNext(result -> assertThat(result.decisionScope()).isEqualTo("DAILY_MARKETING"))
                .verifyComplete();

        verify(service).latestRun(7L, "DAILY_MARKETING");
    }

    @Test
    void recommendationsPassFiltersAndBoundedLimit() {
        tenant(RoleNames.TENANT_ADMIN);
        AiDecisionRecommendationQuery query =
                new AiDecisionRecommendationQuery(501L, "NEXT_BEST_ACTION", "ELIGIBLE", 100);
        when(service.recommendations(7L, query)).thenReturn(List.of(recommendationView()));

        StepVerifier.create(controller.recommendations(501L, "NEXT_BEST_ACTION", "ELIGIBLE", 500)
                        .map(response -> response.getData()))
                .assertNext(result -> assertThat(result).singleElement()
                        .satisfies(row -> assertThat(row.actionKey()).isEqualTo("RETENTION_INTERVENTION")))
                .verifyComplete();

        verify(service).recommendations(7L, query);
    }

    @Test
    void feedbackPassesTenantRecommendationActorAndCommand() {
        tenant(RoleNames.TENANT_ADMIN);
        AiDecisionFeedbackCommand command =
                new AiDecisionFeedbackCommand("ACCEPTED", new BigDecimal("99.9000"), Map.of("canvasId", 123));
        when(service.recordFeedback(7L, 100L, command, "alice")).thenReturn(feedbackView());

        StepVerifier.create(controller.recordFeedback(100L, command).map(response -> response.getData()))
                .assertNext(result -> assertThat(result.feedbackType()).isEqualTo("ACCEPTED"))
                .verifyComplete();

        verify(service).recordFeedback(7L, 100L, command, "alice");
    }

    private void tenant(String role) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, "alice")));
    }

    private AiDecisionRunView runView() {
        return new AiDecisionRunView(
                501L,
                7L,
                AiDecisionModelService.MODEL_KEY,
                "decision_baseline_v1",
                "DAILY_MARKETING",
                LocalDate.of(2026, 6, 6),
                "SUCCESS",
                1,
                1,
                0,
                0,
                Map.of("source", "manual"),
                "alice",
                LocalDateTime.of(2026, 6, 6, 0, 0),
                LocalDateTime.of(2026, 6, 6, 0, 0),
                null);
    }

    private AiDecisionRecommendationView recommendationView() {
        return new AiDecisionRecommendationView(
                100L,
                7L,
                501L,
                "u1",
                AiDecisionModelService.MODEL_KEY,
                "decision_baseline_v1",
                "DAILY_MARKETING",
                "NEXT_BEST_ACTION",
                "RETENTION_INTERVENTION",
                "RETENTION_INTERVENTION",
                null,
                null,
                new BigDecimal("0.80000"),
                new BigDecimal("0.70000"),
                1,
                BigDecimal.ZERO,
                "ELIGIBLE",
                null,
                Map.of(),
                Map.of(),
                LocalDateTime.of(2026, 6, 6, 0, 0));
    }

    private AiDecisionFeedbackView feedbackView() {
        return new AiDecisionFeedbackView(
                77L,
                7L,
                100L,
                "ACCEPTED",
                new BigDecimal("99.9000"),
                Map.of("canvasId", 123),
                "alice",
                LocalDateTime.of(2026, 6, 6, 0, 0));
    }
}
