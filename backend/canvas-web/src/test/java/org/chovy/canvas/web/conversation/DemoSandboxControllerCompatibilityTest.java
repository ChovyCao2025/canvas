package org.chovy.canvas.web.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.conversation.api.DemoSandboxFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class DemoSandboxControllerCompatibilityTest {

    private static final String DEFAULT_ACTOR = "system";
    private static final String HEADER_ACTOR = "sandbox-operator";

    @Test
    void mapsLegacyDemoSandboxRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingDemoSandboxFacade facade = new RecordingDemoSandboxFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("", "install", Map.of("tenantId", 7, "demoName", "Welcome", "ttlDays", 3)),
                post("/7/reset", "reset", Map.of()),
                get("/expired", "expired"),
                post("/7/conversation-replies", "reply", Map.of("executionId", "exec-1", "userId", "user-1")));

        for (RouteProbe probe : probes) {
            probe.exchange(client)
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.errorCode").doesNotExist()
                    .jsonPath("$.traceId").doesNotExist();
        }

        assertThat(facade.operations).containsExactlyElementsOf(probes.stream()
                .map(RouteProbe::operation)
                .toList());
    }

    @Test
    void forwardsHeadersBodiesAndPathVariables() {
        RecordingDemoSandboxFacade facade = new RecordingDemoSandboxFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/demo-sandboxes")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"tenantId": 42, "demoName": "Welcome", "ttlDays": 5}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.installedBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastInstallCommand.tenantId()).isEqualTo(42L);

        client.post()
                .uri("/demo-sandboxes/99/reset")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(99L);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        client.post()
                .uri("/demo-sandboxes/99/conversation-replies")
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "canvasId": 1,
                          "versionId": 2,
                          "executionId": "exec-1",
                          "userId": "user-1",
                          "text": "hello",
                          "attributes": {"locale": "en-US"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.channel").isEqualTo("SANDBOX")
                .jsonPath("$.data.attributes.locale").isEqualTo("en-US");

        assertThat(facade.lastTenantId).isEqualTo(99L);
        assertThat(facade.lastReplyCommand.executionId()).isEqualTo("exec-1");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingDemoSandboxFacade facade = new RecordingDemoSandboxFacade();
        facade.failInstall = true;

        webClient(facade)
                .post()
                .uri("/demo-sandboxes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("tenantId is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(DemoSandboxFacade facade) {
        return WebTestClient.bindToController(new DemoSandboxController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/demo-sandboxes" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
        }
    }

    private static final class RecordingDemoSandboxFacade implements DemoSandboxFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private InstallCommand lastInstallCommand;
        private ConversationReplyCommand lastReplyCommand;
        private boolean failInstall;

        @Override
        public SandboxView install(InstallCommand command, String actor) {
            operations.add("install");
            lastActor = actor;
            lastInstallCommand = command;
            if (failInstall) {
                throw new IllegalArgumentException("tenantId is required");
            }
            return new SandboxView(1L, command.tenantId(), command.demoName(), command.ttlDays(), "ACTIVE",
                    actor, null, null);
        }

        @Override
        public ResetResult reset(Long tenantId, String actor) {
            operations.add("reset");
            lastTenantId = tenantId;
            lastActor = actor;
            return new ResetResult(tenantId, "RESET", actor, null);
        }

        @Override
        public List<SandboxView> expired() {
            operations.add("expired");
            return List.of(new SandboxView(1L, 7L, "Expired", 0, "EXPIRED", DEFAULT_ACTOR, null, null));
        }

        @Override
        public ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor) {
            operations.add("reply");
            lastTenantId = tenantId;
            lastActor = actor;
            lastReplyCommand = command;
            return new ConversationReplyResult(1L, tenantId, "SANDBOX", command.canvasId(), command.versionId(),
                    command.executionId(), command.userId(), command.externalMessageId(), command.eventId(),
                    command.text(), command.intent(), command.attributes(), actor, null);
        }
    }
}
