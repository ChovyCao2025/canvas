# Canvas Performance Tools

This directory contains the low-level performance scripts. The only supported execution path for a capacity test is the guided runbook in `docs/stressTest/README.md`, using `tools/perf/perf-guide.mjs` for smoke, threshold, soak, report, cleanup, and distributed gates.

Use these scripts directly only for debugging or advanced investigation. Capacity numbers are valid only when the matching guide report accepts the runner and verifier evidence.

## Recommended Entry Point

```bash
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
node tools/perf/perf-guide.mjs fixture --rebuild true
node tools/perf/perf-guide.mjs smoke \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --event-secret-env PERF_EVENT_SECRET
```

After smoke passes, continue with threshold and soak in `docs/stressTest/local-capacity-runbook.md`. For multiple load workers or multiple backend nodes, first pass local smoke and accuracy, then use `docs/stressTest/distributed-capacity-runbook.md`. Run `perf-guide report` or `perf-guide distributed-report` only for run ids that have guide-produced runner and verifier evidence.

If verifier is not `PASS`, do not publish throughput, QPS, p95, or capacity estimates. `PASS_WITH_EXPECTED_FAILURES` is only valid for fault reports.

## Distributed Runs

Distributed runs use one global `perfRunId` and split request sequence ranges across workers. Do not start multiple workers by hand with the same `--count` range; that creates duplicate `perfInputId` values and invalidates the verifier evidence.

Control machine:

```bash
node tools/perf/perf-guide.mjs distributed-plan \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --mode event \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count 1 \
  --worker-ids worker-01,worker-02,worker-03 \
  --total-count 300000 \
  --total-concurrency 300 \
  --distributed-root tmp/perf-distributed
```

Each load worker:

```bash
node tools/perf/perf-guide.mjs distributed-worker \
  --plan-file "tmp/perf-distributed/$PERF_RUN_ID/plan.json" \
  --worker-id worker-01
```

Control machine after collecting every worker summary:

```bash
node tools/perf/perf-guide.mjs distributed-report \
  --plan-file "tmp/perf-distributed/$PERF_RUN_ID/plan.json"
```

Use `--accuracy true` on `distributed-plan` for distributed engine accuracy checks. That path runs the global verifier with trace expectations and then runs the WireMock side-effect verifier.

## Fixtures

Prepare the standard local test resources through the guide:

```bash
node tools/perf/perf-guide.mjs fixture \
  --base-url http://localhost:8080 \
  --rebuild true
```

The command creates and publishes:

- `PERF_DIRECT_LIGHT`
- `PERF_EVENT_LIGHT`
- `PERF_ENGINE_ACCURACY`

Use `PERF_ENGINE_ACCURACY` with `perf-guide accuracy` before capacity testing.

## Request Signing Secret

Event and direct modes sign requests with HMAC headers when a secret is available through an environment variable. Do not pass secret values as command-line arguments.

```bash
export PERF_EVENT_SECRET="<local-secret-at-least-32-bytes>"
```

Use `--event-secret-env PERF_EVENT_SECRET` in event and direct commands. The runner records only whether signing was enabled and which env var was used; it does not print the secret value.

## Run IDs

Use one unique run id per scenario:

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)
```

Do not reuse a `PERF_RUN_ID` for capacity evidence. Reuse can mix old and new database rows or old local JSON files.

## Event Runner

```bash
node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --count 10000 \
  --concurrency 100 \
  --summary-file "tmp/perf-$PERF_RUN_ID-event.json"
```

The runner prints `sent`, `success`, `failed`, `p95Ms`, timestamps, run settings, and machine metadata. Exit code `2` means the command completed with request failures; that run is not valid capacity evidence.

## Direct Runner

```bash
node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --event-secret-env PERF_EVENT_SECRET \
  --count 1000 \
  --concurrency 50 \
  --summary-file "tmp/perf-$PERF_RUN_ID-direct.json"
```

Direct mode uses deterministic `idempotencyKey` values of the form `$PERF_RUN_ID:direct:<seq>`.

## Threshold Runner

Use the guide first:

```bash
node tools/perf/perf-guide.mjs threshold \
  --mode event \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count 1
