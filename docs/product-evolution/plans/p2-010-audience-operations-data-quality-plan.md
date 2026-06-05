# Audience Operations And Data Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first audience operations workflow: tenant-scoped union/intersection/difference, audience snapshots, and data quality status for audience operators.

**Architecture:** Keep set math and quality aggregation in `AudienceOperationsService` with repository boundaries for audience members, snapshots, and quality checks. Add additive Flyway tables for snapshot lifecycle and quality check results, expose a thin controller, and add frontend helpers/API wrappers that make status and operation results testable without a browser DOM.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-010-audience-operations-data-quality.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#audience-operations-and-data-quality`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V169__audience_operations_data_quality.sql` - snapshot and quality-check tables.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceOperationsService.java` - set operations, snapshot creation, freshness, and quality status.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceOperationsController.java` - `/audience-operations` API.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceOperationsServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceOperationsControllerTest.java`

**Frontend**
- Create: `frontend/src/services/audienceOperationsApi.ts`
- Create: `frontend/src/pages/audience-list/audienceOperations.ts`
- Create: `frontend/src/pages/audience-list/audienceOperations.test.tsx`
- Modify: `frontend/src/pages/audience-list/index.tsx` - connect operation helpers to the existing audience list page after tests pass.
- Modify: `frontend/src/pages/cdp-user-detail/index.tsx` - show snapshot freshness and quality badges after tests pass.

### Task 1: Set Operations And Snapshot Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V169__audience_operations_data_quality.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceOperationsService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceOperationsServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceOperationsServiceTest.java`:

```java
package org.chovy.canvas.domain.audience;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceOperationsServiceTest {

    @Test
    void migrationCreatesSnapshotAndQualityTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V169__audience_operations_data_quality.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS audience_snapshot")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("audience_id BIGINT NOT NULL")
                .contains("member_count BIGINT NOT NULL")
                .contains("CREATE TABLE IF NOT EXISTS audience_quality_check")
                .contains("freshness_status VARCHAR(32) NOT NULL")
                .contains("INDEX idx_audience_quality_tenant_status");
    }

    @Test
    void unionCombinesMembersAcrossTenantAudiences() {
        AudienceOperationsService.AudienceOperationsRepository repository = mock(AudienceOperationsService.AudienceOperationsRepository.class);
        AudienceOperationsService service = new AudienceOperationsService(repository);
        when(repository.members(8L, 101L)).thenReturn(Set.of("u1", "u2"));
        when(repository.members(8L, 102L)).thenReturn(Set.of("u2", "u3"));

        AudienceOperationsService.OperationResult result = service.runSetOperation(8L, new AudienceOperationsService.OperationRequest(
                "UNION", List.of(101L, 102L), "operator-1"));

        assertThat(result.memberIds()).containsExactly("u1", "u2", "u3");
        assertThat(result.memberCount()).isEqualTo(3);
    }

    @Test
    void intersectionKeepsOnlySharedMembers() {
        AudienceOperationsService.AudienceOperationsRepository repository = mock(AudienceOperationsService.AudienceOperationsRepository.class);
        AudienceOperationsService service = new AudienceOperationsService(repository);
        when(repository.members(8L, 101L)).thenReturn(Set.of("u1", "u2", "u3"));
        when(repository.members(8L, 102L)).thenReturn(Set.of("u2", "u3"));

        AudienceOperationsService.OperationResult result = service.runSetOperation(8L, new AudienceOperationsService.OperationRequest(
                "INTERSECTION", List.of(101L, 102L), "operator-1"));

        assertThat(result.memberIds()).containsExactly("u2", "u3");
    }

    @Test
    void differenceRemovesMembersFromFollowingAudiences() {
        AudienceOperationsService.AudienceOperationsRepository repository = mock(AudienceOperationsService.AudienceOperationsRepository.class);
        AudienceOperationsService service = new AudienceOperationsService(repository);
        when(repository.members(8L, 101L)).thenReturn(Set.of("u1", "u2", "u3"));
        when(repository.members(8L, 102L)).thenReturn(Set.of("u2"));

        AudienceOperationsService.OperationResult result = service.runSetOperation(8L, new AudienceOperationsService.OperationRequest(
                "DIFFERENCE", List.of(101L, 102L), "operator-1"));

        assertThat(result.memberIds()).containsExactly("u1", "u3");
    }

    @Test
    void operationRequiresAtLeastTwoAudiences() {
        AudienceOperationsService service = new AudienceOperationsService(mock(AudienceOperationsService.AudienceOperationsRepository.class));

        assertThatThrownBy(() -> service.runSetOperation(8L, new AudienceOperationsService.OperationRequest(
                "UNION", List.of(101L), "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two audiences are required");
    }

    @Test
    void createSnapshotStoresMemberCountAndStatus() {
        AudienceOperationsService.AudienceOperationsRepository repository = mock(AudienceOperationsService.AudienceOperationsRepository.class);
        AudienceOperationsService service = new AudienceOperationsService(repository);
        when(repository.members(8L, 101L)).thenReturn(Set.of("u1", "u2"));

        AudienceOperationsService.Snapshot snapshot = service.createSnapshot(8L, 101L, "operator-1");

        assertThat(snapshot.memberCount()).isEqualTo(2);
        verify(repository).insertSnapshot(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.audienceId().equals(101L)
                        && saved.status().equals("ACTIVE")));
    }

    @Test
    void qualityReportReturnsChecksSortedBySeverity() {
        AudienceOperationsService.AudienceOperationsRepository repository = mock(AudienceOperationsService.AudienceOperationsRepository.class);
        AudienceOperationsService service = new AudienceOperationsService(repository);
        when(repository.qualityChecks(8L)).thenReturn(List.of(
                new AudienceOperationsService.QualityCheck(8L, 101L, "freshness", "OK", "data refreshed recently", Instant.parse("2026-06-03T00:00:00Z")),
                new AudienceOperationsService.QualityCheck(8L, 102L, "identity_match", "WARN", "identity match rate below target", Instant.parse("2026-06-03T00:00:00Z"))));

        List<AudienceOperationsService.QualityCheck> checks = service.qualityReport(8L);

        assertThat(checks).extracting(AudienceOperationsService.QualityCheck::freshnessStatus)
                .containsExactly("WARN", "OK");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceOperationsServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add audience operations migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V169__audience_operations_data_quality.sql`:

