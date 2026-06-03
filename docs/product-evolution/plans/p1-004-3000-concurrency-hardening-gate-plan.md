# 3000 Concurrency Hardening Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the 3000 active Canvas execution target into a repeatable release gate with validated profiles, evidence artifacts, stop gates, rollback actions, and degradation actions.

**Architecture:** Keep the existing Redis ZSET admission, lane resolver, execution-request backlog metrics, and Node.js perf tools. Tighten them into a release gate by validating profile semantics, writing evidence manifests, adding hardening gate evaluators, recording lane/registry metrics, and documenting the operator runbook.

**Tech Stack:** Java 21, Spring Boot, Micrometer, Redis, RocketMQ, MySQL/Hikari, Node.js built-in test runner, JSON perf profiles, Maven, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-004-3000-concurrency-hardening-gate.md`
- Optimization source: `docs/optimization/3000-concurrency-hardening-checklist.md`

## Current Baseline

- `tools/perf/3000-hardening-profiles.json` already exists with target concurrency `3000` and lane budgets `600/1800/300/300`.
- Current profile names include `retry-surge-300`, `heavy-surge-300`, `slow-downstream-standard`, `redis-registry-latency`, and `rocketmq-backlog-recovery`; the checklist expects explicit `*-3000` hardening profile names and failure-mode drills.
- `tools/perf/hardening-profile.mjs` validates lane totals and renders a threshold-runner command, but it does not write an evidence manifest directory.
- `tools/perf/capacity-report.mjs` estimates capacity bottlenecks, but it does not evaluate the 3000 hardening stop gates from metric samples.
- `InFlightExecutionRegistry` already performs canvas/lane/global Redis ZSET admission and rejects conservatively on Redis failure.
- `ExecutionLaneResolver` already maps overflow retry and persistent request retry to `RETRY`, direct/continuation to `LIGHT`, and scheduled/Groovy/TAGGER/subflow to `HEAVY`; test coverage is not exhaustive.

## File Structure

**Perf profiles and tools**
- Modify: `tools/perf/3000-hardening-profiles.json`
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/capacity-report.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`
- Modify: `tools/perf/capacity-report.test.mjs`
- Modify: `tools/perf/README.md`

**Backend runtime metrics and guards**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

**Docs**
- Create: `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`

**Tests**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`

### Task 1: Profile Contract And Failure-Mode Schema

**Files:**
- Modify: `tools/perf/3000-hardening-profiles.json`
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`
- Modify: `tools/perf/README.md`

- [ ] **Step 1: Add profile schema tests**

Modify `tools/perf/hardening-profile.test.mjs` by adding these tests:

```js
test('validateHardeningProfiles requires protected lane borrow rules', () => {
  const config = structuredClone(validConfig)
  config.protectedLanes = ['LIGHT', 'STANDARD']
  config.borrowRules = {
    HEAVY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
    RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  }

  assert.equal(validateHardeningProfiles(config).protectedLanes.length, 2)
})

test('validateHardeningProfiles rejects heavy borrowing from light', () => {
  const config = structuredClone(validConfig)
  config.protectedLanes = ['LIGHT', 'STANDARD']
  config.borrowRules = {
    HEAVY: { cannotBorrowFrom: ['STANDARD'] },
    RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  }

  assert.throws(
    () => validateHardeningProfiles(config),
    /HEAVY must not borrow protected lane LIGHT/,
  )
})

test('validateHardeningProfiles requires all 3000 failure-mode profiles', () => {
  const config = structuredClone(validConfig)
  config.protectedLanes = ['LIGHT', 'STANDARD']
  config.borrowRules = {
    HEAVY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
    RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  }
  config.requiredProfiles = [
    'default-mixed-3000',
    'retry-surge-3000',
    'heavy-surge-3000',
    'redis-latency-spike-3000',
    'mysql-saturation-3000',
    'rocketmq-backlog-3000',
    'downstream-partial-failure-3000',
    'retry-backlog-explosion-3000',
  ]
  config.profiles = config.requiredProfiles.map((name) => ({
    name,
    description: name,
    mode: 'event',
    eventCode: 'PERF_ORDER_PAID',
    stages: [{ count: 1000, concurrency: 100 }],
    maxFailed: 0,
    maxP95Ms: 1000,
    waitAfterRunMs: 1000,
    stopGates: ['RUNNER_FAILED'],
    rollbackActions: ['restore_previous_concurrency'],
    degradeActions: ['reduce_retry_lane'],
  }))

  assert.equal(validateHardeningProfiles(config).profiles.length, 8)
})
```

Extend `validConfig` at the top of the same test file:

```js
protectedLanes: ['LIGHT', 'STANDARD'],
borrowRules: {
  HEAVY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
},
requiredProfiles: ['default-mixed-3000'],
```

Add the following fields to its existing `default-mixed-3000` test profile:

```js
stopGates: ['RUNNER_FAILED'],
rollbackActions: ['restore_previous_concurrency'],
degradeActions: ['reduce_retry_lane'],
```

- [ ] **Step 2: Run profile tests and confirm red state**

Run: `node --test tools/perf/hardening-profile.test.mjs`

Expected: FAIL because `validateHardeningProfiles` does not validate `protectedLanes`, `borrowRules`, `requiredProfiles`, profile-level `stopGates`, `rollbackActions`, or `degradeActions`.

- [ ] **Step 3: Update profile validator**

Modify `tools/perf/hardening-profile.mjs`:

```js
function requireStringArray(label, value) {
  if (!Array.isArray(value) || value.length === 0 || value.some((item) => typeof item !== 'string' || item.trim() === '')) {
    throw new Error(`${label} must be a non-empty string array`)
  }
  return value
}

function validateProtectedLaneRules(config) {
  const protectedLanes = requireStringArray('protectedLanes', config.protectedLanes)
  for (const lane of ['HEAVY', 'RETRY']) {
    const blocked = config.borrowRules?.[lane]?.cannotBorrowFrom
    requireStringArray(`${lane}.cannotBorrowFrom`, blocked)
    for (const protectedLane of protectedLanes) {
      if (!blocked.includes(protectedLane)) {
        throw new Error(`${lane} must not borrow protected lane ${protectedLane}`)
      }
    }
  }
}

function validateRequiredProfiles(config) {
  const requiredProfiles = requireStringArray('requiredProfiles', config.requiredProfiles)
  const names = new Set(config.profiles.map((profile) => profile.name))
  for (const name of requiredProfiles) {
    if (!names.has(name)) {
      throw new Error(`missing required profile ${name}`)
    }
  }
}
```

Call the helpers inside `validateHardeningProfiles(config)` after lane total validation and after profile loop setup:

```js
validateProtectedLaneRules(config)
```

Inside the profile loop, add:

```js
requireStringArray(`${profile.name}.stopGates`, profile.stopGates)
requireStringArray(`${profile.name}.rollbackActions`, profile.rollbackActions)
requireStringArray(`${profile.name}.degradeActions`, profile.degradeActions)
```

After the profile loop, add:

```js
validateRequiredProfiles(config)
```

- [ ] **Step 4: Align `3000-hardening-profiles.json` with checklist names**

Modify `tools/perf/3000-hardening-profiles.json` so the top-level shape includes:

```json
{
  "targetConcurrency": 3000,
  "observationWindowSeconds": 1800,
  "protectedLanes": ["LIGHT", "STANDARD"],
  "borrowRules": {
    "HEAVY": { "cannotBorrowFrom": ["LIGHT", "STANDARD"] },
    "RETRY": { "cannotBorrowFrom": ["LIGHT", "STANDARD"] }
  },
  "requiredProfiles": [
    "default-mixed-3000",
    "retry-surge-3000",
    "heavy-surge-3000",
    "redis-latency-spike-3000",
    "mysql-saturation-3000",
    "rocketmq-backlog-3000",
    "downstream-partial-failure-3000",
    "retry-backlog-explosion-3000"
  ]
}
```

Keep the existing `lanes` object unchanged:

```json
"lanes": {
  "LIGHT": { "concurrency": 600, "share": 0.2 },
  "STANDARD": { "concurrency": 1800, "share": 0.6 },
  "HEAVY": { "concurrency": 300, "share": 0.1 },
  "RETRY": { "concurrency": 300, "share": 0.1 }
}
```

Rename and expand profiles to this exact set. Each profile must include `stopGates`, `rollbackActions`, and `degradeActions`:

