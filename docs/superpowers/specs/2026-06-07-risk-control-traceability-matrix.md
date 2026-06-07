# Risk Control Rule Engine Traceability Matrix

## 1. Purpose

This document tracks the enterprise risk-control rule engine objective from external references and product requirements through design artifacts, implementation tasks, production operations, and verification evidence.

It prevents a common failure mode: treating a design document as a finished production product. A requirement is complete only when implementation and verification evidence exist.

Related artifacts:

- Product design: `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md`
- Reference matrix: `docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md`
- Production contracts: `docs/superpowers/specs/2026-06-07-risk-control-contracts.md`
- Implementation plan: `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md`
- Production runbook: `docs/runbooks/risk-control-rule-engine.md`

## 2. Status Definitions

| Status | Meaning |
| --- | --- |
| `DESIGNED` | Requirement is covered by design, contract, plan, or runbook |
| `PLANNED` | Implementation task exists but code is not present or not verified |
| `IMPLEMENTED` | Code exists in current worktree |
| `VERIFIED` | Required tests, build, runtime checks, or operational evidence passed |
| `BLOCKED` | Work cannot proceed without explicit user input or external state change |

Current overall status:

```text
DESIGNED + PLANNED. Not IMPLEMENTED or VERIFIED as a production product.
```

The current worktree is a normal `main` checkout with unrelated modified and untracked files. Code implementation must start in an isolated worktree or with explicit authorization to modify `main`.

## 3. External Reference Coverage

| Requirement Source | Required Product Learning | Design Evidence | Implementation Evidence | Status |
| --- | --- | --- | --- | --- |
| Antom Shield | Rule configuration, lists, risk score, simulation, analysis, supported attributes | Design sections 2, 6, 7; reference matrix section 3 | No code yet | `DESIGNED` |
| Old Alipay risk score docs | External score query shape and labels | Design section 6.6; contracts response fields | No adapter code yet | `DESIGNED` |
| Meituan Zeus | Scene/rule-group/rule/factor layering; mark, dual-run, replay; anti-misoperation controls | Design section 2.1; contracts runtime modes; runbook rollout | No runtime code yet | `DESIGNED` |
| Meituan Zeus ZGC/Aviator | Expression engines need compile-cache and class-loading governance | Design section 7.3; contracts metrics; runbook CodeCache incident | No expression cache code yet | `DESIGNED` |
| Youzan rule engine | Realtime features, rule center, offline tasks, ops platform, 100ms target | Design sections 6, 7, 14; plan phases 8 and 10 | No feature platform code yet | `DESIGNED` |
| Drools/DMN/OpenL/Camunda | Decision tables and hit policy semantics | Design section 6.8; contracts section 6 | No decision-table code yet | `DESIGNED` |
| AviatorScript | Governed JVM expression sub-layer | Contracts sections 4 and 9; runbook section Safe Expression | No safe expression code yet | `DESIGNED` |
| Flink | Stateful realtime aggregate features and async external I/O | Reference matrix and plan phase 8 | No risk Flink job code yet | `DESIGNED` |
| OpenTelemetry | Traces, metrics, logs | Contracts section 9; runbook dashboards and alerts | No risk metrics code yet | `DESIGNED` |

## 4. Product Success Criteria Trace

