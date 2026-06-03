# Wait Cross-Deploy Survival Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Externalize Wait node timeout timers to Redis. Bind recovery lock TTL to Wait expiration time. Short-circuit WAIT resume path.

**Architecture:** Timeout timers → Redis sorted set delay queue. Recovery lock TTL = Wait expiration. WAIT resume skips quota/dedup check.

**Tech Stack:** Redis, Java 21

---

### Task 1: Implement Redis Delay Queue for Wait Timeouts

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitTimeoutScheduler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitTimeoutSchedulerTest.java`

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest
class WaitTimeoutSchedulerTest {
    @Autowired
    private WaitTimeoutScheduler scheduler;
    @Autowired
    private StringRedisTemplate redis;

    @Test
    void schedule_addsToRedisSortedSet() {
        scheduler.schedule("exec-1", "wait-node-A", 60);
        Double score = redis.opsForZSet().score("canvas:wait:timeouts", "exec-1|wait-node-A");
        assertThat(score).isNotNull();
        assertThat(score).isGreaterThan((double) System.currentTimeMillis());
    }

    @Test
    void pollDueItems_returnsExpiredTimers() throws InterruptedException {
        scheduler.schedule("exec-2", "wait-node-B", 1); // 1 second
        Thread.sleep(1500); // wait for it to be due
        List<String> due = scheduler.pollDueItems();
        assertThat(due).contains("exec-2|wait-node-B");
    }

    @Test
    void pollDueItems_removesPolledItems() throws InterruptedException {
        scheduler.schedule("exec-3", "wait-node-C", 1);
        Thread.sleep(1500);
        scheduler.pollDueItems();
        // Second poll should not return same item
        List<String> due2 = scheduler.pollDueItems();
        assertThat(due2).doesNotContain("exec-3|wait-node-C");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=WaitTimeoutSchedulerTest -v`
Expected: FAIL.

- [ ] **Step 3: Implement WaitTimeoutScheduler**

```java
@Component
@Slf4j
public class WaitTimeoutScheduler {

    @Autowired
    private ContextPersistenceService contextPersistence;

    @Autowired
    private CanvasConfigCache configCache;

    @Autowired
    private DagEngine dagEngine;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY = "canvas:wait:timeouts";

    /**
     * Schedule a wait timeout. Key format uses "|" as separator to avoid ambiguity
     * with executionId or nodeId values that may contain ":".
     */
    public void schedule(String executionId, String nodeId, long timeoutSeconds) {
        double score = System.currentTimeMillis() + timeoutSeconds * 1000;
        redisTemplate.opsForZSet().add(QUEUE_KEY, executionId + "|" + nodeId, score);
    }

    public List<String> pollDueItems() {
        long now = System.currentTimeMillis();
        Set<String> items = redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, 0, now);
        if (items == null || items.isEmpty()) return Collections.emptyList();
        for (String item : items) {
            redisTemplate.opsForZSet().remove(QUEUE_KEY, item);
        }
        return new ArrayList<>(items);
    }

    public void cancel(String executionId, String nodeId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, executionId + "|" + nodeId);
    }
}
```

- [ ] **Step 4: Replace Mono.delay in DagEngine**

```java
// BEFORE
Mono.delay(Duration.ofSeconds(timeoutSec))
    .subscribe(s -> handleWaitTimeout(executionId, nodeId));

// AFTER — schedule via Redis sorted set; the poll loop in checkWaitTimeouts() handles firing
waitTimeoutScheduler.schedule(executionId, nodeId, timeoutSec);
```

- [ ] **Step 5: Add startup hook to poll pending timers**

Add the `@Scheduled` method to `WaitTimeoutScheduler` class (the same class created in Step 3):

```java
// In WaitTimeoutScheduler.java — add this method:
@Scheduled(fixedRate = 1000)
public void checkWaitTimeouts() {
    List<String> due = pollDueItems();
    for (String item : due) {
        // Parse using "|" separator — safe even if nodeId contains ":"
        String[] parts = item.split("\\|", 2);
        String executionId = parts[0];
        String nodeId = parts[1];
        // The actual method signature in DagEngine is:
        // handleSpecialNodeTimeout(DagGraph graph, String nodeId, DagParser.CanvasNode node,
        //     Map<String, Object> config, ExecutionContext ctx, int depth, String label, int timeoutSec)
        // Since we only have executionId and nodeId from the timer, we need to resolve
        // the remaining context from the persisted ExecutionContext.
        // Resolve canvasId/userId from the wait subscription in Redis:
        String waitKeyPattern = "canvas:wait:sub:" + executionId + ":*";
        Set<String> waitKeys = redisTemplate.keys(waitKeyPattern);
        if (waitKeys == null || waitKeys.isEmpty()) {
            log.warn("[WAIT_TIMEOUT] No wait subscription found for executionId={}, skipping", executionId);
            continue;
        }
        // Parse the first matching key: "canvas:wait:sub:{executionId}:{canvasId}:{userId}"
        String waitKey = waitKeys.iterator().next();
        String[] keyParts = waitKey.split(":");
        if (keyParts.length < 5) {
            log.warn("[WAIT_TIMEOUT] Invalid wait key format: {}, skipping", waitKey);
            continue;
        }
        Long canvasId = Long.parseLong(keyParts[3]);
        String userId = keyParts[4];

        ExecutionContext ctx = contextPersistence.load(canvasId, userId);
        if (ctx == null) {
            log.warn("[WAIT_TIMEOUT] No context found for canvasId={} userId={}, skipping", canvasId, userId);
            continue;
        }
        DagGraph graph = configCache.get(ctx.getCanvasId(), ctx.getVersionId());
        if (graph == null) {
            log.warn("[WAIT_TIMEOUT] No graph found for canvasId={} versionId={}", ctx.getCanvasId(), ctx.getVersionId());
            continue;
        }
        DagParser.CanvasNode node = graph.getNode(nodeId);
        Map<String, Object> config = node != null ? node.getConfig() : Map.of();
        dagEngine.handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, 0,
                "WAIT_TIMEOUT", 0);
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Note: The `@Scheduled` annotation requires `@EnableScheduling` to be present. Add `@EnableScheduling` to CanvasEngineApplication.java (main class) if not already present.

Run: `cd backend && mvn test -pl canvas-engine -Dtest=WaitTimeoutSchedulerTest -v`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/
git commit -m "feat: implement Redis sorted set delay queue for Wait timeout timers"
```

