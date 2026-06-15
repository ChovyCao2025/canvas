# DDD-C09CS Coordinator Closeout

Status: `DONE_WITH_CONCERNS`

## Scope Closed

Closed route gap: `route:/demo-sandboxes`

Implemented final-module route seed for:

- `POST /demo-sandboxes`
- `POST /demo-sandboxes/{tenantId}/reset`
- `GET /demo-sandboxes/expired`
- `POST /demo-sandboxes/{tenantId}/conversation-replies`

## Compatibility Protected

- Install stores tenant, demo name, TTL, status, installed actor, install time,
  and expiration time.
- Reset uses path tenant id and actor defaults.
- Expired returns sandboxes whose status or expiration time is expired.
- Conversation replies preserve sandbox channel, path tenant id, payload fields,
  attributes, and actor.
- Controller maps legacy route shape, request body, path variable, actor header,
  success envelopes, and `API_001` bad-request envelopes.

No ceremonial route-only test class was added; tests cover behavior and
response compatibility.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-conversation -Dtest=DemoSandboxApplicationServiceTest`
  passed: 3 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=DemoSandboxControllerCompatibilityTest test`
  passed: 3 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  passed; current `canvas-web` is 64 controllers / 715 endpoints, and
  `route:/demo-sandboxes` is no longer a top gap. Global `cutoverReady`
  remains `false`; next top gap is `family:CanvasUser`.
- Strict old-coupling scan over DDD-C09CS production files returned no matches.
- Scoped `git diff --check` passed.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed before
  closeout state update.

## Accepted Concerns

- Compact deterministic in-memory compatibility seed only; durable demo sandbox
  provisioning and real conversation adapter execution are out of scope for
  this route-parity batch.
- No normal worker-return packet; the coordinator closed `Euclid` after one
  bounded timeout and finished the exact reserved scope.
- Global route parity remains blocked by remaining gaps.
