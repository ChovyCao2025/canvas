# Filtered Product Opportunities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add supplemental P2/P3 todo documents for useful product ideas that were filtered out of the first product-evolution cleanup.

**Architecture:** Keep the existing `docs/product-evolution/todo/p0|p1|p2|p3` structure. Add two focused supplemental docs instead of reopening archived source files or expanding existing P0/P1 work.

**Tech Stack:** Markdown documentation, existing product-evolution archive and todo structure, shell verification with `find`, `rg`, and `git diff`.

---

### Task 1: Add Supplemental P2 Opportunities

**Files:**
- Create: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md`

- [ ] **Step 1: Create the P2 opportunity document**

Add opportunity cards for medium-term product opportunities from filtered source scope. Each card includes source, useful point, why it was not P0/P1, validation needed, and suggested priority.

- [ ] **Step 2: Verify the P2 document has no placeholders**

Run: `rg -n "$(printf 'TB%s|TO%sDO|PLACE%s' D '' HOLDER)" docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md`

Expected: no output.

### Task 2: Add Supplemental P3 Opportunities

**Files:**
- Create: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md`

- [ ] **Step 1: Create the P3 opportunity document**

Add opportunity cards for long-term product, commercial, ecosystem, AI, internationalization, privacy, and architecture-dependent strategy.

- [ ] **Step 2: Verify the P3 document has no placeholders**

Run: `rg -n "$(printf 'TB%s|TO%sDO|PLACE%s' D '' HOLDER)" docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md`

Expected: no output.

### Task 3: Update Todo Index

**Files:**
- Modify: `docs/product-evolution/todo/INDEX.md`

- [ ] **Step 1: Add the new P2 and P3 documents to the active list**

Add links under the existing P2 and P3 sections.

- [ ] **Step 2: Add a supplemental extraction note**

Explain that the new documents preserve useful filtered opportunities without making them immediate execution commitments.

- [ ] **Step 3: Verify links and counts**

Run: `find docs/product-evolution/todo -type f -name '*.md' | sort`

Expected: existing todo docs plus the two new supplemental docs.

### Task 4: Final Verification

**Files:**
- Verify: `docs/product-evolution/todo/INDEX.md`
- Verify: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md`
- Verify: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md`

- [ ] **Step 1: Confirm no source files returned to root**

Run: `find docs/product-evolution -maxdepth 1 -type f -name '*.md' -print`

Expected: no output.

- [ ] **Step 2: Confirm source archive still has 13 files**

Run: `find docs/product-evolution/archive/2026-06-03 -maxdepth 1 -type f -name '*.md' | sort | wc -l`

Expected: `13`.

- [ ] **Step 3: Confirm placeholder scan passes**

Run: `rg -n "$(printf 'TB%s|TO%sDO|PLACE%s' D '' HOLDER)" docs/product-evolution/todo docs/superpowers/specs/2026-06-03-filtered-product-opportunities-design.md docs/superpowers/plans/2026-06-03-filtered-product-opportunities.md`

Expected: no output.
