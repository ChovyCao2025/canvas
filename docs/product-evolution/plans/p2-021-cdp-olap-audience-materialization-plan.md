# CDP OLAP Audience Materialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first production-oriented CDP OLAP audience materialization slice with stable user indexes, bounded behavior rules, versioned bitmaps, and data quality checks.

**Architecture:** Keep CDP OLTP ingestion authoritative, use Doris for bounded behavior-user queries, and publish audience membership into versioned Redis RoaringBitmap keys. Runtime TAGGER remains isolated from Doris by reading ready bitmap versions or existing snapshots.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, Apache Doris DDL, Redis, RoaringBitmap, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-021-cdp-olap-audience-materialization.md`
- Depends on P1-005A CDP event ingestion, P1-006C realtime audience foundations, P2-016 analytics sink work, and existing `AudienceBitmapStore`.

## Current Status Note

The implementation files and focused GREEN verification are present in the current worktree. The historical RED verification steps remain unchecked because the pre-implementation state cannot be reproduced from the current worktree without reverting completed work.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V214__cdp_olap_audience_materialization.sql` - MySQL metadata for user indexes, bitmap versions, runs, and quality checks.
- Create: `backend/canvas-engine/src/main/resources/infrastructure/doris/cdp-audience-ddl.sql` - Doris ODS/DWD/DWS tables for CDP behavior facts.
- Create data objects and mappers under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/`.
- Create: `StableUserIndexService` - duplicate-safe tenant/user to numeric index allocation.
- Create: `VersionedAudienceBitmapStore` - writes and reads versioned Redis bitmap keys.
- Create: `BehaviorAudienceRuleCompiler` - compiles the approved rule JSON contract into bounded query requests.
- Create: `AudienceMaterializationService` - orchestrates OLAP query, user indexing, bitmap write, and run ledger.
- Create: `AudienceQualityService` - evaluates freshness and drift verdicts.
- Create focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/`.

### Task 1: Schema And DDL

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V214__cdp_olap_audience_materialization.sql`
- Create: `backend/canvas-engine/src/main/resources/infrastructure/doris/cdp-audience-ddl.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/CdpOlapAudienceSchemaTest.java`

- [x] **Step 1: Write failing schema tests**

Create `CdpOlapAudienceSchemaTest`:

```java
package org.chovy.canvas.domain.analytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CdpOlapAudienceSchemaTest {

    @Test
    void mysqlMigrationCreatesMaterializationMetadata() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V214__cdp_olap_audience_materialization.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_user_index")
                .contains("UNIQUE KEY uk_cdp_user_index_user")
                .contains("UNIQUE KEY uk_cdp_user_index_value")
                .contains("CREATE TABLE IF NOT EXISTS audience_bitmap_version")
                .contains("CREATE TABLE IF NOT EXISTS audience_materialization_run")
                .contains("CREATE TABLE IF NOT EXISTS audience_quality_check");
    }

    @Test
    void dorisDdlCreatesBehaviorFactAndAggregateTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/infrastructure/doris/cdp-audience-ddl.sql"));

        assertThat(sql)
                .contains("CREATE DATABASE IF NOT EXISTS canvas_ods")
                .contains("CREATE DATABASE IF NOT EXISTS canvas_dwd")
                .contains("CREATE DATABASE IF NOT EXISTS canvas_dws")
                .contains("canvas_ods.cdp_event_log")
                .contains("canvas_dwd.cdp_user_event_fact")
                .contains("canvas_dws.user_event_metric_daily");
    }
}
```

- [ ] **Step 2: Run schema tests to verify red**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpOlapAudienceSchemaTest
```

Expected: FAIL because the migration and Doris DDL do not exist.

- [x] **Step 3: Add MySQL metadata migration**

Create `V214__cdp_olap_audience_materialization.sql` with additive tables for:

- `cdp_user_index`
- `audience_bitmap_version`
- `audience_materialization_run`
- `audience_quality_check`

- [x] **Step 4: Add Doris DDL**

Create `cdp-audience-ddl.sql` with:

- `canvas_ods.cdp_event_log`
- `canvas_dwd.cdp_user_event_fact`
- `canvas_dws.user_event_metric_daily`

- [x] **Step 5: Run schema tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpOlapAudienceSchemaTest
```

Expected: PASS.

Observed on 2026-06-06: covered by `scripts/verify-olap2-focus.sh`.

### Task 2: Stable User Index

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserIndexDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserIndexMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/StableUserIndexService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/StableUserIndexServiceTest.java`

- [x] **Step 1: Write failing stable index tests**

Test behaviors:

- Existing `(tenantId, userId)` returns existing `userIndex`.
- New user allocates from mapper and inserts a row.
- Duplicate insert race reloads the existing row.
- Blank user id is rejected.

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=StableUserIndexServiceTest
```

Expected: FAIL because the service does not exist.

- [x] **Step 3: Implement stable user index files**

Implement data object, mapper, and service. The service must normalize null tenant to `0L`, trim user ids, and use `DuplicateKeyException` handling to make concurrent allocation safe.

- [x] **Step 4: Run stable index tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=StableUserIndexServiceTest
```

Expected: PASS.

Observed on 2026-06-06: covered by `scripts/verify-olap2-focus.sh`.

### Task 3: Versioned Bitmap Store

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceBitmapVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceBitmapVersionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStore.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStoreTest.java`

- [x] **Step 1: Write failing versioned bitmap tests**

Test behaviors:

- Saving version `3` writes Redis key `audience:bitmap:{audienceId}:v:3`.
- Marking a version ready updates metadata and latest pointer.
- Membership checks load the latest ready version.
- Incomplete versions are not used by latest membership checks.

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=VersionedAudienceBitmapStoreTest
```

