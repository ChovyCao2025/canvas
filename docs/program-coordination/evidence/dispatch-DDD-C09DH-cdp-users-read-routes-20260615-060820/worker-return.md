# DDD-C09DH Worker Return

Worker: Banach `019ec829-9e8c-78c3-8922-a77df56e6758`
Status: DONE

## Contract Summary

Legacy route family: `/cdp/users`

Missing final-module endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/cdp/users` | optional query `keyword`; returns CDP user row list |
| GET | `/cdp/users/{userId}` | returns CDP user profile detail |
| GET | `/cdp/users/{userId}/insight` | returns profile, current tags, and canvas summary rows |

Already covered by existing `CdpUserTagController` and out of scope for this batch:

- `POST /cdp/users/{userId}/tags`
- `GET /cdp/users/{userId}/tags`
- `GET /cdp/users/{userId}/tag-history`
- `DELETE /cdp/users/{userId}/tags/{tagCode}`

Compatibility details used by coordinator:

- Success envelope is `{ code: 0, message: "success", errorCode: null, data, traceId: null }`.
- Bad request envelope is HTTP 400 with `errorCode=API_001`.
- Missing tenant should follow adjacent CDP user tag route compatibility and default to `7L`.
- List row fields are `userId`, `displayName`, `executionCount`, `successCount`, `failedCount`, `latestStatus`, `firstEnteredAt`, `lastEnteredAt`, `tags`.
- Detail profile fields are `userId`, `displayName`, `phone`, `email`, `status`, `propertiesJson`, `firstSeenAt`, `lastSeenAt`.
- Insight fields are `userId`, `profile`, `tags`, `canvasRows`.
- Blank or unknown user ids map to `IllegalArgumentException` from the service layer and then to the API_001 envelope.
- Meaningful tests should cover the three missing routes, tenant default/override, data shape, and bad-request mapping; tag-route tests should not be duplicated.

## Coordinator Action

Coordinator implemented only the missing final-module read routes locally after receiving this sidecar contract, without idle waiting.
