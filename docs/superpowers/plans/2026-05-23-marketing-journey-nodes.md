# Full Marketing Journey Nodes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete marketing journey node system described in `docs/superpowers/specs/2026-05-23-marketing-journey-nodes-design.md`.

**Architecture:** Upgrade the runtime protocol first, then add nodes in layers: Wait/Goal, protection, delivery, entry/data actions, advanced decisioning, and controlled loop/navigation. Existing nodes remain compatible through adapters and legacy route mapping.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Redis, RocketMQ, React 18, Vite, TypeScript, Ant Design, React Flow, JUnit 5, AssertJ, Vitest.

---

## Scope Check

The approved spec spans seven subsystems:

1. Runtime protocol and graph route model.
2. Wait subscriptions and goal detection.
3. Marketing protection policies.
4. Delivery-channel product nodes.
5. Entry enhancements and data-action nodes.
6. Advanced decision nodes.
7. Controlled loop, jump, merge, transfer, and subflow nodes.

This plan keeps them in one sequenced program because the user requested the full system. Each phase ends with working software, tests, and a commit. Do not implement Phase 2 before Phase 1 passes because all later phases depend on `NodeOutcome`, `outlet_schema`, runtime policy handling, and route rendering.

## Pre-Flight

- [ ] **Step 1: Confirm workspace state**

Run:

```bash
git status --short
```

Expected: existing unrelated dirty files may be present. Only stage files touched by the current task.

- [ ] **Step 2: Confirm latest migration number**

Run:

```bash
ls backend/canvas-engine/src/main/resources/db/migration | sort -V | tail -5
```

Expected: the next migration after the current workspace is `V46__journey_node_runtime_protocol.sql`. If a newer migration appears, increment the migration number and keep the suffix descriptive.

- [ ] **Step 3: Run current targeted baselines**

Run:

```bash
cd frontend && npm test -- src/components/canvas/branchHandles.test.ts src/pages/canvas-editor/insertNode.test.ts
cd ../backend && mvn test -pl canvas-engine -Dtest=DagParserTest,IfConditionHandlerTest,AbSplitHandlerTest -q
```

Expected: tests pass before implementation. If a test fails from unrelated current workspace edits, record the failure in the task notes and avoid editing unrelated files.

---

## File Map

### Backend Runtime Foundation

| Action | File |
|---|---|
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeOutcome.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeRouteResolver.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagParser.java` |
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/NodeStatus.java` |
| Create | `backend/canvas-engine/src/main/resources/db/migration/V46__journey_node_runtime_protocol.sql` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeResultV2Test.java` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeRouteResolverTest.java` |

### Frontend Runtime Foundation

| Action | File |
|---|---|
| Create | `frontend/src/components/canvas/outletSchema.ts` |
| Modify | `frontend/src/components/canvas/branchHandles.ts` |
| Modify | `frontend/src/pages/canvas-editor/index.tsx` |
| Modify | `frontend/src/types/canvas.ts` |
| Test | `frontend/src/components/canvas/outletSchema.test.ts` |
| Test | `frontend/src/components/canvas/branchHandles.test.ts` |

### Wait and Goal

| Action | File |
|---|---|
| Create | `backend/canvas-engine/src/main/resources/db/migration/V47__wait_subscription_and_goal.sql` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasWaitSubscription.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasWaitSubscriptionMapper.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/WaitHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GoalCheckHandler.java` |
| Modify | `backend/canvas-engine/src/main/resources/db/migration/V2__seed_node_types.sql` through a new migration update, not by editing V2 |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/WaitHandlerTest.java` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/GoalCheckHandlerTest.java` |

### Protection and Frequency

| Action | File |
|---|---|
| Create | `backend/canvas-engine/src/main/resources/db/migration/V48__marketing_policy_tables.sql` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/customer/CustomerProfile.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/customer/CustomerChannel.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/customer/MarketingConsent.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/customer/MarketingSuppression.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SuppressionCheckHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/QuietHoursHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ChannelAvailabilityHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/FrequencyCapHandler.java` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/FrequencyCapHandlerTest.java` |

### Delivery Nodes

| Action | File |
|---|---|
| Create | `backend/canvas-engine/src/main/resources/db/migration/V49__message_send_record.sql` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/delivery/MessageSendRecord.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/delivery/MessageSendRecordMapper.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendEmailHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendSmsHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendPushHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendInAppHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendWechatHandler.java` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServiceTest.java` |
| Test | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendSmsHandlerTest.java` |

