# AI Native Marketing Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce evidence and a governance decision for AI gateway, AI policy, copy generation, segment/journey assistance, optimization, anomaly detection, prediction, and human-approved agents.

**Architecture:** This is a P3 strategy gate, not an AI runtime build. The slice creates AI discovery, policy matrix, and governance Markdown artifacts; no backend, frontend, provider, model, or Flyway files are changed.

**Tech Stack:** Markdown, shell validation with `rg`, repository documentation review.

---

## Spec Reference

- `docs/product-evolution/specs/p3-007-ai-native-marketing-operations.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#ai-native-marketing-operations`

## File Structure

**Evidence And Governance**
- Create: `docs/product-evolution/evidence/p3-007-ai-operations-discovery.md` - owners, workflow, provider constraints, data boundaries, and demand evidence.
- Create: `docs/product-evolution/evidence/p3-007-ai-policy-matrix.md` - allowed/blocked use cases, human approval, audit, budget, and evaluation methods.
- Create: `docs/product-evolution/governance/p3-007-ai-operations-gate.md` - proceed/park/split decision, KPI, child spec, and revisit trigger.

**No Runtime Files**
- No backend files.
- No frontend files.
- No Flyway migration. If approved, runtime storage starts at `backend/canvas-engine/src/main/resources/db/migration/V179__ai_operations_policy_and_audit.sql`.

### Task 1: AI Operations Discovery

**Files:**
- Create: `docs/product-evolution/evidence/p3-007-ai-operations-discovery.md`

- [ ] **Step 1: Write discovery artifact**

Create `docs/product-evolution/evidence/p3-007-ai-operations-discovery.md`:

```markdown
# P3-007 AI Operations Discovery

## Owners

- AI product owner: Marketing Canvas Product Lead
- Architecture owner: Platform Architect
- Compliance owner: Compliance Lead
- Security reviewer: Security Lead
- Decision date: 2026-07-25

## Target Operator Workflow

- Workflow: campaign copy draft and segment-rule explanation.
- Human approval owner: lifecycle marketing manager.
- Model provider constraints: no raw PII in prompts, provider audit logs required, budget cap required.

## Evidence Register

| Source | Source date | Owner | Confidence | Decision implication |
|---|---:|---|---|---|
| Operator interviews about copy drafting | 2026-06-12 | Product Lead | Medium | Copy assistance has demand but needs approval workflow. |
| Campaign operations bottleneck review | 2026-06-14 | Operations Lead | High | Segment explanation reduces review time more safely than autonomous edits. |
| Cost assumption worksheet | 2026-06-18 | Architecture Owner | Low | Provider cost uncertainty requires budget guard before runtime build. |

## Discovery Conclusion

Recommended gate input: split AI policy/audit foundation from any generative or autonomous workflow. Runtime AI work requires child specs with evaluation and approval gates.
```

- [ ] **Step 2: Verify discovery headings**

Run:

```bash
rg -n "^## (Owners|Target Operator Workflow|Evidence Register|Discovery Conclusion)$" docs/product-evolution/evidence/p3-007-ai-operations-discovery.md
```

Expected: finds all four headings.

- [ ] **Step 3: Commit discovery artifact**

Run:

```bash
git add docs/product-evolution/evidence/p3-007-ai-operations-discovery.md docs/product-evolution/plans/p3-007-ai-native-marketing-operations-plan.md
git commit -m "docs: add ai operations discovery"
```

Expected: commit contains only the discovery artifact and this plan.

### Task 2: AI Policy Matrix

**Files:**
- Create: `docs/product-evolution/evidence/p3-007-ai-policy-matrix.md`

- [ ] **Step 1: Write AI policy matrix**

Create `docs/product-evolution/evidence/p3-007-ai-policy-matrix.md`:

