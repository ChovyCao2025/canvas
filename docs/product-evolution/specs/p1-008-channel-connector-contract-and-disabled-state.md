# P1-008 - Channel Connector Contract And Disabled State Spec

Priority: P1
Sequence: 008
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/archive/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p1-008-channel-connector-contract-and-disabled-state-plan.md`

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

## Goal

Introduce explicit channel connector contracts and fail-closed disabled connector behavior for send-like handlers.

## Current Baseline

- Implemented on 2026-06-05: `ChannelConnector`, `ChannelConnectorRegistry`, `DisabledChannelConnector`, `ChannelConnectorJdbcRepository`, `ChannelConnectorDO`, and `ChannelConnectorMapper` exist.
- Actual migration is `V120__channel_connector_contract.sql` because `V102` was already used by `V102__event_attribute_discovery_internal_event.sql`.
- `SendMessageHandler` now resolves connector mode through `ChannelConnectorRegistry` when the registry is injected.
- Disabled connectors fail closed with visible output fields and do not call `ReachDeliveryService`.
- Sandbox connectors return deterministic fake external message IDs and do not call real delivery.
- Existing one-argument `SendMessageHandler` construction remains available for legacy focused tests.

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

## Implementation Notes

- `AbstractSendMessageHandler` keeps the legacy delivery path when no registry is supplied, and uses registry-backed connector resolution when a registry is present.
- Handler output includes `connectorMode`, `connectorProvider`, `connectorStatus`, `connectorReason`, and `externalMessageId` where applicable.
- `NodeResult.fail(String, Map<String, Object>)` preserves disabled connector output for auditing and frontend visibility.
- REAL connector mode currently validates that a connector is enabled/registered, then persists through the existing `ReachDeliveryService` path. Provider backpressure, fallback, dedupe, and richer provider dispatch remain split into P1-008B.
- The current focused Maven `test` goal is blocked by unrelated global testCompile drift, so P1-008 tests were verified with isolated compilation/runners against production `target/classes`.

## Verification

- `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed.
- Isolated P1-008 suite passed: `ChannelConnectorSchemaTest`, `ChannelConnectorRegistryTest`, `SendMessageHandlerTest`, `ChannelConnectorHandlerTest` (8 tests).
- Isolated affected handler regression passed: `CouponHandlerTest`, `CommitActionHandlerTest`, `ApiCallHandlerRateLimitTest` (25 tests).
