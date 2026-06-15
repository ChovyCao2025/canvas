# DDD-C09CB Coordinator Closeout

Task: DDD-C09CB `/warehouse/e2e-certification`

Status: DONE_WITH_CONCERNS

## Scope

Implemented five legacy Warehouse E2E Certification route aliases in final modules:

- `GET /warehouse/e2e-certification`
- `GET /warehouse/e2e-certification/gate`
- `POST /warehouse/e2e-certification/runs`
- `GET /warehouse/e2e-certification/runs`
- `GET /warehouse/e2e-certification/runs/{id}`

## Files

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseE2eCertificationFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseE2eCertificationApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseE2eCertificationCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseE2eCertificationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseE2eCertificationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseE2eCertificationControllerCompatibilityTest.java`

## Verification

Passed:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseE2eCertificationApplicationServiceTest`
  - `CdpWarehouseE2eCertificationApplicationServiceTest` 2/2
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseE2eCertificationControllerCompatibilityTest test`
  - `CdpWarehouseE2eCertificationControllerCompatibilityTest` 3/3
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - reactor built through `canvas-web`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - diagnostic pass; global cutover remains false
  - current `canvas-web`: 47 controllers / 639 endpoints
  - `/warehouse/e2e-certification` removed from the reported top gaps
  - next top gap: `route:/canvas/data-sources`
- strict old-coupling scan over final Warehouse E2E Certification production files
  - no matches for `canvas-engine`, old domain packages, `TenantContext`, legacy certification services, or `AccessDeniedException`
- scoped `git diff --check`
  - no whitespace errors

## Test Rationale

The tests are compatibility-focused and intentionally non-ceremonial. They cover the five route shapes, tenant default `0L`, repeated `contractKey` forwarding, default and explicit flags, certification/gate/run payload fields, recent run default limit, run creation/queryability, and `API_001` missing-run envelope behavior.

## Accepted Concerns

- Wegener was spawned as a real sidecar worker and landed useful code, but was closed before returning a packet; coordinator-owned evidence is authoritative.
- This is a compact deterministic compatibility seed for final-module route parity. Durable legacy warehouse certification persistence, Doris probes, realtime job checks, and scheduler execution remain outside this route-alias batch.
- Global DDD-C09 route parity still blocks final cutover; the next preflight gap is `route:/canvas/data-sources`.