```markdown
# P3-007 AI Policy Matrix

## Policy Matrix

| Use case | Allowed action | Blocked action | Human approval | Audit requirement | Budget control | Evaluation method |
|---|---|---|---|---|---|---|
| Copy draft | Generate draft variants | Auto-send campaign | Required before publish | Prompt, response, approver | Daily tenant cap | Human quality review |
| Segment explanation | Explain existing rule | Modify audience rule | Required before save | Input rule and explanation | Per-request cap | Accuracy review against known examples |
| Journey optimizer | Recommend next step | Auto-edit journey | Required before edit | Recommendation and accepted action | Experiment cap | A/B holdout proposal |

## Data Boundaries

- Do not send raw PII to model providers.
- Tenant ID must be included in audit records, not in prompts.
- Provider logs must be available for compliance review.

## Evaluation Gate

- At least 20 reviewed examples per use case.
- No blocked action may be executed by AI.
- Cost per successful operator action must be estimated before runtime implementation.
```

- [ ] **Step 2: Verify policy headings**

Run:

```bash
rg -n "^## (Policy Matrix|Data Boundaries|Evaluation Gate)$" docs/product-evolution/evidence/p3-007-ai-policy-matrix.md
```

Expected: finds all three headings.

- [ ] **Step 3: Commit policy matrix**

Run:

```bash
git add docs/product-evolution/evidence/p3-007-ai-policy-matrix.md docs/product-evolution/plans/p3-007-ai-native-marketing-operations-plan.md
git commit -m "docs: add ai operations policy matrix"
```

Expected: commit contains only the policy matrix and this plan.

### Task 3: Governance Gate

**Files:**
- Create: `docs/product-evolution/governance/p3-007-ai-operations-gate.md`

- [ ] **Step 1: Write governance gate**

Create `docs/product-evolution/governance/p3-007-ai-operations-gate.md`:

```markdown
# P3-007 AI Operations Gate

## Decision

- Outcome: split
- Decision owner: Marketing Canvas Product Lead
- Decision date: 2026-07-25
- First measurable KPI: reduce approved copy drafting time by 20% in a controlled pilot.

## Next Child Spec

- Policy foundation child spec: `docs/product-evolution/specs/p3-007a-ai-policy-audit-foundation.md`
- Copy-assist child spec: `docs/product-evolution/specs/p3-007b-human-approved-copy-assist.md`

## Revisit Trigger

If provider cost or compliance review cannot be completed by 2026-08-25, park runtime AI workflows and keep only policy documentation.

## Sign-Off

| Role | Owner | Status |
|---|---|---|
| Product | Marketing Canvas Product Lead | Required |
| Architecture | Platform Architect | Required |
| Compliance | Compliance Lead | Required |
| Security | Security Lead | Required |
```

- [ ] **Step 2: Verify governance gate**

Run:

```bash
rg -n "^- Outcome: (proceed|park|split)$|^## (Decision|Next Child Spec|Revisit Trigger|Sign-Off)$" docs/product-evolution/governance/p3-007-ai-operations-gate.md
```

Expected: finds the split outcome and four headings.

- [ ] **Step 3: Final validation**

Run:

```bash
rg -n "^# P3-007" docs/product-evolution/evidence/p3-007-ai-operations-discovery.md docs/product-evolution/evidence/p3-007-ai-policy-matrix.md docs/product-evolution/governance/p3-007-ai-operations-gate.md
git diff --check docs/product-evolution/specs/p3-007-ai-native-marketing-operations.md docs/product-evolution/plans/p3-007-ai-native-marketing-operations-plan.md
```

Expected: first command finds three document titles; `git diff --check` exits 0.

- [ ] **Step 4: Commit governance gate**

Run:

```bash
git add docs/product-evolution/evidence/p3-007-ai-operations-discovery.md \
  docs/product-evolution/evidence/p3-007-ai-policy-matrix.md \
  docs/product-evolution/governance/p3-007-ai-operations-gate.md \
  docs/product-evolution/specs/p3-007-ai-native-marketing-operations.md \
  docs/product-evolution/plans/p3-007-ai-native-marketing-operations-plan.md
git commit -m "docs: add ai operations strategy gate"
```

Expected: commit contains only P3-007 evidence, governance, spec, and plan files.
