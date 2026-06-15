package org.chovy.canvas.web.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.NotificationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class NotificationControllerCompatibilityTest {

    @Test
    void exposesSixLegacyNotificationRouteShapes() {
        RecordingNotificationFacade facade = new RecordingNotificationFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/canvas/notifications").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.code").isEqualTo(0);
        client.get().uri("/canvas/notifications/unread-count").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.count").isEqualTo(1);
        client.put().uri("/canvas/notifications/ntf-1/read").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.code").isEqualTo(0);
        client.put().uri("/canvas/notifications/read-all").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.code").isEqualTo(0);
        client.put().uri("/canvas/notifications/ntf-1/archive").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.code").isEqualTo(0);
        client.post().uri("/canvas/notifications/ws-ticket").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.ticket").isEqualTo("ntf_ws_test")
                .jsonPath("$.data.expiresInSeconds").isEqualTo(60);

        assertThat(facade.operations).containsExactly("list", "unreadCount", "markRead", "markAllRead", "archive",
                "createWsTicket");
    }

    @Test
    void forwardsDefaultsHeadersAndClampedPaging() {
        RecordingNotificationFacade facade = new RecordingNotificationFacade();

        webClient(facade).get()
                .uri("/canvas/notifications?unreadOnly=true&category=TASK&archived=true&page=0&size=500")
                .header("X-Tenant-Id", "9")
                .header("X-Actor", "user-9")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].notificationId").isEqualTo("ntf-1")
                .jsonPath("$.data[0].payloadJson").isEqualTo("{\"x\":1}");

        assertThat(facade.lastTenantId).isEqualTo(9L);
        assertThat(facade.lastActor).isEqualTo("user-9");
        assertThat(facade.lastUnreadOnly).isTrue();
        assertThat(facade.lastCategory).isEqualTo("TASK");
        assertThat(facade.lastArchived).isTrue();
        assertThat(facade.lastPage).isEqualTo(1);
        assertThat(facade.lastSize).isEqualTo(100);
    }

    @Test
    void defaultsToSystemUserAndTenantSevenForCompatibilitySeeds() {
        RecordingNotificationFacade facade = new RecordingNotificationFacade();

        webClient(facade).get().uri("/canvas/notifications/unread-count").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.count").isEqualTo(1);

        assertThat(facade.lastTenantId).isEqualTo(7L);
        assertThat(facade.lastActor).isEqualTo("system");
    }

    @Test
    void mapsIllegalArgumentAndSecurityFailuresToCompatibilityEnvelopes() {
        RecordingNotificationFacade facade = new RecordingNotificationFacade();
        facade.failBadRequest = true;

        webClient(facade).put().uri("/canvas/notifications/bad/read").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("notificationId is required")
                .jsonPath("$.data").doesNotExist();

        facade.failBadRequest = false;
        facade.failForbidden = true;

        webClient(facade).post().uri("/canvas/notifications/ws-ticket").exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("AUTH_003")
                .jsonPath("$.message").isEqualTo("missing tenant context")
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(NotificationFacade facade) {
        return WebTestClient.bindToController(new NotificationController(facade)).build();
    }

    private static final class RecordingNotificationFacade implements NotificationFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Boolean lastUnreadOnly;
        private String lastCategory;
        private Boolean lastArchived;
        private Integer lastPage;
        private Integer lastSize;
        private boolean failBadRequest;
        private boolean failForbidden;

        @Override
        public List<Map<String, Object>> list(Long tenantId, String actor, boolean unreadOnly, boolean archived,
                                              String category, int page, int size) {
            operations.add("list");
            capture(tenantId, actor);
            lastUnreadOnly = unreadOnly;
            lastCategory = category;
            lastArchived = archived;
            lastPage = page;
            lastSize = size;
            return List.of(ordered(
                    "notificationId", "ntf-1",
                    "type", "TASK_FAILED",
                    "category", "TASK",
                    "severity", "ERROR",
                    "status", "UNREAD",
                    "title", "Import failed",
                    "payloadJson", "{\"x\":1}"));
        }

        @Override
        public Map<String, Object> unreadCount(Long tenantId, String actor) {
            operations.add("unreadCount");
            capture(tenantId, actor);
            return Map.of("count", 1L);
        }

        @Override
        public void markRead(Long tenantId, String actor, String notificationId) {
            operations.add("markRead");
            capture(tenantId, actor);
            if (failBadRequest) {
                throw new IllegalArgumentException("notificationId is required");
            }
        }

        @Override
        public void markAllRead(Long tenantId, String actor) {
            operations.add("markAllRead");
            capture(tenantId, actor);
        }

        @Override
        public void archive(Long tenantId, String actor, String notificationId) {
            operations.add("archive");
            capture(tenantId, actor);
        }

        @Override
        public Map<String, Object> createWsTicket(Long tenantId, String actor) {
            operations.add("createWsTicket");
            capture(tenantId, actor);
            if (failForbidden) {
                throw new SecurityException("missing tenant context");
            }
            return ordered("ticket", "ntf_ws_test", "expiresInSeconds", 60);
        }

        private void capture(Long tenantId, String actor) {
            lastTenantId = tenantId;
            lastActor = actor;
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
