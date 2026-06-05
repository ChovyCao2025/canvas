# CDP Computed Profile Attributes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add governed computed profile attributes with preview, activation, run history, profile JSON writeback, and change logs.

**Architecture:** Store definitions and run history in MySQL, compute first-slice RULE/EXPR values in `ComputedProfileAttributeService`, and write values back into `cdp_user_profile.properties_json` without replacing unrelated fields. Keep UI helpers small and testable before adding full page wiring.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, QLExpress/Aviator where already present, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Vitest.

---

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V99__cdp_computed_profile_attributes.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ComputedProfileAttributeDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ComputedProfileRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProfileAttributeChangeLogDO.java`
- Create: matching mappers under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java`
- Create: `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.ts`
- Create: `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.test.ts`

### Task 1: Schema And Contract Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V99__cdp_computed_profile_attributes.sql`

- [ ] **Step 1: Write schema test**

```java
@Test
void migrationCreatesComputedProfileTables() throws Exception {
    String sql = Files.readString(Path.of(
            "src/main/resources/db/migration/V99__cdp_computed_profile_attributes.sql"));

    assertThat(sql)
            .contains("CREATE TABLE IF NOT EXISTS computed_profile_attribute")
            .contains("attr_code")
            .contains("compute_type")
            .contains("refresh_mode")
            .contains("CREATE TABLE IF NOT EXISTS computed_profile_run")
            .contains("scanned_count")
            .contains("changed_count")
            .contains("CREATE TABLE IF NOT EXISTS profile_attribute_change_log")
            .contains("old_value")
            .contains("new_value");
}
```

