# DDD-W08 Execution Worker Return

date: 2026-06-10
task id: DDD-W08
dispatch id: dispatch-DDD-W08-execution-20260610-120106
status: DONE_WITH_CONCERNS
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004
assigned task pack: docs/ddd-rewrite/task-packs/08-worker-execution.md

## Worker And Review Trail

- Initial worker: multi_agent_v1-worker Copernicus 019eafb4-0e31-7233-a9a4-143434510434.
- Copernicus returned NEEDS_CONTEXT for a canvas contract dependency; coordinator authorized only `backend/canvas-context-execution/pom.xml` to depend on `org.chovy:canvas-context-canvas`.
- Copernicus later returned DONE_WITH_CONCERNS, but coordinator verification found required DDD-W08 gaps in trace persistence wiring, Redis trigger routing, RocketMQ boundaries, and handler coverage.
- Copernicus was re-dispatched, continued editing, then did not return a final canonical packet. Coordinator closed the agent while still running and completed the remaining TDD fixes.
- Spec review: Lovelace 019eaffd-ae25-72c0-b872-9ad90943f5d6 returned SPEC_PASS and allowed DONE_WITH_CONCERNS.
- Quality review: Hegel 019eb009-0c6c-7ac3-9627-cb61efb93cf0 returned QUALITY_FAIL for versionId persistence, duplicate resume insert, aggregate early execution, and Redis wildcard routing.
- Focused re-review: Nietzsche 019eb016-2df0-7692-8dec-b8ffdfea6e43 returned QUALITY_PASS after fixes and confirmed DDD-W08 can close as DONE_WITH_CONCERNS.

## Files Changed

Primary write scope:

- `backend/canvas-context-execution/**`

Notable files:

