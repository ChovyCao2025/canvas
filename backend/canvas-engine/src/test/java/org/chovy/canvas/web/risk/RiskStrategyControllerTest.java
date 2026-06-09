package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.governance.RiskStrategyCommand;
import org.chovy.canvas.domain.risk.governance.RiskStrategyDiffView;
import org.chovy.canvas.domain.risk.governance.RiskStrategyLifecycleStatus;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService;
import org.chovy.canvas.domain.risk.governance.RiskStrategyTransitionRequest;
import org.chovy.canvas.domain.risk.governance.RiskStrategyView;
import org.chovy.canvas.domain.risk.governance.RiskStrategyVersionView;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskStrategyControllerTest {

    private final RecordingAuditSink auditSink = new RecordingAuditSink();
    private final RecordingRuntimeCache runtimeCache = new RecordingRuntimeCache();
    private final RiskStrategyService service = new RiskStrategyService(auditSink, runtimeCache);
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final RiskStrategyController controller = new RiskStrategyController(service, tenantResolver);

    @Test
    void createsDraftVersion() {
        tenant(RoleNames.TENANT_ADMIN, "alice");

        StepVerifier.create(controller.createDraft(command("HIGH")))
                .assertNext(response -> {
                    RiskStrategyView view = response.getData();
                    assertThat(view.strategyKey()).isEqualTo("benefit_default");
                    assertThat(view.draftVersion()).isEqualTo(1);
                    assertThat(view.status()).isEqualTo(RiskStrategyLifecycleStatus.DRAFT);
                })
                .verifyComplete();
    }

    @Test
    void listsStrategiesAndVersionsForWorkbenchReads() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("LOW")).block();

        StepVerifier.create(controller.listStrategies("MARKETING_BENEFIT_ISSUE")
                        .map(response -> response.getData()))
                .assertNext(strategies -> {
                    assertThat(strategies).hasSize(1);
                    assertThat(strategies.getFirst().strategyKey()).isEqualTo("benefit_default");
                    assertThat(strategies.getFirst().sceneKey()).isEqualTo("MARKETING_BENEFIT_ISSUE");
                })
                .verifyComplete();

        StepVerifier.create(controller.listVersions("benefit_default").map(response -> response.getData()))
                .assertNext(versions -> {
                    assertThat(versions).hasSize(1);
                    assertThat(versions.getFirst().status()).isEqualTo(RiskStrategyLifecycleStatus.DRAFT);
                    assertThat(versions.getFirst().definitionJson()).contains("ALLOW");
                })
                .verifyComplete();
    }

    @Test
    void validatesDraftAndStoresValidationResult() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("HIGH")).block();

        StepVerifier.create(controller.validateVersion("benefit_default", 1))
                .assertNext(response -> {
                    RiskStrategyVersionView view = response.getData();
                    assertThat(view.status()).isEqualTo(RiskStrategyLifecycleStatus.VALIDATED);
                    assertThat(view.validationJson()).contains("\"valid\":true");
                })
                .verifyComplete();
    }

    @Test
    void rejectsSubmitWhenValidationFails() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("HIGH").withDefinitionJson("{\"rules\":\"INVALID\"}")).block();
        controller.validateVersion("benefit_default", 1).block();

        StepVerifier.create(controller.submitVersion("benefit_default", 1, transition()))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("validation"))
                .verify();
    }

    @Test
    void submitsHighRiskStrategyForApproval() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        prepareValidatedAndSimulated("HIGH");

        StepVerifier.create(controller.submitVersion("benefit_default", 1, transition()))
                .assertNext(response -> assertThat(response.getData().status())
                        .isEqualTo(RiskStrategyLifecycleStatus.APPROVAL_PENDING))
                .verifyComplete();
    }

    @Test
    void preventsSubmitterSelfApproval() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        prepareValidatedAndSimulated("HIGH");
        controller.submitVersion("benefit_default", 1, transition()).block();

        StepVerifier.create(controller.approveVersion("benefit_default", 1, transition()))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("submitter"))
                .verify();
    }

    @Test
    void activatesApprovedVersionAndInvalidatesRuntimeCache() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        prepareValidatedAndSimulated("HIGH");
        controller.submitVersion("benefit_default", 1, transition()).block();
        tenant(RoleNames.TENANT_ADMIN, "bob");
        controller.approveVersion("benefit_default", 1, transition()).block();

        StepVerifier.create(controller.activateVersion("benefit_default", 1, transition()))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo(RiskStrategyLifecycleStatus.ACTIVE);
                    assertThat(response.getData().activeVersion()).isEqualTo(1);
                })
                .verifyComplete();

        assertThat(runtimeCache.invalidations).containsExactly("7:benefit_default");
    }

    @Test
    void rejectsActivationWithoutSimulationForHighRiskStrategy() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("HIGH")).block();
        controller.validateVersion("benefit_default", 1).block();

        StepVerifier.create(controller.activateVersion("benefit_default", 1, transition()))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("simulation"))
                .verify();
    }

    @Test
    void rollsBackToPreviousImmutableVersion() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("LOW").withDefinitionJson("{\"rules\":[{\"action\":\"ALLOW\"}]}")).block();
        activateLowRiskVersion(1);
        controller.createDraft(command("LOW").withDefinitionJson("{\"rules\":[{\"action\":\"REVIEW\"}]}")).block();
        activateLowRiskVersion(2);

        StepVerifier.create(controller.rollback("benefit_default", new RiskStrategyTransitionRequest("rollback", 1)))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo(RiskStrategyLifecycleStatus.ROLLED_BACK);
                    assertThat(response.getData().activeVersion()).isEqualTo(1);
                })
                .verifyComplete();

        assertThat(runtimeCache.invalidations).contains("7:benefit_default");
    }

    @Test
    void pausesActiveStrategyAndInvalidatesRuntimeCache() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("LOW")).block();
        activateLowRiskVersion(1);
        runtimeCache.invalidations.clear();

        StepVerifier.create(controller.pause("benefit_default", transition()))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo(RiskStrategyLifecycleStatus.PAUSED);
                    assertThat(response.getData().activeVersion()).isEqualTo(1);
                })
                .verifyComplete();

        assertThat(runtimeCache.invalidations).containsExactly("7:benefit_default");
        assertThat(auditSink.events).contains("PAUSED:benefit_default:1:alice");
    }

    @Test
    void returnsVersionDiffWithRuleAndActionChanges() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createDraft(command("LOW").withDefinitionJson("{\"rules\":[{\"action\":\"ALLOW\"}]}")).block();
        controller.createDraft(command("LOW").withDefinitionJson("{\"rules\":[{\"action\":\"BLOCK\"}]}")).block();

        StepVerifier.create(controller.diffVersions("benefit_default", 1, 2))
                .assertNext(response -> {
                    RiskStrategyDiffView diff = response.getData();
                    assertThat(diff.leftVersion()).isEqualTo(1);
                    assertThat(diff.rightVersion()).isEqualTo(2);
                    assertThat(diff.changes()).contains("action changed");
                })
                .verifyComplete();
    }

    @Test
    void recordsAuditEventForEveryStateTransition() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        prepareValidatedAndSimulated("HIGH");
        controller.submitVersion("benefit_default", 1, transition()).block();

        assertThat(auditSink.events).containsExactly(
                "DRAFT_CREATED:benefit_default:1:alice",
                "VALIDATED:benefit_default:1:alice",
                "SIMULATED:benefit_default:1:alice",
                "SUBMITTED:benefit_default:1:alice");
    }

    @Test
    void requiresExpectedPermissionForEachCommand() {
        tenant("VIEWER", "eve");

        StepVerifier.create(controller.createDraft(command("LOW")))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    private void prepareValidatedAndSimulated(String riskLevel) {
        controller.createDraft(command(riskLevel)).block();
        controller.validateVersion("benefit_default", 1).block();
        controller.markSimulated("benefit_default", 1, transition()).block();
    }

    private void activateLowRiskVersion(int version) {
        controller.validateVersion("benefit_default", version).block();
        controller.activateVersion("benefit_default", version, transition()).block();
    }

    private RiskStrategyCommand command(String riskLevel) {
        return new RiskStrategyCommand(
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                "Benefit default",
                riskLevel,
                "{\"rules\":[{\"action\":\"ALLOW\"}]}");
    }

    private RiskStrategyTransitionRequest transition() {
        return new RiskStrategyTransitionRequest("ship it", null);
    }

    private void tenant(String role, String username) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, username)));
    }

    private static final class RecordingAuditSink implements RiskStrategyAuditSink {
        private final List<String> events = new ArrayList<>();

        @Override
        public void record(Long tenantId, String eventType, String strategyKey, int version, String actor) {
            events.add(eventType + ":" + strategyKey + ":" + version + ":" + actor);
        }
    }

    private static final class RecordingRuntimeCache implements RiskStrategyRuntimeCache {
        private final List<String> invalidations = new ArrayList<>();

        @Override
        public void invalidate(Long tenantId, String strategyKey) {
            invalidations.add(tenantId + ":" + strategyKey);
        }
    }
}
