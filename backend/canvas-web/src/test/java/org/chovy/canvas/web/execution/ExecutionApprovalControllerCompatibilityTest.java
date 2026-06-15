package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;

class ExecutionApprovalControllerCompatibilityTest {

    @Test
    void approveAndRejectExposeLegacyRoutesAndWrapVoidCompatibilityEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/execution/exec-pending/approve")
                .header("X-Tenant-Id", "7")
                .header("X-Actor", "operator-1")
                .header("X-Role", "OPS")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.post()
                .uri(builder -> builder
                        .path("/canvas/execution/exec-reject/reject")
                        .queryParam("reason", "risk mismatch")
                        .build())
                .header("X-Tenant-Id", "7")
                .header("X-Actor", "operator-2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.approveCalls).containsExactly(new ApproveCall(7L, "exec-pending", "operator-1", "OPS"));
        assertThat(facade.rejectCalls).containsExactly(new RejectCall(7L, "exec-reject", "operator-2", "risk mismatch", null));
    }

    @Test
    void defaultsTenantActorAndMapsIllegalArgumentToApi001() {
        RecordingFacade facade = new RecordingFacade();
        facade.failure = new IllegalArgumentException("executionId is required");

        webClient(facade)
                .post()
                .uri("/canvas/execution/exec-pending/approve")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("executionId is required")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.approveCalls).containsExactly(new ApproveCall(0L, "exec-pending", "system", null));
    }

    @Test
    void mapsForbiddenResponseStatusToCompatibilityEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        facade.failure = new ResponseStatusException(HttpStatus.FORBIDDEN, "AUTH_003: current user is not an approver");

        webClient(facade)
                .post()
                .uri("/canvas/execution/exec-pending/approve")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("AUTH_003: current user is not an approver")
                .jsonPath("$.errorCode").isEqualTo("AUTH_003")
                .jsonPath("$.data").doesNotExist();
    }

    private static WebTestClient webClient(ExecutionApprovalFacade facade) {
        return WebTestClient.bindToController(new ExecutionApprovalController(facade)).build();
    }

    private static ExecutionApprovalFacade.ExecutionApprovalDecision decision(String executionId, String result) {
        return new ExecutionApprovalFacade.ExecutionApprovalDecision(
                executionId,
                result,
                "operator-1",
                null,
                LocalDateTime.parse("2026-06-15T06:20:00"));
    }

    private static final class RecordingFacade implements ExecutionApprovalFacade {
        private final List<ApproveCall> approveCalls = new ArrayList<>();
        private final List<RejectCall> rejectCalls = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public ExecutionApprovalDecision approve(Long tenantId, String executionId, String actor, String role) {
            approveCalls.add(new ApproveCall(tenantId, executionId, actor, role));
            failIfConfigured();
            return decision(executionId, "APPROVED");
        }

        @Override
        public ExecutionApprovalDecision reject(Long tenantId, String executionId, String actor, String reason, String role) {
            rejectCalls.add(new RejectCall(tenantId, executionId, actor, reason, role));
            failIfConfigured();
            return decision(executionId, "REJECTED");
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private record ApproveCall(Long tenantId, String executionId, String actor, String role) {
    }

    private record RejectCall(Long tenantId, String executionId, String actor, String reason, String role) {
    }
}