- [ ] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedProfileAttributeSchemaTest
```

Expected: FAIL because migration does not exist.

- [ ] **Step 3: Add migration**

Create tables with these required columns:

```sql
CREATE TABLE IF NOT EXISTS computed_profile_attribute (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  attr_code VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  value_type VARCHAR(32) NOT NULL,
  compute_type VARCHAR(32) NOT NULL,
  expression_json JSON NOT NULL,
  refresh_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_computed_profile_attr (tenant_id, attr_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS computed_profile_run (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  attr_id BIGINT NOT NULL,
  source_event_id VARCHAR(128) NULL,
  status VARCHAR(20) NOT NULL,
  scanned_count BIGINT NOT NULL DEFAULT 0,
  matched_count BIGINT NOT NULL DEFAULT 0,
  changed_count BIGINT NOT NULL DEFAULT 0,
  unchanged_count BIGINT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  UNIQUE KEY uk_profile_run_event (tenant_id, attr_id, source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS profile_attribute_change_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  attr_code VARCHAR(128) NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  old_value VARCHAR(1000) NULL,
  new_value VARCHAR(1000) NULL,
  source_run_id BIGINT NOT NULL,
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_profile_attr_change_user (tenant_id, user_id, changed_at),
  INDEX idx_profile_attr_change_attr (tenant_id, attr_code, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedProfileAttributeSchemaTest
```

Expected: PASS.

### Task 4: Commit This Slice

**Files:**
- Modify: `backend/canvas-engine/src/main/resources/db/migration/V99__cdp_computed_profile_attributes.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeSchemaTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java`
- Modify: `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.ts`
- Modify: `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.test.ts`
- Modify: `frontend/src/services/cdpApi.ts`
- Modify: `docs/product-evolution/specs/p1-006-cdp-computed-profile-attributes.md`
- Modify: `docs/product-evolution/plans/p1-006-cdp-computed-profile-attributes-plan.md`

- [ ] **Step 1: Commit computed profile attribute slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V99__cdp_computed_profile_attributes.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeSchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java \
  frontend/src/pages/cdp-computed-profile/computedProfilePresentation.ts \
  frontend/src/pages/cdp-computed-profile/computedProfilePresentation.test.ts \
  frontend/src/services/cdpApi.ts \
  docs/product-evolution/specs/p1-006-cdp-computed-profile-attributes.md \
  docs/product-evolution/plans/p1-006-cdp-computed-profile-attributes-plan.md
git commit -m "feat: add cdp computed profile attributes"
```

Expected: commit contains only computed profile attribute schema, service, API, frontend helpers, tests, spec, and plan.

### Task 2: Service Preview And Run

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputedProfileAttributeServiceTest {

    private ComputedProfileAttributeService.AttributeRepository attributes;
    private ComputedProfileAttributeService.ProfileRepository profiles;
    private ComputedProfileAttributeService.RunRepository runs;
    private ComputedProfileAttributeService.ChangeLogRepository changeLogs;
    private ComputedProfileAttributeService service;

    @BeforeEach
    void setUp() {
        attributes = mock(ComputedProfileAttributeService.AttributeRepository.class);
        profiles = mock(ComputedProfileAttributeService.ProfileRepository.class);
        runs = mock(ComputedProfileAttributeService.RunRepository.class);
        changeLogs = mock(ComputedProfileAttributeService.ChangeLogRepository.class);
        service = new ComputedProfileAttributeService(attributes, profiles, runs, changeLogs, new ObjectMapper());
    }

    @Test
    void previewDoesNotMutateProfiles() {
        when(attributes.getActiveOrDraft(0L, 1L)).thenReturn(new ComputedProfileAttributeService.AttributeDefinition(
                1L, "lifecycle_stage", "STRING", "RULE", "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2}", "DRAFT"));
        when(profiles.scan(0L)).thenReturn(List.of(profile("u1", "{\"paidCount\":2}"), profile("u2", "{\"paidCount\":0}")));

        ComputedProfileAttributeService.PreviewResult result = service.preview(0L, 1L);

        assertThat(result.scannedCount()).isEqualTo(2);
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.changedCount()).isEqualTo(1);
        assertThat(result.samples()).extracting(ComputedProfileAttributeService.PreviewSample::userId).containsExactly("u1");
        verify(profiles, never()).updateProperties(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runNowWritesComputedValueAndChangeLogWhilePreservingUnrelatedProperties() {
        CdpUserProfileDO user = profile("u1", "{\"paidCount\":2,\"city\":\"Shanghai\"}");
        when(attributes.getActive(0L, 1L)).thenReturn(new ComputedProfileAttributeService.AttributeDefinition(
                1L, "lifecycle_stage", "STRING", "RULE", "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}", "ACTIVE"));
        when(profiles.scan(0L)).thenReturn(List.of(user));
        when(runs.start(0L, 1L, null, "operator-1")).thenReturn(99L);

        ComputedProfileAttributeService.RunResult result = service.runNow(0L, 1L, "operator-1");

        assertThat(result.changedCount()).isEqualTo(1);
        verify(profiles).updateProperties(argThat(profile ->
                profile.getPropertiesJson().contains("\"city\":\"Shanghai\"")
                        && profile.getPropertiesJson().contains("\"lifecycle_stage\":\"VIP\"")));
        verify(changeLogs).insert(argThat(change ->
                change.userId().equals("u1")
                        && change.attrCode().equals("lifecycle_stage")
                        && change.newValue().equals("VIP")
                        && change.sourceRunId().equals(99L)));
    }

    @Test
    void activateRejectsInvalidExpression() {
        when(attributes.getActiveOrDraft(0L, 1L)).thenReturn(new ComputedProfileAttributeService.AttributeDefinition(
                1L, "bad_attr", "STRING", "RULE", "{\"field\":\"paidCount\",\"op\":\"UNKNOWN\",\"value\":2}", "DRAFT"));

        assertThatThrownBy(() -> service.activate(0L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported rule operator");
    }

    @Test
    void eventDrivenRunIsIdempotentBySourceEventId() {
        when(runs.reserveEventRun(0L, 1L, "evt-1")).thenReturn(false);

        ComputedProfileAttributeService.RunResult result = service.runForEvent(0L, 1L, "evt-1", Map.of("paidCount", 2));

        assertThat(result.status()).isEqualTo("DUPLICATED");
        verify(profiles, never()).updateProperties(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void pausedDefinitionDoesNotRun() {
        when(attributes.getActive(0L, 1L)).thenThrow(new IllegalStateException("computed profile attribute is not ACTIVE"));

        assertThatThrownBy(() -> service.runNow(0L, 1L, "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }

    private static CdpUserProfileDO profile(String userId, String propertiesJson) {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId(userId);
        profile.setPropertiesJson(propertiesJson);
        return profile;
    }
}
```

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedProfileAttributeServiceTest
```

Expected: FAIL because `ComputedProfileAttributeService` does not exist.

- [ ] **Step 3: Implement service contract**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java` with these public records and methods:

```java
public record AttributeDefinition(Long id, String attrCode, String valueType, String computeType, String expressionJson, String status) {}
public record PreviewSample(String userId, String oldValue, String newValue) {}
public record PreviewResult(long scannedCount, long matchedCount, long changedCount, long unchangedCount, List<PreviewSample> samples) {}
public record RunResult(String status, Long runId, long scannedCount, long matchedCount, long changedCount, long unchangedCount) {}
public record ChangeLogInput(String userId, String attrCode, String oldValue, String newValue, Long sourceRunId) {}

public PreviewResult preview(Long tenantId, Long attrId)
public RunResult runNow(Long tenantId, Long attrId, String operator)
public RunResult runForEvent(Long tenantId, Long attrId, String sourceEventId, Map<String, Object> event)
public void activate(Long tenantId, Long attrId)
public void pause(Long tenantId, Long attrId)
```

Add constructor-facing repository interfaces so tests can use mocks before MyBatis wiring:

```java
public interface AttributeRepository {
    AttributeDefinition getActiveOrDraft(Long tenantId, Long attrId);
    AttributeDefinition getActive(Long tenantId, Long attrId);
    void activate(Long tenantId, Long attrId);
    void pause(Long tenantId, Long attrId);
}

public interface ProfileRepository {
    List<CdpUserProfileDO> scan(Long tenantId);
    void updateProperties(CdpUserProfileDO profile);
}

public interface RunRepository {
    Long start(Long tenantId, Long attrId, String sourceEventId, String operator);
    boolean reserveEventRun(Long tenantId, Long attrId, String sourceEventId);
    void finish(Long runId, String status, long scanned, long matched, long changed, long unchanged, String errorMessage);
}

public interface ChangeLogRepository {
    void insert(ChangeLogInput input);
}
```

- [ ] **Step 4: Implement JSON merge and rule evaluation**

Use Jackson `ObjectMapper` to merge into `properties_json` without replacing unrelated keys:

```java
private String writeComputedValue(String existingJson, String attrCode, String computedValue) throws IOException {
    ObjectNode root = existingJson == null || existingJson.isBlank()
            ? objectMapper.createObjectNode()
            : (ObjectNode) objectMapper.readTree(existingJson);
    root.put(attrCode, computedValue);
    return objectMapper.writeValueAsString(root);
}
```

Implement the first-slice RULE operator set used by the tests:

```java
private boolean evaluateRule(JsonNode expression, JsonNode properties) {
    String field = expression.path("field").asText();
    String op = expression.path("op").asText();
    JsonNode actual = properties.path(field);
    JsonNode expected = expression.path("value");
    return switch (op) {
        case "=" -> actual.asText().equals(expected.asText());
        case ">=" -> actual.asDouble() >= expected.asDouble();
        case "<=" -> actual.asDouble() <= expected.asDouble();
        default -> throw new IllegalArgumentException("unsupported rule operator: " + op);
    };
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedProfileAttributeServiceTest
```

Expected: PASS.

### Task 3: Controller And UI Helpers

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java`
- Create: `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.ts`
- Create: `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.test.ts`
- Modify: `frontend/src/services/cdpApi.ts`

- [ ] **Step 1: Add controller endpoints**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java`:

```java
@RestController
@RequestMapping("/cdp/computed-profile-attributes")
@RequiredArgsConstructor
public class CdpComputedProfileController {
    private final ComputedProfileAttributeService service;

    @GetMapping
    public List<ComputedProfileAttributeService.AttributeDefinition> list() {
        return List.of();
    }

    @PostMapping("/{id}/preview")
    public ComputedProfileAttributeService.PreviewResult preview(@PathVariable Long id) {
        return service.preview(0L, id);
    }

    @PostMapping("/{id}/activate")
    public void activate(@PathVariable Long id) {
        service.activate(0L, id);
    }

    @PostMapping("/{id}/pause")
    public void pause(@PathVariable Long id) {
        service.pause(0L, id);
    }

    @PostMapping("/{id}/run")
    public ComputedProfileAttributeService.RunResult run(@PathVariable Long id) {
        return service.runNow(0L, id, "system");
    }
}
```

When wiring persistence, replace `List.of()` with mapper-backed list/create/runs methods instead of adding query logic to the controller.

- [ ] **Step 2: Write frontend presentation tests**

Create `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  formatPreviewSummary,
  formatRunStatus,
  formatValueChange,
  profileAttributeStatusText
} from './computedProfilePresentation'

describe('computedProfilePresentation', () => {
  it('formats definition and run statuses', () => {
    expect(profileAttributeStatusText('ACTIVE')).toBe('Active')
    expect(profileAttributeStatusText('PAUSED')).toBe('Paused')
    expect(formatRunStatus('DUPLICATED')).toBe('Duplicated')
  })

  it('formats preview summary counts', () => {
    expect(formatPreviewSummary({ scannedCount: 10, matchedCount: 6, changedCount: 4, unchangedCount: 2 }))
      .toBe('Scanned 10, matched 6, changed 4, unchanged 2')
  })

  it('formats old and new value changes', () => {
    expect(formatValueChange(null, 'VIP')).toBe('(empty) -> VIP')
    expect(formatValueChange('Lead', 'VIP')).toBe('Lead -> VIP')
  })
})
```

- [ ] **Step 3: Add frontend presentation helpers**

Create `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.ts`:

```ts
export interface PreviewSummary {
  scannedCount: number
  matchedCount: number
  changedCount: number
  unchangedCount: number
}

export function profileAttributeStatusText(status: string): string {
  return status === 'ACTIVE' ? 'Active' : status === 'PAUSED' ? 'Paused' : 'Draft'
}

export function formatRunStatus(status: string): string {
  return status === 'DUPLICATED' ? 'Duplicated' : status === 'SUCCESS' ? 'Success' : status === 'FAILED' ? 'Failed' : 'Running'
}

export function formatPreviewSummary(summary: PreviewSummary): string {
  return `Scanned ${summary.scannedCount}, matched ${summary.matchedCount}, changed ${summary.changedCount}, unchanged ${summary.unchangedCount}`
}

export function formatValueChange(oldValue?: string | null, newValue?: string | null): string {
  return `${oldValue ?? '(empty)'} -> ${newValue ?? '(empty)'}`
}
```

- [ ] **Step 4: Add typed API wrapper**

Modify `frontend/src/services/cdpApi.ts`:

```ts
export interface ComputedProfileAttributePayload {
  attrCode: string
  displayName: string
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'
  computeType: 'RULE' | 'EXPR'
  expressionJson: Record<string, unknown>
  refreshMode: 'MANUAL' | 'EVENT'
}

export const computedProfileApi = {
  list: () => http.get<R<unknown>>('/cdp/computed-profile-attributes'),
  create: (payload: ComputedProfileAttributePayload) =>
    http.post<R<unknown>>('/cdp/computed-profile-attributes', payload),
  preview: (id: number) =>
    http.post<R<unknown>>(`/cdp/computed-profile-attributes/${id}/preview`),
  activate: (id: number) =>
    http.post<R<void>>(`/cdp/computed-profile-attributes/${id}/activate`),
  pause: (id: number) =>
    http.post<R<void>>(`/cdp/computed-profile-attributes/${id}/pause`),
  run: (id: number) =>
    http.post<R<unknown>>(`/cdp/computed-profile-attributes/${id}/run`),
  runs: (id: number) =>
    http.get<R<unknown>>(`/cdp/computed-profile-attributes/${id}/runs`)
}
```

- [ ] **Step 5: Run verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ComputedProfileAttributeSchemaTest,ComputedProfileAttributeServiceTest
cd frontend && npm test -- computedProfilePresentation.test.ts
```

Expected: PASS.
