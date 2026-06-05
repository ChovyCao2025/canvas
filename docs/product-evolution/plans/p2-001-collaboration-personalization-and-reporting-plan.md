# Collaboration Personalization And Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first collaboration workflow: tenant-scoped editor preferences plus a canvas collaboration summary that the editor can render.

**Architecture:** Store per-user editor preferences in an additive table and expose a thin controller for reading/upserting them. Keep collaboration summary read-only in this slice by returning lock, presence, comment, and notification counters from a dedicated service; deeper comments, share links, reporting, and template market work stay in child specs.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-001-collaboration-personalization-and-reporting.md`
- Source item: `docs/product-evolution/todo/p2/collaboration-personalization-and-reporting.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V160__collaboration_personalization_reporting.sql` - user preference table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceService.java` - tenant/user-scoped preference read and upsert logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/CanvasCollaborationSummaryService.java` - read-only collaboration summary DTOs and aggregation boundary.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasCollaborationController.java` - `/canvas/{canvasId}/collaboration/summary` and `/canvas/preferences/editor`.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java`

**Frontend**
- Create: `frontend/src/services/collaborationApi.ts` - typed API wrapper.
- Create: `frontend/src/pages/canvas-editor/collaborationAwareness.ts` - presentation helpers.
- Create: `frontend/src/pages/canvas-editor/collaborationAwareness.test.ts`

### Task 1: Schema And Preference Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V160__collaboration_personalization_reporting.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceServiceTest.java`

