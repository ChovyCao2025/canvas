package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpTagWriteCommand;
import org.chovy.canvas.cdp.api.CdpUserTagView;
import org.chovy.canvas.cdp.domain.CdpTagDefinition;
import org.chovy.canvas.cdp.domain.CdpTagRepository;
import org.chovy.canvas.cdp.domain.CdpUserTag;
import org.chovy.canvas.cdp.domain.CdpUserTagHistory;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CdpTagApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void setTagWritesHistoryBeforeEnsuringUserAndUpsertingCurrentTag() {
        FakeTagRepository tags = new FakeTagRepository();
        FakeProfileRepository profiles = new FakeProfileRepository(tags.sequence);
        CdpTagApplicationService service = new CdpTagApplicationService(tags, profiles, CLOCK);
        tags.definitions.put("vip", definition("vip", "BOOLEAN", true, 3));

        CdpUserTagView view = service.setTag(7L, " u1 ", new CdpTagWriteCommand(
                " vip ",
                "TRUE",
                "manual mark",
                null,
                null,
                "req-1",
                "admin",
                "idem-1"));

        assertThat(view.tenantId()).isEqualTo(7L);
        assertThat(view.userId()).isEqualTo("u1");
        assertThat(view.tagCode()).isEqualTo("vip");
        assertThat(view.tagValue()).isEqualTo("true");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.expiresAt()).isEqualTo(LocalDateTime.parse("2026-06-09T10:00:00"));
        assertThat(tags.sequence).containsExactly("history", "ensureUser", "saveCurrent");
        assertThat(tags.history).singleElement()
                .satisfies(history -> assertThat(history.operation()).isEqualTo("SET"));
        assertThat(profiles.profiles).containsKey("7:u1");
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingTagWithoutCurrentMutation() {
        FakeTagRepository tags = new FakeTagRepository();
        FakeProfileRepository profiles = new FakeProfileRepository(tags.sequence);
        CdpTagApplicationService service = new CdpTagApplicationService(tags, profiles, CLOCK);
        tags.definitions.put("vip", definition("vip", "BOOLEAN", true, null));
        CdpUserTag existing = new CdpUserTag(
                10L,
                7L,
                "u1",
                "vip",
                "false",
                "BOOLEAN",
                "MANUAL",
                "old",
                "ACTIVE",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                null,
                "admin",
                null,
                null);
        tags.current.put("7:u1:vip", existing);
        tags.duplicateIdempotencyKeys.add("idem-1");

        CdpUserTagView view = service.setTag(7L, "u1", new CdpTagWriteCommand(
                "vip",
                "true",
                "duplicate",
                null,
                "MANUAL",
                "req-2",
                "admin",
                "idem-1"));

        assertThat(view.tagValue()).isEqualTo("false");
        assertThat(tags.current.get("7:u1:vip").tagValue()).isEqualTo("false");
        assertThat(tags.sequence).containsExactly("history");
        assertThat(profiles.profiles).isEmpty();
    }

    @Test
    void manualTaggingIsRejectedWhenDefinitionDisablesManualWrites() {
        FakeTagRepository tags = new FakeTagRepository();
        CdpTagApplicationService service = new CdpTagApplicationService(tags, new FakeProfileRepository(), CLOCK);
        tags.definitions.put("vip", definition("vip", "STRING", false, null));

        assertThatThrownBy(() -> service.setTag(7L, "u1", new CdpTagWriteCommand(
                "vip",
                "gold",
                "bad",
                null,
                null,
                "req-1",
                "admin",
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manual tagging is disabled");
    }

    @Test
    void removeTagMarksCurrentTagRemovedAndWritesHistory() {
        FakeTagRepository tags = new FakeTagRepository();
        CdpTagApplicationService service = new CdpTagApplicationService(tags, new FakeProfileRepository(), CLOCK);
        tags.current.put("7:u1:vip", new CdpUserTag(
                10L,
                7L,
                "u1",
                "vip",
                "true",
                "BOOLEAN",
                "MANUAL",
                "req-1",
                "ACTIVE",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                null,
                "admin",
                null,
                null));

        service.removeTag(7L, "u1", "vip", "cleanup", "admin");

        assertThat(tags.current.get("7:u1:vip").status()).isEqualTo("REMOVED");
        assertThat(tags.history).singleElement()
                .satisfies(history -> {
                    assertThat(history.operation()).isEqualTo("REMOVE");
                    assertThat(history.oldValue()).isEqualTo("true");
                    assertThat(history.newValue()).isNull();
                });
    }

    private static CdpTagDefinition definition(String tagCode, String valueType, boolean manualEnabled, Integer ttlDays) {
        return new CdpTagDefinition(tagCode, tagCode, valueType, true, manualEnabled, ttlDays);
    }

    private static final class FakeTagRepository implements CdpTagRepository {
        private final Map<String, CdpTagDefinition> definitions = new LinkedHashMap<>();
        private final Map<String, CdpUserTag> current = new LinkedHashMap<>();
        private final List<CdpUserTagHistory> history = new ArrayList<>();
        private final List<String> duplicateIdempotencyKeys = new ArrayList<>();
        private final List<String> sequence = new ArrayList<>();

        @Override
        public CdpTagDefinition findEnabledDefinition(String tagCode) {
            return definitions.get(tagCode);
        }

        @Override
        public CdpUserTag findCurrentTag(Long tenantId, String userId, String tagCode) {
            return current.get(key(tenantId, userId, tagCode));
        }

        @Override
        public boolean saveHistory(CdpUserTagHistory row) {
            sequence.add("history");
            if (row.idempotencyKey() != null && duplicateIdempotencyKeys.contains(row.idempotencyKey())) {
                return false;
            }
            history.add(row);
            return true;
        }

        @Override
        public CdpUserTag saveCurrentTag(CdpUserTag tag) {
            sequence.add("saveCurrent");
            CdpUserTag saved = tag.id() == null
                    ? tag.withId(100L)
                    : tag;
            current.put(key(saved.tenantId(), saved.userId(), saved.tagCode()), saved);
            return saved;
        }

        @Override
        public List<CdpUserTag> listCurrentTags(Long tenantId, String userId) {
            return current.values().stream()
                    .filter(tag -> tag.tenantId().equals(tenantId))
                    .filter(tag -> tag.userId().equals(userId))
                    .filter(tag -> "ACTIVE".equals(tag.status()))
                    .toList();
        }

        @Override
        public List<CdpUserTagHistory> listHistory(Long tenantId, String userId) {
            return history.stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .filter(row -> row.userId().equals(userId))
                    .toList();
        }

        private static String key(Long tenantId, String userId, String tagCode) {
            return tenantId + ":" + userId + ":" + tagCode;
        }
    }

    private static final class FakeProfileRepository implements CustomerProfileRepository {
        private final Map<String, CustomerProfile> profiles = new LinkedHashMap<>();
        private final List<String> sequence;

        private FakeProfileRepository() {
            this(new ArrayList<>());
        }

        private FakeProfileRepository(List<String> sequence) {
            this.sequence = sequence;
        }

        @Override
        public CustomerProfile findProfile(Long tenantId, String userId) {
            return profiles.get(key(tenantId, userId));
        }

        @Override
        public CustomerProfile saveProfile(CustomerProfile profile) {
            sequence.add("ensureUser");
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
