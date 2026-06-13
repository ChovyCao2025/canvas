# DDD-W06 Worker Return

Date: 2026-06-10

status: DONE_WITH_CONCERNS

task id: DDD-W06

dispatch id: dispatch-DDD-W06-conversation-20260610-032430

branch: main

worktree: /Users/photonpay/project/canvas

base commit: 01aac65697d524f4cf2e92d954db088895631004

head commit: 01aac65697d524f4cf2e92d954db088895631004 (no commit created)

assigned task pack: docs/ddd-rewrite/task-packs/06-worker-conversation.md

files changed: added main Java files and tests under backend/canvas-context-conversation/src/**. Module pom was not edited.

contracts changed: added `ConversationFacade` plus inbound/work-item/routing command and view records under `org.chovy.canvas.conversation.api`.

old classes migrated: pilot coverage for old `ConversationIngressService`, `ConversationWorkspaceService` work-item flows, `ConversationRoutingService`, and representative `Conversation*DO` / `Conversation*Mapper` persistence rows for session/message/contact profile/work item/audit/routing/SLA.

new public api: `recordInbound`, `ensureWorkItemForSession`, `assignWorkItem`, `updateWorkItemStatus`, `upsertRoutingAgent`, `upsertRoutingRule`, `routeWorkItem`.

domain model changes: added dependency-free conversation records, repository/wait-resume ports, routing request/decision policy, normalization helpers.

persistence ownership changes: added conversation-context persistence rows/mappers/converter for representative W06 rows; inventory files not edited.

tests run:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-conversation`
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`

verification result: pass

verification output summary/path: Maven ran 8 tests, 0 failures/errors/skips; DDD guardrails exited 0 and reported `Guardrail checks passed.`

evidence artifact paths: backend/canvas-context-conversation/target/surefire-reports/*.txt and TEST-*.xml

guardrail checks: passed; advisory only references pre-existing risk validator compatibility text outside DDD-W06 scope.

failure modes reviewed: duplicate inbound idempotency, wait-resume port boundary, work-item creation/audit, assignment/status audit, route hit, route miss, persistence table/mapper mapping, JSON state conversion.

compatibility evidence: table names match old `conversation_*` tables; mapper interfaces extend `BaseMapper`; domain/application/API import scan found no old engine/DAL, WebClient, Spring Web, MyBatis, DO, or Mapper leakage.

temporary bridges: none implemented; execution wait resume is a port only.

open risks: pilot does not yet migrate full private-domain sync, SOP task completion, AI reply/provider adapters, or concrete MyBatis repository adapters.

coordinator actions needed: schedule follow-up worker for remaining conversation scope before G6 closure, or explicitly accept those gaps into later integration/cutover.

ledger update: DDD-W06 completed pilot: added conversation API/application/domain/persistence slice with inbound recording, work-item lifecycle, routing/SLA representative persistence, tests and guardrails passing.

rollback path: revert files under `backend/canvas-context-conversation/src/main/java/**` and `backend/canvas-context-conversation/src/test/java/**`.
