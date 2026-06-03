# Operator Visibility And Testability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give operators bounded UI-backed visibility into execution requests, DLQ, send records, policy records, dry-run traces, and common table operations.

**Architecture:** Reuse existing execution-request, DLQ, dry-run, and trace endpoints where possible. Add two thin backend admin controllers for send records and marketing policy records, add one additive index migration, then wire typed frontend services and pure presentation helpers before page integration.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway SQL, Reactor `Mono`, React 18, Ant Design, Axios, Vitest, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-002-operator-visibility-and-testability.md`
- Source item: `docs/product-evolution/todo/p1/operator-visibility-and-testability.md`

## File Structure

**Backend**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageSendRecordController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPolicyAdminController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingConsentDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingSuppressionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerChannelDO.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V95__operator_visibility_and_testability.sql`

**Frontend**
- Create: `frontend/src/services/operatorApi.ts`
- Modify: `frontend/src/components/canvas/ExecutionTracePanel.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `frontend/src/pages/canvas-editor/dryRunVisualization.ts`
- Modify: `frontend/src/pages/canvas-stats/index.tsx`
- Create: `frontend/src/pages/canvas-stats/operatorTables.ts`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestManagementControllerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageSendRecordControllerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingPolicyAdminControllerTest.java`
- Create: `frontend/src/pages/canvas-editor/dryRunVisualization.test.ts`
- Create: `frontend/src/pages/canvas-stats/operatorTables.test.ts`

### Task 1: Execution Request And DLQ Operator Contracts

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestManagementControllerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`

- [ ] **Step 1: Write execution request controller tests**

Create `CanvasExecutionRequestManagementControllerTest`.

```java
class CanvasExecutionRequestManagementControllerTest {
    @Test
    void listClampsPageSizeToOneHundred() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptor = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptor);
        Page<CanvasExecutionRequestDO> page = new Page<>(1, 100);
        page.setTotal(0);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        controller.list(null, null, null, null, 1, 500).block();

        ArgumentCaptor<Page<CanvasExecutionRequestDO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(mapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    void batchReplayCapsLimitAtFiveHundred() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptor = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptor);
        when(mapper.selectList(any())).thenReturn(List.of());
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("operator");

        controller.replayBatch(null, CanvasExecutionRequestStatus.FAILED, null, null, 900, "retry", false)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new UsernamePasswordAuthenticationToken(claims, null, List.of())))
                .block();

        ArgumentCaptor<LambdaQueryWrapper<CanvasExecutionRequestDO>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getLastSql()).contains("LIMIT 500");
    }
}
```

- [ ] **Step 2: Run execution request tests and confirm red state**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=CanvasExecutionRequestManagementControllerTest`

Expected: FAIL because `list` currently uses the requested `size` directly and the test file is new.

- [ ] **Step 3: Clamp list pagination**

In `CanvasExecutionRequestManagementController`, add a list page-size helper and use it in `list`.

```java
private int normalizePageSize(int size) {
    if (size <= 0) {
        return 20;
    }
    return Math.min(size, 100);
}
```

```java
Page<CanvasExecutionRequestDO> result = mapper.selectPage(
        new Page<>(Math.max(1, page), normalizePageSize(size)), wrapper);
```

- [ ] **Step 4: Clamp DLQ pagination**

In `DlqController.list`, apply the same page-size rule before constructing `Page`.

```java
int safePage = Math.max(1, page);
int safeSize = Math.max(1, Math.min(size, 100));
Page<CanvasExecutionDlqDO> p = new Page<>(safePage, safeSize);
```

- [ ] **Step 5: Run execution request tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=CanvasExecutionRequestManagementControllerTest`

Expected: PASS; list page size is capped at 100 and batch replay limit is capped at 500.

### Task 2: Message Send Record Search And Detail

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageSendRecordControllerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageSendRecordController.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V95__operator_visibility_and_testability.sql`

- [ ] **Step 1: Write send-record controller tests**

Create `MessageSendRecordControllerTest`.

