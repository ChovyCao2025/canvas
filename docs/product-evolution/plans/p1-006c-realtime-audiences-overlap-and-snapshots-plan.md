# Realtime Audiences, Overlap, And Snapshots Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add event-driven realtime audience updates, overlap analysis, guarded merge/exclusion, and audience snapshots.

**Architecture:** Consume P1-005A2 CDP events in a service boundary that can be tested without RocketMQ, update Redis RoaringBitmap membership through `AudienceBitmapStore`, and keep overlap/snapshot operations behind safe-size guards until bitmap collision remediation is accepted.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Redis, RoaringBitmap, RocketMQ, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-006c-realtime-audiences-overlap-and-snapshots.md`
- Depends on P1-005A2 internal CDP event publication and current `AudienceBitmapStore` bitmap persistence.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V101__realtime_audience_overlap_snapshots.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RealtimeAudienceEventLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RealtimeAudienceEventLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceSchemaTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBitmapStoreSetOpsTest.java`
- Create: `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.ts`
- Create: `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.test.ts`
- Modify: `frontend/src/services/cdpApi.ts`

### Task 1: Schema And Bitmap Set Operations

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceSchemaTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBitmapStoreSetOpsTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V101__realtime_audience_overlap_snapshots.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RealtimeAudienceEventLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RealtimeAudienceEventLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`

- [ ] **Step 1: Write schema contract test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceSchemaTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeAudienceSchemaTest {

    @Test
    void migrationCreatesRealtimeEventLogAndSnapshotTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V101__realtime_audience_overlap_snapshots.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS realtime_audience_event_log")
                .contains("tenant_id")
                .contains("audience_id")
                .contains("source_event_id")
                .contains("operation")
                .contains("UNIQUE KEY uk_realtime_audience_event")
                .contains("CREATE TABLE IF NOT EXISTS audience_snapshot")
                .contains("estimated_size")
                .contains("bitmap_key")
                .contains("snapshot_source")
                .contains("created_at");
    }
}
```

- [ ] **Step 2: Write bitmap set operation tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBitmapStoreSetOpsTest.java`:

```java
package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceBitmapStoreSetOpsTest {

    @Test
    void overlapMergeAndExcludeReturnNewBitmapsWithoutMutatingStoredData() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AudienceBitmapStore store = new AudienceBitmapStore(redis);

        store.save(10L, RoaringBitmap.bitmapOf(1, 2, 3));
        store.save(11L, RoaringBitmap.bitmapOf(3, 4));

        ArgumentCaptor<String> storedValues = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("audience:bitmap:10"), storedValues.capture());
        String leftEncoded = storedValues.getValue();
        verify(values).set(eq("audience:bitmap:11"), storedValues.capture());
        String rightEncoded = storedValues.getValue();
        when(values.get("audience:bitmap:10")).thenReturn(leftEncoded);
        when(values.get("audience:bitmap:11")).thenReturn(rightEncoded);

        assertThat(store.overlap(10L, 11L).getCardinality()).isEqualTo(1);
        assertThat(store.merge(10L, 11L).getCardinality()).isEqualTo(4);
        assertThat(store.exclude(10L, 11L).getCardinality()).isEqualTo(2);
        assertThat(store.load(10L).getCardinality()).isEqualTo(3);
    }
}
```

- [ ] **Step 3: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RealtimeAudienceSchemaTest,AudienceBitmapStoreSetOpsTest
```

Expected: FAIL because the migration and bitmap set operation methods do not exist.

- [ ] **Step 4: Add migration, data objects, and mappers**

Create `backend/canvas-engine/src/main/resources/db/migration/V101__realtime_audience_overlap_snapshots.sql`:

```sql
CREATE TABLE IF NOT EXISTS realtime_audience_event_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  audience_id BIGINT NOT NULL,
  source_event_id VARCHAR(128) NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  operation VARCHAR(20) NOT NULL,
  event_time DATETIME(3) NULL,
  processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_realtime_audience_event (tenant_id, audience_id, source_event_id),
  INDEX idx_realtime_audience_event_user (tenant_id, user_id, processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audience_snapshot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  audience_id BIGINT NOT NULL,
  estimated_size BIGINT NOT NULL DEFAULT 0,
  bitmap_key VARCHAR(256) NOT NULL,
  snapshot_source VARCHAR(32) NOT NULL,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audience_snapshot_audience (tenant_id, audience_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Create `RealtimeAudienceEventLogDO` and `AudienceSnapshotDO` with `@TableName`, `@TableId(type = IdType.AUTO)`, fields matching the migration, and `LocalDateTime` for timestamp columns. Create mappers by extending `BaseMapper<RealtimeAudienceEventLogDO>` and `BaseMapper<AudienceSnapshotDO>`.

- [ ] **Step 5: Extend AudienceBitmapStore**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`:

