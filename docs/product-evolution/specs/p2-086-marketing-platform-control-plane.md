# P2-086 - Marketing Platform Control Plane Spec

Priority: P2
Sequence: 086
Source: user-requested marketing middle-platform confirmation and local capability audit
Implementation plan: `../plans/p2-086-marketing-platform-control-plane-plan.md`

## Goal

Turn the implemented marketing-platform slices into an operator-visible middle-platform control plane. The first slice must make the existing domains discoverable as one production capability map, expose integration glue lanes, and highlight configuration gaps without pretending the full marketing middle platform is complete.

## Current Baseline

P2-082 has implemented many bounded marketing domains: attribution and ROI, experiment governance, loyalty, SCRM workspace, private-domain sync, paid-media audience sync, AI decisioning, monitoring, provider credential/OAuth lifecycle, search marketing, creator collaboration, and programmatic DSP foundations.

The remaining gap is operational consolidation. Operators currently need to know which page or API owns each capability, and there is no single endpoint answering whether the marketing platform is ready, which glue lanes exist, and where real provider coverage is still configuration dependent.

## Provider Write Governance References

The glue-layer design is based on official provider write patterns rather than direct ad-hoc API calls:

- Google Ads API documents bulk mutate as repeated mutate operations and supports validate-only and partial failure modes for supported requests: <https://developers.google.com/google-ads/api/docs/mutating/bulk-mutate> and <https://developers.google.com/google-ads/api/docs/best-practices/partial-failures>.
- Display & Video 360 exposes line-item patch and bulk update endpoints with advertiser-scoped resource paths: <https://developers.google.com/display-video/api/reference/rest/v3>.
- Shopify Admin GraphQL documents idempotency guidance for mutation retry safety: <https://shopify.dev/docs/api/usage/implementing-idempotency>.

These references reinforce the production requirement that provider writes remain ledgered, idempotent, approval-gated, dry-run/validate-first where possible, and observable through the marketing middle-platform control plane.

## Integration Contract Health References

The integration contract registry must not be a static spreadsheet. It needs runtime health evidence that follows common API reliability conventions:

- OpenTelemetry HTTP semantic conventions define HTTP span, metric, and exception conventions, including status-code/error metadata such as `error.type`: <https://opentelemetry.io/docs/specs/semconv/http/>.
- RFC 9457 standardizes machine-readable Problem Details for HTTP APIs with fields such as `type`, `status`, `title`, and `detail`: <https://www.rfc-editor.org/rfc/rfc9457.html>.
- Kubernetes probe documentation defines liveness/readiness/startup probe patterns for automated runtime health checks: <https://kubernetes.io/docs/concepts/workloads/pods/probes/>.
- Google SRE guidance treats SLIs/SLOs as the basis for data-driven reliability decisions: <https://sre.google/workbook/implementing-slos/>.

These references justify keeping probe evidence as tenant-scoped runtime rows with HTTP status, latency, error/problem metadata, observation time, and structured JSON evidence. The control plane should only mark production integration contracts live when active production contracts have fresh passing probes and no fresh failing probes.

## Required Behavior

- Add an authenticated backend summary endpoint for the marketing platform control plane.
- Return tenant-scoped metadata with generated time, overall readiness, capability cards, runtime evidence signals, integration lanes, integration assets, readiness gate, and prioritized action items.
- Derive production readiness from tenant runtime evidence instead of a static checklist:
  - published journeys
  - active content releases
  - conversation work items
  - enabled monitoring sources
  - enabled alert channels
  - enabled paid-media destinations
  - active provider credentials
  - enabled search marketing sources
  - active creator campaigns
  - enabled programmatic DSP seats
  - published marketing BI dashboards
  - SEM, creator, and DSP provider mutation ledger totals
  - SEM, creator, and DSP pending/failed provider writes
  - active and production integration contract counts
  - blocked and degraded integration contract counts
  - fresh passing and failing production integration contract probe counts
- Include the existing production slices as first-class capabilities:
  - campaign master ledger
  - integration contract registry
  - journey orchestration
  - content lifecycle
  - SCRM workspace
  - monitoring and alert fanout
  - paid-media audience activation
  - search marketing write governance
  - creator collaboration write governance
  - programmatic DSP write governance
  - measurement and BI
  - provider credential/OAuth governance
- Distinguish live operator surfaces from API-only or provider-configuration-dependent capabilities.
- Mark glue-lane status as governed only when the source and target capabilities have live runtime evidence.
- Include an integration asset catalog for the middle-platform glue layer:
  - marketing integration contract registry
  - campaign master resource ledger
  - content release runtime resolver
  - monitoring provider ingestion
  - paid-media audience sync
  - search provider write gateway
  - creator provider write gateway
  - programmatic DSP provider write gateway
- Add an operator-facing Marketing Integration Contract Registry:
  - Tenant-scoped contract key, provider family, source capability, target capability, asset key, direction, environment, auth mode, credential dependency, API root, owner team, status, SLA tier, timeout, retry policy, schema contract, and metadata.
  - Status values must include `DRAFT`, `ACTIVE`, `DEGRADED`, `BLOCKED`, and `ARCHIVED`.
  - Contract create, update, and archive events must be revisioned and visible as operator audit history.
  - Active production contracts alone are insufficient for production readiness; the registry must also expose recent runtime probes.
