# Bounded Analytics Query APIs Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-safe, date-bounded analytics query APIs and export job creation.

**Architecture:** Store funnel definitions, alert rules, and export jobs in MySQL; enforce tenant/date/max-range guards in `AnalyticsQueryService`; expose a controller that returns typed rejections instead of running unbounded queries.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016c-bounded-analytics-query-apis.md`
- Depends on P2-016 event/trace schema fields.

## Current Status Note

The implementation files are present in the current worktree and fresh focused
verification passed on 2026-06-09:

- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=AnalyticsQuerySchemaTest,AnalyticsQueryGuardTest,AnalyticsQueryServiceTest,AnalyticsControllerTest` passed with 23 tests, zero failures, and zero errors.

The actual migration is `V134__analytics_query_definitions.sql`; the original
plan's `V129` filename was superseded by the current migration sequence.
Historical RED-state checks were not reproduced because the current worktree
already contains implementation files for the schema, mappers, query guard, and
baseline analytics APIs. No commit or merge was created in this audit, so commit
and merge status remain unverified.

## File Structure

- Present: `backend/canvas-engine/src/main/resources/db/migration/V134__analytics_query_definitions.sql`
- Present: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsFunnelDefinitionDO.java`
- Present: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsAlertRuleDO.java`
- Present: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsExportJobDO.java`
- Present matching mappers under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/`
- Present: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java`
- Present: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryGuard.java`
- Present: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java`
- Present: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQuerySchemaTest.java`
- Present: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryGuardTest.java`
- Present: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java`
- Present: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/AnalyticsControllerTest.java`

### Task 1: Query Definition Schema

**Files:**
- Present: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQuerySchemaTest.java`
- Present: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryGuardTest.java`
- Present: `backend/canvas-engine/src/main/resources/db/migration/V134__analytics_query_definitions.sql`

- [x] **Step 1: Write schema and guard tests**

Current tests include:

```java
@Test
void migrationCreatesFunnelAlertAndExportTables() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V134__analytics_query_definitions.sql"));

    assertThat(sql)
            .contains("CREATE TABLE IF NOT EXISTS analytics_funnel_definition")
            .contains("version")
            .contains("steps_json")
            .contains("CREATE TABLE IF NOT EXISTS analytics_alert_rule")
            .contains("threshold_json")
            .contains("CREATE TABLE IF NOT EXISTS analytics_export_job")
            .contains("status")
            .contains("row_limit");
}

@Test
void rejectsMissingTenantOrUnboundedDateRange() {
    AnalyticsQueryService service = new AnalyticsQueryService(mock(AnalyticsEventMapper.class), new AnalyticsQueryGuard());

    assertThatThrownBy(() -> service.eventCounts(null, "2026-06-01", "2026-06-03"))
            .hasMessageContaining("tenantId");
    assertThatThrownBy(() -> service.eventCounts(0L, null, "2026-06-03"))
            .hasMessageContaining("startDate and endDate are required");
}
```

- [x] **Step 2: Run tests and confirm red state**

Historical RED-state boundary: not reproduced in this audit because the current
worktree already contains the schema, mapper, guard, and baseline query API
implementation files.

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsQueryServiceTest
```

Historical expected result before implementation: FAIL because migration and
service did not exist.

- [x] **Step 3: Add migration**

Create `V134__analytics_query_definitions.sql` with tables:

```sql
CREATE TABLE IF NOT EXISTS analytics_funnel_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  funnel_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  name VARCHAR(128) NOT NULL,
  steps_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_funnel_version (tenant_id, funnel_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  rule_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  threshold_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_alert_rule (tenant_id, rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_export_job (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  report_type VARCHAR(64) NOT NULL,
  query_json JSON NOT NULL,
  row_limit INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  file_url VARCHAR(500) NULL,
  error_message VARCHAR(1000) NULL,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_analytics_export_job (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 4: Implement query guard model**

Create `AnalyticsQueryGuard` date, tenant, event-code, attribute-path, and
pagination guards. Service methods call the guard before mapper access.

- [x] **Step 5: Run schema and guard tests**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=AnalyticsQuerySchemaTest,AnalyticsQueryGuardTest
```

Expected: PASS with zero failures and zero errors.

### Task 2: Query Methods And Controller

**Files:**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java`

- [x] **Step 1: Add query behavior tests**

Current tests include:

```java
@Test
void eventCountsGroupByEventCode() {
    AnalyticsEventMapper mapper = mock(AnalyticsEventMapper.class);
    when(mapper.selectEventCounts(0L, "2026-06-01", "2026-06-03"))
            .thenReturn(List.of(Map.of("eventCode", "OrderPaid", "count", 12L)));
    AnalyticsQueryService service = new AnalyticsQueryService(mapper, new AnalyticsQueryGuard());

    assertThat(service.eventCounts(0L, "2026-06-01", "2026-06-03"))
            .extracting("eventCode")
            .contains("OrderPaid");
}

@Test
void exportCreationRejectsRowsAboveLimit() {
    AnalyticsEventMapper eventMapper = mock(AnalyticsEventMapper.class);
    when(eventMapper.countEvents(0L, "2026-06-01", "2026-06-03")).thenReturn(100_001L);
    AnalyticsQueryService service = service(eventMapper, null, null, mock(AnalyticsExportJobMapper.class), 100_000);

    assertThatThrownBy(() -> service.createExport(0L, new AnalyticsQueryService.ExportRequest("EVENT_ANALYSIS", "2026-06-01", "2026-06-03", null, 100_000, "alice")))
            .hasMessageContaining("export row limit exceeded");
}
```

- [x] **Step 2: Implement query methods**

Add methods: `eventCounts`, `countEvents`, `funnelResult`, `userTimeline`,
`attributeDistribution`, `alertPreview`, `createExport`, and `exportStatus`.
Every method calls tenant/date guards before mapper access.

- [x] **Step 3: Add controller endpoints**

Expose:

```text
GET /analytics/events
GET /analytics/funnels/{funnelKey}
GET /analytics/users/{userId}/timeline
GET /analytics/attributes/{attribute}/distribution
POST /analytics/alerts/preview
POST /analytics/exports
GET /analytics/exports/{id}
```

- [x] **Step 4: Run analytics service tests**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=AnalyticsQuerySchemaTest,AnalyticsQueryGuardTest,AnalyticsQueryServiceTest,AnalyticsControllerTest
```

Expected: PASS with zero failures and zero errors.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-016c-bounded-analytics-query-apis.md`
- Modify: `docs/product-evolution/plans/p2-016c-bounded-analytics-query-apis-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=AnalyticsQuerySchemaTest,AnalyticsQueryGuardTest,AnalyticsQueryServiceTest,AnalyticsControllerTest
```

Expected: PASS with zero failures and zero errors.

- [x] **Step 2: Document commit and merge boundary**

No commit or merge was created in this audit because the broader
product-evolution goal is still incomplete and the current worktree contains
unrelated dirty changes. Commit and merge status remain unverified.

Commit commands were not run in this audit.
