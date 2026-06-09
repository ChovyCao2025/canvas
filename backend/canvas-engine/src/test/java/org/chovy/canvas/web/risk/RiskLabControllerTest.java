package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.lab.RiskSimulationActivationGuard;
import org.chovy.canvas.domain.risk.lab.RiskSimulationRequest;
import org.chovy.canvas.domain.risk.lab.RiskSimulationSampleRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationService;
import org.chovy.canvas.domain.risk.runtime.RiskBand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;
import org.chovy.canvas.web.risk.dto.RiskSimulationStartRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskLabControllerTest {

    private final RecordingSampleRepository sampleRepository = new RecordingSampleRepository();
    private final RiskSimulationService service = new RiskSimulationService(sampleRepository, new NoopActivationGuard());
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final RiskLabController controller = new RiskLabController(service, tenantResolver);

    @Test
    void startsSimulationUnderResolvedTenant() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        sampleRepository.samples.add(sample("run-1", "req-1", RiskDecisionAction.ALLOW, 1));

        StepVerifier.create(controller.startSimulation(new RiskSimulationStartRequest(
                        99L, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 100)))
                .assertNext(response -> {
                    assertThat(response.getData().sampleSize()).isEqualTo(1);
                    assertThat(sampleRepository.requests).hasSize(1);
                    assertThat(sampleRepository.requests.getFirst().tenantId()).isEqualTo(7L);
                    assertThat(sampleRepository.requests.getFirst().candidateVersion()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    void listsSimulationHistoryForWorkbenchReads() {
        tenant(RoleNames.OPERATOR, "bob");
        sampleRepository.samples.add(sample("run-1", "req-1", RiskDecisionAction.REVIEW, 1));
        controller.startSimulation(new RiskSimulationStartRequest(
                null, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 50)).block();

        StepVerifier.create(controller.listSimulations("MARKETING_BENEFIT_ISSUE", 20).map(response -> response.getData()))
                .assertNext(simulations -> {
                    assertThat(simulations).hasSize(1);
                    assertThat(simulations.getFirst().sceneKey()).isEqualTo("MARKETING_BENEFIT_ISSUE");
                    assertThat(simulations.getFirst().strategyKey()).isEqualTo("benefit_default");
                    assertThat(simulations.getFirst().sampleSize()).isEqualTo(1);
                    assertThat(simulations.getFirst().actionDistribution()).containsEntry(RiskDecisionAction.REVIEW, 1);
                })
                .verifyComplete();
    }

    @Test
    void acceptsVersionAsCandidateVersionShortcut() {
        tenant(RoleNames.OPERATOR, "bob");
        sampleRepository.samples.add(sample("run-1", "req-1", RiskDecisionAction.REVIEW, 1));

        StepVerifier.create(controller.startSimulation(new RiskSimulationStartRequest(
                        null, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, null, 50)))
                .assertNext(response -> assertThat(sampleRepository.requests.getFirst().candidateVersion()).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void requiresRiskLabPermission() {
        tenant("VIEWER", "eve");

        StepVerifier.create(controller.startSimulation(new RiskSimulationStartRequest(
                        null, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 100)))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    private void tenant(String role, String username) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, username)));
    }

    private RiskDecisionRunRecord sample(String runId,
                                         String requestId,
                                         RiskDecisionAction action,
                                         int version) {
        RiskDecisionResponse response = new RiskDecisionResponse(
                requestId,
                runId,
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                version,
                RiskRuntimeMode.ENFORCE,
                action,
                10,
                RiskBand.LOW,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                20,
                true);
        return new RiskDecisionRunRecord(runId, 7L, requestId, "hash-" + requestId,
                "{requestId=" + requestId + "}", response);
    }

    private static final class RecordingSampleRepository implements RiskSimulationSampleRepository {
        private final List<RiskDecisionRunRecord> samples = new ArrayList<>();
        private final List<RiskSimulationRequest> requests = new ArrayList<>();

        @Override
        public List<RiskDecisionRunRecord> findSamples(Long tenantId, String sceneKey, int limit) {
            requests.add(new RiskSimulationRequest(tenantId, sceneKey, "unknown", 0, 0, limit));
            return samples;
        }

        @Override
        public RiskDecisionAction evaluateCandidate(RiskDecisionRunRecord sample, String strategyKey, int candidateVersion) {
            RiskSimulationRequest previous = requests.removeLast();
            requests.add(new RiskSimulationRequest(previous.tenantId(), previous.sceneKey(), strategyKey,
                    previous.baselineVersion(), candidateVersion, previous.sampleLimit()));
            return sample.response().action();
        }
    }

    private static final class NoopActivationGuard implements RiskSimulationActivationGuard {
        @Override
        public void activate(Long tenantId, String strategyKey, int version) {
        }
    }
}
