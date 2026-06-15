# DDD-C09BV Contract Review

Reviewer: Avicenna `019ec6da-e329-7b41-9856-21f2934c67e1`
Mode: read-only

## Legacy Endpoints

Base path: `/warehouse/audiences`

- `POST /{audienceId}/materialize`
- `POST /{audienceId}/materialize-gated`
- `POST /{audienceId}/materialize-contract-gated`
- `POST /{audienceId}/materialization/rollback`
- `POST /materialization/refresh-due`
- `POST /materialization/refresh-due-gated`
- `GET /materialization-runs`

## Findings Applied

- The three collection-level materialization routes remain directly under `/warehouse/audiences`.
- Write endpoints read `operator` from JSON body; controller keeps body operator precedence.
- Empty refresh body should preserve legacy `limit = 0`; coordinator corrected the catalog fallback and test.
- Rollback accepts nullable body at controller level but still rejects missing `targetVersion` in application behavior.

## Accepted Differences

- The final-module compact web controllers in this DDD-C09 route batch use `X-Tenant-Id` default `7L` and `X-Actor` default `operator-1`. Legacy old-engine fallback used `TenantContext` and could resolve username `system` when the resolver was absent. This remains an accepted compact compatibility seed concern for the route-parity batch.
