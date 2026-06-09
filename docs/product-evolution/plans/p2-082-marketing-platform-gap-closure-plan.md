# Marketing Platform Gap Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the missing production marketing-platform capabilities in sequenced slices. Attribution v2, experiment governance, loyalty, SCRM workspace, private-domain sync, paid-media activation, AI decisioning, sentiment/competitor monitoring, and selected support foundations now have implemented first slices.

**Architecture:** Keep the work decomposed by bounded product track. The first executable slice extends existing canvas attribution tables and service logic in place; later slices add independent domain modules for experimentation, loyalty, private-domain/SCRM, paid-media activation, AI decisioning, and monitoring.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React, TypeScript, Vitest.

**Implementation Status:** Current workspace record: P2-082A through P2-082AC and listed supporting foundations are done as first slices; P2-082AD remains in progress.

---

## Scope

This plan indexes the full P2-082 gap-closure program. P2-082A was implemented first because it is the lowest-risk shared dependency for ROI, experiments, and paid-media activation. P2-082B and P2-082C are now implemented as backend first slices.

Current implementation status:

1. P2-082A attribution v2: done.
2. P2-082B experiment metrics and governance first slice: done.
3. P2-082C loyalty accounts, rules, and redemption first slice: done.
4. P2-082D SCRM operator workspace backend and frontend first slice: done.
5. Supporting conversation adapter catalog, WhatsApp/Web Chat adapters, BI datasource schema-to-dataset onboarding, BI dashboard runtime state, and OLAP evidence automation/query SLO gates: done.
6. P2-082D2 private-domain contact and group sync backend first slice: done.
7. P2-082E paid-media audience sync and audit providers backend first slice: done.
8. P2-082F AI decision models: done.
9. P2-082G sentiment and competitor monitoring backend first slice: done.
10. P2-082H monitoring workbench frontend first slice: done.
11. P2-082I monitoring webhook ingestion backend first slice: done.
12. P2-082J monitoring alert fanout backend first slice: done.
13. P2-082K SCRM routing and SLA backend first slice: done.
14. P2-082L SCRM AI reply assistance backend first slice: done.
15. P2-082M monitoring polling and trends backend first slice: done.
16. P2-082N monitoring scheduler and trend workbench slice: done.
17. P2-082O creator/KOL/KOC collaboration foundation: done.
18. P2-082P SEO/SEM search marketing foundation: done.
19. P2-082Q DSP/programmatic advertising foundation: done.
20. P2-082R monitoring anomaly detection foundation: done.
21. P2-082S monitoring provider connectors: done.
22. P2-082T monitoring LLM sentiment inference governance: done.
23. P2-082U monitoring provider credential lifecycle: done.
24. P2-082V monitoring provider OAuth authorization: done.
25. P2-082W monitoring provider OAuth refresh and revocation: done.
26. P2-082X monitoring provider OAuth wizard UI: done.
27. P2-082Y search marketing provider write gateway: done.
28. P2-082Z creator provider write gateway: done.
29. P2-082AA programmatic DSP provider write gateway: done.
30. P2-082AB provider write operations UI: done.
31. P2-082AC provider write adapter contracts: done.
32. P2-082AD search marketing production closed loop: in progress.

## Files

- Create `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Create `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V274__attribution_multi_touch_models.sql`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionSchemaTest.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasConversionAttributionDO.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasAttributionService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerEffectClosureTest.java`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V276__ab_experiment_metrics_governance.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGovernanceService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentGovernanceController.java`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V277__loyalty_accounts_rules_redemption.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/loyalty/LoyaltyService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/LoyaltyController.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalog.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyAdapter.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyPayload.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceService.java`.

## Tasks

### Task 1: Index P2-082 Program Documents

**Files:**
- Create: `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`
- Create: `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`

- [x] **Step 1: Insert P2-082 index rows**

Insert P2-082 immediately after P2-081 in all three indexes.

- [x] **Step 2: Verify indexability**

Run:

```bash
rg -n "P2-082|p2-082-marketing-platform-gap-closure" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md
```

Expected: every listed file returns at least one P2-082 match.

