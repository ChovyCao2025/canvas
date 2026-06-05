# ADR-001 Runtime Web Stack

status: Proof Required
owner: Platform Runtime

## Source Evidence

Optimization reviews flagged a WebFlux plus blocking persistence mismatch and proposed Spring MVC plus virtual threads.

## Current-Code Evidence

`backend/canvas-engine/pom.xml` includes `spring-boot-starter-webflux`, MyBatis-Plus, JDBC, and reactive Redis. Controllers return `Mono<R<...>>` and offload blocking mapper calls with `Schedulers.boundedElastic()`.

## Decision

Do not migrate the web stack yet. Open a child spec only after a proof shows MVC plus virtual threads improves throughput or simplifies blocking persistence without breaking reactive Redis, WebClient integrations, or current controller contracts.

## Expected Benefit

Lower scheduler complexity around blocking mapper calls and clearer request-thread ownership.

## Cost

High: controller signatures, security filters, OpenAPI WebFlux starter, tests, and operational thread metrics all change.

## Rollback

Keep current WebFlux artifact and controller contracts until a parallel branch proves compatibility. Roll back by restoring WebFlux dependencies and route handlers.

## Proof Command

`mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest`

## Accepted Evidence

Required: dependency inventory, endpoint compatibility matrix, p95 latency comparison, thread utilization comparison, and rollback rehearsal.

## Child Spec

Required before implementation: `p2-018a-web-stack-mvc-virtual-thread-migration`.

## Dependency Notes

Depends on P0/P1 production safety and runtime gates. Must not run before API compatibility tests are stable.