```sql
CREATE TABLE IF NOT EXISTS audience_snapshot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  audience_id BIGINT NOT NULL,
  snapshot_key VARCHAR(128) NOT NULL,
  member_count BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_audience_snapshot_key (tenant_id, snapshot_key),
  INDEX idx_audience_snapshot_audience (tenant_id, audience_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audience_quality_check (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  audience_id BIGINT NOT NULL,
  check_key VARCHAR(128) NOT NULL,
  freshness_status VARCHAR(32) NOT NULL,
  message VARCHAR(512) NOT NULL,
  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_audience_quality_check (tenant_id, audience_id, check_key),
  INDEX idx_audience_quality_tenant_status (tenant_id, freshness_status, checked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement audience operations service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceOperationsService.java`:

```java
package org.chovy.canvas.domain.audience;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AudienceOperationsService {

    private final AudienceOperationsRepository repository;

    public AudienceOperationsService(AudienceOperationsRepository repository) {
        this.repository = repository;
    }

    public OperationResult runSetOperation(Long tenantId, OperationRequest request) {
        if (request.audienceIds().size() < 2) {
            throw new IllegalArgumentException("at least two audiences are required");
        }
        List<Set<String>> memberSets = request.audienceIds().stream()
                .map(audienceId -> repository.members(tenantId, audienceId))
                .toList();
        Set<String> result = new LinkedHashSet<>(memberSets.get(0));
        switch (request.operation()) {
            case "UNION" -> memberSets.stream().skip(1).forEach(result::addAll);
            case "INTERSECTION" -> memberSets.stream().skip(1).forEach(result::retainAll);
            case "DIFFERENCE" -> memberSets.stream().skip(1).forEach(result::removeAll);
            default -> throw new IllegalArgumentException("unsupported audience operation " + request.operation());
        }
        List<String> sortedMembers = result.stream().sorted().toList();
        return new OperationResult(request.operation(), request.audienceIds(), sortedMembers, sortedMembers.size());
    }

    public Snapshot createSnapshot(Long tenantId, Long audienceId, String operator) {
        Set<String> members = repository.members(tenantId, audienceId);
        Snapshot snapshot = new Snapshot(
                tenantId,
                audienceId,
                "audience_" + audienceId + "_" + Instant.now().toEpochMilli(),
                members.size(),
                "ACTIVE",
                operator,
                Instant.now());
        repository.insertSnapshot(snapshot);
        return snapshot;
    }

    public List<QualityCheck> qualityReport(Long tenantId) {
        return repository.qualityChecks(tenantId).stream()
                .sorted(Comparator.comparingInt(AudienceOperationsService::severityRank))
                .toList();
    }

    private static int severityRank(QualityCheck check) {
        return switch (check.freshnessStatus()) {
            case "FAIL" -> 0;
            case "WARN" -> 1;
            case "OK" -> 2;
            default -> 3;
        };
    }

    public record OperationRequest(String operation, List<Long> audienceIds, String operator) {}

    public record OperationResult(String operation, List<Long> audienceIds, List<String> memberIds, int memberCount) {}

    public record Snapshot(Long tenantId, Long audienceId, String snapshotKey, long memberCount, String status, String createdBy, Instant createdAt) {}

    public record QualityCheck(Long tenantId, Long audienceId, String checkKey, String freshnessStatus, String message, Instant checkedAt) {}

    public interface AudienceOperationsRepository {
        Set<String> members(Long tenantId, Long audienceId);
        void insertSnapshot(Snapshot snapshot);
        List<QualityCheck> qualityChecks(Long tenantId);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceOperationsServiceTest
```

