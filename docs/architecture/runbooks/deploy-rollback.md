# Deploy Rollback Runbook

## symptom

Use when a release causes health check failure, migration failure, execution failure spike, route/cache regression, or API p95/SLO burn after deployment.

## severity

- P0: production unavailable or data correctness risk after deploy.
- P1: core runtime degraded after deploy.
- P2: non-critical feature regression with workaround.

## dashboard link placeholder

`<grafana>/d/canvas-release/deploy-rollback`

## diagnostic commands

```bash
bash scripts/release/post-deploy-check.sh
curl -sS "$CANVAS_BASE_URL/actuator/health"
curl -sS "$CANVAS_BASE_URL/actuator/prometheus" | grep -E "canvas.execution|http.server.requests|flyway|route|cache"
kubectl -n "${CANVAS_K8S_NAMESPACE:-canvas}" rollout status deployment/"${CANVAS_K8S_DEPLOYMENT:-canvas-engine}"
```

## remediation commands

```bash
bash scripts/release/pre-deploy-check.sh --dry-run
bash scripts/release/post-deploy-check.sh
```

For route/cache-only regressions, run `route-rebuild.md` or `cache-invalidation.md` before application rollback when source data is correct.

## rollback commands

Application rollback:

```bash
export CANVAS_K8S_NAMESPACE="${CANVAS_K8S_NAMESPACE:-canvas}"
export CANVAS_K8S_DEPLOYMENT="${CANVAS_K8S_DEPLOYMENT:-canvas-engine}"
export CANVAS_ROLLBACK_OWNER="${CANVAS_ROLLBACK_OWNER:-Runtime lead}"
export CANVAS_ROLLBACK_EVIDENCE_DIR="docs/architecture/evidence/release-drills"
bash scripts/release/rollback-drill.sh
```

Database restore requires DBA approval and backup evidence from `flyway-backup-rollback.md`:

```bash
export CANVAS_DB_RESTORE_COMMAND='mysql -h "$CANVAS_DB_HOST" -P "${CANVAS_DB_PORT:-3306}" -u "$CANVAS_DB_USER" -p"$CANVAS_DB_PASSWORD" "$CANVAS_DB_NAME" < "$CANVAS_RESTORE_SQL"'
bash scripts/release/rollback-drill.sh --execute-db-restore
```

## evidence

Capture release version, image tag, migration version, backup checksum, failed health/SLO evidence, rollback owner decision, rollback command output, and post-rollback health under `docs/architecture/evidence/release-<yyyyMMdd-HHmmss>/`.
