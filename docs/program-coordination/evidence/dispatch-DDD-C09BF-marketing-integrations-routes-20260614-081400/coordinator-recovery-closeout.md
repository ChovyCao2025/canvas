# DDD-C09BF Coordinator Recovery Closeout

Date: 2026-06-14

## Dispatch

- Dispatch id: `dispatch-DDD-C09BF-marketing-integrations-routes-20260614-081400`
- Task id: `DDD-C09BF`
- Worker: Hubble `019ec379-5f19-7993-86b9-eb6bed291425`
- Scope: final Marketing integration route aliases under `/canvas/marketing-integrations`.

## Recovery

The coordinator waited once for Hubble. The wait timed out, reserved code paths did not exist, and the evidence directory contained only the reservation note. The coordinator closed Hubble with previous status `running` and recovered the exact reserved scope locally instead of polling again.

## Changes

- Added `MarketingIntegrationFacade`.
- Added `MarketingIntegrationApplicationService`.
- Added compact deterministic `MarketingIntegrationCatalog`.
- Added production `MarketingIntegrationController` for the 11 legacy `/canvas/marketing-integrations` route shapes.
- Added focused application and controller compatibility tests.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingIntegrationApplicationServiceTest,MarketingIntegrationControllerCompatibilityTest test`
  - Expected failure at testCompile: missing `MarketingIntegrationApplicationService`.
- GREEN combined focused suite:
  - `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingIntegrationApplicationServiceTest,MarketingIntegrationControllerCompatibilityTest test`
  - Passed: application 1 test and controller 2 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingIntegrationApplicationServiceTest`
  - Passed: 1 test, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingIntegrationControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed: reactor build success through `canvas-web`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed tool execution; current `canvas-web` moved to 25 controllers / 467 endpoints; `/canvas/marketing-integrations` is no longer in the top 10 reported gaps.
- Strict old-coupling scan over the reserved Marketing integration facade/application/domain/controller files:
  - Exit 1 with no matches.

## Accepted Concerns

- Hubble did not produce a normal worker-return packet or reserved code changes before the bounded wait timed out and the coordinator closed it.
- The implementation is compact deterministic compatibility seed behavior, not durable marketing integration contract/probe persistence or external provider probe parity.
- Global cutover readiness remains blocked by overall route parity: old canvas-engine web still has 142 controllers / 806 endpoints versus current canvas-web 25 controllers / 467 endpoints.

## Rollback

Revert only the exact DDD-C09BF reserved Marketing integration facade/application/domain/controller/test files plus this DDD-C09BF evidence file and the matching coordinator registry rows.