```java
public RoaringBitmap overlap(Long leftAudienceId, Long rightAudienceId) throws IOException {
    RoaringBitmap result = load(leftAudienceId);
    result.and(load(rightAudienceId));
    return result;
}

public RoaringBitmap merge(Long leftAudienceId, Long rightAudienceId) throws IOException {
    RoaringBitmap result = load(leftAudienceId);
    result.or(load(rightAudienceId));
    return result;
}

public RoaringBitmap exclude(Long baseAudienceId, Long excludedAudienceId) throws IOException {
    RoaringBitmap result = load(baseAudienceId);
    result.andNot(load(excludedAudienceId));
    return result;
}
```

- [ ] **Step 6: Run schema and bitmap tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RealtimeAudienceSchemaTest,AudienceBitmapStoreSetOpsTest
```

Expected: PASS.

### Task 5: Commit This Slice

**Files:**
- Modify: `backend/canvas-engine/src/main/resources/db/migration/V101__realtime_audiences_overlap_snapshots.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/AudienceBitmapStore.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceSchemaTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/AudienceBitmapStoreSetOpsTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceServiceTest.java`
- Modify: `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.ts`
- Modify: `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.test.ts`
- Modify: `frontend/src/services/cdpApi.ts`
- Modify: `docs/product-evolution/specs/p1-006c-realtime-audiences-overlap-and-snapshots.md`
- Modify: `docs/product-evolution/plans/p1-006c-realtime-audiences-overlap-and-snapshots-plan.md`

- [ ] **Step 1: Commit realtime audience slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V101__realtime_audiences_overlap_snapshots.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/AudienceBitmapStore.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceSchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/AudienceBitmapStoreSetOpsTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceServiceTest.java \
  frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.ts \
  frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.test.ts \
  frontend/src/services/cdpApi.ts \
  docs/product-evolution/specs/p1-006c-realtime-audiences-overlap-and-snapshots.md \
  docs/product-evolution/plans/p1-006c-realtime-audiences-overlap-and-snapshots-plan.md
git commit -m "feat: add realtime audiences and snapshots"
```

Expected: commit contains only realtime audience schema, bitmap set operations, service/API, frontend helpers, tests, spec, and plan.

### Task 2: Realtime Membership Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java`

