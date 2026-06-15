package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWriteKeyFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWriteKeyControllerCompatibilityTest {

    @Test
    void mapsLegacyWriteKeyRoutesWithCompatibilityEnvelope() {
        RecordingCdpWriteKeyFacade facade = new RecordingCdpWriteKeyFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/cdp/write-keys")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].id").isEqualTo(1)
                .jsonPath("$.data[0].keyPrefix").isEqualTo("ck_live_seed");

        client.post()
                .uri("/cdp/write-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Web SDK",
                          "platform": "WEB",
                          "rateLimitQps": 120,
                          "dailyQuota": 10000,
                          "description": "browser ingestion"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.rawKey").isEqualTo("ck_live_raw")
                .jsonPath("$.data.keyPrefix").isEqualTo("ck_live_seed");

        client.delete().uri("/cdp/write-keys/1").exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.operations).containsExactly("list", "create", "disable");
        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastActor).isEqualTo("system");
        assertThat(facade.lastCommand.name()).isEqualTo("Web SDK");
        assertThat(facade.lastDisabledId).isEqualTo(1L);
    }

    @Test
    void honorsTenantAndActorHeaders() {
        RecordingCdpWriteKeyFacade facade = new RecordingCdpWriteKeyFacade();

        webClient(facade).post()
                .uri("/cdp/write-keys")
                .header("X-Tenant-Id", "42")
                .header("X-Actor", " operator-1 ")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Server","platform":"SERVER"}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastActor).isEqualTo("operator-1");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCdpWriteKeyFacade facade = new RecordingCdpWriteKeyFacade();
        facade.failCreate = true;

        webClient(facade).post()
                .uri("/cdp/write-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":" "}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("name cannot be blank")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWriteKeyFacade facade) {
        return WebTestClient.bindToController(new CdpWriteKeyController(facade)).build();
    }

    private static final class RecordingCdpWriteKeyFacade implements CdpWriteKeyFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private CreateCommand lastCommand;
        private Long lastDisabledId;
        private boolean failCreate;

        @Override
        public List<KeyRow> list(Long tenantId) {
            operations.add("list");
            lastTenantId = tenantId;
            return List.of(new KeyRow(1L, "Seed", "ck_live_seed", "WEB", "ACTIVE", 100, 100000L,
                    "seed key", "system", LocalDateTime.parse("2026-06-14T10:00:00"),
                    LocalDateTime.parse("2026-06-14T10:00:00")));
        }

        @Override
        public CreateResult create(Long tenantId, CreateCommand command, String actor) {
            operations.add("create");
            lastTenantId = tenantId;
            lastActor = actor;
            lastCommand = command;
            if (failCreate) {
                throw new IllegalArgumentException("name cannot be blank");
            }
            return new CreateResult(1L, command.name(), "ck_live_raw", "ck_live_seed",
                    command.platform(), command.rateLimitQps(), command.dailyQuota());
        }

        @Override
        public void disable(Long tenantId, Long id) {
            operations.add("disable");
            lastTenantId = tenantId;
            lastDisabledId = id;
        }
    }
}
