# Audience Streaming Load Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Replace full List<String> loading with streaming/cursor-based processing. Fan-out trigger uses batch submission with rate limiting.

> **Note:** `JdbcConfigResolver`, `SqlWhereGenerator`, and `JdbcConfig` are **existing types** in the codebase (`org.chovy.canvas.engine.audience` package). See:
> - `JdbcConfigResolver` — resolves JSON data source config into `JdbcConfig` records
> - `SqlWhereGenerator` — generates SQL WHERE clauses from rule JSON, returns `SqlWhere` with `sql()` and `params()`
> - `JdbcConfig` — record with fields: `id`, `baseTable`, `url`, `username`, `password`, `userIdColumn`, `driverClassName`, `maxRows`
> - `CdpAudienceSourceService` — CDP audience resolution for non-JDBC sources

**Architecture:** JDBC ResultSet cursor with TYPE_FORWARD_ONLY + CONCUR_READ_ONLY + setFetchSize(Integer.MIN_VALUE) for true streaming. Fan-out submits batches of N users at a time with Semaphore-based rate limiting.

**Tech Stack:** JDBC ResultSet, Java 21 virtual threads, Semaphore, RoaringBitmap

---

### Task 1: Rewrite AudienceUserResolver with JDBC Cursor Streaming

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceUserResolver.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceUserResolverStreamingTest.java`

- [ ] **Step 1: Write failing test for streaming resolution**

```java
package org.chovy.canvas.engine.audience;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudienceUserResolverStreamingTest {

    @Mock
    private AudienceDefinitionMapper definitionMapper;
    @Mock
    private CdpAudienceSourceService cdpAudienceSourceService;
    @Mock
    private JdbcConfigResolver jdbcConfigResolver;
    @Mock
    private SqlWhereGenerator sqlWhereGenerator;
    @Mock
    private DataSource mockDataSource;
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    @Test
    void testStreamingResolve_processesUsersWithoutLoadingAllIntoMemory() throws Exception {
        // Setup: 10,000 users in mock ResultSet
        when(definitionMapper.selectById(1L)).thenReturn(AudienceDefinitionDO.builder()
                .id(1L)
                .dataSourceType("JDBC")
                .dataSourceConfig("{\"url\":\"jdbc:h2:mem:test\"}")
                .enabled(1)
                .build());
        
        when(jdbcConfigResolver.resolve(any())).thenReturn(
                new JdbcConfig(1L, "users", "jdbc:h2:mem:test", "sa", "", "user_id", "org.h2.Driver", null));
        when(sqlWhereGenerator.generate(any())).thenReturn(
                new SqlWhereGenerator.SqlWhere("1=1", new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()));
        
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any(String.class), any(int.class), any(int.class)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Simulate 10,000 rows
        AtomicInteger rowCount = new AtomicInteger(0);
        when(mockResultSet.next()).thenAnswer(inv -> {
            int current = rowCount.incrementAndGet();
            return current <= 10_000;
        });
        when(mockResultSet.getString(1)).thenAnswer(inv -> "user-" + rowCount.get());
        when(mockResultSet.isClosed()).thenReturn(false);
        
        AudienceUserResolver resolver = new AudienceUserResolver(
                definitionMapper, cdpAudienceSourceService, jdbcConfigResolver, sqlWhereGenerator);
        
        // Test: Process users one at a time via Consumer
        List<String> processedUsers = new ArrayList<>();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int count = resolver.resolveAndProcess(1L, processedUsers::add);
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Verify: All users processed, memory increase < 5MB (not 200MB+ for full List)
        assertThat(count).isEqualTo(10_000);
        assertThat(processedUsers).hasSize(10_000);
        assertThat(memoryAfter - memoryBefore).isLessThan(5 * 1024 * 1024L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=AudienceUserResolverStreamingTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[ERROR] Failed to execute goal ... AudienceUserResolverStreamingTest.testStreamingResolve_processesUsersWithoutLoadingAllIntoMemory
```

- [ ] **Step 3: Add resolveAndProcess method to AudienceUserResolver**

```java
package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Resolves an audience definition into concrete user IDs for scheduled batch fan-out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceUserResolver {

    private final AudienceDefinitionMapper definitionMapper;
    private final CdpAudienceSourceService cdpAudienceSourceService;
    private final JdbcConfigResolver jdbcConfigResolver;
    private final SqlWhereGenerator sqlWhereGenerator;

    /**
     * Legacy method: loads all user IDs into memory.
     * @deprecated Use {@link #resolveAndProcess(Long, Consumer)} for large audiences.
     */
    @Deprecated
    public List<String> resolve(Long audienceId) {
        List<String> userIds = new ArrayList<>();
        resolveAndProcess(audienceId, userIds::add);
        return userIds;
    }

    /**
     * Streaming resolution: processes each user ID via Consumer without loading all into memory.
     * Uses JDBC cursor streaming (TYPE_FORWARD_ONLY + CONCUR_READ_ONLY + fetchSize).
     *
     * @param audienceId the audience definition ID
     * @param processor  Consumer to process each user ID
     * @return total count of processed users
     */
    public int resolveAndProcess(Long audienceId, Consumer<String> processor) {
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
            throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
        }
        String sourceType = definition.getDataSourceType();
        if (cdpAudienceSourceService.supports(sourceType)) {
            List<String> userIds = cdpAudienceSourceService.resolveUserIds(sourceType, definition.getRuleJson());
            userIds.forEach(processor);
            return userIds.size();
        }
        if ("JDBC".equals(sourceType)) {
            return resolveJdbcStreaming(definition, processor);
        }
        throw new IllegalStateException("Unsupported audience source for scheduled fan-out: " + sourceType);
    }

    /**
     * JDBC streaming implementation using cursor-based ResultSet.
     * Key: TYPE_FORWARD_ONLY + CONCUR_READ_ONLY + setFetchSize(Integer.MIN_VALUE) for MySQL streaming.
     */
    private int resolveJdbcStreaming(AudienceDefinitionDO definition, Consumer<String> processor) {
        DataSource dataSource = null;
        try {
            JdbcConfig jdbcConfig = jdbcConfigResolver.resolve(definition.getDataSourceConfig());
            dataSource = DataSourceBuilder.create()
                    .driverClassName(jdbcConfig.driverClassName())
                    .url(jdbcConfig.url())
                    .username(jdbcConfig.username())
                    .password(jdbcConfig.password())
                    .build();
            
            SqlWhereGenerator.SqlWhere where = sqlWhereGenerator.generate(definition.getRuleJson());
            String sql = "SELECT " + jdbcConfig.userIdColumn() + " FROM " + jdbcConfig.baseTable() +
                    " WHERE " + where.sql();
            var params = where.params();
            if (jdbcConfig.maxRows() != null) {
                sql += " LIMIT ?";
            }
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                // MySQL streaming mode: Integer.MIN_VALUE forces server-side cursor
                ps.setFetchSize(Integer.MIN_VALUE);
                // Bind parameters with incrementing index
                int paramIndex = 1;
                for (String paramName : params.getParameterNames()) {
                    Object value = params.getValue(paramName);
                    if (value instanceof Integer i) ps.setInt(paramIndex++, i);
                    else if (value instanceof Long l) ps.setLong(paramIndex++, l);
                    else if (value instanceof String s) ps.setString(paramIndex++, s);
                    else ps.setObject(paramIndex++, value);
                }
                // Bind LIMIT ? for maxRows (if present)
                if (jdbcConfig.maxRows() != null) {
                    ps.setInt(paramIndex++, jdbcConfig.maxRows());
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        String userId = rs.getString(1);
                        if (userId != null && !userId.isBlank()) {
                            processor.accept(userId);
                            count++;
                        }
                    }
                    return count;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Resolve JDBC audience users failed: " + e.getMessage(), e);
        } finally {
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // best effort
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=AudienceUserResolverStreamingTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceUserResolver.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceUserResolverStreamingTest.java && git commit -m "feat: implement streaming audience user resolution with JDBC cursor to prevent OOM on large audiences"
```

---

### Task 2: Add Fan-Out Batch Submission with Semaphore Rate Limiting

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineFanOutBatchTest.java`

- [ ] **Step 1: Write failing test for batch fan-out**

```java
package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DagEngineFanOutBatchTest {

    @Test
    void testFanOutBatch_submitsInBatchesWithRateLimit() throws InterruptedException {
        // Setup: 100 users, batch size 10, max concurrent 3
        int batchSize = 10;
        int maxConcurrent = 3;
        List<String> allUsers = Stream.iterate(0, i -> i + 1)
                .limit(100)
                .map(i -> "user-" + i)
                .toList();
        
        // Track batch submissions
        CopyOnWriteArrayList<List<String>> submittedBatches = new CopyOnWriteArrayList<>();
        AtomicInteger maxInFlight = new AtomicInteger(0);
        AtomicInteger currentInFlight = new AtomicInteger(0);
        
        Semaphore semaphore = new Semaphore(maxConcurrent);
        
        // Simulate batch submission
        DagEngine.FanOutBatcher batcher = new DagEngine.FanOutBatcher(batchSize, maxConcurrent);
        batcher.fanOut(allUsers.stream(), batch -> {
            currentInFlight.incrementAndGet();
            maxInFlight.updateAndGet(current -> Math.max(current, currentInFlight.get()));
            submittedBatches.add(batch);
            try {
                Thread.sleep(10); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                currentInFlight.decrementAndGet();
            }
        });
        
        // Verify: 10 batches of 10 users each
        assertThat(submittedBatches).hasSize(10);
        assertThat(submittedBatches.get(0)).hasSize(10);
        assertThat(submittedBatches.stream().flatMap(List::stream).distinct()).hasSize(100);
        // Verify: max concurrent never exceeded 3
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(maxConcurrent);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DagEngineFanOutBatchTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[ERROR] ... DagEngineFanOutBatchTest.testFanOutBatch_submitsInBatchesWithRateLimit
```

- [ ] **Step 3: Add FanOutBatcher inner class to DagEngine**

Add this inner class to `DagEngine.java` after the existing inner classes, before the public methods (around line 850):

```java
    /**
     * Fan-out batch submission with Semaphore-based rate limiting.
     * Prevents overwhelming downstream systems when processing large audiences.
     */
    public static final class FanOutBatcher {
        private final int batchSize;
        private final int maxConcurrent;
        private final Semaphore semaphore;
        private final java.util.concurrent.ExecutorService executor;

        public FanOutBatcher(int batchSize, int maxConcurrent) {
            this.batchSize = batchSize;
            this.maxConcurrent = maxConcurrent;
            this.semaphore = new Semaphore(maxConcurrent);
            this.executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        }

        /**
         * Process a stream of user IDs in batches with rate limiting.
         *
         * @param userIds   Stream of user IDs to process
         * @param processor Consumer for each batch of user IDs
         */
        public void fanOut(java.util.stream.Stream<String> userIds, java.util.function.Consumer<java.util.List<String>> processor) {
            java.util.List<String> batch = new java.util.ArrayList<>(batchSize);
            java.util.concurrent.CountDownLatch latch;
            java.util.concurrent.atomic.AtomicInteger pending;
            
            // First pass: count total batches to initialize latch and pending correctly
            java.util.List<java.util.List<String>> allBatches = new java.util.ArrayList<>();
            java.util.List<String> current = new java.util.ArrayList<>(batchSize);
            userIds.forEach(userId -> {
                current.add(userId);
                if (current.size() >= batchSize) {
                    allBatches.add(new java.util.ArrayList<>(current));
                    current.clear();
                }
            });
            if (!current.isEmpty()) {
                allBatches.add(current);
            }
            
            int totalBatches = allBatches.size();
            if (totalBatches == 0) return;
            
            pending = new java.util.concurrent.atomic.AtomicInteger(totalBatches);
            latch = new java.util.concurrent.CountDownLatch(totalBatches);
            
            for (java.util.List<String> toSubmit : allBatches) {
                try {
                    semaphore.acquire();
                    executor.submit(() -> {
                        try {
                            processor.accept(toSubmit);
                        } finally {
                            semaphore.release();
                            if (pending.decrementAndGet() == 0) {
                                latch.countDown();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Fan-out interrupted", e);
                }
            }
            
            // Wait for all batches to complete
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Fan-out interrupted", e);
            }
        }

        public void shutdown() {
            executor.shutdown();
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DagEngineFanOutBatchTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineFanOutBatchTest.java && git commit -m "feat: add FanOutBatcher with Semaphore rate limiting for batch fan-out submission"
```
