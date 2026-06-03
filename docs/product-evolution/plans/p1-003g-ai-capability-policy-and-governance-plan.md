# AI Capability Policy And Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an internal AI capability policy and governance test protection so unfinished AI nodes remain hidden until P2-019.

**Architecture:** Treat this as a documentation and test guard slice. The policy describes visibility rules; `NodeTypeGovernanceTest` enforces that public node constants do not expose unfinished AI capabilities before the production `AI_LLM` spec is implemented.

**Tech Stack:** Markdown, Java 21, JUnit 5, AssertJ.

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

- [ ] **Step 1: Add AI visibility assertion**

Add this test:

```java
@Test
void unfinishedAiNodesAreNotPublicNodeConstants() {
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

    assertThat(values).doesNotContain("AI_NEXT_BEST_ACTION", "AI_LLM");
}
```

Add imports if absent:

```java
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
```

- [ ] **Step 2: Run governance test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeTypeGovernanceTest
```

Expected: PASS if unfinished AI node constants remain hidden.

### Task 2: Policy Document

**Files:**
- Create: `docs/product-evolution/AI_CAPABILITY_POLICY.md`

- [ ] **Step 1: Add policy document**

Create `AI_CAPABILITY_POLICY.md`:

```markdown
# AI Capability Policy

Status: Internal policy for product-evolution execution order.

## Visible Now

- Non-AI journey orchestration nodes listed in `NodeType`.
- Documentation references to future AI work when they point to an approved spec.

## Hidden Until P2-019

- `AI_LLM`
- `AI_NEXT_BEST_ACTION`
- Any node that calls an LLM provider, prompt template, embedding store, autonomous decision service, or generated offer selector.

## Rules

1. Do not expose unfinished AI nodes in `/meta/node-types`.
2. Do not add AI node constants to `NodeType` before P2-019 is implemented.
3. Do not present AI copy in operator UI unless the backing runtime behavior exists.
4. Use P2-019 for governed `AI_LLM` provider, template, audit, and output-schema work.
5. Use P3 AI strategy specs for autonomous marketing operations, marketplaces, or public ecosystem claims.
```

- [ ] **Step 2: Run governance test again**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeTypeGovernanceTest
```

Expected: PASS.

### Task 3: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-003g-ai-capability-policy-and-governance.md`
- Read: `docs/product-evolution/plans/p1-003g-ai-capability-policy-and-governance-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add docs/product-evolution/AI_CAPABILITY_POLICY.md \
  backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java \
  docs/product-evolution/specs/p1-003g-ai-capability-policy-and-governance.md \
  docs/product-evolution/plans/p1-003g-ai-capability-policy-and-governance-plan.md
git commit -m "docs: add ai capability governance policy"
```

Expected: commit contains only AI governance policy and test guard.
