package org.chovy.canvas.web;

import org.chovy.canvas.domain.meta.AbExperimentGovernanceService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbExperimentGovernanceControllerTest {

    @Test
    void requestMappingExtendsAbExperimentNamespace() {
        RequestMapping mapping = AbExperimentGovernanceController.class
                .getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/canvas/ab-experiments/{experimentId}/governance");
    }

    @Test
    void evaluateReturnsAuditedGovernanceDecision() {
        AbExperimentGovernanceService service = mock(AbExperimentGovernanceService.class);
        when(service.evaluate(42L, "A")).thenReturn(new AbExperimentGovernanceService.Evaluation(
                42L,
                "WINNER_CANDIDATE",
                "B",
                "conversion_rate",
                new BigDecimal("0.990000"),
                1600L,
                "PENDING_REVIEW",
                List.of("winner candidate B requires manual review before audience or tag writeback"),
                List.of()));
        AbExperimentGovernanceController controller = new AbExperimentGovernanceController(service);

        StepVerifier.create(controller.evaluate(42L, "A"))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().winnerVariantKey()).isEqualTo("B");
                    assertThat(response.getData().writebackStatus()).isEqualTo("PENDING_REVIEW");
                })
                .verifyComplete();

        verify(service).evaluate(42L, "A");
    }
}
