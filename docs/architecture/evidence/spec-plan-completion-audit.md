# Architecture Spec And Plan Completion Audit

Date: 2026-06-05

Scope: `docs/architecture/specs`, `docs/architecture/plans`, `docs/architecture/todo`, `docs/architecture/index.md`, architecture-related entries in `docs/INDEX.md`, P3 decision evidence, and P3 promotion gates.

## Verdict

The architecture review material has been covered by active specs, matching plans, evidence files, ADRs, runbooks, or the coverage matrix.

Numbered package coverage is complete:

- `docs/architecture/specs/` has 28 priority-prefixed files: 27 package specs plus the supporting `P3-00` code-verification artifact.
- `docs/architecture/plans/` has 28 priority-prefixed files: 27 matching package plans plus the `P0-00` materialization plan.
- Every package spec from `P0-01` through `P3-09` has a matching plan.
- Every package plan from `P0-01` through `P3-09` has a matching spec.
- The intentional exceptions are `P3-00-architecture-boundary-code-verification.md` as supporting evidence and `P0-00-architecture-spec-plan-materialization-plan.md` as the original materialization plan.

## P3 Architecture Coverage

The architecture-evolution material is not left as a single broad backlog item. It is split into focused decision packages:

- `P3-00`: boundary review and code-level service-extraction verification.
- `P3-01`: platform-evolution entry point and promotion checklist.
- `P3-02`: service decomposition, domain boundaries, and first extraction gate.
- `P3-03`: data-platform thin-slice architecture and governance.
- `P3-04`: multi-datasource ownership, transaction, and migration boundaries.
- `P3-05`: WebFlux/MVC runtime decision, with MVC migration deferred.
- `P3-06`: Kubernetes operating model and Helm scaffold.
- `P3-07`: production platform component decision package.
- `P3-08`: WeCom SCRM integration boundary and first slice.
- `P3-09`: OneID, event schema, tenant, and engine/web platform primitives.

Implementation remains gated by `docs/architecture/platform-evolution-promotion-checklist.md` and by P0/P1 prerequisites where applicable.

## Fixes Recorded In This Audit

- Corrected the P3-07 promotion checklist evidence path to `docs/architecture/evidence/p3-07-platform-components.md`.
- Updated P3 source-queue wording so it points to the focused P3 decision packages instead of presenting the item as unprocessed planning text.
- Updated `docs/architecture/todo/coverage-matrix.md` so evolution-document rows name the concrete P3 decision package that covers each source topic.
- Updated `docs/architecture/EXECUTABLE_PLAN_AUDIT.md` so plan handoff does not require default staging or commits.
- Updated architecture-related entries in `docs/INDEX.md` to point to active specs/plans or archived source documents.

## Verification Commands

No unchecked work items remain in architecture specs and plans, excluding the instructional checkbox-format line:

```bash
rg -n "\[ \]" docs/architecture/plans docs/architecture/specs | rg -v "Steps use checkbox" || true
```

No default VCS staging or commit command remains in architecture specs or plans:

```bash
rg -n "git (add|commit)" docs/architecture/plans docs/architecture/specs || true
```

No stale P3-07 evidence path or old planning-status phrase remains in architecture docs:

```bash
rg -n "p3-07-production[-]components|[Pp]lanning material" docs/architecture docs/INDEX.md || true
```

Spec and plan pairs are complete, with the two intentional support-plan exceptions excluded:

```bash
bash -lc 'comm -23 <(find docs/architecture/specs -maxdepth 1 -type f -name "P*.md" ! -name "P3-00-architecture-boundary-code-verification.md" -printf "%f\n" 2>/dev/null | sed "s/-spec\\.md$//" | sed "s/$/-plan.md/" | sort) <(find docs/architecture/plans -maxdepth 1 -type f -name "P*.md" -printf "%f\n" 2>/dev/null | sort) || true'
bash -lc 'comm -23 <(find docs/architecture/plans -maxdepth 1 -type f -name "P*.md" ! -name "P0-00-architecture-spec-plan-materialization-plan.md" -printf "%f\n" 2>/dev/null | sed "s/-plan\\.md$//" | sed "s/$/-spec.md/" | sort) <(find docs/architecture/specs -maxdepth 1 -type f -name "P*.md" -printf "%f\n" 2>/dev/null | sort) || true'
```

Selected architecture markdown links exist in the active indexes, coverage matrix, and promotion checklist.

## Known Scope Boundary

`docs/INDEX.md` still contains unrelated non-architecture links to product-evolution and code-review materials that appear to have been reorganized separately. They are outside this architecture-folder audit and should be handled in a separate docs-index cleanup.
