# Operating Loop And Effect Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect template start, pre-publish validation, control-group holdout, last-touch attribution, receipt stats, and version diff into one measurable operator loop.

**Architecture:** Reuse existing canvas template, dry-run, stats, message-send-record, event-log, diff, and revert baselines. Add small domain services for missing checks, control-group decisions, and attribution; expose thin controller endpoints; keep frontend display logic in pure helper modules with focused tests.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway SQL, Redis-backed trigger precheck, React 18, Ant Design, Axios, Vitest.

## Implementation Status

- Status: implemented and verified on 2026-06-05.
- Actual migration: `backend/canvas-engine/src/main/resources/db/migration/V96__operating_loop_effect_closure.sql`; `V94` and `V95` already existed in the migration sequence.
- Commit status: not committed in this session because the worktree contains many unrelated existing changes that need scope confirmation before staging.

---

## Spec Reference

- `docs/product-evolution/specs/p1-001-operating-loop-and-effect-closure.md`
- Source item: `docs/product-evolution/todo/p1/operating-loop-and-effect-closure.md`

## File Structure

**Backend**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasControlGroupService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasAttributionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/EventDefinitionServiceImpl.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasControlGroupHoldoutDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasConversionAttributionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasControlGroupHoldoutMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasConversionAttributionMapper.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V96__operating_loop_effect_closure.sql`

**Frontend**
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`
- Create: `frontend/src/pages/canvas-list/templateCatalog.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `frontend/src/pages/canvas-editor/prePublishChecks.ts`
- Modify: `frontend/src/pages/canvas-stats/index.tsx`
- Create: `frontend/src/pages/canvas-stats/effectClosure.ts`

**Tests**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasControlGroupServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerEffectClosureTest.java`
- Create: `frontend/src/pages/canvas-list/templateCloneFlow.test.ts`
- Create: `frontend/src/pages/canvas-editor/prePublishChecks.test.ts`
- Create: `frontend/src/pages/canvas-stats/effectClosure.test.ts`

### Task 1: Template Catalog And One-Click Clone

**Files:**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-list/templateCatalog.ts`
- Create: `frontend/src/pages/canvas-list/templateCloneFlow.test.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [x] **Step 1: Extend backend template test**

Add this test to `OpsControllerTemplateTest`.

```java
@Test
void listTemplatesFiltersByCategoryAndOrdersByUseCount() {
    OpsController controller = new OpsController(
            templateMapper, canvasMapper, canvasVersionMapper, approvalMapper, configCache);

    controller.listTemplates("retention").block();

    ArgumentCaptor<LambdaQueryWrapper<CanvasTemplateDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    verify(templateMapper).selectList(captor.capture());
    assertThat(captor.getValue().getExpression().getNormal().toString()).contains("category");
    assertThat(captor.getValue().getExpression().getOrderBy().toString()).contains("use_count");
}
```

- [x] **Step 2: Write frontend template helper tests**

Create `templateCloneFlow.test.ts`.

```ts
import { describe, expect, it } from 'vitest'
import { buildTemplateCategoryOptions, buildTemplateCloneSuccessMessage } from './templateCatalog'

describe('template catalog helpers', () => {
  it('builds sorted category options from templates', () => {
    expect(buildTemplateCategoryOptions([
      { id: 1, name: 'A', category: 'retention', useCount: 2 },
      { id: 2, name: 'B', category: 'activation', useCount: 1 },
    ])).toEqual([
      { label: 'activation', value: 'activation' },
      { label: 'retention', value: 'retention' },
    ])
  })

  it('builds clone success copy with new canvas id', () => {
    expect(buildTemplateCloneSuccessMessage({ id: 88, name: '新客旅程' }))
      .toBe('已从模板创建「新客旅程」(ID: 88)')
  })
})
```

- [x] **Step 3: Run template tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerTemplateTest && cd ../frontend && npm test -- templateCloneFlow.test.ts
```

Expected: frontend test FAIL because `templateCatalog.ts` does not exist; backend test should PASS or reveal that `listTemplates` needs enabled filtering.

- [x] **Step 4: Add frontend API wrappers**

In `api.ts`, add the template type and methods under `canvasApi`.

```ts
export interface CanvasTemplate {
  id: number
  name: string
  description?: string
  category?: string
  useCount: number
}

