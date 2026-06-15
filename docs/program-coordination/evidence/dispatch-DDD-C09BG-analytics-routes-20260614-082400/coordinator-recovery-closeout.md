# DDD-C09BG Analytics Route Closeout

Date: 2026-06-14 08:34 +08:00

## Worker Handling

Worker Jason `019ec388-0fb3-7302-8bfa-9c5d0b15b566` was spawned as a bounded
sidecar after the exact scope was reserved. The coordinator kept the critical
path local and did not idle-wait. One final `wait_agent` harvest timed out after
10 seconds, then `close_agent` returned previous status `running`; a shutdown
notification followed. No canonical worker-return packet was produced.

## Implemented Scope

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsViews.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/AnalyticsApplicationService.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/AnalyticsCatalog.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/AnalyticsApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/analytics/AnalyticsController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/analytics/AnalyticsControllerCompatibilityTest.java`

## Route Coverage

- `GET /analytics/events/counts`
- `GET /analytics/events`
- `GET /analytics/events/count`
- `GET /analytics/users/{userId}/timeline`
- `GET /analytics/events/attributes/{attribute}/distribution`
- `GET /analytics/attributes/{attribute}/distribution`
- `GET /analytics/funnels/{funnelKey}`
- `POST /analytics/alerts/preview`
- `POST /analytics/exports`
- `GET /analytics/exports/{id}`

## Verification

- RED confirmed:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi -Dtest=AnalyticsApplicationServiceTest`
  failed at testCompile because `AnalyticsViews` and `AnalyticsApplicationService`
  were missing.
- RED confirmed:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AnalyticsControllerCompatibilityTest test`
  failed at testCompile in `canvas-context-bi` for the same missing final API.
- GREEN:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi -Dtest=AnalyticsApplicationServiceTest`
  passed 2/2.
- GREEN:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AnalyticsControllerCompatibilityTest test`
  passed 2/2.
- Compile:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed.
- Preflight:
  `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 26 controllers / 477 endpoints and removed
  `route:/analytics` from the top reported gaps.
- Old-coupling scan:
  `rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|AnalyticsQueryService|AnalyticsEventMapper|AnalyticsFunnelDefinitionMapper|AnalyticsAlertRuleMapper|AnalyticsExportJobMapper" ...`
  returned no matches.

## Accepted Concerns

- No normal worker-return packet from Jason because the worker was intentionally
  closed after one bounded harvest timeout.
- This is a deterministic compatibility seed for route parity; durable analytics
  event/funnel/alert/export persistence remains future work.
- Global cutover remains blocked by broader route parity.
