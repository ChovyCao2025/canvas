# Webhook Subscription API And Operator UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose webhook subscription management, secret rotation, test delivery, delivery logs, and operator UI.

**Architecture:** Add a controller that reuses P1-005B validation and P1-005B2 dispatcher paths. Frontend state stays in a dedicated webhook page with small presentation helpers and API methods in `cdpEventApi.ts`.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Jackson, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Ant Design, Vitest.

**Implementation Status:** Implemented and merged into `main` on 2026-06-05. Backend management endpoints live at `/cdp/webhooks`, and the operator UI is available at `/webhook-subscriptions`.

**Verification:** `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed. Focused webhook backend tests for P1-005B/P1-005B2/P1-005B3 pass in an isolated runner because Maven `testCompile` is still blocked by unrelated existing test-source errors. `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- cdpEventApi.test.ts webhookSubscriptions.test.ts` passed, and `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.

---

## Spec Reference

- `docs/product-evolution/specs/p1-005b3-webhook-subscription-api-and-operator-ui.md`
- Depends on: `docs/product-evolution/specs/p1-005b-webhook-subscription-schema-and-signing.md`
- Depends on: `docs/product-evolution/specs/p1-005b2-webhook-dispatch-retry-and-delivery-log.md`

## File Structure

- Existing: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/webhook/WebhookSubscriptionReq.java`
- Existing: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/webhook/WebhookSubscriptionDTO.java`
- Existing: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/webhook/WebhookDeliveryDTO.java`
- Existing: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/webhook/WebhookRotateSecretResp.java`
- Added: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java`
- Added: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/WebhookSubscriptionControllerTest.java`
- Modified: `frontend/src/services/cdpEventApi.ts`
- Added: `frontend/src/pages/webhook-subscriptions/index.tsx`
- Added: `frontend/src/pages/webhook-subscriptions/webhookSubscriptionPresentation.ts`
- Added: `frontend/src/pages/webhook-subscriptions/webhookSubscriptions.test.ts`
- Modified: `frontend/src/App.tsx`
- Modified: `frontend/src/components/layout/AppLayout.tsx`

### Task 1: Controller DTOs And Tests

**Files:**
- Create: DTOs under `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/webhook/`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/WebhookSubscriptionControllerTest.java`

- [ ] **Step 1: Add DTO records**

Create:

```java
public record WebhookSubscriptionReq(
        String name,
        String callbackUrl,
        List<String> eventTypes,
        Integer maxAttempts
) {
}

public record WebhookSubscriptionDTO(
        Long id,
        String name,
        String callbackUrl,
        String secretPrefix,
        List<String> eventTypes,
        String status,
        Integer maxAttempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

public record WebhookDeliveryDTO(
        Long id,
        String deliveryId,
        String eventType,
        Integer attempt,
        Integer httpStatus,
        String status,
        LocalDateTime nextRetryAt,
        String errorMessage,
        String terminalReason,
        LocalDateTime createdAt
) {
}

public record WebhookRotateSecretResp(
        Long subscriptionId,
        String secret,
        String secretPrefix
) {
}
```

- [ ] **Step 2: Write controller tests**

Create `WebhookSubscriptionControllerTest.java` with tests:

```java
@Test
void createRejectsLocalhostCallbackUrl() {
    WebhookSubscriptionReq req = new WebhookSubscriptionReq(
            "CRM", "http://localhost:8080/hook", List.of("cdp.event.ingested"), 3);

    assertThatThrownBy(() -> controller.create(req).block())
            .hasMessageContaining("callbackUrl is not allowed");
    verify(subscriptionMapper, never()).insert(any());
}

@Test
void createPersistsActiveSubscriptionWithEventTypes() {
    WebhookSubscriptionReq req = new WebhookSubscriptionReq(
            "CRM", "https://example.com/hook", List.of("cdp.event.ingested"), 3);

    WebhookSubscriptionDTO dto = controller.create(req).block().getData();

    verify(subscriptionMapper).insert(argThat(row -> WebhookSubscriptionDO.ACTIVE.equals(row.getStatus())
            && row.getEventTypes().contains("cdp.event.ingested")));
    assertThat(dto.secretPrefix()).startsWith("whsec_");
}

@Test
void pauseResumeAndDisableChangeStatus() {
    WebhookSubscriptionDO row = row(9L, WebhookSubscriptionDO.ACTIVE);
    when(subscriptionMapper.selectById(9L)).thenReturn(row);

    controller.pause(9L).block();
    assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.PAUSED);
    controller.resume(9L).block();
    assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.ACTIVE);
    controller.disable(9L).block();
    assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.DISABLED);
}

@Test
void rotateSecretReturnsRawSecretOnce() {
    when(subscriptionMapper.selectById(9L)).thenReturn(row(9L, WebhookSubscriptionDO.ACTIVE));

    WebhookRotateSecretResp resp = controller.rotateSecret(9L).block().getData();

    assertThat(resp.secret()).startsWith("whsec_");
    assertThat(resp.secretPrefix()).isEqualTo(resp.secret().substring(0, 12));
}

