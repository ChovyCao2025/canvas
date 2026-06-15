# DDD-C09DX canvas event attributes route

## Scope

- Added final compatibility route `GET /canvas/event-attributes/discovered`.
- Added final CDP facade/view shape for discovered event attributes.
- Did not edit `backend/canvas-engine/**` or any `pom.xml`.
- Did not update dispatch state or progress ledger.

## RED

Command:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=EventAttributeDiscoveryControllerCompatibilityTest test
```

Observed failure before implementation:

- `CdpEventAttributeDiscoveryFacade` did not exist.
- `EventAttributeDiscoveryController` final route/controller did not exist for the facade-backed compatibility test.
- A pre-existing untracked `ContactabilityControllerCompatibilityTest` constructor mismatch also appeared during the first compile attempt; it is outside this slice and was not edited.

## GREEN

Command:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=EventAttributeDiscoveryControllerCompatibilityTest test
```

Result:

- Build success.
- `EventAttributeDiscoveryControllerCompatibilityTest`: 2 tests run, 0 failures, 0 errors.

Later rerun note:

- A later rerun of the same target command reached `canvas-web` test compilation and then failed on unrelated missing controller classes in other compatibility tests, including `BiCatalogController`, `DataSourceConfigController`, `ProgrammaticDspController`, `CanvasPreferenceController`, `CdpWarehouseReadinessController`, `TechnicalMigrationCandidateController`, `CdpWriteKeyController`, `CdpWarehouseFieldGovernanceController`, `CanvasBatchOperationController`, `AiController`, `ExecutionRequestController`, `MessageSendRecordController`, `MarketingIntegrationController`, `NotificationController`, and `MqDefinitionController`.
- Those files are outside this slice and were not edited.

## Verification

Commands:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
rg "org\\.chovy\\.canvas\\.(engine|service|mapper|entity|common)" backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/EventAttributeDiscoveryController.java backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/EventAttributeDiscoveryControllerCompatibilityTest.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpEventAttributeDiscoveryFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpEventAttributeDiscoveryApplicationService.java
git diff --check -- backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/EventAttributeDiscoveryController.java backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/EventAttributeDiscoveryControllerCompatibilityTest.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpEventAttributeDiscoveryFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpEventAttributeDiscoveryApplicationService.java
```

Results:

- `mvn compile`: build success.
- `cutover-compatibility-preflight`: completed; global `cutoverReady` remains `false` because canvas-web controller and endpoint counts are still below old canvas-engine counts. The reported top route gaps did not include `route:/canvas/event-attributes`.
- Old package `rg`: no matches.
- `git diff --check`: no whitespace errors.
