# P3-06 K8s Platform Evidence

Date: 2026-06-05

## Verdict

P3-06 now has an operating model, application Helm chart, and rollout/rollback runbook. Kubernetes is treated as a platform deployment layer only; it does not approve service decomposition, datasource splitting, or self-operated stateful dependencies.

## Created Documents And Assets

- `docs/architecture/k8s-operating-model.md`
- `deploy/helm/canvas/Chart.yaml`
- `deploy/helm/canvas/values.yaml`
- `deploy/helm/canvas/values-staging.yaml`
- `deploy/helm/canvas/values-prod.yaml`
- `deploy/helm/canvas/templates/deployment.yaml`
- `deploy/helm/canvas/templates/service.yaml`
- `deploy/helm/canvas/templates/ingress.yaml`
- `deploy/helm/canvas/templates/configmap.yaml`
- `deploy/helm/canvas/templates/secret-ref.yaml`
- `deploy/helm/canvas/templates/hpa.yaml`
- `docs/architecture/runbooks/k8s-rollout-rollback.md`

## Existing Assets Reviewed

- `deploy/k8s/canvas-engine-deployment.yaml`
- `deploy/k8s/canvas-engine-service.yaml`
- `deploy/k8s/canvas-engine-hpa.yaml`
- `deploy/k8s/canvas-engine-network-policy.yaml`
- `backend/canvas-engine/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `backend/canvas-engine/src/main/resources/application-prod.yml`

## Operating Decision

- MySQL: managed service by default.
- Redis: managed service by default.
- RocketMQ: managed service by default.
- Ingress, monitoring, secrets, image registry, and cluster upgrades have named owner roles in the operating model.
- Application chart deploys backend and frontend workloads and references pre-created secrets.
- Stateful dependencies, data platform components, and physical service boundaries are out of scope.

## Verification Commands

```bash
test -f docs/architecture/k8s-operating-model.md
rg -n "MySQL|Redis|RocketMQ|Ingress|Monitoring|Secrets|SLO|staging|production|managed service|self-operated" docs/architecture/k8s-operating-model.md
test -f deploy/helm/canvas/Chart.yaml
helm template canvas deploy/helm/canvas -f deploy/helm/canvas/values-staging.yaml >/tmp/canvas-staging.yaml
rg -n "readinessProbe|livenessProbe|resources|secretKeyRef|actuator|Service|Ingress" /tmp/canvas-staging.yaml
test -f docs/architecture/runbooks/k8s-rollout-rollback.md
rg -n "Smoke|Health|Metrics|Login|Rollback|Migration|Image|Config|Evidence" docs/architecture/runbooks/k8s-rollout-rollback.md
```

Result: documentation checks passed. `helm` was not installed in the base PATH, so a temporary Helm v3.15.4 binary was downloaded under `/tmp/canvas-helm-bin/helm` and used to render `/tmp/canvas-staging.yaml`; the render check passed.

Additional check:

```bash
/tmp/canvas-helm-bin/helm lint deploy/helm/canvas -f deploy/helm/canvas/values-staging.yaml
```

Result: 1 chart linted, 0 failed. Helm reported only an informational missing chart icon recommendation.

## Follow-ups

- Add Helm installation to CI or developer setup before making chart rendering a required merge gate.
- Capture rendered staging and production YAML under `docs/architecture/evidence/k8s/rendered/` during a real rollout or rollback drill.
- Add service account, network policy, PDB, and ServiceMonitor templates only after cluster conventions are confirmed.
- Do not add self-operated MySQL, Redis, RocketMQ, Kafka, Flink, ClickHouse, MinIO, or DataHub charts without an approved operating ADR.

No P3 files were staged or committed by this task.
