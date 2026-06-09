package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.governance.RiskSceneService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskSceneControllerTest {

    private final RiskSceneService service = new RiskSceneService();
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final RiskSceneController controller = new RiskSceneController(service, tenantResolver);

    @Test
    void listsTenantScopedDefaultRiskScenes() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.listScenes().map(response -> response.getData()))
                .assertNext(scenes -> {
                    assertThat(scenes).hasSizeGreaterThanOrEqualTo(5);
                    assertThat(scenes)
                            .extracting("tenantId")
                            .containsOnly(7L);
                    assertThat(scenes)
                            .extracting("sceneKey")
                            .contains(
                                    "MARKETING_BENEFIT_ISSUE",
                                    "MESSAGE_SEND_PRECHECK",
                                    "ACCOUNT_DEVICE_ABUSE",
                                    "TRANSACTION_PAYMENT_PRECHECK",
                                    "AI_DECISION_GUARDRAIL");
                    assertThat(scenes)
                            .allSatisfy(scene -> {
                                assertThat(scene.displayName()).isNotBlank();
                                assertThat(scene.latencyBudgetMs()).isBetween(10, 100);
                                assertThat(scene.status()).isEqualTo("ACTIVE");
                            });
                })
                .verifyComplete();
    }

    @Test
    void requiresRiskSceneReadPermission() {
        tenant("VIEWER");

        StepVerifier.create(controller.listScenes())
                .expectError(AccessDeniedException.class)
                .verify();
    }

    private void tenant(String role) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, "alice")));
    }
}
