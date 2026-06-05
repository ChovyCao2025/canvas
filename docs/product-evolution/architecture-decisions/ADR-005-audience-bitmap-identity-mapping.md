# ADR-005 Audience Bitmap Identity Mapping

status: Proof Required
owner: CDP Data

## Source Evidence

Optimization reviews flagged RoaringBitmap collision risk from hashing user IDs to integer positions.

## Current-Code Evidence

`AudienceBitmapStore.toUid` maps `userId` with Guava `murmur3_32_fixed()` and `Math.abs`, then stores membership in Redis-backed RoaringBitmap.

## Decision

Do not remap production bitmaps until a deterministic identity mapping and backfill proof is accepted.

## Expected Benefit

Collision-free audience membership and more reliable realtime/offline audience operations.

## Cost

High: existing bitmap keys, snapshots, realtime membership, overlap calculations, and historical reads must dual-read or backfill.

## Rollback

Dual-write old and new mappings during rollout. Roll back by reading old bitmap keys and stopping new backfill jobs.

## Proof Command

`mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest`

## Accepted Evidence

Required: deterministic ID table design, collision tests, dual-read plan, backfill limits, and membership parity report.

## Child Spec

Required before implementation: `p2-018e-audience-bitmap-identity-remap`.

## Dependency Notes

Depends on CDP identity evidence and audience snapshot gates. Must not combine with service split.
