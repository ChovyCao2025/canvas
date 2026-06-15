package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpUserReadFacade;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserCanvasSummaryView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserInsightView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserProfileView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserRowView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserTagSummaryView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpUserReadControllerCompatibilityTest {

    @Test
    void listGetAndInsightWrapFacadeResultsInLegacyEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/cdp/users?keyword=alice")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].userId").isEqualTo("user-alice")
                .jsonPath("$.data[0].tags[0].tagCode").isEqualTo("vip");

        client.get()
                .uri("/cdp/users/user-alice")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.displayName").isEqualTo("Alice Chen")
                .jsonPath("$.data.phone").isEqualTo("138****0001");

        client.get()
                .uri("/cdp/users/user-alice/insight")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.profile.userId").isEqualTo("user-alice")
                .jsonPath("$.data.canvasRows[0].canvasName").isEqualTo("Welcome Journey");

        assertThat(facade.listCalls).containsExactly(new ListCall(42L, "alice"));
        assertThat(facade.detailCalls).containsExactly(new DetailCall(42L, "user-alice"));
        assertThat(facade.insightCalls).containsExactly(new DetailCall(42L, "user-alice"));
    }

    @Test
    void defaultsMissingTenantToExistingCdpCompatibilityTenant() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .get()
                .uri("/cdp/users/user-alice")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.userId").isEqualTo("user-alice");

        assertThat(facade.detailCalls).containsExactly(new DetailCall(7L, "user-alice"));
    }

    @Test
    void mapsIllegalArgumentExceptionToApi001BadRequestEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        facade.failure = new IllegalArgumentException("CDP user not found: missing-user");

        webClient(facade)
                .get()
                .uri("/cdp/users/missing-user")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("CDP user not found: missing-user")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpUserReadFacade facade) {
        return WebTestClient.bindToController(new CdpUserReadController(facade)).build();
    }

    private static CdpUserProfileView profile() {
        return new CdpUserProfileView(
                "user-alice",
                "Alice Chen",
                "138****0001",
                "alice@example.com",
                "ACTIVE",
                "{\"tier\":\"gold\"}",
                LocalDateTime.parse("2026-06-01T10:00:00"),
                LocalDateTime.parse("2026-06-12T10:00:00"));
    }

    private static CdpUserTagSummaryView tag() {
        return new CdpUserTagSummaryView(
                "vip",
                "VIP",
                "gold",
                "STRING",
                "MANUAL",
                "ACTIVE",
                LocalDateTime.parse("2026-06-02T10:00:00"),
                null,
                LocalDateTime.parse("2026-06-12T10:00:00"));
    }

    private static CdpUserCanvasSummaryView canvas() {
        return new CdpUserCanvasSummaryView(
                100L,
                "Welcome Journey",
                5,
                4,
                1,
                "SUCCESS",
                LocalDateTime.parse("2026-06-03T10:00:00"),
                LocalDateTime.parse("2026-06-12T10:00:00"));
    }

    private static final class RecordingFacade implements CdpUserReadFacade {
        private final List<ListCall> listCalls = new ArrayList<>();
        private final List<DetailCall> detailCalls = new ArrayList<>();
        private final List<DetailCall> insightCalls = new ArrayList<>();
        private IllegalArgumentException failure;

        @Override
        public List<CdpUserRowView> listUsers(Long tenantId, String keyword) {
            failIfConfigured();
            listCalls.add(new ListCall(tenantId, keyword));
            return List.of(new CdpUserRowView(
                    "user-alice",
                    "Alice Chen",
                    5,
                    4,
                    1,
                    "SUCCESS",
                    LocalDateTime.parse("2026-06-03T10:00:00"),
                    LocalDateTime.parse("2026-06-12T10:00:00"),
                    List.of(tag())));
        }

        @Override
        public CdpUserProfileView getUser(Long tenantId, String userId) {
            failIfConfigured();
            detailCalls.add(new DetailCall(tenantId, userId));
            return profile();
        }

        @Override
        public CdpUserInsightView getInsight(Long tenantId, String userId) {
            failIfConfigured();
            insightCalls.add(new DetailCall(tenantId, userId));
            return new CdpUserInsightView(userId, profile(), List.of(tag()), List.of(canvas()));
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private record ListCall(Long tenantId, String keyword) {
    }

    private record DetailCall(Long tenantId, String userId) {
    }
}
