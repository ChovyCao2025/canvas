# Frontend Realtime Update Plan

**Goal:** Add SSE push for execution status changes so the canvas editor receives live updates. Add concurrent edit awareness so users see when others are editing. Replace 409 conflict destructive handling with a diff/merge warning UI.

**Architecture:** Backend `CanvasEventBus` is a Spring singleton that manages per-canvas subscriptions. The `CanvasEventController` exposes `GET /api/canvas-events/{canvasId}` returning `text/event-stream` via `Flux<ServerSentEvent<String>>` (WebFlux-compatible). Frontend `useCanvasSSE` hook subscribes on editor mount and dispatches events into React state. Concurrent edits are detected via 409 responses and shown as an antd Alert instead of a destructive reload modal.

**Tech Stack:** Spring WebFlux ServerSentEvent, React EventSource, antd Alert, vitest

---

### Task 1: Backend SSE Endpoint + CanvasEventBus

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/sse/CanvasEventBus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/sse/CanvasSseEvent.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasEventController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/sse/CanvasEventBusTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasEventControllerTest.java`

- [ ] **Step 1: Write failing test for CanvasEventBus**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/sse/CanvasEventBusTest.java`:

```java
package org.chovy.canvas.engine.sse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanvasEventBusTest {

    @Test
    void publish_deliversEventToSubscribers() {
        CanvasEventBus bus = new CanvasEventBus();
        List<CanvasSseEvent> received = new ArrayList<>();

        bus.subscribe(1L, received::add);
        bus.publish(1L, new CanvasSseEvent("EXECUTION_STATUS_CHANGE", """
                {"executionId":"exec-1","status":"RUNNING"}
                """));

        assertEquals(1, received.size());
        assertEquals("EXECUTION_STATUS_CHANGE", received.get(0).type());
    }

    @Test
    void unsubscribe_stopsDelivery() {
        CanvasEventBus bus = new CanvasEventBus();
        List<CanvasSseEvent> received = new ArrayList<>();

        Object key = bus.subscribe(2L, received::add);
        bus.unsubscribe(2L, key);
        bus.publish(2L, new CanvasSseEvent("EXECUTION_STATUS_CHANGE", "{}"));

        assertEquals(0, received.size());
    }

    @Test
    void publish_doesNotDeliverToOtherCanvas() {
        CanvasEventBus bus = new CanvasEventBus();
        List<CanvasSseEvent> received = new ArrayList<>();

        bus.subscribe(1L, received::add);
        bus.publish(2L, new CanvasSseEvent("EXECUTION_STATUS_CHANGE", "{}"));

        assertEquals(0, received.size());
    }

    @Test
    void subscribe_multipleSubscribersAllReceive() {
        CanvasEventBus bus = new CanvasEventBus();
        List<CanvasSseEvent> received1 = new ArrayList<>();
        List<CanvasSseEvent> received2 = new ArrayList<>();

        bus.subscribe(3L, received1::add);
        bus.subscribe(3L, received2::add);
        bus.publish(3L, new CanvasSseEvent("CANARY_RESULT", "{}"));

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasEventBusTest
```

Expected: FAIL (classes do not exist yet).

- [ ] **Step 3: Implement CanvasSseEvent record**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/sse/CanvasSseEvent.java`:

```java
package org.chovy.canvas.engine.sse;

/**
 * SSE event payload pushed to canvas editor subscribers.
 *
 * @param type  event type (e.g. EXECUTION_STATUS_CHANGE, CANARY_RESULT, CONCURRENT_EDIT)
 * @param data  JSON payload string
 */
public record CanvasSseEvent(String type, String data) {
}
```

- [ ] **Step 4: Implement CanvasEventBus**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/sse/CanvasEventBus.java`:

