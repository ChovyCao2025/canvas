# Code Review Post-Merge Reconciliation

Date: 2026-06-03
Merge commit: `a6b6ae5 Merge branch 'fix/code-review-remediation-2026-06-03'`

## Decision

The original `docs/code-review` reports are archived as historical review input. This does not mean every verified issue is fixed.

Active tracking remains in:

- `docs/code-review/verification/2026-06-03-code-review-verification.md`
- `docs/code-review/verification/2026-06-03-code-review-verification-third-pass.md`
- `docs/code-review/specs/2026-06-03-confirmed-code-review-remediation-spec.md`
- `docs/code-review/plans/2026-06-03-confirmed-code-review-remediation-plan.md`
- `docs/code-review/runtime-verification.md`

## Closed Or Partially Closed By The Merge

The merge includes concrete remediation and tests for these areas:

- Production configuration guard and `application-prod.yml`.
- HMAC verification for public direct/behavior trigger entrypoints and shared `CanvasHmacVerifier`.
- `/ops/**` authorization restriction.
- Data source password encryption and Lombok password exclusion.
- In-flight deregistration, circuit breaker, execution context, Disruptor lifecycle, and MQ idempotency regression coverage.
- Frontend token logging removal, API business-error handling, notification reconnect hardening, and editor autosave/clipboard helpers.
- Trace ID support in error responses, a custom health indicator, CI workflow, and runtime verification scripts.

Verification already run for the merge:

- `git diff --cached --check`
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml test`
- `PATH=/opt/homebrew/bin:$PATH npm --prefix frontend test -- --run`
- `PATH=/opt/homebrew/bin:$PATH npm --prefix frontend run build`
- `PATH=/opt/homebrew/bin:$PATH npm --prefix frontend audit --audit-level=critical`
- `bash -n scripts/verify-cors.sh && bash -n scripts/verify-mq-idempotency.sh && bash -n scripts/verify-ws-limit.sh`

## Still Open

These are verified examples of remaining open work. They block any claim that all `docs/code-review` findings are fully remediated:

| Area | Current evidence | Follow-up |
|---|---|---|
| Tenant mapping | `V78__saas_foundation.sql` adds `canvas.tenant_id`, but `CanvasDO` still has no `tenantId` field. | Add entity mapping, query scoping, and tenant regression tests. |
| Audit writes | `canvas_audit_log` exists only in migration; no Java write path was found. | Add audit service/writes for publish/offline/kill/rollback/DLQ/admin actions. |
| Main Dockerfile user | `backend/canvas-engine/Dockerfile` runtime stage has no `USER`. | Add non-root runtime user and verify image metadata. |
| Frontend ErrorBoundary | `frontend/src` has no `ErrorBoundary`, `componentDidCatch`, or `getDerivedStateFromError` match. | Add global and editor-local error boundaries with tests. |
| Transaction rollback policy | Main code still has bare `@Transactional` usages. | Add `rollbackFor = Exception.class` where required and test checked-exception paths. |
| Request validation | DTO/controller validation is still incomplete. | Add Bean Validation constraints and controller tests for invalid payloads. |
| Multi-tenant schema completeness | V78 covers only part of the schema and nullable tenant paths remain. | Add staged migrations, backfill proof, indexes, and query isolation tests. |
| Retention/partition/archive | High-growth tables still need retention or archive policy implementation. | Add retention jobs or partition/archival design with operational tests. |
| Runtime-only findings | Capacity, Redis/MQ failure behavior, Hikari pool sizing, and WebSocket connection limits need environment execution. | Run the scripts and fault-injection checks described in `runtime-verification.md`. |

## Archive Boundary

Move only original review reports to `docs/code-review/archive/original-reports-2026-06-03/`.

Do not archive active tracking documents until their rows are reconciled to `已修复`, `不成立`, accepted risk, or a separately tracked follow-up ticket/spec.
