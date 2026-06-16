# Playground

The Playground is the Open Source Growth first-run flow for trying the seeded
demo profile without real external providers. G10/G11 ecosystem seeds now give
the frontend and docs a stable `new-user-welcome` golden path: template metadata,
required plugins, sample payload, expected trace, Canvas DSL validation, and a
mock AI risk audit. The flow remains frontend/docs guided until final DDD-C09
and OSG-W14 live wiring evidence proves the live backend runtime path end to
end. The checked-in fixture and WireMock catalog now have an offline runtime
smoke verifier for local demo readiness.

## Start Demo Dependencies

From the repository root:

```bash
docker compose -f docker-compose.demo.yml config
docker compose -f docker-compose.demo.yml up -d
docker compose -f docker-compose.demo.yml ps
```

The demo shell starts:

| Service | Port | Purpose |
| --- | --- | --- |
| MySQL 8.0 | `3306` | Local `canvas_db` database |
| Redis 7 | `6379` | Cache and route state |
| WireMock | `8099` | Mock message, coupon, approval, AI, and catalog APIs |
| RocketMQ Namesrv | `9876` | MQ trigger name server |
| RocketMQ Broker | `10909`, `10911`, `10912` | Local broker |

Do not run `docker-compose.local.yml` and `docker-compose.demo.yml` at the same
time unless you change ports; both expose the same local service ports.

Run the backend and frontend in separate terminals with the same commands from
[quickstart.md](quickstart.md). The default local account remains:

- username: `admin`
- password: `Admin@123`

## Mock Catalog

After WireMock starts, inspect the demo catalog:

```bash
curl http://localhost:8099/mock/demo/golden-path
curl http://localhost:8099/mock/demo/templates
curl http://localhost:8099/mock/demo/plugins
```

Useful mock provider endpoints:

```bash
curl -X POST http://localhost:8099/mock/message/sms
curl -X POST http://localhost:8099/mock/message/email
curl -X POST http://localhost:8099/mock/approval/start
curl -X POST http://localhost:8099/mock/ai/audit
```

The template catalog includes `new-user-welcome` as the first golden-path
example. Its docs live at
[templates/new-user-welcome.md](templates/new-user-welcome.md).

## Golden Path Data

The frontend catalog exposes the playground handoff from
`frontend/src/pages/canvas-list/templateCatalog.ts` through
`getPlaygroundGoldenPath()`. It is derived from the official
`new-user-welcome` template and includes:

| Field | Value |
| --- | --- |
| Template key | `new-user-welcome` |
| Template title | `新用户欢迎旅程` |
| Required plugins | `canvas-plugin-webhook`, `canvas-plugin-coupon`, `canvas-plugin-message` |
| Sample event | `user.registered` |
| Expected trace nodes | `segment`, `coupon`, `message` |
| Publish boundary | `draft-preview-only` |

## Golden Path Steps

1. Start demo dependencies with `docker compose -f docker-compose.demo.yml up -d`.
2. Start the backend in one terminal.
3. Start the frontend in another terminal.
4. Log in with the default local account.
5. Review the `new-user-welcome` template docs and required plugin list.
6. Clone or import the template as a draft canvas only.
7. Dry-run the sample payload and compare the trace to the expected
   `segment -> coupon -> message` path.
8. Validate the current checked-in CLI fixture for the `new-user-welcome`
   metadata.
9. Run the mock AI risk audit and keep the result as preview/draft-only output.

Offline runtime smoke commands:

```bash
docker compose -f docker-compose.demo.yml config
node tools/open-source-growth/playground-runtime-smoke.mjs
cd tools/canvas-cli && node src/index.mjs validate test/fixtures/playground-new-user-welcome.json
```

The offline smoke verifier reads only checked-in local artifacts. It validates
the dedicated fixture, the `segment -> coupon -> message` path, sample payload
eligibility, WireMock golden-path/template/plugin coherence, and this document's
runtime-smoke command reference without requiring live backend services. This is
the default CI-safe guardrail for playground readiness.

Optional live backend API smoke:

```bash
node tools/open-source-growth/playground-live-api-smoke.mjs --api-url http://localhost:8080
```

Run the live API smoke only after the backend is running. It posts the dedicated
`new-user-welcome` DSL fixture to the live Canvas DSL map endpoint and checks
for the stable mapping response. This live command is evidence for OSG-W14
wiring, but it is not the default offline CI guardrail because it depends on a
running backend. If the local backend requires bearer auth, pass `--token` or
set `CANVAS_API_TOKEN`.

Useful mock catalog and provider checks:

```bash
curl http://localhost:8099/mock/demo/golden-path
curl http://localhost:8099/mock/demo/templates
curl http://localhost:8099/mock/demo/plugins
curl -X POST http://localhost:8099/mock/ai/audit
```

The CLI validation command uses the dedicated checked-in playground fixture
`tools/canvas-cli/test/fixtures/playground-new-user-welcome.json`, whose
`metadata.name` is `new-user-welcome`.

The dry-run and trace step uses the template sample payload:

```json
{
  "event": "user.registered",
  "user": {
    "id": "u_1001",
    "lifecycleStage": "new",
    "phone": "+8613800000001"
  }
}
```

Expected trace checkpoints:

| Node | Outcome | Meaning |
| --- | --- | --- |
| `segment` | `MATCHED` | User is in the new lifecycle stage |
| `coupon` | `SENT` | Welcome coupon is granted |
| `message` | `SENT` | Welcome message is sent by the mock provider |

## AI Preview Boundary

The editor-side mock assistant supports the playground handoff as a preview
surface only. It reports `provider: mock-ai`, `mode: draft-preview-only`, trace
references, and risk-audit references. Its publish action is intentionally
disabled until live draft, publish, trace, and risk APIs are wired and verified.

## Current Limits

- This page documents the G10/G11-seed-aware playground flow and the
  frontend-only handoff surface.
- Local/offline runtime smoke covers checked-in fixture and WireMock catalog
  coherence; live backend runtime smoke remains gated on actual DDD-C09/OSG-W14
  wiring verification.
- Template import, dry-run, trace, DSL export, CLI validation, and AI audit must
  continue through public APIs when live wiring is enabled.
- The mock AI audit must not publish, overwrite published versions, or require
  a real model key.

The demo shell must not bypass tenant, auth, publish, execution, trace, plugin,
or risk boundaries.

## Stop Demo Dependencies

```bash
docker compose -f docker-compose.demo.yml down
```

Add `-v` only when you want to delete local demo volumes:

```bash
docker compose -f docker-compose.demo.yml down -v
```
