# P1-005A2 - Event Attribute Discovery And Internal CDP Event Spec

Priority: P1
Sequence: 005A2
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p1-005a2-event-attribute-discovery-and-internal-cdp-event-plan.md`

Implementation status: implemented on 2026-06-05. The actual Flyway migration is
`backend/canvas-engine/src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql`;
the original `V97_2` filename was superseded by existing migration ordering.

## Goal

Add event attribute discovery and emit a compact internal CDP event after each accepted event log row commits.

## Current Baseline

- P1-005A persists accepted CDP events.
- `event_definition` has no auto-discovery toggle.
- There is no event attribute review table and no internal CDP ingestion event for downstream consumers.

## In Scope

- `event_definition.auto_discover` and `event_definition.discovery_mode`.
- `event_attr_definition` with inferred type, sample value, first/last seen time, and review status.
- `EventAttributeDiscoveryService`.
- `CdpEventPublisher` that publishes tenant id, event log id, message id, event code, user id, anonymous id, event time, and properties after accepted row commit.
- P1-005A ingestion service extension that calls discovery and publisher.

## Out Of Scope

- Attribute approval UI; split into P1-005A3.
- Webhook activation delivery; split into P1-005B and consumes this event shape.
- Computed profile/tag/audience consumers; split into P1-006 series.

## Functional Requirements

1. Unknown properties on auto-discoverable event definitions create or update `PENDING_REVIEW` attribute rows.
2. Attribute type inference returns `NUMBER`, `BOOLEAN`, `DATE`, `JSON`, or `STRING`.
3. Existing attribute rows update `lastSeenAt` without resetting review status.
4. One internal event is emitted for each accepted `cdp_event_log` row.
5. Duplicate events do not emit another internal event.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventAttrDefinitionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventAttrDefinitionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventPublisher.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- `backend/canvas-engine/src/main/resources/application.yml`

## Acceptance Criteria

- Schema test proves discovery fields and table exist.
- Discovery tests prove create, update, and type inference.
- Ingestion tests prove accepted rows publish internal events and duplicates do not.

## Verification Evidence

- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=EventAttributeDiscoverySchemaTest,EventAttributeDiscoveryServiceTest,CdpEventPublisherTest,CdpEventIngestionServiceTest -DfailIfNoTests=true`
- Result on 2026-06-05: 11 tests run, 0 failures, 0 errors, 0 skipped.
