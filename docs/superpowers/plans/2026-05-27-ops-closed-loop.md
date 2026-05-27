# Ops Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the operations workflow layer: operations center, high-risk publish review, exception/DLQ handling entry points, template center, and audit visibility.

**Architecture:** Execute after `feat/saas-foundation` and create `feat/ops-closed-loop` from that branch. Reuse existing canvas publish, templates, DLQ, execution requests, MQ rejected, notifications, and pending review endpoints. Add a small risk evaluator and review flow around publish rather than building a generic BPM engine.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, React 18, TypeScript, Ant Design 5, Vitest, Maven.

---

## Worktree Setup

- [ ] **Step 1: Create worktree from SaaS foundation**

```bash
git fetch --all
git worktree add .worktrees/feat-ops-closed-loop -b feat/ops-closed-loop feat/saas-foundation
cd .worktrees/feat-ops-closed-loop
```

Expected: branch `feat/ops-closed-loop` starts with tenant context and role model from `feat/saas-foundation`.

## File Structure

### Backend

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPublishRisk.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPublishRiskEvaluator.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/OpsCenterSummaryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops/OpsCenterService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsCenterController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishRiskEvaluatorTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsCenterControllerTest.java`

### Frontend

- Create: `frontend/src/services/opsApi.ts`
- Create: `frontend/src/pages/ops-center/opsPresentation.ts`
- Create: `frontend/src/pages/ops-center/opsPresentation.test.ts`
- Create: `frontend/src/pages/ops-center/index.tsx`
- Create: `frontend/src/pages/templates/index.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

## Task 1: Publish Risk Evaluation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPublishRisk.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPublishRiskEvaluator.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishRiskEvaluatorTest.java`

- [ ] **Step 1: Write risk evaluator tests**

```java
@Test
void detectsHighRiskNodesAndMissingLimit() {
    String graphJson = """
        {"nodes":[
          {"id":"start","type":"START","name":"开始","config":{"nextNodeId":"coupon"}},
          {"id":"coupon","type":"COUPON","name":"发券","config":{"nextNodeId":"groovy"}},
          {"id":"groovy","type":"GROOVY","name":"脚本","config":{"code":"return true"}}
        ]}
        """;

    CanvasPublishRiskEvaluator evaluator = new CanvasPublishRiskEvaluator();

    CanvasPublishRisk risk = evaluator.evaluate(graphJson, null, null);

    assertThat(risk.requiresReview()).isTrue();
    assertThat(risk.reasons()).contains("包含权益发放节点", "包含 Groovy 脚本节点", "未配置全局总量上限");
}
```

- [ ] **Step 2: Implement evaluator**

Rules:

- `COUPON`, `POINTS_OPERATION`, `REACH_PLATFORM`, `SEND_SMS`, `SEND_PUSH`, `SEND_EMAIL`, `SEND_WECHAT`, `GROOVY` trigger review.
- `maxTotalExecutions == null` triggers review.
- `canaryPercent > 50` triggers review.

`CanvasPublishRisk` is:

```java
public record CanvasPublishRisk(boolean requiresReview, List<String> reasons) {}
```

- [ ] **Step 3: Add publish precheck endpoint**

Add `GET /canvas/{id}/publish-risk` to `CanvasController`. It loads current draft detail and returns `CanvasPublishRisk`.

- [ ] **Step 4: Run tests and commit**

```bash
cd backend/canvas-engine
mvn test -Dtest=CanvasPublishRiskEvaluatorTest -q
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPublishRisk.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPublishRiskEvaluator.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishRiskEvaluatorTest.java
git commit -m "feat: evaluate publish risk"
```

## Task 2: Operations Center Backend

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/OpsCenterSummaryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops/OpsCenterService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsCenterController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsCenterControllerTest.java`

- [ ] **Step 1: Add summary DTO**

Fields:

- `pendingReviewCount`
- `failedExecutionCount`
- `dlqCount`
- `mqRejectedCount`
- `replayRunningCount`
- `warningNotificationCount`

- [ ] **Step 2: Implement service**

`OpsCenterService.summary(Long tenantId)` counts:

- pending manual approval records.
- failed executions in the last 24 hours.
- DLQ rows.
- MQ rejected rows.
- execution requests with status `PENDING` and `replay_by` not null.
- unread warning/error notifications.

- [ ] **Step 3: Add controller**

Route: `GET /canvas/ops-center/summary`.

Use current tenant unless `SUPER_ADMIN` passes `tenantId`.

- [ ] **Step 4: Run test and commit**

```bash
cd backend/canvas-engine
mvn test -Dtest=OpsCenterControllerTest -q
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/OpsCenterSummaryDTO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops/OpsCenterService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsCenterController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsCenterControllerTest.java
git commit -m "feat: add operations center summary"
```

## Task 3: Template Center Frontend

**Files:**
- Create: `frontend/src/services/opsApi.ts`
- Create: `frontend/src/pages/templates/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Add API client**