- [ ] **Step 1: Write schema and service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceServiceTest.java`:

```java
package org.chovy.canvas.domain.collaboration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserWorkspacePreferenceServiceTest {

    @Test
    void migrationCreatesTenantScopedPreferenceTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V160__collaboration_personalization_reporting.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS user_workspace_preference")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("user_id VARCHAR(128) NOT NULL")
                .contains("preference_key VARCHAR(128) NOT NULL")
                .contains("preference_json JSON NOT NULL")
                .contains("UNIQUE KEY uk_user_workspace_preference");
    }

    @Test
    void getEditorPreferenceReturnsStoredValueForTenantAndUser() {
        UserWorkspacePreferenceService.PreferenceRepository repository = mock(UserWorkspacePreferenceService.PreferenceRepository.class);
        UserWorkspacePreferenceService service = new UserWorkspacePreferenceService(repository);
        when(repository.find(8L, "operator-1", "canvas-editor")).thenReturn(Optional.of(
                new UserWorkspacePreferenceService.Preference("canvas-editor", Map.of("theme", "dark", "sidebarCollapsed", true))));

        UserWorkspacePreferenceService.Preference result = service.getEditorPreference(8L, "operator-1");

        assertThat(result.preferenceKey()).isEqualTo("canvas-editor");
        assertThat(result.preferenceJson()).containsEntry("theme", "dark");
        assertThat(result.preferenceJson()).containsEntry("sidebarCollapsed", true);
    }

    @Test
    void getEditorPreferenceReturnsDefaultsWhenMissing() {
        UserWorkspacePreferenceService.PreferenceRepository repository = mock(UserWorkspacePreferenceService.PreferenceRepository.class);
        UserWorkspacePreferenceService service = new UserWorkspacePreferenceService(repository);
        when(repository.find(8L, "operator-1", "canvas-editor")).thenReturn(Optional.empty());

        UserWorkspacePreferenceService.Preference result = service.getEditorPreference(8L, "operator-1");

        assertThat(result.preferenceJson()).containsEntry("theme", "system");
        assertThat(result.preferenceJson()).containsEntry("sidebarCollapsed", false);
        assertThat(result.preferenceJson()).containsEntry("notificationLevel", "mentions");
    }

    @Test
    void upsertEditorPreferenceRejectsUnknownKeysAndPersistsAllowedKeys() {
        UserWorkspacePreferenceService.PreferenceRepository repository = mock(UserWorkspacePreferenceService.PreferenceRepository.class);
        UserWorkspacePreferenceService service = new UserWorkspacePreferenceService(repository);

        assertThatThrownBy(() -> service.upsertEditorPreference(8L, "operator-1", Map.of("unsafe", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported preference key unsafe");

        service.upsertEditorPreference(8L, "operator-1", Map.of("theme", "dark", "recentNodeTypes", java.util.List.of("SEND_MESSAGE")));

        verify(repository).upsert(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.userId().equals("operator-1")
                        && saved.preferenceKey().equals("canvas-editor")
                        && saved.preferenceJson().containsKey("recentNodeTypes")));
    }
}
```

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserWorkspacePreferenceServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V160__collaboration_personalization_reporting.sql`:

```sql
CREATE TABLE IF NOT EXISTS user_workspace_preference (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  preference_key VARCHAR(128) NOT NULL,
  preference_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_workspace_preference (tenant_id, user_id, preference_key),
  INDEX idx_user_workspace_preference_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement preference service contract**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceService.java`:

```java
package org.chovy.canvas.domain.collaboration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UserWorkspacePreferenceService {

    private static final String EDITOR_KEY = "canvas-editor";
    private static final Set<String> ALLOWED_EDITOR_KEYS = Set.of(
            "theme", "sidebarCollapsed", "notificationLevel", "recentNodeTypes", "editorLayout", "listDefaults");

    private final PreferenceRepository repository;

    public UserWorkspacePreferenceService(PreferenceRepository repository) {
        this.repository = repository;
    }

    public Preference getEditorPreference(Long tenantId, String userId) {
        return repository.find(tenantId, userId, EDITOR_KEY)
                .orElseGet(() -> new Preference(EDITOR_KEY, defaultEditorPreferences()));
    }

    public Preference upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch) {
        for (String key : patch.keySet()) {
            if (!ALLOWED_EDITOR_KEYS.contains(key)) {
                throw new IllegalArgumentException("unsupported preference key " + key);
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>(getEditorPreference(tenantId, userId).preferenceJson());
        merged.putAll(patch);
        StoredPreference stored = new StoredPreference(tenantId, userId, EDITOR_KEY, merged);
        repository.upsert(stored);
        return new Preference(EDITOR_KEY, merged);
    }

    private static Map<String, Object> defaultEditorPreferences() {
        return Map.of(
                "theme", "system",
                "sidebarCollapsed", false,
                "notificationLevel", "mentions",
                "recentNodeTypes", java.util.List.of(),
                "editorLayout", "default",
                "listDefaults", Map.of("pageSize", 20));
    }

    public record Preference(String preferenceKey, Map<String, Object> preferenceJson) {}

    public record StoredPreference(Long tenantId, String userId, String preferenceKey, Map<String, Object> preferenceJson) {}

    public interface PreferenceRepository {
        Optional<Preference> find(Long tenantId, String userId, String preferenceKey);
        void upsert(StoredPreference preference);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserWorkspacePreferenceServiceTest
```

Expected: PASS.

### Task 2: Collaboration Summary API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/CanvasCollaborationSummaryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasCollaborationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.collaboration.CanvasCollaborationSummaryService;
import org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasControllerCollaborationTest {

    @Test
    void summaryUsesCurrentTenantAndCanvasId() {
        CanvasCollaborationSummaryService summaries = mock(CanvasCollaborationSummaryService.class);
        UserWorkspacePreferenceService preferences = mock(UserWorkspacePreferenceService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        when(summaries.summary(8L, 42L)).thenReturn(new CanvasCollaborationSummaryService.Summary(
                42L,
                List.of(new CanvasCollaborationSummaryService.Presence("operator-1", "Alice", "editing")),
                2,
                3,
                1));

        CanvasCollaborationController controller = new CanvasCollaborationController(summaries, preferences, resolver);

        StepVerifier.create(controller.summary(42L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().canvasId()).isEqualTo(42L);
                    assertThat(response.getData().presence()).hasSize(1);
                    assertThat(response.getData().openCommentCount()).isEqualTo(3);
                })
                .verifyComplete();

        verify(summaries).summary(8L, 42L);
    }

    @Test
    void upsertPreferenceUsesCurrentTenantAndUsername() {
        CanvasCollaborationSummaryService summaries = mock(CanvasCollaborationSummaryService.class);
        UserWorkspacePreferenceService preferences = mock(UserWorkspacePreferenceService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        when(preferences.upsertEditorPreference(8L, "operator-1", Map.of("theme", "dark")))
                .thenReturn(new UserWorkspacePreferenceService.Preference("canvas-editor", Map.of("theme", "dark")));

        CanvasCollaborationController controller = new CanvasCollaborationController(summaries, preferences, resolver);

        StepVerifier.create(controller.upsertEditorPreference(Map.of("theme", "dark")))
                .assertNext(response -> assertThat(response.getData().preferenceJson()).containsEntry("theme", "dark"))
                .verifyComplete();
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasControllerCollaborationTest
```

Expected: FAIL because `CanvasCollaborationController` and `CanvasCollaborationSummaryService` do not exist.

- [ ] **Step 3: Add collaboration summary service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/CanvasCollaborationSummaryService.java`:

```java
package org.chovy.canvas.domain.collaboration;

import java.util.List;

public class CanvasCollaborationSummaryService {

    private final SummaryRepository repository;

    public CanvasCollaborationSummaryService(SummaryRepository repository) {
        this.repository = repository;
    }

    public Summary summary(Long tenantId, Long canvasId) {
        return repository.summary(tenantId, canvasId);
    }

    public record Summary(Long canvasId, List<Presence> presence, int activeLockCount, int openCommentCount, int unreadNotificationCount) {}

    public record Presence(String userId, String displayName, String state) {}

    public interface SummaryRepository {
        Summary summary(Long tenantId, Long canvasId);
    }
}
```

- [ ] **Step 4: Add collaboration controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasCollaborationController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.collaboration.CanvasCollaborationSummaryService;
import org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasCollaborationController {

    private final CanvasCollaborationSummaryService summaryService;
    private final UserWorkspacePreferenceService preferenceService;
    private final TenantContextResolver tenantContextResolver;

    @GetMapping("/{canvasId}/collaboration/summary")
    public Mono<R<CanvasCollaborationSummaryService.Summary>> summary(@PathVariable Long canvasId) {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> summaryService.summary(context.tenantId(), canvasId))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @GetMapping("/preferences/editor")
    public Mono<R<UserWorkspacePreferenceService.Preference>> editorPreference() {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> preferenceService.getEditorPreference(context.tenantId(), context.username()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @PutMapping("/preferences/editor")
    public Mono<R<UserWorkspacePreferenceService.Preference>> upsertEditorPreference(@RequestBody Map<String, Object> patch) {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> upsert(context, patch))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    private UserWorkspacePreferenceService.Preference upsert(TenantContext context, Map<String, Object> patch) {
        return preferenceService.upsertEditorPreference(context.tenantId(), context.username(), patch);
    }
}
```

- [ ] **Step 5: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasControllerCollaborationTest
```

Expected: PASS.

### Task 3: Frontend API And Presentation Helpers

**Files:**
- Create: `frontend/src/services/collaborationApi.ts`
- Create: `frontend/src/pages/canvas-editor/collaborationAwareness.ts`
- Create: `frontend/src/pages/canvas-editor/collaborationAwareness.test.ts`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/canvas-editor/collaborationAwareness.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  collaborationSummaryBadge,
  editorPreferencePatch,
  permissionStateText,
  type CanvasCollaborationSummary,
} from './collaborationAwareness'

describe('collaboration awareness helpers', () => {
  it('formats collaboration counters for editor chrome', () => {
    const summary: CanvasCollaborationSummary = {
      canvasId: 42,
      presence: [{ userId: 'u1', displayName: 'Alice', state: 'editing' }],
      activeLockCount: 2,
      openCommentCount: 3,
      unreadNotificationCount: 1,
    }

    expect(collaborationSummaryBadge(summary)).toEqual({
      presenceText: '1 active',
      lockText: '2 locks',
      commentText: '3 comments',
      notificationText: '1 unread',
    })
  })

  it('keeps only supported editor preference keys in update payloads', () => {
    expect(editorPreferencePatch({
      theme: 'dark',
      sidebarCollapsed: true,
      unsafe: 'ignored',
    })).toEqual({
      theme: 'dark',
      sidebarCollapsed: true,
    })
  })

  it('maps permission states to stable labels', () => {
    expect(permissionStateText('FORBIDDEN')).toBe('No permission')
    expect(permissionStateText('ERROR')).toBe('Unable to load')
    expect(permissionStateText('READY')).toBe('Ready')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- collaborationAwareness.test.ts
```

Expected: FAIL because `collaborationAwareness.ts` does not exist.

- [ ] **Step 3: Add frontend API wrapper**

Create `frontend/src/services/collaborationApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { CanvasCollaborationSummary, EditorPreference } from '../pages/canvas-editor/collaborationAwareness'

export const collaborationApi = {
  summary: (canvasId: number) =>
    http.get<R<CanvasCollaborationSummary>, R<CanvasCollaborationSummary>>(`/canvas/${canvasId}/collaboration/summary`),
  editorPreference: () =>
    http.get<R<EditorPreference>, R<EditorPreference>>('/canvas/preferences/editor'),
  updateEditorPreference: (patch: Partial<EditorPreference['preferenceJson']>) =>
    http.put<R<EditorPreference>, R<EditorPreference>>('/canvas/preferences/editor', patch),
}
```

- [ ] **Step 4: Add presentation helpers**

Create `frontend/src/pages/canvas-editor/collaborationAwareness.ts`:

```ts
export interface CanvasPresence {
  userId: string
  displayName: string
  state: string
}

export interface CanvasCollaborationSummary {
  canvasId: number
  presence: CanvasPresence[]
  activeLockCount: number
  openCommentCount: number
  unreadNotificationCount: number
}

export interface EditorPreference {
  preferenceKey: 'canvas-editor'
  preferenceJson: Record<string, unknown>
}

const allowedPreferenceKeys = new Set([
  'theme',
  'sidebarCollapsed',
  'notificationLevel',
  'recentNodeTypes',
  'editorLayout',
  'listDefaults',
])

export function collaborationSummaryBadge(summary: CanvasCollaborationSummary) {
  return {
    presenceText: `${summary.presence.length} active`,
    lockText: `${summary.activeLockCount} locks`,
    commentText: `${summary.openCommentCount} comments`,
    notificationText: `${summary.unreadNotificationCount} unread`,
  }
}

export function editorPreferencePatch(input: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(input).filter(([key]) => allowedPreferenceKeys.has(key)))
}

export function permissionStateText(state: 'READY' | 'FORBIDDEN' | 'ERROR') {
  if (state === 'FORBIDDEN') return 'No permission'
  if (state === 'ERROR') return 'Unable to load'
  return 'Ready'
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- collaborationAwareness.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-001-collaboration-personalization-and-reporting.md`
- Modify: `docs/product-evolution/plans/p2-001-collaboration-personalization-and-reporting-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V160__collaboration_personalization_reporting.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/CanvasCollaborationSummaryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasCollaborationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java`
- Create: `frontend/src/services/collaborationApi.ts`
- Create: `frontend/src/pages/canvas-editor/collaborationAwareness.ts`
- Create: `frontend/src/pages/canvas-editor/collaborationAwareness.test.ts`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserWorkspacePreferenceServiceTest,CanvasControllerCollaborationTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- collaborationAwareness.test.ts
```

Expected: PASS.

- [ ] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: enable the collaboration summary and editor preference API for tenant operators after `V160__collaboration_personalization_reporting.sql` runs. Rollback: hide the editor collaboration chrome and stop calling `/canvas/preferences/editor`; stored preferences are additive and can remain in place.
```

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V160__collaboration_personalization_reporting.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/CanvasCollaborationSummaryService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasCollaborationController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java \
  frontend/src/services/collaborationApi.ts \
  frontend/src/pages/canvas-editor/collaborationAwareness.ts \
  frontend/src/pages/canvas-editor/collaborationAwareness.test.ts \
  docs/product-evolution/specs/p2-001-collaboration-personalization-and-reporting.md \
  docs/product-evolution/plans/p2-001-collaboration-personalization-and-reporting-plan.md
git commit -m "feat: add collaboration preferences foundation"
```

Expected: commit contains only collaboration preference, summary API, frontend helper, tests, spec, and plan files.
