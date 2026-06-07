# Sandbox Demo And Sales Enablement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Provide a safe sandbox demo workflow with tracked sandbox tenants, reset commands, expiry metadata, and a sales demo guide.

**Architecture:** Store sandbox lifecycle state in an additive table and keep demo installation/reset as explicit service commands with a `demo_marker` so generated data cannot be mistaken for production data. This slice records and resets demo state; full mock data generation and demo canvas templates can be expanded in child specs.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest, Markdown docs.

## Implementation Status

Completed on 2026-06-05.

- Actual migration version is `V269__sandbox_demo_sales_enablement.sql`; the earlier planned migration slot was no longer available in this workspace.
- Backend implementation includes `DemoSandboxService`, `JdbcDemoSandboxRepository`, and authenticated `DemoSandboxController` endpoints for install, reset, and expired-sandbox lookup.
- Reset auditing uses the username from `TenantContextResolver.currentOrError()` instead of a client-supplied operator value.
- Frontend implementation includes `demoSandboxApi`, helper/state formatting, a visible `/demo-sandbox` page, route registration, navigation entry, and route accessibility text.
- Sales enablement material lives in `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md`.
- Commit was intentionally skipped per operator instruction; these changes remain in the working tree.

---

## Spec Reference

- `docs/product-evolution/specs/p2-006-sandbox-demo-sales-enablement.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#sandbox-demo-canvas-and-sales-enablement`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql` - sandbox lifecycle table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java` - install/reset/expiry logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/JdbcDemoSandboxRepository.java` - JDBC persistence for sandbox lifecycle rows.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java` - `/demo-sandboxes` API.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoSandboxServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java`

**Frontend And Docs**
- Create: `frontend/src/services/demoSandboxApi.ts`
- Create: `frontend/src/services/demoSandboxApi.test.ts`
- Create: `frontend/src/pages/demo-sandbox/demoSandbox.ts`
- Create: `frontend/src/pages/demo-sandbox/demoSandbox.test.ts`
- Create: `frontend/src/pages/demo-sandbox/index.tsx`
- Create: `frontend/src/pages/demo-sandbox/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`
- Create: `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md`

### Task 1: Sandbox Lifecycle Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoSandboxServiceTest.java`

- [x] **Step 1: Write lifecycle tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoSandboxServiceTest.java`:

```java
package org.chovy.canvas.domain.demo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoSandboxServiceTest {

    @Test
    void migrationCreatesSandboxLifecycleTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS demo_sandbox")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("demo_marker VARCHAR(128) NOT NULL")
                .contains("expires_at DATETIME NOT NULL")
                .contains("last_reset_at DATETIME NULL")
                .contains("UNIQUE KEY uk_demo_sandbox_tenant");
    }

    @Test
    void installCreatesSandboxWithDemoMarkerAndExpiry() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());

        DemoSandboxService.Sandbox sandbox = service.install(8L, "Retail Lifecycle Demo", 14);

        assertThat(sandbox.demoMarker()).isEqualTo("DEMO_TENANT_8");
        assertThat(sandbox.expiresAt()).isEqualTo(Instant.parse("2026-06-17T00:00:00Z"));
        verify(repository).upsert(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.demoName().equals("Retail Lifecycle Demo")
                        && saved.demoMarker().equals("DEMO_TENANT_8")));
    }

    @Test
    void resetRequiresExistingSandboxAndRecordsResetCommand() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());
        when(repository.get(8L)).thenReturn(new DemoSandboxService.Sandbox(
                8L, "Retail Lifecycle Demo", "DEMO_TENANT_8", "ACTIVE", Instant.parse("2026-06-17T00:00:00Z"), null));

        DemoSandboxService.ResetResult result = service.reset(8L, "operator-1");

        assertThat(result.demoMarker()).isEqualTo("DEMO_TENANT_8");
        assertThat(result.resetAt()).isEqualTo(Instant.parse("2026-06-03T00:00:00Z"));
        verify(repository).recordReset(8L, "operator-1", Instant.parse("2026-06-03T00:00:00Z"));
    }

    @Test
    void resetRejectsMissingSandbox() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());
        when(repository.get(8L)).thenReturn(null);

        assertThatThrownBy(() -> service.reset(8L, "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sandbox tenant 8 is not installed");
    }

    @Test
    void expiredSandboxesAreListedForCleanup() {
        DemoSandboxService.SandboxRepository repository = mock(DemoSandboxService.SandboxRepository.class);
        DemoSandboxService service = new DemoSandboxService(repository, fixedClock());
        when(repository.findExpired(Instant.parse("2026-06-03T00:00:00Z"))).thenReturn(List.of(
                new DemoSandboxService.Sandbox(9L, "Old Demo", "DEMO_TENANT_9", "EXPIRED", Instant.parse("2026-06-01T00:00:00Z"), null)));

        assertThat(service.expired()).extracting(DemoSandboxService.Sandbox::tenantId).containsExactly(9L);
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);
    }
}
```

- [x] **Step 2: Run lifecycle tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DemoSandboxServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [x] **Step 3: Add sandbox migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql`:

