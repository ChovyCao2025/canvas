# API Context Payload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an API definition switch that controls whether API_CALL sends existing-product journey environment fields, and show users the exact request JSON preview in API configuration.

**Architecture:** Store the switch on `api_definition`, generate the UI preview from a small frontend helper, and generate the runtime API_CALL body through a focused backend payload builder. API_CALL always sends a one-element JSON array whose object contains `params`, plus environment blocks when the switch is enabled.

**Tech Stack:** React 18, Ant Design, Vitest, Spring Boot WebFlux, MyBatis Plus, Flyway, JUnit 5, AssertJ.

---

## Pre-Flight

**Current workspace note:** If `git status --short` shows `UU`, commits and full backend tests are blocked by unrelated unresolved conflicts. Do not edit conflict files unless this feature requires them. In this workspace, avoid `CanvasExecutionService.java` and `CanvasExecutionServiceTest.java`.

- [ ] **Step 1: Confirm unmerged state before execution**

Run:

```bash
git status --short
```

Expected in a clean execution workspace: no `UU` entries. If `UU` entries exist, continue implementation without committing and report that commits/full Maven verification are blocked by pre-existing conflicts.

---

### Task 1: Frontend Request Preview Helper

**Files:**
- Create: `frontend/src/pages/api-config/requestPreview.ts`
- Create: `frontend/src/pages/api-config/requestPreview.test.ts`

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/pages/api-config/requestPreview.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  buildApiRequestPreview,
  formatApiRequestPreview,
  normalizeApiDefinitionPayload,
} from './requestPreview'

