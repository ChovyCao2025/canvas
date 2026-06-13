# MarketingOps As Code

MarketingOps as Code uses Canvas DSL v1 documents to describe local journey
templates and demos before they are imported into a runtime environment.

The current CLI is intentionally local-only. It validates JSON files against the
stable Canvas DSL v1 fields and can compare node-level changes between two
documents. It does not call backend APIs and does not write to the database.

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

## G10 Limitation

Backend write APIs remain blocked until the G10 public extension/API stability
gate passes. Until then, the CLI must stay local-only: no import, publish, or
other backend write commands should be added.