- [ ] **Step 1: Write realtime service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceServiceTest.java`:

```java
package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeAudienceServiceTest {

    private RealtimeAudienceService.EventLogRepository eventLogs;
    private RealtimeAudienceService.AudienceRuleRepository audienceRules;
    private RealtimeAudienceService.SnapshotRepository snapshots;
    private AudienceBitmapStore bitmapStore;
    private RealtimeAudienceService service;

    @BeforeEach
    void setUp() {
        eventLogs = mock(RealtimeAudienceService.EventLogRepository.class);
        audienceRules = mock(RealtimeAudienceService.AudienceRuleRepository.class);
        snapshots = mock(RealtimeAudienceService.SnapshotRepository.class);
        bitmapStore = mock(AudienceBitmapStore.class);
        service = new RealtimeAudienceService(eventLogs, audienceRules, snapshots, bitmapStore, 50_000);
    }

    @Test
    void processEventAddsMatchingUser() throws Exception {
        when(audienceRules.matches(0L, 10L, Map.of("event", "Paid"))).thenReturn(true);
        when(eventLogs.reserve(0L, 10L, "evt-1", "u1", "ADD")).thenReturn(true);
        when(bitmapStore.load(10L)).thenReturn(new RoaringBitmap());

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-1", "u1", Instant.parse("2026-06-03T00:00:00Z"), Map.of("event", "Paid")),
                true);

        assertThat(result.status()).isEqualTo("UPDATED");
        assertThat(result.operation()).isEqualTo("ADD");
        verify(bitmapStore).save(eq(10L), argThat(bitmap -> bitmap.contains(AudienceBitmapStore.toUid("u1"))));
    }

    @Test
    void processEventRemovesUserWhenRemoveOnNoMatchIsTrue() throws Exception {
        RoaringBitmap existing = RoaringBitmap.bitmapOf(AudienceBitmapStore.toUid("u1"));
        when(audienceRules.matches(0L, 10L, Map.of("event", "Refunded"))).thenReturn(false);
        when(eventLogs.reserve(0L, 10L, "evt-2", "u1", "REMOVE")).thenReturn(true);
        when(bitmapStore.load(10L)).thenReturn(existing);

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-2", "u1", Instant.parse("2026-06-03T00:00:01Z"), Map.of("event", "Refunded")),
                true);

        assertThat(result.status()).isEqualTo("UPDATED");
        assertThat(result.operation()).isEqualTo("REMOVE");
        verify(bitmapStore).save(eq(10L), argThat(bitmap -> !bitmap.contains(AudienceBitmapStore.toUid("u1"))));
    }

    @Test
    void processEventIsIdempotentBySourceEventId() throws Exception {
        when(audienceRules.matches(0L, 10L, Map.of("event", "Paid"))).thenReturn(true);
        when(eventLogs.reserve(0L, 10L, "evt-1", "u1", "ADD")).thenReturn(false);

        RealtimeAudienceService.EventResult result = service.processEvent(0L, 10L,
                new RealtimeAudienceService.CdpEvent("evt-1", "u1", Instant.parse("2026-06-03T00:00:00Z"), Map.of("event", "Paid")),
                true);

        assertThat(result.status()).isEqualTo("DUPLICATED");
        verify(bitmapStore, never()).save(anyLong(), any());
    }

    @Test
    void overlapReturnsCountsAndPercentages() throws Exception {
        when(bitmapStore.load(10L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3, 4));
        when(bitmapStore.load(11L)).thenReturn(RoaringBitmap.bitmapOf(3, 4));

        RealtimeAudienceService.OverlapResult result = service.overlap(10L, 11L);

        assertThat(result.intersectionCount()).isEqualTo(2);
        assertThat(result.leftPercentage()).isEqualTo(50.0);
        assertThat(result.rightPercentage()).isEqualTo(100.0);
    }

    @Test
    void mergeAndExclusionBlockAboveSafeSizeLimit() throws Exception {
        service = new RealtimeAudienceService(eventLogs, audienceRules, snapshots, bitmapStore, 2);
        when(bitmapStore.merge(10L, 11L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3));
        when(bitmapStore.exclude(10L, 11L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3));

        assertThat(service.merge(10L, 11L).status()).isEqualTo("BLOCKED");
        assertThat(service.exclude(10L, 11L).reason()).isEqualTo("SAFE_SIZE_LIMIT_EXCEEDED");
    }

    @Test
    void snapshotStoresAudienceSizeBitmapKeyAndSource() throws Exception {
        when(bitmapStore.load(10L)).thenReturn(RoaringBitmap.bitmapOf(1, 2, 3));

        RealtimeAudienceService.SnapshotResult result = service.createSnapshot(0L, 10L, "MANUAL", "operator-1");

        assertThat(result.estimatedSize()).isEqualTo(3);
        assertThat(result.bitmapKey()).isEqualTo("audience:bitmap:10");
        verify(snapshots).insert(argThat(snapshot ->
                snapshot.estimatedSize() == 3 && snapshot.snapshotSource().equals("MANUAL")));
    }
}
```

- [ ] **Step 2: Run realtime service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RealtimeAudienceServiceTest
```

Expected: FAIL because `RealtimeAudienceService` does not exist.

- [ ] **Step 3: Implement service contract**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java` with these records and constructor-facing repository interfaces:

```java
public record CdpEvent(String sourceEventId, String userId, Instant eventTime, Map<String, Object> properties) {}
public record EventResult(String status, String operation, long audienceId, String sourceEventId, String userId) {}
public record OverlapResult(long leftCount, long rightCount, long intersectionCount, double leftPercentage, double rightPercentage) {}
public record SetOperationResult(String status, String reason, long resultSize, long safeLimit) {}
public record SnapshotInput(Long tenantId, Long audienceId, long estimatedSize, String bitmapKey, String snapshotSource, String createdBy) {}
public record SnapshotResult(Long audienceId, long estimatedSize, String bitmapKey, String snapshotSource) {}