- Add Marketing Integration Contract Probe evidence:
  - Persist tenant, contract id/key, probe key, environment, status, HTTP status code, latency, error type, RFC 9457-style problem fields, observation time, evidence JSON, and actor.
  - Support `PASS`, `WARN`, and `FAIL`.
  - Readiness must require fresh `PASS` probes for every active production contract and zero fresh `FAIL` probes.
  - The first freshness window is 24 hours.
- Add Marketing Integration Contract probe incident bridge:
  - Production `FAIL` probe runs must create or update one OPEN `INTEGRATION_CONTRACT_PROBE_FAILURE` monitoring alert per contract.
  - Duplicate failed probes must extend the alert window and evidence instead of creating alert noise.
  - Production `PASS` probe runs must resolve the matching OPEN probe-failure alert with recovery evidence.
  - Newly opened probe-failure alerts must reuse the existing marketing monitoring alert fanout channels.
- Add Marketing Integration Contract SLO burn-rate evidence:
  - Persist probe observations separately from latest probe-run evidence so SLO windows can be evaluated over historical outcomes.
  - Add an OPEN alert dedupe key on monitoring alerts so probe-failure and SLO burn-rate incidents update the same active incident instead of creating duplicates.
  - Evaluate production contract SLO windows from observation history and open or resolve `INTEGRATION_CONTRACT_SLO_BURN_RATE` alerts.
  - Expose current SLO evaluations through the integration contract API for the control plane.
- Add automated Marketing Integration Contract probe scanning:
  - Generic HTTP probe client supports safe `GET` and `HEAD` readiness probes.
  - Automated scan targets active production contracts only, records PASS/WARN/FAIL evidence, and converts exceptions into FAIL evidence.
  - Scheduler is disabled by default and supports configured tenant, limit, operator, fixed delay, distributed lease TTL, and local overlap guard.
  - Operators can trigger a bounded scan from `/marketing-platform`; scan completion refreshes recent probe evidence and readiness.
- Each integration asset must expose owner capability, asset type, provider family, API root, credential dependency, control points, gaps, runtime evidence, pending write count, and failed write count.
- Include a machine-readable readiness gate:
  - `BLOCKED` when required capabilities/assets are not configured or provider write failures require triage.
  - `DEGRADED` when required runtime evidence is present but API-only/live-adapter warnings remain.
  - `READY` when there are no blockers or warnings.
  - The gate must return `productionReady`, blocker count, warning count, blocker findings, and warning findings with item type, item key, route, title, and reason.
- Add a frontend `/marketing-platform` route and navigation item that renders the control-plane summary for operators.
- Render runtime evidence in the capability table and integration asset catalog so operators can see why a capability or glue asset is live, API-only, or configuration-required.
- Render the readiness gate as an operator-visible go-live panel with blocker and warning detail.
- Render OPEN integration probe alerts in the Integration Contract Registry section so operators can see current glue-layer incidents without leaving the marketing middle-platform control plane.
- Render integration SLO evaluations in the Integration Contract Registry section so operators can see triggered rules and burn-rate thresholds alongside probe evidence.
- Keep this slice additive. Do not move existing modules or rewrite P2-082 behavior.

## Non-Goals

- No new provider-specific live adapter is introduced in this slice.
- No campaign master-data ledger is introduced inside P2-086 itself. P2-087 introduces that ledger as the follow-up glue-layer slice.
- No existing endpoint is renamed.

## Acceptance Criteria

- Backend focused tests prove the summary contains the core middle-platform capabilities, glue lanes, and tenant context.
- Backend focused tests prove missing evidence yields configuration actions and present evidence promotes runtime-backed capabilities to live.
- Backend focused tests prove the integration asset catalog includes provider write gateway counts from SEM, creator, and DSP ledgers.
- Backend focused tests prove readiness gate status for blocked, degraded, and provider-write-failure states.
- Frontend focused tests prove readiness KPIs and status labels are stable.
- Frontend focused tests prove runtime evidence and integration assets are part of the control-plane contract and page rendering.
- Frontend focused tests prove readiness gate blockers and warnings render.
- The route is accessible from the authenticated app layout as `营销中台`.
- Backend focused tests prove the integration contract registry persists tenant-scoped contracts and health probe evidence.
- Backend focused tests prove production integration readiness is blocked when active production contracts are missing fresh PASS probes or have fresh FAIL probes.
- Backend focused tests prove automated probe scanning, HTTP probe behavior, and scheduler lease/overlap behavior.
- Backend focused tests prove probe failure alerts are deduped, dispatched, and resolved on recovery.
- Backend focused tests prove SLO observations, burn-rate evaluation, deduped SLO alerts, and recovery resolution.
- Frontend focused tests prove operators can see recent integration probe evidence, OPEN probe-failure alerts, SLO burn-rate evaluations, open contract audit history and probe recording from the registry, and run automated probe scans from the control plane.
