package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpBatchTrackCommand;
import org.chovy.canvas.cdp.api.CdpIngestionResult;
import org.chovy.canvas.cdp.api.CdpTrackEventCommand;
import org.chovy.canvas.cdp.api.CdpWriteKeyView;
import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventAttributeDiscoveryPort;
import org.chovy.canvas.cdp.domain.CdpEventDefinition;
import org.chovy.canvas.cdp.domain.CdpEventDefinitionRepository;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpEventRepository;
import org.chovy.canvas.cdp.domain.CdpPrivacyTombstonePort;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CdpEventIngestionApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void oversizedBatchIsRejectedBeforeAnyEventMutation() {
        FakeEvents events = new FakeEvents();
        CdpEventIngestionApplicationService service = service(events, new FakeProfiles(), 1);

        CdpIngestionResult result = service.ingestBatch(key(), new CdpBatchTrackCommand(
                List.of(validEvent("msg-1"), validEvent("msg-2")),
                OffsetDateTime.parse("2026-06-06T02:00:00Z")));

        assertThat(result.accepted()).isZero();
        assertThat(result.rejected()).isEqualTo(2);
        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.code()).isEqualTo("BATCH_TOO_LARGE"));
        assertThat(events.saved).isEmpty();
    }

    @Test
    void acceptedTrackEventEnsuresUserDiscoversAttributesPublishesAndMirrorsWarehouse() {
        FakeEvents events = new FakeEvents();
        FakeProfiles profiles = new FakeProfiles();
        CdpEventIngestionApplicationService service = service(events, profiles, 100);

        CdpIngestionResult result = service.ingestBatch(key(), new CdpBatchTrackCommand(
                List.of(new CdpTrackEventCommand(
                        " msg-1 ",
                        "TRACK",
                        "OrderComplete",
                        " user-1 ",
                        "anon-1",
                        "idem-1",
                        Map.of("amount", 99.9),
                        Map.of(
                                "session", Map.of("sessionId", "sess-1"),
                                "device", Map.of("id", "dev-1"),
                                "platform", "WEB"),
                        OffsetDateTime.parse("2026-06-06T01:00:00Z"),
                        null)),
                OffsetDateTime.parse("2026-06-06T02:00:00Z")));

        assertThat(result.accepted()).isEqualTo(1);
        assertThat(result.rejected()).isZero();
        CdpEventLog saved = events.saved.getFirst();
        assertThat(saved.tenantId()).isEqualTo(42L);
        assertThat(saved.writeKeyId()).isEqualTo(7L);
        assertThat(saved.messageId()).isEqualTo("msg-1");
        assertThat(saved.eventType()).isEqualTo("track");
        assertThat(saved.eventCode()).isEqualTo("OrderComplete");
        assertThat(saved.userId()).isEqualTo("user-1");
        assertThat(saved.sessionId()).isEqualTo("sess-1");
        assertThat(saved.deviceId()).isEqualTo("dev-1");
        assertThat(saved.platform()).isEqualTo("WEB");
        assertThat(saved.status()).isEqualTo("ACCEPTED");
        assertThat(saved.receivedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(profiles.profiles).containsKey("42:user-1");
        assertThat(events.discovered).containsExactly("42:OrderComplete");
        assertThat(events.published).containsExactly("msg-1");
        assertThat(events.mirrored).containsExactly("msg-1");
    }

    @Test
    void duplicateMessageAndUnknownEventAreNotPersisted() {
        FakeEvents events = new FakeEvents();
        events.duplicateMessages.add("msg-1");
        CdpEventIngestionApplicationService service = service(events, new FakeProfiles(), 100);

        CdpIngestionResult result = service.ingestBatch(key(), new CdpBatchTrackCommand(
                List.of(validEvent("msg-1"), validEvent("msg-2", "Unknown")),
                OffsetDateTime.parse("2026-06-06T02:00:00Z")));

        assertThat(result.accepted()).isZero();
        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.messageId()).isEqualTo("msg-2");
                    assertThat(error.code()).isEqualTo("INVALID_EVENT");
                    assertThat(error.message()).contains("unknown event code");
                });
        assertThat(events.saved).isEmpty();
        assertThat(events.published).isEmpty();
        assertThat(events.mirrored).isEmpty();
    }

    private static CdpEventIngestionApplicationService service(FakeEvents events,
                                                              FakeProfiles profiles,
                                                              int maxBatchSize) {
        return new CdpEventIngestionApplicationService(
                events,
                events,
                profiles,
                events,
                events,
                events,
                CdpPrivacyTombstonePort.allowAll(),
                CLOCK,
                maxBatchSize);
    }

    private static CdpWriteKeyView key() {
        return new CdpWriteKeyView(7L, 42L, "ck_test", "WEB", 100, null);
    }

    private static CdpTrackEventCommand validEvent(String messageId) {
        return validEvent(messageId, "OrderComplete");
    }

    private static CdpTrackEventCommand validEvent(String messageId, String eventCode) {
        return new CdpTrackEventCommand(
                messageId,
                "track",
                eventCode,
                "user-1",
                "anon-1",
                "idem-" + messageId,
                Map.of("amount", 20),
                Map.of("sessionId", "sess-1", "deviceId", "dev-1", "platform", "WEB"),
                OffsetDateTime.parse("2026-06-06T01:00:00Z"),
                null);
    }

    private static final class FakeEvents implements CdpEventRepository, CdpEventDefinitionRepository,
            CdpEventAttributeDiscoveryPort, CdpAcceptedEventPublisher, CdpWarehouseEventSinkPort {
        private final List<String> duplicateMessages = new ArrayList<>();
        private final List<CdpEventLog> saved = new ArrayList<>();
        private final List<String> discovered = new ArrayList<>();
        private final List<String> published = new ArrayList<>();
        private final List<String> mirrored = new ArrayList<>();

        @Override
        public boolean existsByMessageId(Long tenantId, String messageId) {
            return duplicateMessages.contains(messageId);
        }

        @Override
        public boolean existsByIdempotencyKey(Long tenantId, String idempotencyKey) {
            return false;
        }

        @Override
        public boolean save(CdpEventLog eventLog) {
            saved.add(eventLog.withId(100L));
            return true;
        }

        @Override
        public CdpEventDefinition findPublishedByCode(String eventCode) {
            return "OrderComplete".equals(eventCode)
                    ? new CdpEventDefinition(eventCode, true)
                    : null;
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

    private static final class FakeProfiles implements CustomerProfileRepository {
        private final Map<String, CustomerProfile> profiles = new LinkedHashMap<>();

        @Override
        public CustomerProfile findProfile(Long tenantId, String userId) {
            return profiles.get(key(tenantId, userId));
        }

        @Override
        public CustomerProfile saveProfile(CustomerProfile profile) {
            profiles.put(key(profile.tenantId(), profile.userId()), profile);
            return profile;
        }

        @Override
        public String findUserIdByIdentity(Long tenantId, String identityType, String identityValue) {
            return null;
        }

        @Override
        public void saveIdentity(Long tenantId, String userId, String identityType, String identityValue,
                                 String sourceType, String sourceRefId, boolean verified) {
        }

        private static String key(Long tenantId, String userId) {
            return tenantId + ":" + userId;
        }
    }
}
