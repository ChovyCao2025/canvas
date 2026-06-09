# Evidence Manifest

Purpose: inventory evidence and governance artifacts in this directory so
future agents can identify boundaries, handoffs, pathspecs, and decision inputs
before touching product-evolution work.

This manifest describes existing files only. It is not approval to stage,
commit, merge, or expand scope, and it does not claim runtime work was completed
unless the referenced evidence file states that.

## Coordination And Handoffs

| File | Use |
| --- | --- |
| [INDEX.md](INDEX.md) | This evidence manifest; a directory-level inventory of evidence and governance artifacts. |
| [active-session-coordination-2026-06-07.md](active-session-coordination-2026-06-07.md) | Active worktree coordination record for avoiding duplicate implementation; defines the current P2-086/P2-087/P2-088 control-plane lane, out-of-scope areas, focused verification commands, and known external blockers. |
| [p2-080-conversation-handoff-boundary.md](p2-080-conversation-handoff-boundary.md) | P2-080 conversation/SCRM boundary handoff; identifies P2-080 core goals and file groups, plus overlapping P2-082D/D2/K/L slices that should not be reimplemented as P2-080. |
| [p2-086-088-handoff-summary.md](p2-086-088-handoff-summary.md) | Handoff summary for P2-086 Marketing Platform Control Plane, P2-087 Campaign Master Ledger, and P2-088 Integration Contract Registry; records the review manifest, claimed focused verification evidence, exclusions, and next review tasks. |

## P2-080 Pathspec And Split Evidence

| File | Use |
| --- | --- |
| [p2-080-staging-pathspec.txt](p2-080-staging-pathspec.txt) | Proposed P2-080 staging/review path list, including docs, backend, frontend, and scripts associated with the conversation foundation package. |
| [p2-080-clean-core-candidate-pathspec.txt](p2-080-clean-core-candidate-pathspec.txt) | Narrower P2-080 clean-core candidate path list for review after known mixed frontend/index concerns were separated. |
| [p2-080-deferred-overlap-pathspec.txt](p2-080-deferred-overlap-pathspec.txt) | Deferred overlap path list for P2-082D/D2/K/L conversation/SCRM files that should remain outside the P2-080 core package unless ownership changes. |
| [p2-080-shared-split-required-pathspec.txt](p2-080-shared-split-required-pathspec.txt) | Shared files requiring hunk-level split, refactor, or explicit ownership decision before staging as P2-080. |
| [p2-080-staging-diff-review-2026-06-07.md](p2-080-staging-diff-review-2026-06-07.md) | Review notes for the P2-080 staging pathspec; records that no P2-080 files were staged or committed during the review and lists split findings for mixed index/frontend concerns. |
| [p2-080-product-index-hunks.md](p2-080-product-index-hunks.md) | P2-080-only product index rows for implementation order, specs index, and plans index; explicitly warns not to stage mixed product-evolution index files wholesale. |

## P2-086/P2-087/P2-088 Pathspec Evidence

| File | Use |
| --- | --- |
| [p2-086-088-staging-pathspec.txt](p2-086-088-staging-pathspec.txt) | Authoritative review pathspec for the P2-086/P2-087/P2-088 control-plane package, including backend, frontend, migrations, docs, and evidence handoff files. |

## P3 Discovery And Governance Inputs

| File | Use |
| --- | --- |
| [p3-005-customer-success-discovery.md](p3-005-customer-success-discovery.md) | Customer success discovery input for enterprise tenants, service catalog MVP options, evidence sources, and a gate recommendation to split managed-service playbooks from health-score automation. |
| [p3-005-health-score-inputs.md](p3-005-health-score-inputs.md) | Health-score signal inventory, data-access risks, and validation exit criteria before any runtime health-score implementation spec is created. |
| [p3-006-partner-program-discovery.md](p3-006-partner-program-discovery.md) | Partner program discovery input for ISV segments, partner support/security ownership, evidence register, and recommendation to split governance from public portal build. |
| [p3-006-partner-tier-and-review-checklist.md](p3-006-partner-tier-and-review-checklist.md) | Partner tier model, security review items, and SDK sample requirements for partner review governance. |
| [p3-007-ai-operations-discovery.md](p3-007-ai-operations-discovery.md) | AI operations discovery input for copy drafting and segment-rule explanation workflows, with provider constraints and a recommendation to split policy/audit foundation from runtime AI work. |
| [p3-007-ai-policy-matrix.md](p3-007-ai-policy-matrix.md) | AI policy matrix covering allowed and blocked actions, approval, audit, budget, data boundaries, and evaluation gates. |
| [p3-008-industry-packaging-discovery.md](p3-008-industry-packaging-discovery.md) | Industry packaging discovery input for retail, financial services, and education verticals, including evidence sources and launch constraints. |
| [p3-008-vertical-selection-scorecard.md](p3-008-vertical-selection-scorecard.md) | Vertical selection scorecard comparing candidate verticals by demand, repeatability, compliance risk, template coverage, and sales/support readiness; includes packaging governance rules. |
