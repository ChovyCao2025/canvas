# Advanced Privacy And Compliance Evidence Plan

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a privacy and compliance evidence gate so deletion/export, GDPR, CCPA, PIPL, differential privacy, federated learning, trusted execution, and residency work cannot proceed without reviewed evidence.

**Architecture:** Implement an additive evidence registry and domain service that rank and block privacy candidates. This slice does not implement data-subject request execution, privacy computing, residency enforcement, or compliance certification.

**Tech Stack:** Java 21, Spring Boot project layout, Flyway, JUnit 5, AssertJ, Mockito.

---

## Spec Reference

- `docs/product-evolution/specs/p3-010-advanced-privacy-and-compliance.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#advanced-privacy-and-compliance`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V182__privacy_compliance_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceService.java`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java`

**Documentation**
- Modify: `docs/product-evolution/specs/p3-010-advanced-privacy-and-compliance.md`
- Modify: `docs/product-evolution/plans/p3-010-advanced-privacy-and-compliance-plan.md`

**No Frontend**
- No frontend files are part of this slice because privacy action UI would imply runtime behavior. A child spec must define any compliance operator workflow after evidence is accepted.

### Task 1: Evidence Contract Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java`

- [x] **Step 1: Write migration and service tests**

Create `PrivacyComplianceEvidenceServiceTest`:

```java
package org.chovy.canvas.strategy.privacy;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PrivacyComplianceEvidenceServiceTest {

    @Test
    void migrationCreatesPrivacyEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V182__privacy_compliance_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS privacy_compliance_evidence")
                .contains("capability_key VARCHAR(128) NOT NULL")
                .contains("regulation_profile VARCHAR(128) NOT NULL")
                .contains("affected_data_classes TEXT NOT NULL")
                .contains("audit_artifact_notes TEXT NOT NULL")
                .contains("threat_model_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_note VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingDataAuditThreatOrRollbackEvidence() {
        PrivacyComplianceEvidenceService.EvidenceRepository repository = mock(PrivacyComplianceEvidenceService.EvidenceRepository.class);
        PrivacyComplianceEvidenceService service = new PrivacyComplianceEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new PrivacyComplianceEvidenceService.EvidenceRequest(
                "dsr-export", "owner-1", "GDPR", "", "export audit bundle",
                "EU residency", "abuse and replay reviewed", "mvn test", "hide compliance action")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("affected data classes are required");

        assertThatThrownBy(() -> service.register(new PrivacyComplianceEvidenceService.EvidenceRequest(
                "federated-learning", "owner-1", "PIPL", "profile attributes",
                "model training audit", "China residency review", "", "mvn test", "disable experiment")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threat model notes are required");
    }

    @Test
    void registerStoresBlockedDecisionUntilComplianceReview() {
        PrivacyComplianceEvidenceService.EvidenceRepository repository = mock(PrivacyComplianceEvidenceService.EvidenceRepository.class);
        PrivacyComplianceEvidenceService service = new PrivacyComplianceEvidenceService(repository);

        service.register(new PrivacyComplianceEvidenceService.EvidenceRequest(
                "dsr-delete", "owner-1", "GDPR", "cdp user, event log, execution trace",
                "deletion audit bundle required", "EU residency review", "identity and replay threat reviewed",
                "cd backend && mvn -pl canvas-engine test", "disable privacy_compliance.registry.enabled"));

        verify(repository).insert(argThat(record ->
                record.capabilityKey().equals("dsr-delete")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.auditArtifactNotes().contains("audit bundle")));
    }
}
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PrivacyComplianceEvidenceServiceTest
```

Expected: FAIL because the migration and service do not exist.

Actual: FAIL before implementation because the service and migration did not exist.

### Task 2: Migration And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V182__privacy_compliance_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java`

- [x] **Step 1: Add the additive migration**

Create `V182__privacy_compliance_evidence.sql`:

```sql
CREATE TABLE IF NOT EXISTS privacy_compliance_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  capability_key VARCHAR(128) NOT NULL,
  owner_id VARCHAR(128) NOT NULL,
  regulation_profile VARCHAR(128) NOT NULL,
  affected_data_classes TEXT NOT NULL,
  audit_artifact_notes TEXT NOT NULL,
  residency_impact_notes TEXT NOT NULL,
  threat_model_notes TEXT NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  rollback_note VARCHAR(1000) NOT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  child_spec VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_privacy_evidence_capability_status (capability_key, decision_status),
  INDEX idx_privacy_evidence_regulation (regulation_profile, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 2: Implement the evidence gate**

Create `PrivacyComplianceEvidenceService`:

```java
package org.chovy.canvas.strategy.privacy;

public class PrivacyComplianceEvidenceService {

    private final EvidenceRepository repository;

    public PrivacyComplianceEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.capabilityKey(), "capability key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.regulationProfile(), "regulation profile is required");
        requireText(request.affectedDataClasses(), "affected data classes are required");
        requireText(request.auditArtifactNotes(), "audit artifact notes are required");
        requireText(request.residencyImpactNotes(), "residency impact notes are required");
        requireText(request.threatModelNotes(), "threat model notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.capabilityKey(), request.ownerId(), request.regulationProfile(),
                request.affectedDataClasses(), request.auditArtifactNotes(),
                request.residencyImpactNotes(), request.threatModelNotes(),
                request.proofCommand(), request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String capabilityKey, String reviewerId, String childSpec) {
        requireText(capabilityKey, "capability key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(capabilityKey, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String capabilityKey,
            String ownerId,
            String regulationProfile,
            String affectedDataClasses,
            String auditArtifactNotes,
            String residencyImpactNotes,
            String threatModelNotes,
            String proofCommand,
            String rollbackNote) {}

    public record EvidenceRecord(
            String capabilityKey,
            String ownerId,
            String regulationProfile,
            String affectedDataClasses,
            String auditArtifactNotes,
            String residencyImpactNotes,
            String threatModelNotes,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {}

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);
        void approve(String capabilityKey, String reviewerId, String childSpec);
    }
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PrivacyComplianceEvidenceServiceTest
```

Expected: PASS for migration shape and service gate behavior.

Actual: PASS after adding the migration and service.

### Task 3: Compliance Approval Gate

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java`

- [x] **Step 1: Add approval test**

Add this test:

```java
@Test
void approvalRequiresReviewerAndNamedChildSpec() {
    PrivacyComplianceEvidenceService.EvidenceRepository repository = mock(PrivacyComplianceEvidenceService.EvidenceRepository.class);
    PrivacyComplianceEvidenceService service = new PrivacyComplianceEvidenceService(repository);

    assertThatThrownBy(() -> service.approve("dsr-delete", "", "docs/product-evolution/specs/p3-010a-dsr-delete.md"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reviewer is required");

    assertThatThrownBy(() -> service.approve("dsr-delete", "compliance-1", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("child spec is required");

    service.approve("dsr-delete", "compliance-1", "docs/product-evolution/specs/p3-010a-dsr-delete.md");

    verify(repository).approve("dsr-delete", "compliance-1", "docs/product-evolution/specs/p3-010a-dsr-delete.md");
}
```

- [x] **Step 2: Implement approval method**

Add this method to `PrivacyComplianceEvidenceService` and add the matching method to `EvidenceRepository`:

```java
public void approve(String capabilityKey, String reviewerId, String childSpec) {
    requireText(capabilityKey, "capability key is required");
    requireText(reviewerId, "reviewer is required");
    requireText(childSpec, "child spec is required");
    repository.approve(capabilityKey, reviewerId, childSpec);
}

public interface EvidenceRepository {
    void insert(EvidenceRecord record);
    void approve(String capabilityKey, String reviewerId, String childSpec);
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PrivacyComplianceEvidenceServiceTest
```

Expected: PASS with registration and approval gate coverage.

Actual: PASS with registration and approval gate coverage.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-010-advanced-privacy-and-compliance.md`
- Modify: `docs/product-evolution/plans/p3-010-advanced-privacy-and-compliance-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PrivacyComplianceEvidenceServiceTest
```

Expected: PASS.

Actual: PASS.

- [x] **Step 2: Run migration naming check**

Run:

```bash
test -f backend/canvas-engine/src/main/resources/db/migration/V182__privacy_compliance_evidence.sql
```

Expected: command exits 0.

Actual: command exited 0.

- [x] **Step 3: Rollout notes**

Rollout: run `V182__privacy_compliance_evidence.sql`, then allow compliance owners to register candidate evidence. Keep privacy actions unavailable until a reviewed child spec exists. Rollback: disable evidence registration or hide the admin entry point; no runtime privacy action depends on this additive table.

Commit boundary: no commit was created in this docs-only audit; commit and merge status remains unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V182__privacy_compliance_evidence.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java \
  docs/product-evolution/specs/p3-010-advanced-privacy-and-compliance.md \
  docs/product-evolution/plans/p3-010-advanced-privacy-and-compliance-plan.md
git commit -m "docs: add privacy compliance evidence gate"
```

Expected: commit contains only the privacy evidence migration, service, test, spec, and plan.
