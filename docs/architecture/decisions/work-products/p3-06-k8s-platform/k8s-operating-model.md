# K8s Operating Model

Date: 2026-06-05

Status: P3-06 planning artifact. Kubernetes topology is a platform deployment decision and does not define domain or service boundaries.

## Ownership Matrix

| Capability | Owner | Default model | Responsibility |
|---|---|---|---|
| MySQL | Data platform owner with DBA support | managed service | HA, backup, restore drill, parameter changes, schema migration window, PITR evidence. |
| Redis | Platform owner | managed service | HA, password rotation, eviction policy, memory alerts, failover drill, keyspace isolation. |
| RocketMQ | Platform owner | managed service | NameServer/Broker HA, topic creation, retention, DLQ policy, consumer lag alerts, failover drill. |
| Ingress | Platform owner | self-operated ingress controller or cloud ingress | TLS termination, WAF/rate-limit policy, route ownership, certificate rotation. |
| Monitoring | Operations owner | self-operated Prometheus/Grafana or managed observability | Metrics, logs, traces, dashboards, alerts, on-call routing, retention. |
| Secrets | Security owner | managed secret store preferred | Secret creation, rotation, access review, audit trail, break-glass process. |
| Image registry | Platform owner | managed registry | Image retention, vulnerability scan gate, provenance, promotion tags, rollback tags. |
| Cluster upgrades | Platform owner | self-operated cluster lifecycle or managed Kubernetes | Upgrade plan, node image patching, compatibility testing, workload disruption budget. |

## Environment Model

| Environment | SLO | Replicas | Resource target | Dependency model | Notes |
|---|---|---:|---|---|---|
| dev | Best effort during working hours | 1 backend, 1 frontend | Small requests and no strict limits for local iteration | Local Docker or shared managed dev dependencies | Used for functional validation only. |
| staging | 99.5 percent monthly availability target | 2 backend, 2 frontend | Backend requests 500m CPU and 1Gi memory; frontend requests 100m CPU and 128Mi memory | Managed service dependencies that mirror production class | Required for migration, smoke, and rollback drills. |
| production | 99.9 percent monthly availability target after P1/P2 gates | 3+ backend, 2+ frontend | Backend requests 1 CPU and 2Gi memory; frontend requests 200m CPU and 256Mi memory | managed service for MySQL, Redis, and RocketMQ unless an approved self-operated runbook exists | Requires owner signoff, SLO alerts, and rollback evidence. |

## Managed Service Policy

MySQL, Redis, and RocketMQ are treated as managed service dependencies by default. Self-operated instances are blocked unless the owner supplies:

- HA topology;
- backup and restore procedure;
- failover drill result;
- patching owner;
- retention and capacity plan;
- alert rules and on-call routing;
- rollback plan for configuration changes.

## Self-operated Components

The application chart may self-operate:

- backend `canvas-engine` Deployment and Service;
- frontend Deployment and Service;
- Ingress rules when the cluster ingress controller already exists;
- ConfigMap for non-secret runtime configuration;
- Secret references to pre-created secrets;
- HPA settings that target application Deployments.

The application chart must not self-operate stateful dependencies, data platform components, or domain-specific services without a new ADR.

## SLO And Resource Gates

Production rollout requires:

- readiness and liveness probes on actuator or HTTP health paths;
- Prometheus metrics exposed and scraped;
- request latency, error rate, pod restart, DB pool, Redis, RocketMQ, and ingress alerts;
- CPU and memory requests for every container;
- max unavailable set to zero for backend rolling updates;
- HPA-compatible labels and scale target names;
- namespace, service account, and image pull secret ownership;
- rollback tested in staging before production.

## Boundary Rule

Kubernetes namespaces, Deployments, Services, and Ingress paths must not be treated as service decomposition evidence. Domain boundaries still follow P3-00, ADR-0006, and the focused P3 domain plans.
