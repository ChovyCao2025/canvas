# DDD-C09DJ Worker Return

Worker: Locke `019ec837-056c-79a1-accf-a88622641a81`
Status: DONE

## Contract Summary

Legacy route family: `/canvas/execution`

Endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| POST | `/canvas/execution/{executionId}/approve` | path `executionId`; no body; returns `R<Void>` |
| POST | `/canvas/execution/{executionId}/reject` | path `executionId`; optional query `reason`; no body; returns `R<Void>` |

Compatibility details used by coordinator:

- Success envelope is `{ code: 0, message: "success", errorCode: null, data: null, traceId: null }`.
- Actor context defaults to tenant `0`, role `null`, username `system`.
- Missing pending approval is idempotent no-op success, not 404.
- Pending approval tenant must match actor tenant.
- Pending approval approvers list must contain the actor; failed approver check maps to HTTP 403 with `errorCode=AUTH_003`.
- Successful decision sets status `APPROVED` or `REJECTED`, `resultBy`, and `resultAt`.
- Legacy controller does not resume execution directly despite injected engine services.

## Coordinator Action

Coordinator implemented the final-module compatibility seed locally after receiving this sidecar contract, without idle waiting.
