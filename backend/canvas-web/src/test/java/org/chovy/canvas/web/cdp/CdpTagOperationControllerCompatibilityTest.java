package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpTagOperationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpTagOperationControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "system";
    private static final String HEADER_ACTOR = "tag-operator";

    @Test
    void mapsLegacyTagOperationRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingCdpTagOperationFacade facade = new RecordingCdpTagOperationFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("", "create", Map.of("userId", "user-1", "tagCode", "vip_level")),
                get("?limit=120", "listRecent"),
                get("/10", "get"),
                post("/10/retry-failed", "retryFailed", Map.of()));

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
    void forwardsHeadersBodiesPathVariablesAndBoundedLimit() {
        RecordingCdpTagOperationFacade facade = new RecordingCdpTagOperationFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/cdp/tag-operations")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "userId": "user-1",
                          "tagCode": "vip_level",
                          "memberIds": ["a", "b"]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastCommand.memberIds()).containsExactly("a", "b");

        client.get()
                .uri("/cdp/tag-operations?limit=0")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastLimit).isEqualTo(1);

        client.post()
                .uri("/cdp/tag-operations/99/retry-failed")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastId).isEqualTo(99L);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCdpTagOperationFacade facade = new RecordingCdpTagOperationFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/cdp/tag-operations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId": " "}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("userId is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpTagOperationFacade facade) {
        return WebTestClient.bindToController(new CdpTagOperationController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/cdp/tag-operations" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
        }
    }

    private static final class RecordingCdpTagOperationFacade implements CdpTagOperationFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private BatchTagCommand lastCommand;
        private Integer lastLimit;
        private Long lastId;
        private boolean failCreate;

        @Override
        public TagOperationView create(Long tenantId, BatchTagCommand command, String actor) {
            operations.add("create");
            lastTenantId = tenantId;
            lastActor = actor;
            lastCommand = command;
            if (failCreate) {
                throw new IllegalArgumentException("userId is required");
            }
            return view(tenantId, 10L, actor);
        }

        @Override
        public List<TagOperationView> listRecent(Long tenantId, int limit) {
            operations.add("listRecent");
            lastTenantId = tenantId;
            lastLimit = limit;
            return List.of(view(tenantId, 10L, DEFAULT_ACTOR));
        }

        @Override
        public TagOperationView get(Long tenantId, Long id) {
            operations.add("get");
            lastTenantId = tenantId;
            lastId = id;
            return view(tenantId, id, DEFAULT_ACTOR);
        }

        @Override
        public TagOperationView retryFailed(Long tenantId, Long id, String actor) {
            operations.add("retryFailed");
            lastTenantId = tenantId;
            lastId = id;
            lastActor = actor;
            return view(tenantId, id, actor);
        }

        private static TagOperationView view(Long tenantId, Long id, String actor) {
            return new TagOperationView(id, tenantId, "user-1", "vip_level", "gold", List.of("a"),
                    Map.of(), "SUCCESS", 1, actor, actor, null, null);
        }
    }
}
