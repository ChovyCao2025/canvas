package org.chovy.canvas.web.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.TestUserFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class TestUserControllerCompatibilityTest {

    @Test
    void exposesSixLegacyTestUserRouteShapes() {
        RecordingTestUserFacade facade = new RecordingTestUserFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/test-users/sets").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data[0].name").isEqualTo("Default");
        client.post().uri("/test-users/sets").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "New", "description", "desc")).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data.createdBy").isEqualTo("system");
        client.get().uri("/test-users/sets/100/users").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data[0].profileJson").isEqualTo("{\"tier\":\"VIP\"}");
        client.post().uri("/test-users/sets/100/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", "u-2", "displayName", "U2")).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data.inputParams").isEqualTo("{}");
        client.get().uri("/test-users/1001").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data.userId").isEqualTo("u-1");
        client.get().uri("/test-users/1001/preview").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data.profile.tier").isEqualTo("VIP");

        assertThat(facade.operations).containsExactly("listSets", "createSet", "listUsers", "createUser",
                "getUser", "preview");
    }

    @Test
    void defaultsTenantAndActorLikeLegacyController() {
        RecordingTestUserFacade facade = new RecordingTestUserFacade();

        webClient(facade).get().uri("/test-users/sets").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastActor).isEqualTo("system");
    }

    @Test
    void forwardsHeadersPathVariablesAndRequestBodies() {
        RecordingTestUserFacade facade = new RecordingTestUserFacade();

        webClient(facade).post().uri("/test-users/sets/300/users")
                .header("X-Tenant-Id", "9")
                .header("X-Actor", "operator-9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", "u-9", "displayName", "User Nine",
                        "profile", Map.of("tier", "SILVER"),
                        "inputParams", Map.of("coupon", "NINE")))
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(9L);
        assertThat(facade.lastSetId).isEqualTo(300L);
        assertThat(facade.lastPayload)
                .containsEntry("userId", "u-9")
                .containsEntry("displayName", "User Nine");
    }

    @Test
    void mapsNotFoundToApi001Envelope() {
        RecordingTestUserFacade facade = new RecordingTestUserFacade();
        facade.failNotFound = true;

        webClient(facade).get().uri("/test-users/404").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("test user not found")
                .jsonPath("$.data").doesNotExist();
    }

    private static WebTestClient webClient(TestUserFacade facade) {
        return WebTestClient.bindToController(new TestUserController(facade)).build();
    }

    private static final class RecordingTestUserFacade implements TestUserFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor = "system";
        private Long lastSetId;
        private Map<String, Object> lastPayload;
        private boolean failNotFound;

        @Override
        public List<Map<String, Object>> listSets(Long tenantId) {
            operations.add("listSets");
            capture(tenantId, null);
            return List.of(ordered("id", 100L, "tenantId", tenantId, "name", "Default", "description", "desc",
                    "createdBy", "system"));
        }

        @Override
        public Map<String, Object> createSet(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("createSet");
            capture(tenantId, actor);
            lastPayload = payload;
            return ordered("id", 101L, "tenantId", tenantId, "name", payload.get("name"), "description",
                    payload.get("description"), "createdBy", actor);
        }

        @Override
        public List<Map<String, Object>> listUsers(Long tenantId, Long setId) {
            operations.add("listUsers");
            capture(tenantId, null);
            lastSetId = setId;
            return List.of(user(tenantId, setId));
        }

        @Override
        public Map<String, Object> createUser(Long tenantId, Long setId, Map<String, Object> payload) {
            operations.add("createUser");
            capture(tenantId, null);
            lastSetId = setId;
            lastPayload = payload;
            return ordered("id", 1002L, "tenantId", tenantId, "setId", setId, "userId", payload.get("userId"),
                    "displayName", payload.get("displayName"), "profileJson", "{}", "inputParams", "{}");
        }

        @Override
        public Map<String, Object> getUser(Long tenantId, Long id) {
            operations.add("getUser");
            capture(tenantId, null);
            if (failNotFound) {
                throw new IllegalArgumentException("test user not found");
            }
            return user(tenantId, 100L);
        }

        @Override
        public Map<String, Object> preview(Long tenantId, Long id) {
            operations.add("preview");
            capture(tenantId, null);
            return ordered("id", id, "userId", "u-1", "displayName", "User One", "profile",
                    Map.of("tier", "VIP"), "inputParams", Map.of("coupon", "WELCOME"), "context",
                    Map.of("tenantId", tenantId));
        }

        private void capture(Long tenantId, String actor) {
            lastTenantId = tenantId;
            if (actor != null) {
                lastActor = actor;
            }
        }

        private static Map<String, Object> user(Long tenantId, Long setId) {
            return ordered("id", 1001L, "tenantId", tenantId, "setId", setId, "userId", "u-1",
                    "displayName", "User One", "profileJson", "{\"tier\":\"VIP\"}", "inputParams",
                    "{\"coupon\":\"WELCOME\"}");
        }

        private static Map<String, Object> ordered(Object... pairs) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < pairs.length; i += 2) {
                result.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
            return result;
        }
    }
}
