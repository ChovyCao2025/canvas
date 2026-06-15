# DDD-C09DG Worker Return

Worker: Nash `019ec820-ef65-7810-83e4-dc9df1d91120`
Status: DONE

## Contract Summary

Legacy route family: `/warehouse/slo-policies`

Endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/warehouse/slo-policies` | optional query `status`; returns policy list |
| GET | `/warehouse/slo-policies/effective` | optional query `policyKey`, default `WAREHOUSE_READINESS_DEFAULT`; returns effective policy |
| POST | `/warehouse/slo-policies` | nullable body; upserts policy fields and returns policy |

Compatibility details used by coordinator:

- Response envelope is legacy `R.ok(data)` shape: `code`, `message`, `errorCode`, `data`, `traceId`.
- `IllegalArgumentException` maps to HTTP 400 with `errorCode=API_001`, matching neighboring final controllers.
- Data fields are `id`, `tenantId`, `policyKey`, `displayName`, `offlineWarnRunGapMinutes`, `offlineFailRunGapMinutes`, `offlineWarnWatermarkLagMinutes`, `offlineFailWatermarkLagMinutes`, `audienceWarnRunGapMinutes`, `audienceFailRunGapMinutes`, `status`, `ownerName`, `description`.
- Missing tenant defaults to `0L`.
- Missing policy key defaults to `WAREHOUSE_READINESS_DEFAULT`; policy key and status are uppercased.
- Missing status defaults to `ACTIVE`.
- Default thresholds are `120/360`, `30/120`, and `1440/4320`.
- Blank owner and description become null.
- Threshold values must be positive and each warn threshold must be less than or equal to its fail threshold.
- Tenant-specific policies override global policies for list and effective resolution.
- Effective policy only uses active rows and falls back to an in-code default.
- The route has no `operator` field; final compatibility must not invent one.

## Coordinator Action

Coordinator implemented the final-module compatibility seed locally after receiving this sidecar contract, without idle waiting.
