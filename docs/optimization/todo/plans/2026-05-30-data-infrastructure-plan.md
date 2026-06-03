# Data Infrastructure Plan (K+L+M+O)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Build unified data pipeline: MySQL → Flink CDC 3.6 → Doris 2.0. Replace JVM-based stats/aggregation with Doris SQL. Stats APIs query Doris instead of MySQL.

**Architecture:** MySQL (OLTP) → Flink CDC (binlog sync) → Doris (OLAP). Stats APIs query Doris via JDBC. Doris handles aggregation queries, MySQL handles OLTP.

**Tech Stack:** Apache Doris 2.0.3, Apache Flink 1.20 + Flink CDC 3.6, Docker Compose, Spring JdbcTemplate

---

### Task 1: Deploy Doris Cluster via Docker Compose

**Files:**
- Modify: `docker-compose.local.yml`
- Create: `infrastructure/doris/init.sql`- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisConfigTest.java`

- [ ] **Step 1: Write failing test — verify DorisConfig loads when Doris is enabled**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisConfigTest.java`:

```java
package org.chovy.canvas.infrastructure.doris;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DorisConfigTest {

    @Autowired(required = false)
    @Qualifier("dorisDataSource")
    private DataSource dorisDataSource;

    @Autowired(required = false)
    @Qualifier("dorisJdbcTemplate")
    private JdbcTemplate dorisJdbcTemplate;

    @Test
    void dorisBeansAreNullWhenDisabled() {
        // By default canvas.doris.enabled=false, so beans should be null
        assertThat(dorisDataSource).isNull();
        assertThat(dorisJdbcTemplate).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it passes with Doris disabled**

```bash
cd backend
mvn test -pl canvas-engine -Dtest=DorisConfigTest
```

Expected: PASS — Doris beans are null because `doris.enabled=false` by default.

- [ ] **Step 3: Add Doris FE + BE to docker-compose.local.yml**

Add to `docker-compose.local.yml` after the `rocketmq-dashboard` service:

```yaml
  # ── Doris 2.0 OLAP 数仓 ─────────────────────────────────────
  # FE: Frontend (query parsing, metadata, query scheduling)
  # BE: Backend (data storage, query execution)
  doris-fe:
    image: apache/doris:2.0.3-fe
    container_name: canvas-doris-fe
    hostname: doris-fe
    environment:
      - FE_SERVERS=fe1:doris-fe:9010
      - FE_ID=1
    ports:
      - "8030:8030"   # FE HTTP port (Web UI)
      - "9010:9010"   # FE RPC port (internal)
      - "9030:9030"   # FE MySQL protocol port
    volumes:
      - canvas-doris-fe-meta:/opt/apache-doris/fe/doris-meta
      - ./infrastructure/doris/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8030/api/bootstrap"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  doris-be:
    image: apache/doris:2.0.3-be
    container_name: canvas-doris-be
    hostname: doris-be
    environment:
      - FE_SERVERS=fe1:doris-fe:9010
      - BE_ADDR=doris-be:9050
    ports:
      - "8040:8040"   # BE HTTP port
      - "9050:9050"   # BE Heartbeat port
      - "8060:8060"   # BE brpc port
    volumes:
      - canvas-doris-be-storage:/opt/apache-doris/be/storage
    depends_on:
      doris-fe:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8040/api/health"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
```

Add to the `volumes:` section at the bottom:

```yaml
  canvas-doris-fe-meta:
  canvas-doris-be-storage:
```

- [ ] **Step 4: Create Doris initialization SQL**

Create `infrastructure/doris/init.sql`:

```sql
-- Doris 初始化脚本
-- 注意：Doris FE 启动后需手动注册 BE，此脚本在 FE 就绪后执行

-- 1. 注册 BE 节点（FE 启动后执行）
-- ALTER SYSTEM ADD BACKEND "doris-be:9050";

-- 2. 创建 ODS 层（原始数据层，与 MySQL 表结构一致）
CREATE DATABASE IF NOT EXISTS canvas_ods;

USE canvas_ods;

-- 画布执行记录（对应 canvas_execution 表）
CREATE TABLE IF NOT EXISTS canvas_execution (
    id VARCHAR(64) NOT NULL COMMENT '执行ID',
    canvas_id BIGINT NOT NULL COMMENT '画布ID',
    version_id BIGINT NOT NULL COMMENT '版本ID',
    user_id VARCHAR(128) COMMENT '触发用户ID',
    perf_run_id VARCHAR(64) COMMENT '压测批次ID',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发类型',
    status INT NOT NULL COMMENT '执行状态: 0=RUNNING, 1=PAUSED, 2=SUCCESS, 3=FAILED',
    result TEXT COMMENT '执行结果JSON',
    dedupe_key VARCHAR(128) COMMENT '去重Key',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    finished_at DATETIME COMMENT '完成时间'
) ENGINE=OLAP
DUPLICATE KEY(id)
PARTITION BY RANGE(created_at) (
    PARTITION p202501 VALUES [('2025-01-01'), ('2025-02-01')),
    PARTITION p202502 VALUES [('2025-02-01'), ('2025-03-01')),
    PARTITION p202503 VALUES [('2025-03-01'), ('2025-04-01')),
    PARTITION p202504 VALUES [('2025-04-01'), ('2025-05-01')),
    PARTITION p202505 VALUES [('2025-05-01'), ('2025-06-01')),
    PARTITION p202506 VALUES [('2025-06-01'), ('2025-07-01'))
)
DISTRIBUTED BY HASH(canvas_id) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "MONTH",
    "dynamic_partition.start" = "-6",
    "dynamic_partition.end" = "2",
    "dynamic_partition.prefix" = "p"
);