```sql
CREATE TABLE IF NOT EXISTS demo_sandbox (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  demo_name VARCHAR(128) NOT NULL,
  demo_marker VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  expires_at DATETIME NOT NULL,
  last_reset_at DATETIME NULL,
  last_reset_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_demo_sandbox_tenant (tenant_id),
  UNIQUE KEY uk_demo_sandbox_marker (demo_marker),
  INDEX idx_demo_sandbox_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 4: Implement sandbox service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java`:

```java
package org.chovy.canvas.domain.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DemoSandboxService {

    private final SandboxRepository repository;
    private final Clock clock;

    public DemoSandboxService(SandboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Sandbox install(Long tenantId, String demoName, int ttlDays) {
        Instant now = clock.instant();
        Sandbox sandbox = new Sandbox(
                tenantId,
                demoName,
                "DEMO_TENANT_" + tenantId,
                "ACTIVE",
                now.plus(ttlDays, ChronoUnit.DAYS),
                null);
        repository.upsert(sandbox);
        return sandbox;
    }

    public ResetResult reset(Long tenantId, String operator) {
        Sandbox sandbox = repository.get(tenantId);
        if (sandbox == null) {
            throw new IllegalStateException("sandbox tenant " + tenantId + " is not installed");
        }
        Instant resetAt = clock.instant();
        repository.recordReset(tenantId, operator, resetAt);
        return new ResetResult(tenantId, sandbox.demoMarker(), resetAt);
    }

    public List<Sandbox> expired() {
        return repository.findExpired(clock.instant());
    }

    public record Sandbox(Long tenantId, String demoName, String demoMarker, String status, Instant expiresAt, Instant lastResetAt) {}

    public record ResetResult(Long tenantId, String demoMarker, Instant resetAt) {}

    public interface SandboxRepository {
        void upsert(Sandbox sandbox);
        Sandbox get(Long tenantId);
        void recordReset(Long tenantId, String operator, Instant resetAt);
        List<Sandbox> findExpired(Instant now);
    }
}
```

- [x] **Step 5: Run lifecycle tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DemoSandboxServiceTest
```

Expected: PASS.

### Task 2: Sandbox API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java`

- [x] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.demo.DemoSandboxService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoSandboxControllerTest {

    @Test
    void installRequiresAuthenticatedContextAndDelegatesToService() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        DemoSandboxController controller = new DemoSandboxController(service, resolver);
        when(service.install(8L, "Retail Demo", 14)).thenReturn(new DemoSandboxService.Sandbox(
                8L, "Retail Demo", "DEMO_TENANT_8", "ACTIVE",
                Instant.parse("2026-06-17T00:00:00Z"), null));

        StepVerifier.create(controller.install(new DemoSandboxController.InstallRequest(8L, "Retail Demo", 14)))
                .assertNext(response -> assertThat(response.getData().demoMarker()).isEqualTo("DEMO_TENANT_8"))
                .verifyComplete();
    }

    @Test
    void resetUsesAuthenticatedOperator() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        DemoSandboxController controller = new DemoSandboxController(service, resolver);
        when(service.reset(8L, "seller-1")).thenReturn(new DemoSandboxService.ResetResult(
                8L, "DEMO_TENANT_8", Instant.parse("2026-06-03T00:00:00Z")));

        StepVerifier.create(controller.reset(8L))
                .assertNext(response -> assertThat(response.getData().resetAt())
                        .isEqualTo(Instant.parse("2026-06-03T00:00:00Z")))
                .verifyComplete();

        verify(service).reset(8L, "seller-1");
    }

    @Test
    void expiredRequiresAuthenticatedContext() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        DemoSandboxController controller = new DemoSandboxController(service, resolver);
        when(service.expired()).thenReturn(List.of(new DemoSandboxService.Sandbox(
                9L, "Old Demo", "DEMO_TENANT_9", "EXPIRED",
                Instant.parse("2026-06-01T00:00:00Z"), null)));

        StepVerifier.create(controller.expired())
                .assertNext(response -> assertThat(response.getData()).hasSize(1))
                .verifyComplete();
    }

    @Test
    void rejectsMissingTenantContext() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
        DemoSandboxController controller = new DemoSandboxController(service, resolver);

        StepVerifier.create(controller.expired())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("AUTH_003"))
                .verify();
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "seller-1");
    }
}
```

- [x] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DemoSandboxControllerTest
```

