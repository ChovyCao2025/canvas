# Canvas Stress Testing

The only supported execution path for capacity testing is the local capacity runbook, with `tools/perf/perf-guide.mjs` enforcing correctness gates.

Do not use archived capacity estimates as measured results. A capacity number is valid only when it is backed by:

- unique `perfRunId`
- runner summary JSON
- verifier JSON with `verdict: "PASS"`
- guide `report` result with `status: "PASS"`
- monitor snapshots
- environment details
- capacity input parameters
- cleanup record

## Quick Start

```bash
cd /Users/photonpay/project/canvas
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

Then follow the full runbook:

- [Local capacity runbook](./local-capacity-runbook.md)
- [Performance audit](./performance-audit.md)
- [Report template](./report-template.md)

## Hard Rules

- If verifier is not `PASS`, stop and fix the run before capacity planning.
- If runner `failed` is not `0`, stop and fix the run before capacity planning.
- If guide `report` is not `PASS`, do not publish throughput, QPS, p95, or capacity estimates.
- `PASS_WITH_EXPECTED_FAILURES` is for fault reports only.
- Do not report QPS without the matching `perfRunId`.
- Do not reuse a `perfRunId`.
- Cleanup defaults to ledger-only. Full cleanup requires `--scope all --execute true`.