listTemplates: (category?: string) =>
  http.get<R<CanvasTemplate[]>, R<CanvasTemplate[]>>('/canvas/templates', { params: { category } }),

createFromTemplate: (templateId: number, name?: string) =>
  http.post<R<Canvas>, R<Canvas>>(`/canvas/from-template/${templateId}`, { name }),
```

- [x] **Step 5: Implement template helper and list-page entry**

Create `templateCatalog.ts`.

```ts
import type { Canvas, CanvasTemplate } from '../../services/api'

export function buildTemplateCategoryOptions(templates: Pick<CanvasTemplate, 'category'>[]) {
  return Array.from(new Set(templates.map(t => t.category).filter(Boolean) as string[]))
    .sort()
    .map(value => ({ label: value, value }))
}

export function buildTemplateCloneSuccessMessage(canvas: Pick<Canvas, 'id' | 'name'>) {
  return `已从模板创建「${canvas.name}」(ID: ${canvas.id})`
}
```

In `canvas-list/index.tsx`, add a "从模板创建" button next to "新建画布", load templates with `canvasApi.listTemplates`, and call `canvasApi.createFromTemplate` from the modal confirm action.

- [x] **Step 6: Run template tests**

Run:

```bash
cd frontend && npm test -- templateCloneFlow.test.ts
```

Expected: PASS.

### Task 2: Pre-Publish Checks

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Create: `frontend/src/pages/canvas-editor/prePublishChecks.test.ts`
- Create: `frontend/src/pages/canvas-editor/prePublishChecks.ts`
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [x] **Step 1: Write backend pre-publish tests**

Create `CanvasPrePublishCheckServiceTest`.

```java
@Test
void invalidGraphProducesBlockingError() {
    when(canvasVersionMapper.selectOne(any())).thenReturn(version("{"));

    CanvasPrePublishCheckService.Result result = service.check(10L);

    assertThat(result.blocking()).isTrue();
    assertThat(result.items()).anyMatch(item -> item.code().equals("GRAPH_JSON_INVALID") && item.severity().equals("ERROR"));
}

@Test
void graphWithoutEntryNodeProducesBlockingError() {
    when(canvasVersionMapper.selectOne(any())).thenReturn(version("{\"nodes\":[{\"id\":\"n1\",\"type\":\"SEND_MESSAGE\"}],\"edges\":[]}"));

    CanvasPrePublishCheckService.Result result = service.check(10L);

    assertThat(result.items()).anyMatch(item -> item.code().equals("NO_ENTRY_NODE"));
}
```

- [x] **Step 2: Write frontend check presentation tests**

Create `prePublishChecks.test.ts`.

```ts
import { describe, expect, it } from 'vitest'
import { canPublishFromChecks, summarizePrePublishChecks } from './prePublishChecks'

describe('prePublishChecks helpers', () => {
  it('blocks publish when any item is ERROR', () => {
    expect(canPublishFromChecks({ blocking: true, items: [{ code: 'NO_ENTRY_NODE', severity: 'ERROR', message: '缺少触发器' }] })).toBe(false)
  })

  it('summarizes warnings and errors', () => {
    expect(summarizePrePublishChecks({
      blocking: true,
      items: [
        { code: 'NO_ENTRY_NODE', severity: 'ERROR', message: '缺少触发器' },
        { code: 'NO_TEST_SEND', severity: 'WARNING', message: '尚未测试发送' },
      ],
    })).toEqual({ errors: 1, warnings: 1 })
  })
})
```

- [x] **Step 3: Run pre-publish tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasPrePublishCheckServiceTest && cd ../frontend && npm test -- prePublishChecks.test.ts
```

Expected: FAIL because the service and frontend helper do not exist.

- [x] **Step 4: Implement backend service and endpoint**

Create `CanvasPrePublishCheckService` with records:

