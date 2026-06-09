package org.chovy.canvas.domain.risk.runtime;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RiskListMatcherTest {

    private final RecordingHasher hasher = new RecordingHasher();
    private final RecordingRepository repository = new RecordingRepository();
    private final RiskListMatcher matcher = new RiskListMatcher(repository, hasher,
            Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void blackListMatchReturnsBlockSignal() {
        repository.entry = Optional.of(entry(RiskListType.BLACK));

        RiskListMatchResult result = matcher.match(1L, "blacklist.user", "u-1");

        assertThat(result.matched()).isTrue();
        assertThat(result.signal().action()).isEqualTo(RiskDecisionAction.BLOCK);
    }

    @Test
    void whiteListMatchReturnsAllowSignal() {
        repository.entry = Optional.of(entry(RiskListType.WHITE));

        RiskListMatchResult result = matcher.match(1L, "whitelist.user", "u-1");

        assertThat(result.signal().action()).isEqualTo(RiskDecisionAction.ALLOW);
    }

    @Test
    void complianceBlackListReturnsHighestPriorityBlockSignal() {
        repository.entry = Optional.of(entry(RiskListType.COMPLIANCE_BLACK));

        RiskListMatchResult result = matcher.match(1L, "compliance.user", "u-1");

        assertThat(result.signal().action()).isEqualTo(RiskDecisionAction.BLOCK);
        assertThat(result.signal().listType()).isEqualTo(RiskListType.COMPLIANCE_BLACK);
        assertThat(result.signal().scoreDelta()).isEqualTo(100);
    }

    @Test
    void grayListReturnsReviewSignal() {
        repository.entry = Optional.of(entry(RiskListType.GRAY));

        RiskListMatchResult result = matcher.match(1L, "gray.user", "u-1");

        assertThat(result.signal().action()).isEqualTo(RiskDecisionAction.REVIEW);
    }

    @Test
    void observeListReturnsShadowOnlySignal() {
        repository.entry = Optional.of(entry(RiskListType.OBSERVE));

        RiskListMatchResult result = matcher.match(1L, "observe.user", "u-1");

        assertThat(result.signal().action()).isEqualTo(RiskDecisionAction.SHADOW_ONLY);
        assertThat(result.signal().shadowSignal()).isTrue();
    }

    @Test
    void expiredEntryIsIgnored() {
        repository.entry = Optional.of(entry(RiskListType.BLACK)
                .withEffectiveWindow(Instant.parse("2026-06-01T00:00:00Z"),
                        Instant.parse("2026-06-08T09:59:59Z")));

        RiskListMatchResult result = matcher.match(1L, "blacklist.user", "u-1");

        assertThat(result.matched()).isFalse();
    }

    @Test
    void notYetEffectiveEntryIsIgnored() {
        repository.entry = Optional.of(entry(RiskListType.BLACK)
                .withEffectiveWindow(Instant.parse("2026-06-08T10:00:01Z"), null));

        RiskListMatchResult result = matcher.match(1L, "blacklist.user", "u-1");

        assertThat(result.matched()).isFalse();
    }

    @Test
    void rawSubjectIsHashedBeforeLookup() {
        repository.entry = Optional.of(entry(RiskListType.BLACK));

        matcher.match(1L, "blacklist.user", "raw-user-id");

        assertThat(hasher.rawInputs).containsExactly("raw-user-id");
        assertThat(repository.subjectHash).isEqualTo("hash:raw-user-id");
        assertThat(repository.rawSubjectSeen).isNull();
    }

    @Test
    void resultMasksSubjectAndNeverExposesRawValue() {
        repository.entry = Optional.of(entry(RiskListType.BLACK).withSubjectMasked("u***1"));

        RiskListMatchResult result = matcher.match(1L, "blacklist.user", "raw-user-id");

        assertThat(result.subjectHash()).isEqualTo("hash:raw-user-id");
        assertThat(result.subjectMasked()).isEqualTo("u***1");
        assertThat(result.toString()).doesNotContain("raw-user-id");
    }

    private RiskListEntry entry(RiskListType type) {
        return new RiskListEntry(
                1L,
                "blacklist.user",
                "hash:u-1",
                "masked",
                type,
                "manual review",
                Instant.parse("2026-06-01T00:00:00Z"),
                null);
    }

    private static final class RecordingHasher implements RiskSubjectHasher {
        private final List<String> rawInputs = new ArrayList<>();

        @Override
        public String hash(String rawSubject) {
            rawInputs.add(rawSubject);
            return "hash:" + rawSubject;
        }
    }

    private static final class RecordingRepository implements RiskListEntryRepository {
        private Optional<RiskListEntry> entry = Optional.empty();
        private String subjectHash;
        private String rawSubjectSeen;

        @Override
        public Optional<RiskListEntry> findActiveEntry(Long tenantId, String listKey, String subjectHash) {
            this.subjectHash = subjectHash;
            this.rawSubjectSeen = null;
            return entry;
        }
    }
}
