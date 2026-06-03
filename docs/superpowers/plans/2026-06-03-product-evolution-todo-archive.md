# Product Evolution Todo Archive Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert `docs/product-evolution` from a mixed research folder into a clean priority-based todo workspace with processed source files archived.

**Architecture:** Create a dedicated `docs/product-evolution/todo/` hierarchy with `p0` through `p3` folders and a source-mapping index. Rewrite source content into concise execution-oriented todo documents, then move processed originals into `docs/product-evolution/archive/2026-06-03/`.

**Tech Stack:** Markdown documentation, shell validation commands, git.

---

### Task 1: Create The Product Evolution Todo Skeleton

**Files:**
- Create: `docs/product-evolution/todo/INDEX.md`
- Create directories: `docs/product-evolution/todo/p0`, `docs/product-evolution/todo/p1`, `docs/product-evolution/todo/p2`, `docs/product-evolution/todo/p3`, `docs/product-evolution/archive/2026-06-03`

- [ ] **Step 1: Create the directory structure**

Run:

```bash
mkdir -p docs/product-evolution/todo/p0 docs/product-evolution/todo/p1 docs/product-evolution/todo/p2 docs/product-evolution/todo/p3 docs/product-evolution/archive/2026-06-03
```

Expected: command exits successfully.

- [ ] **Step 2: Create the index skeleton**

Use `apply_patch` to create `docs/product-evolution/todo/INDEX.md` with this content:

```markdown
# Product Evolution Todo Index

Date: 2026-06-03

## Purpose

This folder contains filtered, execution-oriented todo documents extracted from the original `docs/product-evolution` research and strategy files. Original source documents are archived under `../archive/2026-06-03/` after processing.

## Priority Model

| Priority | Meaning |
|----------|---------|
| P0 | Production blockers, compliance/security red lines, white-screen or navigation-breaking UX defects, tenant isolation risks, and high-ROI already-built capabilities that only need exposure. |
| P1 | Near-term operating loop and usability work: templates, preview/dry-run, version visibility, runtime visibility, list/table usability, basic attribution and event foundations. |
| P2 | Medium-term platform capability: analytics depth, collaboration, integrations, plugin foundations, channel expansion, technical migration candidates, and data infrastructure. |
| P3 | Long-term strategy: AI agents, ecosystem commercialization, internationalization, mobile/PWA, advanced automation, privacy computing, and marketplace revenue sharing. |

## Active Todo Documents

### P0

### P1

### P2

### P3

## Source Processing Log

| Source file | Classification | Todo outputs | Archive path | Notes |
|-------------|----------------|--------------|--------------|-------|
```

- [ ] **Step 3: Verify skeleton exists**

Run:

```bash
find docs/product-evolution -maxdepth 3 -type d | sort
```

Expected output includes `docs/product-evolution/todo/p0`, `p1`, `p2`, `p3`, and `docs/product-evolution/archive/2026-06-03`.

### Task 2: Process Audit And Best-Practice Sources Into P0/P1 Work

**Files:**
- Read: `docs/product-evolution/product-audit-report-2026-05-31.md`
- Read: `docs/product-evolution/product-best-practice-roadmap-2026-05-31.md`
- Create: `docs/product-evolution/todo/p0/production-safety-and-compliance-stopgaps.md`
- Create: `docs/product-evolution/todo/p1/operating-loop-and-effect-closure.md`
- Modify: `docs/product-evolution/todo/INDEX.md`
- Move to archive: the two read source files

- [ ] **Step 1: Write the P0 stopgaps todo**

Use `apply_patch` to create `docs/product-evolution/todo/p0/production-safety-and-compliance-stopgaps.md` with this content:

```markdown
# P0 - Production Safety And Compliance Stopgaps

## Sources

- `product-audit-report-2026-05-31.md`
- `product-best-practice-roadmap-2026-05-31.md`

## Why P0

These items block safe production use or create immediate compliance, security, tenant isolation, or user-recovery risk. Broad roadmap items from the source files were filtered out; this document keeps only stopgaps that can be scoped as immediate work.

## Work Items

- Add frontend ErrorBoundary coverage, 404 route fallback, and usable 403 page.
- Put editor/stat/user deep-link pages back inside the main application layout so users retain navigation.
- Enforce tenant isolation at query boundaries, including tenant fields and MyBatis tenant interceptor coverage.
- Protect `/ops` and execution-sensitive endpoints with authentication and authorization.
- Add unsaved-change protection for browser unload and route navigation on key editing forms.
- Handle stub or misleading AI/recommendation nodes by removing them from production palettes or marking them as beta with clear execution behavior.
- Expose circuit breaker status and runtime degradation visibility to operators.
- Connect existing policy services, consent, suppression, and frequency checks into the actual send path.

## Deferred From Sources

- AI journey creation, predictive CLV/churn, marketing calendar, Content Cards, and industry packaging are not P0.
- "Full best-practice adoption" was rejected as too broad for immediate execution.
- Long-term commercial and ecosystem concepts are handled in P2/P3 documents.

## Dependencies

- Tenant isolation work depends on confirming current schema and DO field coverage.
- Policy-send-path work depends on tracing `AbstractSendMessageHandler` and policy service usage.

## Acceptance Criteria

- A React render exception no longer white-screens the entire app.
- Invalid routes show a 404 page and permission failures show a styled 403 page with a clear next action.
- Tenant-scoped reads and writes cannot access another tenant's canvas, audience, CDP, or notification data.
- `/ops` endpoints reject unauthenticated requests.
- Production node palettes do not silently expose hardcoded AI or recommendation stubs as reliable features.
- Operators can see circuit breaker state without reading logs.
```