`opsApi` exports:

```ts
export const opsApi = {
  listTemplates: (category?: string) =>
    http.get<R<CanvasTemplate[]>, R<CanvasTemplate[]>>('/canvas/templates', { params: { category } }),
  createFromTemplate: (templateId: number, name?: string) =>
    http.post<R<Canvas>, R<Canvas>>(`/canvas/from-template/${templateId}`, { name }),
  saveAsTemplate: (canvasId: number, body: { name: string; description?: string; category?: string }) =>
    http.post<R<CanvasTemplate>, R<CanvasTemplate>>(`/canvas/${canvasId}/save-as-template`, body),
}
```

- [ ] **Step 2: Add templates page**

Page behavior:

- Shows template cards in a table/grid.
- New-from-template action opens modal for new journey name.
- On success navigates to `/canvas/{id}/edit`.
- Official templates show readonly tag.

- [ ] **Step 3: Add route and menu**

Route: `/templates`.

Menu: under `自动化营销`, item `模板中心`.

- [ ] **Step 4: Build**

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/services/opsApi.ts frontend/src/pages/templates frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add template center"
```

## Task 4: Operations Center Frontend

**Files:**
- Modify: `frontend/src/services/opsApi.ts`
- Create: `frontend/src/pages/ops-center/opsPresentation.ts`
- Create: `frontend/src/pages/ops-center/opsPresentation.test.ts`
- Create: `frontend/src/pages/ops-center/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Write presentation test**

```ts
import { describe, expect, it } from 'vitest'
import { buildOpsAttentionItems } from './opsPresentation'

describe('ops presentation', () => {
  it('keeps zero values out of attention list', () => {
    const items = buildOpsAttentionItems({
      pendingReviewCount: 1,
      failedExecutionCount: 0,
      dlqCount: 2,
      mqRejectedCount: 0,
      replayRunningCount: 0,
      warningNotificationCount: 3,
    })

    expect(items.map(item => item.key)).toEqual(['pendingReview', 'dlq', 'warningNotification'])
  })
})
```

- [ ] **Step 2: Add API methods**

Add:

```ts
summary: () => http.get<R<OpsCenterSummary>, R<OpsCenterSummary>>('/canvas/ops-center/summary'),
listDlq: (params) => http.get<R<PageResult<DlqRow>>, R<PageResult<DlqRow>>>('/canvas/dlq', { params }),
replayDlq: (id: number) => http.post<R<Record<string, unknown>>, R<Record<string, unknown>>(`/canvas/dlq/${id}/replay`),
listRejected: (params) => http.get<R<PageResult<MqRejectedRow>>, R<PageResult<MqRejectedRow>>>('/canvas/mq-trigger-rejected', { params }),
replayRejected: (id: number) => http.post<R<Record<string, unknown>>, R<Record<string, unknown>>(`/canvas/mq-trigger-rejected/${id}/replay`),
```

- [ ] **Step 3: Add operations center page**

Tabs:

- `总览`: summary cards and attention list.
- `DLQ`: table, replay button.
- `MQ 拒收`: table, replay button.
- `执行请求`: link to `/platform-stability`.
- `审批`: list from `/canvas/pending-reviews`.

- [ ] **Step 4: Add route and menu**

Route: `/ops-center`.

Menu group: `运营工作台`, item `运营中心`.

- [ ] **Step 5: Run tests and build**

```bash
cd frontend
npm test -- --run src/pages/ops-center/opsPresentation.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/services/opsApi.ts frontend/src/pages/ops-center frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add operations center"
```

## Task 5: Publish Flow UI

**Files:**
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Add API method**

Add:

```ts
publishRisk: (id: number) =>
  http.get<R<{ requiresReview: boolean; reasons: string[] }>, R<{ requiresReview: boolean; reasons: string[] }>>(`/canvas/${id}/publish-risk`),
```

- [ ] **Step 2: Update publish handler**

Before `canvasApi.publish(canvasId)`, call `publishRisk`. If `requiresReview` is true and current user is `OPERATOR`, show modal:

Title: `需要提交审批`

Content: risk reasons.

Action: navigate to `/ops-center?tab=reviews`.

If current user is admin, show confirmation with risk reasons and allow direct publish.

- [ ] **Step 3: Build and commit**

```bash
cd frontend
npm run build
git add frontend/src/services/api.ts frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: surface publish risk in editor"
```

## Final Verification

- [ ] **Step 1: Backend focused tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=CanvasPublishRiskEvaluatorTest,OpsCenterControllerTest -q
```

- [ ] **Step 2: Frontend focused tests**

```bash
cd frontend
npm test -- --run src/pages/ops-center/opsPresentation.test.ts
npm run build
```

- [ ] **Step 3: Status check**

```bash
git status --short
```

Expected: clean except ignored/generated artifacts.
