package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.AudienceSnapshotFacade;
import org.chovy.canvas.cdp.api.AudienceSnapshotLockCommand;
import org.chovy.canvas.cdp.api.AudienceSnapshotView;
import org.chovy.canvas.cdp.api.CdpBatchTrackCommand;
import org.chovy.canvas.cdp.api.CdpEventIngestionFacade;
import org.chovy.canvas.cdp.api.CdpIngestionResult;
import org.chovy.canvas.cdp.api.CdpTagFacade;
import org.chovy.canvas.cdp.api.CdpTagWriteCommand;
import org.chovy.canvas.cdp.api.CdpUserTagHistoryView;
import org.chovy.canvas.cdp.api.CdpUserTagView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessSectionView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.chovy.canvas.cdp.api.CdpWriteKeyView;
import org.chovy.canvas.cdp.application.AudienceSnapshotApplicationService;
import org.chovy.canvas.cdp.application.CdpEventIngestionApplicationService;
import org.chovy.canvas.cdp.application.CdpTagApplicationService;
import org.chovy.canvas.cdp.application.CdpWarehouseReadinessApplicationService;
import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotRepository;
import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventAttributeDiscoveryPort;
import org.chovy.canvas.cdp.domain.CdpEventDefinition;
import org.chovy.canvas.cdp.domain.CdpEventDefinitionRepository;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpEventRepository;
import org.chovy.canvas.cdp.domain.CdpPrivacyTombstonePort;
import org.chovy.canvas.cdp.domain.CdpTagDefinition;
import org.chovy.canvas.cdp.domain.CdpTagRepository;
import org.chovy.canvas.cdp.domain.CdpUserTag;
import org.chovy.canvas.cdp.domain.CdpUserTagHistory;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessEvidence;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessRepository;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.chovy.canvas.cdp.domain.WarehouseBiDatasource;
import org.chovy.canvas.cdp.domain.WarehouseIncident;
import org.chovy.canvas.cdp.domain.WarehouseMaterializationRun;
import org.chovy.canvas.cdp.domain.WarehouseRealtimeStatus;
import org.chovy.canvas.cdp.domain.WarehouseSyncRun;
import org.chovy.canvas.cdp.domain.WarehouseWatermark;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class CdpApiCompatibilityTest {

    private static final Long TENANT_ID = 42L;
    private static final String WRITE_KEY = "ck_live_tenant_42";
    private static final String ACTOR = "operator-1";

    @Test
    void trackRoutePreservesEnvelopeTenantScopeBodyMappingValidationAndRejectedNoMutation() {
        Fixture fixture = Fixture.create();
        fixture.events.duplicateMessages.add(eventKey(TENANT_ID, "msg-duplicate"));

        webClient(fixture)
                .post()
                .uri("/cdp/events/track")
                .header("X-Cdp-Write-Key", WRITE_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "batch": [
                            {
                              "messageId": " msg-accepted ",
                              "type": "TRACK",
                              "event": "OrderComplete",
                              "userId": " user-1 ",
                              "anonymousId": "anon-1",
                              "idempotencyKey": "idem-accepted",
                              "properties": {"amount": 99.9, "currency": "CNY"},
                              "context": {
                                "session": {"sessionId": "sess-1"},
                                "device": {"id": "dev-1"},
                                "platform": "WEB"
                              },
                              "timestamp": "2026-06-06T01:00:00Z"
                            },
                            {
                              "messageId": "msg-duplicate",
                              "type": "track",
                              "event": "OrderComplete",
                              "userId": "dup-user",
                              "idempotencyKey": "idem-duplicate",
                              "properties": {"amount": 20},
                              "context": {"platform": "WEB"},
                              "timestamp": "2026-06-06T01:01:00Z"
                            },
                            {
                              "messageId": "msg-unknown",
                              "type": "track",
                              "event": "UnknownEvent",
                              "userId": "unknown-user",
                              "idempotencyKey": "idem-unknown",
                              "properties": {"amount": 30},
                              "context": {"platform": "WEB"},
                              "timestamp": "2026-06-06T01:02:00Z"
                            }
                          ],
                          "sentAt": "2026-06-06T02:00:00Z"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.data.accepted").isEqualTo(1)
                .jsonPath("$.data.rejected").isEqualTo(1)
                .jsonPath("$.data.errors.length()").isEqualTo(1)
                .jsonPath("$.data.errors[0].messageId").isEqualTo("msg-unknown")
                .jsonPath("$.data.errors[0].code").isEqualTo("INVALID_EVENT")
                .jsonPath("$.data.errors[0].message").value(message ->
                        assertThat((String) message).contains("unknown event code"));

        assertThat(fixture.events.saved).hasSize(1);
        CdpEventLog saved = fixture.events.saved.getFirst();
        assertThat(saved.tenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.writeKeyId()).isEqualTo(7L);
        assertThat(saved.messageId()).isEqualTo("msg-accepted");
        assertThat(saved.eventType()).isEqualTo("track");
        assertThat(saved.eventCode()).isEqualTo("OrderComplete");
        assertThat(saved.userId()).isEqualTo("user-1");
        assertThat(saved.anonymousId()).isEqualTo("anon-1");
        assertThat(saved.sessionId()).isEqualTo("sess-1");
        assertThat(saved.deviceId()).isEqualTo("dev-1");
        assertThat(saved.platform()).isEqualTo("WEB");
        assertThat(saved.properties()).containsEntry("currency", "CNY");
        assertThat(saved.idempotencyKey()).isEqualTo("idem-accepted");
        assertThat(saved.status()).isEqualTo(CdpEventLog.ACCEPTED);
        assertThat(fixture.profiles.profiles).containsOnlyKeys(profileKey(TENANT_ID, "user-1"));
        assertThat(fixture.events.discovered).containsExactly("42:OrderComplete");
        assertThat(fixture.events.published).containsExactly("msg-accepted");
        assertThat(fixture.events.mirrored).containsExactly("msg-accepted");
        assertThat(fixture.events.saved).extracting(CdpEventLog::messageId)
                .doesNotContain("msg-duplicate", "msg-unknown");
    }

    @Test
    void userTagRoutesPreserveEnvelopeMappingNormalizationHistoryRemoveAndValidationErrorShape() {
        Fixture fixture = Fixture.create();
        WebTestClient client = webClient(fixture);

        client.post()
                .uri("/cdp/users/{userId}/tags", "user-7")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "tagCode": " vip ",
                          "tagValue": "TRUE",
                          "reason": "manual mark",
                          "sourceType": " manual ",
                          "sourceRefId": "ticket-1",
                          "operator": " tag-admin ",
                          "idempotencyKey": "tag-idem-1"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(fixture.tags.history).singleElement()
                .satisfies(history -> {
                    assertThat(history.tenantId()).isEqualTo(TENANT_ID);
                    assertThat(history.userId()).isEqualTo("user-7");
                    assertThat(history.tagCode()).isEqualTo("vip");
                    assertThat(history.newValue()).isEqualTo("true");
                    assertThat(history.operation()).isEqualTo("SET");
                    assertThat(history.sourceType()).isEqualTo("MANUAL");
                    assertThat(history.sourceRefId()).isEqualTo("ticket-1");
                    assertThat(history.reason()).isEqualTo("manual mark");
                    assertThat(history.operator()).isEqualTo(" tag-admin ");
                });
        assertThat(fixture.profiles.profiles).containsKey(profileKey(TENANT_ID, "user-7"));

        client.get()
                .uri("/cdp/users/{userId}/tags", "user-7")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data[0].userId").isEqualTo("user-7")
                .jsonPath("$.data[0].tagCode").isEqualTo("vip")
                .jsonPath("$.data[0].tagValue").isEqualTo("true")
                .jsonPath("$.data[0].valueType").isEqualTo("BOOLEAN")
                .jsonPath("$.data[0].sourceType").isEqualTo("MANUAL")
                .jsonPath("$.data[0].status").isEqualTo("ACTIVE");

        client.get()
                .uri("/cdp/users/{userId}/tag-history", "user-7")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].tagCode").isEqualTo("vip")
                .jsonPath("$.data[0].newValue").isEqualTo("true")
                .jsonPath("$.data[0].operation").isEqualTo("SET")
                .jsonPath("$.data[0].sourceType").isEqualTo("MANUAL");

        client.delete()
                .uri("/cdp/users/{userId}/tags/{tagCode}", "user-7", "vip")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .header("X-Actor", ACTOR)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success");

        client.get()
                .uri("/cdp/users/{userId}/tags", "user-7")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(0);

        client.get()
                .uri("/cdp/users/{userId}/tag-history", "user-7")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[1].operation").isEqualTo("REMOVE")
                .jsonPath("$.data[1].oldValue").isEqualTo("true")
                .jsonPath("$.data[1].newValue").doesNotExist()
                .jsonPath("$.data[1].operator").isEqualTo(ACTOR);

        int historySizeBeforeInvalidRequest = fixture.tags.history.size();
        client.post()
                .uri("/cdp/users/{userId}/tags", "user-7")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "tagCode": "vip",
                          "tagValue": "yes",
                          "reason": "bad value",
                          "sourceType": "MANUAL",
                          "operator": "tag-admin"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("BOOLEAN tag value must be true or false"))
                .jsonPath("$.data").doesNotExist();
        assertThat(fixture.tags.history).hasSize(historySizeBeforeInvalidRequest);
    }

    @Test
    void audienceSnapshotRoutesMapToFacadeAndExposeLockUsersAndContainsEnvelopeData() {
        Fixture fixture = Fixture.create();
        fixture.audiences.resolvedUsers.put(100L, List.of("user-1", "", "user-2", "user-1", " "));
        WebTestClient client = webClient(fixture);

        client.post()
                .uri("/warehouse/audiences/{audienceId}/snapshots/lock", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "canvasId": 200,
                          "canvasVersionId": 300,
                          "nodeId": "audience-node",
                          "operator": "operator-1"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(500)
                .jsonPath("$.data.audienceId").isEqualTo(100)
                .jsonPath("$.data.canvasId").isEqualTo(200)
                .jsonPath("$.data.canvasVersionId").isEqualTo(300)
                .jsonPath("$.data.nodeId").isEqualTo("audience-node")
                .jsonPath("$.data.snapshotMode").isEqualTo("STATIC_LOCKED")
                .jsonPath("$.data.userCount").isEqualTo(2)
                .jsonPath("$.data.createdBy").isEqualTo("operator-1");

        client.get()
                .uri("/warehouse/audiences/snapshots/{snapshotId}/users", 500L)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0]").isEqualTo("user-1")
                .jsonPath("$.data[1]").isEqualTo("user-2");

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/warehouse/audiences/snapshots/{snapshotId}/contains")
                        .queryParam("userId", "user-2")
                        .build(500L))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.snapshotId").isEqualTo(500)
                .jsonPath("$.data.userId").isEqualTo("user-2")
                .jsonPath("$.data.contains").isEqualTo(true);

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/warehouse/audiences/snapshots/{snapshotId}/contains")
                        .queryParam("userId", "missing")
                        .build(500L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.contains").isEqualTo(false);
    }

    @Test
    void warehouseReadinessRoutePreservesEnvelopeSectionsProductionReadyAndBlockerFields() {
        Fixture fixture = Fixture.create();

        webClient(fixture)
                .get()
                .uri("/warehouse/readiness")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(TENANT_ID.intValue())
                .jsonPath("$.data.status").isEqualTo("FAIL")
                .jsonPath("$.data.productionReady").isEqualTo(false)
                .jsonPath("$.data.blockerCount").isEqualTo(1)
                .jsonPath("$.data.blockers[0].section").isEqualTo("realtime_pipelines")
                .jsonPath("$.data.blockers[0].status").isEqualTo("FAIL")
                .jsonPath("$.data.blockers[0].reason").value(reason ->
                        assertThat((String) reason).contains("realtime pipeline/job(s) failed"))
                .jsonPath("$.data.sections.length()").isEqualTo(5)
                .jsonPath("$.data.sections[0].key").isEqualTo("offline_sync")
                .jsonPath("$.data.sections[0].status").isEqualTo("PASS")
                .jsonPath("$.data.sections[1].key").isEqualTo("realtime_pipelines")
                .jsonPath("$.data.sections[1].status").isEqualTo("FAIL")
                .jsonPath("$.data.sections[2].key").isEqualTo("incidents")
                .jsonPath("$.data.sections[2].status").isEqualTo("PASS")
                .jsonPath("$.data.sections[3].key").isEqualTo("bi_datasources")
                .jsonPath("$.data.sections[3].status").isEqualTo("PASS")
                .jsonPath("$.data.sections[4].key").isEqualTo("audience_materialization")
                .jsonPath("$.data.sections[4].status").isEqualTo("PASS");
    }

    private static WebTestClient webClient(Fixture fixture) {
        return WebTestClient.bindToController(new CdpControllerAdapter(
                        fixture.ingestionFacade,
                        fixture.writeKeys,
                        fixture.tagFacade,
                        fixture.audienceFacade,
                        fixture.readinessFacade))
                .build();
    }

    private static String eventKey(Long tenantId, String messageId) {
        return tenantId + ":" + messageId;
    }

    private static String profileKey(Long tenantId, String userId) {
        return tenantId + ":" + userId;
    }

    @RestController
    private static final class CdpControllerAdapter {
        private final CdpEventIngestionFacade ingestionFacade;
        private final WriteKeyAuthenticator writeKeyAuthenticator;
        private final CdpTagFacade tagFacade;
        private final AudienceSnapshotFacade audienceFacade;
        private final CdpWarehouseReadinessFacade readinessFacade;

        private CdpControllerAdapter(CdpEventIngestionFacade ingestionFacade,
                                     WriteKeyAuthenticator writeKeyAuthenticator,
                                     CdpTagFacade tagFacade,
                                     AudienceSnapshotFacade audienceFacade,
                                     CdpWarehouseReadinessFacade readinessFacade) {
            this.ingestionFacade = ingestionFacade;
            this.writeKeyAuthenticator = writeKeyAuthenticator;
            this.tagFacade = tagFacade;
            this.audienceFacade = audienceFacade;
            this.readinessFacade = readinessFacade;
        }

        @PostMapping("/cdp/events/track")
        Mono<CompatibilityEnvelope<CdpIngestionResult>> track(
                @RequestHeader("X-Cdp-Write-Key") String writeKey,
                @RequestBody(required = false) CdpBatchTrackCommand command) {
            return envelope(() -> ingestionFacade.ingestBatch(
                    writeKeyAuthenticator.authenticate(writeKey),
                    command == null ? new CdpBatchTrackCommand(List.of(), null) : command));
        }

        @PostMapping("/cdp/users/{userId}/tags")
        Mono<CompatibilityEnvelope<Void>> addTag(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @PathVariable String userId,
                @RequestBody(required = false) CdpTagWriteCommand command) {
            return envelope(() -> {
                CdpTagWriteCommand request = command == null
                        ? new CdpTagWriteCommand(null, null, null, null, null, null, actorOrDefault(actor), null)
                        : command;
                tagFacade.setTag(tenantIdOrDefault(tenantId), userId, request);
                return null;
            });
        }

        @GetMapping("/cdp/users/{userId}/tags")
        Mono<CompatibilityEnvelope<List<CdpUserTagView>>> listTags(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @PathVariable String userId) {
            return envelope(() -> tagFacade.listCurrentTags(tenantIdOrDefault(tenantId), userId));
        }

        @GetMapping("/cdp/users/{userId}/tag-history")
        Mono<CompatibilityEnvelope<List<CdpUserTagHistoryView>>> listTagHistory(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @PathVariable String userId) {
            return envelope(() -> tagFacade.listHistory(tenantIdOrDefault(tenantId), userId));
        }

        @DeleteMapping("/cdp/users/{userId}/tags/{tagCode}")
        Mono<CompatibilityEnvelope<Void>> removeTag(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                @RequestHeader(value = "X-Actor", required = false) String actor,
                @PathVariable String userId,
                @PathVariable String tagCode) {
            return envelope(() -> {
                tagFacade.removeTag(tenantIdOrDefault(tenantId), userId, tagCode,
                        "user detail remove tag", actorOrDefault(actor));
                return null;
            });
        }

        @PostMapping("/warehouse/audiences/{audienceId}/snapshots/lock")
        Mono<CompatibilityEnvelope<AudienceSnapshotView>> lockSnapshot(
                @PathVariable Long audienceId,
                @RequestBody AudienceSnapshotLockRequest request) {
            return envelope(() -> audienceFacade.lockSnapshot(request.toCommand(audienceId)));
        }

        @GetMapping("/warehouse/audiences/snapshots/{snapshotId}/users")
        Mono<CompatibilityEnvelope<List<String>>> snapshotUsers(@PathVariable Long snapshotId) {
            return envelope(() -> audienceFacade.users(snapshotId));
        }

        @GetMapping("/warehouse/audiences/snapshots/{snapshotId}/contains")
        Mono<CompatibilityEnvelope<SnapshotContainsView>> snapshotContains(
                @PathVariable Long snapshotId,
                @RequestParam String userId) {
            return envelope(() -> new SnapshotContainsView(snapshotId, userId,
                    audienceFacade.contains(snapshotId, userId)));
        }

        @GetMapping("/warehouse/readiness")
        Mono<CompatibilityEnvelope<WarehouseReadinessCompatibilityView>> warehouseReadiness(
                @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
            return envelope(() -> WarehouseReadinessCompatibilityView.from(
                    readinessFacade.readiness(tenantIdOrDefault(tenantId))));
        }

        private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
            return Mono.fromCallable(() -> {
                try {
                    return CompatibilityEnvelope.ok(supplier.get());
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
                }
            });
        }

        @ExceptionHandler(ResponseStatusException.class)
        ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
            int status = exception.getStatusCode().value();
            String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(CompatibilityEnvelope.fail("API_001", status, message));
        }

        private static Long tenantIdOrDefault(Long tenantId) {
            return tenantId == null ? TENANT_ID : tenantId;
        }

        private static String actorOrDefault(String actor) {
            return actor == null || actor.isBlank() ? ACTOR : actor.trim();
        }
    }

    private record AudienceSnapshotLockRequest(
            Long canvasId,
            Long canvasVersionId,
            String nodeId,
            String operator) {

        private AudienceSnapshotLockCommand toCommand(Long audienceId) {
            return new AudienceSnapshotLockCommand(audienceId, canvasId, canvasVersionId, nodeId, operator);
        }
    }

    private record SnapshotContainsView(Long snapshotId, String userId, boolean contains) {
    }

    private record WarehouseReadinessCompatibilityView(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            List<CdpWarehouseReadinessSectionView> sections,
            boolean productionReady,
            int blockerCount,
            List<WarehouseReadinessBlocker> blockers) {

        private static WarehouseReadinessCompatibilityView from(CdpWarehouseReadinessView view) {
            List<WarehouseReadinessBlocker> blockers = view.sections().stream()
                    .filter(section -> !"PASS".equals(section.status()))
                    .map(section -> new WarehouseReadinessBlocker(section.key(), section.status(), section.reason()))
                    .toList();
            return new WarehouseReadinessCompatibilityView(
                    view.tenantId(),
                    view.status(),
                    view.generatedAt(),
                    view.sections(),
                    "PASS".equals(view.status()),
                    blockers.size(),
                    blockers);
        }
    }

    private record WarehouseReadinessBlocker(String section, String status, String reason) {
    }

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }

    private interface WriteKeyAuthenticator {
        CdpWriteKeyView authenticate(String writeKey);
    }

    private record Fixture(InMemoryEvents events,
                           InMemoryProfiles profiles,
                           InMemoryTags tags,
                           InMemoryAudiences audiences,
                           InMemoryWarehouseReadiness warehouse,
                           WriteKeyAuthenticator writeKeys,
                           CdpEventIngestionFacade ingestionFacade,
                           CdpTagFacade tagFacade,
                           AudienceSnapshotFacade audienceFacade,
                           CdpWarehouseReadinessFacade readinessFacade) {

        private static Fixture create() {
            InMemoryEvents events = new InMemoryEvents();
            InMemoryProfiles profiles = new InMemoryProfiles();
            InMemoryTags tags = new InMemoryTags();
            InMemoryAudiences audiences = new InMemoryAudiences();
            InMemoryWarehouseReadiness warehouse = new InMemoryWarehouseReadiness();
            WriteKeyAuthenticator writeKeys = writeKey -> {
                if (!WRITE_KEY.equals(writeKey)) {
                    throw new IllegalArgumentException("invalid write key");
                }
                return new CdpWriteKeyView(7L, TENANT_ID, writeKey, "WEB", 100, null);
            };
            return new Fixture(
                    events,
                    profiles,
                    tags,
                    audiences,
                    warehouse,
                    writeKeys,
                    new CdpEventIngestionApplicationService(
                            events,
                            events,
                            profiles,
                            events,
                            events,
                            events,
                            CdpPrivacyTombstonePort.allowAll(),
                            100),
                    new CdpTagApplicationService(tags, profiles),
                    new AudienceSnapshotApplicationService(audiences, 100),
                    new CdpWarehouseReadinessApplicationService(warehouse));
        }
    }

    private static final class InMemoryEvents implements CdpEventRepository, CdpEventDefinitionRepository,
            CdpEventAttributeDiscoveryPort, CdpAcceptedEventPublisher, CdpWarehouseEventSinkPort {
        private final Map<String, CdpEventDefinition> definitions = new LinkedHashMap<>();
        private final List<String> duplicateMessages = new ArrayList<>();
        private final List<String> duplicateIdempotencyKeys = new ArrayList<>();
        private final List<CdpEventLog> saved = new ArrayList<>();
        private final List<String> discovered = new ArrayList<>();
        private final List<String> published = new ArrayList<>();
        private final List<String> mirrored = new ArrayList<>();
        private long nextId = 100L;

        private InMemoryEvents() {
            definitions.put("OrderComplete", new CdpEventDefinition("OrderComplete", true));
        }

        @Override
        public boolean existsByMessageId(Long tenantId, String messageId) {
            return duplicateMessages.contains(eventKey(tenantId, messageId))
                    || saved.stream()
                    .anyMatch(event -> tenantId.equals(event.tenantId()) && messageId.equals(event.messageId()));
        }

        @Override
        public boolean existsByIdempotencyKey(Long tenantId, String idempotencyKey) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                return false;
            }
            return duplicateIdempotencyKeys.contains(tenantId + ":" + idempotencyKey)
                    || saved.stream()
                    .anyMatch(event -> tenantId.equals(event.tenantId())
                            && idempotencyKey.equals(event.idempotencyKey()));
        }

        @Override
        public boolean save(CdpEventLog eventLog) {
            if (existsByMessageId(eventLog.tenantId(), eventLog.messageId())
                    || existsByIdempotencyKey(eventLog.tenantId(), eventLog.idempotencyKey())) {
                return false;
            }
            saved.add(eventLog.withId(nextId++));
            return true;
        }

        @Override
        public CdpEventDefinition findPublishedByCode(String eventCode) {
            return definitions.get(eventCode);
        }

        @Override
        public void discover(Long tenantId, String eventCode, Map<String, Object> properties) {
            discovered.add(tenantId + ":" + eventCode);
        }

        @Override
        public void publishAccepted(CdpEventLog eventLog) {
            published.add(eventLog.messageId());
        }

        @Override
        public void mirrorAccepted(CdpEventLog eventLog) {
            mirrored.add(eventLog.messageId());
        }
    }

    private static final class InMemoryProfiles implements CustomerProfileRepository {
        private final Map<String, CustomerProfile> profiles = new LinkedHashMap<>();
        private final List<String> identities = new ArrayList<>();
        private long nextId = 1000L;

        @Override
        public CustomerProfile findProfile(Long tenantId, String userId) {
            return profiles.get(profileKey(tenantId, userId));
        }

        @Override
        public CustomerProfile saveProfile(CustomerProfile profile) {
            CustomerProfile saved = profile.id() == null ? profile.withId(nextId++) : profile;
            profiles.put(profileKey(saved.tenantId(), saved.userId()), saved);
            return saved;
        }

        @Override
        public String findUserIdByIdentity(Long tenantId, String identityType, String identityValue) {
            return null;
        }

        @Override
        public void saveIdentity(Long tenantId, String userId, String identityType, String identityValue,
                                 String sourceType, String sourceRefId, boolean verified) {
            identities.add(tenantId + ":" + userId + ":" + identityType + ":" + identityValue);
        }
    }

    private static final class InMemoryTags implements CdpTagRepository {
        private final Map<String, CdpTagDefinition> definitions = new LinkedHashMap<>();
        private final Map<String, CdpUserTag> current = new LinkedHashMap<>();
        private final List<CdpUserTagHistory> history = new ArrayList<>();
        private final List<String> duplicateIdempotencyKeys = new ArrayList<>();
        private long nextId = 2000L;

        private InMemoryTags() {
            definitions.put("vip", new CdpTagDefinition("vip", "VIP", "BOOLEAN", true, true, null));
            definitions.put("score", new CdpTagDefinition("score", "Score", "NUMBER", true, true, null));
        }

        @Override
        public CdpTagDefinition findEnabledDefinition(String tagCode) {
            return definitions.get(tagCode);
        }

        @Override
        public CdpUserTag findCurrentTag(Long tenantId, String userId, String tagCode) {
            return current.get(tagKey(tenantId, userId, tagCode));
        }

        @Override
        public boolean saveHistory(CdpUserTagHistory row) {
            if (row.idempotencyKey() != null && duplicateIdempotencyKeys.contains(row.idempotencyKey())) {
                return false;
            }
            history.add(row);
            return true;
        }

        @Override
        public CdpUserTag saveCurrentTag(CdpUserTag tag) {
            CdpUserTag saved = tag.id() == null ? tag.withId(nextId++) : tag;
            current.put(tagKey(saved.tenantId(), saved.userId(), saved.tagCode()), saved);
            return saved;
        }

        @Override
        public List<CdpUserTag> listCurrentTags(Long tenantId, String userId) {
            return current.values().stream()
                    .filter(tag -> tenantId.equals(tag.tenantId()))
                    .filter(tag -> userId.equals(tag.userId()))
                    .filter(tag -> "ACTIVE".equals(tag.status()))
                    .sorted(Comparator.comparing(CdpUserTag::tagCode))
                    .toList();
        }

        @Override
        public List<CdpUserTagHistory> listHistory(Long tenantId, String userId) {
            return history.stream()
                    .filter(row -> tenantId.equals(row.tenantId()))
                    .filter(row -> userId.equals(row.userId()))
                    .toList();
        }

        private static String tagKey(Long tenantId, String userId, String tagCode) {
            return tenantId + ":" + userId + ":" + tagCode;
        }
    }

    private static final class InMemoryAudiences implements AudienceSnapshotRepository {
        private final Map<Long, List<String>> resolvedUsers = new LinkedHashMap<>();
        private final Map<Long, AudienceSnapshot> snapshots = new LinkedHashMap<>();
        private final Map<Long, String> defaultModes = new LinkedHashMap<>();
        private long nextSnapshotId = 500L;

        @Override
        public List<String> resolveUsers(Long audienceId) {
            return resolvedUsers.getOrDefault(audienceId, List.of());
        }

        @Override
        public AudienceSnapshot save(AudienceSnapshot snapshot) {
            AudienceSnapshot saved = snapshot.withId(nextSnapshotId++);
            snapshots.put(saved.id(), saved);
            return saved;
        }

        @Override
        public AudienceSnapshot findSnapshot(Long snapshotId) {
            return snapshots.get(snapshotId);
        }

        @Override
        public String defaultSnapshotMode(Long audienceId) {
            return defaultModes.get(audienceId);
        }
    }

    private static final class InMemoryWarehouseReadiness implements CdpWarehouseReadinessRepository {

        @Override
        public CdpWarehouseReadinessEvidence evidence(Long tenantId) {
            LocalDateTime recent = LocalDateTime.parse("2026-06-06T09:55:00");
            return new CdpWarehouseReadinessEvidence(
                    tenantId,
                    List.of(new WarehouseSyncRun("SUCCESS", recent, recent, recent, recent)),
                    List.of(new WarehouseWatermark(recent, recent)),
                    new WarehouseRealtimeStatus(2, 1, 0, 1),
                    List.<WarehouseIncident>of(),
                    List.of(new WarehouseBiDatasource(true)),
                    List.of(new WarehouseMaterializationRun("SUCCESS", recent, recent)));
        }
    }
}
