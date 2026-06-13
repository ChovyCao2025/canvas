# DDD-C09AC Coordinator Recovery Note

Date: 2026-06-13 11:20 +08:00

## Status

DDD-C09AC remains `RUNNING` and requires coordinator recovery inside the exact
reserved scope before it can move to review.

## Evidence Reviewed

- `multi_agent_v1.wait_agent` for Hilbert
  `019ebe6d-4a7d-7853-a7ff-5486e87b2e1d` returned a completed status without a
  final message body or worker-return packet.
- `docs/program-coordination/evidence/dispatch-DDD-C09AC-bi-query-dataset-catalog-routes-20260613-100500/`
  contains only `recovery-note.md`.
- Exact reserved files exist, but several live under currently untracked DDD
  module directories, so ordinary `git diff` is not sufficient evidence for
  this dispatch.

## Verification Run

Initial Maven verification with the shell default Java failed because
`JAVA_HOME` pointed to Java 8 and Maven rejected `--release`.

Re-run with JDK 21:

```bash
export JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl canvas-web -Dtest=BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
mvn -pl canvas-context-bi -Dtest=BiCatalogApplicationServiceTest test
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
node tools/program-coordination/check-dispatch-state.mjs .
```

Result:

- `canvas-web` test compilation failed before focused BI tests because prior
  untracked canvas-web test sources require reactor dependency modules to be
  built together or installed.
- `canvas-context-bi` compilation failed with:
  `BiCatalogApplicationService is not abstract and does not override getQueryDataset(Long,String)`.
- Cutover preflight still reports `cutoverReady: false`, with current
  canvas-web at 15 controllers / 53 endpoints and `/canvas/bi` still the
  largest route gap.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.

## Required Recovery

Within the DDD-C09AC reserved files:

- implement `listQueryDatasets(Long)` and `getQueryDataset(Long, String)` in
  `BiCatalogApplicationService`;
- map `BiQueryDatasetCatalog` domain records into compact
  `BiQueryDatasetView`, `BiQueryFieldView`, and `BiQueryMetricView`;
- expose production `GET /canvas/bi/datasets` and
  `GET /canvas/bi/datasets/{datasetKey}` routes in `BiCatalogController`;
- re-run focused JDK 21 Maven verification with reactor dependencies included
  as needed, then write `worker-return.md` or coordinator closeout evidence.

## Next Action

Coordinator should repair the exact reserved scope, run focused verification,
and only then move DDD-C09AC to review or close it with documented concerns.
