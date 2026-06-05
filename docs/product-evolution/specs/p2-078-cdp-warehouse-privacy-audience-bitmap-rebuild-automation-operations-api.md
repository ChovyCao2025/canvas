# P2-078 - CDP Warehouse Privacy Audience Bitmap Rebuild Automation Operations API Spec

Priority: P2
Sequence: 078
Source: `docs/product-evolution/specs/p2-077-cdp-warehouse-privacy-audience-bitmap-rebuild-automation.md`
Implementation plan: `../plans/p2-078-cdp-warehouse-privacy-audience-bitmap-rebuild-automation-operations-api-plan.md`

## Goal

Expose the P2-077 privacy audience bitmap rebuild automation cycle as a tenant-scoped operator API so production operators can manually run a bounded scan and inspect the per-request summary without waiting for scheduler logs.

## Current Baseline

- P2-076 exposes a manual rebuild proof endpoint for one erasure request.
- P2-077 adds an automation service and a scheduler that scans eligible erasure requests and delegates to P2-076.
- The scheduler discards the returned automation summary after each cycle.
- There is no API for operators to run the P2-077 automation cycle on demand and observe the result.

## In Scope

- Add a manual operations endpoint under the existing privacy erasure API.
- Accept the existing P2-077 `AutomationCommand` request body.
- Resolve tenant from the existing `TenantContextResolver`.
- Delegate directly to `CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.run`.
- Return the existing P2-077 `AutomationResult`, including scanned, eligible, triggered, skipped, failed, and per-request details.
- Add focused controller tests.
- Update product-evolution indexes.

## Out Of Scope

- Changing P2-077 eligibility rules.
- Changing scheduler behavior or scheduler configuration.
- Persisting automation run history.
- Async queueing for large scans.
- UI.

## Runtime Semantics

1. Operators call `POST /warehouse/privacy/erasure/audience-rebuild/automation/run`.
2. The request body is optional; a missing body uses P2-077 defaults.
3. The controller resolves the current tenant and defaults to tenant `0` when no resolver is configured.
4. The controller fails closed when the automation service is not configured.
5. The controller delegates to P2-077 and returns the service result unchanged.
6. Existing single-request P2-076 and scheduled P2-077 behavior remains unchanged.

## Functional Requirements

1. Operators can manually run one audience bitmap rebuild automation cycle for the current tenant.
2. Operators can pass actor, scan limit, audience limit, and retry-failed behavior.
3. The response includes the same summary and per-request result fields as P2-077.
4. The endpoint must not require or expose raw erasure subject values.
5. Existing privacy erasure endpoints continue unchanged.

## Technical Scope

- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureControllerTest.java`.
- Update `docs/product-evolution/specs/INDEX.md`.
- Update `docs/product-evolution/plans/INDEX.md`.
- Update `docs/product-evolution/IMPLEMENTATION_ORDER.md`.

## Acceptance Criteria

- P2-078 spec and plan are indexed.
- Controller tests prove the new endpoint delegates to P2-077 with current tenant and request body.
- Controller tests prove existing P2-076 single-request rebuild endpoint still delegates correctly.
- Focused backend tests pass.
- Warehouse/CDP regression passes.

## Rollout

1. Deploy the endpoint.
2. In staging, run P2-075 erasure execution for a request whose upstream proofs pass.
3. Call `POST /warehouse/privacy/erasure/audience-rebuild/automation/run` with conservative limits.
4. Confirm returned summary shows eligible requests and triggered rebuild proof.
5. Keep P2-077 scheduler disabled until manual runs are understood.

## Rollback

- Stop calling the new operations endpoint.
- P2-076 single-request rebuild and P2-077 scheduler remain available.
