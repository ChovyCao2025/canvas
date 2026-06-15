package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.RealtimeAudienceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RealtimeAudienceControllerCompatibilityTest {

    @Test
    void mapsLegacyRealtimeAudienceRoutesWithCompatibilityEnvelope() {
        RecordingRealtimeAudienceFacade facade = new RecordingRealtimeAudienceFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/cdp/realtime-audiences/100/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "sourceEventId": "evt-1",
                          "userId": "user-1",
                          "eventTime": "2026-06-14T10:00:00Z",
                          "properties": {"tier": "gold"},
                          "removeOnNoMatch": false
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
                .jsonPath("$.data.audienceId").isEqualTo(100)
                .jsonPath("$.data.matched").isEqualTo(true);

        client.post().uri("/cdp/realtime-audiences/100/snapshot").exchange().expectStatus().isOk();
        client.get().uri("/cdp/realtime-audiences/100/snapshots?limit=600").exchange().expectStatus().isOk();
        client.get().uri("/cdp/audiences/100/overlap/200").exchange().expectStatus().isOk();
        client.post().uri("/cdp/audiences/merge?leftId=100&rightId=200").exchange().expectStatus().isOk();
        client.post().uri("/cdp/audiences/exclude?baseId=100&excludedId=200").exchange().expectStatus().isOk();

        assertThat(facade.operations).containsExactly("processEvent", "createSnapshot", "listSnapshots",
                "overlap", "merge", "exclude");
        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastAudienceId).isEqualTo(100L);
        assertThat(facade.lastEvent.userId()).isEqualTo("user-1");
        assertThat(facade.lastRemoveOnNoMatch).isFalse();
        assertThat(facade.lastLimit).isEqualTo(500);
        assertThat(facade.lastLeftId).isEqualTo(100L);
        assertThat(facade.lastRightId).isEqualTo(200L);
    }

    @Test
    void defaultsRemoveOnNoMatchLimitTenantAndActor() {
        RecordingRealtimeAudienceFacade facade = new RecordingRealtimeAudienceFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/cdp/realtime-audiences/100/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"sourceEventId":"evt-1","userId":"user-1","properties":{"tier":"gold"}}
                        """)
                .exchange()
                .expectStatus().isOk();
        client.post().uri("/cdp/realtime-audiences/100/snapshot").exchange().expectStatus().isOk();
        client.get().uri("/cdp/realtime-audiences/100/snapshots?limit=0").exchange().expectStatus().isOk();

        assertThat(facade.lastRemoveOnNoMatch).isTrue();
        assertThat(facade.lastLimit).isEqualTo(100);
        assertThat(facade.lastSnapshotReason).isEqualTo("MANUAL");
        assertThat(facade.lastActor).isEqualTo("system");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingRealtimeAudienceFacade facade = new RecordingRealtimeAudienceFacade();
        facade.failEvent = true;

        webClient(facade).post()
                .uri("/cdp/realtime-audiences/999/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"sourceEventId":"evt-1","userId":"user-1","properties":{"tier":"gold"}}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("audience is not found")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(RealtimeAudienceFacade facade) {
        return WebTestClient.bindToController(new RealtimeAudienceController(facade)).build();
    }

    private static final class RecordingRealtimeAudienceFacade implements RealtimeAudienceFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private Long lastAudienceId;
        private CdpEvent lastEvent;
        private Boolean lastRemoveOnNoMatch;
        private String lastSnapshotReason;
        private String lastActor;
        private Integer lastLimit;
        private Long lastLeftId;
        private Long lastRightId;
        private boolean failEvent;

        @Override
        public EventResult processEvent(Long tenantId, Long audienceId, CdpEvent event, boolean removeOnNoMatch) {
            operations.add("processEvent");
            lastTenantId = tenantId;
            lastAudienceId = audienceId;
            lastEvent = event;
            lastRemoveOnNoMatch = removeOnNoMatch;
            if (failEvent) {
                throw new IllegalArgumentException("audience is not found");
            }
            return new EventResult(audienceId, event.userId(), true, false, 2);
        }

        @Override
        public SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason, String actor) {
            operations.add("createSnapshot");
            lastTenantId = tenantId;
            lastAudienceId = audienceId;
            lastSnapshotReason = reason;
            lastActor = actor;
            return new SnapshotResult(1L, audienceId, reason, actor, 2, "2026-06-14T10:00:00");
        }

        @Override
        public List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
            operations.add("listSnapshots");
            lastTenantId = tenantId;
            lastAudienceId = audienceId;
            lastLimit = limit;
            return List.of(new SnapshotRow(1L, audienceId, 2, "MANUAL", "system", "2026-06-14T10:00:00"));
        }

        @Override
        public OverlapResult overlap(Long leftId, Long rightId) {
            operations.add("overlap");
            lastLeftId = leftId;
            lastRightId = rightId;
            return new OverlapResult(leftId, rightId, 1, List.of("user-1"));
        }

        @Override
        public SetOperationResult merge(Long leftId, Long rightId) {
            operations.add("merge");
            lastLeftId = leftId;
            lastRightId = rightId;
            return new SetOperationResult("MERGE", leftId, rightId, 2, List.of("user-1", "user-2"));
        }

        @Override
        public SetOperationResult exclude(Long baseId, Long excludedId) {
            operations.add("exclude");
            lastLeftId = baseId;
            lastRightId = excludedId;
            return new SetOperationResult("EXCLUDE", baseId, excludedId, 1, List.of("user-2"));
        }
    }
}
