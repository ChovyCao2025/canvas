# Capacity Report Template

## Test Identity

- `perfRunId`:
- Scenario:
- Resource profile:
- Backend image:
- Backend container CPU/memory:
- Operator:
- Date/time:

## Evidence

- Runner summary file:
- Verifier file:
- Guide report output:
- Monitor directory:
- Cleanup record:

## Correctness

- Runner `sent`:
- Runner `success`:
- Runner `failed`:
- Runner `durationMs`:
- Verifier verdict:
- Unexpected loss:
- Duplicate execution:
- Retry pending:
- DLQ:

If verifier verdict is not `PASS`, stop here and do not publish capacity numbers.

## Performance

- Requested count:
- Concurrency:
- Successful sends:
- Failed sends:
- Duration:
- QPS:
- p95:
- p99 source:

## Bottlenecks

- App CPU:
- JVM/GC:
- MySQL:
- Redis:
- RocketMQ:
- Downstream:

## Capacity Inputs

- localStableQps:
- localAppCores:
- prodAppCoresTotal:
- writesPerEvent:
- prodDbSafeWriteQps:
- redisOpsPerEvent:
- prodRedisSafeOps:
- rocketmqCapacity:
- disruptorWorkerCapacity:
- downstreamRateLimitPerSec:
- downstreamCallsPerEvent:
- safetyFactor:

## Conclusion

- Recommended capacity:
- Alert threshold:
- Rate limit threshold:
- Primary bottleneck:
- Evidence gaps:
