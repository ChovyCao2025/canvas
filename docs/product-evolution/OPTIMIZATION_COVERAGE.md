# Optimization Coverage Matrix

Date: 2026-06-03

This file records how `docs/optimization` was filtered into executable product-evolution specs and plans. The goal is to preserve useful product capability points while avoiding duplicated, speculative, or unsafe execution scope.

## Source Set

Reviewed source groups:

- Concurrency and production readiness: `docs/optimization/3000-concurrency-hardening-checklist.md`, `docs/optimization/4000-concurrency-readiness-checklist.md`, `docs/optimization/production-readiness-checklist.md`, `docs/optimization/production-design-gaps.md`
- Product and UX audit: `docs/optimization/bmad-product-review-2026-05.md`, `docs/optimization/optimization_list_v7.md`
- Marketing, CDP, and AI capability gaps: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-marketing-platform-roadmap.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/2026-05-30-cdp-sdk-design.md`, `docs/optimization/todo/2026-05-31-ai-capability-roadmap.md`
- Market and competitor research: `docs/optimization/todo/competitor-analysis-report.md`, `docs/optimization/todo/market-research-report.md`, `docs/optimization/todo/2026-05-31-evolution-directions.md`
- Architecture and old plan handoffs: `docs/optimization/todo/plan-review-findings.md`, `docs/optimization/todo/specs/*.md`, `docs/optimization/todo/plans/*.md`
- Archived older lists: `docs/optimization/archive/optimization_list_v1.md` through `docs/optimization/archive/optimization_list_v6.md`, plus archived 2000-concurrency files

## Already Implemented Or Partially Present

These points were not copied as standalone execution plans because current code already has a baseline. Gaps were still pulled into the new specs where the baseline is not production-complete.

| Area | Current baseline | Follow-up treatment |
|------|------------------|---------------------|
| Wait subscription and resume | `WaitSubscriptionService` and WAIT-style resume flow exist. | P2-017 adds event filter UX, user input, timeout branches, and operator rerun tooling. |
| Event report path | Event reporting writes `event_log` and routes canvas triggers. | P1-005 through P1-005A3 add CDP write keys, ingestion contracts, event log schema, attribute discovery, internal events, and UI entry points; P2-016 adds sink metrics, retention, and analytics. |
| Execution lanes and admission | `ExecutionLaneResolver` and Redis/ZSET-style admission concepts exist. | P1-004 hardens 3000; P2-015 blocks 4000 until lane isolation, adaptive retry, and bulkheads are proven. |
| Some channel-like handlers | In-app, coupon, reach, recommendation, and AI next-best-action handlers exist. | P1-008 turns them into connector-backed, rate-limited, fallback-aware capabilities. |
| Frontend editor and trace panels | Editor, config panel, and trace UI exist. | P1-007 fixes state/API/type contracts; P2-017 adds variable picker, full timeline, path highlight, and rerun. |

## Newly Added Specs And Plans

| Priority | Capability | Why it was added | Spec | Plan |
|----------|------------|------------------|------|------|
| P0-003 | Delivery Outbox, Receipts, And Reconciliation | Delivery queue/outbox, provider receipts, DLQ recovery, and reconciliation are production safety foundations. | [Spec](specs/p0-003-delivery-outbox-receipts-and-reconciliation.md) | [Plan](plans/p0-003-delivery-outbox-receipts-and-reconciliation-plan.md) |
| P0-004 | DAG Side-Effect Idempotency And Context Bounds | Handler idempotency, staged context commit, context size caps, profile snapshots, loop guards, and subflow guards must precede rerun and scale work. | [Spec](specs/p0-004-dag-side-effect-idempotency-and-context-bounds.md) | [Plan](plans/p0-004-dag-side-effect-idempotency-and-context-bounds-plan.md) |
| P0-005 | Production Operability And Runtime Gates | CI gates, production config, dashboards, alerts, runbooks, and secured ops endpoints are release blockers. | [Spec](specs/p0-005-production-operability-and-runtime-gates.md) | [Plan](plans/p0-005-production-operability-and-runtime-gates-plan.md) |
| P1-004 | 3000 Hardening Profile Contract And Evidence | Existing 3000 checklist needs machine-readable profile contracts, protected-lane rules, required failure profiles, and retained evidence manifests. | [Spec](specs/p1-004-3000-hardening-profile-contract-and-evidence.md) | [Plan](plans/p1-004-3000-hardening-profile-contract-and-evidence-plan.md) |
| P1-004B | 3000 Hardening Stop Gate Evaluator | Metric samples need deterministic PASS/STOP evaluation with named Redis, MySQL, MQ, retry, DLQ, and protected-lane gates. | [Spec](specs/p1-004b-3000-hardening-stop-gate-evaluator.md) | [Plan](plans/p1-004b-3000-hardening-stop-gate-evaluator-plan.md) |
| P1-004C | Execution Lane Metrics And Registry Guards | Runtime admission and lane behavior need observable registry, active-lane, retry-backlog, and protected routing coverage before the 3000 gate can be trusted. | [Spec](specs/p1-004c-execution-lane-metrics-and-registry-guards.md) | [Plan](plans/p1-004c-execution-lane-metrics-and-registry-guards-plan.md) |
| P1-004D | 3000 Concurrency Runbook And Baseline Gate | Operators need the exact baseline command, profile execution command, stop gates, rollback, degradation, and 4000-block policy in a runbook. | [Spec](specs/p1-004d-3000-concurrency-runbook-and-baseline-gate.md) | [Plan](plans/p1-004d-3000-concurrency-runbook-and-baseline-gate-plan.md) |
| P1-005 | CDP Write Key Management And Authentication | Tenant write-key governance must exist before SDK/server ingestion. | [Spec](specs/p1-005-cdp-write-key-management-and-authentication.md) | [Plan](plans/p1-005-cdp-write-key-management-and-authentication-plan.md) |
| P1-005A | CDP Event Log And Idempotent Track | CDP event collection needs a separate authenticated endpoint and enriched event log before downstream CDP work. | [Spec](specs/p1-005a-cdp-event-log-and-idempotent-track.md) | [Plan](plans/p1-005a-cdp-event-log-and-idempotent-track-plan.md) |
| P1-005A2 | Event Attribute Discovery And Internal CDP Event | Attribute review and downstream consumers need pending schema rows and a compact internal CDP event. | [Spec](specs/p1-005a2-event-attribute-discovery-and-internal-cdp-event.md) | [Plan](plans/p1-005a2-event-attribute-discovery-and-internal-cdp-event-plan.md) |
| P1-005A3 | Event Config Write Key And Attribute Review UI | Operators need write-key and pending attribute entry points after backend APIs exist. | [Spec](specs/p1-005a3-event-config-write-key-and-attribute-review-ui.md) | [Plan](plans/p1-005a3-event-config-write-key-and-attribute-review-ui-plan.md) |
| P1-005B | Webhook Subscription Schema And Signing | Webhook storage, callback validation, and signing are the first webhook activation slice. | [Spec](specs/p1-005b-webhook-subscription-schema-and-signing.md) | [Plan](plans/p1-005b-webhook-subscription-schema-and-signing-plan.md) |
| P1-005B2 | Webhook Dispatch Retry And Delivery Log | Signed dispatch, retry classification, and delivery logs can be implemented independently after schema/signing. | [Spec](specs/p1-005b2-webhook-dispatch-retry-and-delivery-log.md) | [Plan](plans/p1-005b2-webhook-dispatch-retry-and-delivery-log-plan.md) |
| P1-005B3 | Webhook Subscription API And Operator UI | Operator management API and UI follow once subscription storage and dispatcher behavior exist. | [Spec](specs/p1-005b3-webhook-subscription-api-and-operator-ui.md) | [Plan](plans/p1-005b3-webhook-subscription-api-and-operator-ui-plan.md) |
| P1-005C | Analytics Web SDK Foundation | Browser SDK basics are split out from ingestion so the client package can be implemented and tested independently. | [Spec](specs/p1-005c-analytics-web-sdk-foundation.md) | [Plan](plans/p1-005c-analytics-web-sdk-foundation-plan.md) |
| P1-006 | CDP Computed Profile Attributes | Computed profile attributes are the smallest governed profile-enrichment slice before advanced CDP automation. | [Spec](specs/p1-006-cdp-computed-profile-attributes.md) | [Plan](plans/p1-006-cdp-computed-profile-attributes-plan.md) |
| P1-006B | CDP Computed Tags And Lineage | Computed tags and dependency/impact checks are split out so behavior tags can ship without audience set operations. | [Spec](specs/p1-006b-cdp-computed-tags-and-lineage.md) | [Plan](plans/p1-006b-cdp-computed-tags-and-lineage-plan.md) |
| P1-006C | Realtime Audiences, Overlap, And Snapshots | Realtime membership, overlap, guarded set operations, and snapshots are separately testable audience capabilities. | [Spec](specs/p1-006c-realtime-audiences-overlap-and-snapshots.md) | [Plan](plans/p1-006c-realtime-audiences-overlap-and-snapshots-plan.md) |
| P1-007 | Canvas Editor Edge Projection And History | Edge dual-source risk is split into a smaller editor slice that can land before store/API refactors. | [Spec](specs/p1-007-canvas-editor-edge-projection-and-history.md) | [Plan](plans/p1-007-canvas-editor-edge-projection-and-history-plan.md) |
| P1-007B | Editor Store And Save Queue | Save-loop and high-churn editor state risks are isolated behind tested store and queue primitives. | [Spec](specs/p1-007b-editor-store-and-save-queue.md) | [Plan](plans/p1-007b-editor-store-and-save-queue-plan.md) |
| P1-007C | Frontend HTTP Client And Runtime Schemas | API cancellation, request dedupe, route-preserving 401, and runtime type checks are separately shippable frontend reliability work. | [Spec](specs/p1-007c-frontend-http-client-and-runtime-schemas.md) | [Plan](plans/p1-007c-frontend-http-client-and-runtime-schemas-plan.md) |
| P1-008 | Channel Connector Contract And Disabled State | Connector contract and disabled/sandbox behavior are split out before provider policy complexity. | [Spec](specs/p1-008-channel-connector-contract-and-disabled-state.md) | [Plan](plans/p1-008-channel-connector-contract-and-disabled-state-plan.md) |
| P1-008B | Provider Backpressure, Fallback, And Dedupe | Provider limits, fallback routing, and cross-canvas dedupe are independently testable policy work. | [Spec](specs/p1-008b-provider-backpressure-fallback-and-dedupe.md) | [Plan](plans/p1-008b-provider-backpressure-fallback-and-dedupe-plan.md) |
| P1-008C | Channel Connector Operator Surface | Connector mode, health, fallback decisions, and dedupe records need operator API/UI after backend policy exists. | [Spec](specs/p1-008c-channel-connector-operator-surface.md) | [Plan](plans/p1-008c-channel-connector-operator-surface-plan.md) |
| P2-015 | 4000 Concurrency Readiness And Lane Isolation | 4000 should remain blocked until 3000 evidence, lane isolation, async writer pressure, and bulkhead readiness exist. | [Spec](specs/p2-015-4000-concurrency-readiness-and-lane-isolation.md) | [Plan](plans/p2-015-4000-concurrency-readiness-and-lane-isolation-plan.md) |
| P2-016 | Analytics Event Trace Schema And Sink | Event/trace schema enrichment and OLAP-ready sink are the first analytics foundation slice. | [Spec](specs/p2-016-analytics-event-trace-schema-and-sink.md) | [Plan](plans/p2-016-analytics-event-trace-schema-and-sink-plan.md) |
| P2-016B | Analytics Retention And Archive Policy | Retention and archive controls are split from query features so data growth can be bounded independently. | [Spec](specs/p2-016b-analytics-retention-and-archive-policy.md) | [Plan](plans/p2-016b-analytics-retention-and-archive-policy-plan.md) |
| P2-016C | Bounded Analytics Query APIs | Event analysis, funnels, timelines, attribute distribution, alerts, and exports need tenant/date guards before UI work. | [Spec](specs/p2-016c-bounded-analytics-query-apis.md) | [Plan](plans/p2-016c-bounded-analytics-query-apis-plan.md) |
| P2-016D | Frontend Analytics Views And Export States | Frontend analytics views and export states can ship after backend query APIs are bounded. | [Spec](specs/p2-016d-frontend-analytics-views-and-export-states.md) | [Plan](plans/p2-016d-frontend-analytics-views-and-export-states-plan.md) |
| P2-017 | Template Renderer And Variable Picker | Template rendering and graph-aware variables are split out as the foundation for dynamic content. | [Spec](specs/p2-017-template-renderer-and-variable-picker.md) | [Plan](plans/p2-017-template-renderer-and-variable-picker-plan.md) |
| P2-017B | User Input And Wait Event UX | User input and wait-for-event UX share resume semantics and can be implemented separately from rerun/timeline tools. | [Spec](specs/p2-017b-user-input-and-wait-event-ux.md) | [Plan](plans/p2-017b-user-input-and-wait-event-ux-plan.md) |
| P2-017C | Connected Content Node | Safe external content fetch is isolated behind URL validation, timeout, payload cap, cache, and preview behavior. | [Spec](specs/p2-017c-connected-content-node.md) | [Plan](plans/p2-017c-connected-content-node-plan.md) |
| P2-017D | Test Users And Single User Rerun | Seed/test users and rerun modes require explicit reason and side-effect controls, separate from timeline UI. | [Spec](specs/p2-017d-test-users-and-single-user-rerun.md) | [Plan](plans/p2-017d-test-users-and-single-user-rerun-plan.md) |
| P2-017E | Execution Timeline And Batch Operations | Timeline inspection and batch list actions are split into an operator workflow slice. | [Spec](specs/p2-017e-execution-timeline-and-batch-operations.md) | [Plan](plans/p2-017e-execution-timeline-and-batch-operations-plan.md) |
| P2-018 | Runtime Architecture Migration Evidence | Broad rewrites need evidence, ADRs, proof commands, rollback notes, and child-spec gates before implementation. | [Spec](specs/p2-018-runtime-architecture-migration-evidence.md) | [Plan](plans/p2-018-runtime-architecture-migration-evidence-plan.md) |
| P2-019 | AI LLM Node Productionization | `AI_NEXT_BEST_ACTION` is a fallback stub; a governed `AI_LLM` node is bounded and useful before autonomous AI. | [Spec](specs/p2-019-ai-llm-node-productionization.md) | [Plan](plans/p2-019-ai-llm-node-productionization-plan.md) |
| P2-020 | Churn Prediction And Smart Timing Foundation | Churn probability and best send hour are the smallest useful predictive fields that existing audience and canvas rules can consume. | [Spec](specs/p2-020-churn-prediction-and-smart-timing-foundation.md) | [Plan](plans/p2-020-churn-prediction-and-smart-timing-foundation-plan.md) |

## Covered By Earlier Product-Evolution Specs

| Optimization topic | Existing product-evolution coverage |
|--------------------|-------------------------------------|
| Consent, suppression, blacklist, PII handling, role/tenant safety, production security | P0-001 Production Safety And Compliance |
| Error boundaries, loading/empty/error states, basic accessibility, visible frontend failures | P0-002 Frontend Resilience And A11y |
| Approval flow, publish/offline controls, fatigue controls, quiet hours, preview/dry-run, attribution basics | P1-001 Operating Loop And Effect Closure and P1-002 Operator Visibility And Testability |
| Mautic-style quick wins, template-center basics, import/export, simple operational UX improvements | P1-003 through P1-003G split audience send semantics, safe preview, import/export, project/folder metadata, and AI visibility policy; P2-005 covers the template center. |
| Collaboration, permissions, comments, change review, reporting workflow | P2-001 Collaboration Personalization And Reporting |
| Open API, integration foundation, developer surfaces | P2-002 Plugin And Integration Foundations and P2-008 Integration Readiness |
| Product analytics dashboard and feedback loops | P2-007 Analytics Command Center and P2-009 Product Usage Analytics And Feedback Loop |
| Audience data quality, dedupe, import/export hygiene | P2-010 Audience Operations And Data Quality |
| Editor productivity, navigation, grouped lists, guided experience, design-system work | P2-011 Editor Productivity Beyond Baseline and P2-014 Design System And Guided Experience |
| Channel timing, calendar, scheduling, channel intelligence | P2-012 Channel Intelligence And Scheduling |
| Autonomous AI agents, natural-language canvas generation, advanced personalization, ecosystem, billing, globalization, mobile/PWA, advanced privacy | P3-002 through P3-012 long-term strategy specs |

## Deferred Or Rejected As Immediate Execution

| Topic | Decision | Rationale |
|-------|----------|-----------|
| Enable 4000 production concurrency by config | Deferred | P2-015 requires P1-004 acceptance and its own readiness gates first. |
| Full WebFlux to MVC plus virtual threads rewrite | Deferred to evidence | High blast radius; P2-018 requires proof and rollback before a child spec. |
| Full monolith service split | Deferred to evidence | Depends on P0/P1 stabilization, observed traffic boundaries, and deployment rollback proof. |
| React Flow to X6 migration | Deferred | The product gap is workflow semantics and state contracts first; editor replacement needs evidence. |
| Groovy replacement as direct implementation | Deferred to evidence | Script compatibility and sandbox proof are required before migration. |
| Immediate Doris/ClickHouse migration | Deferred | P2-016 creates a sink and retention foundation first; storage choice belongs in P2-018 evidence. |
| Heatmap, session replay, App click analysis, predictive LTV, next purchase date, channel affinity | Deferred | Valuable later, but depends on event schema, OLAP storage, privacy controls, enough production history, and clear product demand. P2-020 only covers churn probability and best send hour. |
| Full Mobile SDK implementation | Deferred | Web/server ingestion and write-key governance are first; mobile SDK can follow from the P1-005A track protocol. |
| Advertising platform ROI, WeChat/social deep operations, CRM/service integrations | Deferred | These depend on connector foundation, partner credentials, data contracts, and commercial prioritization. |
| Broad 50-direction strategy backlog as direct plans | Rejected as immediate execution | Too broad for implementation. Useful items are mapped into P2/P3 specs; broad market ideas remain strategic. |
| Autonomous AI agents and natural-language journey generation | Deferred | P2-019 creates AI provider/template governance and P2-020 creates baseline predictive fields; agentic behavior still requires evaluation, approval, cost controls, attribution, and rollback evidence. |

## Coverage Conclusion

The optimization folder produced execution-oriented spec/plan pairs after the supplemental AI review. Items not added as new pairs fall into one of four buckets: already present in code with follow-up scope folded into a new spec, already covered by earlier product-evolution specs, deferred behind evidence or dependencies, or rejected as too broad for immediate execution.
