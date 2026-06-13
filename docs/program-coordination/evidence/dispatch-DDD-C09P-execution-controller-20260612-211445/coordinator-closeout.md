# DDD-C09P Coordinator Closeout

Date: 2026-06-13

## Dispatch

- dispatch id: `dispatch-DDD-C09P-execution-controller-20260612-211445`
- task id: `DDD-C09P`
- worker: `multi_agent_v1-worker Epicurus 019ebbfb-0c59-71d0-993d-1bf30c3b9db9`
- recovered by: coordinator
- closeout status: pending read-only review at time of writing

## Timeout Recovery

The coordinator resumed Epicurus and performed the ledger-required single wait.
The wait timed out. The worker handle was then closed; `close_agent` reported
previous status `pending_init`, and no worker-return packet existed.

The exact reserved files were present and were the only DDD-C09P code paths in
scope:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/ExecutionControllerCompatibilityTest.java`

## Recovered Implementation

The recovered implementation adds a compact production execution controller seed
in `canvas-web` backed by the final `CanvasExecutionFacade`.

In scope:

- `POST /canvas/execute/direct/{canvasId}`
- `GET /canvas/{canvasId}/execution/{executionId}/trace`
- old-style compatibility envelopes
- default tenant `7`
- direct execution trigger type `DIRECT_CALL`
- bad-request envelope mapping for missing/blank `userId` and facade
  `IllegalArgumentException`

Out of scope by dispatch boundary:

- management routes
- replay and rerun routes
- manual approval routes
- dry-run routes
- broader execution route parity

## Verification

Initial verification with the shell default Java failed before executing tests
because Maven used Java 8 against Java 21 class files:

```text
mvn -pl canvas-web -Dtest=ExecutionControllerCompatibilityTest,ExecutionApiCompatibilityTest test
=> failed before tests: class file version 65.0, runtime supports up to 52.0
```

After pinning `JAVA_HOME` to Java 21, running only `canvas-web` still failed
before test execution because the reactor dependency output was not on the
forked Surefire classpath:

```text
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -Dtest=ExecutionControllerCompatibilityTest,ExecutionApiCompatibilityTest test
=> failed before tests: NoClassDefFoundError: org/chovy/canvas/execution/api/CanvasExecutionFacade
```

The same focused test set passed when run with upstream modules included:

```text
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=ExecutionControllerCompatibilityTest,ExecutionApiCompatibilityTest test
=> BUILD SUCCESS; 9 tests, 0 failures, 0 errors, 0 skipped
```

The broader canvas-web compatibility set also passed with the recovered
production controller included:

```text
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest,ExecutionApiCompatibilityTest,BiApiCompatibilityTest,CdpApiCompatibilityTest,ExecutionControllerCompatibilityTest test
=> BUILD SUCCESS; 39 tests, 0 failures, 0 errors, 0 skipped
```

Dispatch-state validation passed:

```text
node tools/program-coordination/check-dispatch-state.mjs .
=> { "ok": true }
```

Cutover preflight remained globally blocked, as expected:

```text
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
=> current canvas-web 7 controllers / 32 endpoints; compatibility presentCount 7 / missingCount 0; cutoverReady=false
```

Remaining global blockers:

- `canvas-web` controller count `7` is below old `canvas-engine` web controller
  count `142`
- `canvas-web` endpoint count `32` is below old `canvas-engine` web endpoint
  count `806`

## Accepted Concerns

- The original worker did not return the required worker packet; closeout relies
  on repository evidence, focused tests, broader compatibility tests, preflight,
  and read-only review.
- `idempotencyKey` remains accepted in the request body but has no final
  `CanvasExecutionFacade.ExecutionRequestCommand` field in the current API.
- The compact seed intentionally does not cover dry-run, behavior trigger,
  management, replay, rerun, or manual approval execution routes.
- Global DDD-C09 cutover remains blocked by route parity gaps.
