# DDD-C09EP Warehouse Readiness Incidents Route Closeout

Status: DONE_WITH_CONCERNS

Scope:
- Reclassified `route:/warehouse/readiness` as a real missing legacy sub-route after worker evidence showed old controller owned `POST /warehouse/readiness/incidents/scan`.
- Added final-module compatibility route `POST /warehouse/readiness/incidents/scan` through `CdpWarehouseReadinessFacade`.
- Extended focused readiness compatibility coverage and updated a second readiness fake facade to compile with the expanded interface.

Verification:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseReadinessControllerCompatibilityTest,BiCatalogControllerCompatibilityTest,ExecutionControllerCompatibilityTest,CdpWarehouseRealtimeCutoverReadinessControllerCompatibilityTest test`
  - Passed: 43 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile`
  - Passed: reactor `BUILD SUCCESS`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed: current `canvas-web` 104 controllers / 837 endpoints; route gap candidates 0.
- Strict old-coupling scan over touched final files
  - Passed: no old engine/service/mapper/entity/common imports.
- `git diff --check -- <touched files and coordination docs>`
  - Passed before closeout docs; rerun required after coordination update.

Risks:
- Incident scan is implemented from final readiness sections, not by porting the old domain incident service.
- Global cutover still reports the coarse controller-count blocker even though route gap candidates are now zero.