- [ ] **Step 2: Write the P1 operating-loop todo**

Use `apply_patch` to create `docs/product-evolution/todo/p1/operating-loop-and-effect-closure.md` with this content:

```markdown
# P1 - Operating Loop And Effect Closure

## Sources

- `product-audit-report-2026-05-31.md`
- `product-best-practice-roadmap-2026-05-31.md`

## Why P1

These items make the platform operationally useful after P0 safety is addressed. They are important for an operator workflow but do not all block initial production hardening.

## Work Items

- Build template browsing and one-click clone from existing seeded templates.
- Add touch preview, audience estimate, test send, and dry-run visibility before publish.
- Add lightweight attribution with conversion event intake and last-touch reporting.
- Add global control group support for incremental effect measurement.
- Add canvas version visibility, diff, and rollback flows.
- Add channel receipt tracking for delivered, opened, clicked, bounced, and failed states.
- Add list search, filters, batch operations, empty states, and localized form validation.
- Add InApp notification delivery instead of log-only behavior.
- Expose audit logs through API and UI for operator review.

## Deferred From Sources

- Full multi-touch attribution, RFM, STO, AI content, AI journey creation, and prediction are deferred to P2/P3 unless a separate validated business priority pulls them forward.
- The roadmap's four-stage plan is treated as source context, not an execution commitment.

## Dependencies

- Attribution depends on conversion event intake and touchpoint recording.
- Control groups depend on attribution metrics and stable audience assignment.
- Template clone depends on existing template metadata and clone API validation.

## Acceptance Criteria

- Operators can create a canvas from a template without manual graph copying.
- Operators can preview likely audience and message output before publishing.
- Operators can answer whether a canvas produced a conversion in a lightweight, last-touch sense.
- Operators can inspect version changes and rollback a bad release.
```

- [ ] **Step 3: Update the index for these two sources**

Append these rows under `Source Processing Log` in `docs/product-evolution/todo/INDEX.md`:

```markdown
| `product-audit-report-2026-05-31.md` | Audit | `p0/production-safety-and-compliance-stopgaps.md`, `p1/operating-loop-and-effect-closure.md` | `../archive/2026-06-03/product-audit-report-2026-05-31.md` | P0/P1 extracted; broad competitor gap lists filtered. |
| `product-best-practice-roadmap-2026-05-31.md` | Best-practice roadmap | `p0/production-safety-and-compliance-stopgaps.md`, `p1/operating-loop-and-effect-closure.md` | `../archive/2026-06-03/product-best-practice-roadmap-2026-05-31.md` | "Full adoption" rejected; staged into safety and operating-loop work. |
```

Also add these links under `Active Todo Documents`:

```markdown
### P0

- [Production Safety And Compliance Stopgaps](p0/production-safety-and-compliance-stopgaps.md)

### P1

- [Operating Loop And Effect Closure](p1/operating-loop-and-effect-closure.md)
```

- [ ] **Step 4: Archive the two source files**

Run:

```bash
mv docs/product-evolution/product-audit-report-2026-05-31.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/product-best-practice-roadmap-2026-05-31.md docs/product-evolution/archive/2026-06-03/
```

Expected: both files no longer exist at the root of `docs/product-evolution` and exist in archive.

### Task 3: Process Supplementary, Interaction, And Runtime Quick-Win Sources

**Files:**
- Read: `docs/product-evolution/product-strategy-supplementary-dimensions-2026-05-31.md`
- Read: `docs/product-evolution/product-interaction-directions-2026-06-01.md`
- Read: `docs/product-evolution/product-interaction-directions-2026-06-02.md`
- Create: `docs/product-evolution/todo/p0/frontend-resilience-and-a11y-stopgaps.md`
- Create: `docs/product-evolution/todo/p1/operator-visibility-and-testability.md`
- Create: `docs/product-evolution/todo/p2/collaboration-personalization-and-reporting.md`
- Modify: `docs/product-evolution/todo/INDEX.md`
- Move to archive: the three read source files

