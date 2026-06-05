# P1-005B - Webhook Subscription Schema And Signing Spec

Priority: P1
Sequence: 005B
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p1-005b-webhook-subscription-schema-and-signing-plan.md`

## Goal

Add tenant-scoped webhook subscription storage, delivery log storage, callback URL validation, and HMAC signing primitives.

## Current Baseline

- Implemented on 2026-06-05.
- `webhook_subscription` and `webhook_delivery_log` are created by `V103__webhook_subscription_schema.sql`.
- `OutboundUrlValidator` already validates outbound HTTP URLs and blocks local/private hosts.
- P1-005A2 provides the internal CDP event shape and P1-005A provides enriched event identity fields.

## In Scope

- `webhook_subscription` and `webhook_delivery_log` tables.
- Data objects and mappers.
- `WebhookSignatureService` that signs `timestamp + "\n" + rawPayload` with HMAC-SHA256.
- Subscription validation helper that requires exact event types and safe callback URLs.

## Out Of Scope

- HTTP dispatch, retry classification, and attempt scheduling; split into P1-005B2.
- Subscription CRUD controller, test delivery endpoint, and operator UI; split into P1-005B3.
- Inbound webhooks that trigger canvases.

## Functional Requirements

1. Callback URLs must pass `OutboundUrlValidator` before save or test delivery.
2. Subscription event types must be stored as a JSON array and matched exactly.
3. Subscription secret must not be returned after create or rotation.
4. Signatures use HMAC-SHA256 over `timestamp + "\n" + rawPayload`.
5. Delivery log rows must be able to track delivery id, attempt count, status, HTTP status, next retry time, and terminal reason.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V103__webhook_subscription_schema.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookSubscriptionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookDeliveryLogDO.java`
- Matching MyBatis-Plus mappers.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookSignatureService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionValidator.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookSignatureServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionValidatorTest.java`

## Acceptance Criteria

- Schema tests prove subscription and delivery log fields exist.
- Signature tests prove deterministic HMAC headers.
- Validator tests reject localhost/private callback URLs and blank event type lists.

## Implementation Status

- Status: implemented on 2026-06-05.
- Production compile: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed.
- Focused backend tests were added, but Maven `testCompile` is currently blocked by unrelated existing test-source errors before these tests can run. Known blockers include duplicate `KillSwitchSubscriberTest`, missing P2-079 automation-run classes, stale constructor/API expectations in existing tests, and missing `ExecutionContext#getNodeOutput(...)` expected by `ExecutionContextNamespaceTest`.
