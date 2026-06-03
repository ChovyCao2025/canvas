# P2 - Product Opportunities From Filtered Scope

## Sources

- `product-strategy-dual-track-2026-05-31.md`
- `product-strategy-supplementary-dimensions-2026-05-31.md`
- `product-evolution-directions-2026-05-31.md`
- `product-evolution-directions-ext-2026-05-31.md`
- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`
- `mautic-comparison-2026-06.md`
- `mautic-capabilities-to-adopt.md`
- `plugin-candidate-list.md`

## Why This Exists

The first cleanup intentionally filtered broad strategy catalogs, large configuration inventories, competitor background, and code sketches. Some filtered content still has product value. This document captures those useful items as medium-term opportunities without promoting them to immediate P0/P1 execution.

## Opportunity Cards

### Message Template Center

- Source: strategy supplementary dimensions L, dual-track dimension L, best-practice roadmap, Mautic comparison.
- Useful point: consolidate message template CRUD, variable metadata, channel-specific adaptation, template preview, and template approval into one operator-facing capability.
- Why not P0/P1: basic template clone and preview are already covered in P1; a full template center needs message model, channel contracts, approval policy, and variable metadata decisions.
- Validation needed: confirm current handler template fields, template seed data, Liquid/template engine direction, approval reuse, and which channels need first-class adaptation.
- Suggested priority: P2.

### Sandbox, Demo Canvas, And Sales Enablement

- Source: dual-track dimension N, strategy extension customer journey, Mautic comparison.
- Useful point: provide demo canvases, mock data, isolated sandbox tenants, reset/expiry behavior, and sales materials so prospects and new operators can experience value quickly.
- Why not P0/P1: it improves adoption and sales motion, but does not block production safety or core operator workflow.
- Validation needed: decide whether sandbox equals isolated tenant, whether demo sends must bypass real channels, what data volume is safe, and who owns sales collateral.
- Suggested priority: P2, after template clone, attribution preview, and tenant isolation are stable.

### Analytics Command Center

- Source: dual-track dimension K, supplementary dimension K, product-evolution direction 15, interaction direction 29, best-practice roadmap.
- Useful point: evolve existing stats/funnel/trend into a command center with revenue, ROI, channel comparison, canvas comparison, report templates, scheduled reports, export, and chart drill-down.
- Why not P0/P1: lightweight attribution and basic export are already represented; deep reporting depends on stable event, conversion, cost, and channel receipt data.
- Validation needed: define metric dictionary, attribution scope, cost source, export limits, dashboard cache policy, and which reports operators actually need weekly.
- Suggested priority: P2.

### Integration Readiness

- Source: dual-track dimension M, product-evolution direction 2, strategy extension direction 11, plugin candidate list.
- Useful point: package inbound webhooks, outbound webhooks, API keys, SSO/OIDC decision, third-party data source management, and data sync as an integration foundation.
- Why not P0/P1: API keys and webhooks are platform leverage, but they must follow auth, audit, rate limit, tenant isolation, and integration secret handling.
- Validation needed: choose API key permission model, webhook signature and retry rules, SSO protocol order, data connector security rules, and partner demand ranking.
- Suggested priority: P2.

### Product Usage Analytics And Feedback Loop

- Source: product-evolution extension direction 15, interaction direction 19, strategy supplementary trend calibration.
- Useful point: track page/feature/action usage, collect in-app feedback, run product feature flags, and define alert rules and north-star metrics for product iteration.
- Why not P0/P1: this is internal product management infrastructure, not the primary marketing operator loop.
- Validation needed: select minimum event schema, privacy rules, sampling, retention, feedback ownership, and whether feature flags should be build-time or runtime.
- Suggested priority: P2.

### Audience Operations And Data Quality

- Source: supplementary dimension I, product-evolution direction 3, best-practice roadmap.
- Useful point: add audience union/intersection/difference, snapshots, freshness monitoring, audience health, user 360 refinements, data catalog basics, and data quality checks.
- Why not P0/P1: P1 already keeps audience estimate and last-touch attribution; deeper CDP operations need stable data semantics and potentially heavier compute.
- Validation needed: define snapshot lifecycle, freshness SLA, health metrics, large-audience performance limits, and which data quality rules should block sends.
- Suggested priority: P2.

### Editor Productivity Beyond Baseline

- Source: interaction direction 16, direction 18, direction 19, direction 27, direction 28.
- Useful point: add keyboard shortcuts, node search and locate, batch copy/move/delete, unified context menu, breadcrumbs, recent/favorites, field-level help, skeleton loading, rich inputs, async validation, auto-save, and table column customization.
- Why not P0/P1: several editor and table basics are already in P1; this card preserves the remaining productivity improvements that need UX sequencing.
- Validation needed: measure current operator pain points on large canvases, confirm shortcut conflicts, choose auto-save conflict behavior, and avoid adding controls before core flows are stable.
- Suggested priority: P2.

### Channel Intelligence And Scheduling

- Source: best-practice roadmap, Mautic comparison, product-evolution direction 5, plugin candidate list.
- Useful point: explore smart send time, smart throttling, channel routing, marketing calendar, channel cost tracking, and WeCom-first channel expansion.
- Why not P0/P1: receipt tracking and policy checks are earlier priorities; smart routing and STO require enough historical engagement and conversion data.
- Validation needed: define cold-start behavior, minimum training data, channel cost model, calendar conflict rules, and whether rule-based routing is enough before ML.
- Suggested priority: P2.

### Knowledge Base And Best-Practice Library

- Source: product-evolution extension direction 13, Mautic comparison, strategy extension customer journey.
- Useful point: turn template seed data into a discoverable best-practice library with industry/scenario browsing, contextual help, operator playbooks, FAQ, benchmarks, and case studies.
- Why not P0/P1: template clone is immediate; a knowledge product needs content ownership, taxonomy, and measured adoption loops.
- Validation needed: define content owner, template taxonomy, benchmark anonymization threshold, case-study approval, and whether help content lives in product or external docs.
- Suggested priority: P2.

### Design System And Guided Experience

- Source: product-evolution extension direction 14, interaction directions 19, 20, 24, 25.
- Useful point: create design tokens, consistent empty states, onboarding tours, content style rules, accessible form patterns, motion guidelines, and reduced-motion support.
- Why not P0/P1: P0 keeps resilience and accessibility stopgaps; full design-system work needs a broader frontend design decision and migration plan.
- Validation needed: audit current Ant Design token usage, define minimal design tokens, decide whether dark mode is user-facing, and verify accessibility scope against enterprise requirements.
- Suggested priority: P2.

## Explicitly Still Filtered

- Raw 61/93/154 configuration-item inventories remain source context, not implementation scope.
- "All do" strategy language remains rejected.
- Competitor TCO and market background are not todo items unless used in a specific pricing or positioning discovery.
- Source code sketches remain illustrative and must not be treated as implementation instructions.

## Acceptance Criteria

- Each opportunity has a source, product value, defer reason, validation need, and suggested priority.
- None of these opportunities override P0/P1 safety and operator-loop commitments.
- Any future implementation starts with a separate bounded spec instead of directly executing from archived strategy text.
