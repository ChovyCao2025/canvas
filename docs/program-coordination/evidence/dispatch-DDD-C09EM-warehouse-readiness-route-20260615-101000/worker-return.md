# DDD-C09EM Warehouse Readiness Route

Status: DONE

## Finding

`route:/warehouse/readiness` is already implemented in final modules and is not a real compatibility gap.

The old controller named in the packet,
`backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessIncidentController.java`,
does not own the aggregate readiness route. It owns:

- `POST /warehouse/readiness/incidents/scan`

The final aggregate readiness route is implemented by:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessController.java`
- `GET /warehouse/readiness`
- final facade dependency: `org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade`

The existing focused compatibility test verifies the compatibility envelope, tenant header propagation,
section payload, `productionReady`, `blockerCount`, and blocker details:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessControllerCompatibilityTest.java`

## Files Changed

- `docs/program-coordination/evidence/dispatch-DDD-C09EM-warehouse-readiness-route-20260615-101000/worker-return.md`

No production code changes were made.

## Verification

Passed:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -Dtest=CdpWarehouseReadinessControllerCompatibilityTest test
```

Result: 1 test run, 0 failures, 0 errors.

Passed route scan:

```bash
rg -n "@(RequestMapping|GetMapping|PostMapping).*warehouse/readiness|/warehouse/readiness|CdpWarehouseReadinessController|CdpWarehouseReadinessIncidentController" \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessIncidentController.java \
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessController.java \
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessControllerCompatibilityTest.java
```

Key results:

- old incident controller: `@RequestMapping("/warehouse/readiness/incidents")`
- final readiness controller: `@GetMapping("/warehouse/readiness")`
- focused test calls `.uri("/warehouse/readiness")`

Passed old-coupling scan over the relevant final readiness files:

```bash
rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain|org\\.chovy\\.canvas\\.dal|org\\.chovy\\.canvas\\.web\\.CdpWarehouseReadinessIncidentController|CdpWarehouseReadinessIncidentService" \
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessController.java \
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseReadinessFacade.java \
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseReadinessApplicationService.java || true
```

Result: no matches.

## Gap Decision

`route:/warehouse/readiness` does not remain a real gap. The preflight candidate is a false positive for this route.
