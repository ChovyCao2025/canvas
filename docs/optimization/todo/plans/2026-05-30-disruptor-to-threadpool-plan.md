# Disruptor → Virtual Thread Executor Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-step. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace LMAX Disruptor with virtual thread executor + Semaphore for task distribution.

**Architecture:** Virtual threads are managed by the JVM's ForkJoinPool and CANNOT be used with ThreadPoolExecutor. Use `Executors.newVirtualThreadPerTaskExecutor()` + `Semaphore` for concurrency control. Semaphore provides natural backpressure (block on permit acquisition vs Disruptor's InsufficientCapacityException).

**Tech Stack:** Java 21 virtual threads, Semaphore, Executors.newVirtualThreadPerTaskExecutor()

---

### Task 1: Write Failing Test for New Task Distribution

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasTaskDistributorTest.java`

- [ ] **Step 1: Write the failing test**

```java
import static org.awaitility.Awaitility.await;

@Test
void testPublish_executesTask() {
    CanvasTaskDistributor distributor = new CanvasTaskDistributor(4);
    AtomicBoolean executed = new AtomicBoolean(false);
    distributor.publish(() -> executed.set(true));
    await().atMost(1, TimeUnit.SECONDS).untilTrue(executed);
}

@Test
void testPublish_respectsConcurrencyLimit() throws InterruptedException {
    CanvasTaskDistributor distributor = new CanvasTaskDistributor(2); // max 2 concurrent
    AtomicInteger concurrent = new AtomicInteger(0);
    AtomicInteger maxConcurrent = new AtomicInteger(0);
    CountDownLatch allStarted = new CountDownLatch(2);
    CountDownLatch blocker = new CountDownLatch(1);

    for (int i = 0; i < 2; i++) {
        distributor.publish(() -> {
            int c = concurrent.incrementAndGet();
            maxConcurrent.updateAndGet(m -> Math.max(m, c));
            allStarted.countDown();
            try { blocker.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            concurrent.decrementAndGet();
        });
    }
    allStarted.await();
    assertThat(maxConcurrent.get()).isEqualTo(2);
    blocker.countDown();
}

@Test
void testShutdown() {
    CanvasTaskDistributor distributor = new CanvasTaskDistributor(4);
    distributor.shutdown();
    // No exception thrown
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=CanvasTaskDistributorTest -v`
Expected: FAIL.

- [ ] **Step 3: Implement CanvasTaskDistributor**

**CRITICAL:** Virtual threads CANNOT be used with ThreadPoolExecutor. Virtual threads are managed by the JVM's ForkJoinPool and should use `Executors.newVirtualThreadPerTaskExecutor()` instead. Concurrency is controlled by Semaphore.

```java
@Slf4j
public class CanvasTaskDistributor {

    /** Semaphore limits concurrent task execution (replaces ThreadPoolExecutor pool size). */
    private final Semaphore concurrencyLimiter;

    /** Virtual thread executor — unbounded, each task gets its own virtual thread. */
    private final ExecutorService virtualThreadExecutor;

    /**
     * @param maxConcurrency maximum number of tasks executing concurrently
     */
    public CanvasTaskDistributor(int maxConcurrency) {
        this.concurrencyLimiter = new Semaphore(maxConcurrency);
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Submit a task for execution. The task will acquire a semaphore permit before
     * running, blocking if all permits are in use (natural backpressure).
     */
    public void publish(Runnable task) {
        virtualThreadExecutor.submit(() -> {
            concurrencyLimiter.acquire();
            try {
                task.run();
            } finally {
                concurrencyLimiter.release();
            }
        });
    }

    public void shutdown() {
        virtualThreadExecutor.shutdown();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=CanvasTaskDistributorTest -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasTaskDistributor.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasTaskDistributorTest.java
git commit -m "feat: implement CanvasTaskDistributor with virtual threads + Semaphore"
```

---

### Task 2: Replace CanvasDisruptorService with CanvasTaskDistributor

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- Modify: `application.yml`
- Modify: `pom.xml`

**Location note:** CanvasDisruptorService is at `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`. The existing class has `WorkHandler<CanvasExecutionEvent>[] workers` and Disruptor publish logic. Replace the Disruptor publish call with `canvasTaskDistributor.publish(() -> { for (WorkHandler<CanvasExecutionEvent> handler : handlers) { handler.onEvent(event); } })`. If the existing class uses a single event consumer, replace with `canvasTaskDistributor.publish(() -> eventConsumer.consume(event))`.

- [ ] **Step 1: Update CanvasDisruptorService to delegate to CanvasTaskDistributor**

```java
@Service
public class CanvasDisruptorService {
    private final CanvasTaskDistributor distributor;

    // The existing CanvasDisruptorService has WorkHandler<CanvasExecutionEvent>[] workers.
    // Replace the Disruptor publish with canvasTaskDistributor.publish(() -> handler.onEvent(event))
    // where handler is the existing work handler.
    @Autowired
    private CanvasEventConsumer eventConsumer;

    @PostConstruct
    public void init() {
        // Read max concurrency from config (replaces consumerCount + queueCapacity)
        this.distributor = new CanvasTaskDistributor(maxConcurrency);
    }

    public void publish(CanvasExecutionEvent event) {
        // No event.reset() — new object each time, no stale data risk
        distributor.publish(() -> eventConsumer.consume(event));
    }
}
```

**Note:** `CanvasEventConsumer` (or the existing `WorkHandler` implementation) should be injected here. Replace `eventConsumer.consume(event)` with the actual method name if different. The `consumerCount` and `queueCapacity` fields are no longer applicable with virtual thread executor — only `maxConcurrency` is needed.

- [ ] **Step 2: Update application.yml**

```yaml
canvas:
  task-distributor:
    max-concurrency: 8
```

**Note:** `pool-size` and `queue-capacity` are replaced by single `max-concurrency` (Semaphore permits).

- [ ] **Step 3: Remove Disruptor dependency from pom.xml**

```xml
<!-- REMOVE -->
<dependency>
    <groupId>com.lmax</groupId>
    <artifactId>disruptor</artifactId>
</dependency>
```

- [ ] **Step 4: Build and run all tests**

Run: `cd backend && mvn test -pl canvas-engine`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/
git commit -m "feat: replace Disruptor with virtual thread + Semaphore based CanvasTaskDistributor"
```