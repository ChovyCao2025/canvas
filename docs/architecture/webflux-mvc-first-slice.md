# WebFlux MVC First Slice

Date: 2026-06-05

Status: deferred. No endpoint migration is approved by this document.

## Decision

The first endpoint group is explicitly deferred. The current inventory shows that runtime migration would touch 89 WebFlux controller files and many blocking dependency wrappers, so the first executable work is characterization and gate closure, not code migration.

If the gates later pass, the lowest-risk candidate endpoint group is platform/admin CRUD, starting with `TenantController` or `ApiDefinitionController`. These routes are lower risk than canvas lifecycle, execution ingress, notification/reach, CDP/audience, BI/warehouse, and provider integration paths because they are not runtime hot-path execution endpoints and do not need streaming.

## Deferred Implementation Boundary

The following must not be changed under the current P3-05 decision:

- no replacement of `spring-boot-starter-webflux` with `spring-boot-starter-web`;
- no Tomcat or virtual-thread runtime switch;
- no controller return-type migration from `Mono<R<...>>` to synchronous `R<...>`;
- no removal of `boundedElastic` wrappers just because MVC is being considered;
- no route, DTO, error, auth, tenant, actuator, or frontend API contract change.

## Characterization Tests Before Any Slice

Any future migration slice must first add tests for:

| Area | Required proof |
|---|---|
| request | Query params, path variables, JSON body, empty body, invalid body, raw signed body if applicable. |
| response | `R` wrapper shape, status mapping, headers, pagination shape, empty result behavior. |
| error | Validation failure, auth failure, tenant failure, not found, duplicate, provider timeout, and unexpected exception mapping. |
| auth | Same principal requirements, role restrictions, super-admin checks, anonymous denial behavior. |
| transaction | Commit and rollback behavior on create, update, delete, activation, and side-effect failure. |
| actuator | Health, metrics, readiness, liveness, and route observability remain visible after runtime changes. |
| tenant | Tenant context resolution, super-admin tenant override, cross-tenant denial, and missing tenant behavior. |
| frontend | Existing service tests pass without endpoint payload changes. |

## Candidate Admin CRUD Slice If Approved Later

| Item | Proposed value |
|---|---|
| endpoint group | Platform/admin CRUD. |
| Candidate routes | `/admin/tenants`, `/canvas/api-definitions`. |
| Blocking dependencies | MyBatis-backed services and tenant context resolution. |
| Expected reactive value | Low. |
| Compatibility window | At least one release where old WebFlux behavior and new MVC behavior are compared by contract tests and smoke checks. |
| Rollback trigger | Any route, response wrapper, error code, auth, tenant, actuator, or latency regression outside the accepted threshold. |
| Rollback action | Revert the runtime slice to WebFlux route handling and keep the same database state. |

## Rollback Steps For A Future Slice

1. Stop rollout if smoke tests or contract tests fail.
2. Revert controller/runtime changes for the selected endpoint group.
3. Restore the previous web starter and runtime configuration if they changed.
4. Keep database migrations out of the runtime migration unless separately approved.
5. Run targeted backend controller tests, security route tests, transaction tests, and frontend API tests.
6. Capture diff, test output, metric signal, and incident decision in the evidence file.

## Compatibility Window

The compatibility window is undefined while the implementation is deferred. A future accepted slice must name:

- release range;
- owner;
- traffic percentage or environment scope;
- smoke command;
- rollback command;
- dashboard or metric link;
- frontend and backend contract test commands.

## Exit Criteria To Select A Slice

A first endpoint group can be selected only after:

- `docs/architecture/adr/webflux-vs-mvc.md` is changed from `Deferred` to `Accepted`;
- P0 reactive review candidates are closed or risk-accepted;
- benchmark evidence favors the selected runtime;
- characterization tests pass on current WebFlux behavior;
- rollback and compatibility window are approved by backend, frontend, and operations owners.
