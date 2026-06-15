# DDD-C09BU Channel Connectors Routes Worker Return

Status: completed

## Changed Files

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/ChannelConnectorFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/ChannelConnectorApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/ChannelConnectorCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/ChannelConnectorApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/channels/ChannelConnectorController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/channels/ChannelConnectorControllerCompatibilityTest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09BU-channel-connectors-routes-20260614-232715/worker-return.md`

## Summary

- Added a compact platform facade/application/domain seed for channel connector compatibility behavior.
- Added final-module `canvas-web` aliases for the legacy `/channels/connectors` endpoints:
  - `GET /channels/connectors`
  - `GET /channels/connectors/limits`
  - `POST /channels/connectors/{id}/mode`
  - `POST /channels/connectors/{id}/health-test`
  - `POST /channels/connectors/fallback/validate`
  - `GET /channels/connectors/fallback/decisions`
  - `GET /channels/connectors/dedupe-records`
- Kept responses in the current final-module compatibility envelope: success uses `code=0`, `message=success`; `IllegalArgumentException` maps to HTTP 400 with `code=400`, `errorCode=API_001`.
- Added focused compatibility tests only for route parity, facade mapping, normalization, deterministic seed behavior, and bad-request envelope behavior. Skipped trivial wiring-only tests per the clarified test policy.

## Commands Run

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=ChannelConnectorApplicationServiceTest`
  - Result: passed, 2 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ChannelConnectorControllerCompatibilityTest test`
  - Result: passed, 3 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Result: passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: command passed. `route:/channels` is no longer in the reported top route gaps.

## Concerns

- The preflight command still reports `cutoverReady=false` because of unrelated global gaps: `canvas-web` has fewer controllers/endpoints than old `canvas-engine`, and top reported gaps include `/warehouse/audiences`, `/approvals`, `/canvas/marketing-forms`, and others outside this dispatch scope.