```json
{
  "name": "retry-surge-3000",
  "description": "Retry lane reaches 300 while LIGHT and STANDARD stay protected.",
  "mode": "event",
  "eventCode": "PERF_RETRY_SURGE",
  "stages": [
    { "count": 5000, "concurrency": 100 },
    { "count": 15000, "concurrency": 300 }
  ],
  "maxFailed": 0,
  "maxP95Ms": 1500,
  "waitAfterRunMs": 30000,
  "stopGates": ["RETRY_BACKLOG_GROWING_AFTER_RECOVERY", "DLQ_GROWING_AFTER_RECOVERY"],
  "rollbackActions": ["restore_previous_lane_budgets", "pause_retry_replay"],
  "degradeActions": ["lower_retry_lane", "lengthen_retry_backoff"]
}
```

- [ ] **Step 5: Update perf README profile list**

Modify `tools/perf/README.md` under `3000 Hardening Profiles` so the required profile list is exactly:

```markdown
- `default-mixed-3000`
- `retry-surge-3000`
- `heavy-surge-3000`
- `redis-latency-spike-3000`
- `mysql-saturation-3000`
- `rocketmq-backlog-3000`
- `downstream-partial-failure-3000`
- `retry-backlog-explosion-3000`
```

- [ ] **Step 6: Run profile tests**

Run: `node --test tools/perf/hardening-profile.test.mjs`

Expected: PASS with lane total, borrow rules, required profiles, and command rendering validated.

### Task 2: Evidence Manifest Output

**Files:**
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`

- [ ] **Step 1: Add evidence manifest tests**

Modify `tools/perf/hardening-profile.test.mjs` imports:

```js
import {
  buildEvidenceManifest,
  renderThresholdCommand,
  selectProfile,
  validateHardeningProfiles,
} from './hardening-profile.mjs'
```

Add test:

```js
test('buildEvidenceManifest includes run id, command, lane budget, gates, and sample files', () => {
  const profile = selectProfile(validateHardeningProfiles(validConfig), 'default-mixed-3000')
  const manifest = buildEvidenceManifest(validConfig, profile, {
    baseUrl: 'http://localhost:8080',
    outDir: 'tmp/perf-3000-hardening',
    runIdPrefix: 'perf_3000_gate',
    now: '2026-06-03T10:00:00.000Z',
  })

  assert.equal(manifest.targetConcurrency, 3000)
  assert.equal(manifest.profileName, 'default-mixed-3000')
  assert.equal(manifest.lanes.STANDARD.concurrency, 1800)
  assert.deepEqual(manifest.protectedLanes, ['LIGHT', 'STANDARD'])
  assert.match(manifest.command, /threshold-runner\.mjs/)
  assert.deepEqual(manifest.metricSampleFiles, [
    'redis-latency.json',
    'mysql-pool.json',
    'rocketmq-backlog.json',
    'retry-backlog.json',
    'dlq-count.json',
    'trace-buffer.json',
    'downstream-latency.json',
  ])
})
```

- [ ] **Step 2: Run evidence test and confirm red state**

Run: `node --test tools/perf/hardening-profile.test.mjs`

Expected: FAIL because `buildEvidenceManifest` is not exported.

- [ ] **Step 3: Add evidence manifest builder**

Modify `tools/perf/hardening-profile.mjs`:

```js
export function buildEvidenceManifest(config, profile, options = {}) {
  const now = options.now || new Date().toISOString()
  const runIdPrefix = options.runIdPrefix || `perf_${profile.name}`
  return {
    schemaVersion: 1,
    generatedAt: now,
    runIdPrefix,
    profileName: profile.name,
    targetConcurrency: config.targetConcurrency,
    observationWindowSeconds: config.observationWindowSeconds,
    lanes: config.lanes,
    protectedLanes: config.protectedLanes,
    borrowRules: config.borrowRules,
    stopGates: profile.stopGates,
    rollbackActions: profile.rollbackActions,
    degradeActions: profile.degradeActions,
    command: renderThresholdCommand(profile, options),
    metricSampleFiles: [
      'redis-latency.json',
      'mysql-pool.json',
      'rocketmq-backlog.json',
      'retry-backlog.json',
      'dlq-count.json',
      'trace-buffer.json',
      'downstream-latency.json',
    ],
  }
}
```

- [ ] **Step 4: Add CLI evidence write option**

Import filesystem helpers:

```js
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
```

Extend `parseCliArgs` defaults:

```js
writeEvidence: false,
```

In the CLI parser, accept:

```js
else if (flag === '--write-evidence') args.writeEvidence = value === 'true'
```

Change the CLI block:

```js
const command = renderThresholdCommand(profile, args)
if (args.writeEvidence) {
  const manifest = buildEvidenceManifest(config, profile, args)
  const runDir = path.join(args.outDir, args.runIdPrefix || `perf_${profile.name}`)
  mkdirSync(runDir, { recursive: true })
  writeFileSync(path.join(runDir, 'evidence-manifest.json'), JSON.stringify(manifest, null, 2))
}
console.log(command)
```

- [ ] **Step 5: Run evidence tests**

Run: `node --test tools/perf/hardening-profile.test.mjs`

Expected: PASS with evidence manifest construction covered.

### Task 3: Capacity Stop Gate Evaluator

**Files:**
- Modify: `tools/perf/capacity-report.mjs`
- Modify: `tools/perf/capacity-report.test.mjs`

- [ ] **Step 1: Add stop gate evaluator tests**

Modify `tools/perf/capacity-report.test.mjs` import:

```js
import { estimateCapacity, evaluateHardeningGates, parseCapacityArgs } from './capacity-report.mjs'
```

Add tests:

```js
test('evaluateHardeningGates passes healthy hardening samples', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 8,
    redisP99Ms: 20,
    mysqlActiveConnections: 40,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 200,
    normalMqBacklogGrowing: false,
    disruptorOverflowConsecutiveSamples: 0,
    retryBacklogGrowingAfterRecovery: false,
    dlqGrowingAfterRecovery: false,
    lightP95Ms: 500,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'PASS')
  assert.deepEqual(result.stopGates, [])
})

