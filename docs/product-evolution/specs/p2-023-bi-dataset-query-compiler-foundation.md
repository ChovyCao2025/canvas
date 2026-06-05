# P2-023 - BI Dataset Query Compiler Foundation Spec

Priority: P2
Sequence: 023
Source: `docs/product-evolution/specs/p2-007-analytics-command-center.md`, `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`
Implementation plan: `../plans/p2-023-bi-dataset-query-compiler-foundation-plan.md`

## Goal

Add a safe query compiler foundation for marketing BI datasets so future analytics screens can query Doris DWS tables through registered datasets, dimensions, metrics, filters, and sorts without accepting raw SQL from operators.

## Current Baseline

- Doris query support exists for selected canvas stats endpoints.
- P2-022 adds CDP warehouse ingestion and aggregation jobs.
- There is no reusable BI query compiler that can validate dataset fields and bind filter parameters.

## In Scope

- BI dataset, field, metric, request, filter, sort, and compiled-query value objects.
- A compiler that builds parameterized SQL from a registered dataset only.
- A first registry entry for `canvas_dws.canvas_daily_stats`.
- Tests for grouped query SQL, bound filters, unknown field rejection, unknown metric rejection, and limit bounds.

## Out Of Scope

- BI UI.
- Arbitrary custom SQL.
- Cross-dataset joins.
- Row-level permission policies beyond tenant filtering.
- Query execution service, caching, and exports.

## Functional Requirements

1. The compiler must inject `tenant_id = ?` as the first predicate and first bound parameter.
2. Dimensions must come from registered field specs with role `DIMENSION`.
3. Metrics must come from registered metric specs.
4. Filters must compile to SQL placeholders and bound values.
5. Sorts must reference registered fields or metric aliases.
6. Limits must be positive and no greater than 10,000.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFieldSpec.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiMetricSpec.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasetSpec.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFilter.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiSort.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryRequest.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiCompiledQuery.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCompiler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistry.java`

## Acceptance Criteria

- `BiQueryCompilerTest` proves grouped SQL, tenant predicates, bound filters, unknown field rejection, unknown metric rejection, and limit rejection.
- `MarketingBiDatasetRegistryTest` proves `canvas_daily_stats` exposes core fields and metrics.
- Focused backend tests pass for BI query compiler and registry.
