package org.chovy.canvas.domain.risk.graph;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RiskGraphServiceTest {

    private final RiskGraphService service = new RiskGraphService();

    @Test
    void buildsGraphFromSharedDecisionIdentifiersAndListEntries() {
        Instant now = Instant.parse("2026-06-08T12:00:00Z");
        List<RiskGraphSubjectSnapshot> snapshots = List.of(
                snapshot("user-1", "run-1", now, Map.of(
                        "device", "device-a",
                        "ip", "10.0.0.1",
                        "phone", "+15551234567",
                        "email", "user@example.com",
                        "address", "addr-1",
                        "cardFingerprint", "card-a")),
                snapshot("user-2", "run-2", now, Map.of("device", "device-a")),
                snapshot("user-3", "run-3", now, Map.of("ip", "10.0.0.1")),
                snapshot("user-4", "run-4", now, Map.of("phone", "+15551234567")),
                snapshot("user-5", "run-5", now, Map.of("email", "user@example.com")),
                snapshot("user-6", "run-6", now, Map.of("address", "addr-1")),
                snapshot("user-7", "run-7", now, Map.of("cardFingerprint", "card-a")),
                new RiskGraphSubjectSnapshot(8L, "other-tenant-user", "run-8", now, Map.of("device", "device-a")));
        List<RiskGraphListSubject> listSubjects = List.of(
                new RiskGraphListSubject(7L, "block_device", "device", "device-a", "d***a", "shared bad device"),
                new RiskGraphListSubject(7L, "watch_phone", "phone", "+15551234567", "***4567", "manual review"),
                new RiskGraphListSubject(8L, "block_device", "device", "device-a", "d***a", "other tenant"));

        RiskGraphSummary summary = service.summarize(7L, "user-1", snapshots, listSubjects);

        assertThat(summary.targetSubjectId()).isEqualTo("user-1");
        assertThat(summary.associationCounts()).containsEntry("device", 1)
                .containsEntry("ip", 1)
                .containsEntry("phone", 1)
                .containsEntry("email", 1)
                .containsEntry("address", 1)
                .containsEntry("cardFingerprint", 1);
        assertThat(summary.connections())
                .extracting(RiskGraphConnection::subjectId)
                .containsExactlyInAnyOrder("user-2", "user-3", "user-4", "user-5", "user-6", "user-7");
        assertThat(summary.listHits())
                .extracting(RiskGraphListHit::listKey)
                .containsExactlyInAnyOrder("block_device", "watch_phone");
    }

    private RiskGraphSubjectSnapshot snapshot(String subjectId, String decisionRunId,
                                              Instant observedAt, Map<String, String> identifiers) {
        return new RiskGraphSubjectSnapshot(7L, subjectId, decisionRunId, observedAt, identifiers);
    }
}
