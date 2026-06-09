# Compatibility Contract Test Plan

This plan defines how to prove the DDD rewrite preserves externally observable
behavior for the first cutover.

---

## Principle

The first rewrite milestone is behavior-compatible. Architecture can change, but
clients should not need to change API paths, request shapes, response envelopes,
or critical semantics.

---

## Test Layers

### 1. HTTP Contract Tests

Verify:

```text
method
path
request body fields
query parameters
response envelope
status code behavior
error shape
tenant scoping
```

Critical route groups:

```text
auth and tenant context
canvas CRUD and versioning
canvas publish/offline/archive
execution trigger and trace
marketing campaign and readiness
CDP audience/tag/profile
BI dashboard/dataset/chart
risk strategy/list/decision
conversation session/webhook
```

### 2. Domain Behavior Compatibility Tests

Verify:

```text
status normalization
state transition permission
date range validation
limit clamping
readiness blocker generation
risk decision merge behavior
execution resume behavior
tenant data isolation
```

### 3. Persistence Compatibility Tests

Verify:

```text
same table names
same key columns
same JSON field interpretation
same default values when old behavior depended on them
same Flyway migration history
```

### 4. End-to-End Smoke Tests

Verify:

```text
create canvas
save draft
publish canvas
trigger execution
query trace
create marketing campaign
link required resource
evaluate campaign readiness
evaluate risk decision through execution node path
query CDP audience path
query BI dashboard path
process conversation webhook/session path
```

---

## Contract Test File Targets

Recommended locations:

```text
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
```

---

## Golden Response Policy

For critical routes, store a compact golden response shape in the test class or
test resource.

Golden shape should include:

```text
top-level R envelope
required data fields
status and error fields
tenant-sensitive behavior
```

Golden shape should not include:

```text
unstable timestamps unless explicitly normalized
database-generated IDs unless asserted by pattern
ordering unless the API contract requires ordering
```

---

## Required Compatibility Gates

Before cutover:

```bash
cd backend
mvn test -pl canvas-web -Dtest='*CompatibilityTest'
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
mvn clean install
```

Frontend:

```bash
cd frontend
npm run build
npm run test
```

---

## Failure Policy

When a compatibility test fails, classify it:

```text
rewrite bug
intentional behavior change requiring product approval
old behavior bug preserved intentionally for compatibility
test fixture mismatch
```

Default decision:

```text
preserve old behavior for first cutover
```

Intentional behavior changes require a separate spec and should not be mixed into
the DDD rewrite.