```

Advanced event example:

```bash
node tools/perf/threshold-runner.mjs \
  --mode event \
  --base-url http://localhost:8080 \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --stages 1000:10,5000:50,10000:100,30000:200,50000:400 \
  --matched-canvas-count 1 \
  --max-failed 0 \
  --max-p95-ms 1000 \
  --wait-after-run-ms 10000 \
  --out-dir tmp/perf-threshold \
  --run-id-prefix "perf_$(date +%Y%m%d_%H%M%S)"
```

Advanced direct example:

```bash
node tools/perf/threshold-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --event-secret-env PERF_EVENT_SECRET \
  --stages 1000:10,5000:50,10000:100,30000:200,50000:400 \
  --matched-canvas-count 1 \
  --max-failed 0 \
  --max-p95-ms 1000 \
  --wait-after-run-ms 0 \
  --out-dir tmp/perf-threshold \
  --run-id-prefix "perf_$(date +%Y%m%d_%H%M%S)"
```

Threshold verdicts:

- `MAX_STAGE_STABLE`: all configured stages passed.
- `THRESHOLD_FOUND`: the first unstable stage was found; use `stableStage` as the current stable ceiling.
- `NO_STABLE_STAGE`: even the first stage failed; fix environment, fixtures, or correctness before testing capacity.

Stage failure reasons:

- `RUNNER_FAILED`: HTTP request failures exceeded `--max-failed`.
- `P95_EXCEEDED`: runner p95 exceeded `--max-p95-ms`.
- `VERIFIER_FAIL`: reconciliation failed. This is not valid capacity data.

## Verify Correctness

Normal event run:

```bash
node tools/perf/verifier.mjs \
  --mode event \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1
```

Direct run:

```bash
node tools/perf/verifier.mjs \
  --mode direct \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 1000 \
  --matched-canvas-count 1
```

Use the runner `success` value as `--sent-success`, not the requested `--count`, if any HTTP request failed. Accuracy verification must happen before using latency or throughput numbers.

Trace-level accuracy example:

```bash
node tools/perf/verifier.mjs \
  --mode direct \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success "$RUNNER_SUCCESS" \
  --matched-canvas-count 1 \
  --expect-trace direct:success=all \
  --expect-trace normalize:success=all \
  --expect-trace route_even:success=all \
  --expect-trace send_even:success=even \
  --expect-trace send_odd:success=odd \
  --expect-trace send_even:skipped=odd \
  --expect-trace send_odd:skipped=even \
  --expect-trace join:success=all \
  --expect-trace end:success=all
```

The guide wraps this command automatically:

```bash
node tools/perf/perf-guide.mjs accuracy \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$ENGINE_ACCURACY_CANVAS_ID" \
  --count 20000 \
  --concurrency 100
```

Side-effect audit only:

```bash
node tools/perf/side-effect-verifier.mjs \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success "$RUNNER_SUCCESS" \
  --wiremock-url http://localhost:8099 \
  --path /mock/reach/send
```

## Estimate Capacity

```bash
node tools/perf/capacity-report.mjs \
  --verifier-verdict PASS \
  --local-stable-qps 1200 \
  --local-app-cores 8 \
  --prod-app-cores-total 32 \
  --writes-per-event 4 \
  --prod-db-safe-write-qps 12000 \
  --redis-ops-per-event 3 \
  --prod-redis-safe-ops 30000 \
  --rocketmq-capacity 7000 \
  --disruptor-worker-capacity 9000 \
  --downstream-rate-limit-per-sec 5000 \
  --downstream-calls-per-event 1
```

The capacity report rejects `--verifier-verdict FAIL`. `PASS_WITH_EXPECTED_FAILURES` is accepted only for explicitly declared fault scenarios and must not be used for ordinary capacity reports.

## Cleanup

Cleanup defaults to `--scope ledger`. Ledger cleanup deletes rows tied to the supplied `perfRunId` and preserves `PERF_%` event and MQ definitions.

Preview ledger cleanup:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID"
```

Execute ledger cleanup:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID" --execute true
```

Use `--scope all` only after all local capacity testing is complete. It removes the `PERF_%` event and MQ definitions as well as the run ledger rows.

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID" --scope all --execute true
```