```java
class MessageSendRecordControllerTest {
    @Test
    void listFiltersByCanvasUserChannelAndStatus() {
        MessageSendRecordMapper mapper = mock(MessageSendRecordMapper.class);
        MessageSendRecordController controller = new MessageSendRecordController(mapper);
        Page<MessageSendRecordDO> page = new Page<>(1, 20);
        page.setTotal(0);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        controller.list(10L, null, "user-1", "sms", "sent", null, null, 1, 20).block();

        ArgumentCaptor<LambdaQueryWrapper<MessageSendRecordDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getExpression().getNormal().toString();
        assertThat(sql).contains("canvas_id").contains("user_id").contains("channel").contains("status");
    }

    @Test
    void detailReturnsNotFoundMessageWhenRecordIsMissing() {
        MessageSendRecordMapper mapper = mock(MessageSendRecordMapper.class);
        MessageSendRecordController controller = new MessageSendRecordController(mapper);
        when(mapper.selectById(99L)).thenReturn(null);

        R<MessageSendRecordDO> response = controller.detail(99L).block();

        assertThat(response.getCode()).isEqualTo(-1);
        assertThat(response.getMessage()).isEqualTo("发送记录不存在: 99");
    }
}
```

- [ ] **Step 2: Run send-record tests and confirm red state**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=MessageSendRecordControllerTest`

Expected: FAIL because `MessageSendRecordController` does not exist.

- [ ] **Step 3: Add send-record indexes**

Create `V95__operator_visibility_and_testability.sql`.

```sql
ALTER TABLE message_send_record
  ADD INDEX idx_message_send_canvas_status_created (canvas_id, status, created_at),
  ADD INDEX idx_message_send_canvas_user_created (canvas_id, user_id, created_at),
  ADD INDEX idx_message_send_execution_created (execution_id, created_at);
```

- [ ] **Step 4: Implement `MessageSendRecordController`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageSendRecordController.java`.

```java
@RestController
@RequestMapping("/canvas/message-send-records")
@RequiredArgsConstructor
public class MessageSendRecordController {
    private final MessageSendRecordMapper mapper;

    @GetMapping
    public Mono<R<PageResult<MessageSendRecordDO>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime since,
            @RequestParam(required = false) LocalDateTime until,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<MessageSendRecordDO> q = new LambdaQueryWrapper<MessageSendRecordDO>()
                    .eq(canvasId != null, MessageSendRecordDO::getCanvasId, canvasId)
                    .eq(notBlank(executionId), MessageSendRecordDO::getExecutionId, executionId)
                    .eq(notBlank(userId), MessageSendRecordDO::getUserId, userId)
                    .eq(notBlank(channel), MessageSendRecordDO::getChannel, normalize(channel))
                    .eq(notBlank(status), MessageSendRecordDO::getStatus, normalize(status))
                    .ge(since != null, MessageSendRecordDO::getCreatedAt, since)
                    .le(until != null, MessageSendRecordDO::getCreatedAt, until)
                    .orderByDesc(MessageSendRecordDO::getCreatedAt);
            Page<MessageSendRecordDO> result = mapper.selectPage(
                    new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100))), q);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<MessageSendRecordDO>> detail(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            MessageSendRecordDO record = mapper.selectById(id);
            return record == null ? R.<MessageSendRecordDO>fail("发送记录不存在: " + id) : R.ok(record);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
```

- [ ] **Step 5: Run send-record tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=MessageSendRecordControllerTest,FlywayConfigTest`

Expected: PASS; the controller filters records and the migration version is valid.

### Task 3: Marketing Policy Admin Endpoints

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingPolicyAdminControllerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPolicyAdminController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingConsentDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingSuppressionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerChannelDO.java`

- [ ] **Step 1: Write policy admin tests**

Create `MarketingPolicyAdminControllerTest`.