Expected: FAIL because the store does not exist.

- [x] **Step 3: Implement versioned bitmap store**

Implement Redis binary-safe Base64 storage compatible with current RoaringBitmap serialization. Keep the existing `AudienceBitmapStore` untouched for existing dynamic audiences.

- [x] **Step 4: Run versioned bitmap tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=VersionedAudienceBitmapStoreTest
```

Expected: PASS.

Observed on 2026-06-06: covered by `scripts/verify-olap2-focus.sh`.

### Task 4: Behavior Rule Compiler

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/BehaviorAudienceRuleCompiler.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/BehaviorAudienceRuleCompilerTest.java`

- [x] **Step 1: Write failing compiler tests**

Test behaviors:

- Valid `CDP_EVENT_METRIC` rule compiles to a request containing tenant, event code, window days, metric, operator, threshold, filters, and limit.
- Missing tenant, blank event code, non-positive window, unsupported metric, unsupported operator, unsafe filter path, and more than 10 filters are rejected.

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=BehaviorAudienceRuleCompilerTest
```

Expected: FAIL because the compiler does not exist.

- [x] **Step 3: Implement compiler**

Implement a JSON parser using `ObjectMapper`, explicit enums, a dot-separated filter-path regex, and immutable records for `CompiledBehaviorAudienceQuery` and `Filter`.

- [x] **Step 4: Run compiler tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=BehaviorAudienceRuleCompilerTest
```

Expected: PASS.

Observed on 2026-06-06: covered by `scripts/verify-olap2-focus.sh`.

### Task 5: Audience Materialization Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceMaterializationRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceMaterializationRunMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationServiceTest.java`

- [x] **Step 1: Write failing materialization tests**

Test behaviors:

- Successful materialization creates a run, queries the OLAP repository, maps returned users through `StableUserIndexService`, writes a versioned bitmap, marks the version ready, and marks the run `SUCCESS`.
- Failed OLAP query marks the run `FAILED` and does not publish a ready bitmap.
- Empty OLAP result still publishes a ready empty bitmap.

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceMaterializationServiceTest
```

Expected: FAIL because the service does not exist.

- [x] **Step 3: Implement materialization service**

Implement repository interfaces inside the service:

- `AudienceDefinitionRepository`
- `BehaviorAudienceOlapRepository`

The first implementation should keep infrastructure boundaries injectable and unit-testable without requiring a live Doris connection.

- [x] **Step 4: Run materialization tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceMaterializationServiceTest
```

Expected: PASS.

Observed on 2026-06-06: covered by `scripts/verify-olap2-focus.sh`.

### Task 6: Audience Quality Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceQualityCheckDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceQualityCheckMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceQualityService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceQualityServiceTest.java`

- [x] **Step 1: Write failing quality tests**

Test behaviors:

- Freshness lag within threshold and drift within threshold returns `PASS`.
- Freshness lag above warning threshold returns `WARN`.
- Bitmap drift above fail threshold returns `FAIL`.
- Every verdict inserts a check row.

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceQualityServiceTest
```

Expected: FAIL because the service does not exist.

- [x] **Step 3: Implement quality service**

Implement `evaluate(...)` using explicit thresholds and bounded detail JSON.

- [x] **Step 4: Run quality tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceQualityServiceTest
```

Expected: PASS.

Observed on 2026-06-06: covered by `scripts/verify-olap2-focus.sh`.

### Task 7: Focused Verification

**Files:**
- All files from Tasks 1-6.
- Create: `scripts/verify-olap2-focus.sh` - repeatable Java 21 focused verification entry point.

- [x] **Step 1: Run focused backend tests**

Run:

```bash
scripts/verify-olap2-focus.sh
```

Expected: PASS.

Observed on 2026-06-06: the default shell used Java 8 and failed at `testCompile` with `invalid flag: --release`. Re-running with Java 21 passed:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceMaterializationServiceTest,AudienceQualityServiceTest
```

Result: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`.

Acceleration entry point added on 2026-06-06:

```bash
scripts/verify-olap2-focus.sh --dry-run
scripts/verify-olap2-focus.sh
```

The script ignores a stale Java 8 `JAVA_HOME` when a Java 21 runtime is available through `/usr/libexec/java_home -v 21`. It uses an isolated verification path: compile main sources, build the test classpath, compile only the P2-021 test sources, and run the same six focused backend tests through Surefire. This avoids unrelated dirty-worktree test compilation blockers while preserving the olap2 quality gate. Observed result: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`.

- [x] **Step 2: Review changed files**

Run:

```bash
git status --short
```

Expected: P2-021 files are visible and unrelated dirty files are left untouched.

Observed on 2026-06-06: the focused P2-021 implementation files are present in the current tree; broad unrelated dirty worktree changes remain outside this plan.

- [ ] **Step 3: Commit optional**

Only commit when requested by the operator. This worktree already contains unrelated dirty changes, so do not create a mixed commit without explicit approval.

## Completion Checklist

- P2-021 spec and plan exist and are indexed.
- MySQL metadata migration and Doris DDL exist.
- Stable user indexing is duplicate-safe.
- Behavior audience rule compilation is bounded and rejects unsupported input.
- Materialization publishes versioned bitmaps only after successful OLAP results.
- Quality verdicts are recorded.
- Focused backend tests pass.
