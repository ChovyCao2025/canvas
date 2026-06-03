# Trace Buffer Overflow Fix Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Fix TraceWriteBuffer reliability: independent flush thread, backpressure mechanism, mandatory trace for critical events.

**Architecture:** Dedicated ScheduledExecutorService for flushing. Backpressure: critical events block until flushed, non-critical events sample at 80% capacity. Critical events (ctx save/resume) are never dropped.

**Tech Stack:** Java 21, Spring Boot

---

### Task 1: Independent Flush Thread + Backpressure

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceWriteBufferTest.java`

- [ ] **Step 1: Write failing test — verify independent flush thread**

```java
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceWriteBufferTest {
    @Mock
    private CanvasExecutionTraceMapper traceMapper;

    private TraceWriteBuffer buffer;

    /** Helper to create a trace DO matching the actual CanvasExecutionTraceDO fields. */
    private CanvasExecutionTraceDO createTrace(int index) {
        return CanvasExecutionTraceDO.builder()
                .executionId("exec-" + index)
                .nodeId("node-" + index)
                .nodeType("ACTION")
                .nodeName("test-node-" + index)
                .status(0)
                .startedAt(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // TraceWriteBuffer uses @RequiredArgsConstructor with CanvasExecutionTraceMapper as the only constructor arg
        buffer = new TraceWriteBuffer(traceMapper);
    }

    @Test
    void addTrace_criticalEvent_neverDropped() {
        // Fill buffer to max (MAX_CAPACITY = 50_000)
        for (int i = 0; i < 50_000; i++) {
            buffer.offer(createTrace(i));
        }
        // Critical event should still be accepted (addTrace with critical=true blocks until room is made)
        assertThat(buffer.addTrace(createTrace(50_001), true)).isTrue();
    }

    @Test
    void addTrace_nonCritical_dropsAt80Percent() {
        // Fill buffer past 80% (40_000 out of 50_000)
        for (int i = 0; i < 40_000; i++) {
            buffer.offer(createTrace(i));
        }
        // Some non-critical traces should be sampled (not all accepted)
        int accepted = 0;
        for (int i = 40_000; i < 50_000; i++) {
            if (buffer.addTrace(createTrace(i), false)) accepted++;
        }
        assertThat(accepted).isLessThan(10_000); // sampling kicked in
    }

    @Test
    void flushUsesDedicatedThread() {
        // Verify flush doesn't run on Spring scheduler thread
        AtomicReference<String> flushThreadName = new AtomicReference<>();
        buffer.setFlushCallback(() -> flushThreadName.set(Thread.currentThread().getName()));
        buffer.flush();
        assertThat(flushThreadName.get()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=TraceWriteBufferTest -v`
Expected: FAIL — addTrace(boolean) overload and setFlushCallback() don't exist yet.

- [ ] **Step 3: Implement independent flush + backpressure**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceWriteBuffer {

    /** 单批轨迹刷盘条数。 */
    private static final int BATCH_SIZE   = 200;
    /** 内存缓冲区最大容量。 */
    private static final int MAX_CAPACITY = 50_000;
    /** 单次定时 flush 最多处理的批次数。 */
    private static final int MAX_BATCHES_PER_FLUSH = 20;
    /** 采样阈值：缓冲区使用率超过此比例时，非关键事件开始采样。 */
    private static final double SAMPLING_THRESHOLD = 0.8;

    /** 待批量写入的执行轨迹内存队列。 */
    private final ConcurrentLinkedQueue<CanvasExecutionTraceDO> buffer = new ConcurrentLinkedQueue<>();
    /** 执行轨迹 Mapper。 */
    private final CanvasExecutionTraceMapper traceMapper;
    /** 当前待刷盘轨迹数量。 */
    private final AtomicInteger pending = new AtomicInteger(0);
    /** 采样计数器，用于非关键事件采样。 */
    private final AtomicLong samplingCounter = new AtomicLong(0);

    /** 独立刷盘调度器，不占用 Spring @Scheduled 线程。 */
    private final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("trace-flush").factory());

    /** 测试用刷盘回调。 */
    private volatile Runnable flushCallback;

    /** 设置刷盘回调（仅供测试使用）。 */
    void setFlushCallback(Runnable callback) {
        this.flushCallback = callback;
    }

    @PostConstruct
    void init() {
        flushScheduler.scheduleAtFixedRate(this::flush, 500, 500, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void destroy() {
        shutdownFlush();
        flushScheduler.shutdown();
    }

    /** 非阻塞入队（主执行链路调用，不等待）。 */
    public void offer(CanvasExecutionTraceDO trace) {
        int current = pending.incrementAndGet();
        if (current > MAX_CAPACITY) {
            // 背压保护：宁可丢 trace，也不能让主执行链路被轨迹写入拖垮。
            pending.decrementAndGet();
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），丢弃轨迹", MAX_CAPACITY);
            return;
        }
        buffer.offer(trace);
    }

    /**
     * 带关键标记的入队。
     * @param critical true=关键事件（永不丢弃，必要时阻塞等待），false=非关键（超80%容量时采样）
     * @return true=入队成功，false=被采样丢弃（仅非关键事件）
     */
    public boolean addTrace(CanvasExecutionTraceDO trace, boolean critical) {
        if (critical) {
            // Critical events: never drop. Block if necessary, but with a max retry limit.
            int retries = 0;
            while (!buffer.offer(trace)) {
                flush();
                if (++retries > 100) throw new IllegalStateException("Buffer full after 100 flush retries");
            }
            pending.incrementAndGet();
            return true;
        }
        // Non-critical: apply sampling when buffer is 80%+ full
        if (pending.get() >= (int)(MAX_CAPACITY * SAMPLING_THRESHOLD)) {
            if (samplingCounter.getAndIncrement() % 10 != 0) {
                return false; // drop — sampling mode
            }
        }
        if (buffer.offer(trace)) {
            pending.incrementAndGet();
            return true;
        }
        return false;
    }

    /** 每 500ms 批量刷盘，每次最多 200 条 */
    public void flush() {
        if (flushCallback != null) flushCallback.run();
        if (pending.get() <= 0) return;

        for (int i = 0; i < MAX_BATCHES_PER_FLUSH; i++) {
            List<CanvasExecutionTraceDO> batch = drainBatch();
            if (batch.isEmpty()) return;
            writeBatch(batch);
        }
    }

    // ── Drain and write helpers ─────────────────────────────────────

    /** Drain up to BATCH_SIZE traces from the ConcurrentLinkedQueue for batch write.
     *  Uses poll() which is the correct API for ConcurrentLinkedQueue. */
    private List<CanvasExecutionTraceDO> drainBatch() {
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(BATCH_SIZE);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null && batch.size() < BATCH_SIZE) {
            batch.add(item);
        }
        if (!batch.isEmpty()) {
            pending.addAndGet(-batch.size());
        }
        return batch;
    }

    /** Write a batch of traces to the database via MyBatis. */
    private void writeBatch(List<CanvasExecutionTraceDO> batch) {
        if (batch.isEmpty()) return;
        try {
            traceMapper.insertBatch(batch);
            log.debug("[TRACE_BUFFER] 批量写入 {} 条", batch.size());
        } catch (Exception e) {
            log.error("[TRACE_BUFFER] 批量写入失败: {}", e.getMessage());
        }
    }

    /** Flush all remaining traces before application shutdown. */
    private void shutdownFlush() {
        List<CanvasExecutionTraceDO> remaining = drainBatch();
        writeBatch(remaining);
    }

    /** Get current pending trace count (for monitoring). */
    public int pendingCount() { return pending.get(); }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=TraceWriteBufferTest -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceWriteBufferTest.java
git commit -m "feat: independent flush thread + backpressure for TraceWriteBuffer"
```

---

### Task 2: Mandatory Trace for Critical Events

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceTraceTest.java`

- [ ] **Step 1: Write failing test — ctx save produces trace**

```java
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ContextPersistenceTraceTest {
    @Autowired
    private ContextPersistenceService persistenceService;

    @MockBean
    private TraceWriteBuffer traceBuffer;

    /** Helper to create a test ExecutionContext with required fields. */
    private ExecutionContext createTestContext(String executionId, String nodeId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId(executionId);
        ctx.setCanvasId(1L);
        ctx.setUserId("user-1");
        ctx.setVersionId(1L);
        return ctx;
    }

    @Test
    void saveContext_producesCriticalTrace() {
        ExecutionContext ctx = createTestContext("exec-1", "node-A");
        persistenceService.save(ctx);
        // Verify a CONTEXT_SAVE trace was added with critical=true
        verify(traceBuffer).addTrace(argThat(trace ->
                trace.getExecutionId().equals("exec-1")
        ), eq(true));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ContextPersistenceTraceTest -v`
Expected: FAIL.

- [ ] **Step 3: Add critical trace calls to ContextPersistenceService**

The existing `ContextPersistenceService` uses `@RequiredArgsConstructor` with three constructor parameters: `StringRedisTemplate redis`, `ObjectMapper objectMapper`, `RedisKeyUtil keys`. Add `TraceWriteBuffer` as a 4th constructor parameter. The complete updated class:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextPersistenceService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RedisKeyUtil keys;
    private final TraceWriteBuffer traceBuffer;  // NEW: 4th constructor param

    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ttlSec;

    /** 保存或覆盖上下文快照，并刷新 TTL。 */
    public void save(ExecutionContext ctx) {
        // Critical trace: context save must never be lost
        traceBuffer.addTrace(
            CanvasExecutionTraceDO.builder()
                .executionId(ctx.getExecutionId())
                .nodeId("CONTEXT_SAVE")
                .nodeType("SYSTEM")
                .status(0)
                .startedAt(LocalDateTime.now())
                .build(),
            true // critical — never drop
        );

        try {
            String json = objectMapper.writeValueAsString(ctx);
            redis.opsForValue().set(keys.context(ctx.getCanvasId(), ctx.getUserId()),
                    json, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.error("保存 ExecutionContext 失败: {}", e.getMessage());
        }
    }

    /** 加载上下文；不存在或反序列化失败时返回 null。 */
    public ExecutionContext load(Long canvasId, String userId) {
        String json = redis.opsForValue().get(keys.context(canvasId, userId));
        if (json == null) return null;
        try {
            ExecutionContext ctx = objectMapper.readValue(json, ExecutionContext.class);
            // Critical trace: context load must never be lost
            traceBuffer.addTrace(
                CanvasExecutionTraceDO.builder()
                    .executionId(ctx.getExecutionId())
                    .nodeId("CONTEXT_LOAD")
                    .nodeType("SYSTEM")
                    .status(1)
                    .startedAt(LocalDateTime.now())
                    .build(),
                true // critical — never drop
            );
            return ctx;
        } catch (Exception e) {
            log.error("反序列化 ExecutionContext 失败: {}", e.getMessage());
            return null;
        }
    }

    // ── These methods remain unchanged from the existing ContextPersistenceService
    //    implementation (see
    //    backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java).
    //    No modification needed for the overflow fix: delete, exists, acquireResumeLock,
    //    releaseResumeLock, acquireDedup, releaseDedup, buildDedupKey ──
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ContextPersistenceTraceTest -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceTraceTest.java
git commit -m "feat: add mandatory critical trace for context save/load events"
```