@Test
void testDeliveryUsesDispatcher() {
    when(subscriptionMapper.selectById(9L)).thenReturn(row(9L, WebhookSubscriptionDO.ACTIVE));

    controller.testDelivery(9L).block();

    verify(dispatcher).sendOnce(any(), eq("webhook.test"), anyMap(), anyString(), eq(1));
}
```

- [ ] **Step 3: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionControllerTest
```

Expected: FAIL because controller does not exist.

### Task 2: Controller Implementation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java`

- [ ] **Step 1: Add controller**

Create `WebhookSubscriptionController.java`:

```java
@RestController
@RequestMapping("/cdp/webhooks")
@RequiredArgsConstructor
public class WebhookSubscriptionController {
    private final TenantContextResolver tenantContextResolver;
    private final WebhookSubscriptionMapper subscriptionMapper;
    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final WebhookSubscriptionValidator validator;
    private final WebhookDispatcherService dispatcher;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Mono<R<List<WebhookSubscriptionDTO>>> list() {
        return tenantContextResolver.current().flatMap(ctx -> Mono.fromCallable(() -> R.ok(
                subscriptionMapper.selectList(new LambdaQueryWrapper<WebhookSubscriptionDO>()
                                .eq(WebhookSubscriptionDO::getTenantId, ctx.tenantId())
                                .orderByDesc(WebhookSubscriptionDO::getId))
                        .stream().map(this::toDto).toList()))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<WebhookSubscriptionDTO>> create(@RequestBody WebhookSubscriptionReq req) {
        return tenantContextResolver.current().flatMap(ctx -> Mono.fromCallable(() -> {
            validator.validate(req.callbackUrl(), req.eventTypes());
            WebhookSubscriptionDO row = new WebhookSubscriptionDO();
            row.setTenantId(ctx.tenantId());
            row.setName(req.name().trim());
            row.setCallbackUrl(req.callbackUrl().trim());
            applyNewSecret(row);
            row.setEventTypes(writeJson(req.eventTypes()));
            row.setStatus(WebhookSubscriptionDO.ACTIVE);
            row.setMaxAttempts(req.maxAttempts() == null ? 3 : req.maxAttempts());
            row.setCreatedBy(ctx.username());
            subscriptionMapper.insert(row);
            return R.ok(toDto(row));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{id}/pause")
    public Mono<R<Void>> pause(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.PAUSED);
    }

    @PutMapping("/{id}/resume")
    public Mono<R<Void>> resume(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.ACTIVE);
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.DISABLED);
    }
}
```

Add methods `rotateSecret`, `testDelivery`, `deliveries`, `updateStatus`, `requireTenantRow`, `toDto`, `toDeliveryDto`, `applyNewSecret`, `writeJson`, and `readEventTypes`. `applyNewSecret` creates `whsec_` plus a UUID without dashes, stores prefix/hash/ciphertext, and `rotateSecret` returns the raw secret in `WebhookRotateSecretResp`.

- [ ] **Step 2: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionControllerTest
```

Expected: PASS.

### Task 3: Frontend API And Presentation Helpers

**Files:**
- Modify: `frontend/src/services/cdpEventApi.ts`
- Create: `frontend/src/pages/webhook-subscriptions/webhookSubscriptionPresentation.ts`
- Create: `frontend/src/pages/webhook-subscriptions/webhookSubscriptions.test.ts`

- [ ] **Step 1: Write frontend helper tests**

Create `webhookSubscriptions.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  deliveryStatusColor,
  deliveryStatusLabel,
  maskWebhookSecret,
  subscriptionStatusLabel,
} from './webhookSubscriptionPresentation'

describe('webhook subscription presentation', () => {
  it('labels subscription statuses', () => {
    expect(subscriptionStatusLabel('ACTIVE')).toBe('启用')
    expect(subscriptionStatusLabel('PAUSED')).toBe('暂停')
    expect(subscriptionStatusLabel('DISABLED')).toBe('禁用')
  })

  it('marks dead deliveries as red', () => {
    expect(deliveryStatusLabel('DEAD')).toBe('已终止')
    expect(deliveryStatusColor('DEAD')).toBe('red')
  })

  it('masks webhook secrets after creation', () => {
    expect(maskWebhookSecret('whsec_abcdef')).toBe('whsec_ab****')
  })
})
```

- [ ] **Step 2: Add presentation helper**

Create `webhookSubscriptionPresentation.ts`:

```ts
export function subscriptionStatusLabel(status: string) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'PAUSED') return '暂停'
  if (status === 'DISABLED') return '禁用'
  return status
}

export function deliveryStatusLabel(status: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'RETRYING') return '重试中'
  if (status === 'FAILED') return '失败'
  if (status === 'DEAD') return '已终止'
  return status
}

export function deliveryStatusColor(status: string) {
  if (status === 'SUCCESS') return 'green'
  if (status === 'RETRYING') return 'orange'
  if (status === 'FAILED' || status === 'DEAD') return 'red'
  return 'default'
}

