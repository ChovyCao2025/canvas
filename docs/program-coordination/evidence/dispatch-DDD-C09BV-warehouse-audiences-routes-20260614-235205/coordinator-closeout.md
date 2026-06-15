# DDD-C09BV Coordinator Closeout

Status: DONE_WITH_CONCERNS

## Summary

Added final-module CDP/web coverage for all seven legacy `/warehouse/audiences` materialization routes. The implementation is a compact deterministic in-memory compatibility seed, not durable parity with the old materialization services.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseAudienceApplicationServiceTest` passed; surefire reports 3 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseAudienceControllerCompatibilityTest test` passed; surefire reports 3 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests` passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` passed; current `canvas-web` is 41 controllers / 604 endpoints and `route:/warehouse/audiences` is removed from reported top gaps.
- Strict old-coupling scan over the new Warehouse Audience production files returned no matches.
- Scoped `git diff --check` returned no findings.

## Concerns

- Durable old service parity is out of scope: no old `AudienceMaterializationOperationsService`, schedule service, warehouse availability contract, or persistence adapter was ported in this route batch.
- Legacy username fallback via `TenantContext` is not fully reproduced; this compact web batch follows the existing final-module default header convention.
- Global DDD-C09 cutover remains blocked by route parity; next top preflight gap is `route:/approvals`.