- [ ] **Step 1: Write the P0 frontend resilience todo**

Use `apply_patch` to create `docs/product-evolution/todo/p0/frontend-resilience-and-a11y-stopgaps.md` with this content:

```markdown
# P0 - Frontend Resilience And A11y Stopgaps

## Sources

- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`
- `product-strategy-supplementary-dimensions-2026-05-31.md`

## Why P0

This work prevents white screens, unreachable routes, silent request failures, and basic accessibility failures. The sources propose many interaction improvements; only the baseline resilience items are P0.

## Work Items

- Add global, route-level, and widget-level ErrorBoundary coverage.
- Add 404 route fallback and replace bare 403 text with a styled page.
- Configure request timeout and cancellation for long-running or abandoned requests.
- Add offline detection and a clear offline banner.
- Classify error notifications into network, permission, server, and business errors.
- Add semantic `header`, `nav`, `main`, and skip-link structure to the application layout.
- Add screen-reader utility classes and route-change focus management.
- Add `aria-live` announcements for success, failure, and urgent errors.
- Fix the documented dependency inconsistency: user preference infrastructure is phase 2, not a phase 0 dependency.

## Deferred From Sources

- Page transitions, full motion system, report builder, mobile editor, CRDT, and dark mode are not P0.
- The 154-item interaction configuration inventory is not executable scope.

## Dependencies

- Error reporting can start with console logging and later connect to backend reporting.
- Focus management depends on current router structure.

## Acceptance Criteria

- A broken page component does not blank the whole app.
- Route misses and permission failures render recoverable pages.
- Timed-out or cancelled requests do not leave components stuck in loading state.
- Keyboard and screen-reader users have a main-content target and dynamic status announcements.
```

- [ ] **Step 2: Write the P1 operator visibility todo**

Use `apply_patch` to create `docs/product-evolution/todo/p1/operator-visibility-and-testability.md` with this content:

```markdown
# P1 - Operator Visibility And Testability

## Sources

- `product-strategy-supplementary-dimensions-2026-05-31.md`
- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`

## Why P1

The backend already has several useful capabilities that operators cannot see or use. Exposing them has high ROI after P0 safety work.

## Work Items

- Add execution request management UI where backend controllers already exist.
- Add message sending record search and detail UI.
- Add consent, suppression, channel preference, and frequency policy management UI.
- Add circuit breaker status panel and operational runtime visibility.
- Add canary publish UI using existing canary APIs.
- Add dry-run visualization using existing dry-run execution behavior.
- Add audience estimate before publish.
- Add version history and diff UI where backend version endpoints exist.
- Add canvas editor efficiency improvements: search/locate node, fit view, batch selection, unified context menu, and advanced setting collapse.
- Add table filtering, row selection, CSV/Excel export, fixed operation columns, and column customizer.

## Deferred From Sources

- Real-time collaborative editing, CRDT, full report builder, advanced chart drill-down, and mobile editor are P2/P3.
- User preference infrastructure is deferred to P2 despite the interaction dependency note.

## Dependencies

- Canary UI depends on confirming current canary endpoint contracts.
- Dry-run visualization depends on returned node execution structure; if insufficient, add only the minimal backend enrichment.
- Table export should enforce max row limits before enabling large exports.

## Acceptance Criteria

- Operators can inspect execution requests, message records, runtime breaker state, and canary state from UI.
- Operators can estimate audience and simulate execution before publishing.
- Operators can compare versions and identify material graph/config changes.
- Main operational lists support search/filter, row selection, and export.
```

- [ ] **Step 3: Write the P2 collaboration and reporting todo**

Use `apply_patch` to create `docs/product-evolution/todo/p2/collaboration-personalization-and-reporting.md` with this content:

```markdown
# P2 - Collaboration Personalization And Reporting

## Sources

- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`
- `product-strategy-supplementary-dimensions-2026-05-31.md`

## Why P2

These features improve team productivity and analytical depth but should follow the P0/P1 safety and operator-visibility work.

## Work Items

- Add edit locks, presence, canvas comments, share links, and change notifications.
- Add user preference infrastructure for theme, sidebar state, notification preferences, recent nodes, editor layout, and list defaults.
- Add onboarding tour and contextual help once the core flows are stable.
- Add behavior analytics, chart expansion, report export, chart/table linking, and report builder only after the data contracts are stable.
- Add message template market, variables metadata, and approval integration after basic template CRUD and clone flows are stable.
- Add audience operations, snapshots, freshness monitoring, and 360-view improvements after existing audience APIs are validated.

## Deferred From Sources

