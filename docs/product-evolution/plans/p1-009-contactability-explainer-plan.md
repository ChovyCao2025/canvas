# Contactability Explainer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a read-only contactability explainer that shows why a user/channel is allowed or blocked before a send.

**Architecture:** Reuse `MarketingPolicyService` for existing policy decisions. Add one non-mutating frequency preview method, one focused composition service, and one read-only WebFlux controller endpoint.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus mappers, Redis `StringRedisTemplate`, Reactor `Mono`, JUnit 5, Mockito, AssertJ.

**Implementation status (2026-06-05):** Completed. The backend explainer/controller and frontend contactability API/user-detail card were already present; this pass verified the slice and updated docs. Broader Maven focused suites remain blocked by unrelated `RedisBiQueryResultCacheTest` testCompile failures, so final backend verification used an isolated runner with 5/5 passing tests. Commit was intentionally skipped because the user did not request one.

---

## Spec Reference

- `docs/product-evolution/specs/p1-009-contactability-explainer.md`

## File Structure

**Backend**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java` - add read-only frequency preview using the existing frequency key semantics.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ContactabilityExplainerService.java` - compose ordered policy evidence.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ContactabilityController.java` - expose `/canvas/contactability/explain`.

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceFrequencyPreviewTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/ContactabilityExplainerServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ContactabilityControllerTest.java`

### Task 1: Non-Mutating Frequency Preview

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceFrequencyPreviewTest.java`

- [x] **Step 1: Write the failing frequency preview test**

Create `MarketingPolicyServiceFrequencyPreviewTest` with tests that mock Redis value operations, set the current counter value, and verify preview does not mutate Redis.

- [x] **Step 2: Run the test to verify red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MarketingPolicyServiceFrequencyPreviewTest
```

Expected: FAIL because `previewFrequency` does not exist.

- [x] **Step 3: Implement `previewFrequency`**

Add a public method beside `consumeFrequency` that reads the existing Redis key, parses the current count, and returns a `PolicyDecision` without increment/decrement/expire.

- [x] **Step 4: Run the test to verify green state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MarketingPolicyServiceFrequencyPreviewTest
```

Expected: PASS; Redis mutation methods are never called.

### Task 2: Contactability Composition Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ContactabilityExplainerService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/ContactabilityExplainerServiceTest.java`

- [x] **Step 1: Write the failing composition service test**

Create `ContactabilityExplainerServiceTest` with a mocked `MarketingPolicyService`. Verify the service returns checks in this order: `CONSENT`, `SUPPRESSION`, `CHANNEL`, `QUIET_HOURS`, `FREQUENCY`.

- [x] **Step 2: Run the test to verify red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ContactabilityExplainerServiceTest
```

Expected: FAIL because `ContactabilityExplainerService` does not exist.

- [x] **Step 3: Implement the service records and explanation logic**

Create request, report, and check result records inside `ContactabilityExplainerService`. Evaluate all checks read-only and set `report.allowed()` to true only when every check is allowed.

- [x] **Step 4: Run the test to verify green state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ContactabilityExplainerServiceTest
```

Expected: PASS; ordered checks and overall blocked state match the test.

### Task 3: Read-Only Controller Endpoint

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ContactabilityController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ContactabilityControllerTest.java`

- [x] **Step 1: Write the failing controller test**

Create `ContactabilityControllerTest` with a mocked `ContactabilityExplainerService`. Verify the controller sends defaults for quiet hours, timezone, frequency scope, frequency limit, and frequency window.

- [x] **Step 2: Run the test to verify red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ContactabilityControllerTest
```

Expected: FAIL because `ContactabilityController` does not exist.

- [x] **Step 3: Implement the controller**

Expose:

```text
GET /canvas/contactability/explain
```

Required query params:
- `userId`
- `channel`

Optional query params:
- `requireExplicitConsent`
- `quietStart`
- `quietEnd`
- `quietTimezone`
- `canvasId`
- `nodeId`
- `frequencyScope`
- `frequencyMax`
- `frequencyWindowSeconds`

- [x] **Step 4: Run the controller test to verify green state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ContactabilityControllerTest
```

Expected: PASS; the controller returns `R.ok(report)`.

### Task 4: Focused Verification

**Files:**
- All files above.

- [x] **Step 1: Run all contactability tests**

Actual verification used in this session:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" javac --release 21 -cp "backend/canvas-engine/target/classes:/tmp/canvas-p1-008-test-classes:$(cat /tmp/canvas-p1-008-test.classpath)" -d /tmp/canvas-p1-008-test-classes <P1-009 focused backend test sources>
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" java -cp "backend/canvas-engine/target/classes:/tmp/canvas-p1-008-test-classes:$(cat /tmp/canvas-p1-008-test.classpath)" P1009FocusedRunner
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm run test -- contactabilityApi.test.ts contactabilityPresentation.test.ts
```

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MarketingPolicyServiceFrequencyPreviewTest,ContactabilityExplainerServiceTest,ContactabilityControllerTest
```

Expected: PASS.

- [x] **Step 2: Inspect the diff**

Run:

```bash
git diff -- docs/product-evolution/specs/p1-009-contactability-explainer.md docs/product-evolution/plans/p1-009-contactability-explainer-plan.md backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ContactabilityExplainerService.java backend/canvas-engine/src/main/java/org/chovy/canvas/web/ContactabilityController.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceFrequencyPreviewTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/ContactabilityExplainerServiceTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/ContactabilityControllerTest.java
```

Expected: diff is limited to the contactability explainer slice.
