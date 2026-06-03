# Trigger Version Lock Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Lock canvas version at trigger time, use locked version throughout execution. Preserve old version until all referencing executions complete.

**Architecture:** ExecutionContext carries versionId (already exists as Long field). Cache lookup uses (canvasId, versionId). Old version retained via Redis reference counting. VersionExpiredException thrown when locked version is no longer cached.

**Tech Stack:** Redis, Caffeine cache, MySQL, Java 21

---

### Task 1: Add Version Locking to ExecutionContext

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextVersionTest.java`

- [ ] **Step 1: Write failing test**

```java
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextVersionTest {
    @Test
    void contextCarriesVersionId() {
        // ExecutionContext already has a versionId field (Long type, set via @Setter)
        ExecutionContext ctx = new ExecutionContext();
        ctx.setVersionId(42L);
        assertThat(ctx.getVersionId()).isEqualTo(42L);
    }

    @Test
    void versionIdPersistedInRedis() {
        // This test requires @SpringBootTest with ContextPersistenceService
        // See integration test below
    }
}

@SpringBootTest
class ExecutionContextVersionIntegrationTest {
    @Autowired
    private ContextPersistenceService contextPersistence;

    @Test
    void versionIdPersistedInRedis() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(1L);
        ctx.setVersionId(42L);
        ctx.setUserId("user-1");
        contextPersistence.save(ctx);

