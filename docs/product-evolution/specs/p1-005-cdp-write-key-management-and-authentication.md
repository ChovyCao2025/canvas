# P1-005 - CDP Write Key Management And Authentication Spec

Priority: P1
Sequence: 005
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/2026-05-30-cdp-sdk-design.md`
Implementation plan: `../plans/p1-005-cdp-write-key-management-and-authentication-plan.md`

## Implementation Status

Implemented on 2026-06-05 with the actual migration version `V100__cdp_write_key_management.sql` because `V97` is not the current migration sequence in this workspace. Focused Java 21 verification passed on 2026-06-08 for `CdpWriteKeySchemaTest`, `CdpWriteKeyAuthServiceTest`, and `CdpWriteKeyControllerTest`, covering schema, authentication, and admin API behavior.

## Goal

Create tenant-scoped CDP write keys and Basic Auth validation for the future `/cdp/events/track` ingestion endpoint.

## Current Baseline

- `POST /canvas/events/report` is protected by `EventReportAuthService` HMAC, but that endpoint is for canvas triggers.
- `cdp_write_key` stores tenant-scoped SDK key prefix and BCrypt hash without raw-key persistence.
- `/cdp/write-keys` exposes tenant-scoped list, create, and disable admin actions.
- P0-001 owns broader tenant safety; this slice only adds write-key identity and validation.

## In Scope

- `cdp_write_key` table with key prefix, BCrypt hash, platform, status, QPS limit, daily quota, and tenant id.
- Raw write key generation shown only once on create.
- `CdpWriteKeyAuthService` for Basic Auth parsing, prefix lookup, BCrypt match, disabled-state rejection, and auth result.
- Admin list/create/disable endpoints under `/cdp/write-keys`.

## Out Of Scope

- `/cdp/events/track` batch ingestion and event log persistence; split into P1-005A.
- Attribute discovery and internal event publication; split into P1-005A2.
- Frontend write-key and attribute review UI; split into P1-005A3.
- Browser SDK package; already split into P1-005C.

## Functional Requirements

1. Create writes only key prefix and BCrypt hash to the database.
2. Raw write key is returned only in the create response.
3. Auth accepts Basic Auth with `base64(writeKey + ":")`.
4. Missing, malformed, disabled, unknown, or hash-mismatched keys are rejected before ingestion.
5. List never returns the key hash or raw key.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_write_key_management.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWriteKeyDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWriteKeyMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java`

## Acceptance Criteria

- Schema tests prove write-key fields and indexes exist.
- Auth tests prove active, disabled, malformed, and hash-mismatch paths.
- Controller tests prove create returns raw key once and list omits secrets.