test('evaluateHardeningGates reports redis and mysql stop gates', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 25,
    redisP99Ms: 55,
    mysqlActiveConnections: 90,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 1200,
    normalMqBacklogGrowing: false,
    disruptorOverflowConsecutiveSamples: 0,
    retryBacklogGrowingAfterRecovery: false,
    dlqGrowingAfterRecovery: false,
    lightP95Ms: 500,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'STOP')
  assert.deepEqual(result.stopGates, [
    'REDIS_REGISTRY_LATENCY_SUSTAINED',
    'MYSQL_POOL_SATURATION',
    'MYSQL_SLOW_SQL',
  ])
})
```

- [ ] **Step 2: Run capacity tests and confirm red state**

Run: `node --test tools/perf/capacity-report.test.mjs`

Expected: FAIL because `evaluateHardeningGates` is not exported.

- [ ] **Step 3: Add hardening gate evaluator**

Modify `tools/perf/capacity-report.mjs`:

```js
export function evaluateHardeningGates(samples) {
  const stopGates = []
  if (samples.redisP95Ms > 20 || samples.redisP99Ms > 50) {
    stopGates.push('REDIS_REGISTRY_LATENCY_SUSTAINED')
  }
  if ((samples.mysqlActiveConnections / samples.mysqlMaxConnections) >= 0.85) {
    stopGates.push('MYSQL_POOL_SATURATION')
  }
  if (samples.mysqlSlowSqlMs > 1000) {
    stopGates.push('MYSQL_SLOW_SQL')
  }
  if (samples.normalMqBacklogGrowing) {
    stopGates.push('NORMAL_MQ_BACKLOG_STARVED_BY_RETRY')
  }
  if (samples.disruptorOverflowConsecutiveSamples >= 2) {
    stopGates.push('DISRUPTOR_OVERFLOW_GROWING')
  }
  if (samples.retryBacklogGrowingAfterRecovery) {
    stopGates.push('RETRY_BACKLOG_GROWING_AFTER_RECOVERY')
  }
  if (samples.dlqGrowingAfterRecovery) {
    stopGates.push('DLQ_GROWING_AFTER_RECOVERY')
  }
  if (samples.lightP95Ms > 1000 || samples.standardP95Ms > 1000) {
    stopGates.push('PROTECTED_LANE_LATENCY_VIOLATION')
  }
  return {
    verdict: stopGates.length === 0 ? 'PASS' : 'STOP',
    stopGates,
  }
}
```

- [ ] **Step 4: Run capacity tests**

Run: `node --test tools/perf/capacity-report.test.mjs`

Expected: PASS with existing capacity tests and new stop gate tests green.

### Task 4: Backend Lane And Registry Metrics

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`