        ExecutionContext loaded = contextPersistence.load(1L, "user-1");
        assertThat(loaded.getVersionId()).isEqualTo(42L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextVersionTest -v`
Expected: FAIL — the integration test should fail because versionId persistence in Redis is not yet verified.

- [ ] **Step 3: Implement version locking in CanvasExecutionService**

The existing `CanvasExecutionService` already locks the version at trigger time. Verify and enhance the implementation:

```java
// In CanvasExecutionService.newContext() — version is already locked:
private ExecutionContext newContext(Long canvasId, Long versionId, String userId, String triggerType) {
    ExecutionContext ctx = new ExecutionContext();
    ctx.setExecutionId(UUID.randomUUID().toString());
    ctx.setCanvasId(canvasId);
    ctx.setVersionId(versionId);  // ← version locked here
    ctx.setUserId(userId);
    ctx.setTriggerType(triggerType);
    ...
    return ctx;
}

// In CanvasExecutionService.resolveVersionId() — version resolution logic:
private Long resolveVersionId(CanvasDO canvas, String userId, boolean dryRun) {
    // Canary routing: check if user is in canary group
    if (canvas.getCanaryVersionId() != null && isCanaryUser(canvas, userId)) {
        return canvas.getCanaryVersionId();
    }
    // Default to current published version
    return canvas.getCurrentVersionId();
}
```

The cache lookup already uses the 2-key signature. Show the complete `buildPrepMap()` method:

```java
private Map<String, Object> buildPrepMap(Long canvasId, String userId, String triggerType,
        String triggerNodeType, String matchKey, Map<String, Object> payload,
        boolean dryRun, boolean isResume, CanvasDO canvas, int admissionLimit,
        String acquiredDedupKey, boolean quotaBypass, ExecutionLane executionLane) {
    Map<String, Object> prepMap = new HashMap<>();
    prepMap.put("canvasId", canvasId);
    prepMap.put("userId", userId);
    prepMap.put("triggerType", triggerType);
    prepMap.put("triggerNodeType", triggerNodeType);
    prepMap.put("matchKey", matchKey);
    prepMap.put("payload", payload);
    prepMap.put("dryRun", dryRun);
    prepMap.put("isResume", isResume);
    prepMap.put("canvas", canvas);
    prepMap.put("admissionLimit", admissionLimit);
    prepMap.put("acquiredDedupKey", acquiredDedupKey);
    prepMap.put("quotaBypass", quotaBypass);
    prepMap.put("executionLane", executionLane);
    return prepMap;
}
```

And the integration point in `triggerInternal()`:

```java
// In CanvasExecutionService.triggerInternal(), after resolving version:
Long versionId = resolveVersionId(canvas, userId, dryRun);
ExecutionContext ctx = newContext(canvasId, versionId, userId, triggerType);

// Later, when building prep map and fetching graph:
Map<String, Object> prepMap = buildPrepMap(canvasId, userId, triggerType, triggerNodeType,
        matchKey, payload, dryRun, isResume, canvas, admissionLimit,
        acquiredDedupKey, quotaBypass, executionLane);

// In the execution path, graph is fetched with versionId:
DagGraph graph = configCache.get(canvasId, ctx.getVersionId());
```

**Note:** Replace `configCache` with the actual field name used in CanvasExecutionService. The actual field is `configCache` (type `CanvasConfigCache`), confirmed at line 77 of `CanvasExecutionService.java`. Verify via `grep -n 'configCache\|ConfigCache' CanvasExecutionService.java`.

- [ ] **Step 4: Add version mismatch check with VersionExpiredException**

Create a new exception class:

```java
package org.chovy.canvas.engine.exception;

/**
 * Thrown when a canvas version that an execution was locked to is no longer
 * available in the cache (e.g., evicted before all referencing executions completed).
 */
public class VersionExpiredException extends RuntimeException {
    private final Long canvasId;
    private final Long versionId;

    public VersionExpiredException(Long canvasId, Long versionId) {
        super("Version " + versionId + " no longer available for canvas " + canvasId);
        this.canvasId = canvasId;
        this.versionId = versionId;
    }

    public Long getCanvasId() { return canvasId; }
    public Long getVersionId() { return versionId; }
}
```

Add version expiry check in `buildPrepMap()`:

**Location note:** In `CanvasExecutionService.buildPrepMap()`, locate the line `ExecutionContext ctx = new ExecutionContext(...)` (around line 647). Insert the version expiry check immediately after this line, BEFORE the existing graph lookup.

```java
// In CanvasExecutionService, after creating the ExecutionContext:
DagGraph graph = configCache.get(canvasId, ctx.getVersionId());
if (graph == null) {
    throw new VersionExpiredException(canvasId, ctx.getVersionId());
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextVersionTest,ExecutionContextVersionIntegrationTest -v`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/exception/VersionExpiredException.java
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/
git commit -m "feat: add VersionExpiredException and version expiry check in buildPrepMap"
```

---

### Task 2: Implement Version Reference Counting

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/cache/CanvasVersionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/cache/CanvasVersionServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest
class CanvasVersionServiceTest {
    @Autowired
    private CanvasVersionService versionService;

    @Test
    void acquireAndRelease_versionRetainedWhileInUse() {
        versionService.acquireVersion(1L, 1L);
        versionService.acquireVersion(1L, 1L);
        // Still retained — 2 acquires
        assertThat(versionService.getRefCount(1L, 1L)).isEqualTo(2);

        versionService.releaseVersion(1L, 1L);
        assertThat(versionService.getRefCount(1L, 1L)).isEqualTo(1);
        // Not yet evictable
        assertThat(versionService.isEvictable(1L, 1L)).isFalse();

        versionService.releaseVersion(1L, 1L);
        // Now evictable — ref count = 0
        assertThat(versionService.isEvictable(1L, 1L)).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=CanvasVersionServiceTest -v`
Expected: FAIL.

- [ ] **Step 3: Implement CanvasVersionService**

```java
@Service
@RequiredArgsConstructor
public class CanvasVersionService {
    private static final String REF_PREFIX = "canvas:version:ref:";
    private final StringRedisTemplate redis;

    public void acquireVersion(Long canvasId, Long versionId) {
        redis.opsForValue().increment(REF_PREFIX + canvasId + ":" + versionId);
    }

    public void releaseVersion(Long canvasId, Long versionId) {
        Long remaining = redis.opsForValue().decrement(REF_PREFIX + canvasId + ":" + versionId);
        if (remaining != null && remaining <= 0) {
            redis.delete(REF_PREFIX + canvasId + ":" + versionId);
        }
    }

    public long getRefCount(Long canvasId, Long versionId) {
        String val = redis.opsForValue().get(REF_PREFIX + canvasId + ":" + versionId);
        return val != null ? Long.parseLong(val) : 0;
    }

    public boolean isEvictable(Long canvasId, Long versionId) {
        return getRefCount(canvasId, versionId) <= 0;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=CanvasVersionServiceTest -v`
Expected: PASS.

- [ ] **Step 5: Integrate into execution lifecycle**

```java
// CanvasExecutionService — at execution start:
versionService.acquireVersion(canvasId, versionId);

// At execution complete/fail (in handleSuccess/handleError — these methods exist
// on CanvasExecutionService, confirmed at lines 471 and 500):
versionService.releaseVersion(canvasId, versionId);
```

**Location note:** Insert the version update call in CanvasExecutionService at the end of the success handling path (around line 471) and error handling path (around line 500).

- [ ] **Step 6: Add version mismatch check on Wait resume**

The `WaitResumeService` already calls `executionService.trigger()` which goes through `triggerInternal()` → `buildPrepMap()` → `configCache.get(canvasId, ctx.getVersionId())`. The `VersionExpiredException` added in Task 1 Step 4 will be thrown automatically if the version is no longer cached. No additional code needed in WaitResumeService.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/cache/
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/cache/
git commit -m "feat: implement version reference counting and integrate into execution lifecycle"
```
