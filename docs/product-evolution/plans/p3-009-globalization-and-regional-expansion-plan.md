# Globalization And Regional Expansion Evidence Plan

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a region-readiness evidence gate so globalization and regional expansion work cannot proceed until market demand, compliance, channel, residency, proof, and rollback evidence are reviewed.

**Architecture:** Implement an additive evidence registry and small domain service. This slice records governance decisions only; it does not add translations, regional channels, currency handling, timezone behavior, data residency routing, or deployment changes.

**Tech Stack:** Java 21, Spring Boot project layout, Flyway, JUnit 5, AssertJ, Mockito.

---

## Spec Reference

- `docs/product-evolution/specs/p3-009-globalization-and-regional-expansion.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#globalization-and-regional-expansion`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V181__regional_expansion_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceService.java`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceServiceTest.java`

**Documentation**
- Modify: `docs/product-evolution/specs/p3-009-globalization-and-regional-expansion.md`
- Modify: `docs/product-evolution/plans/p3-009-globalization-and-regional-expansion-plan.md`

**No Frontend**
- No frontend files are part of this slice because the spec is a discovery/evidence gate. A UI must be opened by a child spec after `APPROVED_FOR_CHILD_SPEC`.

### Task 1: Evidence Contract Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceServiceTest.java`

- [x] **Step 1: Write migration and service tests**

Create `RegionalExpansionEvidenceServiceTest`:

```java
package org.chovy.canvas.strategy.globalization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegionalExpansionEvidenceServiceTest {

    @Test
    void migrationCreatesRegionalEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V181__regional_expansion_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS regional_expansion_evidence")
                .contains("region_code VARCHAR(32) NOT NULL")
                .contains("demand_evidence TEXT NOT NULL")
                .contains("compliance_notes TEXT NOT NULL")
                .contains("data_residency_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_note VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingDemandComplianceResidencyOrRollbackEvidence() {
        RegionalExpansionEvidenceService.EvidenceRepository repository = mock(RegionalExpansionEvidenceService.EvidenceRepository.class);
        RegionalExpansionEvidenceService service = new RegionalExpansionEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new RegionalExpansionEvidenceService.EvidenceRequest(
                "JP", "owner-1", "", "JPY and ja-JP", "Asia/Tokyo", "LINE pending",
                "APPI review", "Tokyo residency review", "pilot hypothesis",
                "mvn test", "disable region flag")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demand evidence is required");

        assertThatThrownBy(() -> service.register(new RegionalExpansionEvidenceService.EvidenceRequest(
                "JP", "owner-1", "two enterprise prospects", "JPY and ja-JP", "Asia/Tokyo", "LINE pending",
                "APPI review", "", "pilot hypothesis", "mvn test", "disable region flag")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data residency notes are required");
    }

    @Test
    void registerStoresBlockedDecisionUntilReviewed() {
        RegionalExpansionEvidenceService.EvidenceRepository repository = mock(RegionalExpansionEvidenceService.EvidenceRepository.class);
        RegionalExpansionEvidenceService service = new RegionalExpansionEvidenceService(repository);

        service.register(new RegionalExpansionEvidenceService.EvidenceRequest(
                "JP", "owner-1", "two enterprise prospects", "JPY and ja-JP",
                "Asia/Tokyo", "LINE pending", "APPI review", "Tokyo residency review",
                "pilot limited to sandbox tenants", "cd backend && mvn -pl canvas-engine test",
                "disable regional_expansion.registry.enabled"));

        verify(repository).insert(argThat(record ->
                record.regionCode().equals("JP")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.rollbackNote().contains("disable regional_expansion")));
    }
}
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RegionalExpansionEvidenceServiceTest
```

Expected: FAIL because the migration and service do not exist.

### Task 2: Migration And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V181__regional_expansion_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceServiceTest.java`

- [x] **Step 1: Add the additive migration**

Create `V181__regional_expansion_evidence.sql`:

```sql
CREATE TABLE IF NOT EXISTS regional_expansion_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  region_code VARCHAR(32) NOT NULL,
  owner_id VARCHAR(128) NOT NULL,
  demand_evidence TEXT NOT NULL,
  locale_currency_notes TEXT NOT NULL,
  timezone_notes TEXT NOT NULL,
  channel_notes TEXT NOT NULL,
  compliance_notes TEXT NOT NULL,
  data_residency_notes TEXT NOT NULL,
  rollout_hypothesis TEXT NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  rollback_note VARCHAR(1000) NOT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  child_spec VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_regional_expansion_region_status (region_code, decision_status),
  INDEX idx_regional_expansion_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 2: Implement the evidence gate**

Create `RegionalExpansionEvidenceService`:

```java
package org.chovy.canvas.strategy.globalization;

