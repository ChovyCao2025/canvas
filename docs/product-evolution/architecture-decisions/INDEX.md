# Architecture Decisions Manifest

Purpose: inventory runtime-migration architecture decision records so future
agents can find proof gates without treating ADRs as implementation work.

The policy and dependency rules for this directory live in [README](README.md).
An ADR can name a future child spec, but that is not proof that runtime work
has been implemented or verified.

| File | Scope |
| --- | --- |
| [README](README.md) | ADR field requirements, dependency graph, candidate status table, and corrected old-plan gaps. |
| [ADR-001 Runtime Web Stack](ADR-001-runtime-web-stack.md) | WebFlux versus MVC/virtual-thread migration evidence gate. |
| [ADR-002 DAG Engine Execution Model](ADR-002-dag-engine-execution-model.md) | DAG execution rewrite evidence gate. |
| [ADR-003 Delivery And MQ Topic Split](ADR-003-delivery-and-mq-topic-split.md) | Delivery outbox sequencing and MQ topic split boundary. |
| [ADR-004 Script Engine Sandbox](ADR-004-script-engine-sandbox.md) | Script engine replacement and sandbox compatibility gate. |
| [ADR-005 Audience Bitmap Identity Mapping](ADR-005-audience-bitmap-identity-mapping.md) | Audience bitmap remapping and identity collision evidence gate. |
| [ADR-006 Trace OLAP Storage](ADR-006-trace-olap-storage.md) | Trace storage migration and OLAP sink dependency boundary. |
| [ADR-007 Service Split Boundaries](ADR-007-service-split-boundaries.md) | Service split sequencing after hardening evidence. |
