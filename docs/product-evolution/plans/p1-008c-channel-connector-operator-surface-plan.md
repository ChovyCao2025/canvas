# Channel Connector Operator Surface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose connector mode, health, limits, fallback decisions, and dedupe records to operators.

**Architecture:** Add a thin `ChannelConnectorController` over the P1-008/P1-008B services, a typed frontend API wrapper, testable presentation helpers, and config-panel warnings when selected channel/provider is not REAL.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, React 18, TypeScript, Ant Design, Vitest.

**Implementation status (2026-06-05):** Completed. Existing controller/page/API/helper files were retained and verified; this pass added the missing app route, navigation entry, route announcement, and updated stale presentation test expectations. Broader Maven focused backend testing is still blocked by unrelated `RedisBiQueryResultCacheTest` testCompile failures, so final backend coverage used an isolated `/tmp` runner with 44/44 passing tests. Commit was intentionally skipped because the user did not request one.

Status: Completed on 2026-06-05; broader Maven focused backend testing is still blocked by unrelated testCompile failures.

---

## Spec Reference

- `docs/product-evolution/specs/p1-008c-channel-connector-operator-surface.md`
- Depends on P1-008 connector contract and P1-008B provider policy services.

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelConnectorController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ChannelConnectorControllerTest.java`
- Create: `frontend/src/services/channelConnectorApi.ts`
- Create: `frontend/src/services/channelConnectorApi.test.ts`
- Create: `frontend/src/pages/channel-connectors/channelConnectorPresentation.ts`
- Create: `frontend/src/pages/channel-connectors/channelConnectorPresentation.test.ts`
- Create: `frontend/src/pages/channel-connectors/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`
- Modify: `frontend/src/components/config-panel/presentation.ts`
- Modify: `frontend/src/components/config-panel/presentation.test.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`

### Task 1: Backend Operator API

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ChannelConnectorControllerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelConnectorController.java`

- [x] **Step 1: Write controller tests**

Create focused WebFlux/controller tests:

```java
@Test
void listsConnectorsWithModeAndHealth() {
    ChannelConnectorController.Service service = mock(ChannelConnectorController.Service.class);
    when(service.list(0L)).thenReturn(List.of(new ChannelConnectorController.ConnectorRow(
            1L, "sms-aliyun", "SMS", "ALIYUN", "REAL", "UP", "ok")));

    ChannelConnectorController controller = new ChannelConnectorController(service);

    assertThat(controller.list().get(0).mode()).isEqualTo("REAL");
    assertThat(controller.list().get(0).healthStatus()).isEqualTo("UP");
}

@Test
void validatesFallbackPolicyBeforeSave() {
    ChannelConnectorController.Service service = mock(ChannelConnectorController.Service.class);
    when(service.validateFallback(0L, new ChannelConnectorController.FallbackPolicyReq("PUSH", "JPUSH", "SMS", "ALIYUN")))
            .thenReturn(new ChannelConnectorController.ValidationResult(false, "cycle: PUSH:JPUSH -> SMS:ALIYUN -> PUSH:JPUSH"));

    ChannelConnectorController.ValidationResult result = new ChannelConnectorController(service)
            .validateFallback(new ChannelConnectorController.FallbackPolicyReq("PUSH", "JPUSH", "SMS", "ALIYUN"));

    assertThat(result.valid()).isFalse();
    assertThat(result.message()).contains("cycle");
}
```

- [x] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorControllerTest
```

Expected: FAIL because controller does not exist.

- [x] **Step 3: Implement controller contract**

Create `ChannelConnectorController`:

```java
@RestController
@RequestMapping("/channels/connectors")
@RequiredArgsConstructor
public class ChannelConnectorController {
    private final Service service;

    @GetMapping
    public List<ConnectorRow> list() {
        return service.list(0L);
    }

    @PostMapping("/{id}/mode")
    public void updateMode(@PathVariable Long id, @RequestBody ModeUpdateReq req) {
        service.updateMode(0L, id, req.mode(), req.reason());
    }

    @PostMapping("/{id}/health-test")
    public HealthResult testHealth(@PathVariable Long id) {
        return service.testHealth(0L, id);
    }

    @PostMapping("/fallback/validate")
    public ValidationResult validateFallback(@RequestBody FallbackPolicyReq req) {
        return service.validateFallback(0L, req);
    }

    @GetMapping("/fallback/decisions")
    public List<FallbackDecisionRow> decisions() {
        return service.listDecisions(0L);
    }