```java
public record CheckItem(String code, String severity, String message) {}
public record Result(boolean blocking, List<CheckItem> items) {}
```

The `check(Long canvasId)` method reads the latest draft version, parses `graphJson` with `DagParser`, adds `GRAPH_JSON_INVALID` on parse failure, adds `NO_ENTRY_NODE` when `entryNodes()` is empty, and adds `WARNING` items for missing test-send evidence until P2-017 adds durable test-send records.

Add this endpoint to `CanvasController`.

```java
@GetMapping("/{id}/pre-publish-checks")
public Mono<R<CanvasPrePublishCheckService.Result>> prePublishChecks(@PathVariable Long id) {
    return Mono.fromCallable(() -> prePublishCheckService.check(id))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
```

- [x] **Step 5: Implement frontend helper and publish wiring**

Create `prePublishChecks.ts`.

```ts
export interface PrePublishCheckItem {
  code: string
  severity: 'ERROR' | 'WARNING'
  message: string
}

export interface PrePublishCheckResult {
  blocking: boolean
  items: PrePublishCheckItem[]
}

export function canPublishFromChecks(result: PrePublishCheckResult) {
  return !result.blocking && result.items.every(item => item.severity !== 'ERROR')
}

export function summarizePrePublishChecks(result: PrePublishCheckResult) {
  return {
    errors: result.items.filter(item => item.severity === 'ERROR').length,
    warnings: result.items.filter(item => item.severity === 'WARNING').length,
  }
}
```

Add `canvasApi.prePublishChecks(id)` in `api.ts`. In the editor publish handler, call it after saving the draft and before `canvasApi.publish`; stop and show a modal when `canPublishFromChecks` returns false.

- [x] **Step 6: Run pre-publish tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasPrePublishCheckServiceTest && cd ../frontend && npm test -- prePublishChecks.test.ts
```

Expected: PASS.

### Task 3: Control Group Holdout

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasControlGroupServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasControlGroupService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasControlGroupHoldoutDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasControlGroupHoldoutMapper.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V96__operating_loop_effect_closure.sql`

- [x] **Step 1: Write deterministic holdout tests**

Create `CanvasControlGroupServiceTest`.

```java
@Test
void sameCanvasUserAndSaltProduceStableDecision() {
    CanvasDO canvas = new CanvasDO();
    canvas.setId(9L);
    canvas.setControlGroupPercent(10);
    canvas.setControlGroupSalt("salt-a");

    boolean first = service.isHeldOut(canvas, "user-1");
    boolean second = service.isHeldOut(canvas, "user-1");

    assertThat(second).isEqualTo(first);
}

@Test
void zeroPercentNeverHoldsOut() {
    CanvasDO canvas = new CanvasDO();
    canvas.setControlGroupPercent(0);

    assertThat(service.isHeldOut(canvas, "user-1")).isFalse();
}
```

- [x] **Step 2: Run control-group tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasControlGroupServiceTest
```

Expected: FAIL because the service, mapper, and canvas fields do not exist.

- [x] **Step 3: Add migration and data objects**

Create `V96__operating_loop_effect_closure.sql`.

```sql
ALTER TABLE canvas
  ADD COLUMN control_group_percent INT NOT NULL DEFAULT 0 AFTER cooldown_seconds,
  ADD COLUMN control_group_salt VARCHAR(64) NULL AFTER control_group_percent,
  ADD COLUMN conversion_event_code VARCHAR(128) NULL AFTER control_group_salt,
  ADD COLUMN attribution_window_days INT NOT NULL DEFAULT 7 AFTER conversion_event_code;

