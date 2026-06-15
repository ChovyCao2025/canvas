# DDD-C09BI Programmatic DSP Routes Closeout

Date: 2026-06-14

## Scope

Added final-module compatibility coverage for the legacy
`/canvas/programmatic-dsp` route family:

- `POST /canvas/programmatic-dsp/seats`
- `POST /canvas/programmatic-dsp/campaigns`
- `POST /canvas/programmatic-dsp/line-items`
- `POST /canvas/programmatic-dsp/supply-paths`
- `POST /canvas/programmatic-dsp/snapshots`
- `GET /canvas/programmatic-dsp/summary`
- `POST /canvas/programmatic-dsp/mutations`
- `POST /canvas/programmatic-dsp/mutations/{mutationId}/approve`
- `POST /canvas/programmatic-dsp/mutations/{mutationId}/execute`
- `GET /canvas/programmatic-dsp/mutations`

## Worker Handling

Averroes `019ec39f-b26f-79b2-a81d-3f31f026249a` was spawned as a bounded
sidecar worker. The coordinator kept the critical path local, wrote RED tests,
implemented/fused the reserved files, and ran verification without idle polling.
A single short harvest timed out; `close_agent` then returned the worker's DONE
packet, which matched the coordinator verification.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=ProgrammaticDspApplicationServiceTest`
  - passed; surefire confirms 3 tests
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ProgrammaticDspControllerCompatibilityTest test`
  - passed; 3 tests
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - passed
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - passed; `canvas-web` advanced to 28 controllers / 497 endpoints
  - `route:/canvas/programmatic-dsp` no longer appears in the reported top gaps
- strict old-coupling `rg` scan over the reserved Programmatic DSP production files
  - no matches

## Accepted Concerns

- Programmatic DSP behavior is a deterministic compatibility seed, not durable
  persistence-backed production logic.
- Global cutover remains blocked by broader controller and endpoint parity
  gaps outside this dispatch.