- [ ] **Step 1: Add registry metric tests**

Modify `InFlightExecutionRegistryLaneTest` constructor setup by adding:

```java
@Mock CanvasMetrics metrics;
```

Update the helper:

```java
private InFlightExecutionRegistry registry() {
    InFlightExecutionRegistry registry = new InFlightExecutionRegistry(redis, keys, metrics);
    ReflectionTestUtils.setField(registry, "globalTimeoutSec", 600L);
    return registry;
}
```

Add tests:

```java
@Test
void redisFailureRecordsRegistryUnavailableMetric() {
    InFlightExecutionRegistry registry = registry();
    when(redis.execute(any(RedisScript.class), anyList(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalStateException("redis down"));

    ExecutionLaneAdmissionResult result = registry.tryAcquire(
            10L, "exec-1", ExecutionLane.LIGHT, 3000, 600, 3000);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo(ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE);
    verify(metrics).recordExecutionRegistryAdmission("LIGHT", "REGISTRY_UNAVAILABLE");
}

@Test
void successfulAcquireRecordsLaneActiveGauge() {
    InFlightExecutionRegistry registry = registry();
    when(redis.execute(any(RedisScript.class), anyList(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(1L);

    ExecutionLaneAdmissionResult result = registry.tryAcquire(
            10L, "exec-1", ExecutionLane.HEAVY, 3000, 300, 3000);

    assertThat(result.allowed()).isTrue();
    verify(metrics).recordExecutionRegistryAdmission("HEAVY", "NONE");
    verify(metrics).setExecutionLaneActive("HEAVY", 0L);
}
```

- [ ] **Step 2: Run registry tests and confirm red state**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest`

Expected: FAIL because `CanvasMetrics` does not expose registry methods and `InFlightExecutionRegistry` does not inject metrics.

- [ ] **Step 3: Add CanvasMetrics registry and pressure methods**

Modify `CanvasMetrics`:

```java
private final ConcurrentMap<String, AtomicLong> executionLaneActive = new ConcurrentHashMap<>();
private final ConcurrentMap<String, AtomicLong> traceBufferBacklog = new ConcurrentHashMap<>();

public void recordExecutionRegistryAdmission(String lane, String reason) {
    Counter.builder("canvas.execution.registry.admission.total")
            .tag("lane", lane != null ? lane : "UNKNOWN")
            .tag("reason", reason != null ? reason : "UNKNOWN")
            .register(registry)
            .increment();
}

public void recordExecutionRegistryLatency(long latencyMs) {
    Timer.builder("canvas.execution.registry.latency")
            .publishPercentiles(0.95, 0.99)
            .register(registry)
            .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
}

public void setExecutionLaneActive(String lane, long count) {
    String normalizedLane = lane != null ? lane : "UNKNOWN";
    AtomicLong gauge = executionLaneActive.computeIfAbsent(normalizedLane, key -> {
        AtomicLong value = new AtomicLong();
        Gauge.builder("canvas.execution.lane.active", value, AtomicLong::get)
                .tag("lane", key)
                .register(registry);
        return value;
    });
    gauge.set(Math.max(0L, count));
}

