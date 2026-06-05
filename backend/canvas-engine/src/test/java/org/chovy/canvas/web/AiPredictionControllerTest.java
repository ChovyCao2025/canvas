package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.AiPredictionRunDO;
import org.chovy.canvas.domain.ai.ChurnPredictionService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPredictionControllerTest {

    private final ChurnPredictionService predictionService = mock(ChurnPredictionService.class);
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final AiPredictionController controller = new AiPredictionController(predictionService, tenantResolver);

    @Test
    void latestRunUsesCurrentTenantForTenantAdmin() {
        tenant(RoleNames.TENANT_ADMIN);
        ChurnPredictionService.PredictionRunView run = new ChurnPredictionService.PredictionRunView(
                1L, 7L, ChurnPredictionService.MODEL_KEY, "baseline_v1",
                LocalDate.of(2026, 6, 4), AiPredictionRunDO.STATUS_SUCCESS,
                3, 0, 0, null, null, null);
        when(predictionService.latestRun(7L)).thenReturn(Optional.of(run));

        StepVerifier.create(controller.latestRun().map(response -> response.getData()))
                .assertNext(result -> assertThat(result.id()).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void recomputePassesTenantAndRequest() {
        tenant(RoleNames.TENANT_ADMIN);
        ChurnPredictionService.RecomputeRequest request =
                new ChurnPredictionService.RecomputeRequest(true, LocalDate.of(2026, 6, 4), 10);
        ChurnPredictionService.PredictionRunView run = new ChurnPredictionService.PredictionRunView(
                2L, 7L, ChurnPredictionService.MODEL_KEY, "baseline_v1",
                LocalDate.of(2026, 6, 4), AiPredictionRunDO.STATUS_SUCCESS,
                1, 0, 0, null, null, null);
        when(predictionService.recompute(eq(7L), any())).thenReturn(run);

        StepVerifier.create(controller.recompute(request).map(response -> response.getData()))
                .assertNext(result -> assertThat(result.processedCount()).isEqualTo(1))
                .verifyComplete();

        verify(predictionService).recompute(7L, request);
    }

    @Test
    void distributionAndTopRiskUsersUseCurrentTenant() {
        tenant(RoleNames.SUPER_ADMIN);
        when(predictionService.churnDistribution(7L)).thenReturn(List.of(
                new ChurnPredictionService.RiskDistributionItem("HIGH", 2)));
        when(predictionService.topRiskUsers(7L, 5)).thenReturn(List.of(
                new ChurnPredictionService.TopRiskUser("u1", new BigDecimal("0.90000"), "HIGH", 20,
                        new BigDecimal("0.80000"))));

        StepVerifier.create(controller.distribution().map(response -> response.getData()))
                .assertNext(result -> assertThat(result).extracting(ChurnPredictionService.RiskDistributionItem::count)
                        .containsExactly(2L))
                .verifyComplete();
        StepVerifier.create(controller.topRiskUsers(5).map(response -> response.getData()))
                .assertNext(result -> assertThat(result).extracting(ChurnPredictionService.TopRiskUser::userId)
                        .containsExactly("u1"))
                .verifyComplete();
    }

    @Test
    void rejectsNonAdminRoles() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.latestRun())
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("admin"))
                .verify();
    }

    private void tenant(String role) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, "alice")));
    }
}