```java
class MarketingPolicyAdminControllerTest {
    @Test
    void upsertConsentNormalizesChannelAndStatus() {
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        MarketingPolicyAdminController controller = controller(consentMapper);
        MarketingPolicyAdminController.ConsentReq req =
                new MarketingPolicyAdminController.ConsentReq("user-1", "sms", "opt_out", "manual");

        controller.upsertConsent(req).block();

        ArgumentCaptor<MarketingConsentDO> captor = ArgumentCaptor.forClass(MarketingConsentDO.class);
        verify(consentMapper).insert(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo("SMS");
        assertThat(captor.getValue().getConsentStatus()).isEqualTo("OPT_OUT");
    }

    @Test
    void listPolicyStateReadsConsentSuppressionAndChannel() {
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        CustomerChannelMapper channelMapper = mock(CustomerChannelMapper.class);
        MarketingPolicyAdminController controller =
                new MarketingPolicyAdminController(consentMapper, suppressionMapper, channelMapper);

        MarketingPolicyAdminController.PolicyState state = controller.policyState("user-1", "email").block().getData();

        assertThat(state.userId()).isEqualTo("user-1");
        assertThat(state.channel()).isEqualTo("EMAIL");
        verify(consentMapper).selectOne(any());
        verify(suppressionMapper).selectList(any());
        verify(channelMapper).selectOne(any());
    }
}
```

- [ ] **Step 2: Run policy tests and confirm red state**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=MarketingPolicyAdminControllerTest`

Expected: FAIL because `MarketingPolicyAdminController` does not exist.

- [ ] **Step 3: Add explicit update strategy for nullable policy fields**

In `MarketingSuppressionDO`, mark `expiresAt` and `channel` with `@TableField(updateStrategy = FieldStrategy.ALWAYS)` so admins can clear an expiry or switch to all-channel suppression. In `CustomerChannelDO`, mark `address` and `metadata` with the same update strategy so admins can clear stale values.

- [ ] **Step 4: Implement policy admin controller**

Create `MarketingPolicyAdminController` under `/canvas/policies`.

```java
@RestController
@RequestMapping("/canvas/policies")
@RequiredArgsConstructor
public class MarketingPolicyAdminController {
    private final MarketingConsentMapper consentMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final CustomerChannelMapper channelMapper;

    public record ConsentReq(String userId, String channel, String consentStatus, String source) {}
    public record SuppressionReq(String userId, String channel, String reason, Boolean active, LocalDateTime expiresAt) {}
    public record ChannelReq(String userId, String channel, String address, Integer enabled, Integer verified, String metadata) {}
    public record PolicyState(String userId, String channel, MarketingConsentDO consent,
                              List<MarketingSuppressionDO> suppressions, CustomerChannelDO customerChannel) {}

    @GetMapping("/state")
    public Mono<R<PolicyState>> policyState(@RequestParam String userId, @RequestParam String channel) { ... }

    @PostMapping("/consent")
    public Mono<R<MarketingConsentDO>> upsertConsent(@RequestBody ConsentReq req) { ... }

    @PostMapping("/suppression")
    public Mono<R<MarketingSuppressionDO>> upsertSuppression(@RequestBody SuppressionReq req) { ... }

    @PostMapping("/channel")
    public Mono<R<CustomerChannelDO>> upsertChannel(@RequestBody ChannelReq req) { ... }
}
```

For each upsert, query by `userId + normalized channel`, update when a record exists, and insert otherwise. Use `normalize(channel)` returning upper-case and defaulting blank suppression channel to `ALL`.

- [ ] **Step 5: Run policy tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=MarketingPolicyAdminControllerTest`

Expected: PASS; policy state and admin upserts normalize channels and statuses.

### Task 4: Frontend Operator API And Table Helpers

**Files:**
- Create: `frontend/src/services/operatorApi.ts`
- Create: `frontend/src/pages/canvas-stats/operatorTables.ts`
- Create: `frontend/src/pages/canvas-stats/operatorTables.test.ts`
- Modify: `frontend/src/pages/canvas-stats/index.tsx`

- [ ] **Step 1: Write table helper tests**

Create `operatorTables.test.ts`.

