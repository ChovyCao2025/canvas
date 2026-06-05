# P2-077 - CDP Warehouse Privacy Audience Bitmap Rebuild Automation Spec

Priority: P2
Sequence: 077
Source: `docs/product-evolution/specs/p2-076-cdp-warehouse-privacy-audience-bitmap-rebuild-proof.md`
Implementation plan: `../plans/p2-077-cdp-warehouse-privacy-audience-bitmap-rebuild-automation-plan.md`

## Goal

Automate the P2-076 audience bitmap rebuild proof step so privacy erasure requests do not rely on manual operator calls after upstream CDP, realtime, and Doris erasure assets have passed.

## Current Baseline

- P2-076 can rebuild `AUDIENCE_BITMAP_VERSION` proof for one erasure request.
- P2-076 is exposed as a manual operator endpoint.
- P2-073 backlog readiness remains `WARN` while a request keeps a non-pass audience bitmap proof.
- There is no scheduler that discovers requests ready for audience bitmap rebuild proof.

## In Scope

- Add an automation service that scans recent tenant-scoped erasure requests and selects requests whose audience bitmap proof is not terminal while all upstream proofs are `PASS` or `SKIPPED`.
- Delegate each selected request to the existing P2-076 rebuild service.
- Add a lease-protected scheduler with configurable tenant id, scan limit, audience rebuild limit, actor, and retry-failed behavior.
- Return bounded execution summaries for operator and test visibility.
- Add focused service and scheduler tests.

## Out Of Scope

- Rebuilding audience bitmaps directly without P2-076.
- Executing P2-075 deletes automatically, because raw subject values cannot be recovered from the privacy ledger.
- Persistent async queues.
- UI.

## Runtime Semantics

1. The automation service reads recent erasure requests via the existing P2-073 service.
2. A request is eligible when:
   - It has an `AUDIENCE_BITMAP_VERSION` proof whose status is not `PASS` or `SKIPPED`.
   - All other asset proofs are `PASS` or `SKIPPED`.
   - It is not already request-level `PASS`.
3. Requests with upstream `PLANNED`, `RUNNING`, `WARN`, or `FAIL` proof are skipped.
4. Requests with `AUDIENCE_BITMAP_VERSION=FAIL` are retried only when `retryFailed=true`.
5. The scheduler uses `CdpWarehouseJobLeaseService` when configured and an in-process running guard to prevent overlapping cycles.
6. The scheduler is disabled by default and can be enabled per deployment.

## Functional Requirements

1. Operators or schedulers can run an automation cycle with tenant scope, actor, scan limit, audience limit, and retry-failed flag.
2. The automation cycle must not require raw subject values.
3. The automation cycle must not call P2-076 when upstream erasure proof is incomplete or non-pass.
4. The automation cycle must call P2-076 for eligible requests and include per-request result status.
5. The scheduler must respect enabled=false, lease ownership, and overlapping execution guards.

## Technical Scope

- Add `CdpWarehousePrivacyAudienceBitmapRebuildAutomationService`.
- Add `CdpWarehousePrivacyAudienceBitmapRebuildScheduler`.
- Add focused service and scheduler tests.
- Update product-evolution indexes.

## Acceptance Criteria

- P2-077 spec and plan are indexed.
- Automation service tests prove eligible requests invoke P2-076.
- Automation service tests prove upstream non-pass requests are skipped.
- Automation service tests prove failed audience proof is skipped by default and retried when configured.
- Scheduler tests prove disabled, lease, and non-overlap behavior.
- Focused backend tests pass.
- Warehouse/CDP regression passes.

## Rollout

1. Deploy automation disabled.
2. Enable in staging for the tenant used by P2-073/P2-075/P2-076 verification.
3. Confirm P2-073 backlog clears after P2-075 execution and P2-077 automation.
4. Enable in production with conservative scan and audience limits.

## Rollback

- Disable `canvas.warehouse.privacy-audience-rebuild-scheduler.enabled`.
- Manual P2-076 endpoint remains available.
- Existing P2-073 proof rows remain auditable.
