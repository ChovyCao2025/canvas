# Tenant Platform Contract

Date: 2026-06-05

Status: P3-09 tenant contract. Existing P0 tenant isolation remains the baseline; this contract defines the shared platform rules for future boundaries.

## Tenant Visibility

Tenant visibility rules:

- Tenant users can see only rows owned by their tenant.
- Tenant admins can manage only their tenant unless an explicit delegated administration rule exists.
- Platform admins may query across tenants only through admin-only APIs with audit.
- System jobs must declare explicit tenant ID or explicit system scope.
- Null tenant context is not allowed for tenant-scoped business APIs.
- Tenant ID `0` or null-like values must not be treated as wildcard outside documented global configuration tables.

## Tenant Quota

Tenant quota is a platform contract, not ad hoc controller logic.

Quota dimensions:

- max active canvases;
- execution concurrency;
- execution request replay rate;
- audience compute concurrency;
- connector send rate;
- storage and retention budget;
- AI token or request budget;
- dashboard/export job budget.

Quota decisions must record:

- tenant ID;
- quota key;
- limit;
- current usage source;
- fail-open or fail-closed behavior;
- reset window;
- owner;
- override reason;
- audit event.

## Tenant-Scoped Query

Tenant-scoped query rules:

- every mapper query that reads tenant-owned data must include tenant ID or a documented admin/system scope;
- every write must persist tenant ID from trusted context or from an already-owned parent row;
- cross-tenant admin reads must be explicit and auditable;
- asynchronous jobs must restore tenant context from persisted job/request/event data;
- Redis keys, MQ messages, event envelopes, callback ledgers, and caches must include tenant ID or explicit system scope;
- frontend APIs must not pass arbitrary tenant IDs unless the route is admin-only.

## Tenant-Scope-Change Audit

Tenant-scope-change audit is required when:

- a row tenant ID is created, changed, or repaired;
- a user is moved between tenants;
- a platform admin performs cross-tenant read or write;
- an async job uses system scope;
- a migration backfills tenant ID;
- a service boundary or data platform consumer changes tenant visibility.

Audit fields:

- audit event ID;
- tenant ID or affected tenant IDs;
- actor;
- role;
- action;
- row/table/resource;
- before/after tenant reference;
- reason;
- correlation ID;
- occurred-at timestamp.

## Compatibility

Compatibility with existing CDP and canvas tenant fields:

- existing `tenant_id` columns remain source-of-truth for row ownership;
- `TenantContextResolver` remains the request-side source of trusted tenant context;
- `TenantScopeSupport` remains valid for local query scoping;
- existing `canvas`, `canvas_version`, `canvas_execution`, `canvas_execution_trace`, `canvas_execution_request`, `data_source_config`, CDP, notification, BI, and warehouse tenant columns must not be bypassed;
- admin routes that use null tenant context must be reviewed before extraction;
- future platform library or service must preserve current API response shapes and error codes during compatibility window.

## Required Contract Tests

- tenant user cannot read another tenant's row;
- tenant admin cannot update another tenant's connector, canvas, CDP user, or execution request;
- platform admin cross-tenant action produces audit;
- async execution carries tenant from persisted request or canvas row;
- event envelope carries tenant ID;
- Redis/MQ keys and payloads include tenant or system scope;
- missing tenant fails closed except documented global config reads.
