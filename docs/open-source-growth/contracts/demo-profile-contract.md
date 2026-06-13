# Demo Profile Contract

日期：2026-06-08

## Scope

Demo profile is the Open Source Growth first-run and Playground profile. It
must make the product easy to try without claiming production readiness or
weakening runtime boundaries.

This contract covers demo compose, mock providers, seed data, default demo
account, official plugin/template preload, dry-run, trace, DSL export, CLI
validation, and mock AI risk audit.

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY`: quickstart/playground docs, demo compose, WireMock/mock catalog,
  sample payloads, expected traces, and golden-path narrative. This state must
  not add backend runtime code, production/staging config, or database writes.
- `CURRENT_ENGINE_BRIDGE`: allowed only when the worker packet includes a
  complete Bridge Declaration naming the exact old service/API, exact old files,
  final DDD owner module, idempotency rule, removal gate, and rollback path. The
  bridge may seed demo-only config/data but must be reversible and isolated from
  production and staging.
- `DDD_FINAL_MODULE`: final implementation after `canvas-boot` owns runtime
  profile assembly, `canvas-context-canvas` owns demo draft/template/DSL APIs,
  and `canvas-context-execution` owns demo dry-run and trace APIs.

Final owner:

- Runtime profile and config assembly: `canvas-boot`.
- Demo account, tenant, and seed orchestration: `canvas-boot` through public
  context APIs.
- Official template import, draft creation, and DSL export:
  `canvas-context-canvas`.
- Dry-run, trace readback, and execution validation: `canvas-context-execution`.
- Plugin metadata and enablement consumed by demo seeds: `canvas-platform`.
- Playground/quickstart docs and golden-path evidence: `docs/open-source/**`.

Allowed adapters:

- `docker-compose.demo.yml`, WireMock files, and demo-only application profile
  files assigned by the worker packet.
- Mock provider adapters for message, email, approval, coupon, webhook, and AI.
- Canvas/execution/platform public APIs. Direct database seeding is forbidden
  unless a coordinator-provided `CURRENT_ENGINE_BRIDGE` explicitly names it and
  provides an idempotency rule.

## Demo And Production Safety

- Demo profile activation must be explicit through demo compose/profile only.
- Production and staging profiles must never default to mock providers, default
  demo accounts, demo secrets, or demo seed data.
- Demo must keep authentication, authorization, tenant isolation, publish rules,
  execution rules, trace persistence, and plugin enablement checks enabled.
- Default demo credentials are sample-only and tenant-scoped; they must not
  create a global admin bypass.
- No real SMS, email, approval, coupon, webhook, or AI provider may be required
  for the golden path.
- Demo seeds must enter the system through the same draft, template, publish
  precheck, dry-run, trace, and DSL APIs used outside demo mode.

## Golden Path

```text
docker compose -f docker-compose.demo.yml up
log in with the default demo account and tenant
import the new-user-welcome template as a draft canvas
dry-run with the sample payload
view execution trace
export Canvas DSL
run CLI validate against the exported DSL
run mock AI risk audit and keep the result as preview/draft only
```

Expected result:

- No real external provider is called.
- No production or staging default changes are required.
- Trace and dry-run evidence are produced by the normal execution path.
- AI output never publishes or overwrites a published canvas.

## Mirror documents

- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/program-coordination/execution-readiness-audit.md`

Mirror content must include demo profile location, demo config defaults, mock
provider wiring, seed ownership, golden-path APIs, and production safety
boundaries.

## Verification

Minimum DOCS_ONLY verification:

- `node tools/open-source-growth/guardrail-verifier.mjs`
- Contract review confirms no backend, frontend, DDD mirror, or coordination
  file was changed by the contract worker.

Minimum bridge/final verification before release:

- `docker compose -f docker-compose.demo.yml config` passes before release.
- Backend demo initialization tests prove seed idempotency and tenant scoping.
- Frontend build and playground smoke pass.
- Manual golden-path evidence records login, template import, dry-run, trace,
  DSL export, CLI validate, and mock AI risk audit.
- Scans show demo changes do not alter production/staging defaults, introduce
  real secrets, disable auth/tenant checks, or bypass execution/trace APIs.