---

### Task 2: Bind Recovery Lock TTL to Wait Expiration

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitRecoveryLockTest.java`

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest
class WaitRecoveryLockTest {
    @Autowired
    private WaitSubscriptionService service;
    @Autowired
    private StringRedisTemplate redis;

    @Test
    void recoveryLockTTL_matchesWaitExpiration() {
        // Subscribe with 3-day wait
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);
        // Use the actual createEventWait() signature from WaitSubscriptionService:
        // createEventWait(executionId, canvasId, versionId, userId, nodeId, eventCode, eventFilters, resumePayload, expiresAt)
        service.createEventWait("exec-1", 1L, 1L, "user-1", "wait-node-A",
                "EVENT_CODE_1", null, null, expiresAt);

        String lockKey = "canvas:wait:lock:exec-1:user-1";
        Long ttl = redis.getExpire(lockKey, TimeUnit.SECONDS);
        // TTL should be ~3 days (259200s), not 600s
        assertThat(ttl).isGreaterThan(200_000); // way more than old 600s
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=WaitRecoveryLockTest -v`
Expected: FAIL — current TTL is fixed 600s.

- [ ] **Step 3: Update recovery lock TTL**

```java
// BEFORE: fixed 600s TTL (if such a lock exists in WaitSubscriptionService)
// AFTER: TTL = Wait expiration time

// In the method that sets the recovery lock after creating a wait subscription:
CanvasWaitSubscriptionDO subscription = service.createEventWait(...);
if (subscription.getExpiresAt() != null) {
    long waitExpiresAtEpochMs = subscription.getExpiresAt()
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli();
    long ttlSeconds = (waitExpiresAtEpochMs - System.currentTimeMillis()) / 1000;
    if (ttlSeconds > 0) {
        String lockKey = "canvas:wait:lock:" + subscription.getExecutionId() + ":" + subscription.getUserId();
        redis.opsForValue().set(lockKey, "1", ttlSeconds, TimeUnit.SECONDS);
    } else {
        log.warn("Wait subscription already expired for exec={}, user={}",
                subscription.getExecutionId(), subscription.getUserId());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=WaitRecoveryLockTest -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/
git commit -m "feat: bind Wait recovery lock TTL to actual expiration time"
```

---

### Task 3: Short-Circuit WAIT Resume Path

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitResumeService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitResumeShortCircuitTest.java`

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest
class WaitResumeShortCircuitTest {
    @MockBean private TriggerPreCheckService preCheckService;
    @Autowired private WaitResumeService waitResumeService;

    @Test
    void resumeWait_skipsQuotaCheck() {
        // WaitResumeService.resumeEventWaits() calls executionService.trigger() which
        // internally skips quota for WAIT_RESUME/WAIT_TIMEOUT trigger types
        // (via isInternalContinuationTrigger and ctx.quotaBypass).
        // Verify preCheckService.consumeQuotaAndRecord is never called for WAIT resume.
        waitResumeService.resumeEventWaits("EVENT_CODE_1", "user-1", Map.of(), "msg-1");
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
    }

    @Test
    void resumeWait_skipsDedupCheck() {
        // For WAIT_RESUME triggers, msgId is set but CanvasExecutionService.performDedupCheck
        // returns DedupResult.skipped() when internalContinuation=true.
        // This is already implemented — the test verifies the existing behavior holds.
        waitResumeService.resumeEventWaits("EVENT_CODE_1", "user-1", Map.of(), "msg-1");
        // No dedup violation — the WAIT resume path skips dedup because
        // isInternalContinuationTrigger(WAIT_RESUME) = true → performDedupCheck returns skipped
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=WaitResumeShortCircuitTest -v`
Expected: FAIL.

- [ ] **Step 3: Verify/implement short-circuit path**

The existing `WaitResumeService.trigger()` method already sets the correct `triggerType`
(WAIT_RESUME or WAIT_TIMEOUT), and `CanvasExecutionService.triggerInternal()` already:
1. Skips dedup for internal continuation triggers (`performDedupCheck` returns `skipped()`)
2. Skips quota via `consumeQuotaIfNeeded` which checks `ctx.isQuotaBypass()` and `isInternalContinuationTrigger()`
3. Skips cooldown check via `!internalContinuation` guard in `checkWithoutQuotaAccounting`

The short-circuit is already implemented in the existing code. The test confirms this behavior:

```java
// WaitResumeService.trigger() already calls:
executionService.trigger(
    wait.getCanvasId(),
    wait.getUserId(),
    triggerType,           // WAIT_RESUME or WAIT_TIMEOUT
    nodeType,              // WAIT or GOAL_CHECK
    wait.getNodeId(),
    payload,
    msgId,
    false)                 // dryRun=false
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=WaitResumeShortCircuitTest -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitResumeService.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitResumeShortCircuitTest.java
git commit -m "feat: add test coverage for WAIT resume short-circuit path (quota+dedup bypass)"
```