- L3 CRDT, full mobile editor, PWA push, and advanced AI reporting are not included here.
- AAA-level accessibility and full internationalization are P3 unless enterprise sales requires them sooner.

## Dependencies

- Collaboration depends on stable version and permission models.
- Reporting depends on event, attribution, and analytics data quality.
- Preferences depend on a user preference API and storage model.

## Acceptance Criteria

- Multiple operators can coordinate editing without silent overwrite.
- User layout and notification preferences persist across sessions.
- Reporting work has explicit data sources, dimensions, and export limits.
```

- [ ] **Step 4: Update the index for these three sources**

Add these active links under the existing P0/P1/P2 headings:

```markdown
- [Frontend Resilience And A11y Stopgaps](p0/frontend-resilience-and-a11y-stopgaps.md)
- [Operator Visibility And Testability](p1/operator-visibility-and-testability.md)
- [Collaboration Personalization And Reporting](p2/collaboration-personalization-and-reporting.md)
```

Append these source log rows:

```markdown
| `product-strategy-supplementary-dimensions-2026-05-31.md` | Supplementary product dimensions | `p0/frontend-resilience-and-a11y-stopgaps.md`, `p1/operator-visibility-and-testability.md`, `p2/collaboration-personalization-and-reporting.md` | `../archive/2026-06-03/product-strategy-supplementary-dimensions-2026-05-31.md` | High-ROI exposed-backend work retained; broad dimension catalog filtered. |
| `product-interaction-directions-2026-06-01.md` | Interaction scan | `p0/frontend-resilience-and-a11y-stopgaps.md`, `p1/operator-visibility-and-testability.md`, `p2/collaboration-personalization-and-reporting.md` | `../archive/2026-06-03/product-interaction-directions-2026-06-01.md` | Dependency inconsistency corrected; phase 0/1/2 split retained. |
| `product-interaction-directions-2026-06-02.md` | Interaction scan | `p0/frontend-resilience-and-a11y-stopgaps.md`, `p1/operator-visibility-and-testability.md`, `p2/collaboration-personalization-and-reporting.md` | `../archive/2026-06-03/product-interaction-directions-2026-06-02.md` | Error handling/a11y kept as P0; reporting and motion downgraded. |
```

- [ ] **Step 5: Archive the three source files**

Run:

```bash
mv docs/product-evolution/product-strategy-supplementary-dimensions-2026-05-31.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/product-interaction-directions-2026-06-01.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/product-interaction-directions-2026-06-02.md docs/product-evolution/archive/2026-06-03/
```

Expected: all three files move into archive.

### Task 4: Process Mautic And Plugin Research Into Adoption Todos

**Files:**
- Read: `docs/product-evolution/mautic-comparison-2026-06.md`
- Read: `docs/product-evolution/mautic-capabilities-to-adopt.md`
- Read: `docs/product-evolution/mautic-plugin-feasibility-analysis.md`
- Read: `docs/product-evolution/plugin-candidate-list.md`
- Create: `docs/product-evolution/todo/p1/mautic-inspired-quick-adoptions.md`
- Create: `docs/product-evolution/todo/p2/plugin-and-integration-foundations.md`
- Create: `docs/product-evolution/todo/p3/ecosystem-and-plugin-marketplace-strategy.md`
- Modify: `docs/product-evolution/todo/INDEX.md`
- Move to archive: the four read source files

- [ ] **Step 1: Write the P1 Mautic quick-adoption todo**

Use `apply_patch` to create `docs/product-evolution/todo/p1/mautic-inspired-quick-adoptions.md` with this content:

```markdown
# P1 - Mautic Inspired Quick Adoptions

## Sources

- `mautic-comparison-2026-06.md`
- `mautic-capabilities-to-adopt.md`

## Why P1

These are practical capabilities borrowed from Mautic that can improve operator safety and migration without adopting Mautic's full ecosystem model.

## Work Items

- Add segment sending mode: static locked audience vs dynamic refreshing audience.
- Improve message preview across text, web push, and selected user contexts.
- Add canvas import/export for environment migration and template reuse.
- Add basic project or folder grouping only if it does not duplicate existing canvas grouping work.
- Publish an internal AI capability policy and roadmap if AI features are exposed to operators.

## Fact Corrections

- Mautic version claims should be softened to current 7.x active line and 8.0 alpha expectation in 2026.
- Canvas AI claims in the sources are inconsistent: some files describe AI nodes as stubs while others say AI is implemented. Treat production AI as unverified until code and behavior are confirmed.

## Deferred From Sources

- Mautic partner ecosystem and public community strategy are P3.
- Full AI marketplace is P3.

## Dependencies

- Dynamic audience refresh depends on audience resolver performance and snapshot semantics.
- Import/export depends on graph schema versioning and safe secret handling.

## Acceptance Criteria

