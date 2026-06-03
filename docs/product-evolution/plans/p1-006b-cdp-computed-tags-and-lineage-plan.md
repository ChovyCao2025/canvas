# CDP Computed Tags And Lineage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add computed tags with dependency graph validation and lineage impact checks.

**Architecture:** Store computed tag definitions and dependency edges in MySQL, validate cycles before activation, write tag results through existing `CdpTagService`, and scan audience rules plus canvas graph JSON for lineage impact.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, QLExpress/Aviator where already present, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Vitest.

---

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_computed_tags_lineage.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpLineageService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpLineageServiceTest.java`
- Create: `frontend/src/pages/cdp-computed-tags/computedTagPresentation.ts`
- Create: `frontend/src/pages/cdp-computed-tags/computedTagPresentation.test.ts`

### Task 1: Schema And Dependency Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_computed_tags_lineage.sql`

- [ ] **Step 1: Write schema test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagSchemaTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ComputedTagSchemaTest {

    @Test
    void migrationCreatesComputedTagDefinitionDependencyAndRunTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V100__cdp_computed_tags_lineage.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS computed_tag_definition")
                .contains("tenant_id")
                .contains("tag_code")
                .contains("compute_type")
                .contains("expression_json")
                .contains("refresh_mode")
                .contains("UNIQUE KEY uk_computed_tag_definition")
                .contains("CREATE TABLE IF NOT EXISTS computed_tag_dependency")
                .contains("depends_on_tag_code")
                .contains("UNIQUE KEY uk_computed_tag_dependency")
                .contains("CREATE TABLE IF NOT EXISTS computed_tag_run")
                .contains("cycle_path")
                .contains("scanned_count")
                .contains("matched_count")
                .contains("updated_count")
                .contains("skipped_count")
                .contains("failed_count");
    }
}
```

- [ ] **Step 2: Add migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_computed_tags_lineage.sql`:

```sql
CREATE TABLE IF NOT EXISTS computed_tag_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  tag_code VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
  compute_type VARCHAR(32) NOT NULL,
  expression_json JSON NOT NULL,
  refresh_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_computed_tag_definition (tenant_id, tag_code),
  INDEX idx_computed_tag_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS computed_tag_dependency (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  tag_code VARCHAR(128) NOT NULL,
  depends_on_tag_code VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_computed_tag_dependency (tenant_id, tag_code, depends_on_tag_code),
  INDEX idx_computed_tag_dependency_reverse (tenant_id, depends_on_tag_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS computed_tag_run (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  tag_code VARCHAR(128) NOT NULL,
  status VARCHAR(20) NOT NULL,
  cycle_path VARCHAR(1000) NULL,
  scanned_count BIGINT NOT NULL DEFAULT 0,
  matched_count BIGINT NOT NULL DEFAULT 0,
  updated_count BIGINT NOT NULL DEFAULT 0,
  skipped_count BIGINT NOT NULL DEFAULT 0,
  failed_count BIGINT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  INDEX idx_computed_tag_run_tag (tenant_id, tag_code, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedTagSchemaTest
```

Expected: PASS.

