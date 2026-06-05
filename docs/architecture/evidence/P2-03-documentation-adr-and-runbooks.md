# P2-03 Documentation, ADRs, And Runbooks Evidence

Date: 2026-06-05

## Scope

Implemented and verified active architecture decision records, handler guidance, Redis key catalog, operational runbooks, OpenAPI metadata, frontend API-doc parsing, and archive-source policy for P2-03.

## Implemented Artifacts

- `docs/architecture/decisions/adr/ADR-0000-template.md` plus five initial ADRs for WebFlux/MVC runtime, NodeHandler model, Groovy, Redis/RocketMQ/MySQL choices, and data isolation.
- `docs/architecture/evidence/guides/node-handler-development.md` documents `NodeHandler`, `@NodeHandlerType`, blocking-call rules, trace expectations, mapper access restrictions, and handler test checklist.
- `docs/architecture/evidence/runbooks/dag-execution-flow.md` documents trigger admission through `DagParser`, handler dispatch, wait/resume, trace writes, and completion.
- `docs/architecture/decisions/reference/redis-key-catalog.md` catalogs key prefix, owner service, TTL, payload, invalidation, risk, and cleanup link for Redis key families.
- `docs/architecture/evidence/runbooks/failure-triage.md`, `dlq-replay.md`, `route-rebuild.md`, `cache-invalidation.md`, and `deploy-rollback.md` provide command-oriented runbooks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/OpenApiSecurityConfig.java` defines bearer and trigger HMAC OpenAPI schemes.
- Core Canvas, Execution, and Audience controllers now include OpenAPI tags and operation metadata.
- Frontend API docs preserve response metadata and HMAC auth overrides from OpenAPI/local metadata.
- `docs/architecture/active/specs/README.md` and `docs/architecture/active/plans/README.md` state that archived reviews are historical evidence, not active implementation authority.

## Verification

| Command | Result | Notes |
| --- | --- | --- |
| `rg` documentation checks from P2-03 plan | Passed | ADR template, ADR keywords, DAG/handler guide, Redis catalog, runbooks, and archive policy all matched required terms. |
| `cd backend && mvn test -pl canvas-engine -Dtest=StartHandlerTest,WaitHandlerTest,NodeRouteResolverTest,CanvasEntityCacheTest,CacheConfigTest,OpsControllerTemplateTest,CanvasExecutionDlqSchemaTest,CanvasStatsControllerTest,ExecutionControllerTest,AudienceControllerTest` | Passed | 32 tests, 0 failures. |
| `cd frontend && npm test -- api-docs` | Passed | 3 files, 21 tests, 0 failures. |

## Additional Build Fix

Maven test compilation was blocked by an existing ambiguous constructor call in `AudienceMaterializationScheduleServiceTest`. The test now binds `null` dependencies to concrete service-typed variables before constructing `AudienceMaterializationScheduleService`, so javac resolves the intended package-private test constructor.

## Residual Manual Items

- Runbook commands contain placeholders for production URLs, credentials, dashboards, and evidence paths; operators must fill those values during real incidents.
- No commit was created; the working tree contains unrelated active changes and staging/commit should remain user-controlled.