### Entry, Data Action, Decision, Structure

These files are introduced in Phases 5-7 after the common route and policy foundation is stable:

| Action | File |
|---|---|
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiTriggerHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AudienceTriggerHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UpdateProfileHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TagOperationHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PointsOperationHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CreateTaskHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TrackEventHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RandomSplitHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ExperimentHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ScoringHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RecommendationHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MergeHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/LoopHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GotoHandler.java` |
| Create | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TransferJourneyHandler.java` |

---

## Task 1: Runtime Protocol Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeOutcome.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeResultV2Test.java`

- [ ] **Step 1: Write failing NodeResult V2 tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeResultV2Test.java`:

```java
package org.chovy.canvas.engine.handler;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeResultV2Test {

    @Test
    void ok_uses_success_outcome_and_default_route() {
        NodeResult result = NodeResult.ok("next_node", Map.of("couponId", "c1"));

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "next_node");
        assertThat(result.output()).containsEntry("couponId", "c1");
        assertThat(result.success()).isTrue();
    }

    @Test
    void suppressed_routes_to_suppressed_branch_without_engine_failure() {
        NodeResult result = NodeResult.suppressed("suppressed_node", "UNSUBSCRIBED", "用户已退订");

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUPPRESSED);
        assertThat(result.routes()).containsEntry("suppressed", "suppressed_node");
        assertThat(result.reasonCode()).isEqualTo("UNSUBSCRIBED");
        assertThat(result.success()).isTrue();
    }

    @Test
    void timeout_routes_to_timeout_branch_without_engine_failure() {
        NodeResult result = NodeResult.timeout("timeout_node", "WAIT_TIMEOUT", "等待目标事件超时");

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
        assertThat(result.routes()).containsEntry("timeout", "timeout_node");
        assertThat(result.reasonMessage()).isEqualTo("等待目标事件超时");
        assertThat(result.success()).isTrue();
    }

    @Test
    void skipped_routes_to_skipped_branch() {
        NodeResult result = NodeResult.skipped("after_skip", "NODE_SKIPPED", "节点配置为跳过");

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SKIPPED);
        assertThat(result.routes()).containsEntry("skipped", "after_skip");
        assertThat(result.success()).isTrue();
    }
}
```

- [ ] **Step 2: Run the test and confirm failure**

Run:

```bash
cd backend && mvn test -pl canvas-engine -Dtest=NodeResultV2Test -q
```

Expected: FAIL because `NodeOutcome`, `routes()`, `suppressed()`, `timeout()`, `skipped()`, `reasonCode()`, and `reasonMessage()` do not exist.

- [ ] **Step 3: Add NodeOutcome enum**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeOutcome.java`:

```java
package org.chovy.canvas.engine.handler;

public enum NodeOutcome {
    SUCCESS,
    FAIL,
    TIMEOUT,
    SUPPRESSED,
    SKIPPED,
    PENDING
}
```

