# DDD-C09BV Reservation

Date: 2026-06-14
Coordinator: main agent

## Scope

Top preflight gap: `route:/warehouse/audiences`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java`

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseAudienceFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseAudienceApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseAudienceCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseAudienceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseAudienceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseAudienceControllerCompatibilityTest.java`

## Guardrails

- No edits to `backend/canvas-engine/**`.
- No edits to Maven `pom.xml` files.
- Spawn a real worker before moving this dispatch to `RUNNING`.
- Tests should prove route compatibility, envelope/error behavior, default tenant/actor handling, and deterministic materialization semantics. Avoid ceremonial tests.
