package org.chovy.canvas.web.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.chovy.canvas.platform.api.PlatformActor;
import org.chovy.canvas.platform.api.TechnicalMigrationCandidateFacade;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceRequest;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class TechnicalMigrationCandidateControllerCompatibilityTest {

    @Test
    void registersEvidenceThroughLegacyArchitectureRoute() {
        RecordingTechnicalMigrationCandidateFacade facade = new RecordingTechnicalMigrationCandidateFacade();

        webClient(facade)
                .post()
                .uri("/architecture/migration-candidates/evidence")
                .header("X-Tenant-Id", "8")
                .header("X-Actor", "operator-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "candidateKey": "RocketMQ-Topic-Split",
                          "proofCommand": "mvn test",
                          "baselineResultJson": "{\\"baseline\\":\\"BUILD SUCCESS\\"}",
                          "rollbackCommand": "restore topic config"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(8)
                .jsonPath("$.data.candidateKey").isEqualTo("rocketmq-topic-split")
                .jsonPath("$.data.proofCommand").isEqualTo("mvn test")
                .jsonPath("$.data.baselineResultJson").isEqualTo("{\"baseline\":\"BUILD SUCCESS\"}")
                .jsonPath("$.data.rollbackCommand").isEqualTo("restore topic config")
                .jsonPath("$.data.decisionStatus").isEqualTo("BLOCKED_PENDING_REVIEW")
                .jsonPath("$.data.submittedBy").isEqualTo("operator-1");

        assertThat(facade.actor).isEqualTo(new PlatformActor(8L, "operator-1"));
        assertThat(facade.request.candidateKey()).isEqualTo("RocketMQ-Topic-Split");
    }

    @Test
    void mapsValidationFailureToApi001Envelope() {
        RecordingTechnicalMigrationCandidateFacade facade = new RecordingTechnicalMigrationCandidateFacade();
        facade.failValidation = true;

        webClient(facade)
                .post()
                .uri("/architecture/migration-candidates/evidence")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("candidate key is required")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(TechnicalMigrationCandidateFacade facade) {
        return WebTestClient.bindToController(new TechnicalMigrationCandidateController(facade)).build();
    }

    private static final class RecordingTechnicalMigrationCandidateFacade
            implements TechnicalMigrationCandidateFacade {
        private PlatformActor actor;
        private TechnicalMigrationEvidenceRequest request;
        private boolean failValidation;

        @Override
        public TechnicalMigrationEvidenceView register(PlatformActor actor,
                                                       TechnicalMigrationEvidenceRequest request) {
            this.actor = actor;
            this.request = request;
            if (failValidation) {
                throw new IllegalArgumentException("candidate key is required");
            }
            return new TechnicalMigrationEvidenceView(
                    actor.tenantId(),
                    request.candidateKey().trim().toLowerCase(),
                    request.proofCommand().trim(),
                    request.baselineResultJson().trim(),
                    request.rollbackCommand().trim(),
                    "BLOCKED_PENDING_REVIEW",
                    actor.username());
        }

        @Override
        public boolean canStartMigration(PlatformActor actor, String candidateKey) {
            return false;
        }
    }
}