```ts
import { describe, expect, it } from 'vitest'
import { buildOperatorTableQuery, canExportSynchronously, OPERATION_COLUMN } from './operatorTables'

describe('operator table helpers', () => {
  it('drops empty filters and keeps concrete values', () => {
    expect(buildOperatorTableQuery({ canvasId: 10, status: 'FAILED', userId: '', page: 1, size: 20 }))
      .toEqual({ canvasId: 10, status: 'FAILED', page: 1, size: 20 })
  })

  it('blocks synchronous export above limit', () => {
    expect(canExportSynchronously(5000)).toBe(true)
    expect(canExportSynchronously(5001)).toBe(false)
  })

  it('defines a fixed operation column', () => {
    expect(OPERATION_COLUMN).toEqual({ key: 'operation', fixed: 'right', width: 160 })
  })
})
```

- [ ] **Step 2: Run table tests and confirm red state**

Run: `cd frontend && npm test -- operatorTables.test.ts`

Expected: FAIL because `operatorTables.ts` does not exist.

- [ ] **Step 3: Implement operator table helpers**

Create `operatorTables.ts`.

```ts
export const MAX_SYNC_EXPORT_ROWS = 5000
export const OPERATION_COLUMN = { key: 'operation', fixed: 'right' as const, width: 160 }

export function canExportSynchronously(totalRows: number) {
  return totalRows <= MAX_SYNC_EXPORT_ROWS
}

export function buildOperatorTableQuery(filters: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}
```

- [ ] **Step 4: Add typed operator API wrappers**

Create `frontend/src/services/operatorApi.ts`.

```ts
import type { PageResult, R } from '../types'
import http from './api'

export interface ExecutionRequestRow {
  id: string
  canvasId: number
  userId: string
  status: string
  attemptCount?: number
  lastError?: string
  updatedAt?: string
}

export interface MessageSendRecordRow {
  id: number
  executionId: string
  canvasId: number
  userId: string
  channel: string
  status: string
  externalMessageId?: string
  errorMessage?: string
  createdAt?: string
}

export const operatorApi = {
  executionRequests: (params: Record<string, unknown>) =>
    http.get<R<PageResult<ExecutionRequestRow>>, R<PageResult<ExecutionRequestRow>>>('/canvas/execution-requests', { params }),
  replayExecutionRequest: (id: string, reason?: string, force = false) =>
    http.post<R<Record<string, unknown>>, R<Record<string, unknown>>>(`/canvas/execution-requests/${id}/replay`, null, { params: { reason, force } }),
  messageSendRecords: (params: Record<string, unknown>) =>
    http.get<R<PageResult<MessageSendRecordRow>>, R<PageResult<MessageSendRecordRow>>>('/canvas/message-send-records', { params }),
  messageSendRecord: (id: number) =>
    http.get<R<MessageSendRecordRow>, R<MessageSendRecordRow>>(`/canvas/message-send-records/${id}`),
  policyState: (userId: string, channel: string) =>
    http.get<R<Record<string, unknown>>, R<Record<string, unknown>>>('/canvas/policies/state', { params: { userId, channel } }),
}
```

- [ ] **Step 5: Wire stats page operator tab**

In `canvas-stats/index.tsx`, add an "运营排查" section below existing stats. Use `operatorApi.executionRequests` and `operatorApi.messageSendRecords` with canvasId defaulted from route params. Use `OPERATION_COLUMN` for replay/detail buttons and block CSV export when `canExportSynchronously(total)` is false.

- [ ] **Step 6: Run table tests**

Run: `cd frontend && npm test -- operatorTables.test.ts`

Expected: PASS.

### Task 5: Dry-Run Visualization Helpers

**Files:**
- Create: `frontend/src/pages/canvas-editor/dryRunVisualization.test.ts`
- Create: `frontend/src/pages/canvas-editor/dryRunVisualization.ts`
- Modify: `frontend/src/components/canvas/ExecutionTracePanel.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Write dry-run visualization tests**

Create `dryRunVisualization.test.ts`.

```ts
import { describe, expect, it } from 'vitest'
import { buildDryRunSummary, buildTraceColorMap } from './dryRunVisualization'

