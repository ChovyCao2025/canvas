# Technical Migration Candidates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an evidence registry for technical migration candidates so runtime migrations stay blocked until proof, rollback, and regression data are recorded.

**Architecture:** Store candidate evidence records in an additive table and expose a service/controller for registering proof commands, baseline results, and rollback commands. This slice does not migrate PowerJob, Disruptor, RocketMQ, audience storage, Spring MVC, script engines, Doris, or Flink.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-004-technical-migration-candidates.md`
- Source item: `docs/product-evolution/todo/p2/technical-migration-candidates.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V163__technical_migration_candidate_metrics.sql` - evidence registry table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateService.java` - proof and rollback gate logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java` - `/architecture/migration-candidates`.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`

**Frontend**
- Create: `frontend/src/services/technicalMigrationApi.ts`
- Create: `frontend/src/pages/technical-migration-candidates/technicalMigrationCandidates.ts`
- Create: `frontend/src/pages/technical-migration-candidates/technical-migration-candidates.test.ts`

### Task 1: Evidence Registry Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V163__technical_migration_candidate_metrics.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`

- [ ] **Step 1: Write evidence registry tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`:

```java
package org.chovy.canvas.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicalMigrationCandidateTest {

    @Test
    void migrationCreatesEvidenceTableWithRollbackFields() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V163__technical_migration_candidate_metrics.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS technical_migration_candidate_evidence")
                .contains("candidate_key VARCHAR(128) NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("baseline_result_json JSON NOT NULL")
                .contains("rollback_command VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerEvidenceRejectsMissingProofOrRollbackCommand() {
        TechnicalMigrationCandidateService.EvidenceRepository repository = mock(TechnicalMigrationCandidateService.EvidenceRepository.class);
        TechnicalMigrationCandidateService service = new TechnicalMigrationCandidateService(repository);

        assertThatThrownBy(() -> service.register(new TechnicalMigrationCandidateService.EvidenceRequest(
                "virtual-thread-executor", "", "{\"p95\":120}", "git revert abc123", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proof command is required");

        assertThatThrownBy(() -> service.register(new TechnicalMigrationCandidateService.EvidenceRequest(
                "virtual-thread-executor", "mvn test", "{\"p95\":120}", "", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rollback command is required");
    }

    @Test
    void registerEvidenceStoresBlockedDecisionUntilReview() {
        TechnicalMigrationCandidateService.EvidenceRepository repository = mock(TechnicalMigrationCandidateService.EvidenceRepository.class);
        TechnicalMigrationCandidateService service = new TechnicalMigrationCandidateService(repository);

        service.register(new TechnicalMigrationCandidateService.EvidenceRequest(
                "rocketmq-topic-split",
                "cd backend && mvn -pl canvas-engine test -Dtest=MqTriggerConsumerTest",
                "{\"baseline\":\"BUILD SUCCESS\"}",
                "restore previous RocketMQ topic config",
                "operator-1"));

        verify(repository).insert(argThat(record ->
                record.candidateKey().equals("rocketmq-topic-split")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.rollbackCommand().contains("restore previous")));
    }

    @Test
    void releaseGateOnlyAllowsReviewedCandidates() {
        TechnicalMigrationCandidateService.EvidenceRepository repository = mock(TechnicalMigrationCandidateService.EvidenceRepository.class);
        TechnicalMigrationCandidateService service = new TechnicalMigrationCandidateService(repository);
        when(repository.latest("spring-mvc-command-dag")).thenReturn(new TechnicalMigrationCandidateService.EvidenceRecord(
                "spring-mvc-command-dag", "mvn test", "{\"baseline\":\"BUILD SUCCESS\"}", "git revert mvc", "BLOCKED_PENDING_REVIEW", "operator-1"));

        assertThat(service.canStartMigration("spring-mvc-command-dag")).isFalse();
    }
}
```

- [ ] **Step 2: Run evidence tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TechnicalMigrationCandidateTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V163__technical_migration_candidate_metrics.sql`:

```sql
CREATE TABLE IF NOT EXISTS technical_migration_candidate_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  candidate_key VARCHAR(128) NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  baseline_result_json JSON NOT NULL,
  rollback_command VARCHAR(1000) NOT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  submitted_by VARCHAR(128) NOT NULL,
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_migration_candidate_latest (candidate_key, created_at),
  INDEX idx_migration_candidate_status (decision_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement evidence service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateService.java`:

```java
package org.chovy.canvas.architecture;

public class TechnicalMigrationCandidateService {

    private final EvidenceRepository repository;

    public TechnicalMigrationCandidateService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        if (request.proofCommand() == null || request.proofCommand().isBlank()) {
            throw new IllegalArgumentException("proof command is required");
        }
        if (request.rollbackCommand() == null || request.rollbackCommand().isBlank()) {
            throw new IllegalArgumentException("rollback command is required");
        }
        EvidenceRecord record = new EvidenceRecord(
                request.candidateKey(),
                request.proofCommand(),
                request.baselineResultJson(),
                request.rollbackCommand(),
                "BLOCKED_PENDING_REVIEW",
                request.submittedBy());
        repository.insert(record);
        return record;
    }

    public boolean canStartMigration(String candidateKey) {
        EvidenceRecord latest = repository.latest(candidateKey);
        return latest != null && "APPROVED_FOR_CHILD_SPEC".equals(latest.decisionStatus());
    }

    public record EvidenceRequest(String candidateKey, String proofCommand, String baselineResultJson, String rollbackCommand, String submittedBy) {}

    public record EvidenceRecord(String candidateKey, String proofCommand, String baselineResultJson, String rollbackCommand, String decisionStatus, String submittedBy) {}

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);
        EvidenceRecord latest(String candidateKey);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TechnicalMigrationCandidateTest
```

Expected: PASS.

### Task 2: Evidence API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java`
- Extend: `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`

- [ ] **Step 1: Add controller test**

Add this test to `TechnicalMigrationCandidateTest`:

```java
@Test
void controllerRegistersEvidenceWithSubmittedByHeader() {
    TechnicalMigrationCandidateService service = mock(TechnicalMigrationCandidateService.class);
    TechnicalMigrationCandidateService.EvidenceRequest request = new TechnicalMigrationCandidateService.EvidenceRequest(
            "powerjob-dynamic-scheduling", "mvn test", "{\"baseline\":\"PASS\"}", "disable PowerJob adapter", "operator-1");
    when(service.register(request)).thenReturn(new TechnicalMigrationCandidateService.EvidenceRecord(
            request.candidateKey(), request.proofCommand(), request.baselineResultJson(), request.rollbackCommand(),
            "BLOCKED_PENDING_REVIEW", "operator-1"));
    org.chovy.canvas.web.TechnicalMigrationCandidateController controller =
            new org.chovy.canvas.web.TechnicalMigrationCandidateController(service);

    reactor.test.StepVerifier.create(controller.register("operator-1", request))
            .assertNext(response -> {
                assertThat(response.getCode()).isEqualTo(0);
                assertThat(response.getData().decisionStatus()).isEqualTo("BLOCKED_PENDING_REVIEW");
            })
            .verifyComplete();
}
```

- [ ] **Step 2: Run controller test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TechnicalMigrationCandidateTest
```

Expected: FAIL because `TechnicalMigrationCandidateController` does not exist.

- [ ] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.architecture.TechnicalMigrationCandidateService;
import org.chovy.canvas.common.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/architecture/migration-candidates")
@RequiredArgsConstructor
public class TechnicalMigrationCandidateController {

    private final TechnicalMigrationCandidateService service;

    @PostMapping("/evidence")
    public Mono<R<TechnicalMigrationCandidateService.EvidenceRecord>> register(
            @RequestHeader(name = "X-Operator", defaultValue = "unknown") String operator,
            @RequestBody TechnicalMigrationCandidateService.EvidenceRequest request) {
        TechnicalMigrationCandidateService.EvidenceRequest enriched = new TechnicalMigrationCandidateService.EvidenceRequest(
                request.candidateKey(),
                request.proofCommand(),
                request.baselineResultJson(),
                request.rollbackCommand(),
                operator);
        return Mono.fromCallable(() -> service.register(enriched))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TechnicalMigrationCandidateTest
```

Expected: PASS.

### Task 3: Frontend Candidate Helpers

**Files:**
- Create: `frontend/src/services/technicalMigrationApi.ts`
- Create: `frontend/src/pages/technical-migration-candidates/technicalMigrationCandidates.ts`
- Create: `frontend/src/pages/technical-migration-candidates/technical-migration-candidates.test.ts`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/technical-migration-candidates/technical-migration-candidates.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { canStartMigrationText, evidencePayload, migrationCandidateLabel } from './technicalMigrationCandidates'

describe('technicalMigrationCandidates', () => {
  it('builds evidence payloads with proof and rollback commands', () => {
    expect(evidencePayload({
      candidateKey: 'rocketmq-topic-split',
      proofCommand: 'mvn test',
      baselineResultJson: '{"baseline":"PASS"}',
      rollbackCommand: 'restore topic config',
    })).toEqual({
      candidateKey: 'rocketmq-topic-split',
      proofCommand: 'mvn test',
      baselineResultJson: '{"baseline":"PASS"}',
      rollbackCommand: 'restore topic config',
      submittedBy: 'frontend',
    })
  })

  it('formats migration candidate labels and gate copy', () => {
    expect(migrationCandidateLabel('spring-mvc-command-dag')).toBe('Spring MVC Command DAG')
    expect(canStartMigrationText(false)).toBe('Blocked until reviewed evidence is approved')
    expect(canStartMigrationText(true)).toBe('Approved for child spec')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- technical-migration-candidates.test.ts
```

Expected: FAIL because `technicalMigrationCandidates.ts` does not exist.

- [ ] **Step 3: Add API wrapper**

Create `frontend/src/services/technicalMigrationApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { TechnicalMigrationEvidencePayload, TechnicalMigrationEvidenceRecord } from '../pages/technical-migration-candidates/technicalMigrationCandidates'

export const technicalMigrationApi = {
  registerEvidence: (payload: TechnicalMigrationEvidencePayload) =>
    http.post<R<TechnicalMigrationEvidenceRecord>, R<TechnicalMigrationEvidenceRecord>>('/architecture/migration-candidates/evidence', payload),
}
```

- [ ] **Step 4: Add presentation helpers**

Create `frontend/src/pages/technical-migration-candidates/technicalMigrationCandidates.ts`:

```ts
export interface TechnicalMigrationEvidencePayload {
  candidateKey: string
  proofCommand: string
  baselineResultJson: string
  rollbackCommand: string
  submittedBy: string
}

export interface TechnicalMigrationEvidenceRecord extends TechnicalMigrationEvidencePayload {
  decisionStatus: 'BLOCKED_PENDING_REVIEW' | 'APPROVED_FOR_CHILD_SPEC' | 'REJECTED'
}

export function evidencePayload(input: Omit<TechnicalMigrationEvidencePayload, 'submittedBy'>): TechnicalMigrationEvidencePayload {
  return { ...input, submittedBy: 'frontend' }
}

export function migrationCandidateLabel(candidateKey: string) {
  return candidateKey
    .split('-')
    .map(part => part.toUpperCase() === 'MVC' ? 'MVC' : part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function canStartMigrationText(canStart: boolean) {
  return canStart ? 'Approved for child spec' : 'Blocked until reviewed evidence is approved'
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- technical-migration-candidates.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-004-technical-migration-candidates.md`
- Modify: `docs/product-evolution/plans/p2-004-technical-migration-candidates-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V163__technical_migration_candidate_metrics.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`
- Create: `frontend/src/services/technicalMigrationApi.ts`
- Create: `frontend/src/pages/technical-migration-candidates/technicalMigrationCandidates.ts`
- Create: `frontend/src/pages/technical-migration-candidates/technical-migration-candidates.test.ts`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TechnicalMigrationCandidateTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- technical-migration-candidates.test.ts
```

Expected: PASS.

- [ ] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V163__technical_migration_candidate_metrics.sql`, then allow architects to register migration evidence. Runtime migration remains blocked until a reviewed evidence row reaches `APPROVED_FOR_CHILD_SPEC`. Rollback: hide the evidence entry point; no runtime code path depends on the table.
```

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V163__technical_migration_candidate_metrics.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java \
  frontend/src/services/technicalMigrationApi.ts \
  frontend/src/pages/technical-migration-candidates/technicalMigrationCandidates.ts \
  frontend/src/pages/technical-migration-candidates/technical-migration-candidates.test.ts \
  docs/product-evolution/specs/p2-004-technical-migration-candidates.md \
  docs/product-evolution/plans/p2-004-technical-migration-candidates-plan.md
git commit -m "feat: add technical migration evidence registry"
```

Expected: commit contains only migration evidence registry, API, frontend helpers, tests, spec, and plan files.