public void setTraceBufferBacklog(String buffer, long count) {
    String normalizedBuffer = buffer != null ? buffer : "default";
    AtomicLong gauge = traceBufferBacklog.computeIfAbsent(normalizedBuffer, key -> {
        AtomicLong value = new AtomicLong();
        Gauge.builder("canvas.trace.buffer.backlog", value, AtomicLong::get)
                .tag("buffer", key)
                .register(registry);
        return value;
    });
    gauge.set(Math.max(0L, count));
}
```

- [ ] **Step 4: Record registry metrics in admission**

Modify `InFlightExecutionRegistry` constructor fields:

```java
private final CanvasMetrics metrics;
```

Wrap Redis acquire timing:

```java
long startNs = System.nanoTime();
try {
    result = redis.execute(...);
    metrics.recordExecutionRegistryLatency(
            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs));
} catch (Exception e) {
    metrics.recordExecutionRegistryAdmission(effectiveLane.name(), "REGISTRY_UNAVAILABLE");
    ...
}
```

After a rejection result is mapped:

```java
ExecutionLaneAdmissionResult.Reason reason = mapReason(result);
metrics.recordExecutionRegistryAdmission(effectiveLane.name(), reason.name());
metrics.setExecutionLaneActive(effectiveLane.name(), laneActiveCount(effectiveLane));
return ExecutionLaneAdmissionResult.rejected(reason, activeCount(canvasId), laneActiveCount(effectiveLane), totalActiveCount());
```

After successful local registration:

```java
metrics.recordExecutionRegistryAdmission(effectiveLane.name(), "NONE");
metrics.setExecutionLaneActive(effectiveLane.name(), laneActiveCount(effectiveLane));
```

- [ ] **Step 5: Add backlog metric test for retry pressure**

Modify `CanvasExecutionRequestBacklogMetricsTest`:

```java
@Test
void refreshPublishesRetryBacklogPressure() {
    CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
    CanvasMetrics metrics = mock(CanvasMetrics.class);
    CanvasExecutionRequestBacklogMetrics backlogMetrics =
            new CanvasExecutionRequestBacklogMetrics(mapper, metrics);
    when(mapper.countByStatus()).thenReturn(List.of(
            new CanvasExecutionRequestStatusCount(CanvasExecutionRequestStatus.RETRY, 30L)
    ));

    backlogMetrics.refresh();

    verify(metrics).setExecutionRequestBacklog(CanvasExecutionRequestStatus.RETRY, 30L);
}
```

- [ ] **Step 6: Run backend metric tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest,CanvasExecutionRequestBacklogMetricsTest`

Expected: PASS with registry admission and backlog metrics covered.

### Task 5: Lane Resolver Coverage And Config Gate

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

- [ ] **Step 1: Add exhaustive lane resolver matrix**

Modify `ExecutionLaneResolverTest`:

```java
@Test
void resolvesHeavyLaneForChecklistHeavyWork() {
    assertThat(resolver.resolve(TriggerType.SCHEDULED, NodeType.SCHEDULED_TRIGGER, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.DLQ_REPLAY, NodeType.MQ_TRIGGER, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.EVENT, NodeType.GROOVY, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.EVENT, NodeType.TAGGER, Map.of("mode", "audience"), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.SUB_FLOW_REF, NodeType.SUB_FLOW_REF, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
}

@Test
void resolvesLightLaneForDirectAndInternalContinuationWork() {
    assertThat(resolver.resolve(TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.WAIT_RESUME, NodeType.WAIT, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.HUB_TIMEOUT, NodeType.HUB, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.AGGREGATE_TIMEOUT, NodeType.AGGREGATE, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.THRESHOLD_TIMEOUT, NodeType.THRESHOLD, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
}

@Test
void resolvesRetryLaneForOverflowAndPersistentRequestRetryBeforeOtherRules() {
    assertThat(resolver.resolve(TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL, Map.of(), true, false, 0))
            .isEqualTo(ExecutionLane.RETRY);
    assertThat(resolver.resolve(TriggerType.SCHEDULED, NodeType.SCHEDULED_TRIGGER, Map.of(), false, true, 2))
            .isEqualTo(ExecutionLane.RETRY);
}
```

- [ ] **Step 2: Run lane resolver tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=ExecutionLaneResolverTest`

Expected: PASS with scheduled, replay, Groovy, TAGGER audience, subflow, direct, continuation, overflow retry, and persistent request retry mappings covered.

- [ ] **Step 3: Make 3000 config explicit**

Confirm `backend/canvas-engine/src/main/resources/application.yml` contains exactly these effective values:

```yaml
canvas:
  execution:
    max-concurrency: 3000
  execution-lane:
    light:
      max-concurrency: 600
    standard:
      max-concurrency: 1800
    heavy:
      max-concurrency: 300
    retry:
      max-concurrency: 300
```

The current `application.yml` values already match this block; keep them unchanged and mention the confirmation in the implementation PR.

### Task 6: Runbook

**Files:**
- Create: `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`
- Modify: `tools/perf/README.md`

- [ ] **Step 1: Add runbook**

Create `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`:

````markdown
# 3000 Concurrency Hardening Runbook

## Entry Requirements

- Java 21 backend baseline passes.
- `canvas.execution.max-concurrency=3000`.
- Lane budgets are `LIGHT=600`, `STANDARD=1800`, `HEAVY=300`, `RETRY=300`.
- Redis, MySQL, RocketMQ, downstream test doubles, and local backend are reachable.
- `tools/perf/3000-hardening-profiles.json` validates lane totals and required profile names.

## Baseline Command

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,ExecutionLaneResolverTest,InFlightExecutionRegistryLaneTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Pass condition: `BUILD SUCCESS`.

## Profile Execution

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/3000-hardening-profiles.json \
  --profile default-mixed-3000 \
  --out-dir tmp/perf-3000-hardening \
  --run-id-prefix perf_3000_hardening_$(date +%Y%m%d_%H%M%S) \
  --write-evidence true
```

