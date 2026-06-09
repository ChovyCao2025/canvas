# TAGGER Audience Runtime Snapshot Branching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make TAGGER audience runtime use locked snapshot membership when `audienceSnapshotId` is present and current audience data otherwise.

**Architecture:** Extend `TaggerHandler` with `AudienceSnapshotService` and isolate snapshot lookup in two helpers: one for fan-out user lists and one for realtime membership. Preserve the existing resolver and bitmap behavior for dynamic refresh paths.

**Tech Stack:** Java 21, Spring Boot, Reactor, JUnit 5, Mockito, AssertJ.

---

## Implementation Status

- Status: implemented and focused-verified on 2026-06-05.
- Runtime TAGGER now uses `AudienceSnapshotService.users(snapshotId)` for scheduled static fan-out and `AudienceSnapshotService.contains(snapshotId, userId)` for realtime static membership.
- Dynamic fan-out and realtime membership keep the existing `AudienceUserResolver` and `AudienceBitmapStore` behavior when no `audienceSnapshotId` is present.
- Commit: not created in this session.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003c-tagger-audience-runtime-snapshot-branching.md`
- Depends on `docs/product-evolution/specs/p1-003b-publish-time-audience-snapshot-locking.md`

## File Structure

- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java`

### Task 1: Snapshot Read Methods

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java`

- [x] **Step 1: Add snapshot read tests**

Add to `AudienceSnapshotServiceTest`:

```java
@Test
void usersReadsSnapshotJsonArray() {
    AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
    AudienceSnapshotDO snapshot = new AudienceSnapshotDO();
    snapshot.setId(501L);
    snapshot.setUserIdsJson("[\"u1\",\"u2\"]");
    when(snapshotMapper.selectById(501L)).thenReturn(snapshot);
    AudienceSnapshotService service = new AudienceSnapshotService(
            mock(AudienceUserResolver.class), snapshotMapper, mock(AudienceDefinitionMapper.class), new ObjectMapper(), 10_000);

    assertThat(service.users(501L)).containsExactly("u1", "u2");
    assertThat(service.contains(501L, "u2")).isTrue();
}

@Test
void usersRejectsMissingSnapshot() {
    AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
    when(snapshotMapper.selectById(501L)).thenReturn(null);
    AudienceSnapshotService service = new AudienceSnapshotService(
            mock(AudienceUserResolver.class), snapshotMapper, mock(AudienceDefinitionMapper.class), new ObjectMapper(), 10_000);

    assertThatThrownBy(() -> service.users(501L))
            .hasMessageContaining("Audience snapshot not found");
}
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest
```

Expected: FAIL because `users` and `contains` are missing.

- [x] **Step 3: Implement read methods**

Add to `AudienceSnapshotService`:

```java
public List<String> users(Long snapshotId) {
    AudienceSnapshotDO snapshot = snapshotMapper.selectById(snapshotId);
    if (snapshot == null) {
        throw new IllegalArgumentException("Audience snapshot not found: " + snapshotId);
    }
    try {
        return objectMapper.readValue(snapshot.getUserIdsJson(), new TypeReference<List<String>>() {});
    } catch (Exception e) {
        throw new IllegalStateException("Audience snapshot parse failed: " + snapshotId, e);
    }
}

public boolean contains(Long snapshotId, String userId) {
    return users(snapshotId).contains(userId);
}
```

- [x] **Step 4: Run snapshot service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest
```

Expected: PASS.

### Task 2: TAGGER Fan-Out And Membership Branching

**Files:**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java`

- [x] **Step 1: Add TAGGER runtime tests**

Add to `TaggerHandlerTest`:

```java
@Test
void scheduledBatchStaticAudienceFansOutFromSnapshot() {
    AudienceSnapshotService snapshotService = Mockito.mock(AudienceSnapshotService.class);
    AudienceUserResolver audienceUserResolver = Mockito.mock(AudienceUserResolver.class);
    CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
    TaggerHandler handler = new TaggerHandler(
            Mockito.mock(AudienceBitmapStore.class),
            audienceUserResolver,
            executionService,
            snapshotService);
    when(snapshotService.users(501L)).thenReturn(List.of("locked-u1", "locked-u2"));
    when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean()))
            .thenReturn(Mono.just(Map.of()));

    ExecutionContext ctx = scheduledBatchContext();
    NodeResult result = handler.executeAsync(Map.of(
            MapFieldKeys.NODE_ID_INTERNAL, "audience",
            "mode", "audience",
            "audienceId", 101,
            "audienceSnapshotId", 501,
            "hitNextNodeId", "send_coupon"
    ), ctx).block();

    assertThat(result.output()).containsEntry("fanoutCount", 2);
    Mockito.verify(snapshotService).users(501L);
    Mockito.verifyNoInteractions(audienceUserResolver);
}

@Test
void scheduledBatchDynamicAudienceUsesCurrentResolver() {
    AudienceSnapshotService snapshotService = Mockito.mock(AudienceSnapshotService.class);
    AudienceUserResolver audienceUserResolver = Mockito.mock(AudienceUserResolver.class);
    CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
    TaggerHandler handler = new TaggerHandler(
            Mockito.mock(AudienceBitmapStore.class),
            audienceUserResolver,
            executionService,
            snapshotService);
    when(audienceUserResolver.resolve(101L)).thenReturn(List.of("fresh-u1"));
    when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean()))
            .thenReturn(Mono.just(Map.of()));

    NodeResult result = handler.executeAsync(Map.of(
            MapFieldKeys.NODE_ID_INTERNAL, "audience",
            "mode", "audience",
            "audienceId", 101
    ), scheduledBatchContext()).block();

    assertThat(result.output()).containsEntry("fanoutCount", 1);
    Mockito.verify(audienceUserResolver).resolve(101L);
    Mockito.verifyNoInteractions(snapshotService);
}