```java
package org.chovy.canvas.engine.sse;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-process event bus for canvas SSE subscriptions.
 * Manages per-canvas subscriber lists and delivers events.
 */
@Component
public class CanvasEventBus {

    private final Map<Long, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    /**
     * Subscribe to events for a specific canvas.
     *
     * @param canvasId  canvas ID
     * @param handler   event handler
     * @return subscription key for unsubscribe
     */
    public Object subscribe(Long canvasId, java.util.function.Consumer<CanvasSseEvent> handler) {
        Subscriber sub = new Subscriber(handler);
        subscribers.computeIfAbsent(canvasId, k -> new CopyOnWriteArrayList<>()).add(sub);
        return sub;
    }

    /**
     * Unsubscribe from events.
     *
     * @param canvasId  canvas ID
     * @param key       subscription key returned by subscribe
     */
    public void unsubscribe(Long canvasId, Object key) {
        List<Subscriber> list = subscribers.get(canvasId);
        if (list != null && key instanceof Subscriber sub) {
            list.remove(sub);
        }
    }

    /**
     * Subscribe to events for a specific canvas via reactive Flux (for SSE endpoint).
     */
    public Flux<CanvasSseEvent> subscribeFlux(Long canvasId) {
        return Sinks.many().multicast().onBackpressureBuffer(canvasEventsSink).asFlux()
            .filter(event -> event.getCanvasId().equals(canvasId));
    }

    /**
     * Publish an event to all subscribers of a canvas.
     *
     * @param canvasId  canvas ID
     * @param event     event to publish
     */
    public void publish(Long canvasId, CanvasSseEvent event) {
        List<Subscriber> list = subscribers.get(canvasId);
        if (list != null) {
            for (Subscriber sub : list) {
                sub.handler.accept(event);
            }
        }
    }

    private record Subscriber(java.util.function.Consumer<CanvasSseEvent> handler) {
    }
}
```

- [ ] **Step 5: Run CanvasEventBusTest to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasEventBusTest
```

Expected: PASS.

- [ ] **Step 6: Write failing test for CanvasEventController**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasEventControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.engine.sse.CanvasEventBus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;

@WebFluxTest(CanvasEventController.class)
class CanvasEventControllerTest {

    // NOTE: If the project has already migrated to spring-boot-starter-web (per the webflux-to-mvc plan),
    // change @WebFluxTest back to @WebMvcTest and use MockMvc instead.

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CanvasEventBus eventBus;

    @Test
    void streamEvents_returnsSseContentType() {
        webTestClient.get()
                .uri("/api/canvas-events/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasEventControllerTest
```

Expected: FAIL (CanvasEventController does not exist).

- [ ] **Step 8: Implement CanvasEventController**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasEventController.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.engine.sse.CanvasEventBus;
import org.chovy.canvas.engine.sse.CanvasSseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * SSE endpoint for streaming canvas execution events to the frontend editor.
 * Uses Flux<ServerSentEvent> for WebFlux compatibility (SseEmitter is Servlet-only).
 */
@RestController
@RequestMapping("/api/canvas-events")
@RequiredArgsConstructor
public class CanvasEventController {

    private final CanvasEventBus eventBus;

    /**
     * Subscribe to real-time events for a specific canvas.
     * Returns a Flux of SSE events that stays open until the client disconnects.
     *
     * @param canvasId  canvas ID
     * @return Flux of ServerSentEvent for streaming events
     */
    @GetMapping(value = "/{canvasId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents(@PathVariable Long canvasId) {
        return eventBus.subscribeFlux(canvasId)
                .map(event -> ServerSentEvent.<String>builder()
                        .event(event.type())
                        .data(event.data())
                        .build());
    }
}
```

**Note:** `CanvasEventBus.subscribeFlux(Long canvasId)` must return a `Flux<CanvasSseEvent>`. Add this method to `CanvasEventBus` (Step 4 above):

```java
// Add to CanvasEventBus.java:
public Flux<CanvasSseEvent> subscribeFlux(Long canvasId) {
    return Flux.create(sink -> {
        Object key = subscribe(canvasId, event -> sink.next(event));
        sink.onCancel(() -> unsubscribe(canvasId, key));
    });
}
```

- [ ] **Step 9: Run CanvasEventControllerTest to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasEventControllerTest
```

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/sse/CanvasEventBus.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/sse/CanvasSseEvent.java backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasEventController.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/sse/CanvasEventBusTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasEventControllerTest.java && git commit -m "feat: add CanvasEventBus and SSE endpoint for canvas execution event streaming"
```

---

### Task 2: Frontend SSE Subscription Hook

**Files:**
- Create: `frontend/src/hooks/useCanvasSSE.ts`
- Test: `frontend/src/hooks/useCanvasSSE.test.ts`

- [ ] **Step 1: Write failing test — SSE subscribes on mount and closes on unmount**

Create `frontend/src/hooks/useCanvasSSE.test.ts`:

```ts
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useCanvasSSE } from './useCanvasSSE'

