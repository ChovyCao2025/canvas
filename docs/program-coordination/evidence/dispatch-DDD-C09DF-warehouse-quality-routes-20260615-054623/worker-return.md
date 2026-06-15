# DDD-C09DF Worker Return

Worker: Linnaeus `019ec819-809f-7981-b002-df6e1d735b05`
Status: DONE

## Contract Summary

Legacy route family: `/warehouse/quality`

Endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/warehouse/quality/checks` | query `limit=20`; returns quality check list |
| POST | `/warehouse/quality/reconcile-ods` | body `{ "from"?, "to"?, "tolerance"?, "operator"? }`; returns quality check result |
| POST | `/warehouse/quality/aggregate-lag` | body `{ "now"?, "maxLagMinutes"?, "operator"? }`; returns quality check result |

Compatibility details used by coordinator:

- Response envelope is legacy `R.ok(data)` shape: `code`, `message`, `errorCode`, `data`, `traceId`.
- `IllegalArgumentException` maps to HTTP 400 with `errorCode=API_001`, matching neighboring final controllers.
- Quality result fields are `id`, `tenantId`, `checkType`, `status`, `sourceCount`, `warehouseCount`, `diffCount`, `windowStart`, `windowEnd`, `thresholdValue`, `details`, `checkedAt`, `createdBy`.
- Missing tenant defaults to `0L`.
- Missing or blank operator defaults to `"operator"`.
- `reconcile-ods` requires `from` and `to`; `from` must be before `to`; negative tolerance is bounded to `0`.
- `aggregate-lag` defaults `maxLagMinutes` to `30`; negative max lag is bounded to `0`.
- `recentChecks` bounds `limit <= 0` to `20`, caps at `100`, and sorts newest first.

## Coordinator Action

Coordinator implemented the final-module compatibility seed locally after receiving this sidecar contract, without idle waiting.
