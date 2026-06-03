# Analytics Retention And Archive Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-bounded retention and archive policies for analytics-related records.

**Architecture:** Store tenant policy in MySQL, enforce platform min/max bounds from configuration, and process archive/delete jobs in bounded batches with dry-run and legal-hold skip behavior.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016b-analytics-retention-and-archive-policy.md`
- Depends on P2-016 archive/retention fields.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V128__analytics_retention_policy.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionPolicyDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionPolicyMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionRunMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/RetentionPolicyService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

### Task 1: Schema And Policy Bounds

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V128__analytics_retention_policy.sql`

- [ ] **Step 1: Write schema and bounds tests**

Create tests:

```java
@Test
void migrationCreatesRetentionPolicyAndRunTables() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V128__analytics_retention_policy.sql"));

    assertThat(sql)
            .contains("CREATE TABLE IF NOT EXISTS analytics_retention_policy")
            .contains("record_kind")
            .contains("retention_days")
            .contains("action")
            .contains("max_batch_size")
            .contains("CREATE TABLE IF NOT EXISTS analytics_retention_run")
            .contains("dry_run")
            .contains("archived_count")
            .contains("deleted_count");
}

@Test
void tenantOverrideMustStayWithinPlatformBounds() {
    RetentionPolicyService service = new RetentionPolicyService(
            mock(RetentionPolicyService.PolicyRepository.class),
            mock(RetentionPolicyService.RetentionTargetRepository.class),
            new RetentionPolicyService.Bounds(7, 730));

    assertThatThrownBy(() -> service.validate(new RetentionPolicyService.PolicyInput(0L, "EVENT", 1, "DELETE", 100, false)))
            .hasMessageContaining("below platform minimum");
    assertThatThrownBy(() -> service.validate(new RetentionPolicyService.PolicyInput(0L, "EVENT", 1000, "DELETE", 100, false)))
            .hasMessageContaining("above platform maximum");
}
```

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RetentionPolicyServiceTest
```

Expected: FAIL because migration and service do not exist.

- [ ] **Step 3: Add migration**

Create `V128__analytics_retention_policy.sql`:

```sql
CREATE TABLE IF NOT EXISTS analytics_retention_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  record_kind VARCHAR(32) NOT NULL,
  retention_days INT NOT NULL,
  action VARCHAR(20) NOT NULL,
  max_batch_size INT NOT NULL DEFAULT 1000,
  legal_hold_behavior VARCHAR(20) NOT NULL DEFAULT 'SKIP',
  enabled TINYINT NOT NULL DEFAULT 1,
  updated_by VARCHAR(128) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_retention_policy (tenant_id, record_kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_retention_run (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  record_kind VARCHAR(32) NOT NULL,
  action VARCHAR(20) NOT NULL,
  dry_run TINYINT NOT NULL DEFAULT 1,
  scanned_count BIGINT NOT NULL DEFAULT 0,
  archived_count BIGINT NOT NULL DEFAULT 0,
  deleted_count BIGINT NOT NULL DEFAULT 0,
  skipped_count BIGINT NOT NULL DEFAULT 0,
  failed_count BIGINT NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  INDEX idx_analytics_retention_run (tenant_id, record_kind, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement policy validation**

Create `RetentionPolicyService` records `Bounds`, `PolicyInput`, and `RunResult`. `validate` must reject non-positive batch size, retention below min, retention above max, unknown `recordKind`, and unknown action outside `ARCHIVE|DELETE`.

- [ ] **Step 5: Run schema and bounds tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RetentionPolicyServiceTest#migrationCreatesRetentionPolicyAndRunTables,RetentionPolicyServiceTest#tenantOverrideMustStayWithinPlatformBounds
```

Expected: PASS.

### Task 2: Bounded Archive/Delete Jobs

**Files:**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/RetentionPolicyService.java`

- [ ] **Step 1: Add retention job tests**

Append tests:

```java
@Test
void dryRunCountsEligibleRowsWithoutMutating() {
    RetentionPolicyService.RetentionTargetRepository targets = mock(RetentionPolicyService.RetentionTargetRepository.class);
    when(targets.countEligible(0L, "TRACE", 90, true)).thenReturn(42L);
    RetentionPolicyService service = serviceWithTargets(targets);

    RetentionPolicyService.RunResult result = service.run(new RetentionPolicyService.PolicyInput(0L, "TRACE", 90, "ARCHIVE", 100, true));

    assertThat(result.scannedCount()).isEqualTo(42);
    assertThat(result.archivedCount()).isZero();
    verify(targets, never()).archiveBatch(any(), any(), anyInt());
}

@Test
void archiveAndDeleteAreLimitedToMaxBatchSizeAndSkipLegalHold() {
    RetentionPolicyService.RetentionTargetRepository targets = mock(RetentionPolicyService.RetentionTargetRepository.class);
    when(targets.archiveBatch(0L, "EVENT", 90, 10)).thenReturn(10);
    RetentionPolicyService service = serviceWithTargets(targets);

    RetentionPolicyService.RunResult result = service.run(new RetentionPolicyService.PolicyInput(0L, "EVENT", 90, "ARCHIVE", 10, false));

    assertThat(result.archivedCount()).isEqualTo(10);
    verify(targets).archiveBatch(0L, "EVENT", 90, 10);
}
```

- [ ] **Step 2: Implement bounded run**

`run` must compute cutoff from `retentionDays`, skip rows where `legal_hold=1`, and call only one bounded repository method per invocation: `archiveBatch` or `deleteBatch`.

- [ ] **Step 3: Run retention tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RetentionPolicyServiceTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-016b-analytics-retention-and-archive-policy.md`
- Modify: `docs/product-evolution/plans/p2-016b-analytics-retention-and-archive-policy-plan.md`

- [ ] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RetentionPolicyServiceTest
```

Expected: PASS.

- [ ] **Step 2: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V128__analytics_retention_policy.sql backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/RetentionPolicyService.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionPolicyDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionRunDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionPolicyMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionRunMapper.java backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java backend/canvas-engine/src/main/resources/application.yml docs/product-evolution/specs/p2-016b-analytics-retention-and-archive-policy.md docs/product-evolution/plans/p2-016b-analytics-retention-and-archive-policy-plan.md
git commit -m "feat: add analytics retention policy"
```

Expected: commit contains only retention policy schema, service, tests, config, and related docs.