public interface EventLogRepository {
    boolean reserve(Long tenantId, Long audienceId, String sourceEventId, String userId, String operation);
}

public interface AudienceRuleRepository {
    boolean matches(Long tenantId, Long audienceId, Map<String, Object> eventProperties);
}

public interface SnapshotRepository {
    void insert(SnapshotInput input);
}
```

Use this mutation order in `processEvent`:

```java
boolean matches = audienceRules.matches(tenantId, audienceId, event.properties());
String operation = matches ? "ADD" : removeOnNoMatch ? "REMOVE" : "NOOP";
if ("NOOP".equals(operation)) {
    return new EventResult("SKIPPED", operation, audienceId, event.sourceEventId(), event.userId());
}
if (!eventLogs.reserve(tenantId, audienceId, event.sourceEventId(), event.userId(), operation)) {
    return new EventResult("DUPLICATED", operation, audienceId, event.sourceEventId(), event.userId());
}
RoaringBitmap bitmap = bitmapStore.load(audienceId);
int uid = AudienceBitmapStore.toUid(event.userId());
if ("ADD".equals(operation)) {
    bitmap.add(uid);
} else {
    bitmap.remove(uid);
}
bitmapStore.save(audienceId, bitmap);
return new EventResult("UPDATED", operation, audienceId, event.sourceEventId(), event.userId());
```

- [ ] **Step 4: Implement guarded set operations and snapshots**

Use this guard helper inside `merge` and `exclude`:

```java
private SetOperationResult guard(String operation, RoaringBitmap result) {
    long cardinality = result.getLongCardinality();
    if (cardinality > safeSetOperationLimit) {
        return new SetOperationResult("BLOCKED", "SAFE_SIZE_LIMIT_EXCEEDED", cardinality, safeSetOperationLimit);
    }
    return new SetOperationResult("READY", operation, cardinality, safeSetOperationLimit);
}
```

Implement snapshot creation:

```java
public SnapshotResult createSnapshot(Long tenantId, Long audienceId, String source, String operator) throws IOException {
    RoaringBitmap bitmap = bitmapStore.load(audienceId);
    String bitmapKey = "audience:bitmap:" + audienceId;
    SnapshotInput input = new SnapshotInput(tenantId, audienceId, bitmap.getLongCardinality(), bitmapKey, source, operator);
    snapshots.insert(input);
    return new SnapshotResult(audienceId, input.estimatedSize(), bitmapKey, source);
}
```

- [ ] **Step 5: Run realtime service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RealtimeAudienceServiceTest
```

Expected: PASS.

### Task 3: Controller And Frontend Contracts

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java`
- Create: `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.ts`
- Create: `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.test.ts`
- Modify: `frontend/src/services/cdpApi.ts`

- [ ] **Step 1: Add controller endpoints**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java`:

```java
@RestController
@RequestMapping("/cdp")
@RequiredArgsConstructor
public class RealtimeAudienceController {
    private final RealtimeAudienceService service;

    @PostMapping("/realtime-audiences/{id}/events")
    public RealtimeAudienceService.EventResult processEvent(@PathVariable Long id,
                                                            @RequestBody RealtimeAudienceService.CdpEvent event) throws IOException {
        return service.processEvent(0L, id, event, true);
    }

    @PostMapping("/realtime-audiences/{id}/snapshot")
    public RealtimeAudienceService.SnapshotResult snapshot(@PathVariable Long id) throws IOException {
        return service.createSnapshot(0L, id, "MANUAL", "system");
    }

    @GetMapping("/audiences/{leftId}/overlap/{rightId}")
    public RealtimeAudienceService.OverlapResult overlap(@PathVariable Long leftId, @PathVariable Long rightId) throws IOException {
        return service.overlap(leftId, rightId);
    }

    @PostMapping("/audiences/merge")
    public RealtimeAudienceService.SetOperationResult merge(@RequestParam Long leftId, @RequestParam Long rightId) throws IOException {
        return service.merge(leftId, rightId);
    }

    @PostMapping("/audiences/exclude")
    public RealtimeAudienceService.SetOperationResult exclude(@RequestParam Long baseId, @RequestParam Long excludedId) throws IOException {
        return service.exclude(baseId, excludedId);
    }
}
```