CREATE TABLE canvas_control_group_holdout (
  id BIGINT NOT NULL AUTO_INCREMENT,
  canvas_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  event_id VARCHAR(128) NULL,
  reason VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_canvas_control_holdout (canvas_id, user_id, event_id),
  KEY idx_canvas_control_canvas_created (canvas_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Control group holdout audit records';
```

Add matching fields to `CanvasDO` and create `CanvasControlGroupHoldoutDO` plus `CanvasControlGroupHoldoutMapper extends BaseMapper<CanvasControlGroupHoldoutDO>`.

- [x] **Step 4: Implement holdout service**

Create `CanvasControlGroupService`.

```java
public boolean isHeldOut(CanvasDO canvas, String userId) {
    int percent = canvas.getControlGroupPercent() == null ? 0 : canvas.getControlGroupPercent();
    if (percent <= 0) return false;
    if (percent > 50) throw new IllegalArgumentException("controlGroupPercent cannot exceed 50");
    String salt = canvas.getControlGroupSalt() == null ? "default" : canvas.getControlGroupSalt();
    int bucket = Math.floorMod((canvas.getId() + ":" + salt + ":" + userId).hashCode(), 10_000);
    return bucket < percent * 100;
}
```

`recordHoldout` inserts a holdout row with reason `CONTROL_GROUP` and ignores duplicate-key exceptions.

- [x] **Step 5: Apply holdout before quota consumption**

Inject `CanvasControlGroupService` into `TriggerPreCheckService`. In `checkWithoutQuotaAccounting`, after status/effective-time checks and before cooldown/quota checks, add:

```java
if (controlGroupService.isHeldOut(canvas, userId)) {
    controlGroupService.recordHoldout(canvas.getId(), userId, null, "CONTROL_GROUP");
    throw new TriggerRejectedException("CONTROL_001", "control group holdout");
}
```

- [x] **Step 6: Run control-group tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasControlGroupServiceTest,TriggerPreCheckServiceTest
```

Expected: PASS; held-out users are rejected before quota counters are consumed.

### Task 4: Last-Touch Attribution And Receipt Stats

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerEffectClosureTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasAttributionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasConversionAttributionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasConversionAttributionMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/EventDefinitionServiceImpl.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V96__operating_loop_effect_closure.sql`

- [x] **Step 1: Write attribution tests**

Create `CanvasAttributionServiceTest`.

```java
@Test
void attributesConversionToLatestPriorSentRecord() {
    CanvasDO canvas = canvasWithConversion("ORDER_PAID");
    MessageSendRecordDO send = sentRecord(22L, 10L, "user-1", LocalDateTime.parse("2026-06-01T10:00:00"));
    when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
    when(sendRecordMapper.selectOne(any())).thenReturn(send);

    service.attribute(eventLog(99L, "ORDER_PAID", "user-1", LocalDateTime.parse("2026-06-01T12:00:00")));

    verify(attributionMapper).insert(argThat(row ->
            row.getCanvasId().equals(10L) && row.getSendRecordId().equals(22L) && row.getEventLogId().equals(99L)));
}

@Test
void duplicateAttributionIsIgnoredByEventAndCanvas() {
    doThrow(new DuplicateKeyException("duplicate")).when(attributionMapper).insert(any());

    assertThatCode(() -> service.insertAttribution(row())).doesNotThrowAnyException();
}
```

- [x] **Step 2: Write stats read-model tests**

Create `CanvasStatsControllerEffectClosureTest`.

```java
@Test
void receiptsReturnStatusCountsForCanvas() {
    when(messageSendRecordMapper.selectMaps(any())).thenReturn(List.of(
            Map.of("status", "SENT", "count", 3L),
            Map.of("status", "FAILED", "count", 1L)));

    Map<String, Object> data = controller.receipts(10L).block().getData();

    assertThat(data).containsEntry("sent", 3L).containsEntry("failed", 1L);
}
```

- [x] **Step 3: Run attribution tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasAttributionServiceTest,CanvasStatsControllerEffectClosureTest
```

Expected: FAIL because attribution service, mapper, and stats endpoints do not exist.

- [x] **Step 4: Extend migration for attribution**

Append this table to `V96__operating_loop_effect_closure.sql`.

```sql
CREATE TABLE canvas_conversion_attribution (
  id BIGINT NOT NULL AUTO_INCREMENT,
  canvas_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  event_log_id BIGINT NOT NULL,
  send_record_id BIGINT NULL,
  conversion_event_code VARCHAR(128) NOT NULL,
  conversion_amount DECIMAL(18,2) NULL,
  attribution_model VARCHAR(32) NOT NULL DEFAULT 'LAST_TOUCH',
  attributed_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_canvas_attr_event (canvas_id, event_log_id),
  KEY idx_canvas_attr_canvas_time (canvas_id, attributed_at),
  KEY idx_canvas_attr_send_record (send_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Last-touch conversion attribution';
```

- [x] **Step 5: Implement attribution service**

Create `CanvasAttributionService`. It reads canvases where `conversion_event_code = eventLog.eventCode`, finds the latest `MessageSendRecordDO.STATUS_SENT` for the same canvas and user before `eventLog.createdAt` and after `eventLog.createdAt - attributionWindowDays`, then inserts `CanvasConversionAttributionDO`.

```java
public void attribute(EventLogDO eventLog) {
    List<CanvasDO> canvases = canvasMapper.selectList(new LambdaQueryWrapper<CanvasDO>()
            .eq(CanvasDO::getConversionEventCode, eventLog.getEventCode()));
    for (CanvasDO canvas : canvases) {
        MessageSendRecordDO touch = latestTouch(canvas, eventLog);
        insertAttribution(from(canvas, eventLog, touch));
    }
}
```

Parse `conversionAmount` from `eventLog.attributes` when present; leave it null when absent.

- [x] **Step 6: Hook attribution after event log write**

Inject `CanvasAttributionService` into `EventDefinitionServiceImpl`. After `EventLogDO eventLog = writeEventLog(...)`, call:

```java
canvasAttributionService.attribute(eventLog);
```

Catch and log attribution exceptions so event reporting still returns `ACCEPTED` when attribution storage is temporarily unavailable.

- [x] **Step 7: Add stats endpoints**

In `CanvasStatsController`, add:

```java
@GetMapping("/receipts")
public Mono<R<Map<String, Object>>> receipts(@PathVariable Long id) { ... }

@GetMapping("/attribution-summary")
public Mono<R<Map<String, Object>>> attributionSummary(@PathVariable Long id) { ... }
```

`receipts` returns lower-case status counts from `message_send_record`. `attributionSummary` returns `conversions`, `conversionAmount`, `attributedSends`, and `model = "LAST_TOUCH"`.

- [x] **Step 8: Run attribution tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasAttributionServiceTest,CanvasStatsControllerEffectClosureTest
```

Expected: PASS.

### Task 5: Frontend Effect Closure And Version Diff

**Files:**
- Create: `frontend/src/pages/canvas-stats/effectClosure.test.ts`
- Create: `frontend/src/pages/canvas-stats/effectClosure.ts`
- Modify: `frontend/src/pages/canvas-stats/index.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/services/api.ts`

- [x] **Step 1: Write stats display helper tests**

Create `effectClosure.test.ts`.

```ts
import { describe, expect, it } from 'vitest'
import { buildAttributionKpis, buildReceiptStatusRows } from './effectClosure'

describe('effectClosure helpers', () => {
  it('builds receipt status rows in fixed order', () => {
    expect(buildReceiptStatusRows({ sent: 3, failed: 1, skipped: 2 })).toEqual([
      { status: 'SENT', label: '已发送', count: 3 },
      { status: 'FAILED', label: '失败', count: 1 },
      { status: 'SKIPPED', label: '策略跳过', count: 2 },
    ])
  })

  it('builds attribution KPI copy', () => {
    expect(buildAttributionKpis({ conversions: 4, conversionAmount: 99.5, attributedSends: 3, model: 'LAST_TOUCH' }))
      .toContainEqual({ label: '转化金额', value: '99.50' })
  })
})
```

- [x] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- effectClosure.test.ts
```

Expected: FAIL because `effectClosure.ts` does not exist.

- [x] **Step 3: Add frontend API wrappers**

In `api.ts`, add:

```ts
receipts: (id: number) =>
  http.get<R<Record<string, number>>, R<Record<string, number>>>(`/canvas/${id}/receipts`),

attributionSummary: (id: number) =>
  http.get<R<AttributionSummary>, R<AttributionSummary>>(`/canvas/${id}/attribution-summary`),

diffVersions: (id: number, v1: number, v2: number) =>
  http.get<R<Record<string, unknown>>, R<Record<string, unknown>>>(`/canvas/${id}/versions/${v1}/diff/${v2}`),
```

- [x] **Step 4: Implement stats helpers and page wiring**

Create `effectClosure.ts` with fixed-order receipt rows and KPI formatting. In `canvas-stats/index.tsx`, load `canvasApi.receipts(id)` and `canvasApi.attributionSummary(id)` with the existing stats requests and render a compact delivery table plus attribution KPI row under the trend chart.

- [x] **Step 5: Add version diff action in editor drawer**

In `canvas-editor/index.tsx`, add a diff button for each version row. The button compares that row to the current draft version ID using `canvasApi.diffVersions`, then displays `summary.addedCount`, `summary.removedCount`, and `summary.modifiedCount` in a modal. Keep the existing revert button behavior unchanged.

- [x] **Step 6: Run frontend tests and build**

Run:

```bash
cd frontend && npm test -- templateCloneFlow.test.ts prePublishChecks.test.ts effectClosure.test.ts && npm run build
```

Expected: PASS.

### Task 6: Regression And Rollout

**Files:**
- Modify: `docs/product-evolution/specs/p1-001-operating-loop-and-effect-closure.md`
- Modify: `docs/product-evolution/plans/p1-001-operating-loop-and-effect-closure-plan.md`

- [x] **Step 1: Run focused backend suite**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerTemplateTest,CanvasPrePublishCheckServiceTest,CanvasControlGroupServiceTest,CanvasAttributionServiceTest,CanvasStatsControllerEffectClosureTest
```

Expected: PASS.

- [x] **Step 2: Run focused frontend suite**

Run:

```bash
cd frontend && npm test -- templateCloneFlow.test.ts prePublishChecks.test.ts effectClosure.test.ts
```

Expected: PASS.

- [x] **Step 3: Run module regressions**

Run:

```bash
cd backend && mvn -pl canvas-engine test && cd ../frontend && npm test && npm run build
```

Expected: PASS for backend tests, frontend tests, and production build.

- [x] **Step 4: Add rollout notes to the implementation PR**

Use this exact checklist in the PR body.

```markdown
Rollout notes:
- Existing canvases default to `control_group_percent = 0`, so no users are held out until configured.
- Attribution is last-touch within `attribution_window_days`; it is not multi-touch ROI.
- Conversion attribution is idempotent by `(canvas_id, event_log_id)`.
- Template clone creates draft canvases and does not publish or register runtime routes.
- Pre-publish `ERROR` blocks publish; `WARNING` is informational.
```

### Verification Evidence

- Focused backend suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerTemplateTest,CanvasPrePublishCheckServiceTest,CanvasControllerOperatorLoopTest,CanvasControlGroupServiceTest,TriggerPreCheckServiceControlGroupTest,CanvasAttributionServiceTest,CanvasStatsControllerEffectClosureTest,CanvasServiceDraftUpdateStateTest
```

Result: 23 tests, 0 failures, 0 errors, 0 skipped.

- Backend module regression:

```bash
cd backend && mvn -pl canvas-engine test
```

Result: 1365 tests, 0 failures, 0 errors, 1 skipped.

- Focused frontend suite:

```bash
cd frontend && npm test -- templateCloneFlow.test.ts prePublishChecks.test.ts effectClosure.test.ts
```

Result: 3 test files, 6 tests passed.

- Frontend module regression:

```bash
cd frontend && npm test
```

Result: 70 test files, 257 tests passed.

- Frontend production build:

```bash
cd frontend && npm run build
```

Result: passed.

- [ ] **Step 5: Commit the implementation slice**

Run:

```bash
git add backend/canvas-engine/src frontend/src docs/product-evolution/specs/p1-001-operating-loop-and-effect-closure.md docs/product-evolution/plans/p1-001-operating-loop-and-effect-closure-plan.md && git commit -m "feat: close operating loop metrics"
```

Expected: commit contains the P1-001 operating-loop implementation.