Expected: PASS.

### Task 2: Audience Operations API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceOperationsController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceOperationsControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceOperationsControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.audience.AudienceOperationsService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceOperationsControllerTest {

    @Test
    void runOperationUsesTenantFromSecurityContext() {
        AudienceOperationsService service = mock(AudienceOperationsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        AudienceOperationsController controller = new AudienceOperationsController(service, resolver);
        AudienceOperationsService.OperationRequest request = new AudienceOperationsService.OperationRequest("UNION", List.of(101L, 102L), "operator-1");
        when(service.runSetOperation(8L, request)).thenReturn(new AudienceOperationsService.OperationResult("UNION", List.of(101L, 102L), List.of("u1", "u2"), 2));

        StepVerifier.create(controller.runOperation(request))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().memberCount()).isEqualTo(2);
                })
                .verifyComplete();

        verify(service).runSetOperation(8L, request);
    }

    @Test
    void createSnapshotUsesCurrentUsername() {
        AudienceOperationsService service = mock(AudienceOperationsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.TENANT_ADMIN, "admin-1")));
        AudienceOperationsController controller = new AudienceOperationsController(service, resolver);
        when(service.createSnapshot(8L, 101L, "admin-1")).thenReturn(new AudienceOperationsService.Snapshot(
                8L, 101L, "audience_101_1", 500L, "ACTIVE", "admin-1", Instant.parse("2026-06-03T00:00:00Z")));

        StepVerifier.create(controller.createSnapshot(101L))
                .assertNext(response -> assertThat(response.getData().memberCount()).isEqualTo(500L))
                .verifyComplete();
    }

    @Test
    void qualityReportRejectsMissingTenant() {
        AudienceOperationsService service = mock(AudienceOperationsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.empty());
        AudienceOperationsController controller = new AudienceOperationsController(service, resolver);

        StepVerifier.create(controller.qualityReport())
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("tenant context required"))
                .verify();
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceOperationsControllerTest
```

Expected: FAIL because `AudienceOperationsController` does not exist.

- [ ] **Step 3: Implement controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceOperationsController.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.audience.AudienceOperationsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/audience-operations")
public class AudienceOperationsController {

    private static final Set<String> AUDIENCE_OPERATOR_ROLES = Set.of(
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN,
            RoleNames.OPERATOR);

    private final AudienceOperationsService service;
    private final TenantContextResolver tenantContextResolver;

    public AudienceOperationsController(AudienceOperationsService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/set-operations")
    public Mono<R<AudienceOperationsService.OperationResult>> runOperation(@RequestBody AudienceOperationsService.OperationRequest request) {
        return currentTenant().map(ctx -> R.ok(service.runSetOperation(ctx.tenantId(), request)));
    }

    @PostMapping("/audiences/{audienceId}/snapshots")
    public Mono<R<AudienceOperationsService.Snapshot>> createSnapshot(@PathVariable Long audienceId) {
        return currentTenant().map(ctx -> R.ok(service.createSnapshot(ctx.tenantId(), audienceId, ctx.username())));
    }

    @GetMapping("/quality")
    public Mono<R<List<AudienceOperationsService.QualityCheck>>> qualityReport() {
        return currentTenant().map(ctx -> R.ok(service.qualityReport(ctx.tenantId())));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new IllegalStateException("tenant context required")))
                .filter(ctx -> AUDIENCE_OPERATOR_ROLES.contains(ctx.role()))
                .switchIfEmpty(Mono.error(new IllegalStateException("audience operator role required")));
    }
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceOperationsControllerTest
```