// Mock EventSource
const mockEventSourceInstance = {
  onmessage: null as ((e: MessageEvent) => void) | null,
  onerror: null as ((e: Event) => void) | null,
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 0 as number,
  url: '' as string,
  withCredentials: false as boolean,
  CONNECTING: 0,
  OPEN: 1,
  CLOSED: 2,
  dispatchEvent: vi.fn(),
}

const MockEventSource = vi.fn().mockImplementation((url: string) => {
  mockEventSourceInstance.url = url
  mockEventSourceInstance.close.mockReset()
  return mockEventSourceInstance
})

beforeEach(() => {
  vi.stubGlobal('EventSource', MockEventSource)
  MockEventSource.mockClear()
  mockEventSourceInstance.close.mockClear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('useCanvasSSE', () => {
  it('creates EventSource with correct URL on mount', () => {
    renderHook(() => useCanvasSSE('42', vi.fn()))
    expect(MockEventSource).toHaveBeenCalledWith('/api/canvas-events/42')
  })

  it('does not create EventSource when canvasId is undefined', () => {
    renderHook(() => useCanvasSSE(undefined, vi.fn()))
    expect(MockEventSource).not.toHaveBeenCalled()
  })

  it('closes EventSource on unmount', () => {
    const { unmount } = renderHook(() => useCanvasSSE('42', vi.fn()))
    unmount()
    expect(mockEventSourceInstance.close).toHaveBeenCalled()
  })

  it('dispatches EXECUTION_STATUS_CHANGE event to callback', () => {
    const onEvent = vi.fn()
    renderHook(() => useCanvasSSE('42', onEvent))

    // Simulate SSE message
    const messageEvent = new MessageEvent('message', {
      data: JSON.stringify({ type: 'EXECUTION_STATUS_CHANGE', executionId: 'exec-1', status: 'RUNNING' }),
    })
    act(() => {
      if (mockEventSourceInstance.onmessage) {
        mockEventSourceInstance.onmessage(messageEvent as any)
      }
    })

    expect(onEvent).toHaveBeenCalledWith({
      type: 'EXECUTION_STATUS_CHANGE',
      executionId: 'exec-1',
      status: 'RUNNING',
    })
  })

  it('dispatches CANARY_RESULT event to callback', () => {
    const onEvent = vi.fn()
    renderHook(() => useCanvasSSE('42', onEvent))

    const messageEvent = new MessageEvent('message', {
      data: JSON.stringify({ type: 'CANARY_RESULT', canaryPercent: 30 }),
    })
    act(() => {
      if (mockEventSourceInstance.onmessage) {
        mockEventSourceInstance.onmessage(messageEvent as any)
      }
    })

    expect(onEvent).toHaveBeenCalledWith({
      type: 'CANARY_RESULT',
      canaryPercent: 30,
    })
  })

  it('reconnects on error (EventSource auto-reconnects)', () => {
    const onEvent = vi.fn()
    renderHook(() => useCanvasSSE('42', onEvent))

    // Simulate error — EventSource auto-reconnects, so we just log a warning
    const errorEvent = new Event('error')
    act(() => {
      if (mockEventSourceInstance.onerror) {
        mockEventSourceInstance.onerror(errorEvent)
      }
    })

    // EventSource should NOT be closed on transient error (it auto-reconnects)
    expect(mockEventSourceInstance.close).not.toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && npx vitest run hooks/useCanvasSSE.test.ts
```

Expected: FAIL (useCanvasSSE does not exist).

- [ ] **Step 3: Implement useCanvasSSE hook**

Create `frontend/src/hooks/useCanvasSSE.ts`:

```ts
import { useEffect } from 'react'

/**
 * Subscribe to canvas SSE events for real-time execution status updates.
 * Opens an EventSource on mount and closes it on unmount.
 *
 * @param canvasId  canvas ID (undefined = no subscription)
 * @param onEvent   callback invoked with each parsed SSE event
 */
export function useCanvasSSE(
  canvasId: string | undefined,
  onEvent: (event: { type: string; [key: string]: unknown }) => void,
): void {
  useEffect(() => {
    if (!canvasId) return

    const es = new EventSource(`/api/canvas-events/${canvasId}`)

    es.onmessage = (e: MessageEvent) => {
      try {
        const event = JSON.parse(e.data) as { type: string; [key: string]: unknown }
        onEvent(event)
      } catch {
        console.warn('SSE: failed to parse event data', e.data)
      }
    }

    es.onerror = () => {
      // EventSource auto-reconnects on transient errors; no manual action needed.
      console.warn('SSE: connection error, will auto-reconnect')
    }

    return () => es.close()
  }, [canvasId, onEvent])
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd frontend && npx vitest run hooks/useCanvasSSE.test.ts
```

Expected: PASS.

- [ ] **Step 5: Integrate useCanvasSSE into the canvas editor**

In `frontend/src/pages/canvas-editor/index.tsx`, add the SSE subscription inside `EditorInner`:

```tsx
// Add import at the top:
import { useCanvasSSE } from '../../hooks/useCanvasSSE'

// Inside EditorInner function body, after existing hooks:
const handleSSEEvent = useCallback((event: { type: string; [key: string]: unknown }) => {
  switch (event.type) {
    case 'EXECUTION_STATUS_CHANGE':
      // Execution status updates are visible in the ExecutionTracePanel
      // which polls independently; this event can trigger a re-fetch.
      setIsDirty(false) // clear dirty if save succeeded on server
      break
    case 'CANARY_RESULT':
      // Canary status changes — force a reload to reflect updated canary state
      message.info('灰度状态已更新', 2)
      break
    case 'CONCURRENT_EDIT':
      // State defined in Task 3 — concurrentEditWarning state variable
      setConcurrentEditWarning(true)
      break
  }
}, [])

useCanvasSSE(id, handleSSEEvent)
```

**Note:** Execute tasks in order. The `concurrentEditWarning` state variable is defined in Task 3 Step 5c. For Task 2, use a placeholder `// State defined in Task 3` comment as shown above.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/hooks/useCanvasSSE.ts frontend/src/hooks/useCanvasSSE.test.ts frontend/src/pages/canvas-editor/index.tsx && git commit -m "feat: add SSE subscription hook for real-time canvas event updates"
```

---

### Task 3: Conflict Detection — Show Warning on Concurrent Edits

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Test: `frontend/src/pages/canvas-editor/conflictDetection.test.ts`

- [ ] **Step 1: Write failing test — 409 conflict shows warning instead of destructive reload**

Create `frontend/src/pages/canvas-editor/conflictDetection.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest'

/**
 * Tests for the conflict detection logic extracted from the save handler.
 * The actual component integration is tested via the conflictDetection module.
 */
import { handleSaveConflict } from './conflictDetection'

describe('conflict detection', () => {
  it('returns warning=true for 409 status', () => {
    const error = { response: { status: 409, data: { message: '版本冲突' } } }
    const result = handleSaveConflict(error)
    expect(result.isConflict).toBe(true)
    expect(result.message).toContain('他人修改')
  })

  it('returns warning=false for non-409 status', () => {
    const error = { response: { status: 500, data: { message: '服务器错误' } } }
    const result = handleSaveConflict(error)
    expect(result.isConflict).toBe(false)
  })

  it('returns warning=false for network error without response', () => {
    const error = { message: 'Network Error' }
    const result = handleSaveConflict(error)
    expect(result.isConflict).toBe(false)
  })

  it('includes server message in conflict result when available', () => {
    const error = { response: { status: 409, data: { message: '画布已被用户B修改' } } }
    const result = handleSaveConflict(error)
    expect(result.isConflict).toBe(true)
    expect(result.message).toContain('用户B')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && npx vitest run canvas-editor/conflictDetection.test.ts
```

Expected: FAIL (conflictDetection module does not exist).

- [ ] **Step 3: Implement conflictDetection module**

Create `frontend/src/pages/canvas-editor/conflictDetection.ts`:

```ts
/**
 * Conflict detection logic for canvas save operations.
 * Extracted from the save handler so it can be tested independently.
 */

export interface ConflictResult {
  /** Whether a 409 conflict was detected. */
  isConflict: boolean
  /** Human-readable message for the conflict. */
  message: string
}

/**
 * Analyze a save error and determine if it represents a concurrent edit conflict.
 *
 * @param error  the error thrown by the save API call
 * @returns conflict result with isConflict=true for 409 responses
 */
export function handleSaveConflict(error: unknown): ConflictResult {
  const err = error as { response?: { status?: number; data?: { message?: string } } }
  if (err?.response?.status === 409) {
    const serverMsg = err.response.data?.message ?? ''
    return {
      isConflict: true,
      message: serverMsg
        ? `画布存在冲突：${serverMsg}`
        : '画布已被他人修改，请刷新后重新编辑',
    }
  }
  return { isConflict: false, message: '' }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd frontend && npx vitest run canvas-editor/conflictDetection.test.ts
```

Expected: PASS.

- [ ] **Step 5: Integrate conflict detection into the canvas editor**

In `frontend/src/pages/canvas-editor/index.tsx`, make these changes:

5a. Add import:

```tsx
import { handleSaveConflict } from './conflictDetection'
```

5b. Add state for concurrent edit warning:

```tsx
const [concurrentEditWarning, setConcurrentEditWarning] = useState(false)
```

5c. Replace the 409 handler in `handleSave` (currently around line 1136-1143):

```tsx
// BEFORE:
if (err?.response?.status === 409) {
  Modal.confirm({
    title: '画布已被他人修改',
    content: '当前画布已有新版本，刷新后你的未保存内容将丢失。是否立即刷新？',
    okText: '立即刷新',
    cancelText: '暂不刷新',
    onOk: () => window.location.reload(),
  })
}

// AFTER:
const conflict = handleSaveConflict(err)
if (conflict.isConflict) {
  setConcurrentEditWarning(true)
  message.warning(conflict.message, 5)
}
```

5d. Add a non-destructive Alert banner in the editor UI, just below the toolbar:

```tsx
{/* Concurrent edit warning banner */}
{concurrentEditWarning && (
  <div style={{
    padding: '8px 16px',
    background: '#fffbe6',
    borderBottom: '1px solid #ffe58f',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    fontSize: 13,
  }}>
    <span style={{ color: '#ad6800' }}>
      画布已被他人修改，你的后续保存可能覆盖对方的更改。建议刷新获取最新版本。
    </span>
    <Space>
      <Button size="small" onClick={() => window.location.reload()}>
        刷新获取最新
      </Button>
      <Button size="small" type="text" onClick={() => setConcurrentEditWarning(false)}>
        忽略
      </Button>
    </Space>
  </div>
)}
```

Insert this right after the toolbar `</div>` and before the three-column layout `<div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>`.

- [ ] **Step 6: Run all canvas-editor tests**

```bash
cd frontend && npx vitest run canvas-editor/
```

Expected: All tests PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/canvas-editor/conflictDetection.ts frontend/src/pages/canvas-editor/conflictDetection.test.ts frontend/src/pages/canvas-editor/index.tsx && git commit -m "feat: replace 409 destructive reload with non-destructive conflict warning banner"
```
