# Trace → Doris Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Move execution trace data from MySQL to Apache Doris for 10-100x write throughput + compression.

**Architecture:** TraceWriteBuffer adds Doris Stream Load sink alongside existing MySQL sink. Dual-write during migration, then switch queries to Doris.

**Tech Stack:** Apache Doris 2.0, Docker, Java HTTP client (Doris Stream Load API)

---

### Task 1: Deploy Doris and Create Trace Table

**Files:**
- Modify: `docker-compose.yml`
- Create: `backend/canvas-engine/src/main/resources/infrastructure/doris/trace-ddl.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisDataSourceConfig.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisConnectionTest.java`

- [ ] **Step 1: Write failing test — verify Doris connectivity**

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DorisConnectionTest {
    @Autowired
    @Qualifier("dorisDataSource")
    private DataSource dorisDataSource;

    @Test
    void dorisJdbcConnects() {
        JdbcTemplate dorisJdbc = new JdbcTemplate(dorisDataSource);
        Integer result = dorisJdbc.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DorisConnectionTest -v`
Expected: FAIL — dorisDataSource bean not configured.

- [ ] **Step 3: Add Doris FE + BE to docker-compose.yml**

```yaml
  doris-fe:
    image: apache/doris:2.0.3-fe
    container_name: doris-fe
    ports:
      - "8030:8030"
      - "9030:9030"
    networks:
      - canvas-network
  doris-be:
    image: apache/doris:2.0.3-be
    container_name: doris-be
    ports:
      - "8040:8040"
    depends_on:
      - doris-fe
    networks:
      - canvas-network
```

- [ ] **Step 4: Create trace table DDL**

```sql
-- backend/canvas-engine/src/main/resources/infrastructure/doris/trace-ddl.sql
CREATE TABLE IF NOT EXISTS canvas_execution_trace (
    trace_id BIGINT,
    execution_id VARCHAR(64),
    node_id VARCHAR(64),
    node_type VARCHAR(32),
    node_name VARCHAR(128),
    status INT,
    input_data TEXT,
    output_data TEXT,
    error_msg TEXT,
    started_at DATETIME,
    finished_at DATETIME,
    duration_ms BIGINT,
    created_at DATETIME
)
DUPLICATE KEY(trace_id, execution_id)
DISTRIBUTED BY HASH(execution_id) BUCKETS 8
PROPERTIES ("replication_num" = "1");
```

- [ ] **Step 5: Create DorisDataSourceConfig to define the dorisDataSource bean**

```java
package org.chovy.canvas.infrastructure.doris;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DorisDataSourceConfig {

    @Value("${spring.datasource-doris.url}")
    private String url;

    @Value("${spring.datasource-doris.username}")
    private String username;

    @Value("${spring.datasource-doris.password}")
    private String password;

    @Value("${spring.datasource-doris.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    @Bean("dorisDataSource")
    public DataSource dorisDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        return ds;
    }
}
```

- [ ] **Step 6: Add Doris DataSource config to application.yml**

```yaml
spring:
  datasource-doris:
    url: jdbc:mysql://localhost:9030/canvas_db
    username: root
    password: ""
    driver-class-name: com.mysql.cj.jdbc.Driver

canvas:
  doris:
    stream-load-url: http://${DORIS_BE_HOST:localhost}:${DORIS_BE_PORT:8040}/api/canvas_db/canvas_execution_trace/_stream_load
```

- [ ] **Step 7: Start Doris, execute DDL, run test**

Run: `docker compose up -d doris-fe doris-be && sleep 30 && mysql -h127.0.0.1 -P9030 -uroot < backend/canvas-engine/src/main/resources/infrastructure/doris/trace-ddl.sql`
Then: `cd backend && mvn test -pl canvas-engine -Dtest=DorisConnectionTest -v`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add docker-compose.yml backend/canvas-engine/src/main/resources/infrastructure/doris/ backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/ backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: deploy Apache Doris, create trace table, and add DorisDataSourceConfig"
```

---

### Task 2: Add Doris Stream Load Sink to TraceWriteBuffer

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisStreamLoader.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisSinkTest.java`

- [ ] **Step 1: Write failing test — verify Doris sink writes data**

```java
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.doris.DorisStreamLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DorisSinkTest {
    @MockBean
    private CanvasExecutionTraceMapper mysqlMapper; // mock MySQL to avoid real DB writes

    @Autowired
    private DorisStreamLoader dorisStreamLoader;

    @Autowired
    @Qualifier("dorisDataSource")
    private DataSource dorisDataSource;

    /** Helper to create a trace matching CanvasExecutionTraceDO fields. */
    private CanvasExecutionTraceDO createTrace(long id, String executionId, String nodeId,
                                                String nodeType, int status) {
        return CanvasExecutionTraceDO.builder()
                .id(id)
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeType + "-" + nodeId)
                .status(status)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void flushToDoris_writesTraces() {
        List<CanvasExecutionTraceDO> traces = List.of(
            createTrace(1, "exec-100", "node-A", "CONDITION", 1),
            createTrace(2, "exec-100", "node-B", "ACTION", 1)
        );
        dorisStreamLoader.load(traces);

        // Verify data in Doris
        JdbcTemplate dorisJdbc = new JdbcTemplate(dorisDataSource);
        Integer count = dorisJdbc.queryForObject(
            "SELECT COUNT(*) FROM canvas_execution_trace WHERE execution_id = 'exec-100'", Integer.class);
        assertThat(count).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DorisSinkTest -v`
Expected: FAIL.

- [ ] **Step 3: Implement DorisStreamLoader**

```java
package org.chovy.canvas.infrastructure.doris;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DorisStreamLoader {
    @Value("${canvas.doris.stream-load-url:http://localhost:8040/api/canvas_db/canvas_execution_trace/_stream_load}")
    private String streamLoadUrl;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void load(List<CanvasExecutionTraceDO> traces) {
        String json = traces.stream()
            .map(this::toJsonRow)
            .collect(Collectors.joining("\n"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(streamLoadUrl))
            .header("Content-Type", "application/json")
            .header("label", "trace_" + UUID.randomUUID())
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Doris Stream Load failed: {}", response.body());
            }
        } catch (Exception e) {
            log.error("Doris Stream Load error", e);
        }
    }

    private String toJsonRow(CanvasExecutionTraceDO trace) {
        return String.format(
            "{\"trace_id\":%d,\"execution_id\":\"%s\",\"node_id\":\"%s\",\"node_type\":\"%s\"," +
            "\"node_name\":\"%s\",\"status\":%d,\"started_at\":\"%s\",\"finished_at\":\"%s\"," +
            "\"duration_ms\":%s,\"created_at\":\"%s\"}",
            trace.getId() != null ? trace.getId() : 0,
            trace.getExecutionId(),
            trace.getNodeId(),
            trace.getNodeType(),
            trace.getNodeName() != null ? trace.getNodeName() : "",
            trace.getStatus() != null ? trace.getStatus() : 0,
            trace.getStartedAt() != null ? trace.getStartedAt().format(DTF) : "",
            trace.getFinishedAt() != null ? trace.getFinishedAt().format(DTF) : "",
            trace.getDurationMs() != null ? trace.getDurationMs() : "null",
            trace.getFinishedAt() != null ? trace.getFinishedAt().format(DTF) : ""
        );
    }
}
```

- [ ] **Step 4: Update TraceWriteBuffer to add DorisStreamLoader and dual-write**

The existing `TraceWriteBuffer` uses `@RequiredArgsConstructor` with a single constructor parameter: `CanvasExecutionTraceMapper traceMapper`. Add `DorisStreamLoader` as a 2nd constructor parameter. The complete updated class:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceWriteBuffer {

    private static final int BATCH_SIZE   = 200;
    private static final int MAX_CAPACITY = 50_000;
    private static final int MAX_BATCHES_PER_FLUSH = 20;
    private static final double SAMPLING_THRESHOLD = 0.8;

    private final ConcurrentLinkedQueue<CanvasExecutionTraceDO> buffer = new ConcurrentLinkedQueue<>();
    private final CanvasExecutionTraceMapper traceMapper;
    private final DorisStreamLoader dorisStreamLoader;  // NEW: 2nd constructor param
    private final AtomicInteger pending = new AtomicInteger(0);
    private final AtomicLong samplingCounter = new AtomicLong(0);

    private final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("trace-flush").factory());
    private volatile Runnable flushCallback;

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

    public void offer(CanvasExecutionTraceDO trace) {
        int current = pending.incrementAndGet();
        if (current > MAX_CAPACITY) {
            pending.decrementAndGet();
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），丢弃轨迹", MAX_CAPACITY);
            return;
        }
        buffer.offer(trace);
    }

    public boolean addTrace(CanvasExecutionTraceDO trace, boolean critical) {
        if (critical) {
            // Critical events: never drop. Retry flush if buffer can't accept.
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

    public void flush() {
        if (flushCallback != null) flushCallback.run();
        if (pending.get() <= 0) return;

        for (int i = 0; i < MAX_BATCHES_PER_FLUSH; i++) {
            List<CanvasExecutionTraceDO> batch = drainBatch();
            if (batch.isEmpty()) return;

            // MySQL write (existing, fallback during migration)
            writeBatch(batch);

            // Doris write (async, non-blocking)
            try {
                dorisStreamLoader.load(batch);
            } catch (Exception e) {
                log.error("Doris trace write failed", e);
            }
        }
    }

    // ── Drain and write helpers ─────────────────────────────────────

    /** Drain up to BATCH_SIZE traces from the ConcurrentLinkedQueue for batch write.
     *  Uses poll() which is the correct API for ConcurrentLinkedQueue. */
    private List<CanvasExecutionTraceDO> drainBatch() {
        List<CanvasExecutionTraceDO> batch = new ArrayList<>();
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

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DorisSinkTest -v`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/
git add backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/
git commit -m "feat: add Doris Stream Load sink to TraceWriteBuffer (dual-write mode)"
```
