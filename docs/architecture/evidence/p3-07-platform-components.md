# P3-07 Platform Components Evidence

Date: 2026-06-05

## Verdict

P3-07 accepts only one production platform component for proof: Redisson behind a local distributed-coordination interface. No production rollout, scheduler replacement, config center migration, gateway migration, OLAP expansion, or CI/CD platform adoption is approved.

## Created Documents

- `docs/architecture/decisions/work-products/p3-07-platform-components/platform-component-decision-matrix.md`
- `docs/architecture/decisions/adr/platform-component-first-pov.md`
- `docs/architecture/decisions/work-products/p3-07-platform-components/platform-component-abstraction-plan.md`

## Inventory Commands

```bash
rg -n "^##|^###|XXL|Redisson|Nacos|Knife4j|Sentinel|Spring Boot Admin|ClickHouse|Logstash|Kibana|Zipkin|Harbor|Nexus|SonarQube|JMeter|Locust|Feign|Gateway|DolphinScheduler" docs/architecture/archive/evolution/production-practice-review.md
rg -n "Redisson|RLock|tryLock|StringRedisTemplate|LockSupport|lock:|distributed lock|@Scheduled|Scheduled|Scheduler|Nacos|Sentinel|resilience4j|CircuitBreaker|Spring Boot Admin|Knife4j|springdoc|ClickHouse|Doris|Prometheus|Grafana|Logstash|Zipkin" backend/canvas-engine/src/main/java backend/canvas-engine/src/main/resources docs/architecture/evidence docs/architecture/evidence/runbooks deploy ops
rg -n "@Scheduled" backend/canvas-engine/src/main/java/org/chovy/canvas
```

## Current Findings

- The production-practice review proposes XXL-JOB, Redisson, Nacos, Knife4j, Feign + Sentinel, Spring Boot Admin, ClickHouse, logging/search, tracing, Harbor, Nexus, SonarQube, load-test tooling, gateway, and data-platform schedulers.
- Current source has 20 `@Scheduled` methods, including runtime, warehouse, delivery, BI, watchdog, metrics, and cleanup jobs.
- Existing P2-04 dependency work already introduced local contracts for selected RocketMQ, Redis, Groovy, WebClient, and React Flow boundaries.
- Current observability already has Prometheus/Grafana assets and actuator exposure, so Spring Boot Admin is not the first proof.
- P3-03 deferred full ClickHouse/data-platform rollout, and P3-04 deferred physical datasource routing.

## Decision Summary

- Accepted for proof: Redisson behind `DistributedCoordination`.
- Deferred: XXL-JOB, Nacos, Feign + Sentinel, Spring Boot Admin, ClickHouse, Logstash/ELK or Loki, tracing, Harbor, Nexus, SonarQube, JMeter/Locust, Spring Cloud Gateway, DolphinScheduler, dynamic-datasource.
- Rejected for first proof: Knife4j, because Springdoc exists and it does not close a runtime P0/P1/P2 gap.

## Verification Commands

```bash
test -f docs/architecture/decisions/work-products/p3-07-platform-components/platform-component-decision-matrix.md
rg -n "XXL|Redisson|Nacos|Knife4j|Sentinel|Spring Boot Admin|ClickHouse|owner|failure mode|rollback|decision" docs/architecture/decisions/work-products/p3-07-platform-components/platform-component-decision-matrix.md
test -f docs/architecture/decisions/adr/platform-component-first-pov.md
rg -n "Problem|Decision|Alternatives|Rollout|Rollback|Owner|Success metric|Stop criteria|Deferred" docs/architecture/decisions/adr/platform-component-first-pov.md
test -f docs/architecture/decisions/work-products/p3-07-platform-components/platform-component-abstraction-plan.md
rg -n "interface|proof test|operational drill|metric|dashboard|rollback command|owner signoff" docs/architecture/decisions/work-products/p3-07-platform-components/platform-component-abstraction-plan.md
```

Result: all documentation checks passed.

## Follow-ups

- A future implementation PR may add the local `DistributedCoordination` interface and a disabled-by-default Redisson adapter.
- Do not add Redisson to business code directly.
- Do not expand to XXL-JOB, Nacos, Sentinel, ClickHouse, Gateway, or dynamic-datasource without separate ADRs and owner signoff.

No P3 files were staged or committed by this task.
