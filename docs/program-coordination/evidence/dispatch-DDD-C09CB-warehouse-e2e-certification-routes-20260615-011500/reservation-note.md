# DDD-C09CB Reservation

Task: DDD-C09CB `/warehouse/e2e-certification`

Gate: R5 after DDD-C09CA Test Users route closeout

Top preflight gap: `route:/warehouse/e2e-certification`

Old controllers:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`

Legacy routes:

- `GET /warehouse/e2e-certification`
- `GET /warehouse/e2e-certification/gate`
- `POST /warehouse/e2e-certification/runs`
- `GET /warehouse/e2e-certification/runs`
- `GET /warehouse/e2e-certification/runs/{id}`

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseE2eCertificationFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseE2eCertificationApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseE2eCertificationCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseE2eCertificationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseE2eCertificationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseE2eCertificationControllerCompatibilityTest.java`

Coordinator constraints:

- Do not edit `backend/canvas-engine/**` or `pom.xml`.
- Do not add ceremonial tests. Tests must cover the five route shapes, tenant default `0L`, mode/contractKey/default flag forwarding, certification/gate/run payload compatibility, recent run limit, and missing run envelope behavior.
- Keep Maven verification serial because module builds share `target/`.
