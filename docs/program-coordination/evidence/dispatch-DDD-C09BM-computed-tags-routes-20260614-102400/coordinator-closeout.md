# DDD-C09BM Coordinator Closeout

Dispatch: `dispatch-DDD-C09BM-computed-tags-routes-20260614-102400`

Task: `DDD-C09BM`

Scope:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedTagFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedTagCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedTagController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpComputedTagControllerCompatibilityTest.java`

Result: DONE_WITH_CONCERNS

Verification:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpComputedTagApplicationServiceTest`
  passed with `CdpComputedTagApplicationServiceTest` 2/2.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpComputedTagControllerCompatibilityTest test`
  passed with `CdpComputedTagControllerCompatibilityTest` 3/3.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed through `canvas-web`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` at 32 controllers / 533 endpoints, with
  `route:/cdp/computed-tags` removed from the reported top route gaps. The next
  top gap is `route:/cdp/webhooks`; `cutoverReady` remains false globally.
- Strict old-coupling `rg` scan over the final Computed Tags paths exited 1
  with no matches for legacy engine/web/domain/dto/query/dal coupling,
  `TenantContextResolver`, old computed tag services, old lineage service, or
  old computed tag mapper/dataobject names.

Accepted concerns:

- Hegel timed out once and produced no normal worker-return packet.
- The batch provides compatibility-level deterministic in-memory behavior.
- Durable computed tag persistence, scheduler execution, and lineage parity
  remain outside this batch.
- DDD-C09 final cutover remains blocked by global route parity.
