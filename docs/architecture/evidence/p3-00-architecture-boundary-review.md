# P3-00 Architecture Boundary Review Evidence

Date: 2026-06-05

## Verdict

The current repository still supports the seven bounded-context model, but it is not ready for physical service extraction. The right next step is modular-monolith boundary cleanup: table ownership, explicit ports, contract tests, tenant propagation, observability, rollout, rollback, and reconciliation.

This refresh also confirms that Data Platform / Analytics is no longer only a future document concept. The current worktree contains `domain/warehouse`, `domain/bi`, BI controllers, Doris infrastructure, and many CDP warehouse and BI migrations. Those additions are still inside the same backend, shared mapper package, and shared Flyway stream, so they strengthen the need for an extraction gate rather than weakening it.

## Inventory Commands

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 2 -type d | sort
find backend/canvas-engine/src/main/java/org/chovy/canvas/web -maxdepth 1 -name '*Controller.java' -type f | sort
find backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper -maxdepth 1 -name '*Mapper.java' -type f | sort
find backend/canvas-engine/src/main/resources/db/migration -maxdepth 1 -type f \( -name 'V*.sql' -o -name 'beforeEachMigrate__*.sql' \) | sort -V
rg "Mapper|StringRedisTemplate|RocketMQTemplate|WebClient|@Transactional" backend/canvas-engine/src/main/java/org/chovy/canvas/domain backend/canvas-engine/src/main/java/org/chovy/canvas/engine backend/canvas-engine/src/main/java/org/chovy/canvas/web
```

Observed current-state summary:

- 85 controller files under `backend/canvas-engine/src/main/java/org/chovy/canvas/web`, including CDP warehouse and BI controllers.
- 146 mapper files under the single shared `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper` package.
- 164 Flyway migration files, with 85 files containing `CREATE TABLE`.
- Newer analytics/data-platform anchors exist under `domain/warehouse`, `domain/bi`, `infrastructure/doris`, `infrastructure/bi`, and `web/bi`.

## Seven Bounded Contexts

| Context | Current code anchors | Current extraction blocker |
|---|---|---|
| Canvas Authoring | `domain/canvas`, `web/CanvasController.java`, `CanvasMapper`, `CanvasVersionMapper` | Still imports execution runtime, scheduler, DAG parser, Groovy validation, Redis route/cache, and transaction-side-effect helpers. |
| Execution Runtime | `engine/*`, `engine/trigger`, `engine/scheduler`, `engine/wait`, `domain/execution` | Still owns execution mappers, Redis context persistence, CDP user creation, Disruptor/lane dispatch, and runtime stats in one service boundary. |
| CDP / Audience | `domain/cdp`, `engine/audience`, `domain/analytics`, CDP and audience mappers | CDP query and insight services still read execution and canvas tables directly. |
| Reach / Notification | `domain/notification`, `engine/delivery`, `engine/policy`, send handlers | Delivery, outbox, policy, message records, and external provider calls remain embedded in runtime packages. |
| Integration | `domain/datasource`, channel connector APIs, webhook CDP services, external HTTP handlers | Protocol adapters, credentials, callback flows, and generic DAG handler contracts are not fully separated. |
| Platform | `auth`, `common/tenant`, `domain/tenant`, `domain/meta`, audit/security/config controllers | Tenant usage still reads business tables directly and several controllers normalize missing tenant context locally. |
| Data Platform / Analytics | `domain/warehouse`, `domain/bi`, `domain/analytics`, `infrastructure/doris`, `web/bi`, CDP warehouse/BI migrations | A large in-monolith thin platform exists, but ownership, serving contracts, retention, and physical datasource boundaries are still shared. |

## Direct Cross-Context Imports

| Evidence | Boundary issue |
|---|---|
| `CanvasService.java:14-23`, `:63-82` imports DAG parser, Groovy handler, scheduler, execution service, trigger pre-check, cache, Redis route service, and Redis template. | Canvas Authoring reaches into Execution Runtime and infrastructure directly. |
| `CanvasExecutionService.java:14-35`, `:56-85` imports `CdpUserService`, execution mappers, Redis context/persistence utilities, Disruptor, DAG engine, and runtime metrics. | Execution Runtime mixes runtime orchestration, CDP identity creation, Redis, metrics, and persistence. |
| `CanvasExecutionConfigLoader.java:8-16` imports canvas/version mappers, DAG parser, MQ trigger handler, config cache, and entity cache. | Runtime graph loading still depends on authoring tables and handler details. |
| `CanvasUserQueryService.java:5-8`, `:34-71` reads `CanvasExecutionMapper` directly. | CDP / Audience uses execution tables as its read model instead of a contract. |
| `CdpUserInsightService.java:5-10`, `:52-67` reads `CanvasExecutionMapper` and `CanvasMapper` directly. | CDP insight crosses both Execution Runtime and Canvas Authoring. |
| `TenantService.java:7-14`, `:61-83` reads canvas, execution, and DLQ tables. | Platform usage analytics are coupled to business table shape. |
| `ReachDeliveryService.java:7-12`, `:83-163` writes `MessageSendRecordMapper`, evaluates marketing policy, and calls external reach provider from `engine/delivery`. | Reach / Notification is a plausible candidate but still embedded in runtime and policy packages. |
| `CdpEventIngestionService.java` imports `domain.warehouse.*` services. | CDP ingestion and warehouse checkpoint/retry behavior are coupled inside the monolith and need explicit contracts before service boundaries. |

## Shared Mapper Access

The single `dal/mapper` package remains the main physical blocker. It contains 146 mapper interfaces that are not grouped by bounded context. This makes it easy for one context to read or write another context's tables without a visible contract.

Examples of current shared access:

- Canvas Authoring and Execution Runtime both use canvas/version/execution mappers.
- CDP services use execution and canvas mappers for user insight and directory views.
- Platform tenant usage uses canvas, execution, and DLQ mappers.
- BI and warehouse services use many new mappers in the same shared package instead of context-owned repositories.

## Tenant Propagation Gaps

Tenant propagation has improved since the older review:

- `V78__saas_foundation.sql` adds tenant foundations for tenant, users, system options, canvas, versions, executions, and traces.
- `V92__enforce_core_tenant_not_null.sql` makes core tenant columns non-null.
- `V93__tenant_scope_datasources_and_execution_requests.sql` adds datasource and execution-request tenant scope.
- `V185__production_safety_and_compliance.sql` adds tenant columns and indexes for audience, notification, customer, consent, suppression, message-send, and CDP tables.

Remaining blockers:

- Not all new BI, warehouse, analytics, integration, and operational tables have an extraction-ready tenant ownership policy in a single active map.
- Several controllers still default missing context to `0L` or `system`, which is acceptable for current monolith tests but must be explicit before cross-service contracts.
- There is no global row-policy/interceptor proving every mapper read/write is tenant scoped.
- Cross-context read models do not consistently state whether tenant scope comes from request auth, persisted row ownership, or system context.

## Recommendation

Keep the current backend as a modular monolith. Before extracting any service, complete:

- a context-owned table and repository map;
- explicit API/event/read-model contracts for cross-context calls;
- characterization tests for API shape, mapper reads/writes, tenant behavior, failure behavior, and event/notification behavior;
- tenant, trace, operator, and idempotency propagation across the proposed boundary;
- independent observability, rollout, rollback, and reconciliation plans;
- an accepted candidate ADR that links `docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md`.

Canvas Authoring and Execution Runtime should not be first extraction candidates because graph versioning, scheduler registration, execution context, route/cache behavior, runtime policy, and state transitions remain tightly coupled.

The first candidates to evaluate remain CDP / Audience, Reach / Notification, and Integration / WeCom, but only after their cross-context reads and writes are converted to contracts or read models.

## Verification

Documentation gate checks run on 2026-06-05:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 2 -type d | sort
rg "Mapper|StringRedisTemplate|RocketMQTemplate|WebClient|@Transactional" backend/canvas-engine/src/main/java/org/chovy/canvas/domain backend/canvas-engine/src/main/java/org/chovy/canvas/engine backend/canvas-engine/src/main/java/org/chovy/canvas/web
test -f docs/architecture/evidence/p3-00-architecture-boundary-review.md
rg "Canvas Authoring|Execution Runtime|CDP / Audience|Reach / Notification|Integration|Platform|Data Platform / Analytics|modular monolith" docs/architecture/evidence/p3-00-architecture-boundary-review.md
test -f docs/architecture/decisions/adr/ADR-0000-template.md
test -f docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md
rg "Data Ownership|API Contracts|Event Contracts|Rollback|Observability|Tenant Propagation|Idempotency|Exit Criteria" docs/architecture/decisions/adr/ADR-0000-template.md docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md
```

All documentation gate checks passed.

Characterization command run on 2026-06-05:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=CanvasUserQueryServiceTest,NotificationServiceTest,TenantServiceTest
```

Result: 19 tests run, 0 failures, 0 errors, 0 skipped.

The first Maven attempts were blocked at `testCompile` by untracked BI-resource tests expecting publish-approval gate APIs. The compile blocker was resolved by adding the expected approval-gate surface in `BiPublishApprovalService`; the P3-00 characterization suite then passed without moving packages, tables, or APIs.

No files were staged or committed.
