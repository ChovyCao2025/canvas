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
all worker final responses
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
