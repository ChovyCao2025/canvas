# Platform Product Evolution Workstreams Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert broad platform strategy into a tenant-safe workstream registry and home-page command center summary.

**Architecture:** Add an additive platform workstream table and a service that exposes workstream metadata, dependency state, and child-spec readiness. The implementation is a governance workflow only; platformization, data, channel, operations, knowledge, and integration execution remain blocked until their child specs exist.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-003-platform-product-evolution-workstreams.md`
- Source item: `docs/product-evolution/todo/p2/platform-product-evolution-workstreams.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V162__platform_product_evolution_workstreams.sql` - platform workstream metadata table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/PlatformWorkstreamService.java` - workstream list and child-spec readiness logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformWorkstreamController.java` - `/platform/workstreams`.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java`

**Frontend**
- Create: `frontend/src/services/platformWorkstreamApi.ts`
- Create: `frontend/src/pages/home/platformCommandCenter.ts`
- Create: `frontend/src/pages/home/platformCommandCenter.test.ts`

### Task 1: Workstream Registry

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V162__platform_product_evolution_workstreams.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/PlatformWorkstreamService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java`

- [ ] **Step 1: Write workstream contract tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java`:

```java
package org.chovy.canvas.platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformWorkstreamContractTest {

    @Test
    void migrationCreatesWorkstreamTableWithChildSpecGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V162__platform_product_evolution_workstreams.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS platform_workstream")
                .contains("workstream_key VARCHAR(128) NOT NULL")
                .contains("child_spec_path VARCHAR(255) NULL")
                .contains("requires_child_spec TINYINT NOT NULL DEFAULT 1")
                .contains("UNIQUE KEY uk_platform_workstream_key");
    }

    @Test
    void listMarksWorkstreamsWithoutChildSpecsAsBlocked() {
        PlatformWorkstreamService.WorkstreamRepository repository = mock(PlatformWorkstreamService.WorkstreamRepository.class);
        PlatformWorkstreamService service = new PlatformWorkstreamService(repository);
        when(repository.list()).thenReturn(List.of(
                new PlatformWorkstreamService.Workstream("platformization", "Platformization", "P2", true, null, "Extension points"),
                new PlatformWorkstreamService.Workstream("data-assets", "Data Assets", "P2", true, "docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md", "Event pipeline")));

        List<PlatformWorkstreamService.WorkstreamStatus> result = service.statuses();

        assertThat(result).extracting(PlatformWorkstreamService.WorkstreamStatus::status)
                .containsExactly("BLOCKED_CHILD_SPEC_REQUIRED", "READY_FOR_CHILD_EXECUTION");
    }

    @Test
    void requireExecutableChildSpecRejectsBroadWorkstreamWithoutSpecPath() {
        PlatformWorkstreamService.WorkstreamRepository repository = mock(PlatformWorkstreamService.WorkstreamRepository.class);
        PlatformWorkstreamService service = new PlatformWorkstreamService(repository);
        when(repository.get("channels")).thenReturn(new PlatformWorkstreamService.Workstream(
                "channels", "Channels", "P2", true, null, "WeCom and adapters"));

        assertThatThrownBy(() -> service.requireExecutableChildSpec("channels"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("channels requires a child spec before implementation");
    }
}
```

