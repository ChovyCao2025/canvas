# MarketingOps As Code

MarketingOps as Code uses Canvas DSL v1 documents to describe local journey
templates and demos before they are imported into a runtime environment.

The current CLI validates JSON files against the stable Canvas DSL v1 fields,
can compare node-level changes between two documents, and can call the G10
Canvas DSL import/export preview endpoints. It does not publish journeys.

## Local CLI

From the repository root:

```bash
cd tools/canvas-cli
npm test
node src/index.mjs --help
```

Validate a local Canvas DSL v1 JSON document:

```bash
node src/index.mjs validate path/to/journey.json
```

The validator checks:

- `apiVersion: canvas/v1`
- `kind: Journey`
- `metadata.name` as a non-empty string
- `spec.trigger`
- a non-empty `spec.nodes` array
- `spec.nodes[].id` as a non-empty string
- `spec.nodes[].type` as a non-empty string
- unique node ids

Diff two local Canvas DSL v1 JSON documents:

```bash
node src/index.mjs diff before.json after.json
```

The diff command prints added, removed, and changed node ids. A node is treated
as changed when the JSON content for the same node id differs.

Import a local Canvas DSL v1 JSON document through the G10 backend preview API:

```bash
node src/index.mjs import journey.json --api-url http://localhost:8080
```

Export a Canvas DSL v1 JSON document through the G10 backend preview API:

```bash
node src/index.mjs export <canvasId> \
  --api-url http://localhost:8080 \
  --tenant-id <tenantId>
```

`--api-url` can also come from `CANVAS_API_URL`. `--tenant-id` can also come
from `CANVAS_TENANT_ID`.

## G10 Boundary

CLI import/export is unlocked for the stable Canvas DSL endpoints:

- `POST /canvas/dsl/import`
- `GET /canvas/dsl/export/{canvasId}`

Publish remains gated. Do not claim CLI publish readiness until a stable backend
publish endpoint is verified by current final-module tests.

The repeatable offline evidence check for the current G10 prerequisites is:

```bash
node tools/open-source-growth/g10-public-api-stability.mjs
```

This verifier records that the final-module Canvas DSL public API and
compatibility coverage are present, while confirming CLI import/export remains
within the approved endpoints and publish stays blocked.
