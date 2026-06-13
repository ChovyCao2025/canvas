# DDD-C09AG Quality Review

Date: 2026-06-13
Reviewer: Lagrange `019ec0ca-087b-7f60-8280-48383fc5b7c8`
Verdict: PASS_WITH_CONCERNS

## Finding

1. Medium: dashboard impact entries are dashboard-level, not widget-level.

   `BiCatalogApplicationService.chartReferenceImpact(...)` filters dashboards by `dashboard.chartKeys().contains(...)` and emits one reference per dashboard. The compact seed preserves legacy field names but does not preserve distinct widget references if a dashboard has multiple widgets for the same chart, nor actual widget key/title semantics from the legacy impact route.

## Coordinator Evaluation

The finding is valid for full legacy parity. Current final-context `BiDashboardRepository` exposes `BiDashboard` with `chartKeys`, not widget key/title references. Although `MybatisBiCatalogRepository` reads `bi_dashboard_widget` internally, that data is collapsed into chart keys before crossing the domain repository boundary.

Fixing true widget-level parity would require expanding the final BI repository/domain contract beyond DDD-C09AG's exact reserved scope. For this compact read-route seed, the limitation is accepted and recorded for a future widget-level BI dashboard/reference follow-up.

## Non-blocking Checks

Lagrange found no blocking issue with:

- route mapping for `GET /canvas/bi/charts/resources/{chartKey}/impact`
- compatibility envelope and response field names
- default `X-Tenant-Id` behavior
- missing/archived chart `API_001` bad request mapping
- sorted dashboard references
- non-null empty portal/subscription arrays for the compact seed
- old-domain import cleanliness in reviewed files