- Operators can explicitly choose whether a scheduled segment send locks or refreshes the audience.
- Canvas export/import preserves graph, node config, edge config, and version metadata without leaking environment secrets.
- Preview output masks sensitive data and cannot accidentally send real messages.
```

- [ ] **Step 2: Write the P2 plugin foundation todo**

Use `apply_patch` to create `docs/product-evolution/todo/p2/plugin-and-integration-foundations.md` with this content:

```markdown
# P2 - Plugin And Integration Foundations

## Sources

- `mautic-plugin-feasibility-analysis.md`
- `plugin-candidate-list.md`
- `mautic-comparison-2026-06.md`

## Why P2

Plugin architecture and integrations are valuable platform work, but they should follow core safety and operator workflows. The source files are feasible but overstate near-term P0 value for several plugins.

## Work Items

- Define plugin extension points for node handlers, channel adapters, data exporters, and rule/template packs.
- Add plugin metadata, lifecycle, configuration schema, enable/disable state, and compatibility checks.
- Start with internal/built-in plugin packaging before hot-loaded third-party code.
- Prioritize official plugins that reduce core coupling: WeCom channel adapter, data export connector, batch operation engine, and AI Gateway adapter.
- Add API key and webhook foundation for integration partners.

## Filtered Scope

- Runtime hot-loading, revenue sharing, public marketplace UI, plugin ratings, and ISV review workflow are not P2 foundation scope.
- Feishu and DingTalk plugins stay behind WeCom and data export unless customer demand changes.

## Dependencies

- Plugin safety depends on permission, audit, configuration validation, and clear classloader or remote-call boundaries.
- Channel plugins depend on the channel adapter abstraction.

## Acceptance Criteria

- Internal plugins can be registered, configured, enabled, disabled, and version-checked.
- Built-in handlers can continue to work without plugin migration.
- Plugin failures are isolated from core DAG execution where feasible.
```

- [ ] **Step 3: Write the P3 ecosystem todo**

Use `apply_patch` to create `docs/product-evolution/todo/p3/ecosystem-and-plugin-marketplace-strategy.md` with this content:

```markdown
# P3 - Ecosystem And Plugin Marketplace Strategy

## Sources

- `mautic-comparison-2026-06.md`
- `mautic-plugin-feasibility-analysis.md`
- `plugin-candidate-list.md`

## Why P3

Ecosystem and marketplace work is strategic and can create long-term defensibility, but it is not a near-term execution blocker.

## Work Items

- Define public plugin submission, review, signing, and support policies.
- Define ISV partner tiers and joint solution strategy.
- Define marketplace monetization, plugin revenue sharing, and enterprise support model.
- Define community documentation, SDKs, sample plugins, and sandbox environment.
- Revisit AI-agnostic positioning and public AI manifesto after internal AI capabilities are production-ready.

## Deferred From Sources

- Immediate implementation of public plugin marketplace UI is deferred until P2 plugin foundations are stable.
- Third-party hot-loaded code is deferred until the security model is validated.

## Dependencies

- Requires P2 plugin lifecycle, integration APIs, audit trail, and permission model.
- Requires business decision on marketplace ownership, support SLA, and revenue policy.

## Acceptance Criteria

- Marketplace launch has explicit governance, security, support, and commercial rules.
- Third-party plugin onboarding has a documented compatibility and review process.
```

- [ ] **Step 4: Update the index for the four research sources**

Add active links under P1/P2/P3 and append these rows:

```markdown
| `mautic-comparison-2026-06.md` | Competitor research | `p1/mautic-inspired-quick-adoptions.md`, `p2/plugin-and-integration-foundations.md`, `p3/ecosystem-and-plugin-marketplace-strategy.md` | `../archive/2026-06-03/mautic-comparison-2026-06.md` | Competitor facts converted to adoption and strategy todos; version claims softened. |
| `mautic-capabilities-to-adopt.md` | Adoption candidates | `p1/mautic-inspired-quick-adoptions.md` | `../archive/2026-06-03/mautic-capabilities-to-adopt.md` | Code snippets treated as illustrative, not implementation plan. |
| `mautic-plugin-feasibility-analysis.md` | Plugin feasibility research | `p2/plugin-and-integration-foundations.md`, `p3/ecosystem-and-plugin-marketplace-strategy.md` | `../archive/2026-06-03/mautic-plugin-feasibility-analysis.md` | Feasibility retained; hot-loading and marketplace deferred. |
| `plugin-candidate-list.md` | Plugin candidate list | `p2/plugin-and-integration-foundations.md`, `p3/ecosystem-and-plugin-marketplace-strategy.md` | `../archive/2026-06-03/plugin-candidate-list.md` | P0 plugin claims downgraded to P2 foundation or P3 ecosystem where appropriate. |
```

- [ ] **Step 5: Archive the four source files**

Run:

```bash
mv docs/product-evolution/mautic-comparison-2026-06.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/mautic-capabilities-to-adopt.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/mautic-plugin-feasibility-analysis.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/plugin-candidate-list.md docs/product-evolution/archive/2026-06-03/
```

Expected: all four files move into archive.

### Task 5: Process Strategy Roadmaps Into Staged Product Direction Todos

**Files:**
- Read: `docs/product-evolution/product-evolution-directions-2026-05-31.md`
- Read: `docs/product-evolution/product-evolution-directions-ext-2026-05-31.md`
- Read: `docs/product-evolution/product-strategy-dual-track-2026-05-31.md`
- Create: `docs/product-evolution/todo/p2/platform-product-evolution-workstreams.md`
- Create: `docs/product-evolution/todo/p3/long-term-ai-commerce-and-ecosystem-bets.md`
- Modify: `docs/product-evolution/todo/INDEX.md`
- Move to archive: the three read source files

- [ ] **Step 1: Write the P2 product evolution workstreams todo**

Use `apply_patch` to create `docs/product-evolution/todo/p2/platform-product-evolution-workstreams.md` with this content:

```markdown
# P2 - Platform Product Evolution Workstreams