    @GetMapping("/dedupe-records")
    public List<DedupeRecordRow> dedupeRecords() {
        return service.listDedupeRecords(0L);
    }

    public interface Service {
        List<ConnectorRow> list(Long tenantId);
        void updateMode(Long tenantId, Long id, String mode, String reason);
        HealthResult testHealth(Long tenantId, Long id);
        ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req);
        List<FallbackDecisionRow> listDecisions(Long tenantId);
        List<DedupeRecordRow> listDedupeRecords(Long tenantId);
    }

    public record ConnectorRow(Long id, String connectorKey, String channel, String provider, String mode, String healthStatus, String healthMessage) {}
    public record ModeUpdateReq(String mode, String reason) {}
    public record HealthResult(String status, String message) {}
    public record FallbackPolicyReq(String channel, String provider, String fallbackChannel, String fallbackProvider) {}
    public record ValidationResult(boolean valid, String message) {}
    public record FallbackDecisionRow(String originalChannel, String finalChannel, String decisionReason, String createdAt) {}
    public record DedupeRecordRow(String dedupeGroup, String contentHash, String channel, String userId, String expiresAt) {}
}
```

- [x] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorControllerTest
```

Expected: PASS.

### Task 2: Frontend API And Presentation Helpers

**Files:**
- Create: `frontend/src/services/channelConnectorApi.ts`
- Create: `frontend/src/services/channelConnectorApi.test.ts`
- Create: `frontend/src/pages/channel-connectors/channelConnectorPresentation.ts`
- Create: `frontend/src/pages/channel-connectors/channelConnectorPresentation.test.ts`

- [x] **Step 1: Write frontend API tests**

Create `frontend/src/services/channelConnectorApi.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest'
import { createChannelConnectorApi } from './channelConnectorApi'

describe('channelConnectorApi', () => {
  it('calls connector list and fallback validation endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: { valid: true } }),
    }
    const api = createChannelConnectorApi(http as any)

    await api.list()
    await api.validateFallback({ channel: 'PUSH', provider: 'JPUSH', fallbackChannel: 'SMS', fallbackProvider: 'ALIYUN' })

    expect(http.get).toHaveBeenCalledWith('/channels/connectors')
    expect(http.post).toHaveBeenCalledWith('/channels/connectors/fallback/validate', {
      channel: 'PUSH',
      provider: 'JPUSH',
      fallbackChannel: 'SMS',
      fallbackProvider: 'ALIYUN',
    })
  })
})
```

- [x] **Step 2: Write presentation tests**

Create `channelConnectorPresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { connectorModeBadge, connectorWarning, formatDecisionRow } from './channelConnectorPresentation'

describe('channelConnectorPresentation', () => {
  it('formats connector mode badges and warnings', () => {
    expect(connectorModeBadge('REAL')).toEqual({ text: 'Real', color: 'green' })
    expect(connectorModeBadge('SANDBOX')).toEqual({ text: 'Sandbox', color: 'gold' })
    expect(connectorWarning({ channel: 'SMS', provider: 'ALIYUN', mode: 'DISABLED' }))
      .toBe('SMS/ALIYUN is disabled')
  })

  it('formats fallback decision rows', () => {
    expect(formatDecisionRow({ originalChannel: 'PUSH', finalChannel: 'SMS', decisionReason: 'PRIMARY_THROTTLED', createdAt: '2026-06-03T00:00:00Z' }))
      .toBe('PUSH -> SMS: PRIMARY_THROTTLED at 2026-06-03T00:00:00Z')
  })
})
```

- [x] **Step 3: Implement API wrapper and helpers**

Create `frontend/src/services/channelConnectorApi.ts`:

```ts
import http from './api'

export interface FallbackPolicyPayload {
  channel: string
  provider: string
  fallbackChannel: string
  fallbackProvider: string
}

export function createChannelConnectorApi(client = http) {
  return {
    list: () => client.get('/channels/connectors'),
    updateMode: (id: number, mode: string, reason: string) => client.post(`/channels/connectors/${id}/mode`, { mode, reason }),
    testHealth: (id: number) => client.post(`/channels/connectors/${id}/health-test`),
    validateFallback: (payload: FallbackPolicyPayload) => client.post('/channels/connectors/fallback/validate', payload),
    decisions: () => client.get('/channels/connectors/fallback/decisions'),
    dedupeRecords: () => client.get('/channels/connectors/dedupe-records'),
  }
}

export const channelConnectorApi = createChannelConnectorApi()
```

