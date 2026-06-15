# DDD-C09BD Coordinator Recovery Closeout

Date: 2026-06-14

## Dispatch

- Dispatch id: `dispatch-DDD-C09BD-conversation-remaining-routes-20260614-074000`
- Task id: `DDD-C09BD`
- Worker: Hooke `019ec360-3356-7132-8260-d4a6fb976420`
- Scope: remaining `/canvas/conversations` route aliases in the reserved Conversation facade/application/controller/test files.

## Recovery

The coordinator waited once for Hooke, then inspected reserved paths and this evidence directory. Hooke had not produced reserved-file changes or a worker return packet beyond the reservation note. The coordinator closed Hooke after the bounded wait and recovered the exact reserved scope locally instead of polling again.

## Changes

- Added focused Conversation controller compatibility coverage for the remaining alias routes.
- Added compact final-module Conversation facade methods for session/message lists, workspace inbox/tasks/timeline/SLA, AI reply suggestions, private-domain sync/read routes, adapter ingress, and WhatsApp webhook ingress.
- Added controller mappings that preserve the existing compatibility envelope and default tenant/actor behavior.
- Kept the implementation deterministic and final-module scoped, without old canvas-engine service coupling or POM edits.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ConversationControllerCompatibilityTest test`
  - Expected failure at testCompile: missing new methods on `ConversationFacade`.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ConversationControllerCompatibilityTest test`
  - Passed: `ConversationControllerCompatibilityTest` 3 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-conversation -Dtest=ConversationApplicationServiceTest`
  - Passed: 4 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed: reactor build success through `canvas-web`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed tool execution; current `canvas-web` moved to 23 controllers / 441 endpoints; `/canvas/conversations` is no longer in the top 10 reported gaps.
- Strict old-coupling scan over the reserved Conversation facade/application/controller files:
  - Exit 1 with no matches.

## Accepted Concerns

- No normal Hooke worker-return packet was produced.
- The new alias behavior is compact compatibility seed behavior; durable workspace task, SLA, AI suggestion, private-domain sync, and provider webhook parity remains future work.
- Global cutover readiness remains blocked by overall route parity: old canvas-engine web still has 142 controllers / 806 endpoints versus current canvas-web 23 controllers / 441 endpoints.

## Rollback

Revert only the exact DDD-C09BD reserved Conversation facade/application/controller/test files plus this DDD-C09BD evidence file and the matching coordinator registry rows.
