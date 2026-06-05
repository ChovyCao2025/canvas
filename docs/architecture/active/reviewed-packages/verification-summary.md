# Architecture Verification Summary

Date: 2026-06-03

## Scope Checked

All 35 Markdown files under the original `docs/architecture` tree were inventoried: the current `index.md` plus 34 archived source documents. The coverage matrix maps every archived document and every numbered remediation point to an active priority package or a decision-dependent bucket. High-risk and previously ambiguous claims were re-checked against current code, configuration, migrations, package manifests, and tests.

## Confirmed Or Mostly Confirmed

- Default datasource credentials are still `root/root` in `backend/canvas-engine/src/main/resources/application.yml:8`.
- Event report secret still has a weak default in `application.yml:55`.
- CORS still allows wildcard origins with credentials in `WebConfig.java:36-45`.
- Direct execution, behavior trigger, and ops endpoints are still `permitAll()` in `SecurityConfig.java:64-70`.
- Generic 500 responses still include exception messages in `GlobalExceptionHandler.java:84`.
- `@Valid` / `@Validated` is absent from all 29 controller files.
- Reactive/blocking hazards remain: `.block()`, fire-and-forget `.subscribe()`, `Thread.sleep()`, and many `@Transactional` boundaries coexist with WebFlux.
- `CircuitBreakerRegistry` state transitions are still volatile + atomic-counter combinations without a single atomic state machine.
- `CanvasSchedulerService.closed` remains non-volatile.
- `CanvasService.publish()` and `CanvasTransactionService.publishDb()` still allow a KILLED canvas to become PUBLISHED because no previous-state guard exists.
- `CanvasService.updateDraft()` still updates runtime limits and validity fields without checking whether the canvas is already published.
- `data_source_config.password` is still a plain column and demo migrations contain `root/root`.
- `tenant_id` columns added by V78 remain nullable, and core `CanvasDO` has no `tenantId` field.
- `DagEngine`, `CanvasExecutionService`, and `frontend/src/pages/canvas-editor/index.tsx` are still very large files.
- Logback includes `traceId`, but there is no matching MDC/tracing implementation in current code.
- `GroovyHandler` still exposes the full `ExecutionContext` through a `ctx` binding.
- `CdpUserService.ensureUser()` still uses select-then-insert without duplicate-key recovery, while the schema has a unique `user_id`.
- `frontend/package.json` still lacks an explicit `dayjs` dependency although editor and stats pages import it.
- The cited cleanup comments, deprecated version API, and wildcard imports still exist.

## Partially Confirmed Or Corrected

- The old claim "JWT Secret has no startup validation" is stale. `JwtUtil` now fails fast when `CANVAS_JWT_SECRET` is blank or too short. The remaining issue is deployment/configuration hygiene, not missing validation.
- "Zero tests" is stale. Backend and frontend tests exist. The correct finding is missing integration/critical-path coverage, especially Testcontainers-style DB/Redis/RocketMQ tests and full canvas execution paths.
- The old frontend autosave race claim is partly stale. The editor now keeps the latest save snapshot in a ref and serializes concurrent saves through `savingPromiseRef`, but there is still no component/browser regression coverage for autosave behavior.
- Some prior Redis route consistency issues were partially addressed by DB-only transaction methods and transaction-external route cleanup. Remaining risk exists around rollback semantics, eventual consistency, and missing outbox/repair jobs.
- Some graceful-shutdown pieces exist, including several `@PreDestroy` methods. The remaining issue is incomplete in-flight drain, unmanaged virtual threads, and missing `server.shutdown: graceful`.

## Needs Review

- External production claims such as real RTO/RPO, Redis HA topology, cloud deployment hardening, and cost model accuracy cannot be fully proven from this repository alone.
- Long-term architecture choices in `evolution/` need product and team-capacity decisions before becoming implementation work.

## Architecture Boundary Check

The P3 boundary decision was verified against current backend packages, controllers, mappers, and migrations. The supporting report is [../archive/completed/specs/P3-00-architecture-boundary-code-verification.md](../../archive/completed/specs/P3-00-architecture-boundary-code-verification.md).

Result: the seven bounded contexts are supported by current code anchors, but physical service extraction is not ready. The main blockers are shared `dal` ownership, handler direct mapper access, Canvas/Execution/CDP cross-imports, embedded Reach delivery, Platform tenant usage cross-reads, and incomplete tenant propagation from `V78__saas_foundation.sql`.