### Task 2: Add Attribution v2 Schema Contract

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V274__attribution_multi_touch_models.sql`

- [x] **Step 1: Write the failing schema test**

Create `CanvasAttributionSchemaTest`:

```java
package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasAttributionSchemaTest {

    @Test
    void migrationAddsConfigurableMultiTouchAttributionColumns() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V274__attribution_multi_touch_models.sql").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("ADD COLUMN `attribution_model` VARCHAR(32) NOT NULL DEFAULT 'LAST_TOUCH'");
        assertThat(sql).contains("ADD COLUMN `attribution_weight` DECIMAL(12,8) NOT NULL DEFAULT 1.00000000");
        assertThat(sql).contains("ADD COLUMN `touch_created_at` DATETIME NULL");
        assertThat(sql).contains("DROP INDEX `uk_canvas_attr_event`");
        assertThat(sql).contains("UNIQUE KEY `uk_canvas_attr_event_model_touch`");
    }
}
```

- [x] **Step 2: Run the schema test and verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CanvasAttributionSchemaTest
```

Expected before migration: FAIL because `V274__attribution_multi_touch_models.sql` is missing.

- [x] **Step 3: Add the migration**

Create `V274__attribution_multi_touch_models.sql`:

```sql
ALTER TABLE `canvas`
  ADD COLUMN `attribution_model` VARCHAR(32) NOT NULL DEFAULT 'LAST_TOUCH' AFTER `attribution_window_days`;

ALTER TABLE `canvas_conversion_attribution`
  ADD COLUMN `attribution_weight` DECIMAL(12,8) NOT NULL DEFAULT 1.00000000 AFTER `attribution_model`,
  ADD COLUMN `touch_created_at` DATETIME NULL AFTER `attribution_weight`,
  DROP INDEX `uk_canvas_attr_event`,
  ADD UNIQUE KEY `uk_canvas_attr_event_model_touch` (`canvas_id`, `event_log_id`, `attribution_model`, `send_record_id`),
  ADD KEY `idx_canvas_attr_model_time` (`canvas_id`, `attribution_model`, `attributed_at`);
```

- [x] **Step 4: Verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CanvasAttributionSchemaTest
```

Expected: PASS.

### Task 3: Add Attribution v2 Service Behavior

**Files:**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasConversionAttributionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasAttributionService.java`

- [x] **Step 1: Write failing service tests**

Add tests proving:

- `FIRST_TOUCH` attributes only the earliest eligible sent record with weight `1.00000000`.
- `LINEAR` attributes every eligible sent record and divides weight equally.
- `TIME_DECAY` attributes every eligible sent record, gives newer records larger weights, and sums weights to `1.00000000`.
- Existing `LAST_TOUCH` behavior still attributes only the latest eligible record.

- [x] **Step 2: Run service tests and verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CanvasAttributionServiceTest
```

Expected before implementation: FAIL because the service still queries one latest touch and does not set weights.

- [x] **Step 3: Implement the minimal service changes**

Implementation details:

- Add `private String attributionModel;` to `CanvasDO`.
- Add `private BigDecimal attributionWeight;` and `private LocalDateTime touchCreatedAt;` to `CanvasConversionAttributionDO`.
- Replace `latestTouch` with `eligibleTouches` using `selectList`, ordered by `createdAt ASC`.
- Normalize unknown models to `LAST_TOUCH`.
- For `FIRST_TOUCH`, insert one row for the first touch.
- For `LAST_TOUCH`, insert one row for the last touch.
- For `LINEAR`, insert every touch with weight `1 / touchCount`, scale 8, half-up.
- For `TIME_DECAY`, compute score `0.5 ^ ageDays`, divide each score by total score, scale 8, half-up.
- For no eligible touch, insert one unattributed row with `sendRecordId=0`, weight `1.00000000`, and null `touchCreatedAt`; the `0` sentinel avoids MySQL unique-key duplicate gaps caused by nullable key columns.
- Continue ignoring `DuplicateKeyException`.

- [x] **Step 4: Verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CanvasAttributionServiceTest
```

Expected: PASS.

