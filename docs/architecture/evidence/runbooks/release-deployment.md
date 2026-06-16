# Release Deployment Runbook

## Scope

This runbook turns the release checklist into commands. Run it for staging first, then production after the same evidence is captured.

## Pre-deploy

Dry run from the repository root:

```bash
bash scripts/release/pre-deploy-check.sh --dry-run
```

Actual pre-deploy gate:

```bash
export CANVAS_IMAGE_TAG="<git-sha>"
export CANVAS_MIGRATION_BACKUP_EVIDENCE="docs/architecture/evidence/release-<timestamp>/canvas-before-flyway.sql.sha256"
export CANVAS_DB_HOST="<db-host>"
export CANVAS_DB_PORT="3306"
export SPRING_DATA_REDIS_HOST="<redis-host>"
export SPRING_DATA_REDIS_PORT="6379"
export ROCKETMQ_NAME_SERVER="<rocketmq-host>:9876"

bash scripts/release/pre-deploy-check.sh
```

The script validates production-like profiles, Flyway migration policy, immutable image tag, backup evidence, and dependency reachability.

## Build Image

Dry run:

```bash
CANVAS_IMAGE_NAME="registry.example.com/marketing-canvas/canvas-boot" \
CANVAS_IMAGE_TAG="<git-sha>" \
bash scripts/release/build-image.sh --dry-run
```

Actual build:

```bash
CANVAS_IMAGE_NAME="registry.example.com/marketing-canvas/canvas-boot" \
CANVAS_IMAGE_TAG="<git-sha>" \
bash scripts/release/build-image.sh --push
```

## Deploy

Render the Helm release before applying it. The backend runtime image is `canvas-boot`, while service-name resources intentionally keep the stable `canvas-engine` compatibility names documented in `docs/runbooks/backend-service-name-cutover.md`.

```bash
export CANVAS_IMAGE_NAME="registry.example.com/marketing-canvas/canvas-boot"
export CANVAS_IMAGE_TAG="<git-sha>"

helm template canvas deploy/helm/canvas \
  --namespace canvas \
  --set backend.image.repository="$CANVAS_IMAGE_NAME" \
  --set backend.image.tag="$CANVAS_IMAGE_TAG" \
  > docs/architecture/evidence/release-<timestamp>/canvas-rendered.yaml

helm upgrade --install canvas deploy/helm/canvas \
  --namespace canvas \
  --create-namespace \
  --set backend.image.repository="$CANVAS_IMAGE_NAME" \
  --set backend.image.tag="$CANVAS_IMAGE_TAG"

kubectl -n canvas rollout status deployment/canvas-engine
```

Do not deploy backend static manifests with `kubectl apply -f deploy/k8s/canvas-engine-*` for this cutover path; that bypasses the Helm service-name compatibility policy.

Secrets must be created outside the deployment YAML:

```bash
kubectl -n canvas create secret generic canvas-engine-runtime \
  --from-literal=spring-datasource-url="$SPRING_DATASOURCE_URL" \
  --from-literal=spring-datasource-username="$SPRING_DATASOURCE_USERNAME" \
  --from-literal=spring-datasource-password="$SPRING_DATASOURCE_PASSWORD" \
  --from-literal=spring-data-redis-password="$SPRING_DATA_REDIS_PASSWORD" \
  --from-literal=canvas-event-report-secret="$CANVAS_EVENT_REPORT_SECRET" \
  --from-literal=canvas-internal-api-token="$CANVAS_INTERNAL_API_TOKEN" \
  --from-literal=canvas-public-trigger-secret="$CANVAS_PUBLIC_TRIGGER_SECRET" \
  --from-literal=canvas-jwt-secret="$CANVAS_JWT_SECRET" \
  --from-literal=canvas-secret-cipher-key="$CANVAS_SECRET_CIPHER_KEY"
```

The stable `canvas-engine` ServiceAccount and its RBAC bindings must exist before the Helm upgrade in a fresh namespace, because the chart intentionally preserves `backend.serviceAccountName: canvas-engine` for compatibility:

```bash
kubectl -n canvas get serviceaccount canvas-engine
kubectl auth can-i get pods --as=system:serviceaccount:canvas:canvas-engine -n canvas
```

## Post-deploy

Dry run:

```bash
bash scripts/release/post-deploy-check.sh --dry-run
```

Actual validation:

```bash
export CANVAS_BASE_URL="https://canvas.example.com"
export CANVAS_SMOKE_URL="https://canvas.example.com/api/actuator/health"
export CANVAS_ALERTS_URL="https://alertmanager.example.com/api/v2/alerts"
bash scripts/release/post-deploy-check.sh
```

The script checks health, Prometheus metrics, optional smoke URL, and optional runtime alert source.

## Rollback

Dry-run the rollback command path:

```bash
bash scripts/release/rollback-drill.sh --dry-run
```

Application rollback:

```bash
export CANVAS_K8S_NAMESPACE="canvas"
export CANVAS_K8S_DEPLOYMENT="canvas-engine"
export CANVAS_ROLLBACK_OWNER="Runtime lead"
export CANVAS_ROLLBACK_EVIDENCE_DIR="docs/architecture/evidence/release-drills"
bash scripts/release/rollback-drill.sh
```

Database restore is a separate decision point. Only run it after the rollback owner and DBA approve:

```bash
export CANVAS_DB_RESTORE_COMMAND='mysql -h "$CANVAS_DB_HOST" -P "${CANVAS_DB_PORT:-3306}" -u "$CANVAS_DB_USER" -p"$CANVAS_DB_PASSWORD" "$CANVAS_DB_NAME" < "$CANVAS_RESTORE_SQL"'
bash scripts/release/rollback-drill.sh --execute-db-restore
```

## Evidence

Capture the following in `docs/architecture/evidence/P1-05-release-deployment-governance.md` or a release-specific evidence directory:

- CI workflow run URL.
- Image tag and build output.
- Flyway migration policy output.
- Backup checksum path.
- `kubectl rollout status` output.
- Post-deploy health and Prometheus output.
- Rollback drill evidence path and database restore decision.