Expected: PASS.

### Task 3: Frontend API And Presentation Helpers

**Files:**
- Create: `frontend/src/services/audienceOperationsApi.ts`
- Create: `frontend/src/pages/audience-list/audienceOperations.ts`
- Create: `frontend/src/pages/audience-list/audienceOperations.test.tsx`
- Modify: `frontend/src/pages/audience-list/index.tsx`
- Modify: `frontend/src/pages/cdp-user-detail/index.tsx`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/audience-list/audienceOperations.test.tsx`:

```ts
import { describe, expect, it, vi, type Mock } from 'vitest'
import http from '../../services/api'
import { audienceOperationsApi } from '../../services/audienceOperationsApi'
import {
  buildAudienceOperationSummary,
  canRunAudienceOperation,
  getAudienceQualityBadge,
} from './audienceOperations'

vi.mock('../../services/api', () => ({
  default: {
    post: vi.fn((url: string, body?: unknown) => Promise.resolve({ code: 0, message: 'success', data: { url, body } })),
    get: vi.fn(() => Promise.resolve({ code: 0, message: 'success', data: [] })),
  },
}))

describe('audienceOperationsApi', () => {
  it('posts set operation payloads', async () => {
    await audienceOperationsApi.runOperation({ operation: 'UNION', audienceIds: [101, 102], operator: 'operator-1' })

    expect(http.post as unknown as Mock).toHaveBeenCalledWith('/audience-operations/set-operations', {
      operation: 'UNION',
      audienceIds: [101, 102],
      operator: 'operator-1',
    })
  })

  it('creates snapshots through the audience path', async () => {
    await audienceOperationsApi.createSnapshot(101)

    expect(http.post as unknown as Mock).toHaveBeenCalledWith('/audience-operations/audiences/101/snapshots')
  })
})