public class RegionalExpansionEvidenceService {

    private final EvidenceRepository repository;

    public RegionalExpansionEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.regionCode(), "region code is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.demandEvidence(), "demand evidence is required");
        requireText(request.complianceNotes(), "compliance notes are required");
        requireText(request.dataResidencyNotes(), "data residency notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.regionCode(), request.ownerId(), request.demandEvidence(),
                request.localeCurrencyNotes(), request.timezoneNotes(), request.channelNotes(),
                request.complianceNotes(), request.dataResidencyNotes(), request.rolloutHypothesis(),
                request.proofCommand(), request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String regionCode, String reviewerId, String childSpec) {
        requireText(regionCode, "region code is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(regionCode, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String regionCode,
            String ownerId,
            String demandEvidence,
            String localeCurrencyNotes,
            String timezoneNotes,
            String channelNotes,
            String complianceNotes,
            String dataResidencyNotes,
            String rolloutHypothesis,
            String proofCommand,
            String rollbackNote) {}

    public record EvidenceRecord(
            String regionCode,
            String ownerId,
            String demandEvidence,
            String localeCurrencyNotes,
            String timezoneNotes,
            String channelNotes,
            String complianceNotes,
            String dataResidencyNotes,
            String rolloutHypothesis,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {}

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);
        void approve(String regionCode, String reviewerId, String childSpec);
    }
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RegionalExpansionEvidenceServiceTest
```

Expected: PASS for migration shape and service gate behavior.

### Task 3: Review Gate Extension

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceServiceTest.java`

- [x] **Step 1: Add approval test**

Add a test that proves approval needs reviewer and child spec:

```java
@Test
void approvalRequiresReviewerAndChildSpec() {
    RegionalExpansionEvidenceService.EvidenceRepository repository = mock(RegionalExpansionEvidenceService.EvidenceRepository.class);
    RegionalExpansionEvidenceService service = new RegionalExpansionEvidenceService(repository);

    assertThatThrownBy(() -> service.approve("JP", "", "docs/product-evolution/specs/p3-009a-japan-pilot.md"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reviewer is required");

    assertThatThrownBy(() -> service.approve("JP", "architect-1", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("child spec is required");

    service.approve("JP", "architect-1", "docs/product-evolution/specs/p3-009a-japan-pilot.md");

    verify(repository).approve("JP", "architect-1", "docs/product-evolution/specs/p3-009a-japan-pilot.md");
}
```

- [x] **Step 2: Implement approval method**

Add this method to `RegionalExpansionEvidenceService` and add the matching method to `EvidenceRepository`:

```java
public void approve(String regionCode, String reviewerId, String childSpec) {
    requireText(regionCode, "region code is required");
    requireText(reviewerId, "reviewer is required");
    requireText(childSpec, "child spec is required");
    repository.approve(regionCode, reviewerId, childSpec);
}

public interface EvidenceRepository {
    void insert(EvidenceRecord record);
    void approve(String regionCode, String reviewerId, String childSpec);
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RegionalExpansionEvidenceServiceTest
```

Expected: PASS with registration and approval gate coverage.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-009-globalization-and-regional-expansion.md`
- Modify: `docs/product-evolution/plans/p3-009-globalization-and-regional-expansion-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RegionalExpansionEvidenceServiceTest
```

Expected: PASS.

- [x] **Step 2: Run migration naming check**

Run:

```bash
test -f backend/canvas-engine/src/main/resources/db/migration/V181__regional_expansion_evidence.sql
```

Expected: command exits 0.

- [x] **Step 3: Rollout notes**

Rollout: run `V181__regional_expansion_evidence.sql`, then allow internal strategy owners to register region evidence through the service or admin tooling. Keep all regions blocked until reviewed. Rollback: stop calling the registry writer or hide the admin entry point; the table is additive and no runtime globalization path depends on it.

Commit boundary: no commit was created in this docs-only audit; commit and merge status remains unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V181__regional_expansion_evidence.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceServiceTest.java \
  docs/product-evolution/specs/p3-009-globalization-and-regional-expansion.md \
  docs/product-evolution/plans/p3-009-globalization-and-regional-expansion-plan.md
git commit -m "docs: add regional expansion evidence gate"
```

Expected: commit contains only the regional expansion evidence migration, service, test, spec, and plan.
