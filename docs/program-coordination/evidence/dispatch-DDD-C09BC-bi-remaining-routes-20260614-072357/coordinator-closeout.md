# DDD-C09BC Coordinator Closeout

- Dispatch: `dispatch-DDD-C09BC-bi-remaining-routes-20260614-072357`
- Worker: Epicurus `019ec34e-5a90-7cc0-99e5-1572a80c0ef8`
- Status: `DONE_WITH_CONCERNS`
- Closed at: 2026-06-14T07:37:31+08:00

## Scope

Reserved files:

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`

## Coordinator Recovery Notes

Coordinator waited once for Epicurus, timed out, then continued local non-overlapping implementation and verification instead of idle polling. Epicurus later returned `DONE_WITH_CONCERNS`; coordinator integrated the useful worker changes and fixed remaining compatibility issues:

- DELETE collection aliases split query-param and request-body mappings to avoid WebFlux `415` on no-body DELETE calls.
- Collection POST aliases preserve legacy stable keys while path-based draft endpoints still preserve body-key sentinel coverage.
- Embed dashboard alias returns both `resourceKey` and `dashboardKey` compatibility fields.
- Portal runtime test fake now reflects the saved route key without mutating body-key sentinel assertions.

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi -Dtest=BiCatalogApplicationServiceTest` passed: 40 tests, 0 failures.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests` passed.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=BiCatalogControllerCompatibilityTest test` passed: 32 tests, 0 failures.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` passed command; `canvas-web` is now 23 controllers / 424 endpoints, `/canvas/bi` is no longer in top route gaps, and global `cutoverReady` remains false.
- Strict BI old-coupling scan returned no matches.

## Accepted Concerns

- Global cutover remains blocked by route parity: current `canvas-web` 23 controllers / 424 endpoints versus old `canvas-engine` web 142 controllers / 806 endpoints.
- DDD-C09BC is compact compatibility coverage for remaining BI aliases; durable persistence/external runtime parity remains governed by broader BI follow-up work.

## Rollback

Revert only the four reserved DDD-C09BC files plus this evidence file and DDD-C09BC coordination registry rows.
