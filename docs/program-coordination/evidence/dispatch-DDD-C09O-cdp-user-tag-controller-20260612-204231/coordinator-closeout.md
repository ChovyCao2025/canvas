# DDD-C09O Coordinator Closeout

Date: 2026-06-12

## Dispatch

- Dispatch: `dispatch-DDD-C09O-cdp-user-tag-controller-20260612-204231`
- Worker: Anscombe `019ebbe9-319f-7590-ac6d-fd427a7c2cd0`
- Reviewer: Boole `019ebbef-d774-7ae0-91fb-5955281bbc0f`
- Result: `DONE_WITH_CONCERNS`

## Reserved Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpUserTagController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpUserTagControllerCompatibilityTest.java`

## Result

Anscombe added a compact production `CdpTagFacade`-backed controller seed for:

- `POST /cdp/users/{userId}/tags`
- `GET /cdp/users/{userId}/tags`
- `GET /cdp/users/{userId}/tag-history`
- `DELETE /cdp/users/{userId}/tags/{tagCode}`

The controller preserves compatibility envelopes, default `X-Tenant-Id` `7`,
default `X-Actor` `operator-1`, nonblank actor trimming, null POST body command
creation, fixed delete reason `user detail remove tag`, and
`IllegalArgumentException` to `API_001` bad-request envelope mapping.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CdpUserTagControllerCompatibilityTest`
  - `BUILD SUCCESS`; 7 tests run, 0 failures/errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CdpApiCompatibilityTest,CdpUserTagControllerCompatibilityTest`
  - `BUILD SUCCESS`; 11 tests run, 0 failures/errors.
- Boole read-only review returned `PASS` with no findings and no required fixes.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Passed with `activeDispatches` empty after closeout.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - Passed after closeout.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Exited 0 with current canvas-web 6 controllers and 30 endpoints; global
    `cutoverReady` remains false.
- `git diff --check -- <DDD-C09O files and coordination closeout files>`
  - Passed with no whitespace errors.

## Accepted Concerns

- `/cdp/events/track` and production write-key authentication remain out of
  scope for this dispatch because final production write-key auth is not yet
  exposed as a canvas-context-cdp API port.
- Global DDD-C09 cutover remains blocked by broader production controller and
  endpoint parity gaps.

## Rollback Pointer

Remove only:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpUserTagController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpUserTagControllerCompatibilityTest.java`
