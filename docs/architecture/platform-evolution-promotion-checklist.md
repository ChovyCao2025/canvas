# Platform Evolution Promotion Checklist

Date: 2026-06-05

Status: P3-01 platform-evolution entry point. This checklist blocks implementation until a focused P3 item is promoted with evidence.

## Global Promotion Gate

Every promoted P3 item must fill all rows before implementation starts.

| Gate | Required content | Status |
|---|---|---|
| owner | Named accountable owner and backup reviewer. | Required |
| success metrics | User-facing and operational success metrics with baseline and target. | Required |
| user value | Product or operator value, affected workflow, and out-of-scope items. | Required |
| dependencies | Upstream systems, downstream consumers, teams, vendors, secrets, and feature flags. | Required |
| migration plan | Old path, new path, compatibility window, rollout sequence, and data movement if any. | Required |
| rollback plan | Rollback trigger, command or sequence, data reconciliation, and decision owner. | Required |
| operating model | Runtime owner, SLO, alert routing, incident class, and maintenance window. | Required |
| on-call owner | Named on-call owner and escalation path. | Required |
| runbook | Runbook path with diagnostic and remediation commands. | Required |
| test plan | Unit, contract, integration, frontend, migration, smoke, and rollback tests. | Required |
| data migration | Table/read-model ownership, retention, PII class, deletion behavior, and backup owner. | Required |
| observability | Metrics, logs, traces, dashboards, alerts, and evidence capture path. | Required |
| security | Auth, authorization, secret handling, public routes, and abuse controls. | Required |
| compliance | PII, consent, audit, data deletion, retention, and evidence retention. | Required |
| tenant impact | Tenant propagation, admin/system context, row scope, cross-tenant tests, and quota impact. | Required |
| team capacity | Implementation owner, reviewer capacity, operations capacity, and support handoff. | Required |
| verification command | Command that proves readiness and expected evidence path. | Required |
| ADR | Required for any physical service, runtime, datasource, Kubernetes, or platform-component decision. | Required when applicable |

## P0/P1 Prerequisites

P3 implementation is blocked unless each prerequisite is closed or has explicit risk acceptance with owner and expiration date.

| Prerequisite | Required proof | Risk acceptance rule |
|---|---|---|
| `P0-01` security hardening | `SecurityConfigRouteTest` and linked P0-01 evidence. | Owner and expiration date required if any public-route or auth gap remains. |
| `P0-03` canvas state data consistency | `CanvasTransactionAnnotationTest` and linked P0-03 evidence. | Owner and expiration date required if state transitions or transactions remain unsafe. |
| `P0-06` data security and tenant isolation | `TenantServiceTest` and linked P0-06 evidence. | Owner and expiration date required if tenant scope is incomplete. |
| `P1-04` observability and ops | P1-04 evidence with dashboard, alert, trace, and runbook links. | Owner and expiration date required if operating proof is missing. |
| `P1-05` release deployment governance | P1-05 evidence with deployment, rollback, and smoke commands. | Owner and expiration date required if rollback proof is missing. |

Focused prerequisite command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=SecurityConfigRouteTest,CanvasTransactionAnnotationTest,TenantServiceTest
```

## Focused P3 Promotion Rows

| Item | Scope | evidence file | Approval owner | First executable plan task | ADR requirement |
|---|---|---|---|---|---|
| `P3-02` service decomposition and domain boundaries | Modular-monolith boundary map, contract inventory, and extraction gate. | `docs/architecture/evidence/p3-02-service-decomposition.md` | Architecture owner plus backend/data/ops reviewers | Build ports/read models before moving packages. | Required for any physical service extraction. |
| `P3-03` data platform architecture | Thin analytics/warehouse slice before full data platform. | `docs/architecture/evidence/p3-03-data-platform.md` | Data owner plus backend/ops/compliance reviewers | Pick one vertical slice and prove source contracts. | Required for warehouse/lake/streaming component choices. |
| `P3-04` multi-datasource isolation | Table ownership, datasource groups, transaction split, migration governance. | `docs/architecture/evidence/p3-04-multi-datasource-isolation.md` | Backend data owner plus DBA/ops reviewers | Build table ownership inventory and cross-group write map. | Required before adding a new runtime datasource. |
| `P3-05` WebFlux to MVC migration | Runtime model decision and low-risk endpoint slice. | `docs/architecture/evidence/p3-05-webflux-mvc.md` | Backend runtime owner plus release owner | Inventory reactive/blocking hazards and decide defer/migrate/hybrid. | Required for runtime stack migration. |
| `P3-06` Kubernetes deployment platform | Deployment assets, ownership, SLO, smoke, rollback evidence. | `docs/architecture/evidence/p3-06-k8s-platform.md` | Operations owner plus backend/frontend owners | Define managed vs self-operated dependencies and render manifests. | Required for cluster topology and ingress/runtime decisions. |
| `P3-07` production platform components | Evaluate gateway, config, registry, workflow, tracing, and other platform components. | `docs/architecture/evidence/p3-07-platform-components.md` | Architecture owner plus ops/security reviewers | Select one smallest proof component or defer. | Required for every accepted component proof. |
| `P3-08` WeCom SCRM module | Integration context slice for callback, sync, journey node, and frontend config. | `docs/architecture/evidence/p3-08-wecom-scrm.md` | Integration owner plus compliance/security reviewers | Pick first WeCom slice or defer. | Required if WeCom becomes a separate deployable or provider boundary. |
| `P3-09` identity, event, and tenant platform | Shared OneID, event envelope, tenant contracts, and platform primitive boundaries. | `docs/architecture/evidence/p3-09-platform-primitives.md` | Platform owner plus backend/data/security reviewers | Define canonical identity, event envelope, and tenant contracts. | Required before any service consumes platform primitives. |

## Promotion Template

Copy this table into the focused P3 evidence file before implementation:

| Field | Value |
|---|---|
| Promoted item | |
| Owner | |
| Approval reviewers | |
| Source archive documents | |
| Focused spec | |
| Focused plan | |
| ADR path | |
| Success metrics | |
| User value | |
| Dependencies | |
| Migration plan | |
| Rollback plan | |
| Operating model | |
| On-call owner | |
| Runbook | |
| Test plan | |
| Data migration | |
| Observability | |
| Security | |
| Compliance | |
| Tenant impact | |
| Team capacity | |
| Verification command | |
| Expected evidence path | |
| Risk acceptance owner and expiration date | |

## Stop Rule

If any required row is blank, the P3 item is not implementation authority. Implementation may resume only when the checklist is complete or risk acceptance has an owner, expiration date, and rollback path.
