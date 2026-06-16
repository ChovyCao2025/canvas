# Backend Service Name Cutover

## Current Decision

The production runtime image is `canvas-boot`, but Kubernetes compatibility names stay stable until an explicit service/DNS cutover is approved:

- `backend.name`: `canvas-engine`
- `backend.serviceAccountName`: `canvas-engine`
- `backend.secretName`: `canvas-engine-runtime`
- `backend.image.repository`: `registry.example.com/marketing-canvas/canvas-boot`

This keeps existing service discovery, DNS, RBAC bindings, secret references, ingress rules, dashboards, alerts, and operator runbooks stable while the runtime artifact moves to `canvas-boot`.

## Rename Rule

Do not perform a mechanical rename from `canvas-engine` to `canvas-boot` for service-name resources.

Before rename, prove DNS compatibility for every internal and external caller that still resolves `canvas-engine`, including Kubernetes service DNS, ingress/backoffice routes, monitoring targets, alert labels, and runbook curl examples. The rename requires a separate compatibility window with rollback to the stable `canvas-engine` names.

## Verification

Run the static gate before changing Helm service names:

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
```

Expected: `serviceNameCompatibility.helmValues` keeps the `canvas-engine` backend, service account, and runtime secret names while using the `canvas-boot` image repository and an immutable image tag.
