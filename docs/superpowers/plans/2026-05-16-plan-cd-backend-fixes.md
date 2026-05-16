# Backend Config Fixes (Group C + D) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix SEND_MQ parameter loading bug (V28) and replace hardcoded MQ topics with user-configurable DB-driven list (V29).

**Architecture:** Two independent Flyway SQL migrations. No frontend changes needed — ConfigPanel's `api-input-params` control already supports `apiKeyField`/`defsSource` overrides; we just need to pass the right values via the schema. The `MqTriggerHandler` needs a one-line change to look up the topic from `mq_message_definition`.

**Tech Stack:** MySQL, Flyway, Spring Boot / MyBatis-Plus, JUnit 5 + AssertJ.

---

## File Map

| Action | File |
|--------|------|
| Create | `backend/canvas-engine/src/main/resources/db/migration/V28__fix_send_mq_schema.sql` |
| Create | `backend/canvas-engine/src/main/resources/db/migration/V29__mq_trigger_configurable.sql` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java` |
| Create | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/MqTriggerHandlerTest.java` |

---

### Task 1: Flyway V28 — Fix SEND_MQ config_schema

The `api-input-params` field in SEND_MQ's schema is missing `apiKeyField` and `defsSource`, so the frontend control watches the wrong form field and loads from the wrong API endpoint.

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V28__fix_send_mq_schema.sql`

- [ ] **Step 1: Write the migration file**

```sql
-- V28: Fix SEND_MQ config_schema — api-input-params must specify apiKeyField + defsSource
-- Root cause: without these overrides the component defaults to apiKey + /meta/api-definitions,
-- so selecting a message type never populates the params section.
UPDATE node_type_registry
SET config_schema = '[
  {
    "key":        "messageCodeKey",
    "label":      "消息类型",
    "type":       "select",
    "dataSource": "/meta/mq-definitions",
    "required":   true
  },
  {
    "key":          "params",
    "label":        "消息参数",
    "type":         "api-input-params",
    "apiKeyField":  "messageCodeKey",
    "defsSource":   "/meta/mq-definitions",
    "required":     false
  }
]'
WHERE type_key = 'SEND_MQ';
```

- [ ] **Step 2: Verify Flyway picks it up (app start)**

```bash
cd backend/canvas-engine
./gradlew bootRun 2>&1 | grep -E "V28|Successfully applied|ERROR"
```

Expected output includes:
```
Successfully applied 1 migration to schema `canvas` (execution time ...) -> V28
```

- [ ] **Step 3: Verify schema in DB**

```sql
SELECT config_schema FROM node_type_registry WHERE type_key = 'SEND_MQ';
```

Expected: JSON contains `"apiKeyField": "messageCodeKey"` and `"defsSource": "/meta/mq-definitions"`.

- [ ] **Step 4: Manual browser test**

1. Start frontend (`cd frontend && npm run dev`)
2. Open a canvas with a SEND_MQ node (or create one)
3. Click the node to open config panel
4. Select any message type in "消息类型"
5. "消息参数" section should now populate with fields from that message definition

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V28__fix_send_mq_schema.sql
git commit -m "fix: SEND_MQ api-input-params now loads from mq-definitions (V28)"
```

---

### Task 2: Flyway V29 — MQ_TRIGGER configurable topics

Replace the 3 hardcoded topics in `MetaService.getMqTopics()` with data from `mq_message_definition`. Seed the original 3 topics into the table so existing canvases still work.

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V29__mq_trigger_configurable.sql`

- [ ] **Step 1: Write the migration file**

```sql
-- V29: MQ_TRIGGER topics now come from mq_message_definition table.
-- 1. Seed original 3 hardcoded topics (idempotent)
INSERT INTO mq_message_definition
  (name, message_code, topic, request_schema, description, enabled, created_by, created_at, updated_at)
VALUES
  ('机票订单状态变化',  'flight_order_status_change',  'flight_order_status_change',  '[]', '机票订单状态变更事件', 1, 'system', NOW(), NOW()),
  ('酒店订单状态变化',  'hotel_order_status_change',   'hotel_order_status_change',   '[]', '酒店订单状态变更事件', 1, 'system', NOW(), NOW()),
  ('火车票订单状态变化', 'train_order_status_change',  'train_order_status_change',   '[]', '火车票订单状态变更事件', 1, 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 2. Update MQ_TRIGGER config_schema to use /meta/mq-definitions
UPDATE node_type_registry
SET config_schema = '[
  {
    "key":        "messageCodeKey",
    "label":      "消息类型",
    "type":       "select",
    "dataSource": "/meta/mq-definitions",
    "required":   true
  },
  {
    "key":      "validateResult",
    "label":    "开启消息校验",
    "type":     "toggle"
  },
  {
    "key":      "validateRules",
    "label":    "校验规则",
    "type":     "condition-rule-list",
    "visible":  "validateResult==true"
  }
]'
WHERE type_key = 'MQ_TRIGGER';
```

- [ ] **Step 2: Apply and verify**

```bash
cd backend/canvas-engine
./gradlew bootRun 2>&1 | grep -E "V29|Successfully applied|ERROR"
```

Expected: `Successfully applied 1 migration to schema 'canvas' -> V29`

- [ ] **Step 3: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V29__mq_trigger_configurable.sql
git commit -m "feat: MQ_TRIGGER topics now loaded from mq_message_definition table (V29)"
```