Run the command printed by `hardening-profile.mjs` and keep the generated `evidence-manifest.json`.

## Stop Gates

- Redis p95 above 20 ms or p99 above 50 ms for one observation window.
- MySQL active connections at or above 85% of pool max.
- Slow SQL above 1000 ms in two consecutive samples.
- Normal MQ backlog grows while RETRY drains.
- Disruptor overflow grows for two consecutive samples.
- Retry backlog grows after downstream recovery.
- DLQ grows after downstream recovery.
- LIGHT or STANDARD p95 exceeds 1000 ms.

## Rollback Actions

- Restore previous `canvas.execution.max-concurrency`.
- Restore previous lane budgets.
- Pause scheduled, replay, and heavy traffic when needed.
- Keep normal traffic on the last passing profile.
- Rerun the backend baseline after rollback.

## Degrade Actions

- Lower `RETRY` or lengthen retry backoff.
- Lower `HEAVY` or pause heavy jobs.
- Disable low-priority scheduled/replay traffic.
- Keep `LIGHT` and `STANDARD` protected when their dependencies remain healthy.
- Reject new admission conservatively if Redis registry health is unknown.

## 4000 Block

Do not start 4000 readiness until every required 3000 profile passes and evidence artifacts are retained.
````

- [ ] **Step 2: Link runbook from perf README**

Modify `tools/perf/README.md` in the `3000 Hardening Profiles` section:

```markdown
Detailed operational steps live in `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`.
```

### Task 7: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p1-004-3000-concurrency-hardening-gate.md`
- Modify: `docs/product-evolution/plans/p1-004-3000-concurrency-hardening-gate-plan.md`

- [ ] **Step 1: Run Node perf tool tests**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs tools/perf/capacity-report.test.mjs
```

Expected: PASS with profile validation, evidence manifest, and hardening gate evaluator tests green.

- [ ] **Step 2: Run backend focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest,ExecutionLaneResolverTest,CanvasExecutionRequestBacklogMetricsTest
```

Expected: PASS with registry, lane resolver, and backlog metric tests green.

- [ ] **Step 3: Run 3000 baseline command**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,ExecutionLaneResolverTest,InFlightExecutionRegistryLaneTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Expected: `BUILD SUCCESS` and all listed tests pass.

- [ ] **Step 4: Validate profile total**

Run:

```bash
node -e "const p=require('./tools/perf/3000-hardening-profiles.json'); const total=Object.values(p.lanes).reduce((sum,l)=>sum+l.concurrency,0); if (total !== p.targetConcurrency) throw new Error(String(total)); console.log(total)"
```

Expected: prints `3000`.

- [ ] **Step 5: Render evidence manifest**

Run:

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/3000-hardening-profiles.json \
  --profile default-mixed-3000 \
  --out-dir tmp/perf-3000-hardening \
  --run-id-prefix perf_3000_hardening_doc_check \
  --write-evidence true
```

Expected: prints a `threshold-runner.mjs` command and writes `tmp/perf-3000-hardening/perf_3000_hardening_doc_check/evidence-manifest.json`.

- [ ] **Step 6: Commit implementation slice**

Run:

```bash
git add \
  tools/perf/3000-hardening-profiles.json \
  tools/perf/hardening-profile.mjs \
  tools/perf/capacity-report.mjs \
  tools/perf/hardening-profile.test.mjs \
  tools/perf/capacity-report.test.mjs \
  tools/perf/README.md \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java \
  backend/canvas-engine/src/main/resources/application.yml \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java \
  docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md \
  docs/product-evolution/specs/p1-004-3000-concurrency-hardening-gate.md \
  docs/product-evolution/plans/p1-004-3000-concurrency-hardening-gate-plan.md
git commit -m "test: harden 3000 concurrency release gate"
```

Expected: commit contains only 3000 hardening profiles, tools, metrics, tests, runbook, spec, and plan files.
