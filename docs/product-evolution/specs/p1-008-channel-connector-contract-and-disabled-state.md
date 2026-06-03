# P1-008 - Channel Connector Contract And Disabled State Spec

Priority: P1
Sequence: 008
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p1-008-channel-connector-contract-and-disabled-state-plan.md`

## Goal

Introduce explicit channel connector contracts and fail-closed disabled connector behavior for send-like handlers.

## Current Baseline

- `SendMessageHandler` reads a `channel` value and delegates to `ReachDeliveryService`.
- `ReachDeliveryService` writes `message_send_record` and calls a configured reach platform.
- `CouponHandler` calls a coupon service directly.
- There is no connector mode, capability metadata, connector health abstraction, or disabled-state result.

## In Scope

- `ChannelConnector` contract for mode, health, capabilities, send, and receipt parsing.
- Registry that resolves tenant/channel/provider to a real, sandbox, or disabled connector.
- Disabled connector fail-closed behavior with visible reason.
- First integration for `SendMessageHandler` and `ReachDeliveryService`; coupon stays direct until P1-008B.
- Connector registry schema `V102__channel_connector_contract.sql`.

## Out Of Scope

- Provider backpressure, fallback routing, and cross-canvas dedupe; split into P1-008B.
- Operator UI and management API; split into P1-008C.
- Building every external provider integration.

## Functional Requirements

1. Missing provider config must resolve to a disabled connector, not a null connector.
2. Handler output must include connector mode, provider, status, and disabled reason when blocked.
3. Sandbox mode must return a deterministic fake external message id and not call real providers.
4. REAL mode must call the connector and persist delivery result through the existing delivery path.

## Acceptance Criteria

- Registry tests prove real, sandbox, and disabled resolution.
- Handler tests prove missing provider fails closed with output reason.
- Existing `SendMessageHandlerTest` remains green.
