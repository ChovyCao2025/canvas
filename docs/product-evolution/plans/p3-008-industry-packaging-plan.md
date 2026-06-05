# Industry Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce evidence and a governance decision for vertical templates, nodes, metrics, compliance profiles, and industry packaging.

**Architecture:** This is a P3 strategy gate, not a vertical package build. The slice creates discovery, scorecard, and governance Markdown artifacts; no backend, frontend, template, node, dashboard, or Flyway files are changed.

**Tech Stack:** Markdown, shell validation with `rg`, repository documentation review.

---

## Spec Reference

- `docs/product-evolution/specs/p3-008-industry-packaging.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#industry-packaging`

## File Structure

**Evidence And Governance**
- Create: `docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md` - owners, candidate verticals, buyer/user profile, evidence, and launch constraints.
- Create: `docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md` - demand, repeatability, compliance, template coverage, sales/support readiness, and decision implication.
- Create: `docs/product-evolution/governance/p3-008-industry-packaging-gate.md` - proceed/park/split decision, KPI, child spec, and revisit trigger.

**No Runtime Files**
- No backend files.
- No frontend files.
- No Flyway migration. If approved, runtime storage starts at `backend/canvas-engine/src/main/resources/db/migration/V180__industry_packaging_registry.sql`.

### Task 1: Industry Packaging Discovery

**Files:**
- Create: `docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md`

- [ ] **Step 1: Write discovery artifact**

Create `docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md`:

```markdown
# P3-008 Industry Packaging Discovery

## Owners

- Vertical owner: Industry Packaging Lead
- Product owner: Marketing Canvas Product Lead
- Compliance owner: Compliance Lead
- Sales owner: Sales Enablement Lead
- Support owner: Support Operations Lead
- Decision date: 2026-07-30

## Candidate Verticals

- Retail lifecycle marketing
- Financial services retention
- Education enrollment engagement

## Buyer And User Profile

- Buyer: VP Marketing or Digital Operations
- User: lifecycle campaign operator
- Implementation owner: solution consultant

## Evidence Sources

| Source | Source date | Owner | Confidence | Decision implication |
|---|---:|---|---|---|
| Sales opportunity notes | 2026-06-12 | Sales Enablement Lead | Medium | Retail has repeated template demand. |
| Implementation notes | 2026-06-15 | Solution Consulting Lead | Medium | Financial services needs compliance claim review before packaging. |
| Template usage analytics | 2026-06-18 | Product Analytics Lead | High | Existing retail templates have highest reuse. |

## Launch Constraints

- Every compliance claim requires Compliance Lead approval.
- Templates need a maintenance owner before publication.
- Support must confirm runbook coverage for each vertical.
```

- [ ] **Step 2: Verify discovery headings**

Run:

```bash
rg -n "^## (Owners|Candidate Verticals|Buyer And User Profile|Evidence Sources|Launch Constraints)$" docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md
```

Expected: finds all five headings.

- [ ] **Step 3: Commit discovery artifact**

Run:

```bash
git add docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md docs/product-evolution/plans/p3-008-industry-packaging-plan.md
git commit -m "docs: add industry packaging discovery"
```

Expected: commit contains only the discovery artifact and this plan.

### Task 2: Vertical Selection Scorecard

**Files:**
- Create: `docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md`

- [ ] **Step 1: Write scorecard**

Create `docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md`:

```markdown
# P3-008 Vertical Selection Scorecard

## Scorecard

| Vertical | Evidence source | Demand score | Repeatability score | Compliance risk | Existing template coverage | Sales/support readiness | Decision implication |
|---|---|---:|---:|---|---|---|---|
| Retail lifecycle marketing | Template usage analytics | 5 | 4 | Low | High | Medium | Best first package candidate. |
| Financial services retention | Implementation notes | 4 | 3 | High | Medium | Low | Needs compliance child spec before packaging. |
| Education enrollment engagement | Sales opportunity notes | 3 | 3 | Medium | Low | Medium | Park until more evidence exists. |

## Packaging Governance Brief

- Content owner: Industry Packaging Lead
- Review cadence: monthly while package is active
- Approval rule: product, compliance, sales, and support owners must sign off
- Allowed claims: workflow fit, template contents, implementation prerequisites
- Blocked claims: legal compliance guarantees, revenue uplift guarantees, automated approval claims
- Maintenance trigger: template failure, compliance rule change, or support escalation trend
```

- [ ] **Step 2: Verify scorecard headings**

Run:

```bash
rg -n "^## (Scorecard|Packaging Governance Brief)$" docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md
```

Expected: finds both headings.

- [ ] **Step 3: Commit scorecard**

Run:

```bash
git add docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md docs/product-evolution/plans/p3-008-industry-packaging-plan.md
git commit -m "docs: add industry packaging scorecard"
```

Expected: commit contains only the scorecard and this plan.

### Task 3: Governance Gate

**Files:**
- Create: `docs/product-evolution/governance/p3-008-industry-packaging-gate.md`

- [ ] **Step 1: Write governance gate**

Create `docs/product-evolution/governance/p3-008-industry-packaging-gate.md`:

```markdown
# P3-008 Industry Packaging Gate

## Decision

- Outcome: split
- Decision owner: Industry Packaging Lead
- Decision date: 2026-07-30
- First measurable KPI: create one retail package proposal with three reusable templates and support runbook approval.

## Next Child Spec

- Retail package child spec: `docs/product-evolution/specs/p3-008a-retail-lifecycle-package.md`
- Compliance review child spec: `docs/product-evolution/specs/p3-008b-vertical-compliance-claims-review.md`

## Revisit Trigger

If retail package evidence cannot produce three reusable templates by 2026-08-30, park industry packaging and keep template improvements generic.

## Sign-Off

| Role | Owner | Status |
|---|---|---|
| Vertical | Industry Packaging Lead | Required |
| Product | Marketing Canvas Product Lead | Required |
| Compliance | Compliance Lead | Required |
| Sales | Sales Enablement Lead | Required |
| Support | Support Operations Lead | Required |
```

- [ ] **Step 2: Verify governance gate**

Run:

```bash
rg -n "^- Outcome: (proceed|park|split)$|^## (Decision|Next Child Spec|Revisit Trigger|Sign-Off)$" docs/product-evolution/governance/p3-008-industry-packaging-gate.md
```

Expected: finds the split outcome and four headings.

- [ ] **Step 3: Final validation**

Run:

```bash
rg -n "^# P3-008" docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md docs/product-evolution/governance/p3-008-industry-packaging-gate.md
git diff --check docs/product-evolution/specs/p3-008-industry-packaging.md docs/product-evolution/plans/p3-008-industry-packaging-plan.md
```

Expected: first command finds three document titles; `git diff --check` exits 0.

- [ ] **Step 4: Commit governance gate**

Run:

```bash
git add docs/product-evolution/evidence/p3-008-industry-packaging-discovery.md \
  docs/product-evolution/evidence/p3-008-vertical-selection-scorecard.md \
  docs/product-evolution/governance/p3-008-industry-packaging-gate.md \
  docs/product-evolution/specs/p3-008-industry-packaging.md \
  docs/product-evolution/plans/p3-008-industry-packaging-plan.md
git commit -m "docs: add industry packaging strategy gate"
```

Expected: commit contains only P3-008 evidence, governance, spec, and plan files.
