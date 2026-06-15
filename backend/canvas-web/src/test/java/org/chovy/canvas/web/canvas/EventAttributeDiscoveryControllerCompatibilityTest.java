package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.chovy.canvas.cdp.api.CdpEventAttributeDiscoveryFacade;
import org.chovy.canvas.cdp.api.CdpEventAttributeDiscoveryFacade.DiscoveredAttributeView;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class EventAttributeDiscoveryControllerCompatibilityTest {

    @Test
    void discoveredAttributesReturnLegacyEnvelopeAndDtoFields() {
        RecordingCdpEventAttributeDiscoveryFacade facade = new RecordingCdpEventAttributeDiscoveryFacade();

        webClient(facade)
                .get()
                .uri("/canvas/event-attributes/discovered")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].id").isEqualTo(101)
                .jsonPath("$.data[0].eventCode").isEqualTo("USER_SIGNUP")
                .jsonPath("$.data[0].attrName").isEqualTo("plan")
                .jsonPath("$.data[0].attrType").isEqualTo("STRING")
                .jsonPath("$.data[0].status").isEqualTo("ACTIVE")
                .jsonPath("$.data[0].sampleValue").isEqualTo("pro")
                .jsonPath("$.data[0].firstSeenAt").isEqualTo("2026-06-01T10:00:00")
                .jsonPath("$.data[0].lastSeenAt").isEqualTo("2026-06-14T10:00:00")
                .jsonPath("$.data[1].eventCode").isEqualTo("ORDER_PAID");

        assertThat(facade.lastStatus).isNull();
    }

    @Test
    void discoveredAttributesPassStatusFilterToFinalFacade() {
        RecordingCdpEventAttributeDiscoveryFacade facade = new RecordingCdpEventAttributeDiscoveryFacade();

        webClient(facade)
                .get()
                .uri("/canvas/event-attributes/discovered?status=inactive")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].id").isEqualTo(103)
                .jsonPath("$.data[0].status").isEqualTo("INACTIVE")
                .jsonPath("$.data[0].attrName").isEqualTo("legacySource");

        assertThat(facade.lastStatus).isEqualTo("inactive");
    }

    private static WebTestClient webClient(CdpEventAttributeDiscoveryFacade facade) {
        return WebTestClient.bindToController(new EventAttributeDiscoveryController(facade)).build();
    }

    private static final class RecordingCdpEventAttributeDiscoveryFacade implements CdpEventAttributeDiscoveryFacade {
        private String lastStatus;

        @Override
        public java.util.List<DiscoveredAttributeView> listDiscovered(String status) {
            lastStatus = status;
            java.util.List<DiscoveredAttributeView> rows = java.util.List.of(
                    new DiscoveredAttributeView(
                            101L,
                            "USER_SIGNUP",
                            "plan",
                            "STRING",
                            "ACTIVE",
                            "pro",
                            "2026-06-01T10:00:00",
                            "2026-06-14T10:00:00"),
                    new DiscoveredAttributeView(
                            102L,
                            "ORDER_PAID",
                            "amount",
                            "DECIMAL",
                            "ACTIVE",
                            "12.30",
                            "2026-06-02T10:00:00",
                            "2026-06-14T11:00:00"),
                    new DiscoveredAttributeView(
                            103L,
                            "USER_SIGNUP",
                            "legacySource",
                            "STRING",
                            "INACTIVE",
                            "import",
                            "2026-06-03T10:00:00",
                            "2026-06-14T12:00:00"));
            if (status == null || status.isBlank()) {
                return rows;
            }
            return rows.stream()
                    .filter(row -> row.status().equalsIgnoreCase(status))
                    .toList();
        }
    }
}