## Sources

- `product-evolution-directions-2026-05-31.md`
- `product-evolution-directions-ext-2026-05-31.md`
- `product-strategy-dual-track-2026-05-31.md`

## Why P2

The strategy documents contain useful platform direction, but their "all in" scope and large configuration catalogs are not executable. This todo keeps medium-term workstreams that can become scoped plans after P0/P1 stabilization.

## Work Items

- Platformization: plugin extension points, developer portal basics, API keys, outbound webhooks, and low-code schema improvements.
- Data assets: data quality rules, data catalog basics, path analytics, lightweight report improvements, and event pipeline foundations.
- Channels: WeCom L1/L2, channel adapter abstraction, and channel cost/receipt tracking.
- Operations: approval flow expansion, audit timeline, dashboard command center, and alert rules.
- Knowledge: template market, best-practice library, contextual help, and operator playbooks.
- Integrations: inbound webhook, API key management, SSO/OIDC decision, and data source connection improvements.

## Filtered Scope

- 61, 93, and 154 configuration-item inventories are not implementation scope.
- Microservices, serverless, edge computing, multi-cloud, and full marketplace are deferred unless validated by scale or customer needs.
- The dual-track 60/40 resource split is strategy context, not a commitment.

## Dependencies

- Requires P0 tenant/security work and P1 operator loop visibility.
- WeCom channel work depends on channel adapter boundaries.
- Analytics work depends on event collection and data quality.

## Acceptance Criteria

- Each workstream has a bounded follow-up spec before implementation.
- No workstream enters implementation with "all directions all config" scope.
- Source strategy is traceable without keeping long-form strategy files in active todo.
```

- [ ] **Step 2: Write the P3 strategic bets todo**

Use `apply_patch` to create `docs/product-evolution/todo/p3/long-term-ai-commerce-and-ecosystem-bets.md` with this content:

```markdown
# P3 - Long-Term AI Commerce And Ecosystem Bets

## Sources

- `product-evolution-directions-2026-05-31.md`
- `product-evolution-directions-ext-2026-05-31.md`
- `product-strategy-dual-track-2026-05-31.md`

## Why P3

These ideas may shape the product's long-term positioning, but they require validated data foundations, commercial decisions, or ecosystem readiness before implementation.

## Work Items

- AI agents: copywriter, journey builder, analytics, channel optimizer, segment builder, and anomaly agent with human approval.
- AI-native operations: natural language canvas creation, natural language query, autonomous optimization, and self-healing suggestions.
- Commercial expansion: outcome-based pricing, marketplace commissions, value-added services, managed service mode, and customer success scoring.
- Industry expansion: retail, finance, education, and healthcare template packs and compliance profiles.
- Globalization: i18n framework, multi-timezone, multi-currency, regional compliance, and overseas channels such as WhatsApp or RCS.
- Advanced privacy and architecture: differential privacy, federated learning, TEE, serverless, multi-cloud, edge, and service split.

## Filtered Scope

- None of these should be implemented directly from the archived strategy documents.
- Each item requires a separate discovery or design cycle with explicit business owner, data readiness, and success metric.

## Dependencies

- Requires stable event, attribution, and analytics foundations for optimization and outcome pricing.
- Requires AI Gateway and governance before exposing autonomous agent behavior.
- Requires legal and business review before marketplace commissions, outcome pricing, or cross-border compliance.

## Acceptance Criteria

