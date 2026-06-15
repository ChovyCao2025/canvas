package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.PaidMediaFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class PaidMediaControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "paid-media-operator";

    @Test
    void mapsLegacyAudienceSyncRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingPaidMediaFacade facade = new RecordingPaidMediaFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("/destinations", "upsertDestination", Map.of(
                        "provider", "meta",
                        "destinationKey", "vip-buyers")),
                post("/runs", "syncAudience", Map.of(
                        "destinationId", 1,
                        "audienceId", 9,
                        "userIds", List.of("u-1"))),
                get("/runs?destinationId=1&audienceId=9&status=success&limit=120", "runs"),
                get("/runs/10/members?status=eligible&limit=0", "members"));

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
    void forwardsHeadersBodiesPathVariablesAndBoundedQueryLimits() {
        RecordingPaidMediaFacade facade = new RecordingPaidMediaFacade();

        webClient(facade)
                .post()
                .uri("/canvas/paid-media/audience-sync/destinations")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "provider": "meta",
                          "destinationKey": "vip-buyers",
                          "identifierTypes": ["email"],
                          "metadata": {"pixelId": "px-1"}
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.provider").isEqualTo("META")
                .jsonPath("$.data.createdBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.metadata.pixelId").isEqualTo("px-1");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastDestinationCommand.destinationKey()).isEqualTo("vip-buyers");

        webClient(facade)
                .get()
                .uri("/canvas/paid-media/audience-sync/runs?destinationId=1&audienceId=2&status=success&limit=120")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastRunQuery).isEqualTo(new PaidMediaFacade.RunQuery(1L, 2L, "success", 100));

        webClient(facade)
                .get()
                .uri("/canvas/paid-media/audience-sync/runs/99/members?status=skipped&limit=0")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastMemberQuery).isEqualTo(new PaidMediaFacade.MemberQuery(99L, "skipped", 1));
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingPaidMediaFacade facade = new RecordingPaidMediaFacade();
        facade.failSync = true;

        webClient(facade)
                .post()
                .uri("/canvas/paid-media/audience-sync/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"destinationId": 99, "audienceId": 1}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("paid-media destination is not found")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(PaidMediaFacade facade) {
        return WebTestClient.bindToController(new PaidMediaController(facade)).build();
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/paid-media/audience-sync" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
        }
    }

    private static final class RecordingPaidMediaFacade implements PaidMediaFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private DestinationCommand lastDestinationCommand;
        private RunQuery lastRunQuery;
        private MemberQuery lastMemberQuery;
        private boolean failSync;

        @Override
        public DestinationView upsertDestination(Long tenantId, DestinationCommand command, String actor) {
            operations.add("upsertDestination");
            lastTenantId = tenantId;
            lastActor = actor;
            lastDestinationCommand = command;
            return new DestinationView(1L, tenantId, command.provider().toUpperCase(), command.destinationKey(),
                    command.displayName(), command.accountId(), command.externalAudienceId(),
                    command.identifierTypes(), command.consentChannel(), true, true, command.metadata(), actor,
                    null, null);
        }

        @Override
        public SyncRunView syncAudience(Long tenantId, SyncCommand command, String actor) {
            operations.add("syncAudience");
            lastTenantId = tenantId;
            lastActor = actor;
            if (failSync) {
                throw new IllegalArgumentException("paid-media destination is not found");
            }
            return new SyncRunView(10L, tenantId, command.destinationId(), command.audienceId(), "META",
                    "SUCCESS", command.userIds().size(), command.userIds().size(), 0, 0,
                    command.externalOperationId(), null, command.metadata(), actor, null, null);
        }

        @Override
        public List<SyncRunView> runs(Long tenantId, RunQuery query) {
            operations.add("runs");
            lastTenantId = tenantId;
            lastRunQuery = query;
            return List.of(new SyncRunView(10L, tenantId, query.destinationId(), query.audienceId(), "META",
                    "SUCCESS", 1, 1, 0, 0, "op-1", null, Map.of(), DEFAULT_ACTOR, null, null));
        }

        @Override
        public List<MemberView> members(Long tenantId, MemberQuery query) {
            operations.add("members");
            lastTenantId = tenantId;
            lastMemberQuery = query;
            return List.of(new MemberView(20L, tenantId, query.runId(), 1L, 2L, "META", "u-1",
                    "EMAIL", "hash", "ELIGIBLE", null, null));
        }
    }
}
