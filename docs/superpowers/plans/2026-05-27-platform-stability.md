# Platform Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a platform stability console for execution requests, backlog/overflow/DLQ visibility, replay protection, tenant-scoped governance options, and capacity risk signals.

**Architecture:** Execute this after `feat/saas-foundation` is available and create `feat/platform-stability` from that branch. Reuse existing execution request, DLQ, MQ rejected, system option, and home overview capabilities. Add small summary DTOs and UI pages instead of changing the execution engine topology.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, React 18, TypeScript, Ant Design 5, Recharts, Vitest, Maven.

---

## Worktree Setup

- [ ] **Step 1: Create worktree from SaaS foundation**

```bash
git fetch --all
git worktree add .worktrees/feat-platform-stability -b feat/platform-stability feat/saas-foundation
cd .worktrees/feat-platform-stability
```

Expected: branch `feat/platform-stability` starts with tenant context and role model from `feat/saas-foundation`.

## File Structure

### Backend

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/PlatformRiskSummaryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/BacklogSummaryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops/PlatformStabilityService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformStabilityController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasReplayLimitPolicy.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ops/PlatformStabilityServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestReplayProtectionTest.java`

### Frontend

- Create: `frontend/src/services/platformStabilityApi.ts`
- Create: `frontend/src/pages/platform-stability/platformStabilityPresentation.ts`
- Create: `frontend/src/pages/platform-stability/platformStabilityPresentation.test.ts`
- Create: `frontend/src/pages/platform-stability/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

## Task 1: Backend Risk Summary Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/PlatformRiskSummaryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops/BacklogSummaryDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops/PlatformStabilityService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformStabilityController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ops/PlatformStabilityServiceTest.java`

- [ ] **Step 1: Write risk level tests**

```java
@Test
void riskLevelBecomesBlockedWhenDlqGrows() {
    PlatformStabilityService service = new PlatformStabilityService(null, null, null, null);

    assertThat(service.classifyRisk(0, 0.01, 0, 0)).isEqualTo("NORMAL");
    assertThat(service.classifyRisk(80, 0.03, 10, 0)).isEqualTo("WATCH");
    assertThat(service.classifyRisk(200, 0.08, 40, 3)).isEqualTo("RISK");
    assertThat(service.classifyRisk(500, 0.20, 100, 20)).isEqualTo("BLOCKED");
}
```

- [ ] **Step 2: Implement DTOs**

`PlatformRiskSummaryDTO` fields:

- `tenantId`
- `runningCount`
- `failedRate`
- `retryCount`
- `dlqGrowth`
- `mqRejectedCount`
- `riskLevel`
- `riskReason`

`BacklogSummaryDTO` fields:

- `status`
- `count`
- `oldestUpdatedAt`

- [ ] **Step 3: Implement service**

Public methods:

```java
public PlatformRiskSummaryDTO summary(Long tenantId)
public List<BacklogSummaryDTO> backlog(Long tenantId)
public String classifyRisk(long backlog, double failedRate, long retryCount, long dlqGrowth)
```

Use explicit mapper queries with tenant filters. Risk thresholds:

- `BLOCKED`: backlog >= 500 or failedRate >= 0.15 or dlqGrowth >= 10.
- `RISK`: backlog >= 200 or failedRate >= 0.08 or retryCount >= 40.
- `WATCH`: backlog >= 50 or failedRate >= 0.03 or retryCount >= 10.
- `NORMAL`: otherwise.

- [ ] **Step 4: Add controller**

Routes:

- `GET /canvas/platform-stability/summary`
- `GET /canvas/platform-stability/backlog`

Resolve tenant from `TenantContextResolver`. `SUPER_ADMIN` may pass `tenantId`; tenant users always use their own tenant.

- [ ] **Step 5: Run tests and commit**

```bash
cd backend/canvas-engine
mvn test -Dtest=PlatformStabilityServiceTest -q
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/ops \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformStabilityController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ops/PlatformStabilityServiceTest.java
git commit -m "feat: add platform stability summary"
```

