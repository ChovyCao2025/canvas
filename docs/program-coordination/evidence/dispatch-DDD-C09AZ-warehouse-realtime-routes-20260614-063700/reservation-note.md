# DDD-C09AZ Reservation Note

Time: 2026-06-14T06:37:00+08:00

Coordinator reserved DDD-C09AZ as a code-writing batch for the remaining
`/warehouse/realtime` production route parity gap after DDD-C09AY closeout.

Scope:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseRealtimeFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseRealtimeApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseRealtimeCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseRealtimeApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeControllerCompatibilityTest.java`

The existing final controller already owns
`GET /warehouse/realtime/cutover-readiness`; this batch must not duplicate that
mapping. The batch targets the remaining 20 legacy endpoints under
`/warehouse/realtime`.

Scheduling rule for this dispatch: spawn a real worker before RUNNING, perform
meaningful non-overlapping coordinator work while it runs, and after one bounded
wait timeout inspect reserved paths/evidence and continue without idle polling.
