# Ecosystem And Partner Program Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce evidence and a governance decision for ISV tiers, partner review, SDK samples, submissions, revenue sharing, support, and community governance.

**Architecture:** This is a P3 strategy gate, not a portal or marketplace build. The slice creates discovery, tier/review, and governance Markdown artifacts; no backend, frontend, or Flyway files are changed.

**Tech Stack:** Markdown, shell validation with `rg`, repository documentation review.

**Implementation Status:** Discovery and governance artifacts are complete in the current workspace record. Reverified on 2026-06-08 by checking the P3-006 discovery, partner checklist, and governance-gate headings plus the `split` gate outcome. Commit and merge status was not verified in this docs-only audit; the commit boundaries are documented because no commit was requested.

---

## Spec Reference

- `docs/product-evolution/specs/p3-006-ecosystem-and-partner-program.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#ecosystem-and-partner-program`

## File Structure

**Evidence And Governance**
- Create: `docs/product-evolution/evidence/p3-006-partner-program-discovery.md` - owners, partner segment, value proposition, demand evidence, and support model.
- Create: `docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md` - tier criteria, security review, SDK sample, support, and publish criteria.
- Create: `docs/product-evolution/governance/p3-006-partner-program-gate.md` - proceed/park/split decision, KPI, child spec, and revisit trigger.

**No Runtime Files**
- No backend files.
- No frontend files.
- No Flyway migration. If approved, runtime storage starts at `backend/canvas-engine/src/main/resources/db/migration/V178__partner_program_registry.sql`.

### Task 1: Partner Program Discovery

**Files:**
- Create: `docs/product-evolution/evidence/p3-006-partner-program-discovery.md`

- [x] **Step 1: Write discovery artifact**

Create `docs/product-evolution/evidence/p3-006-partner-program-discovery.md`:

```markdown
# P3-006 Partner Program Discovery

## Owners

- Ecosystem owner: Ecosystem Lead
- Product owner: Marketing Canvas Product Lead
- Security owner: Security Review Lead
- Support owner: Partner Support Lead
- Decision date: 2026-07-20

## Target Partner Segment

- Segment: ISVs integrating campaign execution, data enrichment, or channel delivery.
- Partner value proposition: faster access to tenant-approved canvas extension points.
- Support model: named partner support queue with security review before publishing.

## Evidence Register

| Source | Source date | Owner | Confidence | Decision implication |
|---|---:|---|---|---|
| Integration request backlog | 2026-06-10 | Ecosystem Lead | High | Repeated requests justify review checklist discovery. |
| SDK support tickets | 2026-06-12 | Partner Support Lead | Medium | Samples and test fixtures are required before public submission. |
| Partner interview notes | 2026-06-18 | Product Lead | Medium | Revenue share should not be committed until demand is validated. |

## Discovery Conclusion

Recommended gate input: split partner review governance from public portal build. Portal work requires a child spec after security and support sign-off.
```

- [x] **Step 2: Verify discovery headings**

Run:

```bash
rg -n "^## (Owners|Target Partner Segment|Evidence Register|Discovery Conclusion)$" docs/product-evolution/evidence/p3-006-partner-program-discovery.md
```

Expected: finds all four headings.

- [x] **Step 3: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add docs/product-evolution/evidence/p3-006-partner-program-discovery.md docs/product-evolution/plans/p3-006-ecosystem-and-partner-program-plan.md
git commit -m "docs: add partner program discovery"
```

Expected: commit contains only the discovery artifact and this plan.

### Task 2: Partner Tier And Review Checklist

**Files:**
- Create: `docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md`

- [x] **Step 1: Write tier and review checklist**

Create `docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md`:

```markdown
# P3-006 Partner Tier And Review Checklist

## Tier Model

| Tier | Entry criteria | Review requirements | Support entitlement | Publish criteria |
|---|---|---|---|---|
| Registered | Signed partner contact and use case summary | Basic identity and owner review | Community support | Internal listing only |
| Verified | Working integration demo and SDK sample | Security checklist and support owner approval | Partner support queue | Private tenant pilot |
| Marketplace | Two successful pilots and support runbook | Security, legal, support, and product approval | Named support SLA | Public listing after governance gate |

## Security Review

- Require data classes touched by the integration.
- Require authentication method and secret rotation plan.
- Require rollback and incident contact.

## SDK Sample Requirements

- Sample must include setup, tenant-scoped auth, smoke test, and rollback notes.
- Sample must not include production secrets or real tenant data.
```

- [x] **Step 2: Verify checklist headings**

Run:

```bash
rg -n "^## (Tier Model|Security Review|SDK Sample Requirements)$" docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md
```

Expected: finds all three headings.

- [x] **Step 3: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md docs/product-evolution/plans/p3-006-ecosystem-and-partner-program-plan.md
git commit -m "docs: add partner tier review checklist"
```

Expected: commit contains only the checklist and this plan.

### Task 3: Governance Gate

**Files:**
- Create: `docs/product-evolution/governance/p3-006-partner-program-gate.md`

- [x] **Step 1: Write governance gate**

Create `docs/product-evolution/governance/p3-006-partner-program-gate.md`:

```markdown
# P3-006 Partner Program Gate

## Decision

- Outcome: split
- Decision owner: Ecosystem Lead
- Decision date: 2026-07-20
- First measurable KPI: complete three verified partner reviews with no unresolved security blockers.

## Next Child Spec

- Review governance child spec: `docs/product-evolution/specs/p3-006a-partner-review-governance.md`
- Portal child spec: `docs/product-evolution/specs/p3-006b-partner-portal-submission.md`

## Revisit Trigger

If fewer than three partner-demand sources remain active by 2026-08-20, park portal work and keep only manual review governance.

## Sign-Off

| Role | Owner | Status |
|---|---|---|
| Ecosystem | Ecosystem Lead | Required |
| Security | Security Review Lead | Required |
| Support | Partner Support Lead | Required |
```

- [x] **Step 2: Verify governance gate**

Run:

```bash
rg -n "^- Outcome: (proceed|park|split)$|^## (Decision|Next Child Spec|Revisit Trigger|Sign-Off)$" docs/product-evolution/governance/p3-006-partner-program-gate.md
```

Expected: finds the split outcome and four headings.

- [x] **Step 3: Final validation**

Run:

```bash
rg -n "^# P3-006" docs/product-evolution/evidence/p3-006-partner-program-discovery.md docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md docs/product-evolution/governance/p3-006-partner-program-gate.md
git diff --check docs/product-evolution/specs/p3-006-ecosystem-and-partner-program.md docs/product-evolution/plans/p3-006-ecosystem-and-partner-program-plan.md
```

Expected: first command finds three document titles; `git diff --check` exits 0.

- [x] **Step 4: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add docs/product-evolution/evidence/p3-006-partner-program-discovery.md \
  docs/product-evolution/evidence/p3-006-partner-tier-and-review-checklist.md \
  docs/product-evolution/governance/p3-006-partner-program-gate.md \
  docs/product-evolution/specs/p3-006-ecosystem-and-partner-program.md \
  docs/product-evolution/plans/p3-006-ecosystem-and-partner-program-plan.md
git commit -m "docs: add partner program strategy gate"
```

Expected: commit contains only P3-006 evidence, governance, spec, and plan files.