- `backend/canvas-context-execution/pom.xml`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/node/NodeMetadataView.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/plugin/PluginEnablementView.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/ExecutionTraceView.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/CanvasExecutionApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/CanvasSchedulerApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/CanvasTriggerApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/ExecutionPublicationApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/ExecutionRecoveryApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/ExecutionTraceRepository.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/ExecutionTraceService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/external/RedisTriggerRouteAdapter.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/messaging/MqTriggerConsumer.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/messaging/RocketMqTriggerPublisher.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/persistence/MyBatisExecutionTraceRepository.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/DagRuntimeService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/NodeHandlerRegistry.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/NodeHandlerMigrationCatalog.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/*NodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/**`

## Contracts Changed

- Execution implements `org.chovy.canvas.canvas.api.ExecutionPublicationPort`.
- Execution implements `org.chovy.canvas.canvas.application.UserInputResumePort`.
- Runtime consumes `PublishedCanvasDefinition` from the canvas module and does not import canvas persistence.
- Execution trace reads expose `ExecutionTraceView` from execution API.
- Risk/CDP handlers remain dependency-gated with exact API type names in `NodeHandlerMigrationCatalog`.

## Old Classes Migrated

- DAG runtime parsing/validation and execution facade moved into `canvas-context-execution`.
- START/END/IF_CONDITION/WAIT/USER_INPUT/DIRECT_CALL/DIRECT_RETURN/SPLIT/AGGREGATE handlers moved into execution domain.
- Redis trigger route storage moved behind `TriggerRouteStore` with `RedisTriggerRouteAdapter`.
- RocketMQ trigger publish/consume/rejection boundaries moved into `adapter.messaging`.
- Execution trace persistence ownership moved behind `ExecutionTraceRepository` and `MyBatisExecutionTraceRepository`.

## New Public API

- `CanvasExecutionFacade`
- `ExecutionDryRunFacade`
- `NodeMetadataView`
- `PluginEnablementView`
- `ExecutionTraceView`

## Domain Model Changes

- `DagGraph`, `DagNode`, `DagParser`, `DagRuntimeService`, and `NodeHandlerRegistry` define execution-owned DAG runtime behavior.
- `NodeExecutionResult` carries success, pending, output, routes, and error state.
- Aggregate execution is gated until configured upstream nodes have completed.

## Persistence Ownership Changes

- Execution-owned DO/Mapper classes for execution, trace, wait subscription, request, stats, DLQ, MQ rejection, rerun audit, and user-input resume audit live under `adapter.persistence`.
- `MyBatisExecutionTraceRepository` writes execution rows with tenant/canvas/version/status and reads node trace rows back into `ExecutionTraceView`.
- Resume trace handling appends to an existing execution trace instead of inserting a second execution row.

## Tests Run

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=MyBatisExecutionTraceRepositoryTest,ExecutionTracePersistenceServiceTest,ExecutionRecoveryApplicationServiceTest,CanvasExecutionApplicationServiceTest,RedisTriggerRouteAdapterTest`
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
- `rg -n "org\\.chovy\\.canvas\\.(engine|dal)|canvas-engine|org\\.chovy\\.canvas\\.canvas\\.adapter|org\\.chovy\\.canvas\\.risk\\.adapter|org\\.chovy\\.canvas\\.cdp\\.adapter" backend/canvas-context-execution/src/main/java backend/canvas-context-execution/src/test/java`
- `node tools/open-source-growth/guardrail-verifier.mjs`
- `node tools/program-coordination/check-dispatch-state.mjs .`
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
- `git diff --check`

## Verification Result

- Execution module Maven tests passed with 56 tests.
- Focused quality-fix regression suite passed with 13 tests.
- DDD guardrails passed; advisory matches only pre-existing risk TypeCompatibility names.
- Old engine/dal scan matched only `RuntimeMigrationEvidenceTest` negative assertions.
- OSG guardrail verifier returned ok.
- Coordination checks and dispatch-state verifier returned ok before final ledger/state write.
- `git diff --check` passed before final ledger/state write.

## Guardrail Checks

- Domain has no infrastructure imports.
- Non-persistence execution code does not import DO or Mapper classes.
- New modules do not import old `canvas-engine` internals.
- New module POMs do not depend on `canvas-engine`.

## Failure Modes Reviewed

- Missing `versionId` on `canvas_execution` insert.
- Resume trace duplicate insert / in-memory overwrite.
- Aggregate node executing before all configured upstream branches completed.
- Redis blank matchKey route not matching event-specific route lookup.
- WAITING node trace readback fidelity.

## Compatibility Evidence

- `ExecutionPublicationApplicationService` validates published canvas definitions and registers trigger/scheduler state before saving the runtime definition.
- `ExecutionRecoveryApplicationService` accepts canvas user-input resume requests and records resume output through execution trace service.
- `CanvasExecutionApplicationService` triggers executions from stored published definitions and exposes traces through the execution facade.

## Temporary Bridges

- Execution depends on `canvas-context-canvas` only for the frozen canvas/execution API contracts.
- Runtime definition storage is in-memory pending production cache/persistence wiring.
- RocketMQ wiring is boundary-level; annotated listener replacement and old web/engine bridge removal remain later integration/cutover work.
- Risk/CDP nodes are dependency-gated until exact cross-context API dependencies are authorized.

## Open Risks

- `RESUMED` execution status persists as the existing success status code and reads back as `SUCCESS`; preserve literal `RESUMED` later only if the public trace API requires a distinct status.
- Redis adapter currently uses key scanning for route lookup/removal; replace with indexed keys before production cutover.
- Execution definition repository durability and runtime cache invalidation must be wired before G10/G12 cutover.
- Old `canvas-engine` MQ/listener/web surfaces still exist until later bridge/cutover packs.
- Risk/CDP handler implementations require dependency/API owner approval before replacing dependency-gated catalog entries.

## Coordinator Actions Needed

- Close DDD-W08 as DONE_WITH_CONCERNS.
- Open OSG-C07 plugin registry decision as the next coordinator gate before G10 public extension/API workers.
- Keep execution-owned plugin/template/DSL/AI backend workers blocked until OSG-C07 and G10 evidence pass.
- Keep DDD-C09 final cutover blocked until all context and ecosystem work integrate.

## Ledger Update

- Active dispatch registry cleared.
- DDD-W08 worker board status set to DONE_WITH_CONCERNS.
- Readiness advanced to R5 / G9 execution integrated.

## Rollback Path

- Revert files under `backend/canvas-context-execution/**`.
- Restore from backup `backup/pre-ddd-osg-20260609-222054` only if a full pre-rewrite restore is required.