- Long-term bets are visible but separated from immediate todo queues.
- Future specs can point back to this document for strategic lineage.
```

- [ ] **Step 3: Update the index for the three strategy sources**

Add active links under P2/P3 and append these rows:

```markdown
| `product-evolution-directions-2026-05-31.md` | Strategy roadmap | `p2/platform-product-evolution-workstreams.md`, `p3/long-term-ai-commerce-and-ecosystem-bets.md` | `../archive/2026-06-03/product-evolution-directions-2026-05-31.md` | Broad 10-direction "all do" scope split and downgraded. |
| `product-evolution-directions-ext-2026-05-31.md` | Strategy extension | `p2/platform-product-evolution-workstreams.md`, `p3/long-term-ai-commerce-and-ecosystem-bets.md` | `../archive/2026-06-03/product-evolution-directions-ext-2026-05-31.md` | Directions 11-15 filtered into platform workstreams and long-term bets. |
| `product-strategy-dual-track-2026-05-31.md` | Strategy mother document | `p2/platform-product-evolution-workstreams.md`, `p3/long-term-ai-commerce-and-ecosystem-bets.md` | `../archive/2026-06-03/product-strategy-dual-track-2026-05-31.md` | Stage 0 overload reduced; dual-track strategy retained as archived lineage. |
```

- [ ] **Step 4: Archive the three strategy files**

Run:

```bash
mv docs/product-evolution/product-evolution-directions-2026-05-31.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/product-evolution-directions-ext-2026-05-31.md docs/product-evolution/archive/2026-06-03/
mv docs/product-evolution/product-strategy-dual-track-2026-05-31.md docs/product-evolution/archive/2026-06-03/
```

Expected: all three files move into archive.

### Task 6: Process Technical Whitepaper Into Migration Candidates

**Files:**
- Read: `docs/product-evolution/tech-selection-whitepaper.md`
- Create: `docs/product-evolution/todo/p2/technical-migration-candidates.md`
- Create: `docs/product-evolution/todo/p3/long-term-architecture-evolution.md`
- Modify: `docs/product-evolution/todo/INDEX.md`
- Move to archive: `docs/product-evolution/tech-selection-whitepaper.md`

- [ ] **Step 1: Write the P2 technical migration candidates todo**

Use `apply_patch` to create `docs/product-evolution/todo/p2/technical-migration-candidates.md` with this content:

```markdown
# P2 - Technical Migration Candidates

## Sources

- `tech-selection-whitepaper.md`

## Why P2

The whitepaper contains substantial architecture migration proposals. They should be treated as candidate migration work, not product feature commitments, and each needs its own validation plan before implementation.

## Work Items

- Validate PowerJob migration for dynamic schedule creation and deletion.
- Validate virtual-thread executor replacement for Disruptor in task distribution.
- Validate RocketMQ topic split and delivery Outbox pattern.
- Validate deterministic audience mapping plus Redis BITMAP for collision-free audience membership.
- Validate Spring MVC plus virtual threads and command-style DAG engine as a combined architecture migration.
- Validate Aviator plus QLExpress replacement for Groovy script execution.
- Validate Doris and Flink CDC for reporting and trace data migration after product analytics requirements are stable.

## Fact Corrections

- Apache Doris 4.1.0 exists by 2026-04, so the archived whitepaper's Doris 4.0 wording should be treated as a baseline recommendation, not a latest-version assertion.
- Flink CDC 3.6.0, PowerJob 5.1.2, and @antv/x6 3.1.7 should be rechecked at implementation time.

## Filtered Scope

- Direct @antv/x6 migration is not a P2 default; first improve React Flow where it can satisfy immediate product needs.
- Full service split, serverless, edge, and multi-cloud are P3.

## Dependencies

- Architecture migrations need code-level specs and regression test plans before any implementation.
- Data infrastructure work should follow confirmed analytics and trace data requirements.

## Acceptance Criteria

- Each migration candidate has a separate spec with current-code evidence, risk, rollback, and verification commands.
- No migration is started solely because the whitepaper recommends it.
```

- [ ] **Step 2: Write the P3 long-term architecture todo**

Use `apply_patch` to create `docs/product-evolution/todo/p3/long-term-architecture-evolution.md` with this content:

```markdown
# P3 - Long-Term Architecture Evolution

## Sources

- `tech-selection-whitepaper.md`

## Why P3

These architecture ideas may matter at scale but should not distract from immediate safety, usability, and operator workflow.

## Work Items

- Evaluate service split into editor, engine, CDP, analytics, admin, and billing after module boundaries are stable.
- Evaluate @antv/x6 migration only after React Flow shortfalls are proven by actual product needs.
- Evaluate Flink CEP behavior-trigger migration after Flink and event pipelines exist.
- Evaluate multi-cloud, serverless, edge, and data residency architecture only for validated deployment requirements.

## Dependencies

