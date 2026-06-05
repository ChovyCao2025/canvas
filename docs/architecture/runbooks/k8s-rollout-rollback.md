# K8s Rollout Rollback Runbook

Date: 2026-06-05

Status: P3-06 runbook. Run in staging before production.

## Inputs

- Helm release name.
- Namespace.
- Backend image tag.
- Frontend image tag.
- Rendered YAML path.
- Migration status and Flyway output.
- Owner approval for application, platform, database, security, and operations.

## Smoke Checks

| Check | Command shape | Expected result |
|---|---|---|
| Health | `curl -fsS https://<host>/actuator/health` or internal service port-forward | Health endpoint returns healthy status for backend. |
| Metrics | `curl -fsS https://<host>/actuator/prometheus` or scrape target check | Metrics endpoint is reachable and application tag is present. |
| Frontend load | `curl -fsS https://<host>/` | HTML shell loads and static assets return 200. |
| Login | Browser or API test against `/auth/login` with staging-only credentials | Login succeeds or returns the documented validation error for invalid staging credentials. |
| Safe API | `GET /canvas/list` or another read-only tenant-scoped endpoint | Request returns the documented `R` wrapper and does not mutate state. |

## Rollout Steps

1. Render YAML and save it under `docs/architecture/evidence/k8s/rendered/<env>/<release>-<image-tag>.yaml`.
2. Confirm target images exist and vulnerability scan gate passed.
3. Confirm required Secret keys exist in the namespace.
4. Confirm MySQL, Redis, RocketMQ, ingress, and monitoring owners are available.
5. Run migration gate and capture output before application rollout.
6. Upgrade with Helm using the environment values file.
7. Wait for Deployment rollout and HPA target availability.
8. Run Smoke checks.
9. Capture deployed image tag, pod list, rollout status, smoke output, and dashboard screenshots or links.

## Rollback Triggers

- Image crash loop or startup probe failure.
- Config error such as missing Secret, invalid Redis/RocketMQ host, or CORS mismatch.
- Migration failure or unexpected Flyway drift.
- Ingress 5xx, TLS, route, or body-size regression.
- Dependency failure for MySQL, Redis, RocketMQ, or frontend static hosting.
- Error rate, latency, DB pool saturation, Redis latency, RocketMQ lag, or pod restart alerts breach the release threshold.

## Rollback Actions

| Failure type | Rollback action |
|---|---|
| Image | `helm rollback <release> <revision> -n <namespace>` or redeploy the last known-good image tag. |
| Config | Restore previous ConfigMap/values and run Helm rollback. Do not rotate secrets during incident response unless a secret is the root cause. |
| Migration | Stop rollout. Follow `docs/architecture/runbooks/flyway-backup-rollback.md`; prefer forward repair migration when production data changed. |
| Ingress | Restore previous ingress values or controller rule; verify frontend load and API proxy behavior. |
| Dependency | Fail back to managed service endpoint or previous connection values; involve dependency owner before retrying rollout. |

## Evidence

Capture these files or links:

- Rendered YAML: `docs/architecture/evidence/k8s/rendered/<env>/<release>-<image-tag>.yaml`.
- Deployed image tag: `docs/architecture/evidence/k8s/images/<env>/<release>-<timestamp>.txt`.
- Smoke output: `docs/architecture/evidence/k8s/smoke/<env>/<release>-<timestamp>.log`.
- Rollback drill: `docs/architecture/evidence/k8s/rollback/<env>/<release>-<timestamp>.md`.
- Migration output: `docs/architecture/evidence/k8s/migrations/<env>/<release>-<timestamp>.log`.

## Exit Criteria

- Health, Metrics, Frontend load, Login, and Safe API smoke checks pass.
- No rollback trigger fires during the observation window.
- Operations owner confirms dashboards and alerts are active.
- Database owner confirms migration state.
- Security owner confirms secret references and ingress TLS.
- Release owner records the evidence paths in the P3-06 evidence file.
