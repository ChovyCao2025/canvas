# Value Added Services And Customer Success Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce evidence and a governance decision for managed services, consulting, training, certification, health scoring, renewal, churn, and expansion opportunities.

**Architecture:** This is a P3 strategy gate, not a runtime product build. The slice creates three Markdown artifacts with concrete owner, evidence, health-signal, KPI, and gate-decision sections; no backend, frontend, or Flyway files are changed.

**Tech Stack:** Markdown, shell validation with `rg`, repository documentation review.

---

## Spec Reference

- `docs/product-evolution/specs/p3-005-value-added-services-and-customer-success.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#value-added-services-and-customer-success`

## File Structure

**Evidence And Governance**
- Create: `docs/product-evolution/evidence/p3-005-customer-success-discovery.md` - owners, segment, buyer/user, evidence sources, and discovery conclusion.
- Create: `docs/product-evolution/evidence/p3-005-health-score-inputs.md` - health-score signal inventory, source risk, tenant-scope risk, owner, and validation method.
- Create: `docs/product-evolution/governance/p3-005-customer-success-gate.md` - proceed/park/split decision, KPI, next child spec, and revisit trigger.

**No Runtime Files**
- No backend files.
- No frontend files.
- No Flyway migration. If the gate later approves runtime work, the child spec starts from `backend/canvas-engine/src/main/resources/db/migration/V177__customer_success_evidence_or_health_score.sql`.

### Task 1: Discovery Brief

**Files:**
- Create: `docs/product-evolution/evidence/p3-005-customer-success-discovery.md`

- [ ] **Step 1: Write the discovery brief**

Create `docs/product-evolution/evidence/p3-005-customer-success-discovery.md`:

```markdown
# P3-005 Customer Success Discovery

## Owners

- Business owner: Customer Success Lead
- Product owner: Marketing Canvas Product Lead
- Support owner: Support Operations Lead
- Decision date: 2026-07-15

## Target Segment

- Segment: enterprise tenants running more than 20 active canvases
- Service buyer: VP Marketing Operations
- Operator user: lifecycle marketing operator
- Support owner: named customer success manager

## Evidence Sources

| Source | Source date | Owner | Confidence | Decision implication |
|---|---:|---|---|---|
| Renewal notes for enterprise tenant cohort | 2026-06-10 | Customer Success Lead | High | Renewal risk clusters around campaign governance and operator training. |
| Support tickets tagged canvas-operations | 2026-06-12 | Support Operations Lead | Medium | Repeated issues suggest managed-service playbooks may reduce time to resolution. |
| Usage analytics for active canvases | 2026-06-14 | Product Analytics Lead | Medium | Expansion candidates correlate with high canvas count and low publish cadence. |

## Service Catalog MVP Proposal

| Service | Primary user | Outcome | Evidence source | Operating owner |
|---|---|---|---|---|
| Managed campaign review | lifecycle operator | Reduce failed launches | Support tickets | Customer Success Lead |
| Admin training | tenant admin | Improve self-service setup | Renewal notes | Enablement Lead |
| Health-score review | account owner | Identify churn and expansion | Usage analytics | Customer Success Lead |

## Discovery Conclusion

Recommended gate input: split managed-service playbooks from health-score automation. Health-score automation needs a child spec after signal validation.
```

- [ ] **Step 2: Verify discovery brief sections**

Run:

```bash
rg -n "^## (Owners|Target Segment|Evidence Sources|Service Catalog MVP Proposal|Discovery Conclusion)$" docs/product-evolution/evidence/p3-005-customer-success-discovery.md
```

Expected: finds all five section headings.

- [ ] **Step 3: Commit discovery brief**

Run:

```bash
git add docs/product-evolution/evidence/p3-005-customer-success-discovery.md docs/product-evolution/plans/p3-005-value-added-services-and-customer-success-plan.md
git commit -m "docs: add customer success discovery brief"
```

Expected: commit contains only the discovery brief and this plan.

### Task 2: Health-Score Input Inventory

**Files:**
- Create: `docs/product-evolution/evidence/p3-005-health-score-inputs.md`

- [ ] **Step 1: Write health-score inventory**

Create `docs/product-evolution/evidence/p3-005-health-score-inputs.md`:

