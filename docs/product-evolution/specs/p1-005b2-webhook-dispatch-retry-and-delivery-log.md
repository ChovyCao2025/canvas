# P1-005B2 - Webhook Dispatch Retry And Delivery Log Spec

Priority: P1
Sequence: 005B2
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p1-005b2-webhook-dispatch-retry-and-delivery-log-plan.md`

## Goal

Deliver internal CDP/journey/profile/tag/audience events to active webhook subscriptions with signed HTTP requests, bounded retry, and auditable delivery logs.

## Current Baseline

- Implemented and merged into `main` on 2026-06-05.
- P1-005B adds webhook subscription and delivery log storage plus signature primitives.
- `WebhookDispatcherService`, `WebhookRetryPolicy`, and `WebhookDeliveryPayload` live under `org.chovy.canvas.domain.cdp`.
- P1-005A2 emits internal CDP events that can be used in tests as synthetic payloads.

## In Scope

- Subscription matching by exact event type.
- Signed HTTP delivery with `X-Canvas-Event`, `X-Canvas-Delivery`, `X-Canvas-Timestamp`, and `X-Canvas-Signature`.
- Delivery log insert/update for every attempt.
- Retry classification:
  - network failure, 5xx, and 429 schedule retry until `max_attempts`;
  - non-429 4xx marks `FAILED`;
  - attempts beyond `max_attempts` marks `DEAD`.
- Deterministic bounded backoff.

## Out Of Scope

- Subscription management API and UI; split into P1-005B3.
- Provider-specific destination adapters.
- Public developer portal.

## Functional Requirements

1. Only active subscriptions with matching event type receive a delivery.
2. Every delivery attempt writes or updates a `webhook_delivery_log` row.
3. Retryable failures set `nextRetryAt`.
4. Terminal failures set final status and terminal reason.
5. Dispatcher tests must not call real external systems.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookDispatcherService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookRetryPolicy.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookDeliveryPayload.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookDispatcherServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookRetryPolicyTest.java`

## Acceptance Criteria

- Dispatcher tests prove matching, headers, log writes, and no delivery to inactive subscriptions.
- Retry policy tests prove 429/5xx/network retry and non-429 4xx terminal failure.
- Dead-letter tests prove attempts beyond the configured maximum mark `DEAD`.

## Implementation Status

- Status: implemented and merged into `main` on 2026-06-05.
- P1-005B2 adjustment: `WebhookDispatcherService` now uses exact event-type matching only and sends the canonical headers `X-Canvas-Event`, `X-Canvas-Delivery`, `X-Canvas-Timestamp`, and `X-Canvas-Signature`.
- Production compile: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed.
- Focused webhook tests pass in an isolated runner because Maven `testCompile` is currently blocked by unrelated existing test-source errors. Covered tests: `WebhookSubscriptionSchemaTest`, `WebhookSignatureServiceTest`, `WebhookSubscriptionValidatorTest`, `WebhookDispatcherServiceTest`, and `WebhookRetryPolicyTest`.