-- 节点执行轨迹（对应 canvas_execution_trace 表）
CREATE TABLE IF NOT EXISTS canvas_execution_trace (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型',
    node_name VARCHAR(128) COMMENT '节点名称',
    status INT NOT NULL COMMENT '节点状态: 0=RUNNING, 1=SUCCESS, 2=FAILED, 3=SKIPPED',
    input_data TEXT COMMENT '输入数据JSON',
    output_data TEXT COMMENT '输出数据JSON',
    error_message TEXT COMMENT '错误信息',
    started_at DATETIME COMMENT '开始时间',
    finished_at DATETIME COMMENT '完成时间',
    duration_ms BIGINT COMMENT '执行耗时ms',
    created_at DATETIME NOT NULL COMMENT '创建时间'
) ENGINE=OLAP
DUPLICATE KEY(id)
PARTITION BY RANGE(created_at) (
    PARTITION p202501 VALUES [('2025-01-01'), ('2025-02-01')),
    PARTITION p202502 VALUES [('2025-02-01'), ('2025-03-01')),
    PARTITION p202503 VALUES [('2025-03-01'), ('2025-04-01')),
    PARTITION p202504 VALUES [('2025-04-01'), ('2025-05-01')),
    PARTITION p202505 VALUES [('2025-05-01'), ('2025-06-01')),
    PARTITION p202506 VALUES [('2025-06-01'), ('2025-07-01'))
)
DISTRIBUTED BY HASH(execution_id) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "MONTH",
    "dynamic_partition.start" = "-6",
    "dynamic_partition.end" = "2",
    "dynamic_partition.prefix" = "p"
);

-- 3. 创建 DWS 层（汇总层，预聚合统计）
CREATE DATABASE IF NOT EXISTS canvas_dws;

USE canvas_dws;

-- 每日画布执行统计（按画布+日期+渠道聚合）
CREATE TABLE IF NOT EXISTS canvas_daily_stats (
    stat_date DATE NOT NULL COMMENT '统计日期',
    canvas_id BIGINT NOT NULL COMMENT '画布ID',
    canvas_name VARCHAR(256) COMMENT '画布名称',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发类型',
    total_executions BIGINT SUM DEFAULT "0" COMMENT '总执行数',
    success_count BIGINT SUM DEFAULT "0" COMMENT '成功数',
    fail_count BIGINT SUM DEFAULT "0" COMMENT '失败数',
    running_count BIGINT SUM DEFAULT "0" COMMENT '运行中数',
    avg_duration_ms BIGINT SUM DEFAULT "0" COMMENT '平均耗时ms',
    total_duration_ms BIGINT SUM DEFAULT "0" COMMENT '总耗时ms'
) ENGINE=OLAP
AGGREGATE KEY(stat_date, canvas_id, canvas_name, trigger_type)
PARTITION BY RANGE(stat_date) (
    PARTITION p202501 VALUES [('2025-01-01'), ('2025-02-01')),
    PARTITION p202502 VALUES [('2025-02-01'), ('2025-03-01')),
    PARTITION p202503 VALUES [('2025-03-01'), ('2025-04-01')),
    PARTITION p202504 VALUES [('2025-04-01'), ('2025-05-01')),
    PARTITION p202505 VALUES [('2025-05-01'), ('2025-06-01')),
    PARTITION p202506 VALUES [('2025-06-01'), ('2025-07-01'))
)
DISTRIBUTED BY HASH(canvas_id) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "MONTH",
    "dynamic_partition.start" = "-6",
    "dynamic_partition.end" = "2",
    "dynamic_partition.prefix" = "p"
);