- Requires production traffic, customer deployment requirements, or measurable performance bottlenecks.
- Requires existing module and data contracts to be stable enough to split safely.

## Acceptance Criteria

- Long-term architecture concepts are preserved but do not enter active implementation without separate validation.
```

- [ ] **Step 3: Update the index for the technical source**

Add active links under P2/P3 and append this row:

```markdown
| `tech-selection-whitepaper.md` | Technical whitepaper | `p2/technical-migration-candidates.md`, `p3/long-term-architecture-evolution.md` | `../archive/2026-06-03/tech-selection-whitepaper.md` | Converted to migration candidates; version facts noted for recheck. |
```

- [ ] **Step 4: Archive the technical source**

Run:

```bash
mv docs/product-evolution/tech-selection-whitepaper.md docs/product-evolution/archive/2026-06-03/
```

Expected: the whitepaper moves into archive.

### Task 7: Final Index Cleanup And Validation

**Files:**
- Modify: `docs/product-evolution/todo/INDEX.md`
- Inspect: `docs/product-evolution/todo/**/*.md`
- Inspect: `docs/product-evolution/archive/2026-06-03/*.md`

- [ ] **Step 1: Ensure each priority section lists all generated todo documents**

Confirm `docs/product-evolution/todo/INDEX.md` contains these links:

```markdown
### P0
- [Production Safety And Compliance Stopgaps](p0/production-safety-and-compliance-stopgaps.md)
- [Frontend Resilience And A11y Stopgaps](p0/frontend-resilience-and-a11y-stopgaps.md)

### P1
- [Operating Loop And Effect Closure](p1/operating-loop-and-effect-closure.md)
- [Operator Visibility And Testability](p1/operator-visibility-and-testability.md)
- [Mautic Inspired Quick Adoptions](p1/mautic-inspired-quick-adoptions.md)

### P2
- [Collaboration Personalization And Reporting](p2/collaboration-personalization-and-reporting.md)
- [Plugin And Integration Foundations](p2/plugin-and-integration-foundations.md)
- [Platform Product Evolution Workstreams](p2/platform-product-evolution-workstreams.md)
- [Technical Migration Candidates](p2/technical-migration-candidates.md)

### P3
- [Ecosystem And Plugin Marketplace Strategy](p3/ecosystem-and-plugin-marketplace-strategy.md)
- [Long-Term AI Commerce And Ecosystem Bets](p3/long-term-ai-commerce-and-ecosystem-bets.md)
- [Long-Term Architecture Evolution](p3/long-term-architecture-evolution.md)
```

- [ ] **Step 2: Verify no source Markdown files remain at product-evolution root**

Run:

```bash
find docs/product-evolution -maxdepth 1 -type f -name '*.md' -print
```

Expected: no output.

- [ ] **Step 3: Verify all 13 original files are archived**

Run:

```bash
find docs/product-evolution/archive/2026-06-03 -maxdepth 1 -type f -name '*.md' | sort | wc -l
```

Expected: `13`.

- [ ] **Step 4: Verify todo files are present**

Run:

```bash
find docs/product-evolution/todo -type f -name '*.md' | sort
```

Expected output includes `INDEX.md` plus 12 generated todo documents.

- [ ] **Step 5: Search generated todo for blocked placeholders**

Run:

```bash
rg -n "TBD|TODO|PLACEHOLDER|待定|稍后补|不确定" docs/product-evolution/todo
```

Expected: no output.

- [ ] **Step 6: Inspect git status**

Run:

```bash
git status --short
```

Expected: additions under `docs/product-evolution/todo`, renames or delete/add pairs for archived source files, and no unintended code changes. The unrelated untracked architecture design file may still appear and must not be staged for this task.

- [ ] **Step 7: Commit the product-evolution restructure**

Run:

```bash
git add docs/product-evolution/todo docs/product-evolution/archive/2026-06-03 docs/product-evolution/*.md
git commit -m "docs: organize product evolution todo archive"
```

Expected: commit succeeds. If `docs/product-evolution/*.md` matches no files after moves, add the moved files explicitly with `git add -A docs/product-evolution` instead, then commit.

---

## Self-Review

Spec coverage:

- Dedicated `todo/p0|p1|p2|p3` folders are created in Task 1.
- Todo contains only rewritten execution-oriented documents in Tasks 2 through 6.
- Each source file is processed and moved to archive in Tasks 2 through 6.
- Unreasonable broad scope is explicitly filtered in each generated document.
- Validation commands are defined in Task 7.

Placeholder scan:

- The plan intentionally contains no `TBD`, `TODO`, `PLACEHOLDER`, or vague fill-in steps.

Type and path consistency:

- All generated paths live under `docs/product-evolution/todo` or `docs/product-evolution/archive/2026-06-03`.
- All 13 source filenames listed in the design appear in processing tasks.
