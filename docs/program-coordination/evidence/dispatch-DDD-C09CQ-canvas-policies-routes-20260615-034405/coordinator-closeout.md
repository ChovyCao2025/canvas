# DDD-C09CQ Coordinator Closeout

Status: `DONE_WITH_CONCERNS`

## Scope Closed

Closed route gap: `route:/canvas/policies`

Implemented final-module route seed for:

- `GET /canvas/policies/state`
- `POST /canvas/policies/consent`
- `POST /canvas/policies/suppression`
- `POST /canvas/policies/channel`

## Compatibility Protected

- Policy state is tenant-scoped and returns consent, channel, and matching
  channel or `ALL` suppressions.
- Consent, suppression, and channel writes use legacy upsert keys.
- User ids are trimmed; channels and consent status are normalized uppercase.
- Suppression defaults blank channel to `ALL` and blank active to enabled.
- Channel defaults `enabled=1`, `verified=0`, and trims optional fields.
- Controller maps legacy route shape, tenant header, request payloads, success
  envelopes, and `API_001` bad-request envelopes.

No ceremonial route-only test class was added; tests cover behavior and
response compatibility.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingPolicyApplicationServiceTest`
  passed: 3 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingPolicyControllerCompatibilityTest test`
  passed: 3 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  passed; current `canvas-web` is 62 controllers / 707 endpoints, and
  `route:/canvas/policies` is no longer a top gap. Global `cutoverReady`
  remains `false`; next top gap is `route:/cdp/tag-operations`.
- Strict old-coupling scan over DDD-C09CQ production files returned no matches.
- Scoped `git diff --check` passed.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed before
  closeout state update.

## Accepted Concerns

- Compact deterministic in-memory compatibility seed only; durable marketing
  policy persistence is out of scope for this route-parity batch.
- No normal worker-return packet; the coordinator closed `Halley` after one
  bounded timeout and finished the exact reserved scope.
- Global route parity remains blocked by remaining gaps.