- [ ] **Step 4: Extend NodeResult with V2 fields and factories**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java` so the record signature becomes:

```java
public record NodeResult(
        String nextNodeId,
        String successNodeId,
        String failNodeId,
        String elseNodeId,
        Map<String, String> branchMap,
        Map<String, Object> output,
        boolean success,
        String errorMessage,
        boolean pending,
        NodeOutcome outcome,
        Map<String, String> routes,
        String reasonCode,
        String reasonMessage,
        Long resumeAtEpochMs
) {
```

Update each existing factory to set V2 fields. Add these factories:

```java
public static NodeResult suppressed(String suppressedNodeId, String reasonCode, String reasonMessage) {
    return new NodeResult(null, null, null, null, null, Map.of(),
            true, null, false, NodeOutcome.SUPPRESSED,
            Map.of("suppressed", suppressedNodeId), reasonCode, reasonMessage, null);
}

public static NodeResult timeout(String timeoutNodeId, String reasonCode, String reasonMessage) {
    return new NodeResult(null, null, null, null, null, Map.of(),
            true, null, false, NodeOutcome.TIMEOUT,
            Map.of("timeout", timeoutNodeId), reasonCode, reasonMessage, null);
}

public static NodeResult skipped(String skippedNodeId, String reasonCode, String reasonMessage) {
    return new NodeResult(null, null, null, null, null, Map.of(),
            true, null, false, NodeOutcome.SKIPPED,
            Map.of("skipped", skippedNodeId), reasonCode, reasonMessage, null);
}

public static NodeResult pending(Long resumeAtEpochMs, String reasonCode, String reasonMessage) {
    return new NodeResult(null, null, null, null, null, Map.of(),
            true, null, true, NodeOutcome.PENDING,
            Map.of(), reasonCode, reasonMessage, resumeAtEpochMs);
}
```

- [ ] **Step 5: Run NodeResult tests**

Run:

```bash
cd backend && mvn test -pl canvas-engine -Dtest=NodeResultV2Test -q
```

Expected: PASS.

- [ ] **Step 6: Run compatibility handler tests**

Run:

```bash
cd backend && mvn test -pl canvas-engine -Dtest=IfConditionHandlerTest,SelectorHandlerTest,AbSplitHandlerTest,DelayHandlerTest -q
```

Expected: PASS. If compilation fails, update `NodeResult` factory constructors only; do not edit handlers unless they construct `NodeResult` directly.

- [ ] **Step 7: Commit runtime result model**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeOutcome.java \
        backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeResultV2Test.java
git commit -m "feat: add node outcome result protocol"
```

---

## Task 2: Route Resolver and Engine Outcome Routing

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeRouteResolver.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/NodeStatus.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeRouteResolverTest.java`

- [ ] **Step 1: Write failing route resolver tests**

Create `NodeRouteResolverTest.java`:

```java
package org.chovy.canvas.engine.handler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeRouteResolverTest {

    @Test
    void resolves_v2_route_before_legacy_route() {
        NodeResult result = NodeResult.timeout("timeout_node", "WAIT_TIMEOUT", "timeout");

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("timeout_node");
    }

    @Test
    void resolves_legacy_default_next_route() {
        NodeResult result = NodeResult.ok("next_node", Map.of());

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("next_node");
    }

    @Test
    void resolves_legacy_branch_map_values() {
        NodeResult result = NodeResult.multiNext(Map.of("A", "node_a", "B", "node_b"), null);

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactlyInAnyOrder("node_a", "node_b");
    }

    @Test
    void ignores_blank_routes() {
        NodeResult result = NodeResult.multiNext(Map.of("A", "", "B", "node_b"), null);

        assertThat(NodeRouteResolver.resolveTargets(result)).containsExactly("node_b");
    }
}
```

- [ ] **Step 2: Run test and confirm failure**

Run:

```bash
cd backend && mvn test -pl canvas-engine -Dtest=NodeRouteResolverTest -q
```

Expected: FAIL because `NodeRouteResolver` does not exist.

- [ ] **Step 3: Add NodeRouteResolver**

Create `NodeRouteResolver.java`:

```java
package org.chovy.canvas.engine.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NodeRouteResolver {
    private NodeRouteResolver() {
    }

    public static List<String> resolveTargets(NodeResult result) {
        List<String> targets = new ArrayList<>();
        if (result.routes() != null && !result.routes().isEmpty()) {
            result.routes().values().forEach(target -> addIfPresent(targets, target));
            return targets;
        }
        addIfPresent(targets, result.nextNodeId());
        addIfPresent(targets, result.successNodeId());
        addIfPresent(targets, result.failNodeId());
        addIfPresent(targets, result.elseNodeId());
        if (result.branchMap() != null) {
            for (Map.Entry<String, String> entry : result.branchMap().entrySet()) {
                addIfPresent(targets, entry.getValue());
            }
        }
        return targets;
    }

    private static void addIfPresent(List<String> targets, String target) {
        if (target != null && !target.isBlank()) {
            targets.add(target);
        }
    }
}
```

- [ ] **Step 4: Add NodeStatus values**

Modify `NodeStatus.java` to include:

```java
TIMEOUT,
SUPPRESSED
```

Keep existing enum values intact.

- [ ] **Step 5: Route outcomes in DagEngine**

In `DagEngine`, change downstream routing to call `NodeRouteResolver.resolveTargets(result)` when `result.routes()` is non-empty. Map node status by outcome:

```java
private NodeStatus statusForOutcome(NodeOutcome outcome) {
    if (outcome == null) return NodeStatus.SUCCESS;
    return switch (outcome) {
        case FAIL -> NodeStatus.FAILED;
        case TIMEOUT -> NodeStatus.TIMEOUT;
        case SUPPRESSED -> NodeStatus.SUPPRESSED;
        case SKIPPED -> NodeStatus.SKIPPED;
        case PENDING -> NodeStatus.WAITING;
        case SUCCESS -> NodeStatus.SUCCESS;
    };
}
```

Use this helper in both ordinary execution path and `executeNodeAfterStage2`.

- [ ] **Step 6: Run route and engine tests**

Run:

```bash
cd backend && mvn test -pl canvas-engine -Dtest=NodeRouteResolverTest,DagParserTest,DagEngineDepthTest,SpecialNodeTraceDurationTest -q
```

Expected: PASS.

- [ ] **Step 7: Commit route resolver**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeRouteResolver.java \
        backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/NodeStatus.java \
        backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeRouteResolverTest.java
git commit -m "feat: route node outcomes through engine"
```

---

## Task 3: Node Registry Runtime Metadata

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V46__journey_node_runtime_protocol.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/NodeTypeRegistry.java`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/types/canvas.ts`

- [ ] **Step 1: Add Flyway migration**

Create `V46__journey_node_runtime_protocol.sql`:

```sql
ALTER TABLE node_type_registry
    ADD COLUMN outlet_schema TEXT NULL COMMENT '节点出口定义，驱动前端 handle 和发布校验',
    ADD COLUMN summary_template VARCHAR(500) NULL COMMENT '画布卡片摘要模板',
    ADD COLUMN runtime_policy_schema TEXT NULL COMMENT '节点通用运行策略配置 schema',
    ADD COLUMN risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT 'LOW/MEDIUM/HIGH';

ALTER TABLE canvas_execution_trace
    ADD COLUMN outcome VARCHAR(32) NULL COMMENT 'NodeOutcome 执行结果',
    ADD COLUMN reason_code VARCHAR(128) NULL COMMENT '节点结果原因码',
    ADD COLUMN reason_message VARCHAR(500) NULL COMMENT '节点结果说明',
    ADD COLUMN route_handle VARCHAR(64) NULL COMMENT '实际选择的出口 handle';
```

- [ ] **Step 2: Add fields to NodeTypeRegistry**

Add these fields to `NodeTypeRegistry.java`:

```java
private String outletSchema;
private String summaryTemplate;
private String runtimePolicySchema;
private String riskLevel;
```

- [ ] **Step 3: Add frontend type fields**

In `frontend/src/types/index.ts`, extend `NodeTypeRegistry` with:

```ts
outletSchema?: string
summaryTemplate?: string
runtimePolicySchema?: string
riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | string
```

In `frontend/src/types/canvas.ts`, extend `BizConfig` with:

```ts
runtimePolicy?: Record<string, unknown>
timeoutNodeId?: string
suppressedNodeId?: string
skippedNodeId?: string
maxExceededNodeId?: string
goalMetNodeId?: string
goalNotMetNodeId?: string
```

- [ ] **Step 4: Compile backend and frontend types**

Run:

```bash
cd backend && mvn compile -pl canvas-engine -q
cd ../frontend && npm run build
```

Expected: both commands pass.

- [ ] **Step 5: Commit registry metadata**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V46__journey_node_runtime_protocol.sql \
        backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/NodeTypeRegistry.java \
        frontend/src/types/index.ts \
        frontend/src/types/canvas.ts
git commit -m "feat: add node runtime metadata schema"
```

---

## Task 4: Outlet Schema Driven Frontend Handles

**Files:**
- Create: `frontend/src/components/canvas/outletSchema.ts`
- Create: `frontend/src/components/canvas/outletSchema.test.ts`
- Modify: `frontend/src/components/canvas/branchHandles.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Write outlet schema tests**

Create `outletSchema.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { getOutletHandles, parseOutletSchema } from './outletSchema'

describe('outlet schema', () => {
  it('parses dynamic outlet handles from registry json', () => {
    const schema = JSON.stringify([
      { id: 'success', label: '通过', color: '#52c41a' },
      { id: 'suppressed', label: '被抑制', color: '#f5222d' },
    ])

    expect(parseOutletSchema(schema)).toEqual([
      { id: 'success', label: '通过', color: '#52c41a' },
      { id: 'suppressed', label: '被抑制', color: '#f5222d' },
    ])
  })

  it('falls back to legacy IF handles', () => {
    expect(getOutletHandles({
      nodeType: 'IF_CONDITION',
      bizConfig: {},
      outletSchema: undefined,
    })).toEqual([
      { id: 'success', label: '条件成立', color: '#52c41a' },
      { id: 'else', label: '否则', color: '#8c8c8c' },
    ])
  })
})
```

- [ ] **Step 2: Run outlet tests and confirm failure**

Run:

```bash
cd frontend && npm test -- src/components/canvas/outletSchema.test.ts
```

Expected: FAIL because `outletSchema.ts` does not exist.

- [ ] **Step 3: Add outlet schema helper**

Create `outletSchema.ts`:

```ts
import { getBranchHandles, type BranchHandle } from './branchHandles'

export interface OutletSchemaItem {
  id: string
  label: string
  color?: string
}

export function parseOutletSchema(raw: string | undefined): BranchHandle[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as OutletSchemaItem[]
    return parsed
      .filter(item => item.id && item.label)
      .map(item => ({
        id: item.id,
        label: item.label,
        color: item.color ?? '#1677ff',
      }))
  } catch {
    return []
  }
}

export function getOutletHandles(input: {
  nodeType: string
  bizConfig: Record<string, unknown>
  outletSchema?: string
}): BranchHandle[] {
  const dynamic = parseOutletSchema(input.outletSchema)
  if (dynamic.length > 0) return dynamic
  return getBranchHandles(input.nodeType, input.bizConfig)
}
```

- [ ] **Step 4: Thread outletSchema into CanvasNodeData**

Add optional `outletSchema?: string` to `CanvasNodeData` in both canvas type files. When mapping backend nodes in the editor, populate `outletSchema` from node registry metadata if available. If the editor does not have registry metadata in memory, keep the field undefined and let legacy fallback handle existing nodes.

- [ ] **Step 5: Use getOutletHandles in editor insertion eligibility**

In `frontend/src/pages/canvas-editor/index.tsx`, replace direct `getBranchHandles(nodeType, defaultBizConfig)` calls with `getOutletHandles({ nodeType, bizConfig: defaultBizConfig, outletSchema: undefined })`.

- [ ] **Step 6: Run frontend tests**

Run:

```bash
cd frontend && npm test -- src/components/canvas/outletSchema.test.ts src/components/canvas/branchHandles.test.ts src/pages/canvas-editor/insertNode.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit outlet schema UI**

Run:

```bash
git add frontend/src/components/canvas/outletSchema.ts \
        frontend/src/components/canvas/outletSchema.test.ts \
        frontend/src/components/canvas/branchHandles.ts \
        frontend/src/pages/canvas-editor/index.tsx \
        frontend/src/types/canvas.ts
git commit -m "feat: render canvas handles from outlet schema"
```

---

## Phase 2: Wait and Goal Nodes

**Goal:** Implement `WAIT` and `GOAL_CHECK` as real runtime nodes with persisted wait subscriptions and timeout recovery.

### Task 5: Wait Subscription Storage

**Files:**
- Create `V47__wait_subscription_and_goal.sql`
- Create `CanvasWaitSubscription.java`
- Create `CanvasWaitSubscriptionMapper.java`
- Create `WaitSubscriptionService.java`
- Create `WaitSubscriptionServiceTest.java`

- [ ] **Step 1: Create migration with `canvas_wait_subscription` and indexes from the spec**
- [ ] **Step 2: Add MyBatis entity and mapper**
- [ ] **Step 3: Add service methods `createEventWait`, `expireWait`, `completeWait`, `findActiveEventWaits`**
- [ ] **Step 4: Test event wait creation, active lookup, completion, and expiration**
- [ ] **Step 5: Run `mvn test -pl canvas-engine -Dtest=WaitSubscriptionServiceTest -q`**
- [ ] **Step 6: Commit with `feat: add wait subscription storage`**

### Task 6: WAIT Node

**Files:**
- Create `WaitHandler.java`
- Create `WaitHandlerTest.java`
- Create migration registering `WAIT`
- Modify `NodeType.java`

- [ ] **Step 1: Add `NodeType.WAIT = "WAIT"`**
- [ ] **Step 2: Register `WAIT` with outlet handles `success` and `timeout`**
- [ ] **Step 3: Implement `DURATION`, `UNTIL_DATE`, `RELATIVE_TIME`, `TIME_WINDOW`, `UNTIL_EVENT` modes**
- [ ] **Step 4: Ensure `UNTIL_EVENT` writes a wait subscription and returns `NodeResult.pending(...)`**
- [ ] **Step 5: Test duration, time-window continuation, event wait pending, and timeout routing**
- [ ] **Step 6: Commit with `feat: add journey wait node`**

### Task 7: GOAL_CHECK Node

**Files:**
- Create `GoalCheckHandler.java`
- Create `GoalCheckHandlerTest.java`
- Modify event-report flow to wake goal subscriptions
- Create migration registering `GOAL_CHECK`
- Modify `NodeType.java`

- [ ] **Step 1: Add `NodeType.GOAL_CHECK = "GOAL_CHECK"`**
- [ ] **Step 2: Register outlets `goal_met`, `goal_not_met`, and `timeout`**
- [ ] **Step 3: Implement synchronous event-log lookup for journey-entry-to-now window**
- [ ] **Step 4: Implement async wait mode through `WaitSubscriptionService`**
- [ ] **Step 5: Test goal met, goal not met, async pending, and timeout**
- [ ] **Step 6: Commit with `feat: add goal check node`**

---

## Phase 3: Protection and Frequency Nodes

**Goal:** Implement user consent, suppression, quiet hours, channel availability, and frequency-cap routing.

### Task 8: Marketing Policy Tables and Service

**Files:**
- Create `V48__marketing_policy_tables.sql`
- Create customer and consent entities and mappers
- Create `MarketingPolicyService.java`
- Create `MarketingPolicyServiceTest.java`

- [ ] **Step 1: Add customer profile, channel, consent, suppression, and frequency counter tables**
- [ ] **Step 2: Implement profile timezone lookup with default `Asia/Shanghai`**
- [ ] **Step 3: Implement consent and suppression checks**
- [ ] **Step 4: Implement Redis-backed fixed-window frequency counter with rollback on rejection**
- [ ] **Step 5: Test consent allowed, consent rejected, suppression rejected, quiet-hours active, channel unavailable, and frequency exceeded**
- [ ] **Step 6: Commit with `feat: add marketing policy service`**

### Task 9: Protection Handlers

**Files:**
- Create `SuppressionCheckHandler.java`
- Create `QuietHoursHandler.java`
- Create `ChannelAvailabilityHandler.java`
- Create `FrequencyCapHandler.java`
- Create handler tests
- Create migration registering four node types
- Modify `NodeType.java`

- [ ] **Step 1: Add node constants**
- [ ] **Step 2: Register outlets for `allowed / suppressed`, `available / unavailable`, and `pass / capped`**
- [ ] **Step 3: Implement handlers using `MarketingPolicyService`**
- [ ] **Step 4: Return `NodeResult.suppressed(...)` for blocked paths**
- [ ] **Step 5: Run handler tests**
- [ ] **Step 6: Commit with `feat: add journey protection nodes`**

---

## Phase 4: Delivery Nodes

**Goal:** Productize delivery nodes while keeping existing `REACH_PLATFORM` compatible.

### Task 10: Delivery Service and Send Record

**Files:**
- Create `V49__message_send_record.sql`
- Create `MessageSendRecord.java`
- Create `MessageSendRecordMapper.java`
- Create `ReachDeliveryService.java`
- Create `ReachDeliveryServiceTest.java`

- [ ] **Step 1: Add send-record table with idempotency key unique index**
- [ ] **Step 2: Implement delivery request builder with channel, template, variables, user ID, and idempotency key**
- [ ] **Step 3: Persist `PENDING`, `SENT`, `FAILED`, and `SKIPPED` records**
- [ ] **Step 4: Mock WebClient and test successful send, failed send, and duplicate idempotency**
- [ ] **Step 5: Commit with `feat: add reach delivery service`**

### Task 11: Channel Send Handlers

**Files:**
- Create `SendEmailHandler.java`
- Create `SendSmsHandler.java`
- Create `SendPushHandler.java`
- Create `SendInAppHandler.java`
- Create `SendWechatHandler.java`
- Create handler tests
- Create migration registering five node types
- Modify `NodeType.java`

- [ ] **Step 1: Add `SEND_EMAIL`, `SEND_SMS`, `SEND_PUSH`, `SEND_IN_APP`, `SEND_WECHAT` constants**
- [ ] **Step 2: Register node schemas with template, variables, quiet-hours option, fallback, and failure branch**
- [ ] **Step 3: Implement handlers by delegating to `ReachDeliveryService`**
- [ ] **Step 4: Mark handlers as reach nodes with `isReachNode()`**
- [ ] **Step 5: Test each channel maps config into delivery request**
- [ ] **Step 6: Commit with `feat: add productized send nodes`**

---

## Phase 5: Entry and Data Action Nodes

**Goal:** Add productized API/Audience entry and data mutation actions.

### Task 12: Entry Enhancements

**Files:**
- Create `ApiTriggerHandler.java`
- Create `AudienceTriggerHandler.java`
- Modify `EventTriggerHandler.java`
- Modify `CanvasExecutionService.java`
- Modify `CanvasSchedulerService.java`
- Create migration registering `API_TRIGGER` and `AUDIENCE_TRIGGER`

- [ ] **Step 1: Add node constants**
- [ ] **Step 2: Implement API trigger as product wrapper over direct-call trigger**
- [ ] **Step 3: Add event trigger filters and user filters before routing to next node**
- [ ] **Step 4: Implement audience enter/exit trigger using audience stat diffs**
- [ ] **Step 5: Test reentry, filter rejection, and audience enter trigger**
- [ ] **Step 6: Commit with `feat: add productized journey entry nodes`**

### Task 13: Data Action Nodes

**Files:**
- Create `UpdateProfileHandler.java`
- Create `TagOperationHandler.java`
- Create `PointsOperationHandler.java`
- Create `CreateTaskHandler.java`
- Create `TrackEventHandler.java`
- Create migration registering five node types

- [ ] **Step 1: Add node constants**
- [ ] **Step 2: Implement profile update operations `SET`, `SET_IF_NULL`, `INCREMENT`, `DECREMENT`, `APPEND`, `REMOVE`, `CLEAR`**
- [ ] **Step 3: Implement tag add/remove with TTL metadata**
- [ ] **Step 4: Implement points operation with mandatory idempotency key**
- [ ] **Step 5: Implement task creation record and track-event write-through**
- [ ] **Step 6: Test each handler with context-value resolution**
- [ ] **Step 7: Commit with `feat: add journey data action nodes`**

---

## Phase 6: Advanced Decision Nodes

**Goal:** Add random split, experiment, scoring, recommendation, and AI next-best-action decision nodes.

### Task 14: Split and Experiment

**Files:**
- Create `RandomSplitHandler.java`
- Create `ExperimentHandler.java`
- Create handler tests
- Create migration registering `RANDOM_SPLIT` and `EXPERIMENT`

- [ ] **Step 1: Add node constants**
- [ ] **Step 2: Implement weighted random and stable-hash random split**
- [ ] **Step 3: Implement experiment assignment with weights, control group, and metric config output**
- [ ] **Step 4: Test deterministic assignment and weight boundaries**
- [ ] **Step 5: Commit with `feat: add split and experiment nodes`**

### Task 15: Scoring and Recommendation

**Files:**
- Create `ScoringHandler.java`
- Create `RecommendationHandler.java`
- Create `AiNextBestActionHandler.java`
- Create tests
- Create migration registering `SCORING`, `RECOMMENDATION`, `AI_NEXT_BEST_ACTION`

- [ ] **Step 1: Add node constants**
- [ ] **Step 2: Implement rule-based scoring and band output**
- [ ] **Step 3: Implement recommendation adapter with fallback items**
- [ ] **Step 4: Implement AI next-best-action adapter with fallback branch**
- [ ] **Step 5: Test score bands, recommendation fallback, and AI failure fallback**
- [ ] **Step 6: Commit with `feat: add scoring and recommendation nodes`**

---

## Phase 7: Structure, Loop, and Transfer

**Goal:** Add merge, loop, goto, transfer journey, subflow product wrapper, group, and template nodes.

### Task 16: Controlled Back-Edges

**Files:**
- Modify `DagParser.java`
- Modify `ExecutionContext.java`
- Create `LoopHandler.java`
- Create `GotoHandler.java`
- Create tests
- Create migration registering `LOOP` and `GOTO`

- [ ] **Step 1: Add visit-count and jump-count maps to `ExecutionContext`**
- [ ] **Step 2: Parse normal edges and controlled back-edges separately**
- [ ] **Step 3: Reject loop/goto configs without `maxIterations` or `maxJumps` during publish**
- [ ] **Step 4: Implement loop exit condition and max-exceeded route**
- [ ] **Step 5: Implement goto target route with jump-count enforcement**
- [ ] **Step 6: Test bounded loop, max exceeded, valid goto, and invalid unbounded goto**
- [ ] **Step 7: Commit with `feat: add controlled loop and goto nodes`**

### Task 17: Merge, Transfer, Subflow, Group, Template

**Files:**
- Create `MergeHandler.java`
- Create `TransferJourneyHandler.java`
- Create `SubflowHandler.java`
- Modify frontend node library behavior for `GROUP` and `TEMPLATE_NODE`
- Create migration registering `MERGE`, `TRANSFER_JOURNEY`, `SUBFLOW`, `GROUP`, `TEMPLATE_NODE`

- [ ] **Step 1: Add node constants**
- [ ] **Step 2: Implement `MERGE` by delegating to existing hub/aggregate semantics**
- [ ] **Step 3: Implement `TRANSFER_JOURNEY` through `CanvasExecutionService.trigger` with context mapping**
- [ ] **Step 4: Implement `SUBFLOW` wrapper over existing sub-flow behavior**
- [ ] **Step 5: Make `GROUP` a UI-only node ignored by `DagParser.extractTargets` and engine execution**
- [ ] **Step 6: Make `TEMPLATE_NODE` expand into real nodes on insertion**
- [ ] **Step 7: Test merge routing, transfer trigger, subflow mapping, group persistence, and template expansion**
- [ ] **Step 8: Commit with `feat: add journey structure nodes`**

---

## Final Verification

- [ ] **Step 1: Run backend focused suite**

```bash
cd backend && mvn test -pl canvas-engine -Dtest='*HandlerTest,*ServiceTest,DagParserTest,DagEngineDepthTest,CanvasExecutionServiceTest' -q
```

Expected: PASS.

- [ ] **Step 2: Run frontend focused suite**

```bash
cd frontend && npm test -- src/components/canvas src/components/config-panel src/pages/canvas-editor
```

Expected: PASS.

- [ ] **Step 3: Build frontend**

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 4: Compile backend**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: PASS.

- [ ] **Step 5: Manual smoke path**

Start dependencies, backend, and frontend. Create a journey:

```text
START -> EVENT_TRIGGER -> WAIT(UNTIL_EVENT, timeout) -> GOAL_CHECK -> SUPPRESSION_CHECK -> CHANNEL_AVAILABILITY -> FREQUENCY_CAP -> SEND_SMS -> END
```

Expected:

- Publish succeeds.
- Dry run shows configured route handles.
- Event report wakes the wait subscription.
- Suppressed users route to suppressed branch.
- Frequency-capped users route to capped branch.
- SMS node writes a send record.

## Commit Discipline

Each task commits only its files. Existing unrelated dirty files remain untouched. If a task cannot commit due to pre-existing conflicts, finish the code and tests, then report the blocked commit with exact `git status --short` output.
