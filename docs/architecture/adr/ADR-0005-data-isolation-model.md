# ADR-0005 Data Isolation Model

## Status

Accepted

## Context

The repository now contains tenant context resolution, tenant-scoped data objects, controller tests, and migration evidence for core tenant fields. Some platform and warehouse evolution specs still require clearer service and data boundaries before extraction.

## Decision

Use tenant-scoped access as the default data isolation model for canvas, audience, CDP, execution, notification, and operations APIs. Controllers must resolve `TenantContext` where tenant-owned data is read or written, services must enforce tenant ownership for direct ID access, and operational endpoints must use explicit super-admin or internal-machine controls.

## Alternatives

- Rely on UI filtering only: rejected because backend APIs and jobs can bypass UI state.
- Split every tenant into a separate database now: deferred because it requires provisioning, migration, and cross-tenant operations design.

## Consequences

- Tenant checks remain visible in service/controller tests.
- New migrations must add tenant columns or document why the table is global.
- Data platform specs must preserve tenant and source isolation in warehouse assets.

## Rollback Trigger

Revisit if tenant leakage is found, if major customers require physical database isolation, or if P3-04 multi-datasource isolation approves a stricter runtime boundary.

## Owner

Security and data platform owners.

## Linked Specs

- `docs/architecture/archive/specs/P0-06-data-security-and-tenant-isolation-spec.md`
- `docs/architecture/archive/specs/P3-03-data-platform-architecture-spec.md`
- `docs/architecture/archive/specs/P3-04-multi-datasource-isolation-spec.md`
