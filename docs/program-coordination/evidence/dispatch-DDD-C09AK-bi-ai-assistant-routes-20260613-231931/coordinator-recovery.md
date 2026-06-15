# DDD-C09AK Coordinator Recovery

Date: 2026-06-13
Dispatch: dispatch-DDD-C09AK-bi-ai-assistant-routes-20260613-231931
Task: DDD-C09AK BI AI assistant route batch
Worker: Aquinas 019ec19b-ddd6-7282-8800-79da809fbea2

## Recovery Summary

Aquinas was spawned as the real code-writing worker before the dispatch moved
to `RUNNING`. After one `wait_agent` timeout, the coordinator inspected changed
paths and ran the focused Maven verification instead of repeatedly waiting.

The worktree showed a partial DDD-C09AK RED state: the service test imported
`BiAiRequestCommand` and `BiAiResponseView`, and the controller referenced BI AI
routes, but the reserved API/domain files were not present. The focused Maven
command failed during `canvas-context-bi` test compilation because those two
API records were missing.

The coordinator sent the concrete compiler failure back to Aquinas. After a
second bounded recovery wait timed out, `close_agent` returned
`previous_status: running` and no worker return packet. The coordinator then
completed only the exact DDD-C09AK reserved scope.

## Root Cause

The worker added references and tests for the BI AI batch but did not finish
the reserved API/domain implementation before timing out:

- `BiAiRequestCommand` missing
- `BiAiResponseView` missing
- `BiAiAssistantCatalog` missing
- `BiCatalogController` called `aiEnvelope(...)` before the helper existed
- `RecordingBiCatalogFacade` did not implement the new facade method

## Coordinator Recovery Changes

- Added `BiAiRequestCommand`
- Added `BiAiResponseView`
- Added deterministic final-module `BiAiAssistantCatalog`
- Added the missing `BiCatalogController.aiEnvelope(...)` helper
- Added controller compatibility coverage for the five BI AI routes
- Added the `RecordingBiCatalogFacade.aiAssistant(...)` test double method

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  passed with 65 tests, 0 failures.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  passed; current `canvas-web` reports 15 controllers / 98 endpoints,
  `route:/canvas/bi` reports 1 controller / 58 endpoints, and `cutoverReady`
  remains false.
- Forbidden-coupling `rg` over C09AK production BI paths returned no matches.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- Scoped `git diff --check` passed.

## Risks

- No normal worker return packet was produced.
- The BI AI implementation is a compact deterministic compatibility seed, not
  a durable AI/LLM integration.
- Broader BI route parity and global DDD-C09 cutover remain blocked.

## Rollback

Revert only the exact DDD-C09AK reserved BI API, domain, application,
controller, and BI test files listed in the worker packet and dispatch
reservation.