```markdown
# P3-005 Health Score Inputs

## Signal Inventory

| Signal | Current source | Availability | Tenant-scope risk | Operational owner | Validation method |
|---|---|---|---|---|---|
| Active canvas count | canvas list API export | Available | Low | Product Analytics Lead | Compare active canvas count with renewal notes. |
| Publish cadence | canvas version history | Available | Low | Product Analytics Lead | Check median days between publishes for ten enterprise tenants. |
| Support ticket severity | support export | Partial | Medium | Support Operations Lead | Match ticket tenant IDs before aggregation. |
| Training attendance | enablement roster | Partial | Medium | Enablement Lead | Confirm attendee tenant mapping with customer success manager. |

## Data Access Risk

- No raw customer PII is required for the first validation pass.
- Support and training exports must be aggregated by tenant before joining with product usage.
- Cross-tenant views are allowed only for internal customer-success analysis with named owner approval.

## Validation Exit Criteria

- At least three signals can be populated for eight enterprise tenants.
- At least one signal correlates with a renewal or support outcome.
- Customer Success Lead signs off before any runtime health-score implementation spec is created.
```

- [ ] **Step 2: Verify inventory sections**

Run:

```bash
rg -n "^## (Signal Inventory|Data Access Risk|Validation Exit Criteria)$" docs/product-evolution/evidence/p3-005-health-score-inputs.md
```

Expected: finds all three section headings.

- [ ] **Step 3: Commit health-score inventory**

Run:

```bash
git add docs/product-evolution/evidence/p3-005-health-score-inputs.md docs/product-evolution/plans/p3-005-value-added-services-and-customer-success-plan.md
git commit -m "docs: add customer success health score inventory"
```

Expected: commit contains only the health-score inventory and this plan.

### Task 3: Governance Gate

**Files:**
- Create: `docs/product-evolution/governance/p3-005-customer-success-gate.md`

- [ ] **Step 1: Write governance gate**

Create `docs/product-evolution/governance/p3-005-customer-success-gate.md`:

```markdown
# P3-005 Customer Success Gate

## Decision

- Outcome: split
- Decision owner: Customer Success Lead
- Decision date: 2026-07-15
- First measurable KPI: reduce enterprise tenant severe support tickets by 15% within one quarter.

## Next Child Spec

- Managed services child spec: `docs/product-evolution/specs/p3-005a-managed-service-playbook.md`
- Health-score child spec: `docs/product-evolution/specs/p3-005b-customer-health-score-validation.md`

## Revisit Trigger

If fewer than eight enterprise tenants can provide validated signal data by 2026-08-15, park health-score automation and proceed only with managed-service playbooks.

## Sign-Off

| Role | Owner | Status |
|---|---|---|
| Business | Customer Success Lead | Required |
| Product | Marketing Canvas Product Lead | Required |
| Support | Support Operations Lead | Required |
```

- [ ] **Step 2: Verify governance gate**

Run:

```bash
rg -n "^- Outcome: (proceed|park|split)$|^## (Decision|Next Child Spec|Revisit Trigger|Sign-Off)$" docs/product-evolution/governance/p3-005-customer-success-gate.md
```

Expected: finds the split outcome and four section headings.

- [ ] **Step 3: Run final validation**

Run:

```bash
rg -n "^# P3-005" docs/product-evolution/evidence/p3-005-customer-success-discovery.md docs/product-evolution/evidence/p3-005-health-score-inputs.md docs/product-evolution/governance/p3-005-customer-success-gate.md
git diff --check docs/product-evolution/specs/p3-005-value-added-services-and-customer-success.md docs/product-evolution/plans/p3-005-value-added-services-and-customer-success-plan.md
```

Expected: first command finds three document titles; `git diff --check` exits 0.

- [ ] **Step 4: Commit governance gate**

Run:

```bash
git add docs/product-evolution/evidence/p3-005-customer-success-discovery.md \
  docs/product-evolution/evidence/p3-005-health-score-inputs.md \
  docs/product-evolution/governance/p3-005-customer-success-gate.md \
  docs/product-evolution/specs/p3-005-value-added-services-and-customer-success.md \
  docs/product-evolution/plans/p3-005-value-added-services-and-customer-success-plan.md
git commit -m "docs: add customer success strategy gate"
```

Expected: commit contains only P3-005 evidence, governance, spec, and plan files.
