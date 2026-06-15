# DDD-C09BJ Coordinator Closeout

Date: 2026-06-14

## Result

`DONE_WITH_CONCERNS`

Implemented the final `canvas-web` compatibility seed for the legacy
`/canvas/ab-experiments` route family without editing `backend/canvas-engine/**`
or any `pom.xml`.

## Verification

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=AbExperimentApplicationServiceTest
```

Passed: `AbExperimentApplicationServiceTest` 2/2.

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AbExperimentControllerCompatibilityTest test
```

Passed: `AbExperimentControllerCompatibilityTest` 3/3.

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
```

Passed: reactor build success through `canvas-web`.

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Passed: current `canvas-web` is 29 controllers / 506 endpoints, and
`route:/canvas/ab-experiments` is no longer listed in the reported top route
gaps. Global cutover remains blocked by overall controller/endpoint parity.

```bash
rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|AbExperimentMapper|AbExperimentGroupService|AbExperimentGovernanceService" \
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AbExperimentController.java \
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AbExperimentFacade.java \
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AbExperimentApplicationService.java \
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AbExperimentCatalog.java
```

Passed: exited 1 with no forbidden old-coupling matches.

## Accepted Concerns

- The implementation is a compact deterministic compatibility seed, not durable
  AB experiment persistence or full governance parity.
- Leibniz failed with a platform concurrency error and produced no normal worker
  packet; coordinator recovery evidence is authoritative for this closeout.
- DDD-C09 final cutover remains blocked by broader route parity.
