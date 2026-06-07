# P1-005C - Analytics Web SDK Foundation Spec

Priority: P1
Sequence: 005C
Source: `docs/optimization/todo/2026-05-30-cdp-sdk-design.md`, `docs/optimization/todo/cdp_gap_analysis.md`
Implementation plan: `../plans/p1-005c-analytics-web-sdk-foundation-plan.md`

## Goal

Publish a minimal TypeScript browser analytics SDK that sends governed batches to `/cdp/events/track` and enforces identity, queueing, consent, and reset behavior.

## Current Baseline

- Implemented and merged into `main` on 2026-06-05.
- The repository now has a self-contained `sdk/analytics-web` package.
- P1-005 defines write-key authentication and P1-005A defines the server-side batch ingestion protocol.
- The full SDK design describes plugin pipeline, auto-track, visual tracking, mobile SDKs, and heatmap capabilities; this slice intentionally starts with the smallest reliable Web SDK.

## User And Business Value

Frontend teams can integrate a supported client library instead of hand-coding HTTP requests. Product teams get consistent `messageId`, `anonymousId`, session, page, campaign, and library context on every event.

## In Scope

- Add `sdk/analytics-web` with TypeScript source, tests, package metadata, and README.
- Export `CanvasAnalytics` and `createAnalytics`.
- Support `load`, `track`, `identify`, `page`, `group`, `alias`, `optIn`, `optOut`, `hasOptedOut`, `flush`, and `reset`.
- Persist queue, `anonymousId`, and `userId` in storage when enabled.
- Build Basic Auth as `base64(writeKey + ":")`.
- Send batches to `/cdp/events/track` with stable `messageId` and SDK context.
- Implement delayed consent mode: events stay in memory before opt-in and are not persisted or sent while opted out.

## Out Of Scope

- Auto-click, stay-duration, heatmap, and visual tracking.
- iOS, Android, React Native, Flutter, Node.js, Java, Python, or Go SDKs.
- Plugin marketplace and public developer portal.
- Bundling and publishing to a public npm registry.

## Functional Requirements

1. `track` must enqueue a `track` event with event name, properties, `messageId`, timestamp, and context.
2. `identify` must set `userId` and enqueue an `identify` event with traits.
3. `page`, `group`, and `alias` must use the shared event envelope.
4. `flush` must send the current queue in one batch and remove sent events only after a successful 2xx response.
5. `optOut({ clearPersistence: true })` must clear queue and identity storage and prevent sending.
6. `optIn()` must allow future events to be queued and sent.
7. `reset()` must clear `userId`, `anonymousId`, and queued events.
8. README must include immediate load, delayed consent load, track, identify, page, opt-in, opt-out, flush, reset, and request payload examples.

## Technical Scope

### SDK Touchpoints

- `sdk/analytics-web/package.json`
- `sdk/analytics-web/package-lock.json`
- `sdk/analytics-web/tsconfig.json`
- `sdk/analytics-web/src/index.ts`
- `sdk/analytics-web/src/index.test.ts`
- `sdk/analytics-web/README.md`

## Dependencies

- Depends on P1-005 write-key authentication plus the P1-005A server endpoint and batch protocol.
- P1-005B does not block this SDK because webhooks consume server-side events after ingestion.

## Risks And Controls

- Consent risk: default to no persistence or send when `optedOut` is true.
- Duplicate risk: preserve generated `messageId` across retry.
- Browser compatibility risk: first slice uses `fetch`; beacon/image fallback can be added later.
- Package drift risk: keep the SDK package self-contained with its own `npm test` command.

## Acceptance Criteria

- SDK tests pass for initialization, track, identify, page, group, alias, opt-in, opt-out, flush, and reset.
- `flush` sends Basic Auth and a payload compatible with P1-005A.
- Opt-out prevents outbound network calls.
- README includes the expected integration examples and payload contract.

## Implementation Status

- Status: implemented and merged into `main` on 2026-06-05.
- Added `CanvasAnalytics` and `createAnalytics` with load, track, identify, page, group, alias, opt-in, opt-out, flush, reset, queue inspection helpers, Basic Auth batch sending, stable `messageId`/`idempotencyKey`, consent gating, and optional persistence.
- SDK verification: `cd sdk/analytics-web && npm test` passed; `cd sdk/analytics-web && npm run build` passed.
