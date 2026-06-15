# DDD-C09CY Realtime Audience Compatibility Sidecar

Reviewer: Codex sidecar
Mode: read-only source inspection plus evidence note
Date: 2026-06-15

## Inspected Inputs

- Legacy controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java`
- Legacy behavior source: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java`
- Final-module CDP controller/test patterns:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseAudienceController.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseAudienceControllerCompatibilityTest.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeController.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeControllerCompatibilityTest.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpUserTagController.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpUserTagControllerCompatibilityTest.java`

## Route List And Compatibility Envelope

All six legacy routes are rooted at `/cdp`, not `/warehouse/*`.

- `POST /cdp/realtime-audiences/{id}/events`
  - Body fields: `sourceEventId`, `userId`, `eventTime`, `properties`, `removeOnNoMatch`.
  - Default behavior: `removeOnNoMatch` defaults to `true`; null `properties` becomes an empty map in the service.
  - Expected data shape: event processing result with `status`, `operation`, `audienceId`, `sourceEventId`, `userId`.
  - Expected statuses: `UPDATED` for reserved add/remove, `DUPLICATED` for duplicate event log reservation, `SKIPPED` when the event does not match and `removeOnNoMatch=false`.

- `POST /cdp/realtime-audiences/{id}/snapshot`
  - No body in legacy controller.
  - Expected behavior: creates a manual snapshot with source `MANUAL`; operator comes from resolved context username in old engine.
  - Expected data shape: `audienceId`, `estimatedSize`, `bitmapKey`, `snapshotSource`.
  - `bitmapKey` legacy format is `audience:bitmap:{id}`.

- `GET /cdp/realtime-audiences/{id}/snapshots?limit={limit}`
  - Query `limit` is optional.
  - Legacy controller normalizes `null` or `<=0` to `100`, then caps at `500`.
  - Expected data shape: list of rows with `id`, `tenantId`, `audienceId`, `estimatedSize`, `bitmapKey`, `snapshotSource`, `createdBy`, `createdAt`.

- `GET /cdp/audiences/{leftId}/overlap/{rightId}`
  - Expected data shape: `leftCount`, `rightCount`, `intersectionCount`, `leftPercentage`, `rightPercentage`.
  - Percentages return `0.0` when the relevant denominator is zero.
  - Legacy behavior does not use tenant context for this set operation.

- `POST /cdp/audiences/merge?leftId={leftId}&rightId={rightId}`
  - Query params are required.
  - Expected data shape: `status`, `reason`, `resultSize`, `safeLimit`.
  - Expected behavior: returns `READY` with reason `MERGE` unless result cardinality exceeds the safety limit, then `BLOCKED` with reason `SAFE_SIZE_LIMIT_EXCEEDED`.

- `POST /cdp/audiences/exclude?baseId={baseId}&excludedId={excludedId}`
  - Query params are required.
  - Expected data shape: `status`, `reason`, `resultSize`, `safeLimit`.
  - Expected behavior: returns `READY` with reason `EXCLUDE` unless result cardinality exceeds the safety limit, then `BLOCKED` with reason `SAFE_SIZE_LIMIT_EXCEEDED`.

Final-module CDP web controllers in this batch use a compatibility envelope:

- Success: HTTP 200, JSON fields `code: 0`, `message: "success"`, no serialized `errorCode`, no serialized `traceId`, and result under `data` when non-null.
- Bad request: HTTP 400, JSON fields `code: 400`, `errorCode: "API_001"`, `message` from the validation exception, no serialized `data`, no serialized `traceId`.
- Current pattern defaults missing `X-Tenant-Id` to `7L` and missing/blank `X-Actor` to `operator-1`; old engine resolved `TenantContext` and used tenant fallback `0L`. If main implementation follows final-module batch conventions, call out this accepted compatibility seed difference.

## Meaningful Edge Cases Worth Testing

- Route parity for all six exact `/cdp/...` routes, including the mixed `/cdp/realtime-audiences` and `/cdp/audiences` prefixes.
- Event request defaulting: absent `removeOnNoMatch` maps to `true`; explicit `false` allows a nonmatching event to return `SKIPPED`/`NOOP` instead of removing.
- Event request validation: blank `sourceEventId` and blank `userId` should map to the `API_001` bad-request envelope, not a raw server error.
- Event normalization: `sourceEventId` and `userId` are trimmed before being returned/stored; null `properties` should behave as an empty map.
- Duplicate event reservation should return `DUPLICATED` without mutating the bitmap a second time.
- Snapshot list limit normalization: absent/zero/negative -> `100`; values above `500` -> `500`.
- Snapshot creation should preserve `snapshotSource: "MANUAL"` and legacy bitmap key format `audience:bitmap:{id}`.
- Overlap with empty left or right audience should return percentages `0.0` instead of division errors.
- Merge/exclude safety guard should expose both `READY` and `BLOCKED` outcomes with the legacy reason strings.
- Query param names must remain `leftId`, `rightId`, `baseId`, and `excludedId`; do not convert these routes to body-only commands.

## Old-Engine Coupling Strings To Avoid In New Files

Avoid importing or referencing old `canvas-engine` implementation and persistence types from final-module web/context code:

- `org.chovy.canvas.domain.cdp.RealtimeAudienceService`
- `RealtimeAudienceController`
- `org.chovy.canvas.common.R`
- `TenantContextResolver`
- `TenantContext`
- `org.chovy.canvas.dal.dataobject.AudienceDefinitionDO`
- `org.chovy.canvas.dal.dataobject.CdpAudienceSnapshotDO`
- `org.chovy.canvas.dal.dataobject.CdpRealtimeAudienceEventLogDO`
- `org.chovy.canvas.dal.mapper.AudienceDefinitionMapper`
- `org.chovy.canvas.dal.mapper.CdpAudienceSnapshotMapper`
- `org.chovy.canvas.dal.mapper.CdpRealtimeAudienceEventLogMapper`
- `org.chovy.canvas.engine.audience.AudienceBitmapStore`
- `org.roaringbitmap.RoaringBitmap`
- Legacy literal service/property coupling: `canvas.cdp.realtime-audience.safe-set-operation-limit`, `audience:bitmap:`, `CdpRealtimeAudienceEventLogDO.ADD`, `CdpRealtimeAudienceEventLogDO.REMOVE`

If production needs durable parity later, introduce final-module CDP facade/domain ports for realtime membership, snapshots, overlap, merge, and exclude rather than wiring these old-engine classes into `backend/canvas-web` or `backend/canvas-context-cdp`.

## Verification

- No broad tests run by sidecar.
- No implementation files edited.