export function maskWebhookSecret(secretPrefix: string) {
  return `${secretPrefix.slice(0, 8)}****`
}
```

- [ ] **Step 3: Extend API helper**

Add to `cdpEventApi.ts`:

```ts
export interface WebhookSubscription {
  id: number
  name: string
  callbackUrl: string
  secretPrefix: string
  eventTypes: string[]
  status: string
  maxAttempts: number
  createdAt?: string | null
  updatedAt?: string | null
}

export interface WebhookDelivery {
  id: number
  deliveryId: string
  eventType: string
  attempt: number
  httpStatus?: number | null
  status: string
  nextRetryAt?: string | null
  errorMessage?: string | null
  terminalReason?: string | null
  createdAt?: string | null
}

export const webhookApi = {
  list: () => http.get<R<WebhookSubscription[]>, R<WebhookSubscription[]>>('/cdp/webhooks'),
  create: (body: { name: string; callbackUrl: string; eventTypes: string[]; maxAttempts?: number }) =>
    http.post<R<WebhookSubscription>, R<WebhookSubscription>>('/cdp/webhooks', body),
  pause: (id: number) => http.put<R<void>, R<void>>(`/cdp/webhooks/${id}/pause`),
  resume: (id: number) => http.put<R<void>, R<void>>(`/cdp/webhooks/${id}/resume`),
  disable: (id: number) => http.delete<R<void>, R<void>>(`/cdp/webhooks/${id}`),
  rotateSecret: (id: number) =>
    http.post<R<{ subscriptionId: number; secret: string; secretPrefix: string }>, R<{ subscriptionId: number; secret: string; secretPrefix: string }>>(`/cdp/webhooks/${id}/rotate-secret`),
  testDelivery: (id: number) => http.post<R<void>, R<void>>(`/cdp/webhooks/${id}/test-delivery`),
  deliveries: (id: number) =>
    http.get<R<WebhookDelivery[]>, R<WebhookDelivery[]>>(`/cdp/webhooks/${id}/deliveries`),
}
```

- [ ] **Step 4: Run helper tests**

Run:

```bash
cd frontend && npm test -- webhookSubscriptions.test.ts
```

Expected: PASS.

### Task 4: Webhook Page And Route

**Files:**
- Create: `frontend/src/pages/webhook-subscriptions/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Add page**

Create `frontend/src/pages/webhook-subscriptions/index.tsx` with:

```tsx
export default function WebhookSubscriptionsPage() {
  const [rows, setRows] = useState<WebhookSubscription[]>([])
  const [loading, setLoading] = useState(false)

  const fetchRows = async () => {
    setLoading(true)
    try {
      const res = await webhookApi.list()
      setRows(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRows()
  }, [])

  return (
    <Table
      rowKey="id"
      loading={loading}
      dataSource={rows}
      columns={[
        { title: '名称', dataIndex: 'name' },
        { title: '回调地址', dataIndex: 'callbackUrl' },
        { title: '事件', render: (_, record) => record.eventTypes.join(', ') },
        { title: 'Secret', render: (_, record) => maskWebhookSecret(record.secretPrefix) },
        { title: '状态', render: (_, record) => <Tag>{subscriptionStatusLabel(record.status)}</Tag> },
      ]}
    />
  )
}
```

Add action buttons for pause, resume, disable, rotate secret, test delivery, and delivery logs using `webhookApi`.

- [ ] **Step 2: Add route and navigation**

Modify `App.tsx`:

```tsx
const WebhookSubscriptionsPage = lazy(() => import('./pages/webhook-subscriptions'))
```

Add route:

```tsx
<Route path="/webhooks" element={<WebhookSubscriptionsPage />} />
```

Modify `AppLayout.tsx` selected key logic:

```ts
if (location.pathname.startsWith('/webhooks')) return 'webhooks'
```

Add menu item:

```tsx
{
  key: 'webhooks',
  icon: <ApiOutlined />,
  label: 'Webhook',
  onClick: () => navigate('/webhooks'),
}
```

- [ ] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionControllerTest
cd frontend && npm test -- webhookSubscriptions.test.ts
```

Expected: PASS.

### Task 5: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005b3-webhook-subscription-api-and-operator-ui.md`
- Read: `docs/product-evolution/plans/p1-005b3-webhook-subscription-api-and-operator-ui-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/webhook \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/WebhookSubscriptionControllerTest.java \
  frontend/src/services/cdpEventApi.ts \
  frontend/src/pages/webhook-subscriptions \
  frontend/src/App.tsx \
  frontend/src/components/layout/AppLayout.tsx \
  docs/product-evolution/specs/p1-005b3-webhook-subscription-api-and-operator-ui.md \
  docs/product-evolution/plans/p1-005b3-webhook-subscription-api-and-operator-ui-plan.md
git commit -m "feat: add webhook subscription management"
```

Expected: commit contains only webhook management API/UI, tests, and docs.