describe('dryRunVisualization helpers', () => {
  it('maps trace status to node colors', () => {
    expect(buildTraceColorMap([
      { nodeId: 'n1', status: 1 },
      { nodeId: 'n2', status: 2 },
      { nodeId: 'n3', status: 3 },
    ])).toEqual({ n1: '#52c41a', n2: '#f5222d', n3: '#d9d9d9' })
  })

  it('summarizes traces by status', () => {
    expect(buildDryRunSummary([{ status: 1 }, { status: 1 }, { status: 2 }, { status: 0 }]))
      .toEqual({ running: 1, success: 2, failed: 1, skipped: 0 })
  })
})
```

- [ ] **Step 2: Run dry-run helper tests and confirm red state**

Run: `cd frontend && npm test -- dryRunVisualization.test.ts`

Expected: FAIL because `dryRunVisualization.ts` does not exist.

- [ ] **Step 3: Implement dry-run helpers**

Create `dryRunVisualization.ts`.

```ts
type TraceLike = { nodeId?: string; status: 0 | 1 | 2 | 3 }

export const TRACE_COLORS = {
  0: '#faad14',
  1: '#52c41a',
  2: '#f5222d',
  3: '#d9d9d9',
} as const

export function buildTraceColorMap(traces: TraceLike[]) {
  return Object.fromEntries(
    traces.filter(trace => trace.nodeId).map(trace => [trace.nodeId as string, TRACE_COLORS[trace.status]]),
  )
}

export function buildDryRunSummary(traces: Array<Pick<TraceLike, 'status'>>) {
  return traces.reduce((acc, trace) => {
    if (trace.status === 0) acc.running += 1
    if (trace.status === 1) acc.success += 1
    if (trace.status === 2) acc.failed += 1
    if (trace.status === 3) acc.skipped += 1
    return acc
  }, { running: 0, success: 0, failed: 0, skipped: 0 })
}
```

- [ ] **Step 4: Use helpers in `ExecutionTracePanel`**

Replace inline color-map construction with `buildTraceColorMap(data)` and export the status colors from one place. Keep the visual colors unchanged.

- [ ] **Step 5: Add dry-run summary copy after test run**

In `canvas-editor/index.tsx`, when dry-run returns an `executionId`, show a message that points to the trace panel and includes the execution id prefix. If the result contains an inline `traces` array, use `buildDryRunSummary` to show `成功/失败/跳过` counts.

- [ ] **Step 6: Run dry-run tests**

Run: `cd frontend && npm test -- dryRunVisualization.test.ts`

Expected: PASS.

### Task 6: Regression And Rollout

**Files:**
- Modify: `docs/product-evolution/specs/p1-002-operator-visibility-and-testability.md`
- Modify: `docs/product-evolution/plans/p1-002-operator-visibility-and-testability-plan.md`

- [ ] **Step 1: Run focused backend tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=CanvasExecutionRequestManagementControllerTest,MessageSendRecordControllerTest,MarketingPolicyAdminControllerTest`

Expected: PASS for the P1-002 backend contracts.

- [ ] **Step 2: Run focused frontend tests**

Run: `cd frontend && npm test -- dryRunVisualization.test.ts operatorTables.test.ts`

Expected: PASS for the P1-002 frontend helpers.

- [ ] **Step 3: Run module regressions**

Run: `cd backend && mvn -pl canvas-engine test && cd ../frontend && npm test && npm run build`

Expected: PASS for backend tests, frontend tests, and production build.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Use this checklist in the PR body.

```markdown
Rollout notes:
- Execution request batch replay defaults to FAILED/RETRY and caps limit at 500.
- Message send record and policy endpoints are admin/operator visibility surfaces; tenant filters from P0-001 must be active before production rollout.
- Synchronous CSV export is blocked above 5,000 rows.
- Dry-run visualization uses existing trace status colors: running yellow, success green, failed red, skipped gray.
```

- [ ] **Step 5: Commit the implementation slice**

Run: `git add backend/canvas-engine/src frontend/src docs/product-evolution/specs/p1-002-operator-visibility-and-testability.md docs/product-evolution/plans/p1-002-operator-visibility-and-testability-plan.md && git commit -m "feat: expose operator visibility tools"`

Expected: commit contains only P1-002 operator visibility changes.
