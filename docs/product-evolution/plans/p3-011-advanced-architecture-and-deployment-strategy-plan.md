# Advanced Architecture And Deployment Strategy Evidence Plan

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an architecture and deployment evidence gate so service split, event-driven communication, serverless, edge, multi-cloud, and data residency candidates cannot proceed without current-state proof, dependency review, cost notes, and rollback evidence.

**Architecture:** Implement an additive candidate evidence registry and domain service. This slice records decisions only; it does not split services, change messaging topology, deploy serverless or edge workloads, alter cloud strategy, or change data residency behavior.

**Tech Stack:** Java 21, Spring Boot project layout, Flyway, JUnit 5, AssertJ, Mockito.

---

## Spec Reference

- `docs/product-evolution/specs/p3-011-advanced-architecture-and-deployment-strategy.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#advanced-architecture-and-deployment-strategy`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V183__architecture_deployment_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceService.java`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceServiceTest.java`

**Documentation**
- Modify: `docs/product-evolution/specs/p3-011-advanced-architecture-and-deployment-strategy.md`
- Modify: `docs/product-evolution/plans/p3-011-advanced-architecture-and-deployment-strategy-plan.md`

**No Frontend**
- No frontend files are part of this slice because architecture candidates need proof and review before operator surfaces are useful.

### Task 1: Evidence Contract Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceServiceTest.java`

- [x] **Step 1: Write migration and service tests**

Create `ArchitectureDeploymentEvidenceServiceTest`:

```java
package org.chovy.canvas.strategy.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArchitectureDeploymentEvidenceServiceTest {

    @Test
    void migrationCreatesArchitectureDeploymentEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V183__architecture_deployment_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS architecture_deployment_evidence")
                .contains("candidate_key VARCHAR(128) NOT NULL")
                .contains("current_state_evidence TEXT NOT NULL")
                .contains("target_architecture TEXT NOT NULL")
                .contains("scaling_trigger TEXT NOT NULL")
                .contains("dependency_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_plan VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingCurrentStateDependenciesProofOrRollback() {
        ArchitectureDeploymentEvidenceService.EvidenceRepository repository = mock(ArchitectureDeploymentEvidenceService.EvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceService.EvidenceRequest(
                "service-split-canvas-engine", "architect-1", "", "split execution service",
                "4000 concurrent execution baseline", "ops cost reviewed", "depends on P2-018",
                "mvn test", "deploy monolith artifact")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current-state evidence is required");

        assertThatThrownBy(() -> service.register(new ArchitectureDeploymentEvidenceService.EvidenceRequest(
                "edge-decisioning", "architect-1", "current API latency captured", "edge worker pilot",
                "regional p95 latency threshold", "ops cost reviewed", "", "mvn test", "disable edge route")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dependency notes are required");
    }

    @Test
    void registerStoresBlockedDecisionUntilArchitectureReview() {
        ArchitectureDeploymentEvidenceService.EvidenceRepository repository = mock(ArchitectureDeploymentEvidenceService.EvidenceRepository.class);
        ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

        service.register(new ArchitectureDeploymentEvidenceService.EvidenceRequest(
                "event-driven-delivery", "architect-1", "current delivery outbox metrics captured",
                "separate delivery event topic", "queue lag exceeds reviewed threshold",
                "extra broker operations reviewed", "blocked by P0-003 rollout evidence",
                "cd backend && mvn -pl canvas-engine test -Dtest=MqTriggerConsumerTest",
                "restore previous RocketMQ topic config"));

        verify(repository).insert(argThat(record ->
                record.candidateKey().equals("event-driven-delivery")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.dependencyNotes().contains("P0-003")));
    }
}
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ArchitectureDeploymentEvidenceServiceTest
```

Expected: FAIL because the migration and service do not exist.

### Task 2: Migration And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V183__architecture_deployment_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceServiceTest.java`

- [x] **Step 1: Add the additive migration**

Create `V183__architecture_deployment_evidence.sql`:

```sql
CREATE TABLE IF NOT EXISTS architecture_deployment_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  candidate_key VARCHAR(128) NOT NULL,
  owner_id VARCHAR(128) NOT NULL,
  current_state_evidence TEXT NOT NULL,
  target_architecture TEXT NOT NULL,
  scaling_trigger TEXT NOT NULL,
  operational_cost_notes TEXT NOT NULL,
  dependency_notes TEXT NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  rollback_plan VARCHAR(1000) NOT NULL,
  residency_impact_notes TEXT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  child_spec VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_arch_deploy_candidate_status (candidate_key, decision_status),
  INDEX idx_arch_deploy_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 2: Implement the evidence gate**

Create `ArchitectureDeploymentEvidenceService`:

```java
package org.chovy.canvas.strategy.architecture;