## Task 2: Replay Protection and Tenant Filters

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasReplayLimitPolicy.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestReplayProtectionTest.java`

- [ ] **Step 1: Write replay limit test**

```java
@Test
void batchReplayRejectsLimitAboveTenantCap() {
    CanvasReplayLimitPolicy policy = new CanvasReplayLimitPolicy(100, 500);

    assertThatThrownBy(() -> policy.normalizeBatchLimit(100, 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("批量重放数量超过租户上限");
}
```

- [ ] **Step 2: Enforce batch limit**

Create `CanvasReplayLimitPolicy`:

```java
package org.chovy.canvas.engine.request;

public class CanvasReplayLimitPolicy {
    private final int defaultBatchLimit;
    private final int hardMaxBatchLimit;

    public CanvasReplayLimitPolicy(int defaultBatchLimit, int hardMaxBatchLimit) {
        this.defaultBatchLimit = defaultBatchLimit;
        this.hardMaxBatchLimit = hardMaxBatchLimit;
    }

    public int normalizeBatchLimit(int limit, int tenantCap) {
        int requested = limit <= 0 ? defaultBatchLimit : limit;
        int capped = Math.min(requested, hardMaxBatchLimit);
        if (capped > tenantCap) {
            throw new IllegalArgumentException("批量重放数量超过租户上限: " + tenantCap);
        }
        return capped;
    }
}
```

Use governance option `execution_governance/max_batch_replay`; default to `100`.

- [ ] **Step 3: Add tenant filters**

List and replay queries must include `tenant_id = currentTenantId` unless current role is `SUPER_ADMIN` and an explicit `tenantId` query parameter is present.

- [ ] **Step 4: Run tests and commit**

```bash
cd backend/canvas-engine
mvn test -Dtest=CanvasExecutionRequestReplayProtectionTest -q
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasReplayLimitPolicy.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestReplayProtectionTest.java
git commit -m "feat: protect replay traffic by tenant limits"
```

## Task 3: Governance Options

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V78__platform_governance_options.sql`
- Modify: `frontend/src/services/systemOptions.ts`

- [ ] **Step 1: Add governance seed options**

Create `V78__platform_governance_options.sql`:

```sql
INSERT INTO system_option
  (tenant_id, category, option_key, label, description, sort_order, enabled, system_builtin)
VALUES
  (NULL, 'execution_governance', 'max_batch_replay', '单次批量重放上限', '限制一次批量重放最多处理多少条执行请求', 10, 1, 1),
  (NULL, 'execution_governance', 'replay_qps', '重放 QPS 上限', '限制手工重放流量，避免冲击正常执行', 20, 1, 1),
  (NULL, 'execution_governance', 'canvas_concurrency', '画布并发上限', '单画布同时执行实例上限', 30, 1, 1),
  (NULL, 'execution_governance', 'node_timeout_ms', '节点超时毫秒数', '节点默认执行超时时间', 40, 1, 1)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);
```

- [ ] **Step 2: Add frontend category**

In `frontend/src/services/systemOptions.ts`, add:

```ts
{ value: 'execution_governance', label: '执行治理' }
```

- [ ] **Step 3: Verify migration**

```bash
cd backend/canvas-engine
mvn test -Dtest=FlywayConfigTest -q
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V78__platform_governance_options.sql frontend/src/services/systemOptions.ts
git commit -m "feat: seed execution governance options"
```

## Task 4: Frontend Stability Console

**Files:**
- Create: `frontend/src/services/platformStabilityApi.ts`
- Create: `frontend/src/pages/platform-stability/platformStabilityPresentation.ts`
- Create: `frontend/src/pages/platform-stability/platformStabilityPresentation.test.ts`
- Create: `frontend/src/pages/platform-stability/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Write presentation tests**

```ts
import { describe, expect, it } from 'vitest'
import { getRiskColor, getRiskLabel } from './platformStabilityPresentation'

describe('platform stability presentation', () => {
  it('maps risk levels to labels and colors', () => {
    expect(getRiskLabel('NORMAL')).toBe('正常')
    expect(getRiskColor('WATCH')).toBe('gold')
    expect(getRiskColor('RISK')).toBe('orange')
    expect(getRiskColor('BLOCKED')).toBe('red')
  })
})
```

- [ ] **Step 2: Add API client**

`platformStabilityApi.ts` exports:

```ts
export const platformStabilityApi = {
  summary: () => http.get<R<PlatformRiskSummary>, R<PlatformRiskSummary>>('/canvas/platform-stability/summary'),
  backlog: () => http.get<R<BacklogSummary[]>, R<BacklogSummary[]>>('/canvas/platform-stability/backlog'),
  executionRequests: (params: Record<string, unknown>) =>
    http.get<R<PageResult<ExecutionRequestRow>>, R<PageResult<ExecutionRequestRow>>>('/canvas/execution-requests', { params }),
  replayRequest: (id: string, reason: string) =>
    http.post<R<Record<string, unknown>>, R<Record<string, unknown>>>(`/canvas/execution-requests/${id}/replay`, null, { params: { reason } }),
}
```

- [ ] **Step 3: Add page**

`index.tsx` shows:

- Summary cards: risk level, running count, retry count, DLQ growth.
- Backlog table.
- Execution request table with filters and single replay button.
- Link to system options filtered by `execution_governance`.

- [ ] **Step 4: Add route and menu**

Route: `/platform-stability`.

Menu group: `平台治理` with item `稳定性控制台`.

- [ ] **Step 5: Run tests and build**

```bash
cd frontend
npm test -- --run src/pages/platform-stability/platformStabilityPresentation.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/services/platformStabilityApi.ts frontend/src/pages/platform-stability \
  frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add platform stability console"
```

## Final Verification

- [ ] **Step 1: Backend focused tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=PlatformStabilityServiceTest,CanvasExecutionRequestReplayProtectionTest,FlywayConfigTest -q
```

- [ ] **Step 2: Frontend focused tests**

```bash
cd frontend
npm test -- --run src/pages/platform-stability/platformStabilityPresentation.test.ts
npm run build
```

- [ ] **Step 3: Status check**

```bash
git status --short
```

Expected: clean except ignored/generated artifacts.