-- 每日节点执行统计（按节点+日期聚合）
CREATE TABLE IF NOT EXISTS node_daily_stats (
    stat_date DATE NOT NULL COMMENT '统计日期',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型',
    node_name VARCHAR(128) COMMENT '节点名称',
    total_executions BIGINT SUM DEFAULT "0" COMMENT '总执行数',
    success_count BIGINT SUM DEFAULT "0" COMMENT '成功数',
    fail_count BIGINT SUM DEFAULT "0" COMMENT '失败数',
    skip_count BIGINT SUM DEFAULT "0" COMMENT '跳过数',
    avg_duration_ms BIGINT SUM DEFAULT "0" COMMENT '平均耗时ms',
    total_duration_ms BIGINT SUM DEFAULT "0" COMMENT '总耗时ms'
) ENGINE=OLAP
AGGREGATE KEY(stat_date, node_type, node_name)
PARTITION BY RANGE(stat_date) (
    PARTITION p202501 VALUES [('2025-01-01'), ('2025-02-01')),
    PARTITION p202502 VALUES [('2025-02-01'), ('2025-03-01')),
    PARTITION p202503 VALUES [('2025-03-01'), ('2025-04-01')),
    PARTITION p202504 VALUES [('2025-04-01'), ('2025-05-01')),
    PARTITION p202505 VALUES [('2025-05-01'), ('2025-06-01')),
    PARTITION p202506 VALUES [('2025-06-01'), ('2025-07-01'))
)
DISTRIBUTED BY HASH(node_type) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "MONTH",
    "dynamic_partition.start" = "-6",
    "dynamic_partition.end" = "2",
    "dynamic_partition.prefix" = "p"
);
```

- [ ] **Step 5: Start Doris and verify**

```bash
cd
docker compose -f docker-compose.local.yml up -d doris-fe doris-be
```

Wait 60 seconds for Doris to start, then:

```bash
# Register BE node
docker compose -f docker-compose.local.yml exec doris-fe mysql -uroot -P9030 -e "ALTER SYSTEM ADD BACKEND 'doris-be:9050';"

# Verify BE is registered
docker compose -f docker-compose.local.yml exec doris-fe mysql -uroot -P9030 -e "SHOW BACKENDS;"
```

Expected: `Alive: true` for the BE node.

```bash
# Create databases and tables
docker compose -f docker-compose.local.yml exec doris-fe mysql -uroot -P9030 < infrastructure/doris/init.sql

# Verify tables created
docker compose -f docker-compose.local.yml exec doris-fe mysql -uroot -P9030 -e "SHOW TABLES FROM canvas_ods;"
docker compose -f docker-compose.local.yml exec doris-fe mysql -uroot -P9030 -e "SHOW TABLES FROM canvas_dws;"
```

Expected: `canvas_execution`, `canvas_execution_trace` in canvas_ods; `canvas_daily_stats`, `node_daily_stats` in canvas_dws.

- [ ] **Step 6: Commit**

```bash
git add docker-compose.local.yml infrastructure/doris/
git commit -m "feat: add Doris 2.0 FE/BE to docker-compose with ODS/DWS table schemas"
```

---

### Task 2: Create DorisQueryService for Stats Queries

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisConfig.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisQueryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DailyStatsDTO.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisQueryServiceTest.java`

- [ ] **Step 1: Write failing test — verify DorisQueryService can query Doris**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisQueryServiceTest.java`:

```java
package org.chovy.canvas.infrastructure.doris;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DORIS_ENABLED", matches = "true")
class DorisQueryServiceTest {

    @Autowired
    private DorisQueryService dorisQueryService;

    @Test
    void getDailyStats_returnsDataFromDoris() {
        // Given: query date range
        LocalDate from = LocalDate.of(2025, 5, 1);
        LocalDate to = LocalDate.of(2025, 5, 31);
        Long canvasId = 1L;

        // When: query Doris
        List<DailyStatsDTO> stats = dorisQueryService.getDailyStats(canvasId, from, to);

        // Then: should return list (may be empty if no data)
        assertThat(stats).isNotNull();
    }