### Task 2: Computed Tag Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputedTagServiceTest {

    private ComputedTagService.DefinitionRepository definitions;
    private ComputedTagService.DependencyRepository dependencies;
    private ComputedTagService.UserCandidateRepository users;
    private CdpTagService cdpTagService;
    private ComputedTagService service;

    @BeforeEach
    void setUp() {
        definitions = mock(ComputedTagService.DefinitionRepository.class);
        dependencies = mock(ComputedTagService.DependencyRepository.class);
        users = mock(ComputedTagService.UserCandidateRepository.class);
        cdpTagService = mock(CdpTagService.class);
        service = new ComputedTagService(definitions, dependencies, users, cdpTagService);
    }

    @Test
    void activateRejectsDependencyCycleWithPath() {
        when(dependencies.loadGraph(0L)).thenReturn(Map.of(
                "tag_a", List.of("tag_b"),
                "tag_b", List.of("tag_a")));

        assertThatThrownBy(() -> service.activate(0L, "tag_a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag_a -> tag_b -> tag_a");
    }

    @Test
    void previewDoesNotWriteUserTags() {
        when(definitions.getActiveOrDraft(0L, "vip_likely")).thenReturn(new ComputedTagService.ComputedTagDefinition(
                "vip_likely", "BOOLEAN", "RULE", "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2}", "DRAFT"));
        when(users.scan(0L)).thenReturn(List.of(
                new ComputedTagService.UserCandidate("u1", Map.of("paidCount", 2)),
                new ComputedTagService.UserCandidate("u2", Map.of("paidCount", 0))));

        ComputedTagService.PreviewResult result = service.preview(0L, "vip_likely");

        assertThat(result.scannedCount()).isEqualTo(2);
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.samples()).extracting(ComputedTagService.PreviewSample::userId).containsExactly("u1");
        verify(cdpTagService, never()).setTag(eq("u1"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runNowWritesTagsThroughCdpTagServiceWithDeterministicIdempotencyKey() {
        when(definitions.getActive(0L, "vip_likely")).thenReturn(new ComputedTagService.ComputedTagDefinition(
                "vip_likely", "BOOLEAN", "RULE", "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2}", "ACTIVE"));
        when(users.scan(0L)).thenReturn(List.of(
                new ComputedTagService.UserCandidate("u1", Map.of("paidCount", 2))));

        ComputedTagService.RunResult result = service.runNow(0L, "vip_likely", "operator-1");

        ArgumentCaptor<CdpTagWriteReq> request = ArgumentCaptor.forClass(CdpTagWriteReq.class);
        verify(cdpTagService).setTag(eq("u1"), request.capture());
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(request.getValue().tagCode()).isEqualTo("vip_likely");
        assertThat(request.getValue().tagValue()).isEqualTo("true");
        assertThat(request.getValue().sourceType()).isEqualTo("COMPUTED_TAG");
        assertThat(request.getValue().idempotencyKey()).isEqualTo("computed-tag:" + result.runId() + ":u1:vip_likely");
    }

    @Test
    void pausedDefinitionDoesNotRun() {
        when(definitions.getActive(0L, "vip_likely")).thenThrow(new IllegalStateException("computed tag is not ACTIVE"));

        assertThatThrownBy(() -> service.runNow(0L, "vip_likely", "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }
}
```

- [ ] **Step 2: Implement cycle detection**

Implement DFS that returns a path:

```java
private String findCyclePath(String startTag, Map<String, List<String>> graph) {
    Deque<String> stack = new ArrayDeque<>();
    Set<String> visiting = new LinkedHashSet<>();
    return dfs(startTag, graph, visiting, stack);
}

private String dfs(String tag, Map<String, List<String>> graph, Set<String> visiting, Deque<String> stack) {
    if (visiting.contains(tag)) {
        List<String> cycle = new ArrayList<>(stack);
        int start = cycle.indexOf(tag);
        cycle = cycle.subList(start, cycle.size());
        cycle.add(tag);
        return String.join(" -> ", cycle);
    }
    visiting.add(tag);
    stack.addLast(tag);
    for (String next : graph.getOrDefault(tag, List.of())) {
        String cycle = dfs(next, graph, visiting, stack);
        if (cycle != null) {
            return cycle;
        }
    }
    stack.removeLast();
    visiting.remove(tag);
    return null;
}
```

Call it in `activate` and throw `IllegalArgumentException("computed tag dependency cycle: " + cyclePath)` when it returns a non-null path.

- [ ] **Step 3: Implement run-now writeback**

For each matched user, call:

```java
cdpTagService.setTag(userId, new CdpTagWriteReq(
        tagCode,
        computedValue,
        "computed tag run " + runId,
        expiresAt,
        "COMPUTED_TAG",
        String.valueOf(runId),
        "system",
        "computed-tag:" + runId + ":" + userId + ":" + tagCode));
```

- [ ] **Step 4: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedTagServiceTest
```

Expected: PASS.

### Task 3: Lineage Service And UI Helpers

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpLineageServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpLineageService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java`
- Create: `frontend/src/pages/cdp-computed-tags/computedTagPresentation.ts`
- Create: `frontend/src/pages/cdp-computed-tags/computedTagPresentation.test.ts`
- Modify: `frontend/src/services/cdpApi.ts`

- [ ] **Step 1: Write lineage tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpLineageServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpLineageServiceTest {

    @Test
    void lineageFindsTagDependencyAudienceRuleAndCanvasGraphReferences() {
        CdpLineageService.LineageRepository repo = mock(CdpLineageService.LineageRepository.class);
        when(repo.findDependentComputedTags(0L, "vip_likely")).thenReturn(List.of("high_value_user"));

        AudienceDefinitionDO audience = new AudienceDefinitionDO();
        audience.setId(10L);
        audience.setName("VIP campaign audience");
        audience.setRuleJson("{\"field\":\"tag.vip_likely\",\"op\":\"=\",\"value\":\"true\"}");
        when(repo.findAudiences(0L)).thenReturn(List.of(audience));

        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(20L);
        version.setCanvasId(30L);
        version.setGraphJson("{\"nodes\":[{\"id\":\"n1\",\"type\":\"CONDITION\",\"data\":{\"field\":\"tag.vip_likely\"}}]}");
        when(repo.findPublishedCanvasVersions(0L)).thenReturn(List.of(version));

        CdpLineageService service = new CdpLineageService(repo);

        List<CdpLineageService.LineageImpact> impacts = service.findTagLineage(0L, "vip_likely");

        assertThat(impacts).extracting(CdpLineageService.LineageImpact::objectType)
                .containsExactly("COMPUTED_TAG", "AUDIENCE", "CANVAS_VERSION");
        assertThat(impacts).extracting(CdpLineageService.LineageImpact::referencePath)
                .contains("computed_tag_dependency.depends_on_tag_code",
                        "audience_definition.rule_json",
                        "canvas_version.graph_json");
    }

    @Test
    void incompatibleTypeChangeReturnsBlockedImpactCheck() {
        CdpLineageService.LineageRepository repo = mock(CdpLineageService.LineageRepository.class);
        when(repo.findDependentComputedTags(0L, "vip_likely")).thenReturn(List.of("high_value_user"));

        CdpLineageService service = new CdpLineageService(repo);

        CdpLineageService.ImpactCheck result = service.checkTypeChange(0L, "vip_likely", "BOOLEAN", "NUMBER");

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("INCOMPATIBLE_TYPE_CHANGE");
        assertThat(result.impacts()).hasSize(1);
    }
}
```

- [ ] **Step 2: Implement lineage scanning**

Scan:

```text
computed_tag_dependency.depends_on_tag_code
audience_definition.rule_json
canvas.graph_json
```

Return impacted object type, id, name, and reference path.

- [ ] **Step 3: Add controller endpoints**

Expose computed tag list/create/preview/activate/pause/run/runs plus:

```text
GET /cdp/computed-tags/{tagCode}/lineage
POST /cdp/computed-tags/{tagCode}/impact-check
```

- [ ] **Step 4: Run verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedTagSchemaTest,ComputedTagServiceTest,CdpLineageServiceTest
cd frontend && npm test -- computedTagPresentation.test.ts
```

Expected: PASS.

- [ ] **Step 5: Add frontend presentation tests and helpers**

Create `frontend/src/pages/cdp-computed-tags/computedTagPresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  formatComputedTagRunSummary,
  formatLineageImpact,
  statusText,
  validateFallbackImpact
} from './computedTagPresentation'

describe('computedTagPresentation', () => {
  it('formats status and run counters', () => {
    expect(statusText('ACTIVE')).toBe('Active')
    expect(formatComputedTagRunSummary({
      scannedCount: 5,
      matchedCount: 3,
      updatedCount: 2,
      skippedCount: 1,
      failedCount: 0
    })).toBe('Scanned 5, matched 3, updated 2, skipped 1, failed 0')
  })

  it('formats lineage impacts for operator review', () => {
    expect(formatLineageImpact({
      objectType: 'AUDIENCE',
      objectId: '10',
      objectName: 'VIP campaign audience',
      referencePath: 'audience_definition.rule_json'
    })).toBe('AUDIENCE #10 VIP campaign audience - audience_definition.rule_json')
  })

  it('blocks incompatible type changes with lineage impacts', () => {
    expect(validateFallbackImpact({
      allowed: false,
      reason: 'INCOMPATIBLE_TYPE_CHANGE',
      impacts: [{ objectType: 'COMPUTED_TAG', objectId: 'high_value_user', referencePath: 'computed_tag_dependency.depends_on_tag_code' }]
    })).toEqual({ disabled: true, reason: 'INCOMPATIBLE_TYPE_CHANGE (1 impact)' })
  })
})
```

Create `frontend/src/pages/cdp-computed-tags/computedTagPresentation.ts`:

```ts
export interface ComputedTagRunSummary {
  scannedCount: number
  matchedCount: number
  updatedCount: number
  skippedCount: number
  failedCount: number
}

export interface LineageImpact {
  objectType: string
  objectId?: string | number | null
  objectName?: string | null
  referencePath: string
}

export interface ImpactCheck {
  allowed: boolean
  reason?: string | null
  impacts: LineageImpact[]
}

export function statusText(status: string): string {
  return status === 'ACTIVE' ? 'Active' : status === 'PAUSED' ? 'Paused' : 'Draft'
}

export function formatComputedTagRunSummary(summary: ComputedTagRunSummary): string {
  return `Scanned ${summary.scannedCount}, matched ${summary.matchedCount}, updated ${summary.updatedCount}, skipped ${summary.skippedCount}, failed ${summary.failedCount}`
}

export function formatLineageImpact(impact: LineageImpact): string {
  const name = impact.objectName ? ` ${impact.objectName}` : ''
  return `${impact.objectType} #${impact.objectId ?? '-'}${name} - ${impact.referencePath}`
}

export function validateFallbackImpact(check: ImpactCheck): { disabled: boolean; reason: string } {
  if (check.allowed) {
    return { disabled: false, reason: '' }
  }
  return { disabled: true, reason: `${check.reason ?? 'BLOCKED'} (${check.impacts.length} impact)` }
}
```

Run:

```bash
cd frontend && npm test -- computedTagPresentation.test.ts
```

Expected: PASS.