@Test
void realtimeStaticAudienceChecksSnapshotMembership() {
    AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
    AudienceSnapshotService snapshotService = Mockito.mock(AudienceSnapshotService.class);
    TaggerHandler handler = new TaggerHandler(
            bitmapStore,
            Mockito.mock(AudienceUserResolver.class),
            Mockito.mock(CanvasExecutionService.class),
            snapshotService);
    when(snapshotService.contains(501L, "u1")).thenReturn(true);
    ExecutionContext ctx = new ExecutionContext();
    ctx.setUserId("u1");

    NodeResult result = handler.executeAsync(Map.of(
            "mode", "audience",
            "audienceId", 101,
            "audienceSnapshotId", 501,
            "hitNextNodeId", "hit",
            "missNextNodeId", "miss"
    ), ctx).block();

    assertThat(result.nextNodeId()).isEqualTo("hit");
    assertThat(result.output()).containsEntry("audienceSnapshotId", 501L);
    Mockito.verifyNoInteractions(bitmapStore);
}

private ExecutionContext scheduledBatchContext() {
    ExecutionContext ctx = new ExecutionContext();
    ctx.setCanvasId(62L);
    ctx.setExecutionId("batch-exec");
    ctx.setUserId("__scheduled_batch__:62:schedule");
    ctx.setTriggerPayload(Map.of(MapFieldKeys.SCHEDULED_BATCH, true));
    return ctx;
}
```

- [x] **Step 2: Run TAGGER tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TaggerHandlerTest
```

Expected: FAIL because `TaggerHandler` does not accept `AudienceSnapshotService`.

- [x] **Step 3: Inject snapshot service**

Modify the constructor:

```java
private final AudienceSnapshotService audienceSnapshotService;

@Autowired
public TaggerHandler(AudienceBitmapStore audienceBitmapStore,
                     AudienceUserResolver audienceUserResolver,
                     @Lazy CanvasExecutionService executionService,
                     AudienceSnapshotService audienceSnapshotService) {
    this.audienceBitmapStore = audienceBitmapStore;
    this.audienceUserResolver = audienceUserResolver;
    this.executionService = executionService;
    this.audienceSnapshotService = audienceSnapshotService;
}
```

- [x] **Step 4: Add snapshot helpers**

Add private helpers:

```java
private Long snapshotId(Map<String, Object> config, ExecutionContext ctx) {
    Object fromPayload = ctx == null || ctx.getTriggerPayload() == null
            ? null
            : ctx.getTriggerPayload().get("audienceSnapshotId");
    Object raw = fromPayload != null ? fromPayload : config.get("audienceSnapshotId");
    if (raw == null || String.valueOf(raw).isBlank()) {
        return null;
    }
    return Long.parseLong(String.valueOf(raw));
}

private List<String> resolveFanOutUsers(Map<String, Object> config, ExecutionContext ctx, Long audienceId) {
    Long snapshotId = snapshotId(config, ctx);
    if (snapshotId != null) {
        return audienceSnapshotService.users(snapshotId);
    }
    return audienceUserResolver.resolve(audienceId);
}
```

- [x] **Step 5: Branch membership and fan-out payload**

In non-batch audience mode:

```java
Long snapshotId = snapshotId(config, ctx);
boolean hit = snapshotId == null
        ? audienceBitmapStore.isMember(audienceId, ctx.getUserId())
        : audienceSnapshotService.contains(snapshotId, ctx.getUserId());
String nextNodeId = hit
        ? (String) config.get(MapFieldKeys.HIT_NEXT_NODE_ID)
        : (String) config.get(MapFieldKeys.MISS_NEXT_NODE_ID);
Map<String, Object> output = new LinkedHashMap<>();
output.put(MapFieldKeys.AUDIENCE_HIT, hit);
output.put(MapFieldKeys.AUDIENCE_ID, audienceId);
if (snapshotId != null) {
    output.put("audienceSnapshotId", snapshotId);
}
return Mono.just(NodeResult.ok(nextNodeId, output));
```

In scheduled fan-out, use `resolveFanOutUsers(config, ctx, audienceId)` and add snapshot ID to the child payload:

```java
Long snapshotId = snapshotId(config, ctx);
Map<String, Object> payload = new LinkedHashMap<>();
payload.put(MapFieldKeys.AUDIENCE_ID, audienceId);
payload.put("scheduledBatchExecutionId", ctx.getExecutionId());
if (snapshotId != null) {
    payload.put("audienceSnapshotId", snapshotId);
}
```

- [x] **Step 6: Run TAGGER tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest,TaggerHandlerTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Read: `docs/product-evolution/specs/p1-003c-tagger-audience-runtime-snapshot-branching.md`
- Read: `docs/product-evolution/plans/p1-003c-tagger-audience-runtime-snapshot-branching-plan.md`

- [x] **Step 1: Run focused backend suite**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest,TaggerHandlerTest
```

Expected: PASS.

### Verification Evidence

- P1-003C focused suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest,TaggerHandlerTest
```

Result: 14 tests, 0 failures, 0 errors, 0 skipped.

- P1-003B/P1-003C combined snapshot suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest,CanvasPublishAudienceSnapshotTest,TaggerHandlerTest
```

Result: 15 tests, 0 failures, 0 errors, 0 skipped.

- [x] **Step 2: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java \
  docs/product-evolution/specs/p1-003c-tagger-audience-runtime-snapshot-branching.md \
  docs/product-evolution/plans/p1-003c-tagger-audience-runtime-snapshot-branching-plan.md
git commit -m "feat: use audience snapshots in tagger runtime"
```

Expected: commit contains only runtime snapshot branching.