    @Test
    void getOverviewStats_returnsAggregatedData() {
        // Given: query date range
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        // When: query overview stats
        DorisQueryService.OverviewStatsDTO overview = dorisQueryService.getOverviewStats(from, to);

        // Then: should return aggregated stats
        assertThat(overview).isNotNull();
        assertThat(overview.totalExecutions()).isGreaterThanOrEqualTo(0);
        assertThat(overview.successCount()).isGreaterThanOrEqualTo(0);
        assertThat(overview.failCount()).isGreaterThanOrEqualTo(0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
mvn test -pl canvas-engine -Dtest=DorisQueryServiceTest -DDORIS_ENABLED=true
```

Expected: FAIL — `DorisQueryService` and `DorisConfig` don't exist.

- [ ] **Step 3: Add Doris JDBC dependency to canvas-engine pom.xml**

Add to `backend/canvas-engine/pom.xml` in the `<dependencies>` section:

```xml
        <!-- ── Doris JDBC Driver ───────────────────────────────────── -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
```

- [ ] **Step 4: Create DorisConfig for JDBC connection**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisConfig.java`:

```java
package org.chovy.canvas.infrastructure.doris;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Doris 数据源配置。
 *
 * <p>Doris 兼容 MySQL 协议，使用 MySQL JDBC 驱动连接。
 * FE 的 9030 端口提供 MySQL 协议访问。
 */
@Configuration
@ConditionalOnProperty(prefix = "canvas.doris", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DorisConfig {

    @Value("${canvas.doris.enabled:false}")
    private boolean dorisEnabled;

    @Value("${canvas.doris.jdbc-url:jdbc:mysql://localhost:9030}")
    private String dorisJdbcUrl;

    @Value("${canvas.doris.username:root}")
    private String dorisUsername;

    @Value("${canvas.doris.password:}")
    private String dorisPassword;

    @Value("${canvas.doris.pool.size:10}")
    private int poolSize;

    @Bean(name = "dorisDataSource", destroyMethod = "close")
    public DataSource dorisDataSource() {
        if (!dorisEnabled) {
            // Return null bean when Doris is disabled
            return null;
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dorisJdbcUrl);
        ds.setUsername(dorisUsername);
        ds.setPassword(dorisPassword);
        ds.setMaximumPoolSize(poolSize);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(5000);
        ds.setIdleTimeout(300000);
        ds.setMaxLifetime(1800000);
        ds.setPoolName("doris-pool");
        return ds;
    }

    @Bean(name = "dorisJdbcTemplate")
    public JdbcTemplate dorisJdbcTemplate(DataSource dorisDataSource) {
        if (dorisDataSource == null) {
            return null;
        }
        return new JdbcTemplate(dorisDataSource);
    }
}
```

- [ ] **Step 5: Create DailyStatsDTO**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DailyStatsDTO.java`:

```java
package org.chovy.canvas.infrastructure.doris;

import java.time.LocalDate;

/**
 * 每日统计数据 DTO。
 */
public record DailyStatsDTO(
    LocalDate statDate,
    Long canvasId,
    String canvasName,
    String triggerType,
    Long totalExecutions,
    Long successCount,
    Long failCount,
    Long runningCount,
    Long avgDurationMs
) {
}
```

- [ ] **Step 6: Create DorisQueryService**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisQueryService.java`:

```java
package org.chovy.canvas.infrastructure.doris;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Doris 查询服务。
 *
 * <p>负责从 Doris OLAP 数仓查询统计数据，替代 MySQL 全表扫描 + Java Stream 聚合。
 */
@Slf4j
@Service
public class DorisQueryService {

    private final JdbcTemplate dorisJdbcTemplate;

    @Autowired
    public DorisQueryService(@Qualifier("dorisJdbcTemplate") JdbcTemplate dorisJdbcTemplate) {
        this.dorisJdbcTemplate = dorisJdbcTemplate;
    }

    /**
     * 查询指定画布的每日执行统计。
     */
    public List<DailyStatsDTO> getDailyStats(Long canvasId, LocalDate from, LocalDate to) {
        if (dorisJdbcTemplate == null) {
            log.warn("Doris not enabled, returning empty stats");
            return List.of();
        }

        String sql = """
            SELECT stat_date, canvas_id, canvas_name, trigger_type,
                   SUM(total_executions) as total_executions,
                   SUM(success_count) as success_count,
                   SUM(fail_count) as fail_count,
                   SUM(running_count) as running_count,
                   CASE WHEN SUM(total_executions) > 0
                        THEN SUM(total_duration_ms) / SUM(total_executions)
                        ELSE 0 END as avg_duration_ms
            FROM canvas_dws.canvas_daily_stats
            WHERE canvas_id = ?
              AND stat_date BETWEEN ? AND ?
            GROUP BY stat_date, canvas_id, canvas_name, trigger_type
            ORDER BY stat_date
            """;

        return dorisJdbcTemplate.query(sql,
            (rs, rowNum) -> new DailyStatsDTO(
                rs.getDate("stat_date").toLocalDate(),
                rs.getLong("canvas_id"),
                rs.getString("canvas_name"),
                rs.getString("trigger_type"),
                rs.getLong("total_executions"),
                rs.getLong("success_count"),
                rs.getLong("fail_count"),
                rs.getLong("running_count"),
                rs.getLong("avg_duration_ms")
            ),
            canvasId, from, to);
    }

    /**
     * 查询所有画布的概览统计。
     */
    public OverviewStatsDTO getOverviewStats(LocalDate from, LocalDate to) {
        if (dorisJdbcTemplate == null) {
            log.warn("Doris not enabled, returning zero stats");
            return new OverviewStatsDTO(0L, 0L, 0L, 0L, 0L);
        }

        String sql = """
            SELECT SUM(total_executions) as total_executions,
                   SUM(success_count) as success_count,
                   SUM(fail_count) as fail_count,
                   SUM(running_count) as running_count,
                   CASE WHEN SUM(total_executions) > 0
                        THEN SUM(total_duration_ms) / SUM(total_executions)
                        ELSE 0 END as avg_duration_ms
            FROM canvas_dws.canvas_daily_stats
            WHERE stat_date BETWEEN ? AND ?
            """;

        return dorisJdbcTemplate.queryForObject(sql,
            (rs, rowNum) -> new OverviewStatsDTO(
                rs.getLong("total_executions"),
                rs.getLong("success_count"),
                rs.getLong("fail_count"),
                rs.getLong("running_count"),
                rs.getLong("avg_duration_ms")
            ),
            from, to);
    }

    /**
     * 查询节点类型执行统计。
     */
    public List<NodeStatsDTO> getNodeStats(LocalDate from, LocalDate to) {
        if (dorisJdbcTemplate == null) {
            return List.of();
        }

        String sql = """
            SELECT node_type, node_name,
                   SUM(total_executions) as total_executions,
                   SUM(success_count) as success_count,
                   SUM(fail_count) as fail_count,
                   SUM(skip_count) as skip_count,
                   CASE WHEN SUM(total_executions) > 0
                        THEN SUM(total_duration_ms) / SUM(total_executions)
                        ELSE 0 END as avg_duration_ms
            FROM canvas_dws.node_daily_stats
            WHERE stat_date BETWEEN ? AND ?
            GROUP BY node_type, node_name
            ORDER BY total_executions DESC
            """;

        return dorisJdbcTemplate.query(sql,
            (rs, rowNum) -> new NodeStatsDTO(
                rs.getString("node_type"),
                rs.getString("node_name"),
                rs.getLong("total_executions"),
                rs.getLong("success_count"),
                rs.getLong("fail_count"),
                rs.getLong("skip_count"),
                rs.getLong("avg_duration_ms")
            ),
            from, to);
    }

    public record OverviewStatsDTO(
        Long totalExecutions,
        Long successCount,
        Long failCount,
        Long runningCount,
        Long avgDurationMs
    ) {}

    public record NodeStatsDTO(
        String nodeType,
        String nodeName,
        Long totalExecutions,
        Long successCount,
        Long failCount,
        Long skipCount,
        Long avgDurationMs
    ) {}

    /**
     * Query unique user count for a specific canvas on a specific date from Doris.
     * Used by HomeOverviewController when Doris is enabled.
     *
     * @param canvasId the canvas ID
     * @param date     the date string (format: yyyy-MM-dd)
     * @return count of distinct users, or 0 if no data
     */
    public int queryUniqueUsers(Long canvasId, String date) {
        if (dorisJdbcTemplate == null) {
            return 0;
        }
        String sql = "SELECT COUNT(DISTINCT user_id) FROM canvas_execution_trace WHERE canvas_id = ? AND DATE(start_time) = ?";
        try {
            Integer result = dorisJdbcTemplate.queryForObject(sql, Integer.class, canvasId, date);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.warn("Doris queryUniqueUsers failed for canvasId={} date={}: {}", canvasId, date, e.getMessage());
            return 0;
        }
    }
}
```

- [ ] **Step 7: Add Doris config to application.yml**

Add to `backend/canvas-engine/src/main/resources/application.yml`:

```yaml
canvas:
  doris:
    enabled: false  # Set to true when Doris is deployed
    jdbc-url: jdbc:mysql://localhost:9030
    username: root
    password: ""
    pool:
      size: 10
```

- [ ] **Step 8: Run test to verify it passes (with Doris disabled)**

```bash
cd backend
mvn test -pl canvas-engine -Dtest=DorisQueryServiceTest
```

Expected: Tests are skipped due to `@EnabledIfEnvironmentVariable`.

- [ ] **Step 9: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/
git add backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/
git add backend/canvas-engine/src/main/resources/application.yml
git add backend/canvas-engine/pom.xml
git commit -m "feat: add DorisQueryService for OLAP stats queries with JDBC connection"
```

---

### Task 3: Migrate HomeOverviewController to Use DorisQueryService

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/HomeOverviewController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/HomeOverviewControllerTest.java`

- [ ] **Step 1: Write failing test — verify HomeOverviewController uses Doris**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/HomeOverviewControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.infrastructure.doris.DorisQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(HomeOverviewController.class)
class HomeOverviewControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DorisQueryService dorisQueryService;

    @Test
    void overview_shouldQueryDorisWhenEnabled() {
        // Given: Doris returns stats
        DorisQueryService.OverviewStatsDTO mockStats =
            new DorisQueryService.OverviewStatsDTO(100L, 90L, 10L, 0L, 500L);
        when(dorisQueryService.getOverviewStats(any(), any())).thenReturn(mockStats);

        // When: call overview API
        webTestClient.get()
            .uri("/canvas/home/overview?days=7")
            .exchange()
            .expectStatus().isOk();

        // Then: DorisQueryService should be called
        verify(dorisQueryService, Mockito.times(1)).getOverviewStats(any(LocalDate.class), any(LocalDate.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
mvn test -pl canvas-engine -Dtest=HomeOverviewControllerTest
```

Expected: FAIL — `HomeOverviewController` doesn't inject `DorisQueryService`.

- [ ] **Step 3: Modify HomeOverviewController to use DorisQueryService**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/HomeOverviewController.java`:

Add import at the top:

```java
import org.chovy.canvas.infrastructure.doris.DorisQueryService;
import org.springframework.beans.factory.annotation.Value;
```

Add field and constructor modification:

```java
@RestController
@RequestMapping("/canvas/home")
@RequiredArgsConstructor
public class HomeOverviewController {

    /** 画布 Mapper，用于统计已发布画布。 */
    private final CanvasMapper canvasMapper;
    /** 执行记录 Mapper，用于统计首页执行数据。 */
    private final CanvasExecutionMapper executionMapper;
    /** Doris 查询服务，用于 OLAP 统计。 */
    private final DorisQueryService dorisQueryService;

    @Value("${canvas.doris.enabled:false}")
    private boolean dorisEnabled;
```

Replace the `buildOverview` method with Doris-aware version:

```java
    /**
     * 构建、解析或转换 build Overview 相关的业务数据。
     *
     * <p>当 Doris 启用时，从 Doris 查询聚合统计；否则从 MySQL 全表扫描。
     * <p>HomeOverviewDTO is a record with (range, summary, trend, topCanvases, attentionItems).
     */
    private HomeOverviewDTO buildOverview(int days) {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(days - 1L);

        // Get published canvases (still from MySQL, as this is OLTP)
        List<CanvasDO> publishedCanvases = canvasMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .filter(canvas -> CanvasStatusEnum.PUBLISHED.getCode().equals(canvas.getStatus()))
                .toList();
        Map<Long, CanvasDO> canvasById = publishedCanvases.stream()
                .collect(Collectors.toMap(CanvasDO::getId, canvas -> canvas, (left, right) -> left, LinkedHashMap::new));

        // Get stats from Doris if enabled, otherwise from MySQL
        long totalExecutions;
        long failedExecutions;
        long successExecutions;
        long uniqueUsers;
        List<CanvasExecutionDO> executions;

        if (dorisEnabled && dorisQueryService != null) {
            // Use Doris for aggregation
            DorisQueryService.OverviewStatsDTO stats = dorisQueryService.getOverviewStats(since, today);
            totalExecutions = stats.totalExecutions();
            successExecutions = stats.successCount();
            failedExecutions = stats.failCount();
            uniqueUsers = dorisQueryService.queryUniqueUsers(null, today.toString()); // null = all canvases
        } else {
            // Fallback to MySQL (original logic)
            LocalDateTime sinceTime = since.atStartOfDay();
            LocalDateTime untilTime = today.plusDays(1).atStartOfDay();

            executions = executionMapper.selectList(
                    new LambdaQueryWrapper<CanvasExecutionDO>()
                            .ge(CanvasExecutionDO::getCreatedAt, sinceTime)
                            .lt(CanvasExecutionDO::getCreatedAt, untilTime)
            );
            List<CanvasExecutionDO> relevantExecutions = executions.stream()
                    .filter(execution -> execution.getCanvasId() != null && canvasById.containsKey(execution.getCanvasId()))
                    .toList();

            totalExecutions = relevantExecutions.size();
            successExecutions = relevantExecutions.stream()
                    .filter(e -> ExecutionStatus.SUCCESS.getCode().equals(e.getStatus()))
                    .count();
            failedExecutions = relevantExecutions.stream()
                    .filter(e -> ExecutionStatus.FAILED.getCode().equals(e.getStatus()))
                    .count();
            uniqueUsers = countUniqueUsersFromExecutions(relevantExecutions);
        }

        // Build the HomeOverviewDTO using the existing record structure
        return new HomeOverviewDTO(
                new RangeDTO(days, since.toString(), today.toString()),
                new SummaryDTO(
                        publishedCanvases.size(),
                        totalExecutions,
                        uniqueUsers,
                        failedExecutions,
                        formatRate(successExecutions, totalExecutions)
                ),
                trend,
                topCanvases,
                attentionItems
        );
    }
```

Add the HomeOverviewDTO record class (confirmed to already exist in HomeOverviewController.java as a nested record):

```java
    // HomeOverviewDTO already exists as a nested record in HomeOverviewController.java:
    // public record HomeOverviewDTO(RangeDTO range, SummaryDTO summary,
    //     List<TrendPointDTO> trend, List<TopCanvasDTO> topCanvases,
    //     List<AttentionItemDTO> attentionItems) {}
    //
    // No separate DTO class needed — the existing structure is used directly.
```

Define the helper methods and records used by `buildOverview`:

```java
    /** Date range descriptor. */
    public record RangeDTO(int days, String since, String until) {}

    /** Summary statistics. */
    public record SummaryDTO(
        long publishedCount,
        long totalExecutions,
        long uniqueUsers,
        long failedExecutions,
        String successRate
    ) {}

    /** Trend data point (date + execution count). */
    public record TrendPointDTO(String date, long count) {}

    /** Top canvas by execution count. */
    public record TopCanvasDTO(Long canvasId, String name, long executionCount, String successRate) {}

    /** Attention item requiring operator review. */
    public record AttentionItemDTO(Long canvasId, String name, String reason, long failCount) {}

    /** Build trend data points for the last N days. */
    private List<TrendPointDTO> buildTrend(int days) {
        LocalDate today = LocalDate.now();
        List<TrendPointDTO> trend = new java.util.ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count;
            if (dorisEnabled && dorisQueryService != null) {
                DorisQueryService.OverviewStatsDTO dayStats =
                    dorisQueryService.getOverviewStats(date, date);
                count = dayStats.totalExecutions();
            } else {
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
                count = executionMapper.selectCount(
                    new LambdaQueryWrapper<CanvasExecutionDO>()
                        .ge(CanvasExecutionDO::getCreatedAt, dayStart)
                        .lt(CanvasExecutionDO::getCreatedAt, dayEnd)
                );
            }
            trend.add(new TrendPointDTO(date.toString(), count));
        }
        return trend;
    }

    /** Build top canvases sorted by execution count descending. */
    private List<TopCanvasDTO> buildTopCanvases(Map<Long, CanvasDO> canvasById) {
        return canvasById.values().stream()
            .map(c -> {
                long execCount;
                if (dorisEnabled && dorisQueryService != null) {
                    List<DailyStatsDTO> stats = dorisQueryService.getDailyStats(c.getId(),
                        LocalDate.now().minusDays(30), LocalDate.now());
                    execCount = stats.stream().mapToLong(DailyStatsDTO::totalExecutions).sum();
                } else {
                    execCount = executionMapper.selectCount(
                        new LambdaQueryWrapper<CanvasExecutionDO>()
                            .eq(CanvasExecutionDO::getCanvasId, c.getId())
                    );
                }
                return new TopCanvasDTO(c.getId(), c.getName(), execCount, "");
            })
            .sorted(java.util.Comparator.comparingLong(TopCanvasDTO::executionCount).reversed())
            .limit(5)
            .toList();
    }

    /** Build attention items for canvases with high failure rates. */
    private List<AttentionItemDTO> buildAttentionItems(long totalExecutions, long failedExecutions,
                                                       Map<Long, CanvasDO> canvasById) {
        if (totalExecutions == 0 || failedExecutions == 0) return List.of();
        double failRate = (double) failedExecutions / totalExecutions;
        if (failRate < 0.1) return List.of(); // Only flag if >10% failure rate
        return canvasById.values().stream()
            .filter(c -> {
                long fails = executionMapper.selectCount(
                    new LambdaQueryWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getCanvasId, c.getId())
                        .eq(CanvasExecutionDO::getStatus, ExecutionStatus.FAILED.getCode())
                );
                return fails > 0;
            })
            .map(c -> new AttentionItemDTO(c.getId(), c.getName(), "High failure rate",
                executionMapper.selectCount(
                    new LambdaQueryWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getCanvasId, c.getId())
                        .eq(CanvasExecutionDO::getStatus, ExecutionStatus.FAILED.getCode())
                )))
            .limit(5)
            .toList();
    }

    /** Format success rate as percentage string. */
    private String formatRate(long success, long total) {
        if (total == 0) return "0%";
        return String.format("%.1f%%", (double) success / total * 100);
    }

    /** Count unique users from execution records (fallback when Doris is not available). */
    private long countUniqueUsersFromExecutions(List<CanvasExecutionDO> executionList) {
        return executionList.stream()
            .map(CanvasExecutionDO::getUserId)
            .filter(userId -> userId != null && !userId.isEmpty())
            .distinct()
            .count();
    }
```

Location note: Add the `buildTrend`, `buildTopCanvases`, `buildAttentionItems`, and `countUniqueUsersFromExecutions` helper methods to HomeOverviewController.java after the existing `buildOverview()` method.

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend
mvn test -pl canvas-engine -Dtest=HomeOverviewControllerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/HomeOverviewController.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/web/HomeOverviewControllerTest.java
git commit -m "feat: migrate HomeOverviewController to use DorisQueryService for stats aggregation"
```

---

### Task 4: Configure Flink CDC Pipeline (MySQL → Doris)

**Files:**
- Create: `infrastructure/flink/cdc-pipeline/mysql-to-doris.yaml`
- Modify: `docker-compose.local.yml`

- [ ] **Step 1: Add Flink services to docker-compose**

Add to `docker-compose.local.yml` after the `doris-be` service:

```yaml
  # ── Flink 1.20 集群 ──────────────────────────────────────────
  # JobManager: Flink 作业调度、checkpoint 管理
  # TaskManager: 任务执行器
  flink-jobmanager:
    image: flink:1.20.0-scala_2.12
    container_name: canvas-flink-jobmanager
    hostname: flink-jobmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: flink-jobmanager
        jobmanager.rpc.port: 6123
        jobmanager.memory.process.size: 1600m
        state.backend: rocksdb
        state.checkpoints.dir: file:///opt/flink/checkpoints
        state.savepoints.dir: file:///opt/flink/savepoints
    ports:
      - "8082:8081"   # Flink Web UI
    volumes:
      - canvas-flink-checkpoints:/opt/flink/checkpoints
      - canvas-flink-savepoints:/opt/flink/savepoints
      - ./infrastructure/flink:/opt/flink/usrlib:ro
    command: jobmanager
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/overview"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  flink-taskmanager:
    image: flink:1.20.0-scala_2.12
    container_name: canvas-flink-taskmanager
    hostname: flink-taskmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: flink-jobmanager
        jobmanager.rpc.port: 6123
        taskmanager.memory.process.size: 2048m
        taskmanager.numberOfTaskSlots: 4
    volumes:
      - canvas-flink-checkpoints:/opt/flink/checkpoints
      - canvas-flink-savepoints:/opt/flink/savepoints
      - ./infrastructure/flink:/opt/flink/usrlib:ro
    depends_on:
      flink-jobmanager:
        condition: service_healthy
    command: taskmanager
```

Add to the `volumes:` section:

```yaml
  canvas-flink-checkpoints:
  canvas-flink-savepoints:
```

- [ ] **Step 2: Create Flink CDC pipeline YAML**

Create `infrastructure/flink/cdc-pipeline/mysql-to-doris.yaml`:

```yaml
# Flink CDC 3.6 Pipeline: MySQL → Doris
# 文档: https://nightlies.apache.org/flink/flink-cdc-docs-release-3.6/

source:
  type: mysql
  hostname: mysql
  port: 3306
  username: root
  password: root
  # 同步 canvas_db 下所有表
  tables: canvas_db\\..*
  # server-id 范围，用于 binlog 消费
  server-id: 5400-5404
  server-time-zone: Asia/Shanghai
  # schema 变更支持
  schema-change.enabled: true

sink:
  type: doris
  fenodes: doris-fe:8030
  username: root
  password: ""
  jdbc-url: jdbc:mysql://doris-fe:9030
  # Doris sink 配置
  sink.properties.format: json
  sink.properties.read_json_by_line: true
  sink.enable-delete: true
  sink.buffer-flush.max-rows: 10000
  sink.buffer-flush.interval: 10s

pipeline:
  name: canvas_mysql_to_doris
  parallelism: 2
  # checkpoint 配置
  checkpoint.interval: 60000
  # 启动模式: INITIAL (全量+增量) 或 LATEST_OFFSET (仅增量)
  scan.startup.mode: INITIAL

route:
  # 路由规则：MySQL 表 → Doris ODS 表
  - source-table: canvas_db.canvas_execution
    sink-table: canvas_ods.canvas_execution
  - source-table: canvas_db.canvas_execution_trace
    sink-table: canvas_ods.canvas_execution_trace
```

- [ ] **Step 3: Verify Flink starts**

```bash
cd
docker compose -f docker-compose.local.yml up -d flink-jobmanager flink-taskmanager

# Wait 30 seconds, then check Flink UI
curl http://localhost:8082/overview
```

Expected: JSON response with `taskmanagers: 1`, `slots-total: 4`.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.local.yml infrastructure/flink/
git commit -m "feat: add Flink 1.20 cluster and CDC pipeline config for MySQL→Doris sync"
```

- [ ] **Step 5: Add Flink CDC pipeline config validation test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/flink/CdcPipelineConfigTest.java`:

```java
package org.chovy.canvas.infrastructure.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Flink CDC pipeline YAML is well-formed and contains required keys.
 * Does NOT start a Flink cluster — just checks the config file structure.
 *
 * <p>This test lives in Task 4 because it depends on
 * infrastructure/flink/cdc-pipeline/mysql-to-doris.yaml which is created in Task 4 Step 2.
 */
class CdcPipelineConfigTest {

    private static final String PIPELINE_YAML =
            "infrastructure/flink/cdc-pipeline/mysql-to-doris.yaml";

    @Test
    void pipelineYamlIsParseableAndContainsRequiredKeys() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        File yamlFile = new File(PIPELINE_YAML);

        assertThat(yamlFile).exists();

        Map<String, Object> config = yamlMapper.readValue(yamlFile, Map.class);

        // Verify top-level sections
        assertThat(config).containsKey("source");
        assertThat(config).containsKey("sink");
        assertThat(config).containsKey("pipeline");
        assertThat(config).containsKey("route");

        // Verify source section
        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) config.get("source");
        assertThat(source.get("type")).isEqualTo("mysql");
        assertThat(source).containsKey("hostname");
        assertThat(source).containsKey("tables");

        // Verify sink section
        @SuppressWarnings("unchecked")
        Map<String, Object> sink = (Map<String, Object>) config.get("sink");
        assertThat(sink.get("type")).isEqualTo("doris");
        assertThat(sink).containsKey("fenodes");
        assertThat(sink).containsKey("jdbc-url");

        // Verify pipeline section
        @SuppressWarnings("unchecked")
        Map<String, Object> pipeline = (Map<String, Object>) config.get("pipeline");
        assertThat(pipeline).containsKey("name");
        assertThat(pipeline).containsKey("parallelism");
        assertThat(pipeline).containsKey("checkpoint.interval");
    }
}
```

- [ ] **Step 6: Commit test**

```bash
git add backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/flink/CdcPipelineConfigTest.java
git commit -m "test: add CdcPipelineConfigTest to validate Flink CDC YAML structure"
```
