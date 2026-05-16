# Node Merge (Group A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge TAGGER_OFFLINE + TAGGER_REALTIME into a single `TAGGER` node, and merge BEHAVIOR_IN_APP + DIRECT_CALL into a single `BEHAVIOR_TRIGGER` node. Old types remain in DB with `enabled=0` for backward compatibility.

**Architecture:** Two Flyway migrations (V26, V27) add new node types and hide old ones. Two new Java Handler classes delegate to existing handlers. Frontend adds `showWhen` conditional rendering to ConfigPanel, dynamic `isTrigger` logic to CanvasNode, and updates constants. No data migration is required for running canvases (old node types still render correctly from their unchanged DB schema).

**Tech Stack:** Spring Boot (WebFlux), MyBatis-Plus, Flyway, MySQL, JUnit 5, React/TypeScript.

---

## File Map

| Action | File |
|--------|------|
| Create | `backend/.../db/migration/V26__tagger_merged_type.sql` |
| Create | `backend/.../db/migration/V27__behavior_trigger_merged_type.sql` |
| Create | `backend/.../handlers/TaggerHandler.java` |
| Create | `backend/.../handlers/BehaviorTriggerHandler.java` |
| Create | `backend/.../handlers/TaggerHandlerTest.java` |
| Create | `backend/.../handlers/BehaviorTriggerHandlerTest.java` |
| Modify | `frontend/src/components/config-panel/index.tsx` |
| Modify | `frontend/src/components/canvas/CanvasNode.tsx` |
| Modify | `frontend/src/components/canvas/constants.ts` |

Full backend path prefix: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/`

---

### Task 1: Flyway V26 — TAGGER merged type

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V26__tagger_merged_type.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V26: Merge TAGGER_OFFLINE + TAGGER_REALTIME into a single TAGGER node type.
-- Old types kept with enabled=0 so existing canvases continue to render.

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'TAGGER',
  'Tagger 标签',
  '行为策略',
  'org.chovy.canvas.engine.handlers.TaggerHandler',
  '[
    {
      "key":      "mode",
      "label":    "标签模式",
      "type":     "radio",
      "required": true,
      "options":  [
        {"label": "实时触发（监听 MQ 事件）", "value": "realtime"},
        {"label": "离线打标（流程内执行）",    "value": "offline"}
      ]
    },
    {
      "key":        "tagCodeKey",
      "label":      "标签",
      "type":       "select",
      "dataSource": "/meta/tagger-tags",
      "required":   true
    }
  ]',
  '[]',
  0, 0,
  '实时或离线方式对用户打 Tagger 标签。',
  1
);

UPDATE node_type_registry SET enabled = 0
WHERE type_key IN (''TAGGER_OFFLINE'', ''TAGGER_REALTIME'');
```

Wait — single quotes in SQL need escaping. Rewrite:

```sql
-- V26: Merge TAGGER_OFFLINE + TAGGER_REALTIME into TAGGER node type.
INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'TAGGER',
  'Tagger 标签',
  '行为策略',
  'org.chovy.canvas.engine.handlers.TaggerHandler',
  '[{"key":"mode","label":"标签模式","type":"radio","required":true,"options":[{"label":"实时触发（监听 MQ 事件）","value":"realtime"},{"label":"离线打标（流程内执行）","value":"offline"}]},{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true}]',
  '[]',
  0, 0,
  '实时或离线方式对用户打 Tagger 标签。',
  1
);

UPDATE node_type_registry SET enabled = 0
WHERE type_key IN ('TAGGER_OFFLINE', 'TAGGER_REALTIME');
```

- [ ] **Step 2: Apply and verify**

```bash
cd backend/canvas-engine && ./gradlew bootRun 2>&1 | grep -E "V26|Successfully|ERROR"
```

Expected: `Successfully applied 1 migration -> V26`

- [ ] **Step 3: Verify DB**

```sql
SELECT type_key, enabled FROM node_type_registry
WHERE type_key IN ('TAGGER', 'TAGGER_OFFLINE', 'TAGGER_REALTIME');
```

Expected:
```
TAGGER          | 1
TAGGER_OFFLINE  | 0
TAGGER_REALTIME | 0
```

- [ ] **Step 4: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V26__tagger_merged_type.sql
git commit -m "feat: add TAGGER merged node type, disable old TAGGER_OFFLINE/REALTIME (V26)"
```

---

### Task 2: TaggerHandler.java

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java`
- Create: corresponding test