Create `channelConnectorPresentation.ts`:

```ts
export function connectorModeBadge(mode: string) {
  if (mode === 'REAL') return { text: 'Real', color: 'green' }
  if (mode === 'SANDBOX') return { text: 'Sandbox', color: 'gold' }
  return { text: 'Disabled', color: 'red' }
}

export function connectorWarning(row: { channel: string; provider: string; mode: string }) {
  return row.mode === 'REAL' ? null : `${row.channel}/${row.provider} is ${row.mode.toLowerCase()}`
}

export function formatDecisionRow(row: { originalChannel: string; finalChannel?: string | null; decisionReason: string; createdAt: string }) {
  return `${row.originalChannel} -> ${row.finalChannel ?? 'SKIP'}: ${row.decisionReason} at ${row.createdAt}`
}
```

- [x] **Step 4: Run frontend helper tests**

Run:

```bash
cd frontend && npm test -- channelConnectorApi.test.ts channelConnectorPresentation.test.ts
```

Expected: PASS.

### Task 3: Page And Config Panel Warnings

**Files:**
- Create: `frontend/src/pages/channel-connectors/index.tsx`
- Modify: `frontend/src/components/config-panel/presentation.ts`
- Modify: `frontend/src/components/config-panel/presentation.test.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`

- [x] **Step 1: Add config-panel warning tests**

Append to `presentation.test.ts`:

```ts
it('returns connector warnings for non-real send message channels', () => {
  expect(resolveConnectorWarning({
    nodeType: 'SEND_MESSAGE',
    bizConfig: { channel: 'SMS', provider: 'ALIYUN' },
    connectorMode: 'DISABLED',
  })).toBe('SMS/ALIYUN is disabled')
})
```

- [x] **Step 2: Add warning helper**

In `presentation.ts`:

```ts
export function resolveConnectorWarning(input: {
  nodeType: string
  bizConfig: Record<string, unknown>
  connectorMode?: string | null
}): string | null {
  if (input.nodeType !== 'SEND_MESSAGE' || !input.connectorMode || input.connectorMode === 'REAL') return null
  const channel = String(input.bizConfig.channel ?? 'UNKNOWN')
  const provider = String(input.bizConfig.provider ?? 'default')
  return `${channel}/${provider} is ${input.connectorMode.toLowerCase()}`
}
```

- [x] **Step 3: Build connector page**

Create a page with connector table, mode badge, health badge, mode update action, fallback validation form, latest decisions table, and dedupe records table. Keep data formatting in `channelConnectorPresentation.ts`.

- [x] **Step 4: Run page and config-panel tests**

Run:

```bash
cd frontend && npm test -- channelConnectorPresentation.test.ts presentation.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p1-008c-channel-connector-operator-surface.md`
- Modify: `docs/product-evolution/plans/p1-008c-channel-connector-operator-surface-plan.md`

Actual verification used in this session:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests clean compile
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -Dtest=ChannelConnectorControllerTest test
# Broader Maven focused suites were blocked by unrelated RedisBiQueryResultCacheTest testCompile failures.
# Final backend focused coverage used /tmp/canvas-p1-008-test.classpath + /tmp/canvas-p1-008-test-classes and P1008FocusedRunner: 44/44 pass.
cd ../frontend
PATH="/opt/homebrew/bin:$PATH" npm run test -- channelConnectorApi.test.ts channelConnectorPresentation.test.ts presentation.test.ts AppLayout.a11y.test.tsx
PATH="/opt/homebrew/bin:$PATH" npm run build
```

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorControllerTest
cd frontend && npm test -- channelConnectorApi.test.ts channelConnectorPresentation.test.ts presentation.test.ts
```

Expected: PASS.

- [x] **Step 2: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [x] **Step 3: Commit skipped**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelConnectorController.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/ChannelConnectorControllerTest.java frontend/src/services/channelConnectorApi.ts frontend/src/services/channelConnectorApi.test.ts frontend/src/pages/channel-connectors frontend/src/components/config-panel docs/product-evolution/specs/p1-008c-channel-connector-operator-surface.md docs/product-evolution/plans/p1-008c-channel-connector-operator-surface-plan.md
# Skipped in this session because the user did not request a commit.
```

Expected: commit contains only connector operator API, frontend API/page/helpers, config-panel warnings, tests, and related docs.
