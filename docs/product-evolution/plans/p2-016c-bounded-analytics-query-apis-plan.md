# Bounded Analytics Query APIs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-safe, date-bounded analytics query APIs and export job creation.

**Architecture:** Store funnel definitions, alert rules, and export jobs in MySQL; enforce tenant/date/max-range guards in `AnalyticsQueryService`; expose a controller that returns typed rejections instead of running unbounded queries.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016c-bounded-analytics-query-apis.md`
- Depends on P2-016 event/trace schema fields.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V129__analytics_query_definitions.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsFunnelDefinitionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsAlertRuleDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsExportJobDO.java`
- Create matching mappers under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java`

### Task 1: Query Definition Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V129__analytics_query_definitions.sql`

- [ ] **Step 1: Write schema and guard tests**

Create tests:

```java
@Test
void migrationCreatesFunnelAlertAndExportTables() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V129__analytics_query_definitions.sql"));

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
    AnalyticsQueryService service = new AnalyticsQueryService(mock(AnalyticsQueryService.Repository.class), 31, 100_000);

    assertThatThrownBy(() -> service.eventAnalysis(new AnalyticsQueryService.QueryScope(null, "2026-06-01", "2026-06-03")))
            .hasMessageContaining("tenantId is required");
    assertThatThrownBy(() -> service.eventAnalysis(new AnalyticsQueryService.QueryScope(0L, null, "2026-06-03")))
            .hasMessageContaining("date range is required");
}
```

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsQueryServiceTest
```

Expected: FAIL because migration and service do not exist.

- [ ] **Step 3: Add migration**

Create `V129__analytics_query_definitions.sql` with tables:

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

- [ ] **Step 4: Implement query guard model**

Create `AnalyticsQueryService.QueryScope(Long tenantId, String startDate, String endDate)` and `validateScope` that rejects missing tenant, missing dates, invalid ranges, and ranges longer than configured `maxRangeDays`.

- [ ] **Step 5: Run schema and guard tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsQueryServiceTest#migrationCreatesFunnelAlertAndExportTables,AnalyticsQueryServiceTest#rejectsMissingTenantOrUnboundedDateRange
```

Expected: PASS.

### Task 2: Query Methods And Controller

**Files:**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java`

- [ ] **Step 1: Add query behavior tests**

Append tests:

```java
@Test
void eventAnalysisGroupsByEventCodeAndUserTimelineIsPaged() {
    AnalyticsQueryService.Repository repo = mock(AnalyticsQueryService.Repository.class);
    when(repo.groupEvents(0L, "2026-06-01", "2026-06-03")).thenReturn(List.of(new AnalyticsQueryService.EventCount("OrderPaid", 12)));
    when(repo.userTimeline(0L, "u1", "2026-06-01", "2026-06-03", 1, 20)).thenReturn(List.of(new AnalyticsQueryService.TimelineRow("OrderPaid", "2026-06-02T10:00:00")));
    AnalyticsQueryService service = new AnalyticsQueryService(repo, 31, 100_000);

    assertThat(service.eventAnalysis(new AnalyticsQueryService.QueryScope(0L, "2026-06-01", "2026-06-03"))).extracting("eventCode").contains("OrderPaid");
    assertThat(service.userTimeline(0L, "u1", "2026-06-01", "2026-06-03", 1, 20)).hasSize(1);
}

@Test
void exportCreationRejectsRowsAboveLimit() {
    AnalyticsQueryService.Repository repo = mock(AnalyticsQueryService.Repository.class);
    when(repo.estimateRows(any())).thenReturn(100_001L);
    AnalyticsQueryService service = new AnalyticsQueryService(repo, 31, 100_000);

    assertThatThrownBy(() -> service.createExport(new AnalyticsQueryService.ExportRequest(0L, "EVENT_ANALYSIS", "2026-06-01", "2026-06-03", 100_000)))
            .hasMessageContaining("export row limit exceeded");
}
```

- [ ] **Step 2: Implement query methods**

Add methods: `eventAnalysis`, `funnelResult`, `userTimeline`, `attributeDistribution`, `alertPreview`, `createExport`, and `exportStatus`. Every method must call `validateScope` or equivalent tenant/date checks before repository access.

- [ ] **Step 3: Add controller endpoints**

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

- [ ] **Step 4: Run analytics service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsQueryServiceTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-016c-bounded-analytics-query-apis.md`
- Modify: `docs/product-evolution/plans/p2-016c-bounded-analytics-query-apis-plan.md`

- [ ] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsQueryServiceTest
```

Expected: PASS.

- [ ] **Step 2: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V129__analytics_query_definitions.sql backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsFunnelDefinitionDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsAlertRuleDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsExportJobDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsFunnelDefinitionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsAlertRuleMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsExportJobMapper.java backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java docs/product-evolution/specs/p2-016c-bounded-analytics-query-apis.md docs/product-evolution/plans/p2-016c-bounded-analytics-query-apis-plan.md
git commit -m "feat: add bounded analytics query APIs"
```

Expected: commit contains only analytics query definitions, service, controller, tests, migration, and related docs.
