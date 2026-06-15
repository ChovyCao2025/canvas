package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpBatchTrackCommand;
import org.chovy.canvas.cdp.api.CdpEventIngestionFacade;
import org.chovy.canvas.cdp.api.CdpIngestionResult;
import org.chovy.canvas.cdp.api.CdpTrackEventCommand;
import org.chovy.canvas.cdp.api.CdpWriteKeyAuthenticationFacade;
import org.chovy.canvas.cdp.api.CdpWriteKeyView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpEventIngestionControllerCompatibilityTest {

    @Test
    void trackRouteMapsBasicWriteKeyAuthAndBatchBodyThroughLegacyEnvelope() {
        RecordingWriteKeyAuthenticationFacade writeKeys = new RecordingWriteKeyAuthenticationFacade();
        RecordingCdpEventIngestionFacade facade = new RecordingCdpEventIngestionFacade();

        webClient(writeKeys, facade)
                .post()
                .uri("/cdp/events/track")
                .header(HttpHeaders.AUTHORIZATION, basic("ck_live_tenant_42", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "sentAt": "2026-06-15T00:52:00Z",
                          "batch": [
                            {
                              "messageId": "msg-1",
                              "type": "track",
                              "event": "OrderComplete",
                              "userId": "user-1",
                              "anonymousId": "anon-1",
                              "idempotencyKey": "idem-1",
                              "properties": {"amount": 99.9},
                              "context": {"platform": "WEB"},
                              "timestamp": "2026-06-15T00:51:00Z"
                            }
                          ]
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
                .jsonPath("$.data.accepted").isEqualTo(1)
                .jsonPath("$.data.rejected").isEqualTo(0)
                .jsonPath("$.data.errors").isArray();

        assertThat(writeKeys.lastAuthorization).isEqualTo(basic("ck_live_tenant_42", ""));
        assertThat(facade.lastWriteKey.writeKey()).isEqualTo("ck_live_tenant_42");
        assertThat(facade.lastWriteKey.tenantId()).isEqualTo(42L);
        assertThat(facade.lastCommand.batch()).hasSize(1);
        CdpTrackEventCommand event = facade.lastCommand.batch().getFirst();
        assertThat(event.messageId()).isEqualTo("msg-1");
        assertThat(event.event()).isEqualTo("OrderComplete");
        assertThat(event.properties()).containsEntry("amount", 99.9);
    }

    @Test
    void trackRouteDefaultsMissingBodyToEmptyBatch() {
        RecordingWriteKeyAuthenticationFacade writeKeys = new RecordingWriteKeyAuthenticationFacade();
        RecordingCdpEventIngestionFacade facade = new RecordingCdpEventIngestionFacade();

        webClient(writeKeys, facade)
                .post()
                .uri("/cdp/events/track")
                .header(HttpHeaders.AUTHORIZATION, basic("ck_test", "ignored"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.accepted").isEqualTo(0)
                .jsonPath("$.data.rejected").isEqualTo(0);

        assertThat(facade.lastCommand.batch()).isEmpty();
        assertThat(facade.lastWriteKey.writeKey()).isEqualTo("ck_test");
        assertThat(facade.lastWriteKey.tenantId()).isZero();
    }

    private static WebTestClient webClient(CdpWriteKeyAuthenticationFacade writeKeys,
                                           CdpEventIngestionFacade facade) {
        return WebTestClient.bindToController(new CdpEventIngestionController(writeKeys, facade)).build();
    }

    private static String basic(String username, String password) {
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private static final class RecordingWriteKeyAuthenticationFacade implements CdpWriteKeyAuthenticationFacade {
        private String lastAuthorization;

        @Override
        public CdpWriteKeyView authenticate(String authorizationHeader) {
            lastAuthorization = authorizationHeader;
            if ("ck_live_tenant_42".equals(rawWriteKey(authorizationHeader))) {
                return new CdpWriteKeyView(7L, 42L, "ck_live_tenant_42", "WEB", 100, null);
            }
            return new CdpWriteKeyView(8L, 0L, "ck_test", "WEB", 100, null);
        }

        private static String rawWriteKey(String authorizationHeader) {
            String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            return colon >= 0 ? decoded.substring(0, colon) : decoded;
        }
    }

    private static final class RecordingCdpEventIngestionFacade implements CdpEventIngestionFacade {
        private CdpWriteKeyView lastWriteKey;
        private CdpBatchTrackCommand lastCommand;

        @Override
        public CdpIngestionResult ingestBatch(CdpWriteKeyView writeKey, CdpBatchTrackCommand command) {
            lastWriteKey = writeKey;
            lastCommand = command;
            int accepted = command == null || command.batch() == null ? 0 : command.batch().size();
            return new CdpIngestionResult(accepted, 0, List.of());
        }
    }
}
