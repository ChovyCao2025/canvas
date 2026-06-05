# Safe Message Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add side-effect-free SEND_MESSAGE preview with masked content, backend API, and editor entry point.

**Architecture:** Keep preview separate from delivery services. `CanvasMessagePreviewService` parses graph JSON, reads one selected `SEND_MESSAGE` node, resolves supplied context values, masks output with `DataMaskingUtil`, and returns a response through `CanvasController`.

**Tech Stack:** Java 21, Spring Boot WebFlux controller style, Jackson, JUnit 5, AssertJ, React 18, TypeScript, Ant Design, Vitest.

---

## Implementation Status

- Status: implemented and focused-verified on 2026-06-05.
- Backend preview service, DTOs, and `/canvas/{id}/message-preview` endpoint are present and verified.
- Frontend helper, API client method, and selected `SEND_MESSAGE` editor modal entry are implemented.
- Commit: not created in this session.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003d-safe-message-preview.md`

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewResp.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-editor/messagePreview.ts`
- Create: `frontend/src/pages/canvas-editor/messagePreview.test.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

### Task 1: Backend Preview Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewService.java`

- [x] **Step 1: Write preview service tests**

Create `CanvasMessagePreviewServiceTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dto.canvas.MessagePreviewReq;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasMessagePreviewServiceTest {

    @Test
    void previewMasksSensitiveFieldsAndResolvesVariables() {
        CanvasMessagePreviewService service = new CanvasMessagePreviewService(new ObjectMapper());
        String graphJson = """
                {"nodes":[{"id":"send","type":"SEND_MESSAGE","config":{
                  "channel":"WEB_PUSH",
                  "templateId":"tpl-1",
                  "title":"订单提醒",
                  "body":"Hi $name, phone $phone",
                  "variables":{"name":"$name","phone":"$phone","token":"$token"}
                }}]}
                """;

        var resp = service.preview(new MessagePreviewReq(
                62L,
                "send",
                "u1",
                graphJson,
                Map.of("name", "Alice", "phone", "13812345678", "token", "secret-token")));

        assertThat(resp.channel()).isEqualTo("WEB_PUSH");
        assertThat(resp.templateId()).isEqualTo("tpl-1");
        assertThat(resp.variables()).containsEntry("phone", "******");
        assertThat(String.valueOf(resp.content().get("body"))).contains("138****5678");
        assertThat(resp.warnings()).contains("PREVIEW_ONLY_NO_SEND");
    }

    @Test
    void previewRejectsNonSendMessageNode() {
        CanvasMessagePreviewService service = new CanvasMessagePreviewService(new ObjectMapper());
        String graphJson = "{\"nodes\":[{\"id\":\"tag\",\"type\":\"TAGGER\",\"config\":{}}]}";

        assertThatThrownBy(() -> service.preview(new MessagePreviewReq(
                62L, "tag", "u1", graphJson, Map.of())))
                .hasMessageContaining("SEND_MESSAGE");
    }
}
```

- [x] **Step 2: Run preview tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasMessagePreviewServiceTest
```

Expected: FAIL because preview DTOs and service do not exist.

- [x] **Step 3: Add DTO records**

Create `MessagePreviewReq.java`:

```java
package org.chovy.canvas.dto.canvas;

import java.util.Map;

public record MessagePreviewReq(
        Long canvasId,
        String nodeId,
        String userId,
        String graphJson,
        Map<String, Object> context
) {
}
```

Create `MessagePreviewResp.java`:

```java
package org.chovy.canvas.dto.canvas;

import java.util.List;
import java.util.Map;

public record MessagePreviewResp(
        String channel,
        String templateId,
        Map<String, Object> content,
        Map<String, Object> variables,
        List<String> warnings
) {
}
```

- [x] **Step 4: Implement preview service**

Create `CanvasMessagePreviewService.java`:

```java
@Service
@RequiredArgsConstructor
public class CanvasMessagePreviewService {

    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public MessagePreviewResp preview(MessagePreviewReq req) {
        Map<String, Object> root = parseGraph(req.graphJson());
        Map<String, Object> node = findNode(root, req.nodeId());
        if (!"SEND_MESSAGE".equals(String.valueOf(node.get("type")))) {
            throw new IllegalArgumentException("Message preview requires a SEND_MESSAGE node: " + req.nodeId());
        }
        Map<String, Object> config = node.get("config") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        Map<String, Object> context = req.context() == null ? Map.of() : req.context();
        Map<String, Object> content = new LinkedHashMap<>();
        copyResolved(config, content, context, "subject");
        copyResolved(config, content, context, "previewText");
        copyResolved(config, content, context, "title");
        copyResolved(config, content, context, "body");
        copyResolved(config, content, context, "content");
        copyResolved(config, content, context, "imageUrl");
        copyResolved(config, content, context, "clickUrl");
        copyResolved(config, content, context, "fromName");
        copyResolved(config, content, context, "fromEmail");

        Map<String, Object> variables = resolveVariables(config.get("variables"), context);
        return new MessagePreviewResp(
                string(config, "channel", "MESSAGE"),
                string(config, "templateId", null),
                (Map<String, Object>) DataMaskingUtil.maskObject(content),
                (Map<String, Object>) DataMaskingUtil.maskObject(variables),
                List.of("PREVIEW_ONLY_NO_SEND"));
    }
}
```

Add private helpers `parseGraph`, `findNode`, `copyResolved`, `resolveVariables`, `resolveTemplate`, and `string`. `resolveTemplate` replaces `$key` tokens with values from the supplied context and returns the original value when a key is absent.

- [x] **Step 5: Run preview service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasMessagePreviewServiceTest
```

Expected: PASS.

