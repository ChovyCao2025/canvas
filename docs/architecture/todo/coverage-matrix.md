# Architecture Coverage Matrix

Date: 2026-06-03

This matrix audits every archived architecture document and maps its findings or major topics to the active todo queue. It is a coverage map, not proof that every claim is already fixed.

## Status Legend

- `confirmed`: repository evidence supports the issue.
- `partially_confirmed`: the issue exists but the old wording is incomplete, overstated, or partly fixed.
- `stale`: repository evidence shows the old claim no longer applies as written.
- `planning`: future-state design material; keep as planning input until promoted.
- `archive_only`: reference material, not an active issue by itself.
- `needs_review`: requires external production, product, or architecture decision.

## Package Legend

- `P0/security`: `todo/p0/security-hardening`; `archive/specs/P0-01-security-hardening-spec.md`; `archive/plans/P0-01-security-hardening-plan.md`
- `P0/reactive`: `todo/p0/reactive-threading-and-transactions`; `archive/specs/P0-02-reactive-threading-and-transactions-spec.md`; `archive/plans/P0-02-reactive-threading-and-transactions-plan.md`
- `P0/state`: `todo/p0/canvas-state-data-consistency`; `archive/specs/P0-03-canvas-state-data-consistency-spec.md`; `archive/plans/P0-03-canvas-state-data-consistency-plan.md`
- `P0/concurrency`: `todo/p0/execution-concurrency-safety`; `archive/specs/P0-04-execution-concurrency-safety-spec.md`; `archive/plans/P0-04-execution-concurrency-safety-plan.md`
- `P0/resilience`: `todo/p0/production-resilience-and-dr`; `archive/specs/P0-05-production-resilience-and-dr-spec.md`; `archive/plans/P0-05-production-resilience-and-dr-plan.md`
- `P0/data`: `todo/p0/data-security-and-tenant-isolation`; `archive/specs/P0-06-data-security-and-tenant-isolation-spec.md`; `archive/plans/P0-06-data-security-and-tenant-isolation-plan.md`
- `P1/dag`: `todo/p1/dag-engine-and-handler-boundaries`; `archive/specs/P1-01-dag-engine-and-handler-boundaries-spec.md`; `archive/plans/P1-01-dag-engine-and-handler-boundaries-plan.md`
- `P1/api`: `todo/p1/api-contract-and-validation`; `archive/specs/P1-02-api-contract-and-validation-spec.md`; `archive/plans/P1-02-api-contract-and-validation-plan.md`
- `P1/frontend`: `todo/p1/frontend-canvas-state`; `archive/specs/P1-03-frontend-canvas-state-spec.md`; `archive/plans/P1-03-frontend-canvas-state-plan.md`
- `P1/observability`: `todo/p1/observability-and-ops`; `archive/specs/P1-04-observability-and-ops-spec.md`; `archive/plans/P1-04-observability-and-ops-plan.md`
- `P1/release`: `todo/p1/release-deployment-governance`; `archive/specs/P1-05-release-deployment-governance-spec.md`; `archive/plans/P1-05-release-deployment-governance-plan.md`
- `P2/testing`: `todo/p2/testing-foundation`; `archive/specs/P2-01-testing-foundation-spec.md`; `archive/plans/P2-01-testing-foundation-plan.md`
- `P2/capacity`: `todo/p2/cost-capacity-and-retention`; `archive/specs/P2-02-cost-capacity-and-retention-spec.md`; `archive/plans/P2-02-cost-capacity-and-retention-plan.md`
- `P2/docs`: `todo/p2/documentation-adr-and-runbooks`; `archive/specs/P2-03-documentation-adr-and-runbooks-spec.md`; `archive/plans/P2-03-documentation-adr-and-runbooks-plan.md`
- `P2/deps`: `todo/p2/dependency-abstraction-and-vendor-lock-in`; `archive/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md`; `archive/plans/P2-04-dependency-abstraction-and-vendor-lock-in-plan.md`
- `P2/compliance`: `todo/p2/compliance-data-governance`; `archive/specs/P2-05-compliance-data-governance-spec.md`; `archive/plans/P2-05-compliance-data-governance-plan.md`
- `P2/a11y`: `todo/p2/frontend-accessibility-and-quality`; `archive/specs/P2-06-frontend-accessibility-and-quality-spec.md`; `archive/plans/P2-06-frontend-accessibility-and-quality-plan.md`
- `P3/boundary-review`: `archive/specs/P3-00-architecture-boundary-review-spec.md`; `archive/specs/P3-00-architecture-boundary-code-verification.md`; `archive/plans/P3-00-architecture-boundary-review-plan.md`
- `P3/evolution`: `todo/p3/platform-evolution`; `archive/specs/P3-01-platform-evolution-spec.md`; `archive/plans/P3-01-platform-evolution-plan.md`
- `P3/service-split`: `archive/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`; `archive/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md`
- `P3/data-platform`: `archive/specs/P3-03-data-platform-architecture-spec.md`; `archive/plans/P3-03-data-platform-architecture-plan.md`
- `P3/multi-datasource`: `archive/specs/P3-04-multi-datasource-isolation-spec.md`; `archive/plans/P3-04-multi-datasource-isolation-plan.md`
- `P3/webflux-mvc`: `archive/specs/P3-05-webflux-to-mvc-migration-spec.md`; `archive/plans/P3-05-webflux-to-mvc-migration-plan.md`
- `P3/k8s`: `archive/specs/P3-06-k8s-deployment-platform-spec.md`; `archive/plans/P3-06-k8s-deployment-platform-plan.md`
- `P3/platform-components`: `archive/specs/P3-07-production-platform-components-spec.md`; `archive/plans/P3-07-production-platform-components-plan.md`
- `P3/wecom`: `archive/specs/P3-08-wecom-scrm-module-spec.md`; `archive/plans/P3-08-wecom-scrm-module-plan.md`
- `P3/identity-event-tenant`: `archive/specs/P3-09-identity-event-and-tenant-platform-spec.md`; `archive/plans/P3-09-identity-event-and-tenant-platform-plan.md`
- `needs-review`: `todo/needs-review/stale-and-duplicate-findings.md`

## P3 Platform Evolution Gate

P3 platform evolution is blocked until `docs/architecture/platform-evolution-promotion-checklist.md` is complete for the promoted item. The checklist must include owner, success metrics, user value, dependencies, migration plan, rollback plan, operating model, on-call owner, runbook, test plan, data migration, observability, security, compliance, tenant impact, team capacity, verification command, and expected evidence path. Archived evolution documents remain source evidence, not active implementation authority.

## Document Coverage

| Archived document | Coverage |
|---|---|
| `archive/remediation/README.md` | All 35 problem rows plus Top 15 mapped below |
| `archive/remediation/architecture-remediation-plan-2026-05.md` | Index-only; same coverage as remediation parts |
| `archive/remediation/part1-structure.md` | Problems 1-6 mapped below |
| `archive/remediation/part2-security-concurrency.md` | S1-S10, C1-C8, E1-E8, R1-R5 mapped below |
| `archive/remediation/part3-frontend.md` | Problems 11-15 mapped below |
| `archive/remediation/part4-ops.md` | Problems 16-20 mapped below |
| `archive/remediation/part5-engine-deep.md` | Problems 21.1-24.13 mapped below |
| `archive/remediation/part6-logic-testing.md` | Problems 25.1-27.6 mapped below |
| `archive/remediation/part7-resilience.md` | Problems 28.1-28.11 mapped below |
| `archive/reviews/architect-checklist-report.md` | 10 checklist dimensions, Top 8 risks, deep-scan findings mapped below |
| `archive/reviews/architecture-constraints-risks-2026-06-02.md` | Critical/high/medium/low issues #1-#10 mapped below |
| `archive/reviews/architecture-deep-review-2026-05.md` | Major sections mapped below |
| `archive/reviews/architecture-supplement-review-2026-05.md` | 10 supplemental dimensions mapped below |
| `archive/reviews/brownfield-architecture.md` | Enhancement components, risks, and handoff rules mapped below |
| `archive/reviews/production-deployment-checklist-2026-06-02.md` | Deployment checklist sections mapped below |
| `archive/reference/api-spec-summary.md` | API/public endpoint findings mapped below |
| `archive/reference/backend-architecture.md` | Reference architecture; covered as archive_only plus P1/P3 links |
| `archive/reference/coding-standards.md` | Standards reference; covered by P2/docs and relevant package acceptance criteria |
| `archive/reference/database-schema.md` | Schema/security issues mapped to P0/data and P2/compliance |
| `archive/reference/deployment-guide.md` | Deployment reference mapped to P1/release |
| `archive/reference/frontend-architecture.md` | Frontend known issues mapped to P1/frontend and P2/a11y |
| `archive/reference/security-considerations.md` | Gaps mapped to P0/security, P0/data, P2/compliance |
| `archive/reference/tech-stack.md` | Stack constraints mapped to P0/reactive, P2/capacity, P3/evolution |
| `archive/reference/testing-strategy.md` | Testing gaps mapped to P2/testing |
| `archive/evolution/architect-critical-review.md` | 8 critical architecture topics mapped to P0/P1/P2/P3 below |
| `archive/evolution/architecture-evolution-roadmap.md` | Phases 1-4 mapped to P3/boundary-review, P3/evolution, P3/service-split, P3/data-platform, P3/k8s plus P0/P1 prerequisites |
| `archive/evolution/data-platform-architecture.md` | Data platform topics mapped to P3/data-platform |
| `archive/evolution/k8s-deployment-plan.md` | K8s/deploy topics mapped to P1/release and P3/k8s |
| `archive/evolution/multi-datasource-isolation.md` | DB isolation topics mapped to P0/data, P2/capacity, P3/multi-datasource |
| `archive/evolution/production-practice-review.md` | Production component choices mapped to P1/release, P1/observability, P2/deps |
| `archive/evolution/service-architecture-design.md` | Service split topics mapped to P3/boundary-review and P3/service-split |
| `archive/evolution/target-architecture-overview.md` | Target architecture topics mapped to P3/boundary-review, P3/evolution, P3/service-split, P3/multi-datasource, P3/identity-event-tenant |
| `archive/evolution/webflux-to-mvc-migration.md` | Migration topics mapped to P0/reactive and P3/webflux-mvc |
| `archive/evolution/wecom-scrm-module-design.md` | WeCom module topics mapped to P3/wecom |

## Remediation README And Top 15

| Source point | Package | Status |
|---|---|---|
| Problems 1, 4, 5: monolith boundaries, handler flat package, missing service layer | P1/dag | confirmed |
| Problem 2: WebFlux + MyBatis contradiction | P0/reactive | confirmed |
| Problem 3: single DB/pool coupling | P0/data, P2/capacity, P3/evolution | confirmed |
| Problem 6: DO directly exposed | P1/api | confirmed |
| Problem 7: security vulnerabilities | P0/security | mixed |
| Problem 8: concurrency defects | P0/concurrency | confirmed |
| Problem 9: exception handling defects | P0/security, P1/api, P1/observability | confirmed |
| Problem 10: resource leaks | P0/reactive, P0/resilience, P0/concurrency | confirmed |
| Problems 11-15: frontend state/API/routing/typing/perf | P1/frontend, P1/api, P2/a11y | confirmed / partially_confirmed |
| Problems 16-20: production deploy/config/observability/MQ/Flyway | P1/release, P1/observability, P0/security, P2/deps | partially_confirmed |
| Problems 21-24: engine/data/API/frontend deep defects | P1/dag, P0/data, P1/api, P1/frontend | confirmed |
| Problem 25: business correctness | P0/state, P0/data, P2/compliance | confirmed |
| Problem 26: testing gaps | P2/testing | partially_confirmed |
| Problem 27: technical debt/code quality | P1/dag, P1/api, P2/docs, P2/deps | confirmed |
| Problem 28: resilience | P0/resilience, P1/observability, P2/capacity | partially_confirmed |
| Problems 28.7-28.10: reactor, validation, delete, transaction | P0/reactive, P1/api, P0/data, P0/state | confirmed |
| Problem 28.11: frontend state/data flow | P1/frontend | confirmed |
| Top 15 items 1-3 | P0/security | mixed; JWT validation subclaim stale |
| Top 15 items 4-5, 13 | P0/state | confirmed |
| Top 15 items 6, 9, 11, 14 | P0/reactive | confirmed |
| Top 15 item 7 | P0/concurrency | confirmed |
| Top 15 item 8 | P0/reactive, P0/resilience | confirmed |
| Top 15 item 10 | P1/api | confirmed |
| Top 15 item 12 | P0/data | confirmed |
| Top 15 item 15 | P0/data, P2/compliance | confirmed |

## Remediation Detail Coverage

| Source point | Package | Status |
|---|---|---|
| 1. Single monolith without service boundary | P1/dag, P3/evolution | confirmed |
| 2. WebFlux + MyBatis-Plus mismatch | P0/reactive | confirmed |
| 3. Single DB / single Hikari pool coupling | P0/data, P2/capacity, P3/evolution | confirmed |
| 4. Handler flat package | P1/dag | confirmed |
| 5. Service layer nearly absent | P1/dag, P1/api | confirmed |
| 6. DTO/DO direct exposure | P1/api, P0/data | confirmed |
| S1 DB root/root default | P0/security | confirmed |
| S2 weak event secret default | P0/security | confirmed |
| S3 JWT blank/no startup validation | P0/security, needs-review | stale as written; `JwtUtil` validates startup |
| S4/S5 CORS wildcard with credentials | P0/security | confirmed |
| S6 actuator health details | P0/security, P1/release | confirmed |
| S7 Swagger public | P0/security, P1/release | confirmed / environment-dependent |
| S8 direct/behavior endpoints public | P0/security | confirmed |
| S9 `/ops/**` public | P0/security | confirmed |
| S10 500 error leaks exception message | P0/security, P1/api | confirmed |
| C1-C3 circuit breaker TOCTOU/non-atomic transitions | P0/concurrency | confirmed |
| C4 benefit/reach volatile flags | P0/concurrency, P0/state | confirmed |
| C5 `ExecutionContext.putNodeOutput()` compound write | P0/concurrency | confirmed |
| C6 scheduler `closed` plain boolean | P0/concurrency | confirmed |
| C7 `PendingJitterGroup.add()` race | P0/concurrency | partially_confirmed; rollback logic exists but needs tests |
| C8 `NodeGate` public atomics | P0/concurrency, P1/dag | confirmed |
| E1-E8 exception swallowing / weak logging / client leak | P1/observability, P1/api, P0/security | confirmed |
| R1-R5 fire-and-forget subscribe / Disposable tracking / pending tasks | P0/reactive, P0/resilience, P0/concurrency | confirmed |
| 11. Global/frontend state management | P1/frontend | partially_confirmed; contexts exist but editor state lacks boundaries |
| 12. Frontend API layer defects | P1/api, P1/frontend | confirmed |
| 13. Routing/permission guard defects | P1/frontend, P2/a11y | confirmed / needs UI audit |
| 14. TypeScript any | P1/frontend, P1/api | confirmed |
| 15. Frontend performance | P1/frontend, P2/testing | confirmed / needs browser profiling |
| 16. No production-grade deployment | P1/release | partially_confirmed |
| 17. Config management defects | P1/release, P0/security | confirmed |
| 18. Observability missing | P1/observability | confirmed |
| 19. RocketMQ config incomplete | P1/release, P2/deps | partially_confirmed |
| 20. Flyway rollback missing | P1/release | confirmed as governance gap |
| 21.1-21.4 DagEngine god class, executeNode complexity, handler leakage, cycles | P1/dag | confirmed |
| 21.5-21.7 ExecutionContext maps, flatContext overwrite, context size warning-only | P0/concurrency, P0/state, P1/dag | confirmed |
| 21.8-21.11 internal events, Disruptor backpressure, null-check event type, TraceWriteBuffer drops | P1/dag, P0/resilience, P1/observability | confirmed / partially_confirmed |
| 21.12-21.14 exception hierarchy, swallowed exceptions, map config | P1/api, P1/dag, P2/docs | confirmed |
| 21.15 DagParser hard-coded edge fields | P1/dag | confirmed |
| 21.16 Groovy full context exposure | P0/security, P2/compliance | confirmed; `GroovyHandler` still binds `ctx` |
| 21.17-21.19 NodeGate, registry lifecycle, string literals | P0/concurrency, P1/dag, P2/docs | confirmed |
| 22.1-22.5 anemic model, no FKs, tenant nullable, graph_json, shared DB | P0/data, P0/state, P3/evolution | confirmed |
| 22.6-22.7 N+1 / unbounded queries | P2/capacity, P1/api | confirmed on cited paths; broader inventory tracked by P2/capacity |
| 22.8-22.10 Saga/transaction/virtual-thread boundaries | P0/reactive, P0/state | confirmed |
| 22.11 ensureUser race | P0/concurrency, P0/data | confirmed; `ensureUser()` does select-then-insert without duplicate recovery |
| 22.12-22.17 idempotency, cross-context consistency, stats schema, repository pattern, duplicate helpers, partition no-op | P0/state, P1/dag, P2/capacity, P2/docs | confirmed / partially_confirmed |
| 23.1-23.4 API versioning, REST naming, error codes, DO exposure | P1/api | confirmed |
| 23.5 public endpoints | P0/security | confirmed |
| 23.6 inline authorization | P1/api, P0/security | confirmed |
| 23.7 raw WebClient | P0/reactive, P2/deps | confirmed |
| 23.8 anti-corruption layer missing | P2/deps, P1/dag | confirmed as architecture gap |
| 23.9 correlation/tracing | P1/observability | confirmed |
| 23.10 frontend 401 redirect | P1/frontend | confirmed |
| 23.11 ops cache invalidation auth | P0/security | confirmed |
| 23.12 route lock Lua release | P0/concurrency, P0/state | partially_confirmed; publish lock has Lua release, route init needs review |
| 24.1-24.4 editor/config panel god components, duplicated type, stale caches | P1/frontend | confirmed |
| 24.5 outlet mapping triplication | P1/frontend, P2/docs | confirmed |
| 24.6 node type plugin model | P1/frontend, P3/evolution | planning |
| 24.7 service-side state layer | P1/frontend, P1/api | confirmed |
| 24.8 autosave race | P1/frontend, P2/testing | partially_confirmed; stale-closure part mitigated, component regression test still missing |
| 24.9 API interceptor implicit contract | P1/api, P1/frontend | confirmed |
| 24.10-24.11 component tests / jsdom | P2/testing | partially_confirmed; frontend tests exist but component coverage remains gap |
| 24.12 dayjs dependency | P1/frontend, P2/deps | confirmed; `frontend/package.json` lacks explicit `dayjs` dependency |
| 24.13 inline styles | P2/a11y, P1/frontend | confirmed |
| 25.1 state machine violations | P0/state | confirmed |
| 25.2 idempotency gaps | P0/state, P0/concurrency | confirmed |
| 25.3 data integrity/version cleanup | P0/state | confirmed |
| 25.4 business rule enforcement | P0/state, P1/api | confirmed / needs rule-by-rule tests |
| 25.5 audit trail gaps | P2/compliance, P0/state | confirmed |
| 25.6 boundary cases | P0/state, P0/resilience, P2/testing | partially_confirmed |
| 26.1 backend test coverage overview | P2/testing | partially_confirmed; current counts differ but gap remains |
| 26.2 zero integration tests | P2/testing | partially_confirmed; no Testcontainers found |
| 26.3 DagEngine key path tests | P2/testing, P1/dag | confirmed gap |
| 26.4 CircuitBreaker tests | P2/testing, P0/concurrency | confirmed gap |
| 26.5 frontend test gaps | P2/testing, P1/frontend | partially_confirmed |
| 26.6 test quality | P2/testing | partially_confirmed; cited brittle/trivial examples still exist |
| 27.1 duplicated code | P1/dag, P1/api, P2/docs | confirmed |
| 27.2 naming inconsistency | P2/docs | confirmed / low urgency |
| 27.3 magic values | P2/docs, P1/api | confirmed |
| 27.4 dependency management | P2/deps, P1/release | confirmed |
| 27.5 documentation debt | P2/docs | confirmed |
| 27.6 dead code / cleanup markers | P2/docs, P1/dag | confirmed; cited cleanup comments, deprecated API, and wildcard imports still exist |
| 28.1 health checks shallow | P1/observability, P0/resilience | partially_confirmed |
| 28.2 graceful shutdown gaps | P0/resilience | partially_confirmed |
| 28.3 backup/recovery strategy | P0/resilience, P1/release | needs external ops decision |
| 28.4 capacity limits | P2/capacity, P0/resilience | partially_confirmed |
| 28.5 chaos/degradation | P0/resilience, P2/deps | partially_confirmed |
| 28.6 performance bottlenecks | P0/reactive, P2/capacity | confirmed / needs benchmark |
| 28.7 Reactor model violations | P0/reactive | confirmed |
| 28.8 API validation zero coverage | P1/api | confirmed |
| 28.9 hard delete inconsistency | P0/data, P2/compliance | confirmed |
| 28.10 reactive transaction trap | P0/reactive, P0/state | confirmed |
| 28.11 frontend state/data flow | P1/frontend | confirmed |

## Review Document Coverage

| Source point | Package | Status |
|---|---|---|
| Architect checklist dimensions: requirements alignment | P2/docs, needs-review | needs_review; depends on PRD/product scope |
| Architecture fundamentals | P1/dag, P3/evolution | confirmed |
| Technical stack/decision traceability | P0/reactive, P2/docs | confirmed |
| Frontend design | P1/frontend, P2/a11y | confirmed |
| Resilience/operational readiness | P0/resilience, P1/observability, P1/release | partially_confirmed |
| Security/compliance | P0/security, P0/data, P2/compliance | confirmed / partially_confirmed |
| Implementation guidance | P2/docs, P2/testing | partially_confirmed |
| Dependency/integration management | P2/deps, P0/resilience | confirmed |
| AI agent implementation suitability | P1/dag, P1/frontend, P2/docs | confirmed |
| Accessibility | P2/a11y | needs UI audit |
| Architect checklist Top 8 risks | P0/reactive, P1/dag, P0/data, P1/observability, P1/frontend, P0/security | mapped individually in active packages |
| Constraints #1 WebFlux/MyBatis/Disruptor | P0/reactive, P2/capacity | confirmed |
| Constraints #2 handler mapper injection | P1/dag | confirmed |
| Constraints #3 CORS wildcard | P0/security | confirmed |
| Constraints #4 god classes | P1/dag | confirmed |
| Constraints #5 @Lazy cycles | P1/dag | confirmed |
| Constraints #6 distributed tracing | P1/observability | confirmed |
| Constraints #7 frontend editor crash risk | P1/frontend, P2/a11y | confirmed |
| Constraints #8 middleware metrics deployment | P1/observability, P1/release | partially_confirmed |
| Constraints #9 domain indirect engine dependency | P1/dag | confirmed |
| Constraints #10 frontend state gaps | P1/frontend | confirmed |
| Deep review sections 1-4: code evidence, stack, capacity, infra | P0/reactive, P1/release, P2/capacity | mapped |
| Deep review sections 5-8: engine, DB, handler side effects, frontend | P1/dag, P0/state, P1/frontend | mapped |
| Deep review sections 9-12: safety, reactive chain, locks, consistency | P0/security, P0/reactive, P0/concurrency, P0/state | mapped |
| Deep review sections 13-15: API/data/security/observability | P1/api, P0/data, P0/security, P1/observability | mapped |
| Supplement review 1 cost architecture | P2/capacity | partially_confirmed |
| Supplement review 2 DR/BCP | P0/resilience | partially_confirmed |
| Supplement review 3 evolution feasibility | P3/evolution, needs-review | covered by P3-01 promotion gate; external roadmap decision remains |
| Supplement review 4 team/org fit | P1/dag, P2/docs | needs_review for team facts |
| Supplement review 5 vendor lock-in | P2/deps | confirmed risk |
| Supplement review 6 knowledge management | P2/docs | partially_confirmed |
| Supplement review 7 testability | P2/testing | partially_confirmed |
| Supplement review 8 performance/capacity | P2/capacity | partially_confirmed |
| Supplement review 9 AI adaptation | P1/dag, P1/frontend, P2/docs | confirmed |
| Supplement review 10 compliance readiness | P2/compliance | partially_confirmed |
| Brownfield data models: ExecutionSpan, DataSourceCredential | P1/observability, P0/data | planning |
| Brownfield components: NodeHandlerRepository, DagEngineDecomposer, CanvasEditorStore, ErrorBoundaryShell | P1/dag, P1/frontend, P2/a11y | planning / confirmed need |
| Brownfield API/source tree/testing/security/deployment sections | P1/api, P1/release, P2/testing, P0/security | mapped |
| Production deployment checklist critical/high/medium/low | P1/release, P0/security, P1/observability, P2/capacity | partially_confirmed |
| Production rollback/postdeploy/KPI/continuous optimization | P1/release, P1/observability, P2/capacity | planning |

## Reference Document Coverage

| Source point | Package | Status |
|---|---|---|
| API summary public endpoints | P0/security, P1/api | confirmed |
| API summary API domains | P1/api | archive_only plus contract input |
| Backend architecture | P1/dag, P0/reactive, P3/evolution | archive_only plus active references |
| Frontend architecture known issues | P1/frontend, P2/a11y, P2/testing | confirmed |
| Database schema critical password issue | P0/data, P0/security | confirmed |
| Tech stack version constraints | P0/reactive, P2/capacity, P3/evolution | archive_only plus active references |
| Coding standards | P2/docs | archive_only plus quality input |
| Security considerations GAP-1 password | P0/data | confirmed |
| Security considerations GAP-2 public endpoints | P0/security | confirmed |
| Security considerations GAP-3 CORS | P0/security | confirmed |
| Security considerations GAP-4/5 static data and infra passwords | P0/security, P2/compliance | partially_confirmed |
| Security considerations GAP-6 tracing | P1/observability | confirmed |
| Security considerations GAP-7/8/9 validation/XSS/partition | P1/api, P2/a11y, P2/capacity | mapped |
| Testing strategy backend/frontend/integration/security gaps | P2/testing | partially_confirmed |
| Deployment guide local/docker/prod considerations | P1/release | archive_only plus release input |

## Evolution Document Coverage

| Source point | Package | Status |
|---|---|---|
| Multi datasource isolation: current problem, 3 DBs, routing, Flyway, cross-DB transactions, migration, monitoring, tenant visibility | P0/data, P2/capacity, P3/multi-datasource | covered by P3-04 decision package; physical split blocked by P0 data and reconciliation gates |
| Target architecture overview: current problems, bounded contexts, modules, data source isolation, API gateway, observability, roadmap | P3/evolution, P3/service-split, P3/multi-datasource, P1/observability, P0/data | covered by P3-00, P3-01, P3-02, P3-04, and P3-09 decision packages |
| WebFlux to MVC migration: problem, migration strategy, steps, risk, expected perf | P0/reactive, P3/webflux-mvc | covered by P3-05 runtime decision package; MVC migration deferred and current mismatch tracked by P0/reactive |
| Production practice review: XXL-JOB, production stack, Redisson, Nacos, Knife4j, logs, Feign/Sentinel, Spring Boot Admin, ClickHouse | P2/deps, P1/release, P1/observability, P3/platform-components | covered by P3-07 decision package; only Redisson proof behind local interface accepted |
| Data platform architecture: need, warehouse layers, CDC/Flink, OLAP, governance, API layer, integration, K8s, roadmap, risk | P3/data-platform, P2/compliance | covered by P3-03 thin-slice data-platform package; full rollout deferred |
| Service architecture design: 12 services, communication, event bus, deployment, priority, module layout, build/start order | P3/service-split | covered by P3-00 and P3-02; immediate physical service split rejected in favor of modular-monolith boundaries |
| K8s deployment plan: Helm, values, Deployment, HPA, Redis HA, MySQL, RocketMQ, CI/CD, observability, security, steps | P1/release, P1/observability, P0/resilience, P3/k8s | covered by P3-06 Helm and operating-model package; production rollout needs environment owner evidence |
| Architecture evolution roadmap: phases, baseline, tasks, dependencies, resources, risk, success criteria | P3/boundary-review, P3/evolution, P3/service-split, P3/data-platform, P3/k8s, needs-review | covered by P3-00 through P3-09 with promotion gates; roadmap timing remains a product/team decision |
| Architect critical review: tenant isolation, OneID, event schema, engine/web split, tracing, tenant quota, service degradation, strangler migration | P0/data, P1/observability, P2/deps, P3/boundary-review, P3/identity-event-tenant, P3/service-split | covered by P3-09 primitives plus P0/P1 prerequisites; implementation remains gated |
| WeCom SCRM module design: module scope, data model, API, handlers, WeCom client, callbacks, frontend, plan, risks | P3/wecom | covered by P3-08 integration-boundary and first-slice package; implementation deferred |

## Unresolved Or Decision-Dependent

| Point | Reason |
|---|---|
| Exact production RTO/RPO, Redis HA, cloud topology, cost model | Requires production/environment facts outside repo |
| Formal compliance certification readiness | Requires organizational/process evidence outside repo |
| WebFlux-to-MVC versus hardening current WebFlux model | Architecture decision needed |
| Service split, data platform, WeCom, OneID timing | Product/team roadmap decision needed; tracked in P3/service-split, P3/data-platform, P3/wecom, and P3/identity-event-tenant |
| Accessibility pass/fail details | Requires browser/user-flow audit beyond repository grep |
| Some old exact line-number claims | Code has changed; active package records current evidence where rechecked |
