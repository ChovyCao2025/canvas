# Spec: Data Security And Tenant Isolation

Source package: `docs/architecture/todo/p0/data-security-and-tenant-isolation/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Confirmed.

## Problems

- `data_source_config.password` is stored as a plain `VARCHAR`.
- Migration seed/demo data contains `root/root` JDBC credentials.
- V78 added `tenant_id` as nullable to several core tables.
- Core DO classes such as `CanvasDO` do not expose `tenantId`, so tenant filtering cannot be consistently enforced by ORM-level wrappers.
- Several services use ad hoc tenant filtering, which leaves gaps for future queries and mutations.

## Evidence

- `V71__data_source_config.sql:7`
- `V41__audience_demo_data.sql` and `V43__audience_demo_repoint_to_canvas_demo.sql` demo JDBC credentials.
- `DataSourceConfigDO.java:36`
- `V78__saas_foundation.sql:26-58`
- `CanvasDO.java:1-95` has no `tenantId` field despite `canvas.tenant_id`.
- `TenantService.java` uses manual `tenant_id` query wrappers.

## Acceptance Criteria

- External datasource credentials are encrypted or moved to a credential vault abstraction.
- Demo seed data does not introduce real-looking root credentials into production migrations.
- Tenant columns are non-null where tenant isolation is required.
- Core DO/DTO/query paths carry tenant identity consistently.
- Mutation and list APIs enforce tenant boundaries with tests.
