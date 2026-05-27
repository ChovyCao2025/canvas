# 3000 Concurrency Hardening Checklist

## Purpose

This checklist is the production-readiness gate for the 3000 Canvas execution concurrency target. 3000 means cluster-level active Canvas executions, not HTTP connections, MQ backlog, DAU, or single-instance concurrency.

## Backend Test Baseline

Run before any 3000 hardening code change:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Pass condition:

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Stop condition:

- Test compilation fails.
- Any listed test class fails.
- A failure is marked unrelated without a file, test name, and reproducible command.

## 3000 Completion Gate

3000 is complete only when:

- affected backend tests pass on Java 21
- the default mixed profile passes the full observation window
- retry surge passes after downstream recovery
- heavy surge does not degrade LIGHT or STANDARD
- slow downstream is contained by timeout, circuit breaker, and bulkhead behavior
- Redis registry latency or outage fails conservatively
- RocketMQ backlog recovery does not let RETRY starve normal traffic
- retry backlog, DLQ growth, Disruptor overflow, and MQ backlog have stop gates
- rollback and degrade actions have been exercised
- 4000 remains blocked until this checklist passes
