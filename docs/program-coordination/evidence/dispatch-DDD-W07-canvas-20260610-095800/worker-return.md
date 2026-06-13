# DDD-W07 Canvas Worker Return

Date: 2026-06-10

## Result

status: DONE_WITH_CONCERNS
task id: DDD-W07
dispatch id: dispatch-DDD-W07-canvas-20260610-095800
worker: multi_agent_v1-worker Volta 019eaf45-24aa-7fb3-876f-322261c31e6a
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004
assigned task pack: docs/ddd-rewrite/task-packs/07-worker-canvas.md

## Files Changed

All accepted implementation changes are under:

- backend/canvas-context-canvas/**

## Implemented Scope

- Canvas draft, version, publish, query, and project-folder application services.
- Canvas aggregate, version aggregate, runtime options, lifecycle policy, and repository ports.
- Canvas and canvas-version MyBatis persistence adapters and mapping tests.
- Published canvas definition assembly with runtime execution options and parsed node/edge views.
- Canvas-side user-input form/response authoring with `UserInputResumePort` as the execution boundary.
- User-input submit idempotency including a coordinator-added red/green fix for the stale pending race: only a conditional PENDING -> COMPLETED update may request execution resume.

## Contracts Changed

No DDD-C07 API shape was broken. `PublishedCanvasDefinition` is now populated with runtime options and parsed graph views. DDD-W07 added a canvas-owned `UserInputResumePort` / `UserInputResumeRequest` boundary for DDD-W08 execution integration.

## Old Classes Migrated

- Canvas draft/version/publish lifecycle pilot behavior from the old canvas domain.
- Project/folder metadata persistence assignment slice.
- UserInputService form/response authoring slice.

`UserInputResumeAuditDO` remains execution-owned and was not moved into canvas.

## Public API

- org.chovy.canvas.canvas.api.ExecutionPublicationPort
- org.chovy.canvas.canvas.api.PublishedCanvasDefinition
- org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider
- org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition
- org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition
- org.chovy.canvas.canvas.api.template.TemplateValidationPort
- org.chovy.canvas.canvas.api.ai.AiJourneyDraftProposal
- org.chovy.canvas.canvas.application.UserInputResumePort
- org.chovy.canvas.canvas.application.UserInputResumeRequest

## Verification

Commands run by coordinator after worker return:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=UserInputApplicationServiceTest
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
node tools/open-source-growth/guardrail-verifier.mjs
node tools/program-coordination/check-dispatch-state.mjs .
git diff --check
```

Results:

- `UserInputApplicationServiceTest` red/green verified the stale pending race fix; final run passed 5 tests.
- `canvas-context-canvas` Maven tests passed: 29 tests, 0 failures, 0 errors.
- DDD guardrails passed; advisory remains only for pre-existing `canvas-context-risk` `validateTypeCompatibility` names.
- OSG guardrail verifier returned `{ "ok": true }`.
- Dispatch-state verifier returned `{ "ok": true }` before closure.
- `git diff --check` passed.

Evidence artifacts:

- backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.application.UserInputApplicationServiceTest.xml
- backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.application.CanvasPublishApplicationServiceLifecycleTest.xml
- backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.application.CanvasQueryApplicationServiceTest.xml
- backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.adapter.persistence.CanvasPersistenceMappingTest.xml
- backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.adapter.persistence.UserInputPersistenceMappingTest.xml

## Guardrails

- New canvas module production code does not import old `canvas-engine` internals.
- Canvas domain/application code does not depend on old DAL, Redis, scheduler, cache, or concrete execution services.
- MyBatis and DO classes stay in `adapter.persistence`.
- User-input resume/audit remains outside canvas persistence ownership and is exposed through a port.

## Accepted Concerns

- Robust publication outbox/retry remains follow-up infrastructure work before cutover.
- Canvas version number race still needs DB uniqueness or locking for `(canvas_id, version)` or an equivalent migration.
- DDD-W08 must implement `ExecutionPublicationPort` and `UserInputResumePort`.
- DDD-C09 must preserve HTTP route compatibility and complete final web/boot cutover.

These concerns do not block G8 because the canvas module compiles, carries the frozen contract data, owns canvas/user-input persistence correctly, and exposes execution integration through ports.

## Coordinator Actions Needed

- Dispatch DDD-W08 execution worker after registry closure.
- Include `ExecutionPublicationPort` and `UserInputResumePort` implementation in the DDD-W08 handoff.
- Track outbox/retry and canvas version uniqueness/locking as integration or migration concerns before DDD-C09/final cutover.

## Rollback Path

Revert files under `backend/canvas-context-canvas/**`; backup/pre-ddd-osg-20260609-222054 remains the pre-rewrite restore point.