Expected: FAIL because `DemoSandboxController` does not exist.

- [x] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.demo.DemoSandboxService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/demo-sandboxes")
public class DemoSandboxController {

    private final DemoSandboxService service;
    private final TenantContextResolver tenantContextResolver;

    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<DemoSandboxService.Sandbox>> install(@RequestBody InstallRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.install(request.tenantId(), request.demoName(), request.ttlDays())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/reset")
    public Mono<R<DemoSandboxService.ResetResult>> reset(@PathVariable Long tenantId) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.reset(tenantId, context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/expired")
    public Mono<R<List<DemoSandboxService.Sandbox>>> expired() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.expired()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public record InstallRequest(Long tenantId, String demoName, int ttlDays) {
    }
}
```

- [x] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DemoSandboxControllerTest
```

Expected: PASS.

### Task 3: Frontend Helpers And Sales Guide

**Files:**
- Create: `frontend/src/services/demoSandboxApi.ts`
- Create: `frontend/src/pages/demo-sandbox/demoSandbox.ts`
- Create: `frontend/src/pages/demo-sandbox/demoSandbox.test.ts`
- Create: `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md`

- [x] **Step 1: Write frontend helper tests**

Create `frontend/src/pages/demo-sandbox/demoSandbox.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { demoExpiryText, demoMarkerWarning, resetStateText, type DemoSandbox } from './demoSandbox'

describe('demoSandbox helpers', () => {
  const sandbox: DemoSandbox = {
    tenantId: 8,
    demoName: 'Retail Demo',
    demoMarker: 'DEMO_TENANT_8',
    status: 'ACTIVE',
    expiresAt: '2026-06-17T00:00:00Z',
    lastResetAt: null,
  }

  it('formats expiry and marker warnings', () => {
    expect(demoExpiryText(sandbox)).toBe('Expires at 2026-06-17T00:00:00Z')
    expect(demoMarkerWarning(sandbox)).toBe('Demo data marker: DEMO_TENANT_8')
  })

  it('formats reset state', () => {
    expect(resetStateText({ status: 'IDLE' })).toBe('Ready to reset')
    expect(resetStateText({ status: 'RUNNING' })).toBe('Reset running')
    expect(resetStateText({ status: 'FAILED', message: 'sandbox tenant 8 is not installed' })).toBe('Reset failed: sandbox tenant 8 is not installed')
  })
})
```

- [x] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- demoSandbox.test.ts
```

Expected: FAIL because `demoSandbox.ts` does not exist.

- [x] **Step 3: Add API wrapper**

Create `frontend/src/services/demoSandboxApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { DemoSandbox, DemoSandboxResetResult } from '../pages/demo-sandbox/demoSandbox'

export const demoSandboxApi = {
  install: (payload: { tenantId: number; demoName: string; ttlDays: number }) =>
    http.post<R<DemoSandbox>, R<DemoSandbox>>('/demo-sandboxes', payload),
  reset: (tenantId: number) =>
    http.post<R<DemoSandboxResetResult>, R<DemoSandboxResetResult>>(`/demo-sandboxes/${tenantId}/reset`),
  expired: () =>
    http.get<R<DemoSandbox[]>, R<DemoSandbox[]>>('/demo-sandboxes/expired'),
}
```

- [x] **Step 4: Add frontend helpers**

Create `frontend/src/pages/demo-sandbox/demoSandbox.ts`:

```ts
export interface DemoSandbox {
  tenantId: number
  demoName: string
  demoMarker: string
  status: string
  expiresAt: string
  lastResetAt: string | null
}

export interface DemoSandboxResetResult {
  tenantId: number
  demoMarker: string
  resetAt: string
}

export type ResetState =
  | { status: 'IDLE' }
  | { status: 'RUNNING' }
  | { status: 'FAILED'; message: string }

export function demoExpiryText(sandbox: DemoSandbox) {
  return `Expires at ${sandbox.expiresAt}`
}

export function demoMarkerWarning(sandbox: DemoSandbox) {
  return `Demo data marker: ${sandbox.demoMarker}`
}

export function resetStateText(state: ResetState) {
  if (state.status === 'RUNNING') return 'Reset running'
  if (state.status === 'FAILED') return `Reset failed: ${state.message}`
  return 'Ready to reset'
}
```

- [x] **Step 5: Add sales guide**

Create `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md`:

```markdown
# Sandbox Demo Sales Guide

## Demo Boundary

- Use only tenants with a `DEMO_TENANT_*` marker.
- Reset the sandbox before each customer-facing walkthrough.
- Do not import production customer data into a demo tenant.

## Core Walkthrough

1. Open the demo tenant.
2. Show the prepared lifecycle canvas.
3. Trigger the mock paid-order event.
4. Show the message preview and execution trace.
5. Reset the sandbox after the walkthrough.

## Reset Command

```bash
curl -X POST http://localhost:8080/demo-sandboxes/8/reset -H "Authorization: Bearer <token>"
```

## Rollback

Hide the demo sandbox page and stop calling `/demo-sandboxes`; the lifecycle table is additive and can remain in place for cleanup.
```

- [x] **Step 6: Run frontend tests**

Run:

```bash
cd frontend && npm test -- demoSandbox.test.ts
```

Expected: PASS.

### Task 4: Verification And Rollout Notes

**Files:**
- Modify: `docs/product-evolution/specs/p2-006-sandbox-demo-sales-enablement.md`
- Modify: `docs/product-evolution/plans/p2-006-sandbox-demo-sales-enablement-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/JdbcDemoSandboxRepository.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoSandboxServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java`
- Create: `frontend/src/services/demoSandboxApi.ts`
- Create: `frontend/src/services/demoSandboxApi.test.ts`
- Create: `frontend/src/pages/demo-sandbox/demoSandbox.ts`
- Create: `frontend/src/pages/demo-sandbox/demoSandbox.test.ts`
- Create: `frontend/src/pages/demo-sandbox/index.tsx`
- Create: `frontend/src/pages/demo-sandbox/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`
- Create: `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md`

- [x] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DemoSandboxServiceTest,DemoSandboxControllerTest
```

Expected: PASS.

- [x] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- src/pages/demo-sandbox/demoSandbox.test.ts src/services/demoSandboxApi.test.ts src/pages/demo-sandbox/index.test.tsx src/components/layout/AppLayout.a11y.test.tsx
```

Expected: PASS.

- [x] **Step 3: Verify sales guide sections**

Run:

```bash
rg -n "Demo Boundary|Core Walkthrough|Reset Command|Rollback" docs/product-evolution/runbooks/sandbox-demo-sales-guide.md
```

Expected: output includes all four section headings.

- [x] **Step 4: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V269__sandbox_demo_sales_enablement.sql`, create sandbox lifecycle rows, and expose demo reset only to internal sellers/operators. Rollback: hide the demo sandbox route and keep demo rows marked with `DEMO_TENANT_*` for cleanup.
```

- [x] **Step 5: Commit skipped by operator instruction**

Commit was not created in this session. Keep the working tree changes available for continued product-evolution implementation and create a commit only when explicitly requested.

## Acceptance Checklist

- [x] Additive Flyway migration creates `demo_sandbox` lifecycle storage with marker, expiry, reset audit, and cleanup indexes.
- [x] Backend service installs sandbox tenants, validates TTL/operator inputs, records reset metadata, and lists expired cleanup candidates.
- [x] Backend API requires authenticated tenant context and uses the authenticated username for reset audit.
- [x] Frontend exposes a `/demo-sandbox` operator page with loading, empty, error, install, expired-list, and reset states.
- [x] Sales guide documents demo boundaries, walkthrough, reset command, and rollback path.
- [x] Rollout and rollback notes are captured in the spec and this plan.
- [x] No commit was created because the operator asked to continue without committing.

## Verification Evidence

- Backend red: `mvn -pl canvas-engine test -Dtest=DemoSandboxServiceTest` failed before `DemoSandboxService` and migration existed.
- Backend green: `mvn -pl canvas-engine -Dtest=DemoSandboxServiceTest,DemoSandboxControllerTest test` passed, 10 tests.
- Frontend red: focused Vitest failed before `demoSandboxApi`, page helpers, page component, and route announcement existed.
- Frontend green: `npm run test -- src/pages/demo-sandbox/demoSandbox.test.ts src/services/demoSandboxApi.test.ts src/pages/demo-sandbox/index.test.tsx src/components/layout/AppLayout.a11y.test.tsx` passed, 7 tests.
- Runbook headings: `rg -n "Demo Boundary|Core Walkthrough|Reset Command|Rollback" docs/product-evolution/runbooks/sandbox-demo-sales-guide.md` returned all required sections.
- Frontend build: `npm run build` passed.
