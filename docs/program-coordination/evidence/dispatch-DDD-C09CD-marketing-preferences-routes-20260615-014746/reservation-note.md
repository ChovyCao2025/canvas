# DDD-C09CD Marketing Preferences Routes Reservation

- Reserved at: 2026-06-15T01:47:46+08:00
- Coordinator: Codex
- Worker: Parfit (`019ec73e-e8cf-76f0-940d-cddd2883b002`)
- Status: RUNNING

## Scope

Migrate the legacy `/canvas/marketing-preferences` route family into final modules without editing `backend/canvas-engine/**` or `pom.xml`.

## Write Scope

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingPreferenceFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingPreferenceApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingPreferenceCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingPreferenceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingPreferenceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingPreferenceControllerCompatibilityTest.java`

## Compatibility Contract

- `GET /canvas/marketing-preferences/users/{userId}`
- `PUT /canvas/marketing-preferences/users/{userId}/consents/{channel}`
- `PUT /canvas/marketing-preferences/users/{userId}/channels/{channel}`
- `POST /canvas/marketing-preferences/users/{userId}/suppressions`
- `PUT /canvas/marketing-preferences/suppressions/{id}/deactivate`

Meaningful checks only: route shape, tenant default/header behavior, path/body field forwarding, channel normalization, suppression lifecycle, report summary shape, tenant isolation, and `API_001` bad request envelopes.