- [ ] **Step 2: Run contract tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PlatformWorkstreamContractTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V162__platform_product_evolution_workstreams.sql`:

```sql
CREATE TABLE IF NOT EXISTS platform_workstream (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workstream_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  priority VARCHAR(16) NOT NULL,
  requires_child_spec TINYINT NOT NULL DEFAULT 1,
  child_spec_path VARCHAR(255) NULL,
  summary VARCHAR(1000) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DISCOVERY',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_platform_workstream_key (workstream_key),
  INDEX idx_platform_workstream_status (priority, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO platform_workstream(workstream_key, display_name, priority, requires_child_spec, child_spec_path, summary, status)
VALUES
  ('platformization', 'Platformization', 'P2', 1, NULL, 'Extension points, developer portal basics, API keys, outbound webhooks, and schema improvements.', 'DISCOVERY'),
  ('data-assets', 'Data Assets', 'P2', 1, 'docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md', 'Data quality, data catalog, path analytics, reports, and event pipeline foundations.', 'READY'),
  ('channels', 'Channels', 'P2', 1, NULL, 'WeCom L1/L2, adapter abstraction, and channel cost/receipt tracking.', 'DISCOVERY'),
  ('operations', 'Operations', 'P2', 1, NULL, 'Approval expansion, audit timeline, command dashboard, and alert rules.', 'DISCOVERY'),
  ('knowledge', 'Knowledge', 'P2', 1, NULL, 'Template market, best-practice library, contextual help, and playbooks.', 'DISCOVERY'),
  ('integrations', 'Integrations', 'P2', 1, NULL, 'Inbound webhook, API key management, SSO/OIDC decision, and data source improvements.', 'DISCOVERY');
```

- [ ] **Step 4: Implement service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/PlatformWorkstreamService.java`:

```java
package org.chovy.canvas.platform;

import java.util.List;

public class PlatformWorkstreamService {

    private final WorkstreamRepository repository;

    public PlatformWorkstreamService(WorkstreamRepository repository) {
        this.repository = repository;
    }

    public List<WorkstreamStatus> statuses() {
        return repository.list().stream().map(this::toStatus).toList();
    }

    public Workstream requireExecutableChildSpec(String workstreamKey) {
        Workstream workstream = repository.get(workstreamKey);
        if (workstream.requiresChildSpec() && (workstream.childSpecPath() == null || workstream.childSpecPath().isBlank())) {
            throw new IllegalStateException(workstreamKey + " requires a child spec before implementation");
        }
        return workstream;
    }

    private WorkstreamStatus toStatus(Workstream workstream) {
        String status = workstream.requiresChildSpec() && (workstream.childSpecPath() == null || workstream.childSpecPath().isBlank())
                ? "BLOCKED_CHILD_SPEC_REQUIRED"
                : "READY_FOR_CHILD_EXECUTION";
        return new WorkstreamStatus(workstream.workstreamKey(), workstream.displayName(), workstream.priority(), status, workstream.childSpecPath(), workstream.summary());
    }

    public record Workstream(String workstreamKey, String displayName, String priority, boolean requiresChildSpec, String childSpecPath, String summary) {}

    public record WorkstreamStatus(String workstreamKey, String displayName, String priority, String status, String childSpecPath, String summary) {}

    public interface WorkstreamRepository {
        List<Workstream> list();
        Workstream get(String workstreamKey);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PlatformWorkstreamContractTest
```

Expected: PASS.

### Task 2: Workstream API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformWorkstreamController.java`
- Extend: `backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java`

- [ ] **Step 1: Add controller test**

Add this test to `PlatformWorkstreamContractTest`:

```java
@Test
void controllerReturnsWorkstreamStatuses() {
    PlatformWorkstreamService service = mock(PlatformWorkstreamService.class);
    when(service.statuses()).thenReturn(List.of(new PlatformWorkstreamService.WorkstreamStatus(
            "data-assets", "Data Assets", "P2", "READY_FOR_CHILD_EXECUTION",
            "docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md", "Event pipeline")));
    org.chovy.canvas.web.PlatformWorkstreamController controller = new org.chovy.canvas.web.PlatformWorkstreamController(service);

    reactor.test.StepVerifier.create(controller.list())
            .assertNext(response -> {
                assertThat(response.getCode()).isEqualTo(0);
                assertThat(response.getData()).hasSize(1);
                assertThat(response.getData().get(0).status()).isEqualTo("READY_FOR_CHILD_EXECUTION");
            })
            .verifyComplete();
}
```

- [ ] **Step 2: Run controller test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PlatformWorkstreamContractTest
```

Expected: FAIL because `PlatformWorkstreamController` does not exist.

- [ ] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformWorkstreamController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.platform.PlatformWorkstreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/platform/workstreams")
@RequiredArgsConstructor
public class PlatformWorkstreamController {

    private final PlatformWorkstreamService service;

    @GetMapping
    public Mono<R<List<PlatformWorkstreamService.WorkstreamStatus>>> list() {
        return Mono.fromCallable(service::statuses)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
```

- [ ] **Step 4: Run controller test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PlatformWorkstreamContractTest
```

Expected: PASS.

### Task 3: Home Command Center Helpers

**Files:**
- Create: `frontend/src/services/platformWorkstreamApi.ts`
- Create: `frontend/src/pages/home/platformCommandCenter.ts`
- Create: `frontend/src/pages/home/platformCommandCenter.test.ts`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/home/platformCommandCenter.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { blockedWorkstreamCount, groupWorkstreamsByStatus, workstreamStatusText, type PlatformWorkstreamStatus } from './platformCommandCenter'

describe('platformCommandCenter', () => {
  const rows: PlatformWorkstreamStatus[] = [
    { workstreamKey: 'platformization', displayName: 'Platformization', priority: 'P2', status: 'BLOCKED_CHILD_SPEC_REQUIRED', childSpecPath: null, summary: 'Extension points' },
    { workstreamKey: 'data-assets', displayName: 'Data Assets', priority: 'P2', status: 'READY_FOR_CHILD_EXECUTION', childSpecPath: 'docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md', summary: 'Events' },
  ]

  it('counts blocked workstreams', () => {
    expect(blockedWorkstreamCount(rows)).toBe(1)
  })

  it('groups workstreams by status', () => {
    expect(groupWorkstreamsByStatus(rows).BLOCKED_CHILD_SPEC_REQUIRED).toHaveLength(1)
    expect(groupWorkstreamsByStatus(rows).READY_FOR_CHILD_EXECUTION).toHaveLength(1)
  })

  it('formats stable status labels', () => {
    expect(workstreamStatusText('BLOCKED_CHILD_SPEC_REQUIRED')).toBe('Child spec required')
    expect(workstreamStatusText('READY_FOR_CHILD_EXECUTION')).toBe('Ready for child execution')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- platformCommandCenter.test.ts
```

Expected: FAIL because `platformCommandCenter.ts` does not exist.

- [ ] **Step 3: Add API wrapper**

Create `frontend/src/services/platformWorkstreamApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { PlatformWorkstreamStatus } from '../pages/home/platformCommandCenter'

export const platformWorkstreamApi = {
  list: () => http.get<R<PlatformWorkstreamStatus[]>, R<PlatformWorkstreamStatus[]>>('/platform/workstreams'),
}
```

- [ ] **Step 4: Add home helpers**

Create `frontend/src/pages/home/platformCommandCenter.ts`:

```ts
export interface PlatformWorkstreamStatus {
  workstreamKey: string
  displayName: string
  priority: string
  status: 'BLOCKED_CHILD_SPEC_REQUIRED' | 'READY_FOR_CHILD_EXECUTION'
  childSpecPath: string | null
  summary: string
}

export function blockedWorkstreamCount(rows: PlatformWorkstreamStatus[]) {
  return rows.filter(row => row.status === 'BLOCKED_CHILD_SPEC_REQUIRED').length
}

export function groupWorkstreamsByStatus(rows: PlatformWorkstreamStatus[]) {
  return rows.reduce<Record<string, PlatformWorkstreamStatus[]>>((groups, row) => {
    groups[row.status] = [...(groups[row.status] ?? []), row]
    return groups
  }, {})
}

export function workstreamStatusText(status: PlatformWorkstreamStatus['status']) {
  return status === 'BLOCKED_CHILD_SPEC_REQUIRED'
    ? 'Child spec required'
    : 'Ready for child execution'
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- platformCommandCenter.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-003-platform-product-evolution-workstreams.md`
- Modify: `docs/product-evolution/plans/p2-003-platform-product-evolution-workstreams-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V162__platform_product_evolution_workstreams.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/PlatformWorkstreamService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformWorkstreamController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java`
- Create: `frontend/src/services/platformWorkstreamApi.ts`
- Create: `frontend/src/pages/home/platformCommandCenter.ts`
- Create: `frontend/src/pages/home/platformCommandCenter.test.ts`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PlatformWorkstreamContractTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- platformCommandCenter.test.ts
```

Expected: PASS.

- [ ] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V162__platform_product_evolution_workstreams.sql`, then surface `/platform/workstreams` in the home command center. Rollback: hide the command-center panel; table rows are additive governance metadata and do not affect runtime execution.
```

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V162__platform_product_evolution_workstreams.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/PlatformWorkstreamService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformWorkstreamController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java \
  frontend/src/services/platformWorkstreamApi.ts \
  frontend/src/pages/home/platformCommandCenter.ts \
  frontend/src/pages/home/platformCommandCenter.test.ts \
  docs/product-evolution/specs/p2-003-platform-product-evolution-workstreams.md \
  docs/product-evolution/plans/p2-003-platform-product-evolution-workstreams-plan.md
git commit -m "feat: add platform workstream command center"
```

Expected: commit contains only platform workstream registry, command-center helpers, tests, spec, and plan files.
