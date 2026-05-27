# Canvas Performance Testing

Local performance harness for load generation, correctness reconciliation, capacity estimation, and cleanup.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- Docker dependencies from `docker-compose.local.yml`
- Local backend on `http://localhost:8080`
- Local MySQL reachable with `mysql -uroot -proot canvas_db`
- For this machine, Maven commands may need:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

## Run IDs

Every run uses a unique `perfRunId`:

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)
```

Use one run ID per scenario so verifier and cleanup queries stay isolated.

## Small-Flow Smoke

Before any capacity run, prove that the local fixtures, backend, middleware, verifier, and cleanup path are all wired correctly with a small isolated run:

1. Create one fresh `PERF_RUN_ID`.
2. Run one small scenario:
   - event: `--count 100 --concurrency 10`
   - direct: `--count 50 --concurrency 5`
   - MQ: `--count 100`
   - audience: `--count 1 --concurrency 1`
3. Run the verifier for that same `PERF_RUN_ID`.
4. Continue to capacity testing only when the verifier verdict is `PASS`.
5. Run cleanup first as a dry run, then execute it with `--execute true`.

This smoke run is intentionally small. Its job is correctness and isolation, not throughput.

## HTTP Event Test

```bash
node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --count 10000 \
  --concurrency 100 \
  --summary-file "tmp/perf-$PERF_RUN_ID-event.json"
```

The runner prints `sent`, `success`, `failed`, `p95Ms`, timestamps, run settings, and machine metadata. A nonzero `failed` count exits with code `2`.

## Threshold Runner

Use this wrapper after smoke passes. It runs staged pressure, verifies each run, and stops at the first unstable stage.

Event example:

```bash
node tools/perf/threshold-runner.mjs \
  --mode event \
  --base-url http://localhost:8080 \
  --event-code PERF_ORDER_PAID \
  --stages 1000:10,5000:50,10000:100,30000:200,50000:400 \
  --matched-canvas-count 1 \
  --max-failed 0 \
  --max-p95-ms 1000 \
  --wait-after-run-ms 10000 \
  --out-dir tmp/perf-threshold \
  --run-id-prefix "perf_$(date +%Y%m%d_%H%M%S)"
```

Direct example:

```bash
node tools/perf/threshold-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --stages 1000:10,5000:50,10000:100,30000:200,50000:400 \
  --matched-canvas-count 1 \
  --max-failed 0 \
  --max-p95-ms 1000 \
  --wait-after-run-ms 0 \
  --out-dir tmp/perf-threshold \
  --run-id-prefix "perf_$(date +%Y%m%d_%H%M%S)"
```

Verdicts:

- `MAX_STAGE_STABLE`: all configured stages passed. Add a higher stage if you still need the limit.
- `THRESHOLD_FOUND`: the first unstable stage was found. Use `stableStage` as the current maximum stable point.
- `NO_STABLE_STAGE`: even the first stage failed. Fix correctness or environment before capacity testing.

Stage failure reasons:

- `RUNNER_FAILED`: HTTP request failures exceeded `--max-failed`.
- `P95_EXCEEDED`: runner p95 exceeded `--max-p95-ms`.
- `VERIFIER_FAIL`: correctness reconciliation failed. This result is not valid capacity data.

## Direct Call Test

```bash
node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id 1 \
  --count 1000 \
  --concurrency 50 \
  --summary-file "tmp/perf-$PERF_RUN_ID-direct.json"
```

Direct mode uses deterministic `idempotencyKey` values of the form `$PERF_RUN_ID:direct:<seq>`.

### Direct Duplicate Test

Use this to prove concurrent idempotency behavior. The runner reuses the first N direct keys at the tail of the run, where `N = floor(count * duplicateRate)`.

```bash
node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id 1 \
  --count 1000 \
  --concurrency 100 \
  --duplicate-rate 0.01 \
  --summary-file "tmp/perf-$PERF_RUN_ID-direct-dup.json"