describe('audience operation presentation', () => {
  it('requires two selected audiences before running an operation', () => {
    expect(canRunAudienceOperation([101], 'UNION')).toBe(false)
    expect(canRunAudienceOperation([101, 102], 'UNION')).toBe(true)
    expect(canRunAudienceOperation([101, 102], '')).toBe(false)
  })

  it('summarizes member count and source audiences', () => {
    expect(buildAudienceOperationSummary({ operation: 'INTERSECTION', audienceIds: [101, 102], memberCount: 37 }))
      .toBe('INTERSECTION of 2 audiences produced 37 users')
  })

  it('maps quality status to stable badge metadata', () => {
    expect(getAudienceQualityBadge('OK')).toEqual({ tone: 'success', label: 'Fresh' })
    expect(getAudienceQualityBadge('WARN')).toEqual({ tone: 'warning', label: 'Review' })
    expect(getAudienceQualityBadge('FAIL')).toEqual({ tone: 'error', label: 'Stale' })
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- audienceOperations.test.tsx
```

Expected: FAIL because the API wrapper and helper do not exist.

- [ ] **Step 3: Implement frontend API wrapper**

Create `frontend/src/services/audienceOperationsApi.ts`:

```ts
import type { R } from '../types'
import http from './api'

export type AudienceOperation = 'UNION' | 'INTERSECTION' | 'DIFFERENCE'

export interface AudienceOperationRequest {
  operation: AudienceOperation
  audienceIds: number[]
  operator?: string
}

export interface AudienceOperationResult {
  operation: AudienceOperation
  audienceIds: number[]
  memberIds: string[]
  memberCount: number
}

export interface AudienceSnapshot {
  tenantId: number
  audienceId: number
  snapshotKey: string
  memberCount: number
  status: string
  createdBy?: string
  createdAt: string
}

export interface AudienceQualityCheck {
  tenantId: number
  audienceId: number
  checkKey: string
  freshnessStatus: 'OK' | 'WARN' | 'FAIL'
  message: string
  checkedAt: string
}

export const audienceOperationsApi = {
  runOperation: (body: AudienceOperationRequest) =>
    http.post<R<AudienceOperationResult>, R<AudienceOperationResult>>('/audience-operations/set-operations', body),
  createSnapshot: (audienceId: number) =>
    http.post<R<AudienceSnapshot>, R<AudienceSnapshot>>(`/audience-operations/audiences/${audienceId}/snapshots`),
  qualityReport: () =>
    http.get<R<AudienceQualityCheck[]>, R<AudienceQualityCheck[]>>('/audience-operations/quality'),
}
```

- [ ] **Step 4: Implement presentation helpers and connect the page**

Create `frontend/src/pages/audience-list/audienceOperations.ts`:

```ts
import type { AudienceOperation } from '../../services/audienceOperationsApi'

export interface AudienceOperationSummaryInput {
  operation: AudienceOperation
  audienceIds: number[]
  memberCount: number
}

export function canRunAudienceOperation(audienceIds: number[], operation: string): boolean {
  return audienceIds.length >= 2 && operation.trim().length > 0
}

export function buildAudienceOperationSummary(input: AudienceOperationSummaryInput): string {
  return `${input.operation} of ${input.audienceIds.length} audiences produced ${input.memberCount} users`
}

export function getAudienceQualityBadge(status: string): { tone: 'success' | 'warning' | 'error'; label: string } {
  if (status === 'OK') return { tone: 'success', label: 'Fresh' }
  if (status === 'WARN') return { tone: 'warning', label: 'Review' }
  return { tone: 'error', label: 'Stale' }
}
```

Modify `frontend/src/pages/audience-list/index.tsx` by importing these helpers and rendering the returned summary text after the user runs an operation. Modify `frontend/src/pages/cdp-user-detail/index.tsx` by using `getAudienceQualityBadge` for any quality status returned by `audienceOperationsApi.qualityReport()`.

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- audienceOperations.test.tsx
```

Expected: PASS.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-010-audience-operations-data-quality.md`
- Modify: `docs/product-evolution/plans/p2-010-audience-operations-data-quality-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceOperationsServiceTest,AudienceOperationsControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- audienceOperations.test.tsx
```

Expected: PASS.

- [ ] **Step 3: Run broad regression gates**

Run:

```bash
(cd backend && mvn -pl canvas-engine test)
(cd frontend && npm test -- --run)
(cd frontend && npm run build)
```

Expected: PASS for the backend module tests, PASS for Vitest, and PASS for the Vite build.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Use this text in the PR:

```markdown
Rollout notes:
- Feature flag: keep the audience operations controls hidden until `V169__audience_operations_data_quality.sql` is applied and the first quality report returns rows for the tenant.
- Migration: apply `V169__audience_operations_data_quality.sql` before snapshot creation or quality-report reads.
- Tenant and role impact: set operations, snapshots, and quality reads resolve tenant context from JWT claims and reject users outside audience operator roles.
- Manual verification: select two ready audiences, run UNION, create one snapshot, and confirm the quality badge appears on the audience list and CDP user detail surfaces.
- Rollback: hide the audience operations controls; additive snapshot and quality tables can remain in place.
```

- [ ] **Step 5: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V169__audience_operations_data_quality.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceOperationsService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceOperationsController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceOperationsServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceOperationsControllerTest.java \
  frontend/src/services/audienceOperationsApi.ts \
  frontend/src/pages/audience-list/audienceOperations.ts \
  frontend/src/pages/audience-list/audienceOperations.test.tsx \
  frontend/src/pages/audience-list/index.tsx \
  frontend/src/pages/cdp-user-detail/index.tsx \
  docs/product-evolution/specs/p2-010-audience-operations-data-quality.md \
  docs/product-evolution/plans/p2-010-audience-operations-data-quality-plan.md
git commit -m "feat: add audience operations data quality plan"
```

Expected: commit contains only the P2-010 implementation files and the matching spec/plan documentation.
