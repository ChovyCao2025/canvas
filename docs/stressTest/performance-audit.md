# Performance Testing Audit

## Verdict

The previous stress-test material had useful raw commands, but it was not safe enough for non-specialist execution. It mixed manual run steps, assumed event requests did not need signing, and allowed capacity narratives that were not tied to measured evidence.

## What Was Reasonable

- The backend should be tested in fixed-resource containers.
- Load generation should run on the host so backend resource limits remain meaningful.
- `perfRunId` isolation is the right way to separate test data.
- `verifier.mjs` checks correctness, not only HTTP success.
- Capacity estimation must reject verifier `FAIL`.

## Blocking Issues Removed

- Event and direct pressure tests now support HMAC signing through `--event-secret-env`; secret values are not accepted as CLI flags.
- The active docs no longer contain historical capacity claims without runner and verifier evidence.
- Cleanup defaults to ledger-only, so ordinary cleanup preserves `PERF_%` event and MQ definitions.
- Guide report gates require complete runner evidence, verifier evidence, matching `perfRunId`, zero request failures, and capacity duration evidence.
- Soak runs shorter than the configured duration are invalid for capacity reporting.

## Current Operating Model

Use the runbook as the operator checklist. Use `perf-guide.mjs` for gates and reportability. Fixture creation remains explicit because it creates published local `PERF_` canvases and must not be hidden behind an unsafe automatic rebuild.

Only reports generated from `tools/perf/perf-guide.mjs report` and backed by verifier `PASS` are valid capacity evidence.
