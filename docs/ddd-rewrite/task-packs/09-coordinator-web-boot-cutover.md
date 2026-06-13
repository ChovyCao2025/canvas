# Task Pack 09: Coordinator Web and Boot Cutover

**Owner:** Main coordinator

**Program:** DDD modular rewrite

**Task id:** DDD-C09

**Readiness level:** R6 cutover

**Target backend state:** DDD_FINAL_MODULE

**Goal:** Move controllers to `canvas-web`, wire `canvas-boot`, run compatibility
tests, and remove the old `canvas-engine` runtime only after verification.

---

## Allowed Write Scope

```text
backend/canvas-web/**
backend/canvas-boot/**
backend/pom.xml
README.md
docs/INDEX.md
docs/ddd-rewrite/**
```

## Forbidden Changes

```text
new context business behavior rewrites
new plugin runtime implementations
new template import implementation
new DSL mapper implementation outside cutover compatibility wiring
new AI backend implementation
direct edits to applied Flyway migrations
production secret defaults
old canvas-engine patching after cutover begins
```

## Run-With Constraints

Can run with:

```text
read-only reviewers
docs-only release note workers that do not edit README.md or docs/INDEX.md
```

Must not run with:

```text
any DDD code-writing worker
DDD-C00
DDD-C07
OSG-W02
OSG-W07A through OSG-W07F
OSG-W09
OSG-W10
OSG-W11
OSG-W12
OSG-W14
any worker editing backend/**, frontend/**, docker-compose.demo.yml,
README.md, or docs/INDEX.md
```

Cutover is the single active backend/frontend writer in shared workspace mode.
Workers that discover missing API or compatibility behavior must return
`NEEDS_CONTEXT`; they must not patch old `canvas-engine` during cutover.

---

## Required Inputs

```text
docs/ddd-rewrite/inventory/http-api-inventory.md
docs/ddd-rewrite/inventory/persistence-ownership.md
docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
docs/open-source-growth/contracts/demo-profile-contract.md
all worker final responses
```

---

## Demo Profile Cutover Contract

The demo profile is assembled in `canvas-boot` during cutover. It must stay
explicitly demo-only and must not alter production or staging defaults.

Final placement:

- `canvas-boot` owns profile activation, demo configuration, demo account and
  tenant bootstrap orchestration, and calls into context APIs for seed setup.
- `canvas-platform` owns plugin manifest metadata and enablement consumed by
  demo seeds.
- `canvas-context-canvas` owns official template import, draft creation, publish
  precheck inputs, and Canvas DSL export used by the demo golden path.
- `canvas-context-execution` owns demo dry-run, execution validation, trace
  persistence/readback, and mock-mode execution evidence.
- `docs/open-source/**` owns playground and golden-path evidence records.

Required demo defaults:

- Demo activation is explicit through `docker-compose.demo.yml` or an
  equivalent `demo` profile only.
- Mock provider wiring may cover message, email, approval, coupon, webhook, and
  AI providers, but real provider secrets must not be required for the demo
  golden path.
- Default demo credentials are sample-only, tenant-scoped, and must not create
  a global admin bypass.
- Demo seeds must be idempotent and must enter through public context APIs,
  not direct database writes, unless a coordinator-approved bridge already
  names the exact old-engine files and removal gate.

Production safety:

- Production and staging profiles must not default to mock providers, demo
  accounts, demo secrets, or demo seed data.
- Demo setup must keep authentication, authorization, tenant isolation, plugin
  enablement, publish rules, execution rules, trace persistence, and audit
  behavior enabled.
- Mock AI/risk output is preview or draft evidence only; it must not publish or
  overwrite a published canvas.

Golden-path APIs to verify before release:

```text
login with default demo account and tenant
import official template as draft canvas
run publish precheck
run dry-run with sample payload
read execution trace
export Canvas DSL
run CLI validate against exported DSL
run mock AI risk audit without publishing
```

---

## Steps

- [ ] Move controllers into `canvas-web`.
- [ ] Replace controller dependencies with context facades or application APIs.
- [ ] Ensure controllers do not import `*DO`, `*Mapper`, or
      `adapter.persistence`.
- [ ] Wire `canvas-boot` dependencies on all context modules and `canvas-web`.
- [ ] Move Flyway resources into `canvas-boot` without renaming existing
      migrations.
- [ ] Configure mapper scanning for context `adapter.persistence` packages.
- [ ] Wire demo-only profile/config assembly in `canvas-boot` without changing
      production or staging defaults.
- [ ] Prove demo seed orchestration uses context APIs and remains idempotent
      and tenant-scoped.
- [ ] Create or port compatibility contract tests under
      `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/`:
      `CanvasApiCompatibilityTest`, `ExecutionApiCompatibilityTest`,
      `MarketingApiCompatibilityTest`, `CdpApiCompatibilityTest`,
      `BiApiCompatibilityTest`, `RiskApiCompatibilityTest`, and
      `ConversationApiCompatibilityTest`.
- [ ] Store compact golden response shapes in the compatibility tests or test
      resources.
- [ ] Run backend full build.
- [ ] Run frontend build and tests.
- [ ] Run compatibility contract test plan.
- [ ] Run demo compose config and record the demo golden path evidence.
- [ ] Remove old `canvas-engine` from active reactor only after all checks pass.

---

## Verification

```bash
cd backend
mvn clean install
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

```bash
cd frontend
npm run build
npm run test
```

Runtime smoke:

```text
GET /actuator/health
create canvas
save draft
publish canvas
trigger execution
query execution trace
list marketing campaigns
evaluate risk decision path
read CDP audience path
read BI dashboard path
process conversation webhook/session path
```

---

## Cutover Gate

Cutover is allowed only when:

- [ ] All context workers returned `DONE` or resolved concerns.
- [ ] Architecture tests pass.
- [ ] Guardrail checks pass.
- [ ] Controller compatibility tests pass.
- [ ] Full backend build passes.
- [ ] Frontend build and tests pass.
- [ ] Demo profile config, mock provider wiring, seed idempotency, tenant
      scoping, and golden path evidence pass.
- [ ] Old `canvas-engine` is not a compile dependency.
- [ ] Operations run command points to `canvas-boot`.

## Rollback

Revert only web adapter wiring, boot assembly, root Maven cutover, compatibility
tests, and docs index changes created by this task. If old `canvas-engine` was
removed from the active reactor, restore that reactor entry together with the
cutover rollback. Do not revert completed context worker output.

## Coordinator Response

Return:

```text
status:
files changed:
controllers moved:
boot wiring changed:
compatibility tests changed:
tests run:
guardrail checks:
open risks:
release or operations actions needed:
```