describe('api request preview', () => {
  it('wraps configured parameters under params when environment info is disabled', () => {
    const preview = buildApiRequestPreview({
      requestSchema: [
        { name: 'define_item1', displayName: '优惠券ID', type: 'STRING', required: true },
        { name: 'define_item2', displayName: '优惠券内容示例', type: 'TEXT', required: false },
      ],
      includeContextPayload: false,
    })

    expect(preview).toEqual([
      {
        params: {
          define_item1: '优惠券ID',
          define_item2: '优惠券内容示例',
        },
      },
    ])
  })

  it('adds user_profile callback_params and process_info when environment info is enabled', () => {
    const preview = buildApiRequestPreview({
      requestSchema: [
        { name: 'define_item1', displayName: '优惠券ID', type: 'STRING', required: true },
      ],
      includeContextPayload: true,
    })

    expect(preview).toEqual([
      {
        user_profile: {
          target_type: 'OPEN_ID',
          target_id: '1917810',
          customer_id: '1917810',
        },
        params: {
          define_item1: '优惠券ID',
        },
        callback_params: {
          webhook_id: '',
          send_time: '1625037472000',
          nodeId: '节点Id',
          instanceId: '实例Id',
          batchId: '执行动作批次Id，可做批次幂等ID',
          actionId: '执行动作实例Id，可做单条幂等ID',
          customerId: '用户Id，customerId',
        },
        process_info: {
          processInstanceId: '新版：旅程周期中，每个用户的旅程实例ID',
          processInstanceStartTime: '新版：旅程周期中，每个用户的旅程实例开始时间，时间戳格式',
          processNodeInstanceId: '新版：旅程节点实例ID（每次不同）',
          processNodeInstanceStartTime: '新版：旅程周期中，每个用户的旅程的节点实例开始时间，时间戳格式',
          groupName: 'nodeId:nodeName:groupResult(node.result),groupName(node.resultExt)',
        },
      },
    ])
  })

  it('formats preview as pretty JSON', () => {
    expect(formatApiRequestPreview([{ params: { couponId: '优惠券ID' } }])).toBe(
      '[\n  {\n    "params": {\n      "couponId": "优惠券ID"\n    }\n  }\n]',
    )
  })

  it('normalizes API definition form values for submission', () => {
    const body = normalizeApiDefinitionPayload({
      name: '发券接口',
      enabled: true,
      includeContextPayload: true,
      requestSchema: [{ name: 'couponId', displayName: '优惠券ID', type: 'STRING', required: true }],
    })

    expect(body).toEqual({
      name: '发券接口',
      enabled: 1,
      includeContextPayload: 1,
      requestSchema: '[{"name":"couponId","displayName":"优惠券ID","type":"STRING","required":true}]',
    })
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
cd frontend && npm test -- src/pages/api-config/requestPreview.test.ts
```

Expected: FAIL because `requestPreview.ts` does not exist.

- [ ] **Step 3: Add the helper implementation**

Create `frontend/src/pages/api-config/requestPreview.ts`:

```ts
import type { ApiParam } from './index'

export interface BuildApiRequestPreviewInput {
  requestSchema?: ApiParam[]
  includeContextPayload?: boolean
}

export interface ApiDefinitionFormValues {
  requestSchema?: ApiParam[]
  enabled?: boolean
  includeContextPayload?: boolean
  [key: string]: unknown
}

const CONTEXT_PREVIEW = {
  user_profile: {
    target_type: 'OPEN_ID',
    target_id: '1917810',
    customer_id: '1917810',
  },
  callback_params: {
    webhook_id: '',
    send_time: '1625037472000',
    nodeId: '节点Id',
    instanceId: '实例Id',
    batchId: '执行动作批次Id，可做批次幂等ID',
    actionId: '执行动作实例Id，可做单条幂等ID',
    customerId: '用户Id，customerId',
  },
  process_info: {
    processInstanceId: '新版：旅程周期中，每个用户的旅程实例ID',
    processInstanceStartTime: '新版：旅程周期中，每个用户的旅程实例开始时间，时间戳格式',
    processNodeInstanceId: '新版：旅程节点实例ID（每次不同）',
    processNodeInstanceStartTime: '新版：旅程周期中，每个用户的旅程的节点实例开始时间，时间戳格式',
    groupName: 'nodeId:nodeName:groupResult(node.result),groupName(node.resultExt)',
  },
}

export function buildApiRequestPreview(input: BuildApiRequestPreviewInput): unknown[] {
  const params = Object.fromEntries(
    (input.requestSchema ?? [])
      .filter(param => param.name?.trim())
      .map(param => [param.name.trim(), param.displayName?.trim() || param.name.trim()]),
  )

  const item = input.includeContextPayload
    ? {
        user_profile: CONTEXT_PREVIEW.user_profile,
        params,
        callback_params: CONTEXT_PREVIEW.callback_params,
        process_info: CONTEXT_PREVIEW.process_info,
      }
    : { params }

  return [item]
}

export function formatApiRequestPreview(preview: unknown): string {
  return JSON.stringify(preview, null, 2)
}

export function normalizeApiDefinitionPayload(values: ApiDefinitionFormValues): Record<string, unknown> {
  return {
    ...values,
    enabled: values.enabled ? 1 : 0,
    includeContextPayload: values.includeContextPayload ? 1 : 0,
    requestSchema: JSON.stringify(values.requestSchema ?? []),
  }
}
```

- [ ] **Step 4: Run the helper test to verify it passes**

Run:

```bash
cd frontend && npm test -- src/pages/api-config/requestPreview.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit if the workspace has no unmerged paths**

Run:

```bash
git status --short
```

If there are no `UU` entries:

```bash
git add frontend/src/pages/api-config/requestPreview.ts frontend/src/pages/api-config/requestPreview.test.ts
git commit -m "test: add api request payload preview helper"
```

If there are `UU` entries, do not commit; note that commits are blocked by pre-existing conflicts.

---

### Task 2: API Configuration Page UI

**Files:**
- Modify: `frontend/src/pages/api-config/index.tsx`

- [ ] **Step 1: Import preview helpers and extend the API definition type**

In `frontend/src/pages/api-config/index.tsx`, update imports:

```ts
import {
  buildApiRequestPreview,
  formatApiRequestPreview,
  normalizeApiDefinitionPayload,
} from './requestPreview'
```

Extend `ApiDefinition`:

```ts
interface ApiDefinition {
  id: number
  name: string
  apiKey: string
  url: string
  method: string
  bizLine?: string
  description?: string
  requestSchema?: string
  includeContextPayload?: number
  enabled: number
}
```

- [ ] **Step 2: Add watched values for live preview**

Inside `ApiConfigPage`, after `const [submitting, setSubmitting] = useState(false)`, add:

```ts
  const requestSchemaPreview = Form.useWatch('requestSchema', form) as ApiParam[] | undefined
  const includeContextPayloadPreview = Form.useWatch('includeContextPayload', form) as boolean | undefined
  const requestPreviewJson = formatApiRequestPreview(buildApiRequestPreview({
    requestSchema: requestSchemaPreview,
    includeContextPayload: !!includeContextPayloadPreview,
  }))
```

- [ ] **Step 3: Set defaults when creating and editing**

Change `openCreate`:

```ts
    form.setFieldsValue({
      method: 'POST',
      enabled: true,
      includeContextPayload: false,
      requestSchema: [],
    })
```

Change `openEdit` form values:

```ts
    form.setFieldsValue({
      ...record,
      enabled: record.enabled === 1,
      includeContextPayload: record.includeContextPayload === 1,
      requestSchema: schema,
    })
```

- [ ] **Step 4: Use the payload normalizer on submit**

Replace the `body` construction in `handleOk` with:

```ts
      const body = normalizeApiDefinitionPayload(values)
```

- [ ] **Step 5: Add a list column for the switch**

Add this column before `状态`:

```tsx
    {
      title: '环境信息',
      dataIndex: 'includeContextPayload',
      width: 92,
      render: (v: number) => <Tag color={v === 1 ? 'blue' : 'default'}>{v === 1 ? '携带' : '不携带'}</Tag>,
    },
```

- [ ] **Step 6: Add the form switch and JSON preview**

After the `enabled` form item, add:

```tsx
            <Form.Item name="includeContextPayload" label="环境信息" valuePropName="checked">
              <Switch checkedChildren="携带" unCheckedChildren="不携带" />
            </Form.Item>
```

After the request schema editor form item, add:

```tsx
            <Divider style={{ margin: '16px 0 12px' }}>请求示例 JSON</Divider>
            <Input.TextArea
              value={requestPreviewJson}
              readOnly
              autoSize={{ minRows: 10, maxRows: 18 }}
              style={{ fontFamily: 'SFMono-Regular, Consolas, monospace', fontSize: 12 }}
            />
```

- [ ] **Step 7: Run frontend tests and build**

Run:

```bash
cd frontend && npm test -- src/pages/api-config/requestPreview.test.ts
cd frontend && npm run build
```

Expected: both commands exit 0.

- [ ] **Step 8: Commit if the workspace has no unmerged paths**

If there are no `UU` entries:

```bash
git add frontend/src/pages/api-config/index.tsx
git commit -m "feat: show api request payload preview"
```

If there are `UU` entries, do not commit; note that commits are blocked by pre-existing conflicts.

---

### Task 3: API Definition Persistence Field

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V44__api_context_payload_flag.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinition.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ApiDefinitionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`

- [ ] **Step 1: Add Flyway migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V44__api_context_payload_flag.sql`:

```sql
-- V44: API definitions can request journey environment payload blocks.

ALTER TABLE `api_definition`
  ADD COLUMN `include_context_payload` TINYINT NOT NULL DEFAULT 0
  COMMENT '是否携带旅程环境信息，1=携带，0=不携带'
  AFTER `response_schema`;
```

- [ ] **Step 2: Add the domain property**

In `ApiDefinition.java`, after `private String responseSchema;`, add:

```java
    /** 是否携带旅程环境信息，1=携带，0=不携带 */
    private Integer includeContextPayload;
```

- [ ] **Step 3: Default null switch values on create**

In `ApiDefinitionController.create`, after enabled defaulting, add:

```java
            if (body.getIncludeContextPayload() == null) body.setIncludeContextPayload(0);
```

- [ ] **Step 4: Expose the flag in metadata**

In `MetaController.getApiDefinitions`, add this map entry after `requestSchema`:

```java
                m.put("includeContextPayload", def.getIncludeContextPayload() != null ? def.getIncludeContextPayload() : 0);
```

- [ ] **Step 5: Run compile when conflicts are resolved**

Run:

```bash
cd backend && mvn -pl canvas-engine -DskipTests compile
```

Expected in a clean workspace: exit 0. If unresolved `UU` files remain, this command may fail before reaching this feature.

- [ ] **Step 6: Commit if the workspace has no unmerged paths**

If there are no `UU` entries:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V44__api_context_payload_flag.sql backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ApiDefinitionController.java backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java
git commit -m "feat: persist api context payload flag"
```

If there are `UU` entries, do not commit.

---

### Task 4: Backend API Call Payload Builder

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilder.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilderTest.java`

- [ ] **Step 1: Write the failing backend unit tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilderTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCallPayloadBuilderTest {

    @Test
    void wraps_params_without_context_payload() {
        ApiCallPayloadBuilder builder = new ApiCallPayloadBuilder(() -> 1625037472000L);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("1917810");
        ctx.setExecutionId("exec-1");

        List<Map<String, Object>> payload = builder.build(
                Map.of("define_item1", "优惠券ID"),
                ctx,
                "api-node",
                false
        );

        assertThat(payload).containsExactly(Map.of(
                "params", Map.of("define_item1", "优惠券ID")
        ));
    }

    @Test
    void adds_environment_blocks_when_context_payload_is_enabled() {
        ApiCallPayloadBuilder builder = new ApiCallPayloadBuilder(() -> 1625037472000L);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("1917810");
        ctx.setExecutionId("exec-1");

        List<Map<String, Object>> payload = builder.build(
                Map.of("define_item1", "优惠券ID"),
                ctx,
                "api-node",
                true
        );

        assertThat(payload).hasSize(1);
        Map<String, Object> item = payload.getFirst();
        assertThat(item.get("user_profile")).isEqualTo(Map.of(
                "target_type", "OPEN_ID",
                "target_id", "1917810",
                "customer_id", "1917810"
        ));
        assertThat(item.get("params")).isEqualTo(Map.of("define_item1", "优惠券ID"));
        assertThat(item.get("callback_params")).isEqualTo(Map.of(
                "webhook_id", "",
                "send_time", "1625037472000",
                "nodeId", "api-node",
                "instanceId", "exec-1",
                "batchId", "exec-1",
                "actionId", "exec-1:api-node",
                "customerId", "1917810"
        ));
        assertThat(item.get("process_info")).isEqualTo(Map.of(
                "processInstanceId", "exec-1",
                "processInstanceStartTime", "1625037472000",
                "processNodeInstanceId", "exec-1:api-node",
                "processNodeInstanceStartTime", "1625037472000",
                "groupName", ""
        ));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ApiCallPayloadBuilderTest test
```

Expected: FAIL because `ApiCallPayloadBuilder` does not exist. If unresolved conflict files prevent Maven from parsing sources, record that the red run is blocked by pre-existing conflicts.

- [ ] **Step 3: Add the payload builder**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilder.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

@Component
public class ApiCallPayloadBuilder {

    private final LongSupplier nowMillis;

    public ApiCallPayloadBuilder() {
        this(System::currentTimeMillis);
    }

    ApiCallPayloadBuilder(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    public List<Map<String, Object>> build(Map<String, Object> params,
                                           ExecutionContext ctx,
                                           String nodeId,
                                           boolean includeContextPayload) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (includeContextPayload) {
            item.put("user_profile", userProfile(ctx));
        }
        item.put("params", new LinkedHashMap<>(params));
        if (includeContextPayload) {
            item.put("callback_params", callbackParams(ctx, nodeId));
            item.put("process_info", processInfo(ctx, nodeId));
        }
        return List.of(item);
    }

    private Map<String, Object> userProfile(ExecutionContext ctx) {
        String userId = value(ctx != null ? ctx.getUserId() : null);
        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put("target_type", "OPEN_ID");
        userProfile.put("target_id", userId);
        userProfile.put("customer_id", userId);
        return userProfile;
    }

    private Map<String, Object> callbackParams(ExecutionContext ctx, String nodeId) {
        String executionId = value(ctx != null ? ctx.getExecutionId() : null);
        String currentNodeId = value(nodeId);
        String userId = value(ctx != null ? ctx.getUserId() : null);
        String now = String.valueOf(nowMillis.getAsLong());

        Map<String, Object> callbackParams = new LinkedHashMap<>();
        callbackParams.put("webhook_id", "");
        callbackParams.put("send_time", now);
        callbackParams.put("nodeId", currentNodeId);
        callbackParams.put("instanceId", executionId);
        callbackParams.put("batchId", executionId);
        callbackParams.put("actionId", executionId + ":" + currentNodeId);
        callbackParams.put("customerId", userId);
        return callbackParams;
    }

    private Map<String, Object> processInfo(ExecutionContext ctx, String nodeId) {
        String executionId = value(ctx != null ? ctx.getExecutionId() : null);
        String currentNodeId = value(nodeId);
        String now = String.valueOf(nowMillis.getAsLong());

        Map<String, Object> processInfo = new LinkedHashMap<>();
        processInfo.put("processInstanceId", executionId);
        processInfo.put("processInstanceStartTime", now);
        processInfo.put("processNodeInstanceId", executionId + ":" + currentNodeId);
        processInfo.put("processNodeInstanceStartTime", now);
        processInfo.put("groupName", "");
        return processInfo;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
```

- [ ] **Step 4: Run the backend builder tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ApiCallPayloadBuilderTest test
```

Expected in a clean workspace: PASS.

- [ ] **Step 5: Commit if the workspace has no unmerged paths**

If there are no `UU` entries:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilder.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilderTest.java
git commit -m "test: add api call context payload builder"
```

If there are `UU` entries, do not commit.

---

### Task 5: API_CALL Runtime Integration

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`

- [ ] **Step 1: Inject the payload builder into ApiCallHandler**

In `ApiCallHandler`, add the final field:

```java
    private final ApiCallPayloadBuilder payloadBuilder;
```

- [ ] **Step 2: Build the new request body format**

Replace:

```java
        log.info("[API_CALL] → {} {} body={}", method, url, reqBody);
```

with:

```java
        boolean includeContextPayload = Integer.valueOf(1).equals(def.getIncludeContextPayload());
        String currentNodeId = (String) config.getOrDefault("__nodeId", "");
        List<Map<String, Object>> requestBody = payloadBuilder.build(
                reqBody, ctx, currentNodeId, includeContextPayload);
        log.info("[API_CALL] → {} {} body={}", method, url, requestBody);
```

Replace POST body:

```java
                .bodyValue(reqBody)
```

with:

```java
                .bodyValue(requestBody)
```

- [ ] **Step 3: Inject nodeId into API_CALL config**

In `DagEngine`, replace:

```java
            Map<String, Object> config = NodeType.MANUAL_APPROVAL.equals(node.getType())
                    ? resolveConfigWithNodeId(rawConfig, ctx, nodeId)
                    : resolveConfig(rawConfig, ctx);
```

with:

```java
            boolean needsNodeId = NodeType.MANUAL_APPROVAL.equals(node.getType())
                    || NodeType.API_CALL.equals(node.getType());
            Map<String, Object> config = needsNodeId
                    ? resolveConfigWithNodeId(rawConfig, ctx, nodeId)
                    : resolveConfig(rawConfig, ctx);
```

- [ ] **Step 4: Run focused backend tests when conflicts are resolved**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ApiCallPayloadBuilderTest test
cd backend && mvn -pl canvas-engine -DskipTests compile
```

Expected in a clean workspace: both commands exit 0.

- [ ] **Step 5: Commit if the workspace has no unmerged paths**

If there are no `UU` entries:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallHandler.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java
git commit -m "feat: send api calls with context payload wrapper"
```

If there are `UU` entries, do not commit.

---

### Task 6: Final Verification

**Files:**
- Verify all files changed by Tasks 1-5.

- [ ] **Step 1: Run frontend verification**

Run:

```bash
cd frontend && npm test -- src/pages/api-config/requestPreview.test.ts
cd frontend && npm run build
```

Expected: both commands exit 0.

- [ ] **Step 2: Run backend verification when conflict-free**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ApiCallPayloadBuilderTest test
cd backend && mvn -pl canvas-engine -DskipTests compile
```

Expected in a clean workspace: both commands exit 0. If existing unrelated `UU` files remain, report backend verification as blocked by unresolved conflicts.

- [ ] **Step 3: Inspect diff**

Run:

```bash
git diff -- frontend/src/pages/api-config backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ApiDefinitionController.java backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallHandler.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilder.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/main/resources/db/migration/V44__api_context_payload_flag.sql
```

Expected: diff only includes API context payload changes.
