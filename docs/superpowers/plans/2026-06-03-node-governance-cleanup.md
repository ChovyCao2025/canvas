# Node Governance Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce the canvas node catalog to the approved governed set and remove unreleased duplicate or placeholder node implementations.

**Architecture:** The backend remains registry-driven, but the registry is seeded only with governed product nodes. `SPLIT` and `SEND_MESSAGE` replace multiple duplicated node families while reusing the existing weighted-choice and delivery-service foundations. The frontend derives node labels, categories, and branch handles from the same governed node type vocabulary.

**Tech Stack:** Java 21, Spring Boot, Reactor, Flyway SQL migrations, React, TypeScript, Vitest.

---

## File Structure

- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`: final node type constants.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SplitHandler.java`: generic split handler.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`: generic message send handler.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`: allow dynamic channel resolution.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`: remove deleted node and trigger references.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagParser.java`: remove deleted convergence node allowance.
- `backend/canvas-engine/src/main/resources/db/migration/*.sql`: remove deleted node registrations and demo references.
- `frontend/src/components/canvas/constants.ts`: final categories and labels.
- `frontend/src/components/canvas/branchHandles.ts`: replace old split-family handles with `SPLIT`.
- `frontend/src/components/node-panel/nodeLibrary.ts`: final common nodes and summary fallbacks.
- `frontend/src/pages/canvas-editor/insertNode.ts`: remove `TEMPLATE_NODE` expansion.
- Tests under matching `src/test` and `frontend/src` paths.

### Task 1: Governance Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`
- Modify: `frontend/src/components/canvas/constants.test.ts`
- Modify: `frontend/src/components/canvas/branchHandles.test.ts`

- [ ] **Step 1: Write backend governance test**

```java
package org.chovy.canvas.common.enums;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NodeTypeGovernanceTest {

    @Test
    void exposesOnlyGovernedProductNodeTypes() {
        Set<String> actual = Arrays.stream(NodeType.class.getDeclaredFields())
                .filter(field -> String.class.equals(field.getType()))
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(actual).containsExactlyInAnyOrder(
                "START", "END", "DIRECT_RETURN",
                "DIRECT_CALL", "EVENT_TRIGGER", "MQ_TRIGGER", "SCHEDULED_TRIGGER",
                "IF_CONDITION", "SPLIT",
                "WAIT", "HUB", "AGGREGATE", "THRESHOLD",
                "API_CALL", "SEND_MQ", "GROOVY",
                "SEND_MESSAGE",
                "TAGGER", "COMMIT_ACTION",
                "SUB_FLOW_REF"
        );
    }
}
```

- [ ] **Step 2: Update frontend tests**

```ts
it('exposes only governed default node names', () => {
  expect(Object.keys(DEFAULT_NAMES).sort()).toEqual([
    'AGGREGATE', 'API_CALL', 'COMMIT_ACTION', 'DIRECT_CALL', 'DIRECT_RETURN',
    'END', 'EVENT_TRIGGER', 'GROOVY', 'HUB', 'IF_CONDITION', 'MQ_TRIGGER',
    'SCHEDULED_TRIGGER', 'SEND_MESSAGE', 'SEND_MQ', 'SPLIT', 'START',
    'SUB_FLOW_REF', 'TAGGER', 'THRESHOLD', 'WAIT',
  ].sort())
})
```

```ts
it('SPLIT handles use configured branches and weights', () => {
  const handles = getBranchHandles('SPLIT', {
    branches: [
      { branchId: 'a', label: 'A组', weight: 30 },
      { branchId: 'b', label: 'B组', weight: 70 },
    ],
  })
  expect(handles.map(h => h.id)).toEqual(['branch-a', 'branch-b'])
  expect(handles.map(h => h.label)).toEqual(['A组 30%', 'B组 70%'])
})
```

- [ ] **Step 3: Run tests to verify RED**

Run:

```bash
cd backend && mvn -q -pl canvas-engine -Dtest=NodeTypeGovernanceTest test
cd frontend && npm install && npm test -- src/components/canvas/constants.test.ts src/components/canvas/branchHandles.test.ts
```

Expected: backend fails because deleted constants still exist and `SPLIT`/`SEND_MESSAGE` do not; frontend fails because old names and split handles still exist.

### Task 2: Backend Node Vocabulary and Handlers

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SplitHandler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Delete: handler files for removed node types.

- [ ] **Step 1: Replace `NodeType` with the governed constants from the design.**
- [ ] **Step 2: Implement `SplitHandler` using `WeightedChoice.choose(branches, userId + ":" + splitKey, stable)`.**
- [ ] **Step 3: Implement `SendMessageHandler` by resolving `channel` from config and delegating to `AbstractSendMessageHandler`.**
- [ ] **Step 4: Delete handler classes for removed node types.**
- [ ] **Step 5: Run backend governance and new handler tests.**

### Task 3: Backend Runtime References and Migrations

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagParser.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/TriggerType.java`
- Modify: `backend/canvas-engine/src/main/resources/db/migration/*.sql`

- [ ] **Step 1: Remove deleted node and trigger references from runtime code.**
- [ ] **Step 2: Edit node registry migrations so only governed node types are inserted.**
- [ ] **Step 3: Repair demo migrations that reference deleted nodes to use governed nodes.**
- [ ] **Step 4: Run `cd backend && mvn -q -pl canvas-engine -DskipTests compile`.**

### Task 4: Frontend Catalog and Branch Handles

**Files:**
- Modify: `frontend/src/components/canvas/constants.ts`
- Modify: `frontend/src/components/canvas/branchHandles.ts`
- Modify: `frontend/src/components/node-panel/nodeLibrary.ts`
- Modify: `frontend/src/pages/canvas-editor/insertNode.ts`
- Modify: related frontend tests.

- [ ] **Step 1: Replace categories, default names, and common nodes with the final catalog.**
- [ ] **Step 2: Replace old split-family handle logic with `SPLIT`.**
- [ ] **Step 3: Remove `TEMPLATE_NODE`, `CANVAS_TRIGGER`, `SELECTOR`, approval, and old send-node UI references.**
- [ ] **Step 4: Run targeted Vitest suites.**

### Task 5: Final Verification

**Files:**
- All files touched above.

- [ ] **Step 1: Search for deleted node identifiers in production code.**

Run:

```bash
rg "API_TRIGGER|BEHAVIOR_IN_APP|COUPON|POINTS_OPERATION|TAGGER_OFFLINE|TAGGER_REALTIME|IN_APP_NOTIFY|REACH_PLATFORM|SELECTOR|LOGIC_RELATION|MERGE|RANDOM_SPLIT|EXPERIMENT|GROUP|GOTO|TRANSFER_JOURNEY|SUBFLOW|TEMPLATE_NODE|AI_NEXT_BEST_ACTION|RECOMMENDATION|SCORING|TRACK_EVENT|CHANNEL_AVAILABILITY|FREQUENCY_CAP|QUIET_HOURS|SUPPRESSION_CHECK|DELAY|MANUAL_APPROVAL|CREATE_TASK|GOAL_CHECK|UPDATE_PROFILE|TAG_OPERATION|CDP_TAG_WRITE|CANVAS_TRIGGER|LOOP|SEND_EMAIL|SEND_SMS|SEND_PUSH|SEND_IN_APP|SEND_WECHAT|AUDIENCE_TRIGGER" backend/canvas-engine/src/main frontend/src
```

Expected: no production-code matches except non-node words unrelated to node identifiers.

- [ ] **Step 2: Run backend compile and targeted tests.**
- [ ] **Step 3: Run frontend build or targeted tests.**
- [ ] **Step 4: Review git diff for accidental unrelated changes.**

