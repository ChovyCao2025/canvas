package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpTagFacade;
import org.chovy.canvas.cdp.api.CdpTagWriteCommand;
import org.chovy.canvas.cdp.api.CdpUserTagHistoryView;
import org.chovy.canvas.cdp.api.CdpUserTagView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpUserTagControllerCompatibilityTest {

    @Test
    void postMapsSetTagDefaultsAbsentBodyAndWrapsCompatibilityEnvelope() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();
        facade.setTagResponse = tagView("vip", "gold");

        webClient(facade)
                .post()
                .uri("/cdp/users/user-123/tags")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.setTagCalls).hasSize(1);
        SetTagCall call = facade.setTagCalls.getFirst();
        assertThat(call.tenantId()).isEqualTo(7L);
        assertThat(call.userId()).isEqualTo("user-123");
        assertThat(call.command()).isEqualTo(new CdpTagWriteCommand(
                null, null, null, null, null, null, "operator-1", null));
    }

    @Test
    void postPropagatesExplicitTenantAndTrimmedActorIntoCommandBody() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();

        webClient(facade)
                .post()
                .uri("/cdp/users/user-456/tags")
                .header("X-Tenant-Id", "42")
                .header("X-Actor", "  analyst-7  ")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "tagCode": "segment",
                          "tagValue": "new-user",
                          "reason": "manual set",
                          "sourceType": "console",
                          "sourceRefId": "ticket-1",
                          "operator": "body-operator",
                          "idempotencyKey": "idem-1"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success");

        assertThat(facade.setTagCalls).hasSize(1);
        SetTagCall call = facade.setTagCalls.getFirst();
        assertThat(call.tenantId()).isEqualTo(42L);
        assertThat(call.userId()).isEqualTo("user-456");
        assertThat(call.command().tagCode()).isEqualTo("segment");
        assertThat(call.command().tagValue()).isEqualTo("new-user");
        assertThat(call.command().reason()).isEqualTo("manual set");
        assertThat(call.command().sourceType()).isEqualTo("console");
        assertThat(call.command().sourceRefId()).isEqualTo("ticket-1");
        assertThat(call.command().operator()).isEqualTo("body-operator");
        assertThat(call.command().idempotencyKey()).isEqualTo("idem-1");
    }

    @Test
    void getCurrentTagsWrapsFacadeResponseInCompatibilityEnvelope() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();
        facade.currentTags = List.of(tagView("vip", "gold"));

        webClient(facade)
                .get()
                .uri("/cdp/users/user-123/tags")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].tenantId").isEqualTo(42)
                .jsonPath("$.data[0].userId").isEqualTo("user-123")
                .jsonPath("$.data[0].tagCode").isEqualTo("vip")
                .jsonPath("$.data[0].tagValue").isEqualTo("gold");

        assertThat(facade.listCurrentCalls).containsExactly(new ListCall(42L, "user-123"));
    }

    @Test
    void getTagHistoryWrapsFacadeResponseInCompatibilityEnvelope() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();
        facade.history = List.of(historyView());

        webClient(facade)
                .get()
                .uri("/cdp/users/user-123/tag-history")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].tenantId").isEqualTo(7)
                .jsonPath("$.data[0].userId").isEqualTo("user-123")
                .jsonPath("$.data[0].tagCode").isEqualTo("vip")
                .jsonPath("$.data[0].oldValue").isEqualTo("silver")
                .jsonPath("$.data[0].newValue").isEqualTo("gold")
                .jsonPath("$.data[0].operation").isEqualTo("SET")
                .jsonPath("$.data[0].operator").isEqualTo("operator-1");

        assertThat(facade.listHistoryCalls).containsExactly(new ListCall(7L, "user-123"));
    }

    @Test
    void deleteMapsDefaultTenantBlankActorAndFixedReason() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();

        webClient(facade)
                .delete()
                .uri("/cdp/users/user-123/tags/vip")
                .header("X-Actor", "   ")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.removeTagCalls).containsExactly(new RemoveTagCall(
                7L, "user-123", "vip", "user detail remove tag", "operator-1"));
    }

    @Test
    void deletePropagatesExplicitTenantAndTrimsActor() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();

        webClient(facade)
                .delete()
                .uri("/cdp/users/user-789/tags/churn")
                .header("X-Tenant-Id", "99")
                .header("X-Actor", "  ops-2 ")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success");

        assertThat(facade.removeTagCalls).containsExactly(new RemoveTagCall(
                99L, "user-789", "churn", "user detail remove tag", "ops-2"));
    }

    @Test
    void illegalArgumentExceptionMapsToApi001BadRequestEnvelope() {
        RecordingCdpTagFacade facade = new RecordingCdpTagFacade();
        facade.failure = new IllegalArgumentException("tagCode is required");

        webClient(facade)
                .get()
                .uri("/cdp/users/user-123/tags")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("tagCode is required")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpTagFacade facade) {
        return WebTestClient.bindToController(new CdpUserTagController(facade)).build();
    }

    private static CdpUserTagView tagView(String tagCode, String tagValue) {
        return new CdpUserTagView(
                1L,
                42L,
                "user-123",
                tagCode,
                tagValue,
                "STRING",
                "console",
                "ACTIVE",
                LocalDateTime.parse("2026-06-12T10:00:00"),
                null,
                LocalDateTime.parse("2026-06-12T10:05:00"));
    }

    private static CdpUserTagHistoryView historyView() {
        return new CdpUserTagHistoryView(
                7L,
                "user-123",
                "vip",
                "silver",
                "gold",
                "SET",
                "console",
                "ticket-1",
                "manual set",
                "operator-1",
                LocalDateTime.parse("2026-06-12T10:05:00"));
    }

    private static final class RecordingCdpTagFacade implements CdpTagFacade {
        private final List<SetTagCall> setTagCalls = new ArrayList<>();
        private final List<RemoveTagCall> removeTagCalls = new ArrayList<>();
        private final List<ListCall> listCurrentCalls = new ArrayList<>();
        private final List<ListCall> listHistoryCalls = new ArrayList<>();
        private CdpUserTagView setTagResponse;
        private List<CdpUserTagView> currentTags = List.of();
        private List<CdpUserTagHistoryView> history = List.of();
        private IllegalArgumentException failure;

        @Override
        public CdpUserTagView setTag(Long tenantId, String userId, CdpTagWriteCommand command) {
            failIfConfigured();
            setTagCalls.add(new SetTagCall(tenantId, userId, command));
            return setTagResponse;
        }

        @Override
        public void removeTag(Long tenantId, String userId, String tagCode, String reason, String operator) {
            failIfConfigured();
            removeTagCalls.add(new RemoveTagCall(tenantId, userId, tagCode, reason, operator));
        }

        @Override
        public List<CdpUserTagView> listCurrentTags(Long tenantId, String userId) {
            failIfConfigured();
            listCurrentCalls.add(new ListCall(tenantId, userId));
            return currentTags;
        }

        @Override
        public List<CdpUserTagHistoryView> listHistory(Long tenantId, String userId) {
            failIfConfigured();
            listHistoryCalls.add(new ListCall(tenantId, userId));
            return history;
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private record SetTagCall(Long tenantId, String userId, CdpTagWriteCommand command) {
    }

    private record RemoveTagCall(Long tenantId, String userId, String tagCode, String reason, String operator) {
    }

    private record ListCall(Long tenantId, String userId) {
    }
}
