# Editor Productivity Beyond Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first editor productivity slice: user-scoped recent/favorite canvas state, keyboard shortcut and node search helpers, batch operation helpers, and async config validation state.

**Architecture:** Persist editor user state in an additive key/value table and keep productivity logic in small helpers that can be tested without browser automation. Extend `CanvasController` with tenant/user-scoped editor state endpoints, then wire the helpers into the canvas editor and config panel after focused tests pass.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-011-editor-productivity-beyond-baseline.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#editor-productivity-beyond-baseline`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V170__editor_productivity_user_state.sql` - recent/favorite/preference storage.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityService.java` - user state, recent canvas, favorite canvas, and table preference logic.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java` - editor productivity endpoints.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerEditorProductivityTest.java`

**Frontend**
- Create: `frontend/src/pages/canvas-editor/editorProductivity.ts`
- Create: `frontend/src/pages/canvas-editor/editorProductivity.test.tsx`
- Create: `frontend/src/components/config-panel/asyncValidation.ts`
- Create: `frontend/src/components/config-panel/asyncValidation.test.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/components/canvas/CanvasNode.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`

### Task 1: Editor User State Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V170__editor_productivity_user_state.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityServiceTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasEditorProductivityServiceTest {

    @Test
    void migrationCreatesUserScopedEditorStateTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V170__editor_productivity_user_state.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS canvas_editor_user_state")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("username VARCHAR(128) NOT NULL")
                .contains("state_key VARCHAR(128) NOT NULL")
                .contains("state_json JSON NOT NULL")
                .contains("UNIQUE KEY uk_canvas_editor_user_state");
    }

    @Test
    void recordRecentCanvasKeepsNewestCanvasFirst() {
        CanvasEditorProductivityService.EditorStateRepository repository = mock(CanvasEditorProductivityService.EditorStateRepository.class);
        CanvasEditorProductivityService service = new CanvasEditorProductivityService(repository);
        when(repository.getState(8L, "operator-1", "recent_canvases")).thenReturn(new CanvasEditorProductivityService.EditorState(
                8L, "operator-1", "recent_canvases", "[42,7]", Instant.parse("2026-06-03T00:00:00Z")));

        CanvasEditorProductivityService.EditorState state = service.recordRecentCanvas(8L, "operator-1", 7L);

        assertThat(state.stateJson()).isEqualTo("[7,42]");
        verify(repository).upsertState(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.username().equals("operator-1")
                        && saved.stateKey().equals("recent_canvases")));
    }

    @Test
    void toggleFavoriteAddsAndRemovesCanvasIds() {
        CanvasEditorProductivityService.EditorStateRepository repository = mock(CanvasEditorProductivityService.EditorStateRepository.class);
        CanvasEditorProductivityService service = new CanvasEditorProductivityService(repository);
        when(repository.getState(8L, "operator-1", "favorite_canvases")).thenReturn(null);

        CanvasEditorProductivityService.EditorState added = service.toggleFavoriteCanvas(8L, "operator-1", 42L);

        assertThat(added.stateJson()).isEqualTo("[42]");
        when(repository.getState(8L, "operator-1", "favorite_canvases")).thenReturn(added);

        CanvasEditorProductivityService.EditorState removed = service.toggleFavoriteCanvas(8L, "operator-1", 42L);

        assertThat(removed.stateJson()).isEqualTo("[]");
    }

    @Test
    void saveTableColumnsPersistsCompactPreferenceJson() {
        CanvasEditorProductivityService.EditorStateRepository repository = mock(CanvasEditorProductivityService.EditorStateRepository.class);
        CanvasEditorProductivityService service = new CanvasEditorProductivityService(repository);

        CanvasEditorProductivityService.EditorState state = service.saveTableColumns(8L, "operator-1", List.of("name", "status", "updatedAt"));

        assertThat(state.stateKey()).isEqualTo("audience_table_columns");
        assertThat(state.stateJson()).isEqualTo("[\"name\",\"status\",\"updatedAt\"]");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasEditorProductivityServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add editor user state migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V170__editor_productivity_user_state.sql`:

```sql
CREATE TABLE IF NOT EXISTS canvas_editor_user_state (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(128) NOT NULL,
  state_key VARCHAR(128) NOT NULL,
  state_json JSON NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_editor_user_state (tenant_id, username, state_key),
  INDEX idx_canvas_editor_user_state_user (tenant_id, username, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement editor productivity service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityService.java`:

```java
package org.chovy.canvas.domain.canvas;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CanvasEditorProductivityService {

    private final EditorStateRepository repository;

    public CanvasEditorProductivityService(EditorStateRepository repository) {
        this.repository = repository;
    }

    public EditorState recordRecentCanvas(Long tenantId, String username, Long canvasId) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(canvasId);
        ids.addAll(readLongArray(repository.getState(tenantId, username, "recent_canvases")));
        return save(tenantId, username, "recent_canvases", toJson(ids.stream().limit(10).toList()));
    }

    public EditorState toggleFavoriteCanvas(Long tenantId, String username, Long canvasId) {
        List<Long> current = new ArrayList<>(readLongArray(repository.getState(tenantId, username, "favorite_canvases")));
        if (current.contains(canvasId)) {
            current.remove(canvasId);
        } else {
            current.add(canvasId);
        }
        return save(tenantId, username, "favorite_canvases", toJson(current));
    }

    public EditorState saveTableColumns(Long tenantId, String username, List<String> columns) {
        return save(tenantId, username, "audience_table_columns", "[\"" + String.join("\",\"", columns) + "\"]");
    }

    public EditorState state(Long tenantId, String username, String stateKey) {
        EditorState state = repository.getState(tenantId, username, stateKey);
        return state == null ? new EditorState(tenantId, username, stateKey, "[]", Instant.now()) : state;
    }

    private EditorState save(Long tenantId, String username, String stateKey, String stateJson) {
        EditorState state = new EditorState(tenantId, username, stateKey, stateJson, Instant.now());
        repository.upsertState(state);
        return state;
    }

    private List<Long> readLongArray(EditorState state) {
        if (state == null || state.stateJson().equals("[]")) {
            return List.of();
        }
        String trimmed = state.stateJson().replace("[", "").replace("]", "").trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        List<Long> values = new ArrayList<>();
        for (String part : trimmed.split(",")) {
            values.add(Long.valueOf(part.trim()));
        }
        return values;
    }

    private String toJson(List<Long> ids) {
        return "[" + String.join(",", ids.stream().map(String::valueOf).toList()) + "]";
    }

    public record EditorState(Long tenantId, String username, String stateKey, String stateJson, Instant updatedAt) {}

    public interface EditorStateRepository {
        EditorState getState(Long tenantId, String username, String stateKey);
        void upsertState(EditorState state);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasEditorProductivityServiceTest
```

Expected: PASS.

### Task 2: Canvas Controller Editor State Endpoints

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerEditorProductivityTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerEditorProductivityTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.canvas.CanvasEditorProductivityService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasControllerEditorProductivityTest {

    @Test
    void recordRecentCanvasUsesCurrentTenantAndUser() {
        CanvasEditorProductivityService service = mock(CanvasEditorProductivityService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        CanvasController controller = CanvasController.editorProductivityOnly(service, resolver);
        when(service.recordRecentCanvas(8L, "operator-1", 42L)).thenReturn(new CanvasEditorProductivityService.EditorState(
                8L, "operator-1", "recent_canvases", "[42]", Instant.parse("2026-06-03T00:00:00Z")));

        StepVerifier.create(controller.recordRecentCanvas(42L))
                .assertNext(response -> assertThat(response.getData().stateJson()).isEqualTo("[42]"))
                .verifyComplete();

        verify(service).recordRecentCanvas(8L, "operator-1", 42L);
    }

    @Test
    void saveTableColumnsDelegatesPreferenceList() {
        CanvasEditorProductivityService service = mock(CanvasEditorProductivityService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.TENANT_ADMIN, "admin-1")));
        CanvasController controller = CanvasController.editorProductivityOnly(service, resolver);
        CanvasController.TableColumnsReq request = new CanvasController.TableColumnsReq(List.of("name", "status"));
        when(service.saveTableColumns(8L, "admin-1", request.columns())).thenReturn(new CanvasEditorProductivityService.EditorState(
                8L, "admin-1", "audience_table_columns", "[\"name\",\"status\"]", Instant.parse("2026-06-03T00:00:00Z")));

        StepVerifier.create(controller.saveTableColumns(request))
                .assertNext(response -> assertThat(response.getData().stateKey()).isEqualTo("audience_table_columns"))
                .verifyComplete();
    }

    @Test
    void editorStateRejectsMissingTenantContext() {
        CanvasEditorProductivityService service = mock(CanvasEditorProductivityService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.empty());
        CanvasController controller = CanvasController.editorProductivityOnly(service, resolver);

        StepVerifier.create(controller.editorState("recent_canvases"))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("tenant context required"))
                .verify();
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasControllerEditorProductivityTest
```

Expected: FAIL because `CanvasController` does not expose the editor productivity methods.

- [ ] **Step 3: Extend CanvasController**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java` by adding the service dependency, a test-only factory, request record, and four methods:

```java
private final CanvasEditorProductivityService editorProductivityService;
private final TenantContextResolver tenantContextResolver;

public static CanvasController editorProductivityOnly(CanvasEditorProductivityService service, TenantContextResolver resolver) {
    return new CanvasController(null, null, null, null, null, service, resolver);
}

@PostMapping("/{id}/editor/recent")
public Mono<R<CanvasEditorProductivityService.EditorState>> recordRecentCanvas(@PathVariable Long id) {
    return currentEditorUser().map(ctx -> R.ok(editorProductivityService.recordRecentCanvas(ctx.tenantId(), ctx.username(), id)));
}

@PostMapping("/{id}/editor/favorite")
public Mono<R<CanvasEditorProductivityService.EditorState>> toggleFavoriteCanvas(@PathVariable Long id) {
    return currentEditorUser().map(ctx -> R.ok(editorProductivityService.toggleFavoriteCanvas(ctx.tenantId(), ctx.username(), id)));
}

@GetMapping("/editor/state/{stateKey}")
public Mono<R<CanvasEditorProductivityService.EditorState>> editorState(@PathVariable String stateKey) {
    return currentEditorUser().map(ctx -> R.ok(editorProductivityService.state(ctx.tenantId(), ctx.username(), stateKey)));
}

@PutMapping("/editor/table-columns")
public Mono<R<CanvasEditorProductivityService.EditorState>> saveTableColumns(@RequestBody TableColumnsReq req) {
    return currentEditorUser().map(ctx -> R.ok(editorProductivityService.saveTableColumns(ctx.tenantId(), ctx.username(), req.columns())));
}

private Mono<TenantContext> currentEditorUser() {
    return tenantContextResolver.current()
            .switchIfEmpty(Mono.error(new IllegalStateException("tenant context required")));
}

public record TableColumnsReq(List<String> columns) {}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasControllerEditorProductivityTest
```

Expected: PASS.

### Task 3: Editor Productivity Frontend Helpers

**Files:**
- Create: `frontend/src/pages/canvas-editor/editorProductivity.ts`
- Create: `frontend/src/pages/canvas-editor/editorProductivity.test.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/components/canvas/CanvasNode.tsx`

- [ ] **Step 1: Write editor helper tests**

Create `frontend/src/pages/canvas-editor/editorProductivity.test.tsx`:

```ts
import { describe, expect, it } from 'vitest'
import {
  applyBatchOperation,
  buildShortcutLabel,
  searchCanvasNodes,
  type CanvasEditorNode,
} from './editorProductivity'

const nodes: CanvasEditorNode[] = [
  { id: 'start', label: 'Start', type: 'START' },
  { id: 'sms', label: 'Send SMS', type: 'SEND_MESSAGE' },
  { id: 'coupon', label: 'Issue Coupon', type: 'COUPON' },
]

describe('editorProductivity helpers', () => {
  it('searches nodes by label and type', () => {
    expect(searchCanvasNodes(nodes, 'sms')).toEqual([nodes[1]])
    expect(searchCanvasNodes(nodes, 'coupon')).toEqual([nodes[2]])
    expect(searchCanvasNodes(nodes, '')).toEqual(nodes)
  })

  it('builds readable shortcut labels', () => {
    expect(buildShortcutLabel(['meta', 'shift', 'f'])).toBe('Meta + Shift + F')
    expect(buildShortcutLabel(['ctrl', 'd'])).toBe('Ctrl + D')
  })

  it('applies batch delete without mutating unselected nodes', () => {
    expect(applyBatchOperation(nodes, ['sms'], 'DELETE')).toEqual([nodes[0], nodes[2]])
  })

  it('applies batch duplicate with stable copied ids', () => {
    expect(applyBatchOperation(nodes, ['sms'], 'DUPLICATE')).toContainEqual({
      id: 'sms_copy',
      label: 'Send SMS Copy',
      type: 'SEND_MESSAGE',
    })
  })
})
```

- [ ] **Step 2: Run editor helper tests and confirm red state**

Run:

```bash
cd frontend && npm test -- editorProductivity.test.tsx
```

Expected: FAIL because `editorProductivity.ts` does not exist.

- [ ] **Step 3: Implement editor helper module**

Create `frontend/src/pages/canvas-editor/editorProductivity.ts`:

```ts
export interface CanvasEditorNode {
  id: string
  label: string
  type: string
}

export type BatchOperation = 'DELETE' | 'DUPLICATE'

export function searchCanvasNodes(nodes: CanvasEditorNode[], query: string): CanvasEditorNode[] {
  const normalized = query.trim().toLowerCase()
  if (!normalized) return nodes
  return nodes.filter((node) =>
    node.label.toLowerCase().includes(normalized) || node.type.toLowerCase().includes(normalized),
  )
}

export function buildShortcutLabel(keys: string[]): string {
  return keys.map((key) => key.charAt(0).toUpperCase() + key.slice(1)).join(' + ')
}

export function applyBatchOperation(nodes: CanvasEditorNode[], selectedIds: string[], operation: BatchOperation): CanvasEditorNode[] {
  const selected = new Set(selectedIds)
  if (operation === 'DELETE') {
    return nodes.filter((node) => !selected.has(node.id))
  }
  return [
    ...nodes,
    ...nodes
      .filter((node) => selected.has(node.id))
      .map((node) => ({ ...node, id: `${node.id}_copy`, label: `${node.label} Copy` })),
  ]
}
```

Modify `frontend/src/pages/canvas-editor/index.tsx` so the existing editor toolbar can call `searchCanvasNodes`, `buildShortcutLabel`, and `applyBatchOperation`. Modify `frontend/src/components/canvas/CanvasNode.tsx` so duplicated labels and search matches render through existing node title props.

- [ ] **Step 4: Run editor helper tests**

Run:

```bash
cd frontend && npm test -- editorProductivity.test.tsx
```

Expected: PASS.

### Task 4: Config Panel Async Validation

**Files:**
- Create: `frontend/src/components/config-panel/asyncValidation.ts`
- Create: `frontend/src/components/config-panel/asyncValidation.test.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`

- [ ] **Step 1: Write async validation tests**

Create `frontend/src/components/config-panel/asyncValidation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  buildAsyncValidationState,
  getAsyncValidationDelay,
  shouldRunAsyncValidation,
} from './asyncValidation'

describe('async config validation helpers', () => {
  it('runs validation only for dirty fields with a value', () => {
    expect(shouldRunAsyncValidation({ dirty: true, value: 'coupon_code' })).toBe(true)
    expect(shouldRunAsyncValidation({ dirty: true, value: '' })).toBe(false)
    expect(shouldRunAsyncValidation({ dirty: false, value: 'coupon_code' })).toBe(false)
  })

  it('uses a short debounce for normal typing and a longer delay after errors', () => {
    expect(getAsyncValidationDelay(false)).toBe(400)
    expect(getAsyncValidationDelay(true)).toBe(1000)
  })

  it('builds loading, success, and error states', () => {
    expect(buildAsyncValidationState('LOADING')).toEqual({ status: 'validating', message: 'Checking...' })
    expect(buildAsyncValidationState('OK')).toEqual({ status: 'success', message: 'Looks good' })
    expect(buildAsyncValidationState('ERROR', 'Coupon code does not exist')).toEqual({
      status: 'error',
      message: 'Coupon code does not exist',
    })
  })
})
```

- [ ] **Step 2: Run async validation tests and confirm red state**

Run:

```bash
cd frontend && npm test -- asyncValidation.test.ts
```

Expected: FAIL because `asyncValidation.ts` does not exist.

- [ ] **Step 3: Implement async validation helpers**

Create `frontend/src/components/config-panel/asyncValidation.ts`:

```ts
export type AsyncValidationResult = 'LOADING' | 'OK' | 'ERROR'

export interface AsyncValidationInput {
  dirty: boolean
  value: string
}

export function shouldRunAsyncValidation(input: AsyncValidationInput): boolean {
  return input.dirty && input.value.trim().length > 0
}

export function getAsyncValidationDelay(hasRecentError: boolean): number {
  return hasRecentError ? 1000 : 400
}

export function buildAsyncValidationState(result: AsyncValidationResult, errorMessage?: string) {
  if (result === 'LOADING') return { status: 'validating', message: 'Checking...' }
  if (result === 'OK') return { status: 'success', message: 'Looks good' }
  return { status: 'error', message: errorMessage || 'Validation failed' }
}
```

Modify `frontend/src/components/config-panel/index.tsx` so expensive remote field checks call `shouldRunAsyncValidation`, use `getAsyncValidationDelay` for debounce timing, and display `buildAsyncValidationState` output through the existing form item feedback UI.

- [ ] **Step 4: Run async validation tests**

Run:

```bash
cd frontend && npm test -- asyncValidation.test.ts
```

Expected: PASS.

### Task 5: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-011-editor-productivity-beyond-baseline.md`
- Modify: `docs/product-evolution/plans/p2-011-editor-productivity-beyond-baseline-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasEditorProductivityServiceTest,CanvasControllerEditorProductivityTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- editorProductivity.test.tsx asyncValidation.test.ts
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
- Feature flag: keep advanced editor productivity controls hidden until `V170__editor_productivity_user_state.sql` is applied and editor state endpoints pass smoke tests.
- Migration: apply `V170__editor_productivity_user_state.sql` before enabling recent/favorite canvas state or table preferences.
- Tenant and role impact: editor state is scoped by tenant ID and username from JWT claims; no cross-tenant state is read or written.
- Manual verification: open a canvas, record it as recent, favorite it, search for a node, duplicate a selected node, and trigger one async config validation.
- Rollback: hide the editor controls; persisted state can remain because it is additive and only read by the new UI.
```

- [ ] **Step 5: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V170__editor_productivity_user_state.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasEditorProductivityServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerEditorProductivityTest.java \
  frontend/src/pages/canvas-editor/editorProductivity.ts \
  frontend/src/pages/canvas-editor/editorProductivity.test.tsx \
  frontend/src/components/config-panel/asyncValidation.ts \
  frontend/src/components/config-panel/asyncValidation.test.ts \
  frontend/src/pages/canvas-editor/index.tsx \
  frontend/src/components/canvas/CanvasNode.tsx \
  frontend/src/components/config-panel/index.tsx \
  docs/product-evolution/specs/p2-011-editor-productivity-beyond-baseline.md \
  docs/product-evolution/plans/p2-011-editor-productivity-beyond-baseline-plan.md
git commit -m "feat: add editor productivity plan"
```

Expected: commit contains only the P2-011 implementation files and the matching spec/plan documentation.