```

Then verify the intentional duplicate count:

```bash
node tools/perf/verifier.mjs \
  --mode direct \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 1000 \
  --matched-canvas-count 1 \
  --intentional-duplicates 10
```

The duplicate run is valid only when the verifier returns `PASS`. A `FAIL` means the system either executed duplicated inputs, lost accepted inputs, over-deduplicated, or left work unfinished.

## Audience Compute Test

```bash
node tools/perf/perf-runner.mjs \
  --mode audience \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --audience-id 1 \
  --count 10 \
  --concurrency 2
```

Pass the expected audience size to the verifier when checking an audience run.
Audience runs are isolated through `audience_compute_run.perf_run_id`.

## RocketMQ Test

Prefetch Maven dependencies before any timed run so artifact download latency is not mixed into the MQ measurement:

```bash
cd tools/perf/mq-producer
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q -DskipTests dependency:go-offline
```

```bash
cd tools/perf/mq-producer
mvn -q test
mvn -q exec:java \
  -Dexec.mainClass=org.chovy.canvas.perf.mq.PerfMqProducer \
  -Dexec.args="--name-server localhost:9876 --topic CANVAS_MQ_TRIGGER --tag PERF_MQ --perf-run-id $PERF_RUN_ID --count 10000 --user-modulo 1000"
```

The producer sends deterministic keys of the form `$PERF_RUN_ID:mq:<seq>`.

## Verify Correctness

Normal event or MQ run:

```bash
node tools/perf/verifier.mjs \
  --mode event \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1
```

Audience run:

```bash
node tools/perf/verifier.mjs \
  --mode audience \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10 \
  --audience-id 1 \
  --expected-audience-count 10000
```

Fault scenario with expected failures:

```bash
node tools/perf/verifier.mjs \
  --mode event \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1 \
  --expected-failed-records 50
```

Verdicts:

- `PASS`: no loss, duplicate input, bad dedup, unfinished retry, DLQ, rejected record, or failed execution.
- `PASS_WITH_EXPECTED_FAILURES`: only explicitly declared failure records were observed.
- `FAIL`: unexpected loss, duplicate input, bad dedup, unfinished retry, wrong audience count, or unplanned failure records.

For concurrent runs, use the runner `success` value as `--sent-success`, not the requested `--count`, if any HTTP request failed. Accuracy verification must happen before using latency or throughput numbers for capacity planning.

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

Use `recommendedCapacity` as the first production planning value. Use `alertThreshold` for the first alert line. The default safety factor is `0.5`. The capacity report rejects `--verifier-verdict FAIL`; failed correctness data is not valid capacity input. `PASS_WITH_EXPECTED_FAILURES` is accepted only for explicitly declared fault scenarios and should be called out in the report.

## Report Discipline

Every saved performance report should include:

- `PERF_RUN_ID`, scenario mode, requested count, successful sends, concurrency, duplicate rate, and verifier verdict.
- Runner summary JSON from `--summary-file`, including measured p95 latency, CPU/core/memory/OS metadata, Node version, Java version, and JVM-related environment options.
- Any local runtime details not visible to the process, especially container CPU/memory limits when the backend is not running directly on the host.
- Backend deployment shape: app instance count, worker/thread settings, Disruptor settings, connection pools, and log level.
- Dependency shape: MySQL version/config and safe write QPS, Redis version/config and safe ops, RocketMQ broker/topic/consumer config, and any downstream API rate limit.
- Capacity inputs and the resulting bottleneck candidates from `capacity-report.mjs`.
- Cleanup dry-run output and executed cleanup confirmation for the run ID.

## Cleanup

Preview SQL:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID"
```

Execute cleanup:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID" --execute true
```

Cleanup deletes only rows tied to the given `perfRunId`, including audience compute run ledgers, plus `PERF_%` event and MQ fixture definitions. It does not delete canvas definitions.