The class-level `@RequestMapping("/cdp")` plus method-level `/audiences/{leftId}/overlap/{rightId}` registers exactly `/cdp/audiences/{leftId}/overlap/{rightId}`.

- [ ] **Step 2: Write frontend presentation tests**

Create `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  formatOverlapPercent,
  formatSnapshotRow,
  formatSetOperation,
  realtimeStatusText
} from './realtimeAudiencePresentation'

describe('realtimeAudiencePresentation', () => {
  it('formats realtime event statuses', () => {
    expect(realtimeStatusText('UPDATED')).toBe('Updated')
    expect(realtimeStatusText('DUPLICATED')).toBe('Duplicated')
    expect(realtimeStatusText('BLOCKED')).toBe('Blocked')
  })

  it('formats overlap percentages with one decimal place', () => {
    expect(formatOverlapPercent(33.333)).toBe('33.3%')
    expect(formatOverlapPercent(100)).toBe('100.0%')
  })

  it('formats guarded operation messages', () => {
    expect(formatSetOperation({ status: 'BLOCKED', reason: 'SAFE_SIZE_LIMIT_EXCEEDED', resultSize: 120000, safeLimit: 50000 }))
      .toBe('Blocked: SAFE_SIZE_LIMIT_EXCEEDED, result size 120000 exceeds safe limit 50000')
  })

  it('formats snapshot trend rows', () => {
    expect(formatSnapshotRow({ audienceId: 10, estimatedSize: 42, bitmapKey: 'audience:bitmap:10', snapshotSource: 'MANUAL', createdAt: '2026-06-03T00:00:00Z' }))
      .toBe('#10 size 42 from MANUAL at 2026-06-03T00:00:00Z')
  })
})
```

- [ ] **Step 3: Add frontend presentation helpers**

Create `frontend/src/pages/realtime-audiences/realtimeAudiencePresentation.ts`:

```ts
export interface SetOperationResult {
  status: string
  reason?: string | null
  resultSize: number
  safeLimit: number
}

export interface AudienceSnapshotRow {
  audienceId: number
  estimatedSize: number
  bitmapKey: string
  snapshotSource: string
  createdAt: string
}

export function realtimeStatusText(status: string): string {
  return status === 'UPDATED' ? 'Updated' : status === 'DUPLICATED' ? 'Duplicated' : status === 'BLOCKED' ? 'Blocked' : 'Skipped'
}

export function formatOverlapPercent(value: number): string {
  return `${value.toFixed(1)}%`
}

export function formatSetOperation(result: SetOperationResult): string {
  if (result.status === 'BLOCKED') {
    return `Blocked: ${result.reason ?? 'UNKNOWN'}, result size ${result.resultSize} exceeds safe limit ${result.safeLimit}`
  }
  return `Ready: result size ${result.resultSize}`
}

export function formatSnapshotRow(row: AudienceSnapshotRow): string {
  return `#${row.audienceId} size ${row.estimatedSize} from ${row.snapshotSource} at ${row.createdAt}`
}
```

- [ ] **Step 4: Add typed API wrapper**

Modify `frontend/src/services/cdpApi.ts`:

```ts
export interface RealtimeAudienceEventPayload {
  sourceEventId: string
  userId: string
  eventTime: string
  properties: Record<string, unknown>
}

export const realtimeAudienceApi = {
  processEvent: (audienceId: number, payload: RealtimeAudienceEventPayload) =>
    http.post<R<unknown>>(`/cdp/realtime-audiences/${audienceId}/events`, payload),
  createSnapshot: (audienceId: number) =>
    http.post<R<unknown>>(`/cdp/realtime-audiences/${audienceId}/snapshot`),
  overlap: (leftId: number, rightId: number) =>
    http.get<R<unknown>>(`/cdp/audiences/${leftId}/overlap/${rightId}`),
  merge: (leftId: number, rightId: number) =>
    http.post<R<unknown>>('/cdp/audiences/merge', null, { params: { leftId, rightId } }),
  exclude: (baseId: number, excludedId: number) =>
    http.post<R<unknown>>('/cdp/audiences/exclude', null, { params: { baseId, excludedId } })
}
```

- [ ] **Step 5: Run full verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RealtimeAudienceSchemaTest,AudienceBitmapStoreSetOpsTest,RealtimeAudienceServiceTest
cd frontend && npm test -- realtimeAudiencePresentation.test.ts
```

Expected: PASS.