---

### Task 3: MqTriggerHandler — look up topic from DB

The handler currently reads `config.topicKey` directly as the MQ topic string. After V29, we store `messageCodeKey` instead, and need to join to `mq_message_definition` to get the actual `topic`.

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/MqTriggerHandlerTest.java`

- [ ] **Step 1: Find current handler**

```bash
find backend -name "MqTriggerHandler.java" -exec grep -n "topicKey\|topic" {} \;
```

Note the line where `topicKey` is read from config.

- [ ] **Step 2: Write the failing test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/MqTriggerHandlerTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqTriggerHandlerTest {

    @Mock
    MqMessageDefinitionMapper mqMapper;

    @InjectMocks
    MqTriggerHandler handler;

    @Test
    void resolveTopic_from_messageCodeKey() {
        MqMessageDefinition def = new MqMessageDefinition();
        def.setTopic("flight_order_status_change");
        when(mqMapper.selectOne(any())).thenReturn(def);

        String topic = handler.resolveTopic(
            Map.of("messageCodeKey", "flight_order_status_change"));

        assertThat(topic).isEqualTo("flight_order_status_change");
    }

    @Test
    void resolveTopic_falls_back_to_topicKey_for_old_canvases() {
        when(mqMapper.selectOne(any())).thenReturn(null);

        String topic = handler.resolveTopic(
            Map.of("topicKey", "legacy_topic"));

        assertThat(topic).isEqualTo("legacy_topic");
    }
}
```

- [ ] **Step 3: Run test — expect compile failure (method doesn't exist yet)**

```bash
cd backend/canvas-engine
./gradlew test --tests "*.MqTriggerHandlerTest" 2>&1 | tail -20
```

Expected: compilation error mentioning `resolveTopic`.

- [ ] **Step 4: Add `resolveTopic` method to MqTriggerHandler**

Open the handler file and add the injected mapper + method. Replace the existing topic resolution logic:

```java
@Autowired
private MqMessageDefinitionMapper mqMessageDefinitionMapper;

/**
 * Resolves the actual MQ topic string from config.
 * Tries messageCodeKey first (new format), falls back to topicKey (legacy).
 */
public String resolveTopic(Map<String, Object> config) {
    String messageCode = (String) config.get("messageCodeKey");
    if (messageCode != null) {
        MqMessageDefinition def = mqMessageDefinitionMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MqMessageDefinition>()
                .eq(MqMessageDefinition::getMessageCode, messageCode)
                .eq(MqMessageDefinition::getEnabled, 1));
        if (def != null) return def.getTopic();
    }
    // Backward-compat: old canvases store topicKey directly
    return (String) config.getOrDefault("topicKey", "");
}
```

Also update wherever the handler previously read `config.get("topicKey")` directly to call `resolveTopic(config)` instead.

- [ ] **Step 5: Run test — expect PASS**

```bash
./gradlew test --tests "*.MqTriggerHandlerTest" 2>&1 | tail -10
```

Expected:
```
MqTriggerHandlerTest > resolveTopic_from_messageCodeKey() PASSED
MqTriggerHandlerTest > resolveTopic_falls_back_to_topicKey_for_old_canvases() PASSED
```

- [ ] **Step 6: Run full test suite**

```bash
./gradlew test 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`, no test failures.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/MqTriggerHandlerTest.java
git commit -m "feat: MqTriggerHandler resolves topic from mq_message_definition, fallback to legacy topicKey"
```

---

### Task 4: mq-config page — add usage hint

Tell users that configured MQ definitions are usable in both SEND_MQ and MQ_TRIGGER nodes.

**Files:**
- Modify: `frontend/src/pages/mq-config/index.tsx`

- [ ] **Step 1: Add hint after the page title**

Open `frontend/src/pages/mq-config/index.tsx`. After the `<Title>` element add:

```tsx
<Typography.Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 16 }}>
  在此配置好 MQ 消息类型后，可在画布中的「MQ 消息触发」节点和「发送 MQ」节点直接选用。
</Typography.Text>
```

- [ ] **Step 2: Verify in browser**

Navigate to the MQ 配置 page. Confirm the hint text appears below the title.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/mq-config/index.tsx
git commit -m "docs: add usage hint to mq-config page linking to canvas nodes"
```