Find the existing handler package by running:
```bash
ls backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ | grep -i tagger
```

- [ ] **Step 1: Write failing test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TaggerHandlerTest {

    @Test
    void delegates_to_offline_handler_when_mode_is_offline() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler);

        NodeResult expected = NodeResult.next("next_node");
        when(offlineHandler.executeAsync(any(), any())).thenReturn(Mono.just(expected));

        ExecutionContext ctx = new ExecutionContext(Map.of("userId", "u1"), Map.of());
        NodeResult result = handler.executeAsync(
            Map.of("mode", "offline", "tagCodeKey", "high_value"),
            ctx
        ).block();

        assertThat(result).isEqualTo(expected);
        Mockito.verify(offlineHandler).executeAsync(any(), any());
        Mockito.verify(realtimeHandler, Mockito.never()).executeAsync(any(), any());
    }

    @Test
    void delegates_to_realtime_handler_when_mode_is_realtime() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler);

        NodeResult expected = NodeResult.next("next_node");
        when(realtimeHandler.executeAsync(any(), any())).thenReturn(Mono.just(expected));

        ExecutionContext ctx = new ExecutionContext(Map.of("userId", "u1"), Map.of());
        handler.executeAsync(
            Map.of("mode", "realtime", "tagCodeKey", "realtime_active"),
            ctx
        ).block();

        Mockito.verify(realtimeHandler).executeAsync(any(), any());
        Mockito.verify(offlineHandler, Mockito.never()).executeAsync(any(), any());
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```bash
cd backend/canvas-engine && ./gradlew test --tests "*.TaggerHandlerTest" 2>&1 | tail -15
```

Expected: compilation error mentioning `TaggerHandler` not found.

- [ ] **Step 3: Create TaggerHandler.java**

Locate the existing handlers' package and base class by checking:
```bash
head -5 backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerOfflineHandler.java
```

Create `TaggerHandler.java` in the same package:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.AbstractNodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Unified Tagger handler — delegates to offline or realtime based on config.mode.
 */
@Component("TAGGER")
public class TaggerHandler extends AbstractNodeHandler {

    private final TaggerOfflineHandler  offlineHandler;
    private final TaggerRealtimeHandler realtimeHandler;

