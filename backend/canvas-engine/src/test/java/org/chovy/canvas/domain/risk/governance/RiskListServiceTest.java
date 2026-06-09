package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.runtime.RiskListType;
import org.chovy.canvas.web.risk.RiskListAuditSink;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RiskListServiceTest {

    private final RecordingAuditSink auditSink = new RecordingAuditSink();
    private final RecordingHasher hasher = new RecordingHasher();
    private final RiskListService service = new RiskListService(auditSink, hasher,
            Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsTenantScopedList() {
        RiskListView view = service.createList(7L, command(), "alice");

        assertThat(view.tenantId()).isEqualTo(7L);
        assertThat(view.listKey()).isEqualTo("blacklist.user");
        assertThat(view.subjectType()).isEqualTo(RiskSubjectType.USER_ID);
    }

    @Test
    void addsEntryWithHashedSubjectAndMaskedValue() {
        service.createList(7L, command(), "alice");

        RiskListEntryView entry = service.addEntry(7L, "blacklist.user",
                entryCommand("user-123"), "alice");

        assertThat(entry.subjectHash()).isEqualTo("hash:user-123");
        assertThat(entry.subjectMasked()).isEqualTo("u***3");
        assertThat(entry.toString()).doesNotContain("user-123");
    }

    @Test
    void rejectsEntryWhenSubjectTypeDoesNotMatchList() {
        service.createList(7L, command(), "alice");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.addEntry(7L, "blacklist.user",
                        entryCommand("device-1").withSubjectType(RiskSubjectType.DEVICE_ID), "alice"))
                .withMessageContaining("subject type");
    }

    @Test
    void importsEntriesAtomicallyWithRowLevelErrors() {
        service.createList(7L, command(), "alice");
        RiskListImportCommand importCommand = new RiskListImportCommand(List.of(
                entryCommand("user-1"),
                entryCommand("device-1").withSubjectType(RiskSubjectType.DEVICE_ID),
                entryCommand("user-2")));

        RiskListImportResult result = service.importEntries(7L, "blacklist.user", importCommand, "alice");

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.acceptedRows()).isEqualTo(2);
        assertThat(result.rejectedRows()).isEqualTo(1);
        assertThat(result.rowErrors()).containsExactly("row 2: subject type mismatch");
        assertThat(result.importAuditId()).startsWith("audit-");
        assertThat(service.entries(7L, "blacklist.user")).hasSize(2);
    }

    @Test
    void preventsRawPiiInResponseBody() {
        service.createList(7L, command(), "alice");

        RiskListEntryView entry = service.addEntry(7L, "blacklist.user",
                entryCommand("user-secret-123"), "alice");

        assertThat(entry.toString()).doesNotContain("user-secret-123");
        assertThat(entry.subjectMasked()).doesNotContain("secret");
    }

    @Test
    void ignoresExpiredEntriesInLookup() {
        service.createList(7L, command(), "alice");
        service.addEntry(7L, "blacklist.user",
                entryCommand("user-123").withWindow(
                        Instant.parse("2026-06-01T00:00:00Z"),
                        Instant.parse("2026-06-08T09:59:59Z")), "alice");

        assertThat(service.lookup(7L, "blacklist.user", "user-123")).isEmpty();
    }

    @Test
    void removesEntryWithAuditEvent() {
        service.createList(7L, command(), "alice");
        RiskListEntryView entry = service.addEntry(7L, "blacklist.user", entryCommand("user-123"), "alice");

        service.removeEntry(7L, "blacklist.user", entry.id(), "alice");

        assertThat(service.entries(7L, "blacklist.user")).isEmpty();
        assertThat(auditSink.events).contains("ENTRY_REMOVED:blacklist.user:" + entry.id() + ":alice");
    }

    @Test
    void recordsListHitWithoutRawSubject() {
        service.createList(7L, command(), "alice");
        service.addEntry(7L, "blacklist.user", entryCommand("user-123"), "alice");

        RiskListHitView hit = service.recordHit(7L, "blacklist.user", "user-123", "decision-1");

        assertThat(hit.subjectHash()).isEqualTo("hash:user-123");
        assertThat(hit.toString()).doesNotContain("user-123");
        assertThat(hit.decisionRunId()).isEqualTo("decision-1");
    }

    private RiskListCommand command() {
        return new RiskListCommand("blacklist.user", RiskListType.BLACK, RiskSubjectType.USER_ID, false);
    }

    private RiskListEntryCommand entryCommand(String rawSubject) {
        return new RiskListEntryCommand(rawSubject, RiskSubjectType.USER_ID, "manual", "operator",
                Instant.parse("2026-06-01T00:00:00Z"), null);
    }

    private static final class RecordingAuditSink implements RiskListAuditSink {
        private final List<String> events = new ArrayList<>();

        @Override
        public String record(Long tenantId, String eventType, String resourceKey, String resourceId, String actor) {
            events.add(eventType + ":" + resourceKey + ":" + resourceId + ":" + actor);
            return "audit-" + events.size();
        }
    }

    private static final class RecordingHasher implements RiskListSubjectHasher {
        @Override
        public String hash(String rawSubject) {
            return "hash:" + rawSubject;
        }
    }
}
