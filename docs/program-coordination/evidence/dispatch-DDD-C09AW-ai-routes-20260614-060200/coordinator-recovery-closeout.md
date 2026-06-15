# DDD-C09AW AI Routes Coordinator Recovery Closeout

## Dispatch

- Dispatch: `dispatch-DDD-C09AW-ai-routes-20260614-060200`
- Worker: Goodall `019ec2fe-f4c2-7242-8d30-a5bbc875a3c7`
- Scope: all 23 legacy `/ai` routes through `canvas-platform` and `canvas-web`
- Status: `DONE_WITH_CONCERNS`

## Recovery Notes

- A real code-writing worker was spawned before the dispatch moved to `RUNNING`.
- After one wait timeout, the coordinator inspected the reserved paths and evidence instead of continuing to wait.
- Goodall had written RED tests only:
  - `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AiApplicationServiceTest.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/ai/AiControllerCompatibilityTest.java`
- The coordinator closed Goodall after timeout; `close_agent` returned `previous_status: running`.
- The coordinator ran the RED test command and observed the expected compile failure because `AiCatalog` and `AiApplicationService` were missing.

## Implementation

- Added `AiFacade` in `canvas-platform`.
- Added `AiApplicationService` in `canvas-platform`.
- Added compact in-memory `AiCatalog` in `canvas-platform`.
- Added `AiController` in `canvas-web` exposing the 23 legacy `/ai` route shapes.
- Kept implementation free of old `canvas-engine` imports and old AI domain service coupling.
- Did not edit POM files.

## Verification

Command:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-platform,canvas-web -am -Dtest=AiApplicationServiceTest,AiControllerCompatibilityTest test
```

Result:

- `AiApplicationServiceTest`: 3 tests, 0 failures, 0 errors
- `AiControllerCompatibilityTest`: 4 tests, 0 failures, 0 errors
- Reactor result: `BUILD SUCCESS`

Command:

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result:

- `canvas-web` controllers: 19
- `canvas-web` endpoints: 294
- `/ai` removed from the top route gap candidates
- Global cutover remains blocked by route parity: old 142 controllers / 806 endpoints vs current 19 controllers / 294 endpoints

Command:

```bash
rg -n "canvas-engine|org\.chovy\.canvas\.domain\.ai|TenantContextResolver|AiDecisionModelService|ChurnPredictionService|AiPromptTemplateService|AiPromptEvaluationService|AiProviderModelRegistryService" backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/AiController.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AiFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AiApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AiCatalog.java
```

Result:

- Exit 1 with no matches.

Command:

```bash
node tools/program-coordination/check-dispatch-state.mjs .
bash docs/program-coordination/checks/program-coordination-checks.sh .
git diff --check -- backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AiFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AiApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AiCatalog.java backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AiApplicationServiceTest.java backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/AiController.java backend/canvas-web/src/test/java/org/chovy/canvas/web/ai/AiControllerCompatibilityTest.java docs/program-coordination/dispatch-state.json docs/program-coordination/progress-ledger.md docs/program-coordination/subagent-worker-packets.md docs/program-coordination/evidence/dispatch-DDD-C09AW-ai-routes-20260614-060200
```

Result:

- Dispatch state check passed.
- Program coordination checks passed.
- Scoped diff whitespace check passed.

## Accepted Concerns

- No normal Goodall worker-return packet.
- Coordinator recovered implementation locally after Goodall produced RED tests only.
- AI behavior is a compact in-memory compatibility seed; durable decision, prediction, prompt-template, provider, and model-registry semantics remain out of scope for this batch.
- Global DDD-C09 final cutover remains blocked by broader route parity gaps.
