# DDD-C09O Recovery Note

Date: 2026-06-12

Dispatch `dispatch-DDD-C09O-cdp-user-tag-controller-20260612-204231` reserves a compact
production CDP user tag controller seed.

## Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpUserTagController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpUserTagControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- `node tools/program-coordination/check-dispatch-state.mjs .` passed with no active dispatch.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0 with
  current canvas-web 5 controllers / 26 endpoints and global `cutoverReady=false`.
- The exact reserved files were absent before reservation.

## Scope Boundary

Implement only final `CdpTagFacade`-backed `/cdp/users/{userId}/tags` routes:

- `POST /cdp/users/{userId}/tags`
- `GET /cdp/users/{userId}/tags`
- `GET /cdp/users/{userId}/tag-history`
- `DELETE /cdp/users/{userId}/tags/{tagCode}`

Do not implement `/cdp/events/track` in this dispatch because final production write-key
authentication is not yet exposed as a canvas-context-cdp API port.