public class ArchitectureDeploymentEvidenceService {

    private final EvidenceRepository repository;

    public ArchitectureDeploymentEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.candidateKey(), "candidate key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.currentStateEvidence(), "current-state evidence is required");
        requireText(request.targetArchitecture(), "target architecture is required");
        requireText(request.scalingTrigger(), "scaling trigger is required");
        requireText(request.operationalCostNotes(), "operational cost notes are required");
        requireText(request.dependencyNotes(), "dependency notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackPlan(), "rollback plan is required");

        EvidenceRecord record = new EvidenceRecord(
                request.candidateKey(), request.ownerId(), request.currentStateEvidence(),
                request.targetArchitecture(), request.scalingTrigger(), request.operationalCostNotes(),
                request.dependencyNotes(), request.proofCommand(), request.rollbackPlan(),
                "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String candidateKey, String reviewerId, String childSpec) {
        requireText(candidateKey, "candidate key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(candidateKey, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String candidateKey,
            String ownerId,
            String currentStateEvidence,
            String targetArchitecture,
            String scalingTrigger,
            String operationalCostNotes,
            String dependencyNotes,
            String proofCommand,
            String rollbackPlan) {}

    public record EvidenceRecord(
            String candidateKey,
            String ownerId,
            String currentStateEvidence,
            String targetArchitecture,
            String scalingTrigger,
            String operationalCostNotes,
            String dependencyNotes,
            String proofCommand,
            String rollbackPlan,
            String decisionStatus) {}

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);
        void approve(String candidateKey, String reviewerId, String childSpec);
    }
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ArchitectureDeploymentEvidenceServiceTest
```

Expected: PASS for migration shape and service gate behavior.

### Task 3: Child-Spec Approval Gate

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceServiceTest.java`

- [x] **Step 1: Add approval test**

Add this test:

```java
@Test
void approvalRequiresReviewerAndChildSpec() {
    ArchitectureDeploymentEvidenceService.EvidenceRepository repository = mock(ArchitectureDeploymentEvidenceService.EvidenceRepository.class);
    ArchitectureDeploymentEvidenceService service = new ArchitectureDeploymentEvidenceService(repository);

    assertThatThrownBy(() -> service.approve("service-split-canvas-engine", "", "docs/product-evolution/specs/p3-011a-service-boundary-proof.md"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reviewer is required");

    assertThatThrownBy(() -> service.approve("service-split-canvas-engine", "architect-1", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("child spec is required");

    service.approve("service-split-canvas-engine", "architect-1", "docs/product-evolution/specs/p3-011a-service-boundary-proof.md");

    verify(repository).approve("service-split-canvas-engine", "architect-1", "docs/product-evolution/specs/p3-011a-service-boundary-proof.md");
}
```

- [x] **Step 2: Implement approval method**

Add this method to `ArchitectureDeploymentEvidenceService` and add the matching method to `EvidenceRepository`:

```java
public void approve(String candidateKey, String reviewerId, String childSpec) {
    requireText(candidateKey, "candidate key is required");
    requireText(reviewerId, "reviewer is required");
    requireText(childSpec, "child spec is required");
    repository.approve(candidateKey, reviewerId, childSpec);
}

public interface EvidenceRepository {
    void insert(EvidenceRecord record);
    void approve(String candidateKey, String reviewerId, String childSpec);
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ArchitectureDeploymentEvidenceServiceTest
```

Expected: PASS with registration and approval gate coverage.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-011-advanced-architecture-and-deployment-strategy.md`
- Modify: `docs/product-evolution/plans/p3-011-advanced-architecture-and-deployment-strategy-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ArchitectureDeploymentEvidenceServiceTest
```

Expected: PASS.

- [x] **Step 2: Run migration naming check**

Run:

```bash
test -f backend/canvas-engine/src/main/resources/db/migration/V183__architecture_deployment_evidence.sql
```

Expected: command exits 0.

- [x] **Step 3: Rollout notes**

Rollout: run `V183__architecture_deployment_evidence.sql`, then allow architects to register deployment candidates with current-state proof. Keep runtime architecture unchanged until a reviewed child spec exists. Rollback: disable evidence registration or hide the admin entry point; no runtime deployment path depends on this additive table.

Commit boundary: no commit was created in this docs-only audit; commit and merge status remains unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V183__architecture_deployment_evidence.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceServiceTest.java \
  docs/product-evolution/specs/p3-011-advanced-architecture-and-deployment-strategy.md \
  docs/product-evolution/plans/p3-011-advanced-architecture-and-deployment-strategy-plan.md
git commit -m "docs: add architecture deployment evidence gate"
```

Expected: commit contains only the architecture evidence migration, service, test, spec, and plan.
