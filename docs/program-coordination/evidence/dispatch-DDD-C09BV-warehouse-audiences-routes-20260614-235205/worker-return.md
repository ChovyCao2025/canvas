# DDD-C09BV Worker Return

Worker: James `019ec6d6-7b84-7b43-ad09-c737f69b011a`
Status: completed

## Changed Files

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseAudienceFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseAudienceApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseAudienceCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseAudienceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseAudienceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseAudienceControllerCompatibilityTest.java`

## Worker-Reported Tests

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseAudienceApplicationServiceTest` passed 3/3.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CdpWarehouseAudienceControllerCompatibilityTest -DfailIfNoTests=false` passed 3/3.

## Concerns

- Shell default Java is 8; Java 21 must be selected for repo builds.
- `canvas-web` verification should use `-am` so it compiles against current sibling module changes.
