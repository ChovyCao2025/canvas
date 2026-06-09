package org.chovy.canvas.domain.risk.lab;

import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.runtime.RiskBand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RiskSimulationServiceTest {

    private final FakeSampleRepository sampleRepository = new FakeSampleRepository();
    private final RecordingActivationGuard activationGuard = new RecordingActivationGuard();
    private final RiskSimulationService service = new RiskSimulationService(sampleRepository, activationGuard);

    @Test
    void historicalSampleRunProducesActionDistribution() {
        sampleRepository.samples.add(sample("run-1", "req-1", RiskDecisionAction.ALLOW, 10, 1));
        sampleRepository.samples.add(sample("run-2", "req-2", RiskDecisionAction.REVIEW, 60, 1));
        sampleRepository.samples.add(sample("run-3", "req-3", RiskDecisionAction.REVIEW, 70, 1));

        RiskSimulationResult result = service.run(new RiskSimulationRequest(
                7L, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 1, 100));

        assertThat(result.sampleSize()).isEqualTo(3);
        assertThat(result.actionDistribution()).containsEntry(RiskDecisionAction.ALLOW, 1);
        assertThat(result.actionDistribution()).containsEntry(RiskDecisionAction.REVIEW, 2);
        assertThat(result.status()).isEqualTo(RiskSimulationStatus.COMPLETED);
    }

    @Test
    void baselineCandidateDiffCountsActionChanges() {
        sampleRepository.samples.add(sample("run-1", "req-1", RiskDecisionAction.ALLOW, 10, 1));
        sampleRepository.samples.add(sample("run-2", "req-2", RiskDecisionAction.REVIEW, 70, 1));
        sampleRepository.candidateActions.put("req-1", RiskDecisionAction.BLOCK);
        sampleRepository.candidateActions.put("req-2", RiskDecisionAction.REVIEW);

        RiskSimulationResult result = service.run(new RiskSimulationRequest(
                7L, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 100));

        assertThat(result.changedActionCount()).isEqualTo(1);
        assertThat(result.actionChanges()).containsEntry("ALLOW->BLOCK", 1);
        assertThat(result.actionChanges()).doesNotContainKey("REVIEW->REVIEW");
    }

    @Test
    void missingSampleSourceFailsValidation() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.run(new RiskSimulationRequest(
                        7L, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 100)))
                .withMessageContaining("sample source");
    }

    @Test
    void simulationNeverActivatesStrategy() {
        sampleRepository.samples.add(sample("run-1", "req-1", RiskDecisionAction.ALLOW, 10, 1));

        service.run(new RiskSimulationRequest(
                7L, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 100));

        assertThat(activationGuard.activations).isEmpty();
    }

    private RiskDecisionRunRecord sample(String runId,
                                         String requestId,
                                         RiskDecisionAction action,
                                         int score,
                                         int version) {
        RiskDecisionResponse response = new RiskDecisionResponse(
                requestId,
                runId,
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                version,
                RiskRuntimeMode.ENFORCE,
                action,
                score,
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

    private static final class FakeSampleRepository implements RiskSimulationSampleRepository {
        private final List<RiskDecisionRunRecord> samples = new ArrayList<>();
        private final Map<String, RiskDecisionAction> candidateActions = new java.util.LinkedHashMap<>();

        @Override
        public List<RiskDecisionRunRecord> findSamples(Long tenantId, String sceneKey, int limit) {
            return samples.stream()
                    .filter(sample -> sample.tenantId().equals(tenantId))
                    .filter(sample -> sample.response().sceneKey().equals(sceneKey))
                    .limit(limit)
                    .toList();
        }

        @Override
        public RiskDecisionAction evaluateCandidate(RiskDecisionRunRecord sample, String strategyKey, int candidateVersion) {
            return candidateActions.getOrDefault(sample.requestId(), sample.response().action());
        }
    }

    private static final class RecordingActivationGuard implements RiskSimulationActivationGuard {
        private final List<String> activations = new ArrayList<>();

        @Override
        public void activate(Long tenantId, String strategyKey, int version) {
            activations.add(tenantId + ":" + strategyKey + ":" + version);
        }
    }
}