    @Autowired
    public TaggerHandler(TaggerOfflineHandler offlineHandler,
                         TaggerRealtimeHandler realtimeHandler) {
        this.offlineHandler  = offlineHandler;
        this.realtimeHandler = realtimeHandler;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String mode = (String) config.getOrDefault("mode", "offline");
        if ("realtime".equals(mode)) {
            return realtimeHandler.executeAsync(config, ctx);
        }
        return offlineHandler.executeAsync(config, ctx);
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

```bash
./gradlew test --tests "*.TaggerHandlerTest" 2>&1 | tail -10
```

Expected: `TaggerHandlerTest > delegates_to_offline_handler... PASSED` etc.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java
git commit -m "feat: TaggerHandler delegates to offline/realtime based on mode config"
```

---

### Task 3: Flyway V27 — BEHAVIOR_TRIGGER merged type

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V27__behavior_trigger_merged_type.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V27: Merge BEHAVIOR_IN_APP + DIRECT_CALL into BEHAVIOR_TRIGGER node type.
INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'BEHAVIOR_TRIGGER',
  '行为触发',
  '行为策略',
  'org.chovy.canvas.engine.handlers.BehaviorTriggerHandler',
  '[{"key":"triggerType","label":"触发方式","type":"radio","required":true,"options":[{"label":"端内行为事件（监听 MQ）","value":"inapp"},{"label":"业务直调（HTTP 推送）","value":"direct"}]},{"key":"eventCode","label":"触发事件","type":"select","dataSource":"/meta/event-definitions","required":true,"showWhen":"triggerType==inapp"},{"key":"_attrHint","label":"可用上下文变量","type":"event-attr-preview","showWhen":"triggerType==inapp"},{"key":"inputParams","label":"入参定义","type":"param-define-list","showWhen":"triggerType==direct"}]',
  '[]',
  1, 0,
  '通过端内行为事件或业务 HTTP 直调触发旅程。',
  1
);

UPDATE node_type_registry SET enabled = 0
WHERE type_key IN ('BEHAVIOR_IN_APP', 'DIRECT_CALL');
```

- [ ] **Step 2: Apply and verify**

```bash
cd backend/canvas-engine && ./gradlew bootRun 2>&1 | grep -E "V27|Successfully|ERROR"
```

- [ ] **Step 3: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V27__behavior_trigger_merged_type.sql
git commit -m "feat: add BEHAVIOR_TRIGGER merged node type, disable old BEHAVIOR_IN_APP/DIRECT_CALL (V27)"
```

---

### Task 4: BehaviorTriggerHandler.java

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/BehaviorTriggerHandler.java`
- Create: test

- [ ] **Step 1: Write failing test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/BehaviorTriggerHandlerTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class BehaviorTriggerHandlerTest {

    @Test
    void routes_inapp_to_behaviorInAppHandler() {
        BehaviorInAppHandler inappHandler  = Mockito.mock(BehaviorInAppHandler.class);
        DirectCallHandler    directHandler = Mockito.mock(DirectCallHandler.class);
        BehaviorTriggerHandler handler = new BehaviorTriggerHandler(inappHandler, directHandler);

        NodeResult expected = NodeResult.next("n2");
        when(inappHandler.executeAsync(any(), any())).thenReturn(Mono.just(expected));

        ExecutionContext ctx = new ExecutionContext(Map.of("userId", "u1"), Map.of());
        handler.executeAsync(Map.of("triggerType", "inapp", "eventCode", "add_cart"), ctx).block();

        verify(inappHandler).executeAsync(any(), any());
        verify(directHandler, never()).executeAsync(any(), any());
    }

    @Test
    void routes_direct_to_directCallHandler() {
        BehaviorInAppHandler inappHandler  = Mockito.mock(BehaviorInAppHandler.class);
        DirectCallHandler    directHandler = Mockito.mock(DirectCallHandler.class);
        BehaviorTriggerHandler handler = new BehaviorTriggerHandler(inappHandler, directHandler);

        when(directHandler.executeAsync(any(), any())).thenReturn(Mono.just(NodeResult.next("n3")));

        ExecutionContext ctx = new ExecutionContext(Map.of("userId", "u1"), Map.of());
        handler.executeAsync(Map.of("triggerType", "direct"), ctx).block();

        verify(directHandler).executeAsync(any(), any());
        verify(inappHandler, never()).executeAsync(any(), any());
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```bash
./gradlew test --tests "*.BehaviorTriggerHandlerTest" 2>&1 | tail -10
```

- [ ] **Step 3: Create BehaviorTriggerHandler.java**

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.AbstractNodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component("BEHAVIOR_TRIGGER")
public class BehaviorTriggerHandler extends AbstractNodeHandler {

    private final BehaviorInAppHandler inappHandler;
    private final DirectCallHandler    directHandler;

    @Autowired
    public BehaviorTriggerHandler(BehaviorInAppHandler inappHandler,
                                   DirectCallHandler directHandler) {
        this.inappHandler  = inappHandler;
        this.directHandler = directHandler;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String triggerType = (String) config.getOrDefault("triggerType", "inapp");
        if ("direct".equals(triggerType)) {
            return directHandler.executeAsync(config, ctx);
        }
        return inappHandler.executeAsync(config, ctx);
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

```bash
./gradlew test --tests "*.BehaviorTriggerHandlerTest" 2>&1 | tail -10
```

- [ ] **Step 5: Run full test suite**

```bash
./gradlew test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/BehaviorTriggerHandler.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/BehaviorTriggerHandlerTest.java
git commit -m "feat: BehaviorTriggerHandler delegates to inapp/direct based on triggerType"
```

---

### Task 5: Frontend — ConfigPanel `showWhen` support

The new node schemas use `"showWhen": "fieldKey==value"` to conditionally show fields. ConfigPanel already has `evaluateVisible` for a `visible` field — reuse the same logic for `showWhen`.

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`

- [ ] **Step 1: Extend `SchemaField` type**

Find the `SchemaField` interface (or type) and add:
```ts
showWhen?: string  // same syntax as visible: "fieldKey==value"
```

- [ ] **Step 2: Apply `showWhen` in the field-map filter**

Find the `.filter(f => evaluateVisible(f.visible, formValues))` call. Extend it:

```ts
.filter(f =>
  evaluateVisible(f.visible,  formValues) &&
  evaluateVisible(f.showWhen, formValues)
)
```

`evaluateVisible` already handles `undefined` → returns `true`, so this is safe with no other changes.

- [ ] **Step 3: Type check**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
```

- [ ] **Step 4: Manual test**

1. Drag a BEHAVIOR_TRIGGER node onto the canvas
2. Config panel shows "触发方式" radio only
3. Select "端内行为事件" → "触发事件" and "可用上下文变量" fields appear
4. Select "业务直调" → only "入参定义" appears

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/config-panel/index.tsx
git commit -m "feat: ConfigPanel supports showWhen conditional field rendering"
```

---

### Task 6: Frontend — dynamic isTrigger + constants update

**Files:**
- Modify: `frontend/src/components/canvas/CanvasNode.tsx`
- Modify: `frontend/src/components/canvas/constants.ts`

- [ ] **Step 1: Update `constants.ts`**

In `TRIGGER_TYPES`, remove old types and add new ones:
```ts
export const TRIGGER_TYPES = new Set([
  'START',
  'BEHAVIOR_TRIGGER',  // replaces BEHAVIOR_IN_APP + DIRECT_CALL
  // TAGGER is NOT in TRIGGER_TYPES — its trigger status is dynamic (see CanvasNode)
])
```

In `DEFAULT_NAMES`, add new types and keep old ones for backward compat:
```ts
TAGGER:            'Tagger 标签',
BEHAVIOR_TRIGGER:  '行为触发',
// Keep for existing canvases:
TAGGER_OFFLINE:    'Tagger离线标签',
TAGGER_REALTIME:   'Tagger实时标签',
BEHAVIOR_IN_APP:   '端内行为触发',
DIRECT_CALL:       '业务直调',
```

- [ ] **Step 2: Dynamic isTrigger in CanvasNode**

In `CanvasNode.tsx`, replace:
```tsx
const isTrigger = TRIGGER_TYPES.has(d.nodeType)
```
With:
```tsx
const isTrigger = TRIGGER_TYPES.has(d.nodeType)
  || (d.nodeType === 'TAGGER' && d.bizConfig?.mode === 'realtime')
```

- [ ] **Step 3: Update START node label for TAGGER mode**

The START node renders a small mode label from `d.bizConfig?.triggerType`. This doesn't affect TAGGER (which is not a START-type node), so no change needed there.

- [ ] **Step 4: Type check**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
```

- [ ] **Step 5: Manual test**

1. Drag a TAGGER node → initially shows target handle (offline mode default)
2. In config panel, switch mode to "实时触发" → target handle disappears (node becomes trigger)
3. Switch back to "离线打标" → target handle reappears
4. Drag a BEHAVIOR_TRIGGER node → no target handle (it's always a trigger)

- [ ] **Step 6: Run tests**

```bash
cd frontend && npm test 2>&1 | tail -10
cd backend/canvas-engine && ./gradlew test 2>&1 | tail -10
```

- [ ] **Step 7: Commit**

```bash
git add \
  frontend/src/components/canvas/CanvasNode.tsx \
  frontend/src/components/canvas/constants.ts
git commit -m "feat: dynamic isTrigger for TAGGER mode, update TRIGGER_TYPES + DEFAULT_NAMES"
```

---

### Task 7: Verify node panel hides old types

**Files:**
- Modify: `frontend/src/components/node-panel/index.tsx` (if needed)

- [ ] **Step 1: Check LEGACY_TRIGGERS**

Open `frontend/src/components/node-panel/index.tsx` line 8. Confirm `LEGACY_TRIGGERS` contains all 4 old types:

```ts
const LEGACY_TRIGGERS = new Set([
  'BEHAVIOR_IN_APP',
  'SCHEDULED_TRIGGER',
  'MQ_TRIGGER',
  'DIRECT_CALL',
  'TAGGER_REALTIME',
  'TAGGER_OFFLINE',   // ← ADD if not present
])
```

The node panel filters out `LEGACY_TRIGGERS` from the drag list. Since the new types (`TAGGER`, `BEHAVIOR_TRIGGER`) come from the backend API with `enabled=1`, they will appear automatically.

- [ ] **Step 2: Manual test**

Open the node panel. Confirm:
- "Tagger 标签" appears under 行为策略
- "行为触发" appears under 行为策略
- Individual TAGGER_OFFLINE, TAGGER_REALTIME, BEHAVIOR_IN_APP, DIRECT_CALL entries are gone

- [ ] **Step 3: Commit (if changes were needed)**

```bash
git add frontend/src/components/node-panel/index.tsx
git commit -m "feat: hide old TAGGER_OFFLINE/REALTIME from node panel (covered by LEGACY_TRIGGERS)"
```