| ID | Success Criterion | Design Evidence | Contract Evidence | Plan Evidence | Runbook Evidence | Required Verification | Current Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RISK-SC-01 | Support at least five risk scenes | Design section 5 lists marketing benefit, touch, account/device, transaction/payment, content, AI guardrail | Scene key in contracts API and snapshot | Phase 1 scene metadata; Phase 6 Studio | Dashboard and incident sections use scene labels | Tests seed or create five active scenes; API routes each scene | `PLANNED` |
| RISK-SC-02 | Support rules, rule groups, lists, decision tables, and scoring | Design sections 6.2-6.8 | DSL, strategy snapshot, decision table, list DDL | Phases 1-3 and 7 | Rollout validates rules, lists, tables | Parser, validator, evaluator, matcher, merger tests pass | `PLANNED` |
| RISK-SC-03 | Provide online decision API | Design section 10.1 | Contracts section 3 | Phase 4 Task 4.1 | Decision API latency playbook | Controller and service tests; idempotency tests | `PLANNED` |
| RISK-SC-04 | Provide Canvas `RISK_DECISION` node | Design section 11.1 | Contracts section 11 | Phase 5 | False-positive remediation references Canvas actions | Handler tests route all actions and fail policy | `PLANNED` |
| RISK-SC-05 | Support versions, approval, activation, rollback | Design section 12 | Runtime modes and audit events | Phase 4 Task 4.2; Phase 7 Task 7.2 | Emergency pause and rollback sections | Strategy lifecycle tests; rollback API tests; audit tests | `PLANNED` |
| RISK-SC-06 | Record hit logs, decision logs, and audit logs | Design sections 9.2, 13.4, 15 | DDL, MQ, metrics, audit contracts | Phases 1, 3, 10 | Audit write failure and export sections | Decision run, rule hit, trace, audit persistence tests | `PLANNED` |
| RISK-SC-07 | Support simulation, mark, dual-run, canary | Design section 6.9 | Runtime modes in contracts section 2.3 | Phase 7 | Normal rollout sequence | Simulation tests; shadow/dual-run/canary tests | `PLANNED` |
| RISK-SC-08 | P95 online decision below 50ms excluding remote model | Design section 14 | Metrics and deadline contracts | Phase 10 metrics; verification gate | Latency incident playbook | Performance test with 100 rules, 20 groups, cache warm | `PLANNED` |
| RISK-SC-09 | Enforce tenant isolation, PII masking, permission boundaries | Design section 13 | API tenant rule, DDL tenant_id, audit contract | Phase 4 API tests; Phase 3 list matcher | PII export rules and startup configuration | Security tests, tenant override tests, PII masking tests | `PLANNED` |
| RISK-SC-10 | Provide production dashboards and alerts | Design section 15 | Metrics and trace contracts | Phase 10 Task 10.1 | Dashboard and alert sections | Metrics tests and alert rule verification | `PLANNED` |
| RISK-SC-11 | Provide rollback and incident runbook | Design section 20 | Readiness gates include runbook commands | Phase 10 Task 10.2 marked complete for docs | Runbook exists | Placeholder scan and runbook link verification | `DESIGNED` |

## 5. Contract Readiness Trace

| Contract Gate | Required Implementation | Plan Task | Verification Command | Current Status |
| --- | --- | --- | --- | --- |
| API tests assert tenant context overrides request body tenant | `RiskDecisionController` ignores body tenant and uses authenticated context | Task 4.1 | `mvn test -pl canvas-engine -Dtest=RiskDecisionControllerTest` | `PLANNED` |
| DSL parser rejects unsafe operands and unknown operators | `RiskRuleParser` accepts only structured operands | Task 2.1 | `mvn test -pl canvas-engine -Dtest=RiskRuleParserTest` | `PLANNED` |
| DSL validator rejects unknown/offline factors and list mismatches | `RiskRuleValidator` uses factor and list catalogs | Task 2.2 | `mvn test -pl canvas-engine -Dtest=RiskRuleValidatorTest` | `PLANNED` |
| Strategy compiler computes stable hash and records required features | `RiskStrategyCompiler` produces immutable `CompiledRiskStrategy` | Task 3.1 | `mvn test -pl canvas-engine -Dtest=RiskStrategyCompilerTest` | `PLANNED` |
| Decision service records decision runs and rule hits | `RiskDecisionService` persists run and hit rows | Task 3.4 | `mvn test -pl canvas-engine -Dtest=RiskDecisionServiceTest` | `PLANNED` |
| Idempotency returns stable decisions for repeated requests | Decision service uses `tenantId + requestId` and request hash | Task 3.4 and Task 4.1 | `mvn test -pl canvas-engine -Dtest=RiskDecisionServiceTest,RiskDecisionControllerTest` | `PLANNED` |
| List matcher hashes subjects before lookup and logging | `RiskListMatcher` hashes PII and returns masked evidence | Task 3.2 | `mvn test -pl canvas-engine -Dtest=RiskListMatcherTest` | `PLANNED` |
| Metrics exist with contract names | `RiskDecisionService`, list import, strategy activation, simulation emit metrics | Task 10.1 | `mvn test -pl canvas-engine -Dtest=RiskMetricsTest` | `PLANNED` |
| Audit events exist for governance actions | Strategy/list/model services write `risk_audit_event` | Phase 4 and Phase 10 | Governance service tests and audit export tests | `PLANNED` |
| Runbook verification commands pass | Backend and frontend tests pass | Phase 11 | Commands in runbook section Verification Commands | `PLANNED` |

## 6. Implementation Phase Trace

| Phase | Product Capability | Required Evidence | Current Status |
| --- | --- | --- | --- |
| Phase 1 Foundation | Metadata schema, DOs, mappers | `V351__risk_control_rule_engine_foundation.sql`, schema test, mapping test | `PLANNED` |
| Phase 2 DSL | Parser, validator, evaluator | Parser, validator, evaluator tests | `PLANNED` |
| Phase 3 Runtime | Compiler, cache, lists, merger, decision service | Runtime unit tests and persistence tests | `PLANNED` |
| Phase 4 Governance API | Decision, strategy, list controllers | Controller tests and RBAC tests | `PLANNED` |
| Phase 5 Canvas Integration | `RISK_DECISION` node and config schema | Handler tests and frontend schema tests | `PLANNED` |
| Phase 6 Strategy Studio | Risk workbench UI and API client | Vitest state/API/page tests and screenshot after implementation | `PLANNED` |
| Phase 7 Risk Lab | Simulation, mark, shadow, dual-run, canary | Simulation and mode tests | `PLANNED` |
| Phase 8 Feature Platform | Redis feature store and Flink realtime aggregate job | Feature store tests and Flink job tests | `PLANNED` |
| Phase 9 Intelligence | Model gateway and graph foundation | Model gateway tests and graph analysis tests | `PLANNED` |
| Phase 10 Observability | Metrics, alerts, production runbook | Metrics tests, alert rules, runbook | `PLANNED` |
| Phase 11 Verification | Full backend/frontend/build/readiness audit | Targeted tests, build checks, readiness audit | `PLANNED` |

## 7. Current Artifact Evidence

| Artifact | Evidence | Current Status |
| --- | --- | --- |
| Product and architecture design | `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md` | `DESIGNED` |
| Source evidence matrix | `docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md` | `DESIGNED` |
| Production contracts | `docs/superpowers/specs/2026-06-07-risk-control-contracts.md` | `DESIGNED` |
| Implementation plan | `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md` | `PLANNED` |
| Production runbook | `docs/runbooks/risk-control-rule-engine.md` | `DESIGNED` |
| Specs index entry | `docs/superpowers/specs/INDEX.md` | `DESIGNED` |
| Code implementation | No risk-control production code has been implemented in the current risk task | `PLANNED` |
| Automated verification | No backend/frontend risk implementation tests have been run because implementation has not started | `PLANNED` |

## 8. Entry Criteria For Code Implementation

Code implementation may start only when all of these are true:

1. Work happens in an isolated worktree, or the user explicitly authorizes direct changes in the current `main` checkout.
2. Current migration directory is rechecked and the next available migration version is confirmed.
3. Existing unrelated dirty files are recorded and not touched.
4. TDD is followed for each new behavior.
5. The implementation plan is updated if file paths, migration numbers, framework conventions, or test commands differ from current project reality.

## 9. Completion Criteria For The Goal

The overall user objective is complete only when all of these are true:

1. Documentation artifacts remain internally consistent and free of unresolved markers.
2. Risk-control backend schema, domain model, DSL, runtime, APIs, governance, lab, feature platform, model gateway, graph foundation, metrics, and runbook are implemented.
3. Canvas `RISK_DECISION` node works and routes by action.
4. Strategy Studio UI works for scenes, strategies, rules, lists, simulations, traces, and approvals.
5. All plan verification commands pass.
6. Production readiness audit proves SLO, tenant isolation, PII masking, fail policy, rollback, audit, and observability.
7. No unrelated user changes are overwritten or reverted.

Current state does not satisfy completion criteria because production code implementation and verification remain outstanding.
