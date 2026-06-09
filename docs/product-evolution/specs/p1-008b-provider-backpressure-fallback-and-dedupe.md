# P1-008B - Provider Backpressure, Fallback, And Dedupe Spec

Priority: P1
Sequence: 008B
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/todo/2026-05-31-evolution-directions.md`
Implementation plan: `../plans/p1-008b-provider-backpressure-fallback-and-dedupe-plan.md`

## Goal

Add provider rate-limit decisions, one-level fallback routing, and cross-canvas dedupe for connector-backed delivery.

## Current Baseline

- P1-008 provides connector mode and disabled-state behavior.
- `MessageSendRecordDO` has idempotency support but no cross-canvas dedupe group.
- There is no provider backpressure policy, fallback decision log, or fallback cycle validation.

## In Scope

- Provider backpressure policy keyed by tenant, channel, provider, and operation.
- Redis-backed rate counters with fail-closed REAL mode behavior.
- One-level fallback policy with cycle rejection.
- Fallback decision audit records.
- Cross-canvas dedupe records keyed by tenant, dedupe group, content hash, channel, and user.
- Integrate `SendMessageHandler`, `CouponHandler`, and `ReachDeliveryService` with policy decisions.
- Policy schema `V121__channel_provider_policies.sql`.

## Out Of Scope

- UI and management API; split into P1-008C.
- Multi-level fallback optimization.
- Billing and channel cost accounting beyond carrying cost fields.

## Functional Requirements

1. Provider limits must decide before calling external providers.
2. REAL mode must fail closed when rate-limit state is unavailable.
3. Fallback must record original channel/provider, final channel/provider, decision reason, and attempt chain.
4. Fallback cycles must be rejected at save time.
5. Duplicate content in the configured dedupe window must suppress provider calls.

## Acceptance Criteria

- Backpressure tests cover isolation, per-second limits, daily limits, recovery, Redis unavailable fail-closed, and sandbox bypass.
- Fallback tests prove route selection, disabled target rejection, cycle rejection, and audit output.
- Dedupe tests prove duplicate suppression across canvases.
- Handler regression tests remain green.

## Implementation Status

Status: Completed on 2026-06-05; full Maven test execution is still blocked by unrelated legacy test compilation issues.

- Actual migration is `V121__channel_provider_policies.sql`; earlier migration numbers were already allocated.
- Provider limit, fallback policy/decision, and dedupe repositories/services are present under `org.chovy.canvas.engine.channel`.
- `SendMessageHandler` now evaluates connector mode, dedupe, provider backpressure, and one-level fallback before sandbox or real delivery.
- `ReachDeliveryService` preserves the actual provider in delivery request payloads and outbox-facing request metadata.
- `CouponHandler` now evaluates coupon dedupe and provider backpressure before calling the coupon service.
- Full Maven test execution is still blocked by unrelated legacy test compilation issues, so this slice was verified with isolated javac/reflection runners plus production compile; final post-clean backend focused runner coverage was `44/44` across P1-008B/P1-008C and handler regression tests.
