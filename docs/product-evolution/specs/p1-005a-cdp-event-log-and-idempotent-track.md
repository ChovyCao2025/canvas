# P1-005A - CDP Event Log And Idempotent Track Spec

Priority: P1
Sequence: 005A
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/2026-05-30-cdp-sdk-design.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p1-005a-cdp-event-log-and-idempotent-track-plan.md`

Implementation status: implemented on 2026-06-05. The actual Flyway migration is
`backend/canvas-engine/src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql`;
the original `V97_1` filename was superseded by existing migration ordering.

## Goal

Add the server-side `/cdp/events/track` batch ingestion path and enriched `cdp_event_log` persistence with duplicate protection.

## Current Baseline

- P1-005 adds write-key authentication.
- Legacy `event_log` only stores canvas-trigger-oriented fields.
- `CdpUserService` can ensure profiles when `userId` is present.
- `EventDefinitionServiceImpl` validates published canvas events, but CDP SDK ingestion needs a separate endpoint.

## In Scope

- `cdp_event_log` schema with SDK, identity, context, timing, and idempotency fields.
- Batch request/response DTOs.
- `POST /cdp/events/track` endpoint that calls write-key authentication before ingestion.
- Per-item accepted/rejected results.
- Duplicate protection by tenant plus `messageId` and tenant plus explicit `idempotencyKey`.
- Unknown track event code rejection by default.
- `CdpUserService` ensure call when `userId` is present.

## Out Of Scope

- Attribute discovery for auto-discoverable event definitions; split into P1-005A2.
- Internal CDP event publication; split into P1-005A2.
- Web SDK implementation; split into P1-005C.
- OLAP, funnels, retention, and path analytics.

## Functional Requirements

1. `/cdp/events/track` is public at the security filter level but authenticates every request through `CdpWriteKeyAuthService`.
2. Batch size is bounded by configuration.
3. Valid events are persisted with tenant id, write key id, message id, event type, event code, user id, anonymous id, session id, device id, platform, context JSON, properties JSON, event time, sent time, received time, idempotency key, and status.
4. Duplicates by `messageId` or explicit `idempotencyKey` do not create another accepted row.
5. Unknown event codes are rejected by default.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpEventLogDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpEventLogMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/BatchTrackReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/TrackEventReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/IngestionResult.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/IngestionError.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`

## Acceptance Criteria

- Controller tests prove write-key auth is called before ingestion.
- Service tests prove enriched persistence, duplicate handling, unknown event rejection, and identity ensure.
- Legacy `/canvas/events/report` tests remain green.

## Verification Evidence

- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=CdpEventLogSchemaTest,CdpEventIngestionServiceTest,CdpEventIngestionControllerTest,SecurityConfigRouteTest -DfailIfNoTests=true`
- Result on 2026-06-05: 11 tests run, 0 failures, 0 errors, 0 skipped.
