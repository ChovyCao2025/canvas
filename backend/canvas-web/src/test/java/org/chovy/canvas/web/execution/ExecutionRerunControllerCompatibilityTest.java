package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.ExecutionRerunFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ExecutionRerunControllerCompatibilityTest {

    @Test
    void mapsLegacyRerunStatusAndAuditRoutesWithCompatibilityEnvelope() {
        RecordingExecutionRerunFacade facade = new RecordingExecutionRerunFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/execution-reruns/canvas/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "mode": "DRY_RUN",
                          "reason": "reproduce failed coupon path",
                          "userId": "user-1",
                          "originalExecutionId": "exec-1",
                          "inputParams": {"couponCode": "A10"}
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
                .jsonPath("$.data.auditId").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("SUCCESS");

        client.get().uri("/execution-reruns/1").exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.canvasId").isEqualTo(42);

        client.get().uri("/execution-reruns?canvasId=42").exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo(1);

        assertThat(facade.operations).containsExactly("rerun", "audit", "audits");
        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastOperator).isEqualTo("system");
        assertThat(facade.lastAdmin).isFalse();
        assertThat(facade.lastCanvasId).isEqualTo(42L);
        assertThat(facade.lastCommand.userId()).isEqualTo("user-1");
        assertThat(facade.lastAuditId).isEqualTo(1L);
        assertThat(facade.lastAuditCanvasFilter).isEqualTo(42L);
    }

    @Test
    void honorsTenantActorAndAdminHeaders() {
        RecordingExecutionRerunFacade facade = new RecordingExecutionRerunFacade();

        webClient(facade).post()
                .uri("/execution-reruns/canvas/42")
                .header("X-Tenant-Id", "7")
                .header("X-Actor", " operator-1 ")
                .header("X-Admin", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"mode":"ADMIN_REPLAY","reason":"replay production execution","userId":"user-1"}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(7L);
        assertThat(facade.lastOperator).isEqualTo("operator-1");
        assertThat(facade.lastAdmin).isTrue();
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingExecutionRerunFacade facade = new RecordingExecutionRerunFacade();
        facade.failRerun = true;

        webClient(facade).post()
                .uri("/execution-reruns/canvas/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"mode":"UNKNOWN","reason":"reproduce failed coupon path","userId":"user-1"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("unsupported rerun mode: UNKNOWN")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(ExecutionRerunFacade facade) {
        return WebTestClient.bindToController(new ExecutionRerunController(facade)).build();
    }

    private static final class RecordingExecutionRerunFacade implements ExecutionRerunFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastOperator;
        private boolean lastAdmin;
        private Long lastCanvasId;
        private RerunCommand lastCommand;
        private Long lastAuditId;
        private Long lastAuditCanvasFilter;
        private boolean failRerun;

        @Override
        public RerunResult rerun(Long tenantId, String operator, boolean admin, Long canvasId, RerunCommand command) {
            operations.add("rerun");
            lastTenantId = tenantId;
            lastOperator = operator;
            lastAdmin = admin;
            lastCanvasId = canvasId;
            lastCommand = command;
            if (failRerun) {
                throw new IllegalArgumentException("unsupported rerun mode: " + command.mode());
            }
            return new RerunResult(1L, command.mode(), "SUCCESS", Map.of("canvasId", canvasId));
        }

        @Override
        public AuditRow audit(Long tenantId, Long id) {
            operations.add("audit");
            lastTenantId = tenantId;
            lastAuditId = id;
            return auditRow(id, 42L);
        }

        @Override
        public List<AuditRow> audits(Long tenantId, Long canvasId) {
            operations.add("audits");
            lastTenantId = tenantId;
            lastAuditCanvasFilter = canvasId;
            return List.of(auditRow(1L, canvasId));
        }

        private static AuditRow auditRow(Long id, Long canvasId) {
            return new AuditRow(id, 0L, canvasId, "user-1", null, "exec-1", "DRY_RUN",
                    "reproduce failed coupon path", "system", "SUCCESS", Map.of("couponCode", "A10"),
                    "2026-06-14T10:00:00", "2026-06-14T10:00:01");
        }
    }
}
