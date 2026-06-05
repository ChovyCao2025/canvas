# ADR-007 Service Split Boundaries

status: Deferred
owner: Platform Architecture

## Source Evidence

Optimization plans proposed monolith split, runtime service decomposition, and separate data/platform services.

## Current-Code Evidence

Current package boundaries include web controllers, domain services, engine handlers, scheduler, delivery, channel, CDP, analytics, Redis, Doris, and MQ infrastructure in one Spring Boot artifact.

## Decision

Defer service split until concurrency hardening, runtime lane metrics, tenant isolation evidence, and rollback economics are accepted.

## Expected Benefit

Clear ownership, independent scaling, and reduced blast radius if boundaries are already stable.

## Cost

Very high: API contracts, transactions, cache invalidation, eventing, deployment, observability, and incident response all change.

## Rollback

Ship split services behind route and feature flags while preserving the single artifact. Roll back by routing all traffic to the monolith.

## Proof Command

`node tools/perf/runtime-migration-baseline.mjs --format json`

## Accepted Evidence

Required: package dependency graph, runtime lane saturation metrics, transaction boundary inventory, rollback rehearsal, and cost estimate.

## Child Spec

Required before implementation: `p2-018f-service-split-boundary-proof`.

## Dependency Notes

Depends on P1-004D and P2-015 hardening evidence. Must not start before production safety gates and trace observability are stable.
