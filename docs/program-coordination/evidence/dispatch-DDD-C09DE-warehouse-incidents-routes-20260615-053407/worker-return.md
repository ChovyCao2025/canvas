# DDD-C09DE Worker Return

Worker: Feynman `019ec80e-3586-72c0-93f2-bc8b274f9be1`
Status: DONE

## Contract Summary

Legacy route family: `/warehouse/incidents`

Endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/warehouse/incidents` | query `status?`, `limit=20`; returns `IncidentView[]` |
| POST | `/warehouse/incidents/{id}/ack` | body `{ "operator"?: string }`; returns `boolean` |
| POST | `/warehouse/incidents/{id}/resolve` | body `{ "operator"?: string }`; returns `boolean` |

Compatibility details used by coordinator:

- Response envelope is legacy `R` shape: `code`, `message`, `errorCode`, `data`, `traceId`.
- Incident view fields are `id`, `tenantId`, `incidentKey`, `sourceType`, `sourceId`, `severity`, `status`, `title`, `description`, `occurrenceCount`, `firstSeenAt`, `lastSeenAt`, `acknowledgedBy`, `acknowledgedAt`, `resolvedBy`, `resolvedAt`.
- `status` filters are optional and normalized by trimming and uppercasing.
- `limit <= 0` falls back to `20`; `limit > 100` is bounded to `100`.
- List ordering is `lastSeenAt DESC, id DESC`.
- `ack` updates only `OPEN`; `resolve` updates `OPEN` or `ACKNOWLEDGED`.
- Wrong tenant, missing row, or invalid lifecycle returns `false`.
- Missing or blank operator falls back to `"operator"` and is not derived from tenant context username.

## Coordinator Action

Coordinator implemented the final-module compatibility seed locally in the default workspace after receiving this sidecar contract, without waiting idly.