### Task 4: Keep Attribution Summary Backward Compatible

**Files:**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerEffectClosureTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`

- [x] **Step 1: Write failing summary test**

Extend the summary test so mapper rows with weighted fields return:

- `conversions` from `COUNT(DISTINCT event_log_id)`.
- `conversionAmount` from weighted amount.
- `attributedSends` from distinct non-null send records.
- `models` as a comma-separated model list.

- [x] **Step 2: Run stats test and verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CanvasStatsControllerEffectClosureTest
```

Expected before implementation: FAIL because the controller hardcodes `model=LAST_TOUCH` and sums unweighted rows.

- [x] **Step 3: Implement summary query update**

Use this aggregate shape:

```sql
COUNT(DISTINCT event_log_id) AS conversions,
COALESCE(SUM(conversion_amount * attribution_weight), 0) AS conversionAmount,
COUNT(DISTINCT send_record_id) AS attributedSends,
GROUP_CONCAT(DISTINCT attribution_model ORDER BY attribution_model) AS models
```

Return both `model` and `models`; keep `model=LAST_TOUCH` as default when the row has no model data.

- [x] **Step 4: Verify focused backend tests**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CanvasAttributionSchemaTest,CanvasAttributionServiceTest,CanvasStatsControllerEffectClosureTest
```

Expected: PASS.

### Task 5: Add Experiment Metrics and Governance Backend Slice

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V276__ab_experiment_metrics_governance.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGovernanceService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentGovernanceController.java`
- Create/modify focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`

- [x] **Step 1: Add governance schema contract**
- [x] **Step 2: Add governance service behavior**
- [x] **Step 3: Expose governance controller API**
- [x] **Step 4: Verify focused backend tests**

Verified command:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AbExperimentGovernanceControllerTest,AbExperimentGovernanceSchemaTest,AbExperimentGovernanceServiceTest,AbExperimentGroupServiceTest test
```

### Task 6: Add Loyalty Accounts, Rules, and Redemption Backend Slice

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V277__loyalty_accounts_rules_redemption.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/loyalty/LoyaltyService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/LoyaltyController.java`
- Create/modify loyalty data objects, mappers, and focused tests.

- [x] **Step 1: Add loyalty schema contract**
- [x] **Step 2: Add account, rule, journal, and redemption service behavior**
- [x] **Step 3: Expose loyalty controller API**
- [x] **Step 4: Verify focused backend tests**

Verified command:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=LoyaltySchemaTest,LoyaltyServiceTest,LoyaltyControllerTest test
```

### Task 7: Add Supporting Adapter, BI Onboarding, and OLAP Evidence Fixes

**Files:**
- Create/modify conversation adapter catalog, harness, WhatsApp/Web Chat adapters, and tests.
- Create/modify BI datasource schema-to-dataset command/service/controller entrypoint and tests.
- Modify OLAP evidence automation/query SLO proof gates and tests.

- [x] **Step 1: Verify conversation adapter catalog and harness**
- [x] **Step 2: Add Web Chat conversation adapter**
- [x] **Step 3: Add BI datasource schema-to-dataset backend onboarding**
- [x] **Step 4: Verify OLAP evidence automation and query SLO focused tests**

Verified commands:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WebChatConversationReplyAdapterTest test
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=BiDatasetFromDatasourceServiceTest,BiDatasetControllerTest test
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=CdpWarehouseEnterpriseOlapEvidenceServiceTest,CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest,CdpWarehouseEnterpriseOlapEvidenceSchedulerTest,CdpWarehouseEnterpriseOlapEvidenceControllerTest,ApplicationYamlTest test
```

### Task 8: Implement Next Gap Track - SCRM Workspace

**Files:** see `docs/product-evolution/plans/p2-082d-scrm-operator-workspace-plan.md`.

- [x] **Step 1: Add operator inbox schema and service**
- [x] **Step 2: Add customer conversation timeline API**
- [x] **Step 3: Add assignment, reminders, SOP tasks, and audit trail**
- [x] **Step 4: Add frontend operator workspace**
- [x] **Step 5: Verify backend focused tests**
