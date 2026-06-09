# Open Execution Plan Closeout Audit

Date: 2026-06-09
Status: Not complete

This audit expands the explicit open execution plan queue from
[`SPEC_PLAN_COMPLETION_STATUS_AUDIT.md`](SPEC_PLAN_COMPLETION_STATUS_AUDIT.md)
and [`NO_TOP_STATUS_QUEUE_AUDIT.md`](NO_TOP_STATUS_QUEUE_AUDIT.md). It covers
the 15 spec/plan pairs that now have top-level open status in both files.

## Method

The scan reads the matching plan files and counts unchecked checkbox execution
steps outside fenced code samples. A pair stays in this queue when the plan
retains real implementation, test, verification, or rollout tasks. Commit-only
and historical RED-state boundaries are not listed here; those records were
moved to verification-gap status instead.

This is a docs-only closeout queue. It does not prove runtime implementation,
and it does not descope any plan.

## Summary

| Group | Pairs | Open plan tasks | Closeout meaning |
| --- | ---: | ---: | --- |
| Product operations surfaces | 8 | 153 | Backend and frontend product workflows remain unimplemented or unverified from the plan checklists. |
| Analytics and warehouse foundations | 5 | 80 | Data, analytics, and warehouse infrastructure plans retain implementation and verification tasks. The bounded analytics query APIs and BI compiler foundation moved to verification-gap status after focused verification. |
| Canvas runtime and workflow UX | 0 | 0 | The former P2-017 through P2-017E open records now have current focused verification evidence and moved to verification-gap status. |
| Architecture and AI foundations | 2 | 49 | AI node productionization and churn/smart-timing foundations remain open. Runtime architecture evidence moved to verification-gap status after focused verification. |
| Total | 15 | 282 | These are the current explicit implementation backlog for product-evolution closeout. |

## Todo Deferral Boundary

On 2026-06-09 the user clarified that development sourced from
`docs/product-evolution/todo` can be deferred for now. Under that narrowed
boundary, the 8 product-operations plans below are out of the immediate
closeout scope:

- `p2-007-analytics-command-center`
- `p2-008-integration-readiness`
- `p2-009-product-usage-analytics-feedback-loop`
- `p2-010-audience-operations-data-quality`
- `p2-011-editor-productivity-beyond-baseline`
- `p2-012-channel-intelligence-and-scheduling`
- `p2-013-knowledge-base-best-practice-library`
- `p2-014-design-system-guided-experience`

That deferral removes 153 unchecked plan tasks from the immediate queue, but
it does not close the broader product-evolution goal. The remaining 7 open
plans still account for 129 unchecked tasks and are sourced from
`docs/optimization/*`, `docs/superpowers/*`, product-evolution spec
dependencies, or architecture docs rather than `docs/product-evolution/todo`.

## Product Operations Surfaces

These plans are broad product surfaces with backend and frontend work. They are
not closeout candidates without implementation or an explicit descope decision.

| Pair | Open tasks | Primary unresolved scope |
| --- | ---: | --- |
| `p2-007-analytics-command-center` | 18 | Metric dictionary, tenant dashboard summary, channel comparison, export-state helpers. |
| `p2-008-integration-readiness` | 18 | Integration API keys, masked display, revoke behavior, readiness summary. |
| `p2-009-product-usage-analytics-feedback-loop` | 19 | Usage events, feedback/NPS capture, feature flags, alert-rule evaluation. |
| `p2-010-audience-operations-data-quality` | 19 | Audience set operations, snapshots, and data-quality status. |
| `p2-011-editor-productivity-beyond-baseline` | 22 | Recent/favorite state, keyboard/node search helpers, batch helpers, async validation state. |
| `p2-012-channel-intelligence-and-scheduling` | 20 | Rule-based routing, marketing calendar conflicts, channel cost, send-time observations. |
| `p2-013-knowledge-base-best-practice-library` | 19 | Searchable articles, template links, contextual help, benchmark metadata. |
| `p2-014-design-system-guided-experience` | 18 | Design tokens, empty states, product guide helpers, accessible option lookup, reduced motion. |

## Analytics And Warehouse Foundations

These plans are infrastructure-heavy and should be sequenced carefully because
later analytics and BI surfaces depend on bounded data contracts.

| Pair | Open tasks | Primary unresolved scope |
| --- | ---: | --- |
| `p2-015-4000-concurrency-readiness-and-lane-isolation` | 24 | 4000-readiness profile, DAG cost profiling, lane isolation, retry pressure, runbook gates. |
| `p2-016-analytics-event-trace-schema-and-sink` | 12 | Analytics trace fields, `TraceEventSink`, buffered write metrics. |
| `p2-016b-analytics-retention-and-archive-policy` | 10 | Tenant retention policy, archive/delete jobs, legal hold behavior. |
| `p2-016d-frontend-analytics-views-and-export-states` | 12 | Frontend analytics API wrapper, presentation helpers, export states. |
| `p2-022-cdp-warehouse-ingestion-and-aggregation` | 22 | Realtime Doris ingestion, offline backfill, bounded aggregation, run/watermark metadata. |
`p2-016c-bounded-analytics-query-apis` moved out of this group on 2026-06-09
after focused schema, guard, service, and controller tests passed with 23 tests,
zero failures, and zero errors; commit and merge status remain unverified.
`p2-023-bi-dataset-query-compiler-foundation` moved out of this group on
2026-06-09 after focused BI compiler and registry tests passed; commit and
merge status remain unverified.

## Canvas Runtime And Workflow UX

The former open records in this group were verified in the current worktree on
2026-06-09 and moved to verification-gap status because commit and merge status
remain unverified:

| Pair | Open tasks | Primary unresolved scope |
| --- | ---: | --- |
| `p2-017-template-renderer-and-variable-picker` | 0 | Focused backend and frontend verification passed; commit/merge remains unverified. |
| `p2-017b-user-input-and-wait-event-ux` | 0 | Focused backend verification passed; commit/merge remains unverified. |
| `p2-017c-connected-content-node` | 0 | Focused backend and config-panel verification passed; commit/merge remains unverified. |
| `p2-017d-test-users-and-single-user-rerun` | 0 | Focused backend verification and frontend build passed; commit/merge remains unverified. |
| `p2-017e-execution-timeline-and-batch-operations` | 0 | Focused backend and frontend verification passed; commit/merge remains unverified. |

## Architecture And AI Foundations

These plans have high blast radius or production-governance impact. They should
not be treated as done without runtime implementation evidence and focused
verification.

| Pair | Open tasks | Primary unresolved scope |
| --- | ---: | --- |
| `p2-019-ai-llm-node-productionization` | 24 | Governed `AI_LLM` provider/template/audit storage, gateway, handler, admin UI. |
| `p2-020-churn-prediction-and-smart-timing-foundation` | 25 | Churn scoring, smart timing, CDP profile writes, UI run status. |

## Closeout Paths

Each open pair needs one of these before the broader goal can be closed:

- execute the plan tasks and record fresh verification evidence;
- explicitly descope or defer the pair in both the spec and plan;
- split the plan into smaller child specs with clear ownership and leave the
  parent as a documented umbrella;
- or narrow the active goal to docs-structure/package closeout and record that
  product runtime completion is out of scope.

## Current Boundary

The open queue is no longer a status-normalization problem. It is a product
scope and implementation-completion problem. Deferring
`docs/product-evolution/todo` removes the 8 product-operations plans from the
immediate execution queue, but 7 non-todo open plans remain. The docs now make
that boundary explicit, but they do not satisfy it.
