# P2-088 - Marketing Integration Contract Registry Spec

Priority: P2
Sequence: 088
Source: user-requested marketing middle-platform glue-layer gap
Implementation plan: `../plans/p2-088-marketing-integration-contract-registry-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a tenant-scoped marketing integration contract registry so the marketing middle platform can govern API glue across provider-write gateways, monitoring connectors, credential onboarding, and internal marketing capabilities. The registry is not a provider adapter; it is the declared contract and ownership layer that makes external platform connections visible, queryable, and readiness-gated.

## External References

- OpenAPI Specification: https://spec.openapis.org/oas/
- AsyncAPI Specification: https://www.asyncapi.com/docs/reference/specification/latest
- OWASP API Security Top 10 2023: https://owasp.org/API-Security/editions/2023/en/0x11-t10/
- Kubernetes liveness, readiness, and startup probes: https://kubernetes.io/docs/concepts/workloads/pods/probes/
- OpenTelemetry HTTP semantic conventions: https://opentelemetry.io/docs/specs/semconv/http/
- RFC 9457 Problem Details for HTTP APIs: https://www.rfc-editor.org/rfc/rfc9457.html
- Google SRE Implementing SLOs: https://sre.google/workbook/implementing-slos/

## Current Baseline

P2-086 introduced the marketing platform control plane and P2-087 added a campaign master ledger. Provider writes and monitoring credentials already exist as separate modules, but there was no platform-level registry describing which API contracts connect which capability to which external or internal asset. That meant operators could see provider-write queues, but not the governance layer binding API roots, auth modes, retry policy, schema metadata, owners, and production environment status.

## Required Behavior

- Add additive persistence for tenant-scoped integration contracts with stable `contractKey`, provider family, source and target capability keys, asset key, direction, environment, auth mode, credential dependency, API root, owner team, status, SLA tier, timeout, retry policy JSON, schema contract JSON, metadata JSON, created/updated actor, and timestamps.
- Add additive audit-event persistence for contract revisions, event type, previous/new status, structured snapshots, changed fields, actor, and timestamps.
- Enforce unique contract keys per tenant.
- Normalize contract, capability, and asset keys into stable lower-case identifiers.
- Normalize provider family, environment, direction, auth mode, status, and SLA tier into upper-case values.
- Reject unsupported direction, environment, auth mode, status, timeout, and missing required fields.
- List contracts by tenant with optional status and provider-family filters.
- Archive contracts by tenant guard instead of hard deleting them.
- Persist health probe evidence per integration contract with probe key, environment, status, HTTP status code, latency, error/problem metadata, observed time, and structured evidence JSON.
- Persist append-only production probe observations separately from the latest-probe table so SLO windows can be evaluated without losing historical samples.
- Support automated HTTP readiness probes for active production contracts:
  - Only safe `GET` and `HEAD` probes are allowed.
  - 2xx responses record `PASS`, 3xx responses record `WARN`, and 4xx/5xx responses record `FAIL`.
  - Exceptions record `FAIL` with machine-readable problem/evidence metadata.
  - Relative API roots require an explicit configured probe base URL.
- Add a scheduler for automated contract probes:
  - Disabled by default.
  - Tenant, limit, operator, fixed delay, and lease TTL are configurable.
  - Distributed lease and local overlap guard prevent duplicate scans across instances.
- Bridge production probe outcomes into the existing marketing monitoring alert workflow:
  - `FAIL` opens or updates one tenant-scoped `INTEGRATION_CONTRACT_PROBE_FAILURE` alert per contract.
  - Repeated failures update the OPEN alert window, occurrence count, reason, and probe metadata instead of spamming duplicates.
  - `PASS` auto-resolves the matching OPEN probe-failure alert with recovered probe evidence.
  - Newly opened probe-failure alerts reuse the existing monitoring fanout channel service.
- Evaluate integration contract SLO burn-rate from append-only observations:
  - Default SLO targets derive from SLA tier and can be overridden from contract metadata.
  - Multi-window rules evaluate fast page, slow page, and ticket burn-rate thresholds over production probe observations.
  - Breached SLOs open or update one deduped `INTEGRATION_CONTRACT_SLO_BURN_RATE` monitoring alert per contract.
  - Recovered SLOs resolve matching OPEN burn-rate alerts and release the alert dedupe key for future incidents.
- Expose authenticated APIs:
  - `POST /canvas/marketing-integrations/contracts`
  - `GET /canvas/marketing-integrations/contracts`
  - `DELETE /canvas/marketing-integrations/contracts/{contractId}`
  - `GET /canvas/marketing-integrations/contracts/{contractId}/audit-events`
  - `POST /canvas/marketing-integrations/contracts/{contractId}/probes`
  - `GET /canvas/marketing-integrations/contracts/{contractId}/probes`
  - `GET /canvas/marketing-integrations/probes`
  - `POST /canvas/marketing-integrations/contract-probe-runs/scan`
  - `GET /canvas/marketing-integrations/contract-slo-evaluations`
  - Existing `GET /canvas/marketing-monitoring/alerts` surfaces OPEN integration probe and SLO alerts for the control plane.
- Feed active, active-production, blocked, and degraded contract counts into the marketing-platform control-plane evidence provider.
- Feed fresh production PASS/FAIL probe counts into the marketing-platform control-plane evidence provider.
- Add an Integration Contract Registry capability, integration asset, and contracts-to-provider-credentials lane to the control plane.
- Block marketing-platform readiness when there is no active production integration contract, when blocked/degraded contracts exist, when active production contracts lack fresh PASS probes, or when fresh FAIL probes exist.
- Add an operator-visible Integration Contract Registry section to `/marketing-platform` with KPIs, creation form, contract table, archive action, audit history, recent probe evidence, SLO burn-rate evaluation, OPEN integration alerts, manual probe recording, and an automated probe scan action.

## Non-Goals

- No live external provider adapter is introduced in this slice.
- No contract version negotiation or OpenAPI/AsyncAPI parser is introduced in this slice.
- No credential secrets are stored in the contract registry; contracts only reference credential dependencies.

## Production Follow-Up

The registry now provides declared contracts, runtime probe evidence, append-only SLO observations, multi-window burn-rate evaluation, and deduped monitoring-alert bridges for probe failures, SLO breaches, and recovery. The next production-hardening slices should add provider-specific signed probe adapters, trace/span export, contract version negotiation, and real provider semantic certification. Generic HTTP probes prove endpoint reachability, but they do not validate every provider-specific write semantic.

## Acceptance Criteria

- Schema tests prove the registry table, tenant unique key, status/provider/asset indexes, and JSON metadata columns exist.
- Service tests prove tenant scoping, key normalization, enum validation, timeout bounds, JSON serialization, upsert behavior, filters, audit revision writes, audit listing, and archive tenant guard.
- Controller tests prove tenant context and operator identity are passed into create, list, and archive APIs.
- Control-plane tests prove integration contract runtime evidence affects capability, asset, lane, actions, and readiness blockers.
- Probe service and automation tests prove PASS/FAIL evidence is persisted, only active production contracts are scanned, scan limits are bounded, and exceptions become FAIL evidence.
- HTTP probe client tests prove 2xx, 5xx, relative-base-url, and unsafe-method behavior.
- Scheduler tests prove disabled, enabled, lease-denied, and overlap-guard behavior.
- Alert bridge tests prove production FAIL probes create or update deduped OPEN monitoring alerts, PASS probes resolve matching OPEN alerts, and new failures dispatch through existing fanout channels.
- SLO tests prove probe observations are append-only, multi-window burn-rate evaluation can trigger PAGE/TICKET states, and SLO alerts are deduped and fanout-dispatched on first breach.
- Frontend API tests prove registry endpoints are called with the expected paths, params, and payloads.
- Frontend page tests prove the Integration Contract Registry section, KPIs, create action, audit history, probe evidence, SLO burn-rate evidence, OPEN probe/SLO alerts, manual probe recording, and automated probe scan action appear in `/marketing-platform`.
- Focused backend compile and tests pass for the registry, campaign ledger, and control-plane slice.
- Focused frontend tests and production build pass for the marketing-platform page and API service.