### Task 2: API Endpoint And Frontend Helper

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-editor/messagePreview.ts`
- Create: `frontend/src/pages/canvas-editor/messagePreview.test.ts`

- [x] **Step 1: Add controller endpoint**

Inject service:

```java
private final CanvasMessagePreviewService messagePreviewService;
```

Add endpoint:

```java
@PostMapping("/{id}/message-preview")
public Mono<R<MessagePreviewResp>> previewMessage(
        @PathVariable Long id,
        @RequestBody MessagePreviewReq req) {
    MessagePreviewReq normalized = new MessagePreviewReq(
            id, req.nodeId(), req.userId(), req.graphJson(), req.context());
    return Mono.fromCallable(() -> messagePreviewService.preview(normalized))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
```

- [x] **Step 2: Add frontend helper test**

Create `messagePreview.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { buildMessagePreviewPayload } from './messagePreview'

describe('message preview payload', () => {
  it('includes graph json, node id, user id, and parsed context', () => {
    expect(buildMessagePreviewPayload({
      canvasId: 62,
      nodeId: 'send',
      userId: 'u1',
      graphJson: '{"nodes":[]}',
      contextJson: '{"phone":"13812345678"}',
    })).toEqual({
      canvasId: 62,
      nodeId: 'send',
      userId: 'u1',
      graphJson: '{"nodes":[]}',
      context: { phone: '13812345678' },
    })
  })
})
```

- [x] **Step 3: Add frontend helper and API method**

Create `messagePreview.ts`:

```ts
export interface MessagePreviewInput {
  canvasId: number
  nodeId: string
  userId: string
  graphJson: string
  contextJson: string
}

export function buildMessagePreviewPayload(input: MessagePreviewInput) {
  return {
    canvasId: input.canvasId,
    nodeId: input.nodeId,
    userId: input.userId,
    graphJson: input.graphJson,
    context: input.contextJson.trim() ? JSON.parse(input.contextJson) : {},
  }
}
```

In `api.ts`, add response type and `canvasApi.previewMessage`:

```ts
export interface MessagePreviewResp {
  channel: string
  templateId?: string
  content: Record<string, unknown>
  variables: Record<string, unknown>
  warnings: string[]
}

previewMessage: (id: number, body: {
  canvasId: number
  nodeId: string
  userId: string
  graphJson: string
  context: Record<string, unknown>
}) => http.post<R<MessagePreviewResp>, R<MessagePreviewResp>>(`/canvas/${id}/message-preview`, body),
```

- [x] **Step 4: Run helper test**

Run:

```bash
cd frontend && npm test -- messagePreview.test.ts
```

Expected: PASS.

### Task 3: Editor UI Wiring

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [x] **Step 1: Add editor modal state**

Add state near existing editor side-panel state:

```tsx
const [messagePreviewOpen, setMessagePreviewOpen] = useState(false)
const [messagePreviewUserId, setMessagePreviewUserId] = useState('user_test_001')
const [messagePreviewContext, setMessagePreviewContext] = useState('{}')
const [messagePreviewResult, setMessagePreviewResult] = useState<MessagePreviewResp | null>(null)
```

- [x] **Step 2: Add preview action**

Add handler:

```tsx
const handleMessagePreview = async () => {
  if (!selectedNodeId) return
  const res = await canvasApi.previewMessage(canvasId, buildMessagePreviewPayload({
    canvasId,
    nodeId: selectedNodeId,
    userId: messagePreviewUserId,
    graphJson: buildSaveGraphJson(nodes),
    contextJson: messagePreviewContext,
  }))
  setMessagePreviewResult(res.data)
}
```

- [x] **Step 3: Render button and modal**

Render the action only for the selected message node:

```tsx
{selectedNode?.data?.nodeType === 'SEND_MESSAGE' && (
  <Button onClick={() => setMessagePreviewOpen(true)}>预览</Button>
)}
```

Render modal near existing editor modals:

```tsx
<Modal
  title="消息预览"
  open={messagePreviewOpen}
  onOk={handleMessagePreview}
  onCancel={() => setMessagePreviewOpen(false)}
  okText="生成预览"
  cancelText="关闭"
>
  <Space direction="vertical" style={{ width: '100%' }}>
    <Input value={messagePreviewUserId} onChange={event => setMessagePreviewUserId(event.target.value)} />
    <Input.TextArea rows={6} value={messagePreviewContext} onChange={event => setMessagePreviewContext(event.target.value)} />
    {messagePreviewResult && (
      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(messagePreviewResult, null, 2)}</pre>
    )}
  </Space>
</Modal>
```

- [x] **Step 4: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasMessagePreviewServiceTest
cd frontend && npm test -- messagePreview.test.ts
```

Expected: PASS.

### Verification Evidence

- Backend preview service and controller suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasMessagePreviewServiceTest,CanvasControllerOperatorLoopTest
```

Result: 6 tests, 0 failures, 0 errors, 0 skipped.

- Frontend message preview helper suite:

```bash
cd frontend && npm test -- messagePreview.test.ts
```

Result: 1 test file, 2 tests passed.

- Frontend production build:

```bash
cd frontend && npm run build
```

Result: TypeScript and Vite build passed.

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-003d-safe-message-preview.md`
- Read: `docs/product-evolution/plans/p1-003d-safe-message-preview-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewResp.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewServiceTest.java \
  frontend/src/services/api.ts \
  frontend/src/pages/canvas-editor/messagePreview.ts \
  frontend/src/pages/canvas-editor/messagePreview.test.ts \
  frontend/src/pages/canvas-editor/index.tsx \
  docs/product-evolution/specs/p1-003d-safe-message-preview.md \
  docs/product-evolution/plans/p1-003d-safe-message-preview-plan.md
git commit -m "feat: add safe message preview"
```

Expected: commit contains only message preview backend, frontend, tests, and docs.
