# AI Capability Policy And Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an internal AI capability policy and governance test protection so unfinished AI nodes remain hidden unless governed runtime artifacts exist.

**Architecture:** Treat this as a documentation and test guard slice. The policy describes visibility rules; `NodeTypeGovernanceTest` enforces that public node constants do not expose unfinished AI capabilities and that `AI_LLM` visibility is backed by P2-019 runtime artifacts when present.

**Implementation note:** The current codebase already contains `AI_LLM`, `AiLlmHandler`, and `AiLlmGateway`, so this slice permits governed `AI_LLM` and blocks legacy `AI_NEXT_BEST_ACTION` rather than removing the productionized AI node.

**Tech Stack:** Markdown, Java 21, JUnit 5, AssertJ.

**Implementation Status:** Implemented and focused-verified on 2026-06-05. The original "hide `AI_LLM` until P2-019" assumption is superseded by the current codebase, where P2-019 runtime artifacts exist; governance now permits `AI_LLM` only with backing runtime, audit, tenant, and UI controls, and continues to block `AI_NEXT_BEST_ACTION`.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003g-ai-capability-policy-and-governance.md`
- Related future implementation: `docs/product-evolution/specs/p2-019-ai-llm-node-productionization.md`

## File Structure

- Create: `docs/product-evolution/AI_CAPABILITY_POLICY.md`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`

### Task 1: Governance Test

**Files:**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`

- [x] **Step 1: Add AI visibility assertion**

Add an AI visibility test. In the current implementation this verifies that `AI_NEXT_BEST_ACTION` is absent and that public `AI_LLM` has P2-019 runtime and policy artifacts:

```java
@Test
void aiCapabilityConstantsFollowVisibilityPolicy() {
    Set<String> values = Arrays.stream(NodeType.class.getDeclaredFields())
            .filter(field -> String.class.equals(field.getType()))
            .map(field -> {
                try {
                    return (String) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            })
            .collect(Collectors.toSet());

    assertThat(values).doesNotContain("AI_NEXT_BEST_ACTION");
    if (values.contains("AI_LLM")) {
        assertThat(AiLlmHandler.class.getName()).isEqualTo("org.chovy.canvas.engine.handlers.AiLlmHandler");
        assertThat(AiLlmGateway.class.getName()).isEqualTo("org.chovy.canvas.engine.llm.AiLlmGateway");
        assertThat(Files.readString(aiPolicyPath())).contains("P2-019", "AI_LLM", "AI_NEXT_BEST_ACTION");
    }
}
```

Add imports if absent:

```java
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
```

- [x] **Step 2: Run governance test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeTypeGovernanceTest
```

Expected: PASS if unfinished AI node constants remain hidden.

### Task 2: Policy Document

**Files:**
- Create: `docs/product-evolution/AI_CAPABILITY_POLICY.md`

- [x] **Step 1: Add policy document**

Create `AI_CAPABILITY_POLICY.md`:

```markdown
# AI Capability Policy

Status: Internal policy for product-evolution execution order.

## Visible Now

- Non-AI journey orchestration nodes listed in `NodeType`.
- `AI_LLM` only because P2-019 provides the governed provider, template, audit, handler, and frontend configuration path.
- Documentation references to future AI work when they point to an approved spec.

## Hidden Until Governed

- `AI_NEXT_BEST_ACTION`
- Any node that calls an LLM provider, prompt template, embedding store, autonomous decision service, or generated offer selector without the P2-019 runtime, audit, and policy controls.

## Rules

1. Do not expose unfinished AI nodes in `/meta/node-types`.
2. Do not add AI node constants to `NodeType` unless the backing runtime behavior, audit trail, tenant controls, and frontend configuration exist.
3. Do not present AI copy in operator UI unless the backing runtime behavior exists.
4. Use P2-019 for governed `AI_LLM` provider, template, audit, and output-schema work.
5. Use P3 AI strategy specs for autonomous marketing operations, marketplaces, or public ecosystem claims.
```

- [x] **Step 2: Run governance test again**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeTypeGovernanceTest
```

Expected: PASS.

### Verification Evidence

- Backend AI node type governance suite:

```bash
cd backend && mvn -pl canvas-engine -Dtest=NodeTypeGovernanceTest -DfailIfNoTests=true test
```

Result: 3 tests, 0 failures, 0 errors, 0 skipped.

### Task 3: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-003g-ai-capability-policy-and-governance.md`
- Read: `docs/product-evolution/plans/p1-003g-ai-capability-policy-and-governance-plan.md`

- [x] **Step 1: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add docs/product-evolution/AI_CAPABILITY_POLICY.md \
  backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java \
  docs/product-evolution/specs/p1-003g-ai-capability-policy-and-governance.md \
  docs/product-evolution/plans/p1-003g-ai-capability-policy-and-governance-plan.md
git commit -m "docs: add ai capability governance policy"
```

Expected: commit contains only AI governance policy and test guard.
