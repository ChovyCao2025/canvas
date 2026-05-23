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

## HTTP Event Test

```bash
node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --count 10000 \
  --concurrency 100
```

The runner prints `sent`, `success`, `failed`, and `p95Ms`. A nonzero `failed` count exits with code `2`.

## Direct Call Test

```bash
node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id 1 \
  --count 1000 \
  --concurrency 50
```

Direct mode uses deterministic `idempotencyKey` values of the form `$PERF_RUN_ID:direct:<seq>`.

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

## RocketMQ Test

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

## Estimate Capacity

```bash
node tools/perf/capacity-report.mjs \
  --local-stable-qps 1200 \
  --local-app-cores 8 \
  --prod-app-cores-total 32 \
  --writes-per-event 4 \
  --prod-db-safe-write-qps 12000 \
  --redis-ops-per-event 3 \
  --prod-redis-safe-ops 30000 \
  --rocketmq-capacity 7000 \
  --downstream-rate-limit-per-sec 5000 \
  --downstream-calls-per-event 1
```

Use `recommendedCapacity` as the first production planning value. Use `alertThreshold` for the first alert line. The default safety factor is `0.5`.

## Cleanup

Preview SQL:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID"
```

Execute cleanup:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID" --execute true
```

Cleanup deletes only rows tied to the given `perfRunId` plus `PERF_%` event and MQ fixture definitions. It does not delete canvas definitions.
